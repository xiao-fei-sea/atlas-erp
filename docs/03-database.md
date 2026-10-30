# 数据库设计（MySQL 8.0）

- 字符集 `utf8mb4`，排序规则 `utf8mb4_0900_ai_ci`，引擎 InnoDB。
- 主键统一 `BIGINT AUTO_INCREMENT`；金额 `DECIMAL(18,4)`；数量 `DECIMAL(18,4)`。
- 所有业务表含审计字段：`created_by`、`created_at`、`updated_by`、`updated_at`、`deleted`（逻辑删除）。
- 迁移文件：`backend/erp-app/src/main/resources/db/migration/V1__schema.sql`、`V2__seed.sql`。

## 1. 实体关系概览

```
sys_dept 1─n sys_user n─n sys_role n─n sys_menu
sys_role.data_scope ─ 数据范围
md_product n─1 md_product_category / md_unit
md_customer 1─n sal_order 1─n sal_order_item
md_supplier 1─n pur_order 1─n pur_order_item
sal_order 1─n sal_shipment 1─n sal_shipment_item ─► inv_flow ─► inv_stock
pur_order 1─n pur_receipt  1─n pur_receipt_item   ─► inv_flow ─► inv_stock
sal_shipment ─► fin_receivable ─► fin_receipt_alloc ◄─ fin_receipt
pur_receipt  ─► fin_payable    ─► fin_payment_alloc ◄─ fin_payment
业务单据 ─► wf_instance 1─n wf_task / wf_history
```

## 2. 核心表清单

### 2.1 系统（sys_*）

| 表 | 关键字段 | 说明 |
| --- | --- | --- |
| sys_user | username(uk), password, real_name, dept_id, phone, email, status | 用户 |
| sys_role | code(uk), name, data_scope(ALL/DEPT/DEPT_AND_CHILD/SELF), status | 角色 |
| sys_user_role | user_id, role_id (uk 联合) | 用户角色 |
| sys_menu | parent_id, name, type(DIR/MENU/BUTTON), path, component, permission, sort, visible | 菜单与按钮权限 |
| sys_role_menu | role_id, menu_id (uk 联合) | 角色权限 |
| sys_dept | parent_id, name, code(uk), leader, sort, status | 部门 |
| sys_dict_type / sys_dict_item | type_code(uk) / dict_type_id, label, value | 字典 |
| sys_config | config_key(uk), config_value, group_code | 参数 |
| sys_id_sequence | seq_code(uk), prefix, date_pattern, current_value | 单据编号序列 |
| sys_login_log | username, ip, user_agent, success, message, login_at | 登录日志 |
| sys_oper_log | module, action, method, params, result, success, cost_ms, operator, created_at | 操作日志 |
| sys_idempotency | idem_key(uk), user_id, result_json, created_at | 幂等记录 |
| sys_notice | title, content, type, receiver_id, read_flag | 站内通知 |

### 2.2 基础资料（md_*）

| 表 | 关键字段 |
| --- | --- |
| md_product_category | parent_id, code(uk), name, sort, status |
| md_unit | code(uk), name, precision |
| md_product | code(uk), name, category_id, spec, unit_id, tax_rate, purchase_price, sale_price, safety_stock, status |
| md_customer | code(uk), name, level, contact_name, phone, email, tax_no, address, credit_limit, status |
| md_customer_contact | customer_id, name, phone, email, position |
| md_supplier | code(uk), name, contact_name, phone, email, address, status |
| md_warehouse | code(uk), name, address, manager, status |
| md_location | warehouse_id, code, name, status (uk: warehouse_id+code) |

### 2.3 库存（inv_*）

| 表 | 关键字段 |
| --- | --- |
| inv_stock | product_id, warehouse_id, batch_no, expiry_date, qty_on_hand, qty_reserved, qty_frozen, avg_cost, version；uk(product_id, warehouse_id, batch_no) |
| inv_flow | flow_no, biz_type(PUR_IN/SAL_OUT/TRANSFER_IN/TRANSFER_OUT/STOCKTAKE_ADJ/OTHER_IN/OTHER_OUT), biz_no, product_id, warehouse_id, batch_no, qty_change, qty_before, qty_after, unit_cost, amount, ref_type, ref_id, operator_id |
| inv_transfer | transfer_no(uk), from_warehouse_id, to_warehouse_id, status(DRAFT/DONE/CANCELLED), remark |
| inv_transfer_item | transfer_id, product_id, batch_no, qty |
| inv_stocktake | stocktake_no(uk), warehouse_id, status(DRAFT/SUBMITTED/APPROVED), remark |
| inv_stocktake_item | stocktake_id, product_id, batch_no, system_qty, actual_qty, diff_qty |
| inv_batch | product_id, batch_no, expiry_date, production_date (uk: product_id+batch_no) |

