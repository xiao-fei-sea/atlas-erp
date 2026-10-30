# 部署说明

## 1. 运行环境

| 组件 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 21+ | 后端编译与运行 |
| Maven | 3.9+ | 仅构建阶段需要 |
| Node.js | 20+（推荐 24） | 仅前端构建阶段需要 |
| MySQL | 8.0 | 字符集 utf8mb4，时区 Asia/Shanghai |
| Docker | 24+（可选） | 容器化部署；本机守护进程未运行，容器方式未做运行验证 |

## 2. 配置项

后端 `backend/erp-app/src/main/resources/application.yml` 支持环境变量覆盖：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | 8080 | 后端端口 |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3307/erp?...` | 数据库连接 |
| `DB_USER` / `DB_PASSWORD` | `erp` / `Erp@123456` | 数据库账号 |
| `JWT_SECRET` | 内置开发密钥 | 生产必须替换 |
| `JWT_EXPIRE_MINUTES` | 480 | Token 有效期（分钟） |

前端开发环境通过 Vite `server.proxy` 将 `/api` 转发至 `http://127.0.0.1:8080`；生产环境由 Nginx 反向代理（见 `frontend/nginx.conf`）。

## 3. 本机原生部署

```bash
# 1) 数据库：创建库与账号（Flyway 负责建表/初始化，无需手工执行 SQL）
CREATE DATABASE erp DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'erp'@'%' IDENTIFIED BY 'Erp@123456';
GRANT ALL PRIVILEGES ON erp.* TO 'erp'@'%';
FLUSH PRIVILEGES;

# 2) 后端
cd backend
mvn -B -DskipTests package
java -jar erp-app/target/erp-app-1.0.0.jar

# 3) 前端
cd frontend
npm ci
npm run build
npx vite preview --host --port 4173   # 或部署 dist/ 到 Nginx
```

## 4. Docker Compose 部署

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

服务：`mysql:3306`、`backend:8080`、`frontend:80`。数据持久化在命名卷 `erp-mysql-data`。

> 注意：本机 Docker CLI 已安装但守护进程未启动，`docker compose` 未实际运行验证；配置文件已按标准写法提供，启动 Docker Desktop 后可直接使用。

## 5. 部署校验清单

1. 访问前端首页 → 跳转 `/login`。
2. 使用 `admin / Admin@123` 登录 → 进入仪表盘，统计卡片与图表有数据。
3. 打开"系统管理 → 用户管理"、`基础资料 → 商品管理`，列表返回真实数据库数据。
4. 使用 `sales01 / Admin@123` 访问 `/api/system/users` 应返回 403。
5. 检查日志：Flyway 打印 `Successfully validated 3 migrations`，无异常堆栈。
