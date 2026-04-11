# ShakePro Backend

ShakePro 鸡尾酒应用后端服务，提供用户认证、鸡尾酒与材料查询、收藏、文件上传预签名、AI 推荐，以及后台管理能力。

## 项目概览

- 项目名称：`shakepro-backend`
- 当前版本：`1.0.0-SNAPSHOT`
- 默认端口：`8080`
- 默认运行 Profile：`dev`
- 接口文档：`http://localhost:8080/swagger-ui.html`
- 健康检查：`http://localhost:8080/actuator/health`

## 技术栈（按当前代码）

- Java 25
- Spring Boot 3.5.11
- Spring Security + JWT（jjwt 0.12.5）
- Spring Data JPA（Hibernate）
- MySQL 8.4.8（Docker）
- Redis 8.6（Docker）
- MinIO（对象存储，Docker）
- Flyway（数据库迁移）
- SpringDoc OpenAPI 2.8.16（Swagger UI）

## 核心能力

- 用户体系：注册、登录、获取当前用户
- 后台体系：管理员登录、仪表盘、用户管理、材料管理、鸡尾酒管理
- 鸡尾酒：分页查询、详情、轮播、分类
- 材料：列表与分类
- 收藏：新增、取消、列表
- 文件上传：MinIO 预签名 URL + 文件记录落库
- AI 推荐：支持 `mock` / 外部 AI Provider（预留推荐算法）
- AI 配方生成：调用通义千问（DashScope）
- 可观测性：Actuator 健康检查、全链路 `traceId`

## 目录结构

```text
src/main/java/com/shakepro
├── common
│   ├── exception      # 业务异常、全局异常处理
│   ├── result         # 统一响应体与错误码
│   └── util           # JWT、TraceId 等工具
├── config             # 安全、跨域、Redis、MinIO、OpenAPI、AI 配置
├── controller         # REST 接口层
├── dto                # 请求/响应 DTO
├── entity             # JPA 实体
├── repository         # 数据访问层
└── service            # 业务服务与实现

src/main/resources
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── db/migration       # Flyway SQL（V1~V4）
```

## 快速启动（本地）

### 1) 启动依赖服务

```bash
docker compose up -d
```

默认会启动：

- MySQL：`localhost:3307`
- Redis：`localhost:6379`
- MinIO API：`localhost:9000`
- MinIO Console：`localhost:9001`

`docker-compose.yml` 中包含 `minio-init`，会自动创建 `shakepro` bucket 并设置公开读。

### 2) 启动后端

```bash
mvn spring-boot:run
```

或显式指定开发环境：

```bash
mvn -Dspring-boot.run.profiles=dev spring-boot:run
```

### 3) 验证

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 健康检查：<http://localhost:8080/actuator/health>

## 生产运行

### 打包

```bash
mvn clean package -DskipTests
```

### 启动

