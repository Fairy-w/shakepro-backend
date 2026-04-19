# ShakePro 鸡尾酒库前端接口文档

更新时间：2026-04-18  
适用环境：`/api/cocktails` 前端列表页、详情页、Banner、分类渲染

## 1. 基础信息

- Base URL：`http://<host>:8080`
- 数据格式：`application/json`
- 鉴权方式：`Authorization: Bearer <token>`
- 说明：当前除 `/api/auth/**` 外，鸡尾酒相关接口都需要登录态

## 2. 获取 Token（前端首次登录）

### `POST /api/auth/login`

请求体：

```json
{
  "username": "testuser",
  "password": "test123456"
}
```

成功响应（`data`）：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9....",
  "tokenType": "Bearer",
  "expireSeconds": 86400,
  "user": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户"
  }
}
```

## 3. 通用响应结构

所有接口统一返回：

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": "f7b1c9e2a4d3b6c1"
}
```

- `code = 0` 表示成功
- `traceId` 便于后端排查（同时会在响应头 `X-Trace-Id` 返回）

## 4. 鸡尾酒库接口

### 4.1 鸡尾酒分页列表

### `GET /api/cocktails`

Query 参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| keyword | string | 否 | - | 按酒名模糊搜索（忽略大小写） |
| page | int | 否 | 0 | 页码，从 0 开始 |
| size | int | 否 | 10 | 每页条数 |

请求示例：

```bash
curl "http://localhost:8080/api/cocktails?page=0&size=10&keyword=mojito" \
  -H "Authorization: Bearer <token>"
```

成功响应（`data`）示例：

```json
{
  "content": [
    {
      "id": 1,
      "name": "莫吉托 Mojito",
      "englishName": "Mojito",
      "category": "经典鸡尾酒",
      "heroImage": "https://cdn.xxx/hero/mojito.jpg",
      "difficulty": "简单",
      "abv": "15%",
      "imageUrl": "https://cdn.xxx/hero/mojito.jpg",
      "imageUrlThumb": "https://cdn.xxx/hero/mojito.jpg?x-oss-process=style/thumb",
      "imageUrlCard": "https://cdn.xxx/hero/mojito.jpg?x-oss-process=style/card",
      "description": "经典古巴鸡尾酒...",
      "alcoholLevel": 15
    }
  ],
  "totalElements": 23,
  "totalPages": 3,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false,
  "numberOfElements": 10,
  "empty": false
}
```

字段说明（`content[]`）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | long | 鸡尾酒 ID |
| name | string | 中文名（可能带英文） |
| englishName | string/null | 英文名 |
| category | string/null | 分类 |
| heroImage | string/null | 主图 |
| difficulty | string/null | 难度，如“简单/中等” |
| abv | string/null | 展示酒精度，如 `15%` |
| imageUrl | string/null | 推荐渲染图（优先 `heroImage`，否则旧字段 `imageUrl`） |
| imageUrlThumb | string/null | 列表缩略图 URL（优先返回 OSS `thumb` 样式） |
| imageUrlCard | string/null | 卡片图 URL（优先返回 OSS `card` 样式） |
| description | string/null | 简介 |
| alcoholLevel | int/null | 数值酒精度（0-100） |

---

### 4.2 鸡尾酒详情

### `GET /api/cocktails/{id}`

Path 参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | long | 是 | 鸡尾酒 ID |

请求示例：

```bash
curl "http://localhost:8080/api/cocktails/1" \
  -H "Authorization: Bearer <token>"
```

成功响应（`data`）示例：

