# ShakePro Backend

ShakePro 鸡尾酒应用后端服务 - Spring Boot 3.x

## 技术栈

- Java 17 + Spring Boot 3.2
- Spring Data JPA + MySQL 8
- Spring Security + JWT
- Redis 7 缓存
- MinIO 对象存储
- Flyway 数据库迁移
- Swagger/OpenAPI 文档

## 快速启动

### 1. 启动依赖服务

```bash
docker-compose up -d
```

等待所有服务健康运行（MySQL: 3306, Redis: 6379, MinIO: 9000/9001）。

### 2. 启动后端

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

或在 IDE 中运行 `ShakeProApplication`，设置 active profile 为 `dev`。

### 3. 验证

- 健康检查: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin)

## 测试账号

| 用户名 | 密码 |
|--------|------|
| testuser | test123456 |

## API 概览

| 模块 | 接口 | 说明 |
|------|------|------|
| Auth | POST /api/auth/register | 用户注册 |
| Auth | POST /api/auth/login | 用户登录 |
| Auth | GET /api/me | 当前用户信息 |
| Cocktails | GET /api/cocktails | 鸡尾酒列表（分页） |
| Cocktails | GET /api/cocktails/{id} | 鸡尾酒详情 |
| Cocktails | GET /api/cocktails/banner | 轮播图数据 |
| Cocktails | GET /api/cocktails/categories | 分类列表 |
| Favorites | POST /api/favorites/{cocktailId} | 收藏 |
| Favorites | DELETE /api/favorites/{cocktailId} | 取消收藏 |
| Favorites | GET /api/favorites | 收藏列表 |
| Materials | GET /api/materials | 材料列表 |
| Materials | GET /api/materials/categories | 材料分类 |
| OSS | POST /api/oss/presign | 获取预签名上传URL |
| Files | POST /api/files | 保存文件记录 |
| AI | POST /api/ai/recommend | AI推荐鸡尾酒 |

## 配置说明

所有敏感配置均支持环境变量覆盖：

| 环境变量 | 说明 | 默认值(dev) |
|----------|------|-------------|
| MYSQL_HOST | MySQL地址 | localhost |
| MYSQL_PORT | MySQL端口 | 3306 |
| MYSQL_USER | MySQL用户 | root |
| MYSQL_PASSWORD | MySQL密码 | root123456 |
| REDIS_HOST | Redis地址 | localhost |
| REDIS_PORT | Redis端口 | 6379 |
| JWT_SECRET | JWT密钥 | (内置开发密钥) |
| MINIO_ENDPOINT | MinIO地址 | http://localhost:9000 |
| AI_PROVIDER | AI供应商 | mock |
| AI_API_KEY | AI API Key | (仅后端持有) |

## 项目结构

```
com.shakepro
├── ShakeProApplication.java
├── controller/          # REST 控制器
├── dto/
│   ├── request/         # 请求 DTO
│   └── response/        # 响应 DTO
├── service/
│   └── impl/            # 服务实现
├── repository/          # JPA Repository
├── entity/              # JPA 实体
├── config/
│   └── security/        # 安全相关配置
├── common/
│   ├── result/          # 统一返回 ApiResponse
│   ├── exception/       # 异常处理
│   └── util/            # 工具类
└── resources/
    └── db/migration/    # Flyway SQL
```
