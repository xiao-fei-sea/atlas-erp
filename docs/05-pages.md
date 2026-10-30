# 前端页面清单

## 1. 布局

- 登录页（`/login`）：账号密码登录、验证码可选、错误提示、Token 保存。
- 主布局（`/`）：左侧可折叠菜单 + 顶部导航（面包屑、通知、用户菜单）+ 内容区。
- 路由守卫：未登录跳登录；无权限页面跳 403；动态菜单由后端权限生成。
- 通用组件：`PageHeader`、`SearchForm`、`DataTable`、`Pagination`、`StatusTag`、`MoneyText`、`ConfirmButton`、`PermissionButton`。

## 2. 页面

| 模块 | 页面 | 关键功能 |
| --- | --- | --- |
| 仪表盘 | /dashboard | 销售/采购/库存/应收应付卡片、趋势图、排行、逾期提醒 |
| 系统 | /system/users | 列表、搜索、新增/编辑、启停、重置密码、分配角色 |
| 系统 | /system/roles | 角色 CRUD、菜单权限树分配、数据范围 |
| 系统 | /system/menus | 菜单树、按钮权限 |
| 系统 | /system/depts | 部门树 CRUD |
| 系统 | /system/dicts | 字典类型与字典项 |
| 系统 | /system/configs | 参数配置 |
| 系统 | /system/logs | 登录日志、操作日志（Tab） |
| 基础资料 | /masterdata/products | 商品列表/表单/导入导出/安全库存 |
| 基础资料 | /masterdata/categories | 分类树 |
| 基础资料 | /masterdata/units | 单位管理 |
| 基础资料 | /masterdata/customers | 客户列表/详情（联系人）/信用额度 |
| 基础资料 | /masterdata/suppliers | 供应商列表/详情 |
| 基础资料 | /masterdata/warehouses | 仓库/库位 |
| 库存 | /inventory/stocks | 实时库存、条件筛选、冻结/解冻、预警 |
| 库存 | /inventory/flows | 库存流水（按单据号、商品、时间） |
| 库存 | /inventory/transfers | 调拨单（新建、确认） |
| 库存 | /inventory/stocktakes | 盘点（录入实盘、提交、审批） |
| 库存 | /inventory/replenishment | 补货建议 |
| 采购 | /purchase/orders | 采购订单列表/编辑/提交审批/详情 |
| 采购 | /purchase/receipts | 收货单（分批收货、确认入库） |
| 销售 | /sales/orders | 销售订单列表/编辑/提交审批/详情 |
| 销售 | /sales/shipments | 发货单（分批发货、确认出库） |
| 财务 | /finance/receivables | 应收列表、账龄、核销入口 |
| 财务 | /finance/payables | 应付列表、账龄、核销入口 |
| 财务 | /finance/receipts | 收款登记、核销 |
| 财务 | /finance/payments | 付款登记、核销 |
| 财务 | /finance/accounts | 资金账户 |
| 审批 | /workflow/todo | 我的待办、通过/驳回 |
| 审批 | /workflow/instances | 审批实例与历史 |
| 审批 | /workflow/processes | 流程定义 |
| CRM | /crm/leads | 线索与转化 |
| CRM | /crm/opportunities | 商机与漏斗 |
| CRM | /crm/follow-ups | 跟进记录 |
| 报表 | /report/sales | 销售趋势、商品/客户排行、导出 |
| 报表 | /report/purchase | 采购趋势、供应商绩效、导出 |
| 报表 | /report/inventory | 库存金额、库龄、周转率 |

## 3. 交互规范

- 所有列表：查询区 + 表格 + 分页 + 排序；加载骨架、空状态、错误重试。
- 所有表单：必填校验、数字/金额校验、提交防重（loading + 按钮禁用）。
- 危险操作（删除、作废、驳回、冻结）：二次确认。
- 权限：按钮与路由均以 `v-permission` 或 `hasPerm` 校验，后端仍强制校验。
- 金额展示统一 2 位小数，数量按单位精度展示。