```json
{
  "id": 1,
  "name": "莫吉托 Mojito",
  "englishName": "Mojito",
  "category": "经典鸡尾酒",
  "heroImage": "https://cdn.xxx/hero/mojito.jpg",
  "difficulty": "简单",
  "abv": "15%",
  "glass": "高球杯",
  "garnish": "薄荷枝 + 青柠角",
  "highlight": "清爽、草本、夏日",
  "subtitle": "古巴代表作",
  "description": "经典古巴鸡尾酒...",
  "story": "起源于哈瓦那...",
  "imageUrl": "https://cdn.xxx/hero/mojito.jpg",
  "heroImageCard": "https://cdn.xxx/hero/mojito.jpg?x-oss-process=style/card",
  "heroImageDetail": "https://cdn.xxx/hero/mojito.jpg?x-oss-process=style/detail",
  "alcoholLevel": 15,
  "legacySteps": "1. ...\\n2. ...",
  "flavorTags": ["清爽", "酸甜", "草本"],
  "flavorMetrics": [
    { "sortOrder": 1, "name": "甜感", "value": 2 },
    { "sortOrder": 2, "name": "酸感", "value": 4 }
  ],
  "pairings": ["海鲜", "烤鸡"],
  "serviceNotes": ["建议加满碎冰", "现打薄荷更佳"],
  "steps": [
    { "order": 1, "title": "预处理", "detail": "捣压薄荷与糖浆" },
    { "order": 2, "title": "混合", "detail": "加入朗姆和青柠汁后搅拌" }
  ],
  "materials": [
    {
      "materialId": 11,
      "name": "白朗姆酒",
      "category": "spirit",
      "displayName": "白朗姆酒",
      "amount": "45ml",
      "note": "可用金朗姆替代",
      "sortOrder": 1
    }
  ],
  "createdAt": "2026-03-22T20:10:25"
}
```

说明：

- `steps` 是结构化步骤，优先用于前端展示
- `legacySteps` 是旧版纯文本步骤，可做兼容兜底
- `materials[].name` 已做兼容：优先材料库名称，否则回退 `displayName`
- `heroImageCard` 适合详情首屏展示；`heroImageDetail` 适合点开大图/高分屏展示

---

### 4.3 首页 Banner

### `GET /api/cocktails/banner`

说明：

- 返回最多 5 条（按创建时间倒序）
- 后端 Redis 缓存 300 秒

成功响应（`data`）示例：

```json
[
  {
    "cocktailId": 1,
    "imageUrl": "https://cdn.xxx/hero/mojito.jpg",
    "title": "莫吉托 Mojito"
  },
  {
    "cocktailId": 2,
    "imageUrl": "https://cdn.xxx/hero/margarita.jpg",
    "title": "玛格丽特 Margarita"
  }
]
```

---

### 4.4 鸡尾酒分类

### `GET /api/cocktails/categories`

当前为固定枚举，成功响应（`data`）示例：

```json
[
  { "name": "全部", "icon": "all" },
  { "name": "入门", "icon": "beginner" },
  { "name": "金酒", "icon": "gin" },
  { "name": "伏特加", "icon": "vodka" },
  { "name": "朗姆", "icon": "rum" },
  { "name": "龙舌兰", "icon": "tequila" },
  { "name": "威士忌", "icon": "whiskey" },
  { "name": "白兰地", "icon": "brandy" },
  { "name": "利口酒", "icon": "liqueur" },
  { "name": "无醇", "icon": "non_alcoholic" }
]
```

## 5. 常见错误码

| code | 含义 | 常见场景 |
| --- | --- | --- |
| 0 | OK | 成功 |
| 40001 | 参数错误 | 分页参数非法等 |
| 40100 | 未登录或 Token 无效 | 缺失/过期/错误 token |
| 40400 | 资源不存在 | 详情 ID 不存在 |
| 50000 | 服务端异常 | 未捕获异常 |

## 6. 前端接入建议

- 列表页优先用 `imageUrlThumb`，卡片位可用 `imageUrlCard`，缺失时回退 `imageUrl`
- 分页以 `data.content` + `data.totalElements` 为准
- 详情页主图优先 `heroImageCard`，查看高清图时使用 `heroImageDetail`，缺失时回退 `imageUrl` 或 `heroImage`
- 详情页优先渲染 `steps`，`legacySteps` 仅兜底
- 接口报错时记录并上报 `traceId`，方便后端快速排查
