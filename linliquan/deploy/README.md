# 邻里圈 - 测试环境部署指南

## 快速开始

### 一键部署

```bash
cd deploy
./deploy.sh
```

### 服务管理

```bash
# 查看状态
./manage.sh status

# 查看日志
./manage.sh logs backend

# 重启服务
./manage.sh restart

# 停止服务
./manage.sh stop

# 重新构建后端
./manage.sh rebuild

# 运行接口冒烟测试
./manage.sh test
```

## 服务清单

| 服务 | 端口 | 说明 |
|------|------|------|
| PostgreSQL+PostGIS | 5432 | 数据库，自动执行 init.sql |
| Redis | 6379 | 缓存服务，开启 AOF 持久化 |
| Spring Boot 后端 | 8080 | API 服务，上下文路径 /api |
| Nginx | 80 / 443 | 反向代理，静态资源服务 |

## API 地址

- 本地访问: `http://localhost:8080/api`
- Nginx 代理: `http://localhost/api`

## 目录结构

```
deploy/
├── docker-compose.yml      # Docker Compose 编排文件
├── Dockerfile.backend      # 后端镜像构建文件
├── deploy.sh               # 一键部署脚本
├── manage.sh               # 服务管理脚本
├── .env.example            # 环境变量模板
├── .env                    # 实际环境配置（自动生成）
├── nginx/
│   ├── nginx.conf          # Nginx 配置
│   └── ssl/                # SSL 证书目录（自行放置）
├── scripts/
│   └── smoke-test.sh       # 接口冒烟测试脚本
├── uploads/                # 文件上传目录
└── test-results/           # 测试报告输出目录
```

## 前端环境切换

前端支持 4 种环境，通过个人中心调试入口或代码切换：

```javascript
const envConfig = require('./utils/env.js');

// 切换到测试环境
envConfig.setCurrentEnv(envConfig.ENV_TEST);

// 切换到 Mock 环境（无需后端）
envConfig.setCurrentEnv(envConfig.ENV_MOCK);

// 查看当前环境
console.log(envConfig.getCurrentEnv());
```

| 环境 | 说明 | API 地址 |
|------|------|----------|
| mock | Mock 模式，纯前端模拟 | - |
| dev | 开发环境 | http://localhost:8080/api |
| test | 测试环境 | https://test-api.linliquan.com/api |
| prod | 生产环境 | https://api.linliquan.com/api |

## 后端接口集成测试

### 运行单元测试 + 集成测试

```bash
cd backend
mvn test
```

### 运行端到端接口集成测试

```bash
cd backend
mvn test -Dtest=ApiIntegrationE2ETest
```

测试覆盖范围：
- 认证模块（登录、Mock登录、认证申请、状态查询）
- 帖子模块（列表、详情、发布、点赞）
- 评论模块（列表、发表、删除、点赞）
- 互助任务模块（附近查询、发布、接单）
- 权限红线规则（三态权限矩阵、读写分离）
- 完整业务流程（登录→浏览→发布→点赞→评论）

### 接口冒烟测试（对已部署环境）

```bash
cd deploy
./scripts/smoke-test.sh

# 指定 API 地址
API_BASE_URL=https://test-api.linliquan.com/api ./scripts/smoke-test.sh
```

测试报告输出到 `deploy/test-results/` 目录。

## 常见问题

### 1. 端口被占用

修改 `.env` 文件中的端口配置：
```env
BACKEND_PORT=8081
DB_PORT=5433
REDIS_PORT=6380
```

### 2. 数据库初始化失败

PostGIS 镜像首次拉取较慢，等待 `start_period` 结束后重试：
```bash
./manage.sh restart postgres
```

### 3. 后端启动失败

查看日志排查：
```bash
./manage.sh logs backend
```

常见原因：
- 数据库连接失败 → 检查 DB 配置
- Redis 连接失败 → 检查 Redis 密码配置
- JWT 密钥问题 → 检查 JWT_SECRET 配置

### 4. 清理所有数据重新部署

```bash
./manage.sh clean
./deploy.sh
```

## 测试账号

数据库初始化后内置测试用户（手机号哈希值）：

| 用户 | 认证状态 | phone_hash |
|------|---------|------------|
| 未认证用户 | UNAUTH | a665a45920422f9d417e4867efdc4fb8 |
| 认证中用户 | PENDING | a665a45920422f9d417e4867efdc4fb9 |
| 已认证业主A | VERIFIED | a665a45920422f9d417e4867efdc4fb0 |
| 已认证业主B | VERIFIED | a665a45920422f9d417e4867efdc4fb1 |

也可以使用 `/v1/auth/login-mock?statusType=0|1|2` 接口快速获取不同状态的 Mock Token。
