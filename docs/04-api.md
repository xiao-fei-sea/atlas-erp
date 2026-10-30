# Java API 设计

## 1. 统一约定

- 前缀：`/api`
- 认证：`Authorization: Bearer <JWT>`，登录接口除外。
- 响应：

```json
{ "code": 0, "message": "success", "data": {...}, "traceId": "..." }
```

- 分页响应 `data`：`{ "records": [], "total": 0, "page": 1, "size": 10 }`
- 错误码：`0` 成功；`400xx` 参数/业务校验；`401xx` 认证；`403xx` 授权；`404xx` 不存在；`409xx` 冲突/幂等；`500xx` 系统错误。
- 分页参数：`page`（默认 1）、`size`（默认 10，最大 100）、`sort`（如 `createdAt,desc`）。
- 幂等：关键写接口支持请求头 `Idempotency-Key`，相同 key 在 24h 内返回首次结果。
- 文档：`/swagger-ui.html`、`/v3/api-docs`。

## 2. 认证与系统

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | /api/auth/login | 匿名 | 登录，返回 token + 用户信息 |
| POST | /api/auth/logout | 登录 | 记录退出日志 |
| GET | /api/auth/me | 登录 | 当前用户、角色、权限、菜单树 |
| POST | /api/auth/password | 登录 | 修改密码 |
| GET/POST/PUT/DELETE | /api/system/users | system:user:* | 用户管理 |
| PUT | /api/system/users/{id}/status | system:user:update | 启停 |
| PUT | /api/system/users/{id}/password | system:user:reset | 重置密码 |
| GET/POST/PUT/DELETE | /api/system/roles | system:role:* | 角色管理 |
| PUT | /api/system/roles/{id}/menus | system:role:assign | 分配权限 |
| GET/POST/PUT/DELETE | /api/system/menus | system:menu:* | 菜单管理 |
| GET | /api/system/menus/tree | 登录 | 菜单树 |
| GET/POST/PUT/DELETE | /api/system/depts | system:dept:* | 部门管理 |
| GET/POST/PUT/DELETE | /api/system/dicts | system:dict:* | 字典 |
| GET/POST/PUT/DELETE | /api/system/configs | system:config:* | 参数 |
| GET | /api/system/logs/login | system:log:view | 登录日志 |
| GET | /api/system/logs/oper | system:log:view | 操作日志 |

## 3. 基础资料

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET/POST/PUT/DELETE | /api/masterdata/products | masterdata:product:* |
| GET/POST/PUT/DELETE | /api/masterdata/product-categories | masterdata:category:* |
| GET/POST/PUT/DELETE | /api/masterdata/units | masterdata:unit:* |
| GET/POST/PUT/DELETE | /api/masterdata/customers | masterdata:customer:* |
| GET/POST/PUT/DELETE | /api/masterdata/suppliers | masterdata:supplier:* |
| GET/POST/PUT/DELETE | /api/masterdata/warehouses | masterdata:warehouse:* |
| GET/POST/PUT/DELETE | /api/masterdata/locations | masterdata:location:* |
| GET | /api/masterdata/export/{type} | 对应查询权限，返回 CSV |

## 4. 库存

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET | /api/inventory/stocks | inventory:stock:view |
| GET | /api/inventory/stocks/warnings | inventory:stock:view |
| GET | /api/inventory/flows | inventory:flow:view |
| POST | /api/inventory/inbound | inventory:inbound |
| POST | /api/inventory/outbound | inventory:outbound |
| POST | /api/inventory/transfers | inventory:transfer |
| PUT | /api/inventory/transfers/{id}/confirm | inventory:transfer |
| GET/POST | /api/inventory/stocktakes | inventory:stocktake |
| PUT | /api/inventory/stocktakes/{id}/submit | inventory:stocktake |
| PUT | /api/inventory/stocktakes/{id}/approve | inventory:stocktake:approve |
| PUT | /api/inventory/stocks/freeze | inventory:stock:freeze |
| PUT | /api/inventory/stocks/release | inventory:stock:freeze |
| GET | /api/inventory/replenishment | inventory:stock:view |

## 5. 采购与销售

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET/POST/PUT | /api/purchase/orders | purchase:order:* |
| GET | /api/purchase/orders/{id} | purchase:order:view |
| PUT | /api/purchase/orders/{id}/submit | purchase:order:submit |
| GET/POST | /api/purchase/receipts | purchase:receipt:* |
| PUT | /api/purchase/receipts/{id}/confirm | purchase:receipt:confirm |
| GET/POST | /api/sales/orders | sales:order:* |
| GET | /api/sales/orders/{id} | sales:order:view |
| PUT | /api/sales/orders/{id}/submit | sales:order:submit |
| GET/POST | /api/sales/shipments | sales:shipment:* |
| PUT | /api/sales/shipments/{id}/confirm | sales:shipment:confirm |

## 6. 财务

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET | /api/finance/receivables | finance:receivable:view |
| GET | /api/finance/payables | finance:payable:view |
| GET/POST | /api/finance/receipts | finance:receipt:* |
| POST | /api/finance/receipts/{id}/write-off | finance:receipt:writeoff |
| GET/POST | /api/finance/payments | finance:payment:* |
| POST | /api/finance/payments/{id}/write-off | finance:payment:writeoff |
| GET/POST | /api/finance/accounts | finance:account:* |
| GET | /api/finance/aging/receivable | finance:report:view |
| GET | /api/finance/aging/payable | finance:report:view |

## 7. 审批 / CRM / 报表

| 方法 | 路径 | 权限 |
| --- | --- | --- |
| GET/POST | /api/workflow/processes | workflow:process:view / create |
| GET/PUT/DELETE | /api/workflow/processes/{id} | workflow:process:view / update |
| GET | /api/workflow/instances | workflow:instance:view |
| GET | /api/workflow/instances/{id} | workflow:instance:view |
| GET | /api/workflow/tasks/pending | workflow:task:view |
| POST | /api/workflow/tasks/{id}/approve | workflow:task:view（会签需全部通过，或签一人通过） |
| POST | /api/workflow/tasks/{id}/reject | workflow:task:view |
| GET | /api/workflow/tasks/transfer-options | workflow:task:transfer |
| POST | /api/workflow/tasks/{id}/transfer | workflow:task:transfer |
| PUT | /api/workflow/instances/{id}/cancel | workflow:instance:view（提交人或管理员） |
| GET/POST/PUT | /api/crm/leads | crm:lead:* |
| GET/POST/PUT | /api/crm/opportunities | crm:opportunity:* |
| GET/POST | /api/crm/follow-ups | crm:followup:* |
| GET | /api/report/dashboard | report:dashboard:view |
| GET | /api/report/sales-trend | report:sales:view |
| GET | /api/report/purchase-trend | report:purchase:view |
| GET | /api/report/product-ranking | report:sales:view |
| GET | /api/report/customer-ranking | report:sales:view |
| GET | /api/report/supplier-performance | report:purchase:view |
| GET | /api/report/export/{type} | 对应权限 |

审批节点支持 `signType`（OR 或签 / AND 会签）、`approverIds`（逗号分隔多审批人）、`deptId`（限定发起部门）、`timeoutHours`（超时小时数，0 不限）；超时任务由定时扫描标记 `overdue=1`。
