# OSS 历史图片迁移执行手册

更新时间：2026-04-18  
适用项目：`shakepro-backend`

## 1. 目标

将数据库里历史的非 OSS 图片地址迁移到当前 OSS，并满足以下要求：

1. 先盘点存量规模（非 OSS 域名）
2. 批量迁移（下载旧图 -> 上传 OSS -> 更新数据库）
3. 幂等与重试（已是 OSS 跳过；429/5xx 自动重试；相同 URL 复用上传结果）
4. 迁移后抽样验收（默认抽样 50 条）
5. 生成回滚 SQL（灰度 1-2 周可回退）

---

## 2. 覆盖字段

默认迁移以下字段：

- `cocktails.hero_image`
- `cocktails.image_url`
- `users.avatar_url`

可选字段（默认关闭）：

- `files.url`（通过参数 `IncludeFileRecordUrl` 开启）

---

## 3. 执行脚本

脚本路径：

- `D:\documents\学校课程\大四上\毕设\项目源代码\shakepro-backend\scripts\run-legacy-image-migration.ps1`

脚本会自动读取项目根目录 `.env`，并设置：

- `SPRING_MAIN_WEB_APPLICATION_TYPE=none`（不启动 Web 服务，仅执行迁移任务）
- `MIGRATION_LEGACY_IMAGES_*` 一组迁移参数

---

## 4. 具体执行步骤

### 步骤 1：盘点存量（只统计，不改数据）

```powershell
.\scripts\run-legacy-image-migration.ps1 -Mode inventory -Profile prod
```

日志会输出每个字段的：

- `totalWithValue`
- `httpUrl`
- `alreadyOss`
- `pendingMigration`

---

### 步骤 2：先 dry-run 演练（推荐）

```powershell
.\scripts\run-legacy-image-migration.ps1 -Mode migrate -Profile prod -DryRun
```

行为：

- 真实下载 + 上传验证链路
- 不写数据库
- 生成报告 CSV，便于先看失败原因

---

### 步骤 3：正式迁移

```powershell
.\scripts\run-legacy-image-migration.ps1 -Mode migrate -Profile prod
```

默认机制：

- 非 `http/https` URL 跳过
- 已是 OSS URL 跳过
- 相同 URL 复用同一上传结果
- 遇到 `429` 或 `5xx` 自动重试（默认 3 次）
- 单条失败只记日志，不中断整体任务

OSS 对象路径格式：

- `uploads/legacy/<table>/<id>-<sha256前24位>.<ext>`

---

### 步骤 4：迁移后抽样校验

若 `migrate` 模式默认会自动校验；也可单独执行：

```powershell
.\scripts\run-legacy-image-migration.ps1 -Mode verify -Profile prod -SampleSize 50
```

校验方式：

- 从 OSS URL 中随机抽样 50 条（可调）
- 请求 URL 检查可访问状态（2xx/3xx 通过）

---

### 步骤 5：灰度与兜底

每次 `migrate` 会在 `migration-reports` 目录生成：

- `legacy-image-migration-<timestamp>.csv`（逐条处理结果）
- `legacy-image-rollback-<timestamp>.sql`（回滚 SQL）

建议流程：

1. 迁移后先灰度 1-2 周
2. 观察业务日志、前端图片加载日志
3. 若异常，可执行回滚 SQL
4. 灰度稳定后再清理旧存储资源

---

## 5. 常用参数

- `-Mode inventory|migrate|verify`
- `-DryRun`：迁移演练
- `-BatchSize 200`：批大小
- `-SampleSize 50`：校验抽样数
- `-MaxRetries 3`：重试次数
- `-MaxImageSizeMB 10`：单张图片大小上限（超过则失败）
- `-IncludeFileRecordUrl`：启用 `files.url` 迁移
- `-SkipVerifyAfterMigrate`：迁移后不自动验收
- `-ReportDir migration-reports`：报告输出目录

---

## 6. 结果文件说明

处理报告 CSV 字段：

- `table,id,column,source_url,target_url,status,error,reused_upload`

`status` 常见值：

- `UPDATED`：已迁移并更新数据库
- `DRY_RUN`：演练通过但未更新
- `FAILED`：迁移失败（查看 `error`）

---

## 7. 建议的执行顺序（生产）

1. `inventory`
2. `migrate -DryRun`
3. 小批量正式迁移（可先调小 `BatchSize`）
4. 全量迁移
5. `verify` + 灰度观察
