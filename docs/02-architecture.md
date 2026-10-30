# 系统架构与业务流程

## 1. 总体架构

```
浏览器 (Vue3 + Element Plus + Pinia)
        │  HTTPS / Axios (JWT Bearer)
        ▼
Nginx ──静态资源 / 反向代理──► Spring Boot 3 (erp-app)
                                ├─ erp-security  (JWT 认证、Spring Security 授权)
                                ├─ erp-system    (用户/角色/菜单/部门/字典/日志)
                                ├─ erp-masterdata(商品/客户/供应商/仓库)
                                ├─ erp-inventory (库存余额/流水/出入库/调拨/盘点)
                                ├─ erp-purchase  (采购订单/收货/入库)
                                ├─ erp-sales     (销售订单/发货/出库)
                                ├─ erp-finance   (应收应付/收付款/核销/账户)
                                ├─ erp-workflow  (审批定义/实例/任务)
                                ├─ erp-crm       (线索/商机/跟进)
                                └─ erp-report    (统计查询/导出)
                                        │ MyBatis-Plus
                                        ▼
                                   MySQL 8.0 (Flyway)
```

- 模块化单体：一个 Spring Boot 进程，Maven 多模块，模块间通过 Service 接口调用。
- 前端仅做展示与交互，业务规则、金额计算、库存变动、状态流转全部在后端执行。

## 2. 后端模块依赖

```
erp-common  ◄── erp-system ◄── erp-security
   ▲              ▲   ▲
   │              │   └──────── erp-masterdata
   │              │                 ▲
   │        erp-workflow ◄──────────┤
   │              ▲                 │
   │        erp-finance ◄──── erp-inventory
   │              ▲                 ▲
   │        erp-sales / erp-purchase┘
   └──────── erp-report / erp-crm
                 ▲
             erp-app (聚合 + 启动 + Flyway)
```

依赖方向单向，禁止反向依赖；跨模块副作用通过 Service 调用或业务事件完成。

## 3. 核心业务流程

### 3.1 销售闭环

```
客户建档 → 商品建档 → 销售订单(草稿) → 提交审批 → 审批通过
   → 发货单/销售出库 → 扣减库存(校验可用量) + 生成库存流水
   → 生成应收账款 → 收款登记 → 核销 → 应收余额更新 → 报表
```

状态机：`DRAFT → PENDING → APPROVED → PARTIAL_SHIPPED → SHIPPED → CLOSED`；驳回 `REJECTED`，异常 `CANCELLED`。出库只在 `APPROVED/PARTIAL_SHIPPED` 且剩余可发数量 > 0 时允许。

### 3.2 采购闭环

```
供应商建档 → 采购订单 → 提交审批 → 审批通过
   → 收货单(分批) → 采购入库 → 增加库存 + 库存流水
   → 生成应付账款 → 付款登记 → 核销 → 应付余额更新 → 报表
```

重复收货通过"订单明细已收数量 + 行锁"约束；重复付款通过核销金额上限与幂等键约束。

### 3.3 库存闭环

```
初始入库 → 销售出库 → 采购入库 → 调拨 → 盘点 → 差异审批 → 调整入库/出库
```

每次库存变动在一个事务内完成：`SELECT ... FOR UPDATE` 锁定库存行 → 校验可用量 → 更新余额 → 写入流水（记录 before/after）。

### 3.4 审批闭环

```
业务单据提交 → 匹配审批流程(业务类型+金额) → 创建实例与任务
→ 审批人通过/驳回 → 更新任务与实例 → 回调业务模块更新状态 → 写审计日志
```

审批接口校验当前登录人是否为当前节点审批人；前端按钮状态不作为授权依据。

## 4. 关键技术决策

| 主题 | 决策 |
| --- | --- |
| 认证 | JWT（HS256）无状态；token 含 userId、username；每次请求加载权限 |
| 授权 | `@PreAuthorize("hasAuthority('sales:order:create')")` + URL 级兜底 |
| 密码 | BCrypt，强度 10 |
| ORM | MyBatis-Plus LambdaQueryWrapper，报表用 XML |
| 事务 | Service 层 `@Transactional(rollbackFor = Exception.class)` |
| 并发 | 库存行 `FOR UPDATE` + `version` 乐观锁 + 状态机条件更新 |
| 幂等 | 客户端 `Idempotency-Key` 存 `sys_idempotency`，重复请求返回首次结果 |
| 金额 | `DECIMAL(18,4)` 存储，`BigDecimal` 计算，最终金额 2 位四舍五入 |
| 审计 | `@OperLog` 注解 + AOP 入库；登录日志独立记录 |
| 迁移 | Flyway `V1__schema.sql`、`V2__seed.sql`，禁止修改已执行脚本 |
| 环境 | `application.yml` + 环境变量覆盖（DB_URL/DB_USER/DB_PASSWORD/JWT_SECRET） |

## 5. 数据一致性约定

1. 库存余额只能由库存服务写入，禁止前端直接改数量。
2. 应收/应付由出入库服务在同一事务内创建。
3. 核销金额不得超过单据未核销金额，核销后余额由聚合查询推导。
4. 所有单据状态流转走 `status` 条件更新（`UPDATE ... WHERE status = ?`），失败即回滚。
5. 库存流水与余额可核对：`SUM(qty_change)` 等于当前在库数量（同一商品/仓库/批次）。
