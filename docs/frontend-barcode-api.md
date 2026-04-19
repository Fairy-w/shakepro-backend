# ShakePro 条码扫描与用户材料接口文档（App 端）

更新时间：2026-04-19（图片字段已补齐）  
适用场景：App 前端扫码识别 + 维护当前登录用户材料库

## 1. 基础说明

- Base URL：`http://<host>:8080`
- 鉴权：`Authorization: Bearer <token>`
- 响应体：统一 `ApiResponse<T>`

## 2. 核心接口

### 2.1 条码识别（不落库）

`POST /api/barcodes/lookup`

请求体：

```json
{
  "barcode": "5010327755017",
  "locale": "zh-CN"
}
```

成功响应（`data`）示例：

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

说明：

- 若当前用户已保存过同条码，返回该用户当前“有无标记”状态（`hasItem`、`tags`）
- 识别不到条码时返回业务码 `40410`

### 2.2 保存/更新用户材料（扫码 upsert）

`POST /api/user-materials`

请求体示例：

```json
{
  "barcode": "5010327755017",
  "materialId": 4,
  "name": "Hendrick's Gin",
  "brand": "Hendrick's",
  "categoryId": "spirit",
  "source": "scan",
  "hasItem": true,
  "tags": ["杜松子", "草本", "黄瓜"]
}
```

入库规则：

- 唯一键：`(user_id, barcode)`，重复提交为更新
- `barcode` 入库前会标准化为仅数字
- 后端可自动进行别名匹配并尝试回填 `materialId`

### 2.3 手动新增/更新用户材料（从基础材料勾选）

`POST /api/user-materials/manual`

请求体示例：

```json
{
  "materialId": 4,
  "source": "manual",
  "hasItem": true,
  "tags": ["杜松子", "草本"]
}
```

### 2.4 批量手动新增/更新用户材料

`POST /api/user-materials/manual/batch`

请求体示例：

```json
{
  "items": [
    { "materialId": 4, "hasItem": true },
    { "materialId": 10, "hasItem": false }
  ]
}
```

### 2.5 获取当前用户材料列表

`GET /api/user-materials`

Query 参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| keyword | 否 | 按 `name` 模糊搜索 |
| categoryId | 否 | 按分类筛选（如 `spirit`） |

### 2.6 删除当前用户材料（按条码）

`DELETE /api/user-materials/{barcode}`

说明：

- path 参数 `barcode` 支持原始扫码值，后端会自动标准化
- 删除成功返回 `code=0`

## 3. 基础材料列表接口（手动勾选依赖）

### 3.1 获取基础材料列表

`GET /api/materials`

Query 参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| keyword | 否 | 按材料名称搜索 |

返回（`data`）示例：

```json
[
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
]
```

### 3.2 获取基础材料分类

`GET /api/materials/categories`

返回（`data`）示例：

```json
["基酒", "利口酒", "果汁", "软饮", "水果", "辅料"]
```

## 4. 字段定义

### 4.1 `BarcodeLookupResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| barcode | string | 标准化条码（仅数字） |
| materialId | long/null | 匹配到的标准材料ID |
| name | string | 材料名称 |
| brand | string/null | 品牌 |
| categoryId | string/null | 分类ID |
| source | string | 来源，`scan`/`manual` |
| imageUrl | string/null | 材料原图 URL（优先 OSS） |
| imageUrlThumb | string/null | 缩略图 URL（OSS style: thumb） |
| imageUrlCard | string/null | 卡片图 URL（OSS style: card） |
| imageUrlDetail | string/null | 详情图 URL（OSS style: detail） |
| hasItem | boolean | 是否已在用户材料库 |
| tags | string[] | 标签 |

### 4.2 `UserMaterialResponse`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 用户材料记录ID |
| userId | long | 用户ID |
| barcode | string | 标准化条码（仅数字） |
| materialId | long/null | 标准材料ID |
| name | string | 材料名称 |
| brand | string/null | 品牌 |
| categoryId | string/null | 分类ID |
| source | string | 来源，`scan`/`manual` |
| imageUrl | string/null | 材料原图 URL（优先 OSS） |
| imageUrlThumb | string/null | 缩略图 URL（OSS style: thumb） |
| imageUrlCard | string/null | 卡片图 URL（OSS style: card） |
| imageUrlDetail | string/null | 详情图 URL（OSS style: detail） |
| hasItem | boolean | 是否持有 |
| tags | string[] | 标签 |
| createdAt | string | 创建时间（ISO-8601） |
| updatedAt | string | 更新时间（ISO-8601） |

## 5. 兼容期说明（库存字段停用）

以下历史字段已停用：

- `capacityText`
- `remainLevel`
- `opened`

兼容策略：

- 请求体可以保留这些字段，后端会忽略。
- 响应体中这些字段固定为 `null`，请不要在前端依赖。

## 6. Web 管理端边界（避免误用）

- Web 管理端只保留：`GET /api/admin/user-materials?userId=...`
- 管理端新增/编辑/删除用户材料接口已下线，不要在 Web 端调用写接口。
- 用户材料写入统一走 App 端 `/api/user-materials/**`。

## 7. 前后端边界

以下字段不再由后端返回，前端本地规则维护：

- `sourceLabel`
- `subtitle`
- `note`
- `badge`
- `accentColor`
- `softColor`

## 8. 常见错误码

- `40001`：参数错误（如条码不合法）
- `40100`：未登录或 Token 无效
- `40300`：无权限
- `40400`：资源不存在
- `40410`：未识别到商品信息
- `50030`：条码识别服务异常
