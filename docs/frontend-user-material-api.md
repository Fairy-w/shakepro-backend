# ShakePro 用户材料接口文档（App + Web 管理端）

- 版本：v1.3
- 更新时间：2026-04-19
- 模块：条码识别 + 用户材料库 + 管理端只读分析（有无标记模式）

## 1. 基础约定

- Base URL：`http://<host>:8080`
- 鉴权：`Authorization: Bearer <token>`
- 接口前缀：`/api`
- 响应包装：统一 `ApiResponse<T>`

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": "f7b1c9e2a4d3b6c1"
}
```

- `code = 0` 表示成功
- `traceId` 也会通过响应头 `X-Trace-Id` 返回

## 2. 核心数据结构

### 2.1 `BarcodeLookupResponse`

```json
{
  "barcode": "5010327755017",
  "materialId": 4,
  "name": "Hendrick's Gin",
  "brand": "Hendrick's",
  "categoryId": "spirit",
  "source": "scan",
  "imageUrl": "https://oss.example.com/uploads/materials/cocktaildb/gin.png",
  "imageUrlThumb": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/thumb",
  "imageUrlCard": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/card",
  "imageUrlDetail": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/detail",
  "hasItem": false,
  "tags": ["杜松子", "草本", "黄瓜"]
}
```

### 2.2 `UserMaterialResponse`

```json
{
  "id": 1001,
  "userId": 12,
  "barcode": "5010327755017",
  "materialId": 4,
  "name": "Hendrick's Gin",
  "brand": "Hendrick's",
  "categoryId": "spirit",
  "source": "scan",
  "imageUrl": "https://oss.example.com/uploads/materials/cocktaildb/gin.png",
  "imageUrlThumb": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/thumb",
  "imageUrlCard": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/card",
  "imageUrlDetail": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/detail",
  "hasItem": true,
  "tags": ["杜松子", "草本", "黄瓜"],
  "createdAt": "2026-04-18T10:12:30",
  "updatedAt": "2026-04-18T10:20:10"
}
```

### 2.3 `MaterialResponse`（基础材料列表）

```json
{
  "id": 4,
  "name": "金酒",
  "category": "基酒",
  "nameEn": "Gin",
  "imageUrl": "https://oss.example.com/uploads/materials/cocktaildb/gin.png",
  "imageUrlThumb": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/thumb",
  "imageUrlCard": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/card",
  "imageUrlDetail": "https://oss.example.com/uploads/materials/cocktaildb/gin.png?x-oss-process=style/detail",
  "source": "thecocktaildb",
  "sourceId": "gin"
}
```

## 3. App 端接口（可写）

### 3.1 获取基础材料列表（用于手动勾选）

- 方法：`GET`
- 路径：`/api/materials`
- Query：`keyword`（可选）
- 返回：`ApiResponse<MaterialResponse[]>`

说明：

- 图片字段统一返回原图与样式图：`imageUrl` / `imageUrlThumb` / `imageUrlCard` / `imageUrlDetail`

### 3.2 获取基础材料分类

- 方法：`GET`
- 路径：`/api/materials/categories`
- 返回：`ApiResponse<string[]>`

### 3.3 条码识别（不落库）

- 方法：`POST`
- 路径：`/api/barcodes/lookup`

请求：

```json
{
  "barcode": "5010327755017",
  "locale": "zh-CN"
}
```

返回：`ApiResponse<BarcodeLookupResponse>`

### 3.4 保存/更新用户材料（扫码 upsert）

- 方法：`POST`
- 路径：`/api/user-materials`
- 返回：`ApiResponse<UserMaterialResponse>`

规则：

- 唯一键：`(user_id, barcode)`，重复提交会更新
- `barcode` 入库前标准化为仅数字
- `materialId` 优先级：请求值 > 别名匹配 > 旧值
- 默认值：`source=scan`，新记录 `hasItem=true`

### 3.5 手动新增/更新用户材料（从基础材料选择）

- 方法：`POST`
- 路径：`/api/user-materials/manual`

请求示例：

```json
{
  "materialId": 4,
  "source": "manual",
  "brand": "Hendrick's",
  "categoryId": "spirit",
  "hasItem": true,
  "tags": ["杜松子", "草本"]
}
```

返回：`ApiResponse<UserMaterialResponse>`

规则：

- 以 `(user_id, material_id)` 语义 upsert（命中已存在同材料记录时更新）
- 手动新增时，后端自动生成内部条码占位值用于兼容删除接口
- `name` 默认取基础材料 `materials.name`
- 默认值：`source=manual`，新记录 `hasItem=true`

### 3.6 批量手动新增/更新用户材料

- 方法：`POST`
- 路径：`/api/user-materials/manual/batch`

请求：

```json
{
  "items": [
    { "materialId": 4, "hasItem": true },
    { "materialId": 10, "hasItem": false }
  ]
}
```

返回：`ApiResponse<UserMaterialResponse[]>`

### 3.7 获取当前用户材料列表

- 方法：`GET`
- 路径：`/api/user-materials`
- Query：
  - `keyword`（可选）：按 `name` 模糊匹配
  - `categoryId`（可选）：按分类过滤
- 返回：`ApiResponse<UserMaterialResponse[]>`
- 排序：按 `updatedAt` 倒序

### 3.8 删除用户材料（按条码）

- 方法：`DELETE`
- 路径：`/api/user-materials/{barcode}`
- 返回：`ApiResponse<Void>`

## 4. Web 管理端接口（只读）

### 4.1 查询指定用户材料列表（管理员）

- 方法：`GET`
- 路径：`/api/admin/user-materials`
- Query：
  - `userId`（必填）：目标用户 ID
  - `keyword`（可选）：按名称模糊匹配
  - `categoryId`（可选）：按分类过滤
- 返回：`ApiResponse<UserMaterialResponse[]>`
- 权限：`ROLE_ADMIN`

说明：

- Web 管理端用于画像分析与推荐辅助，**仅查看，不写入**。
- `/api/admin/user-materials` 的写接口（新增/编辑/删除）已下线。
- 若 `userId` 不存在，返回 `40400`。

## 5. 临时停用字段（兼容期）

以下字段为历史库存能力字段，当前版本已停用：

- `capacityText`
- `remainLevel`
- `opened`

兼容策略：

- 请求体中可继续透传上述字段（旧端兼容），后端会忽略。
- 响应体中上述字段固定返回 `null`，请前端不要依赖。

## 6. 常见错误码

- `40001`：参数错误（条码格式等）
- `40100`：未登录或 Token 无效
- `40300`：无权限（非管理员访问后台接口）
- `40400`：资源不存在
- `40410`：未识别到商品信息
- `50030`：条码识别服务异常
- `50000`：服务端异常

## 7. 前后端边界

以下字段不再由后端返回，前端本地维护：

- `sourceLabel`
- `subtitle`
- `note`
- `badge`
- `accentColor`
- `softColor`
