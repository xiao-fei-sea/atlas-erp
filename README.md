# ERP 企业管理系统

模块化单体的企业级 ERP，覆盖系统管理、基础资料、库存、采购、销售、财务、审批流、CRM 与报表。

- 后端：Java 21 + Spring Boot 3.3.5 + Spring Security 6 + MyBatis-Plus 3.5.7 + MySQL 8 + Flyway
- 前端：Vue 3 + JavaScript（无 TypeScript）+ Vite 6 + Element Plus + Pinia + Vue Router + Axios + ECharts 5
- 数据库：42 张表，Flyway 自动迁移 + 初始化数据

## 目录结构

```
backend/            多模块 Maven 工程
  erp-common        统一响应/异常/审计/金额工具/MyBatis-Plus 配置
  erp-security      JWT、Spring Security、登录
  erp-system        用户/角色/菜单/部门/字典/参数/日志/序列/幂等
  erp-masterdata    商品/分类/单位/客户/供应商/仓库
  erp-inventory     库存余额/流水/调拨/盘点/补货
  erp-purchase      采购订单/收货入库
  erp-sales         销售订单/发货出库
  erp-finance       应收/应付/收款/付款/核销/资金账户
  erp-workflow      流程定义/审批实例/待办/审批回调事件
  erp-crm           线索/商机/跟进
  erp-report        仪表盘与销售/采购/库存报表、CSV 导出
  erp-app           启动模块、Flyway 迁移脚本、application.yml
frontend/           Vue 3 前端工程
docs/               需求、架构、数据库、API、页面、计划与项目状态
```

## 本地运行

前置：JDK 21、Maven 3.9+、Node 20+、MySQL 8。默认连接 `127.0.0.1:3307/erp`，账号 `erp / Erp@123456`，可通过环境变量 `DB_URL / DB_USER / DB_PASSWORD` 覆盖。

```bash
# 后端
cd backend
mvn -DskipTests package
java -jar erp-app/target/erp-app-1.0.0.jar

# 前端
cd frontend
npm install
npm run dev          # http://localhost:5173 ，/api 代理到 8080
```

默认账号：`admin / Admin@123`。其他演示账号（密码相同）：`sales01`、`salesmgr`、`purchase01`、`purchasemgr`、`wh01`、`fin01`。

## Docker Compose

```bash
docker compose up -d --build
# 前端 http://localhost ，后端 http://localhost:8080 ，MySQL localhost:3306
```

首次启动由 Flyway 自动建表并写入初始化数据。

## 测试

```bash
# 后端：单元 + 集成 + 权限 + 并发 + 金额 + 幂等（20 个用例）
cd backend && mvn test

# 前端：静态检查、生产构建、端到端
cd frontend && npm run lint && npm run build && npm run e2e
```

E2E 使用 Playwright + 本机 Microsoft Edge，无需额外下载浏览器；需先启动后端与前端 dev server（或由 Playwright 自动拉起 `npm run dev`）。

## 关键实现

- 认证：JWT（HS384）+ Spring Security 6，接口级 `@PreAuthorize` 权限码校验，菜单与按钮权限由角色动态下发。
- 审批：`wf_process → wf_node（角色/用户 + 金额区间）→ wf_instance → wf_task/wf_history`，审批结果通过 Spring 事件在同事务内回写业务单据。
- 库存：`SELECT ... FOR UPDATE` 行锁 + 条件更新状态机，入库按加权平均成本核算，禁止负库存。
- 金额：全链路 `BigDecimal`，数量/单价精度 4 位、展示金额 2 位，统一 `HALF_UP`。
- 幂等：`sys_idempotency` 唯一键控制重复提交。

## 文档

- [需求说明](docs/01-requirements.md)
- [架构设计](docs/02-architecture.md)
- [数据库设计](docs/03-database.md)
- [API 清单](docs/04-api.md)
- [页面清单](docs/05-pages.md)
- [实施计划](docs/06-plan.md)
- [部署说明](docs/07-deployment.md)
- [项目状态](docs/PROJECT_STATUS.md)
