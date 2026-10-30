# 贡献指南

> Thanks for your interest in Atlas ERP. This guide covers local setup, branch and commit conventions, code style and the checks we expect before a pull request. 以下为中文说明。

## 开发环境

- JDK 21、Maven 3.9+、Node 20+、MySQL 8
- 后端默认连接 `127.0.0.1:3307/erp`，可通过 `DB_URL / DB_USER / DB_PASSWORD` 覆盖
- 前端 `npm install` 后执行 `npm run dev`，`/api` 已代理到 `8080`

## 分支与提交

- `main` 保持可运行，功能开发从 `main` 拉出分支：`feat/<模块>-<简述>`、`fix/<模块>-<简述>`、`docs/<简述>`
- 提交信息使用 `<type>(<scope>): <描述>`，`type` 取 `feat / fix / docs / refactor / test / chore`
- 一次提交只做一件事，避免把格式化与功能改动混在一起

## 代码规范

后端：

- 保持 Controller → Service → Mapper 分层，业务校验写在 Service
- 金额统一使用 `BigDecimal`，禁止 `double`；对外金额保留 2 位、`HALF_UP`
- 新接口需在 `@PreAuthorize` 标注权限码，并在菜单/按钮数据中登记
- 数据库变更只新增 Flyway 脚本 `V{n}__<简述>.sql`，不修改已执行的历史脚本

前端：

- 使用 JavaScript，不引入 TypeScript
- 列表页优先复用 `CrudPage` 组件，按钮级权限使用 `v-permission`
- 颜色、圆角、阴影统一走 `src/styles/index.css` 中的设计 token，不写散落的硬编码色值

## 提交前自检

```bash
# 后端
cd backend && mvn test

# 前端
cd frontend && npm run lint && npm run build && npm run e2e
```

涉及界面改动时，请附上改动前后的截图或录屏说明。

## 提交 PR

PR 描述中请包含：

1. 变更内容与动机
2. 影响范围（模块、接口、数据库迁移）
3. 验证方式（命令、用例、手工步骤）
4. 关联 Issue 编号

## 问题反馈

提 Issue 时请说明复现步骤、期望结果与实际结果，并附上关键日志或接口响应。