```bash
java -jar target/shakepro-backend-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

> 生产环境请务必提供完整环境变量（见下文）。

## 默认账号（数据库迁移种子）

来自 `V2__seed.sql` 与 `V4__reset_admin_password.sql`：

- 普通用户：`testuser / test123456`
- 管理员：`admin / admin123456`

## 鉴权与权限模型

- 认证方式：`Authorization: Bearer <token>`
- Token 在登录接口返回，过期时间由 `jwt.expiration-seconds` 控制
- 权限规则：
  - 放行：`/api/auth/**`、`/api/admin/auth/login`、`/api/app/config/**`、Swagger、Actuator
  - `ROLE_ADMIN`：`/api/admin/**`（除登录接口）
  - 其他接口：均需登录

## 统一响应格式

所有接口返回 `ApiResponse<T>`：

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": "f7b1c9e2a4d3b6c1"
}
```

- `traceId` 同时会写入响应头：`X-Trace-Id`
- 成功码：`0`
- 常见业务码：
  - `40001` 参数错误
  - `40002` 用户名已存在
  - `40003` 用户名或密码错误
  - `40004` 文件类型不允许
  - `40005` 文件大小超限
  - `40100` 未登录或 Token 无效
  - `40300` 无权限
  - `40400` 资源不存在
  - `50000` 服务端异常
  - `50010` AI 调用失败
  - `50020` OSS 预签名失败

## API 清单（按当前 Controller）

### 1) 应用配置（免登录）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/app/config/auth` | 否 | 获取登录/注册页配置 |

### 2) 用户认证

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 否 | 用户注册 |
| POST | `/api/auth/login` | 否 | 用户登录 |
| GET | `/api/me` | 是 | 获取当前用户信息 |

`register` 参数校验：

- `username`: 3-50 字符
- `password`: 6-50 字符
- `nickname`: 最长 50 字符

### 3) 后台认证与管理

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/admin/auth/login` | 否 | 管理员登录 |
| GET | `/api/admin/auth/me` | 管理员 | 获取当前管理员信息 |
| GET | `/api/admin/dashboard` | 管理员 | 仪表盘统计 |
| GET | `/api/admin/users` | 管理员 | 用户分页列表 |
| GET | `/api/admin/materials` | 管理员 | 材料列表 |
| POST | `/api/admin/materials` | 管理员 | 新增材料 |
| PUT | `/api/admin/materials/{id}` | 管理员 | 修改材料 |
| DELETE | `/api/admin/materials/{id}` | 管理员 | 删除材料 |
| GET | `/api/admin/cocktails` | 管理员 | 鸡尾酒分页列表 |
| GET | `/api/admin/cocktails/{id}` | 管理员 | 鸡尾酒详情 |
| POST | `/api/admin/cocktails` | 管理员 | 新增鸡尾酒 |
| PUT | `/api/admin/cocktails/{id}` | 管理员 | 修改鸡尾酒 |
| DELETE | `/api/admin/cocktails/{id}` | 管理员 | 删除鸡尾酒 |

分页参数：`page` 从 `0` 开始，默认 `size=10`。

### 4) 鸡尾酒与材料

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/cocktails` | 是 | 鸡尾酒分页列表（支持 `keyword`） |
| GET | `/api/cocktails/{id}` | 是 | 鸡尾酒详情 |
| GET | `/api/cocktails/banner` | 是 | 轮播数据（Redis 缓存 300s） |
| GET | `/api/cocktails/categories` | 是 | 鸡尾酒分类（固定枚举） |
| GET | `/api/materials` | 是 | 材料列表（支持 `keyword`） |
| GET | `/api/materials/categories` | 是 | 材料分类列表 |

### 5) 收藏

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/favorites/{cocktailId}` | 是 | 收藏（幂等） |
| DELETE | `/api/favorites/{cocktailId}` | 是 | 取消收藏 |
| GET | `/api/favorites` | 是 | 我的收藏列表 |
| POST | `/api/favorites/ai-cocktails` | 是 | 收藏 AI 生成的单个配方（幂等） |
| DELETE | `/api/favorites/ai-cocktails/{favoriteId}` | 是 | 按收藏 ID 取消 AI 配方收藏 |
| DELETE | `/api/favorites/ai-cocktails?recipeKey=xxx` | 是 | 按 recipeKey 取消当前用户的 AI 配方收藏 |
| GET | `/api/favorites/ai-cocktails` | 是 | 我的 AI 配方收藏列表（支持 `pageNo` `pageSize` `keyword` `sort`） |
| GET | `/api/favorites/ai-cocktails/status?recipeKey=xxx` | 是 | 查询当前 AI 配方是否已收藏 |

### 6) 文件上传（OSS）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/oss/presign` | 是 | 获取 MinIO 预签名上传 URL |
| POST | `/api/files` | 是 | 保存文件记录 |

上传约束：

- 允许类型：`image/jpeg` `image/png` `image/gif` `image/webp` `image/svg+xml`
- 大小限制：最大 `10MB`

### 7) AI 推荐

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/ai/recommend` | 是 | 根据材料推荐鸡尾酒 |
| POST | `/api/ai/generate-recipe` | 是 | 使用通义千问生成鸡尾酒配方 |
| POST | `/api/ai/generate-recipe-by-text` | 是 | 根据自然语言描述生成鸡尾酒配方 |

- `ai.provider=mock` 时返回内置模拟数据
- 非 `mock` 时 `/api/ai/recommend` 调用 `${ai.base-url}/chat/completions`
- `/api/ai/generate-recipe` 调用 `${ai.qwen.base-url}/services/aigc/text-generation/generation`
- `/api/ai/generate-recipe-by-text` 调用 `${ai.qwen.base-url}/services/aigc/text-generation/generation`
- 配方生成接口返回的 `steps` 为数组类型（`string[]`）

## 文件上传调用流程（前端建议）

1. 调用 `POST /api/oss/presign` 获取 `uploadUrl`、`objectKey`、`publicUrl`
2. 前端直接 `PUT uploadUrl` 上传文件到 MinIO
3. 调用 `POST /api/files` 持久化文件信息（`objectKey`、`url` 等）

## 环境变量说明

### 通用

| 变量名 | 说明 |
| --- | --- |
| `MYSQL_HOST` | MySQL 地址 |
| `MYSQL_PORT` | MySQL 端口 |
| `MYSQL_USER` | MySQL 用户 |
| `MYSQL_PASSWORD` | MySQL 密码 |
| `REDIS_HOST` | Redis 地址 |
| `REDIS_PORT` | Redis 端口 |
| `REDIS_PASSWORD` | Redis 密码（可空） |
| `JWT_SECRET` | JWT 密钥（建议 32+ 字节） |
| `AI_PROVIDER` | AI 提供商（`mock`/`openai` 等） |
| `AI_BASE_URL` | AI 网关地址 |
| `AI_API_KEY` | AI Key |
| `AI_MODEL` | AI 模型名 |
| `AI_TIMEOUT` | AI 超时（毫秒） |
| `QWEN_BASE_URL` | DashScope 网关地址 |
| `QWEN_API_KEY` | DashScope Key |
| `QWEN_MODEL` | 通义千问模型名 |
| `QWEN_TIMEOUT` | DashScope 超时（毫秒） |

### `dev` 默认值（application-dev.yml）

| 配置项 | 默认值 |
| --- | --- |
| `MYSQL_HOST` | `localhost` |
| `MYSQL_PORT` | `3307` |
| `MYSQL_USER` | `root` |
| `MYSQL_PASSWORD` | `root123456` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `JWT_SECRET` | `ShakeProDevSecretKeyForJWT2024MustBeAtLeast256BitsLong!!` |
| `AI_PROVIDER` | `mock` |
| `AI_BASE_URL` | `https://api.openai.com/v1` |
| `AI_MODEL` | `gpt-3.5-turbo` |
| `AI_TIMEOUT` | `15000` |
| `QWEN_BASE_URL` | `https://dashscope.aliyuncs.com/api/v1` |
| `QWEN_MODEL` | `qwen-plus` |
| `QWEN_TIMEOUT` | `15000` |

`dev` 下 OSS 使用以下环境变量：

- `MINIO_ENDPOINT`（默认 `http://localhost:9000`）
- `MINIO_ACCESS_KEY`（默认 `minioadmin`）
- `MINIO_SECRET_KEY`（默认 `minioadmin`）
- `MINIO_BUCKET`（默认 `shakepro`）
- `MINIO_PUBLIC_URL`（默认 `http://localhost:9000/shakepro`）

### `prod` 必填项（application-prod.yml）

`prod` 下 OSS 环境变量命名为：

- `OSS_TYPE`（默认 `minio`）
- `OSS_ENDPOINT`
- `OSS_ACCESS_KEY`
- `OSS_SECRET_KEY`
- `OSS_BUCKET`
- `OSS_PUBLIC_URL`

此外建议同时设置：

- `JWT_EXPIRATION`（默认 `86400` 秒）
- `REDIS_PASSWORD`

## 数据库迁移

Flyway 自动执行：

- `V1__init.sql`：基础表结构
- `V2__seed.sql`：初始测试数据（用户/鸡尾酒/材料）
- `V3__admin_support.sql`：管理员角色字段与初始 admin
- `V4__reset_admin_password.sql`：重置 admin 密码与状态

## 开发调试建议

### 1) 登录并调用受保护接口

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123456"}'
```

拿到 `token` 后：

```bash
curl "http://localhost:8080/api/cocktails?page=0&size=10" \
  -H "Authorization: Bearer <your_token>"
```

### 2) 查看 API 文档

Swagger UI：<http://localhost:8080/swagger-ui.html>

### 3) 执行测试

```bash
mvn test
```

（当前仓库测试用例较少，建议后续补充集成测试）

## 已知实现细节

- CORS 当前为宽松策略（`*` + AllowCredentials），上线前建议按域名收敛
- `/api/cocktails/**` 与 `/api/materials/**` 当前需要登录访问
- `banner` 缓存 Key：`cache:cocktail:banner`，TTL 为 300 秒

---

如果你希望，我可以在下一步再补一份“前端联调版 README”（增加更多请求/响应示例 JSON 与 Postman 导入说明）。
