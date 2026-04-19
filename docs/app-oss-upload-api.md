# ShakePro App 端 OSS 图片上传接口文档

更新时间：2026-04-17  
适用范围：App/小程序/H5 客户端图片上传（头像、鸡尾酒主图等）

## 1. 总体流程

客户端上传固定三步：

1. 调用 `POST /api/oss/presign` 获取一次性上传地址  
2. 使用 `PUT uploadUrl` 直传到阿里云 OSS  
3. 调用 `POST /api/files` 保存文件记录（拿到 `fileId`）

> 第 2 步调用的是 OSS 域名，不是后端域名。

---

## 2. 鉴权与基础信息

- 后端 Base URL：`https://<api-host>`（本地示例 `http://localhost:8080`）
- 鉴权：`Authorization: Bearer <token>`
- 统一响应结构：

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": "f7b1c9e2a4d3b6c1"
}
```

---

## 3. 服务端 OSS 配置约定（App 联调必读）

后端当前已切换到阿里云 OSS SDK，建议固定如下变量：

- `OSS_ENDPOINT=https://oss-<region>.aliyuncs.com`（不带 bucket）
- `OSS_BUCKET=<bucket-name>`
- `OSS_PUBLIC_URL=https://<bucket-name>.oss-<region>.aliyuncs.com`

例如（成都）：

- `OSS_ENDPOINT=https://oss-cn-chengdu.aliyuncs.com`
- `OSS_BUCKET=shakepro-img-prod`
- `OSS_PUBLIC_URL=https://shakepro-img-prod.oss-cn-chengdu.aliyuncs.com`

注意：

- `OSS_ENDPOINT` 不能写成 `https://<bucket>.oss-...`，否则会出现重复 bucket 主机名并触发 SSL 错误。
- bucket 所在地域必须和 endpoint 一致，否则会报 `AccessDenied: must be addressed using the specified endpoint`。

---

## 4. 接口明细

### 4.1 获取预签名上传地址

`POST /api/oss/presign`

请求体：

```json
{
  "filename": "mojito-cover.png",
  "contentType": "image/png",
  "size": 245123
}
```

字段说明：

- `filename`：原始文件名（用于生成扩展名）
- `contentType`：必须是允许类型之一
- `size`：文件字节数

允许类型：

- `image/jpeg`
- `image/png`
- `image/gif`
- `image/webp`
- `image/svg+xml`

大小限制：

- 最大 `10MB`

成功响应（`data`）：

```json
{
  "uploadUrl": "https://shakepro-img-prod.oss-cn-chengdu.aliyuncs.com/uploads/1/abc...png?....",
  "objectKey": "uploads/1/abc...png",
  "publicUrl": "https://shakepro-img-prod.oss-cn-chengdu.aliyuncs.com/uploads/1/abc...png",
  "expireSeconds": 3600
}
```

---

### 4.2 直传 OSS（非后端接口）

`PUT uploadUrl`

请求头：

- `Content-Type: <与 presign 一致的 contentType>`

请求体：

- 文件二进制（Blob/File）

成功状态码：

- `200` 或 `204`

常见失败原因：

- OSS CORS 没放行 `PUT`
- 上传时 `Content-Type` 和预签名时不一致
- 预签名过期（超过 `expireSeconds`）

---

### 4.3 保存文件记录

`POST /api/files`

请求体：

```json
{
  "objectKey": "uploads/1/abc...png",
  "url": "https://shakepro-img-prod.oss-cn-chengdu.aliyuncs.com/uploads/1/abc...png",
  "contentType": "image/png",
  "size": 245123
}
```

成功响应（`data`）：

```json
{
  "fileId": 1001
}
```

---

## 5. AI 生成配方入库时主图自动转存

以下接口在保存数据库时，会自动处理 `heroImage`：

- `POST /api/admin/cocktails/generated`
- `PUT /api/admin/cocktails/generated/{id}`

规则：

1. 如果 `heroImage` 已经是本项目 OSS 地址（`OSS_PUBLIC_URL/...`），直接使用  
2. 如果 `heroImage` 是外部 `http/https` 图片，后端下载后上传到 `uploads/generated/...` 并替换为 OSS URL  
3. 批量抓取自动入库（`/api/admin/crawl/import-from-list` 且 `autoSave=true`）同样适用

兜底策略：

- 自动转存失败时，保留原始 URL，不阻塞入库

---

## 6. OSS 控制台 CORS 最低要求（上传必须）

请在 Bucket 的 CORS 规则至少放行：

- AllowedOrigin：`https://<app-host>`（开发可临时 `*`）
- AllowedMethod：`PUT`, `GET`, `HEAD`
- AllowedHeader：`*`
- ExposeHeader：`ETag`
- MaxAgeSeconds：`300` 或以上

---

## 7. 常见错误与排查

- `AccessDenied: must be addressed using the specified endpoint`  
  说明 endpoint 地域配置错了。用报错里的 `Endpoint` 覆盖 `OSS_ENDPOINT` 和 `OSS_PUBLIC_URL`。

- `SslException` 且主机类似 `bucket.bucket.oss-...`  
  说明 `OSS_ENDPOINT` 错写成了 bucket 域名。改成 `https://oss-<region>.aliyuncs.com`。

- `InvalidAccessKeyId` / `SignatureDoesNotMatch`  
  检查 `OSS_ACCESS_KEY`、`OSS_SECRET_KEY` 是否有空格或引号，建议重新复制粘贴。

- `PUT uploadUrl` 返回 403  
  检查 CORS、预签名是否过期、`Content-Type` 是否一致。

---

## 8. 客户端伪代码

```ts
// 1) presign
const presign = await api.post('/api/oss/presign', {
  filename: file.name,
  contentType: file.type,
  size: file.size,
})

// 2) upload to OSS
await fetch(presign.uploadUrl, {
  method: 'PUT',
  headers: { 'Content-Type': file.type },
  body: file,
})

// 3) save record
const fileRecord = await api.post('/api/files', {
  objectKey: presign.objectKey,
  url: presign.publicUrl,
  contentType: file.type,
  size: file.size,
})
```