### 2.4 采购（pur_*）

| 表 | 关键字段 |
| --- | --- |
| pur_order | order_no(uk), supplier_id, order_date, status, total_amount, tax_amount, discount_amount, remark, approval_instance_id |
| pur_order_item | order_id, product_id, qty, received_qty, price, tax_rate, amount |
| pur_receipt | receipt_no(uk), order_id, supplier_id, warehouse_id, receipt_date, status, total_amount |
| pur_receipt_item | receipt_id, order_item_id, product_id, batch_no, qty, price |
| pur_return | return_no(uk), receipt_id, supplier_id, warehouse_id, return_date, total_amount, status |
| pur_return_item | return_id, product_id, batch_no, qty, price |

### 2.5 销售（sal_*）

| 表 | 关键字段 |
| --- | --- |
| sal_order | order_no(uk), customer_id, order_date, status, total_amount, tax_amount, discount_amount, remark, approval_instance_id |
| sal_order_item | order_id, product_id, qty, shipped_qty, price, tax_rate, amount |
| sal_shipment | shipment_no(uk), order_id, customer_id, warehouse_id, shipment_date, status, total_amount |
| sal_shipment_item | shipment_id, order_item_id, product_id, batch_no, qty, price |
| sal_return | return_no(uk), shipment_id, customer_id, warehouse_id, return_date, total_amount, status |
| sal_return_item | return_id, product_id, batch_no, qty, price |

### 2.6 财务（fin_*）

| 表 | 关键字段 |
| --- | --- |
| fin_account | code(uk), name, type(CASH/BANK), balance |
| fin_receivable | receivable_no(uk), customer_id, source_type(SHIPMENT), source_id, source_no, amount, settled_amount, status(UNSETTLED/PARTIAL/SETTLED), due_date |
| fin_payable | payable_no(uk), supplier_id, source_type(RECEIPT/RETURN), source_id, source_no, amount, settled_amount, status, due_date |
| fin_receipt | receipt_no(uk), customer_id, account_id, amount, used_amount, receipt_date, status, remark |
| fin_receipt_alloc | receipt_id, receivable_id, amount |
| fin_payment | payment_no(uk), supplier_id, account_id, amount, used_amount, payment_date, status, remark |
| fin_payment_alloc | payment_id, payable_id, amount |
| fin_expense | expense_no(uk), applicant_id, dept_id, amount, expense_type, status, approval_instance_id |
| fin_invoice | invoice_no, biz_type, biz_id, amount, tax_amount, invoice_date, status |

### 2.7 审批（wf_*）

| 表 | 关键字段 |
| --- | --- |
| wf_process | code(uk), name, biz_type(PUR_ORDER/SAL_ORDER/EXPENSE/PAYMENT/STOCK_ADJUST), enabled |
| wf_node | process_id, seq, name, approver_type(ROLE/USER), approver_id, min_amount, max_amount |
| wf_instance | process_id, biz_type, biz_id, biz_no, submitter_id, status(RUNNING/APPROVED/REJECTED/CANCELLED), current_seq |
| wf_task | instance_id, node_seq, node_name, approver_id, approver_role, status(PENDING/APPROVED/REJECTED), opinion, acted_at |
| wf_history | instance_id, node_seq, action, operator_id, opinion, created_at |

### 2.8 CRM（crm_*）

| 表 | 关键字段 |
| --- | --- |
| crm_lead | name, company, phone, source, status(NEW/FOLLOWING/CONVERTED/INVALID), owner_id |
| crm_opportunity | customer_id, name, amount, stage, probability, expected_close_date, owner_id |
| crm_follow_up | customer_id / lead_id / opportunity_id, content, follow_type, follow_at, owner_id |
| crm_visit_plan | customer_id, plan_date, purpose, status, owner_id |

## 3. 索引与约束原则

- 业务编码（order_no、code 等）建唯一索引。
- 外键在应用层保证（避免分库与批量导入限制），关键关联字段建普通索引。
- 库存表唯一键 `(product_id, warehouse_id, batch_no)`，插入冲突时行锁重试。
- 逻辑删除字段 `deleted` 参与唯一索引时采用 `deleted` 0/1 + 业务唯一键组合策略。
- 期初库存通过库存流水写入，禁止直接改余额。
