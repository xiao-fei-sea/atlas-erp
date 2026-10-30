-- =============================================================
-- ERP V2 初始化数据
-- 默认管理员： admin / Admin@123
-- =============================================================

INSERT INTO sys_dept (id, parent_id, name, code, leader, sort, status) VALUES
 (1, 0, '总部', 'HQ', '管理员', 1, 1),
 (2, 1, '销售部', 'SALES', '张三', 1, 1),
 (3, 1, '采购部', 'PURCHASE', '李四', 2, 1),
 (4, 1, '仓储部', 'WAREHOUSE', '王五', 3, 1),
 (5, 1, '财务部', 'FINANCE', '赵六', 4, 1);

INSERT INTO sys_role (id, code, name, data_scope, remark, status) VALUES
 (1, 'ADMIN', '超级管理员', 'ALL', '系统内置角色', 1),
 (2, 'SALES_MANAGER', '销售经理', 'DEPT_AND_CHILD', '销售业务审批', 1),
 (3, 'SALES', '销售专员', 'SELF', '销售执行', 1),
 (4, 'PURCHASE_MANAGER', '采购经理', 'DEPT_AND_CHILD', '采购业务审批', 1),
 (5, 'PURCHASE', '采购专员', 'SELF', '采购执行', 1),
 (6, 'WAREHOUSE', '仓库管理员', 'ALL', '仓储执行', 1),
 (7, 'FINANCE', '财务专员', 'ALL', '资金与往来', 1);

INSERT INTO sys_user (id, username, password, real_name, dept_id, phone, email, status, remark) VALUES
 (1, 'admin', '$2a$10$SkmLOzjS0C50CQerKpSUsuEEQQsysXONhFsjTnP/1whworkemeaZq', '系统管理员', 1, '13800000000', 'admin@erp.local', 1, '内置管理员'),
 (2, 'sales01', '$2a$10$SkmLOzjS0C50CQerKpSUsuEEQQsysXONhFsjTnP/1whworkemeaZq', '销售专员', 2, '13800000001', 'sales@erp.local', 1, NULL),
 (3, 'salesmgr', '$2a$10$SkmLOzjS0C50CQerKpSUsuEEQQsysXONhFsjTnP/1whworkemeaZq', '销售经理', 2, '13800000002', 'salesmgr@erp.local', 1, NULL),
 (4, 'purchase01', '$2a$10$SkmLOzjS0C50CQerKpSUsuEEQQsysXONhFsjTnP/1whworkemeaZq', '采购专员', 3, '13800000003', 'purchase@erp.local', 1, NULL),
 (5, 'purchasemgr', '$2a$10$SkmLOzjS0C50CQerKpSUsuEEQQsysXONhFsjTnP/1whworkemeaZq', '采购经理', 3, '13800000004', 'purchasemgr@erp.local', 1, NULL),
 (6, 'wh01', '$2a$10$SkmLOzjS0C50CQerKpSUsuEEQQsysXONhFsjTnP/1whworkemeaZq', '仓库管理员', 4, '13800000005', 'wh@erp.local', 1, NULL),
 (7, 'fin01', '$2a$10$SkmLOzjS0C50CQerKpSUsuEEQQsysXONhFsjTnP/1whworkemeaZq', '财务专员', 5, '13800000006', 'fin@erp.local', 1, NULL);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 3), (3, 2), (4, 5), (5, 4), (6, 6), (7, 7);

-- ---------- 菜单 ----------
INSERT INTO sys_menu (id, parent_id, name, type, path, component, permission, icon, sort, visible, status) VALUES
 (1, 0, '仪表盘', 'MENU', '/dashboard', 'dashboard/index', 'report:dashboard:view', 'Odometer', 1, 1, 1),
 (2, 0, '系统管理', 'DIR', '/system', NULL, NULL, 'Setting', 10, 1, 1),
 (21, 2, '用户管理', 'MENU', '/system/users', 'system/users/index', 'system:user:view', NULL, 1, 1, 1),
 (211, 21, '新增用户', 'BUTTON', NULL, NULL, 'system:user:create', NULL, 1, 1, 1),
 (212, 21, '修改用户', 'BUTTON', NULL, NULL, 'system:user:update', NULL, 2, 1, 1),
 (213, 21, '删除用户', 'BUTTON', NULL, NULL, 'system:user:delete', NULL, 3, 1, 1),
 (214, 21, '重置密码', 'BUTTON', NULL, NULL, 'system:user:reset', NULL, 4, 1, 1),
 (22, 2, '角色管理', 'MENU', '/system/roles', 'system/roles/index', 'system:role:view', NULL, 2, 1, 1),
 (221, 22, '新增角色', 'BUTTON', NULL, NULL, 'system:role:create', NULL, 1, 1, 1),
 (222, 22, '修改角色', 'BUTTON', NULL, NULL, 'system:role:update', NULL, 2, 1, 1),
 (223, 22, '删除角色', 'BUTTON', NULL, NULL, 'system:role:delete', NULL, 3, 1, 1),
 (224, 22, '分配权限', 'BUTTON', NULL, NULL, 'system:role:assign', NULL, 4, 1, 1),
 (23, 2, '菜单管理', 'MENU', '/system/menus', 'system/menus/index', 'system:menu:view', NULL, 3, 1, 1),
 (231, 23, '新增菜单', 'BUTTON', NULL, NULL, 'system:menu:create', NULL, 1, 1, 1),
 (232, 23, '修改菜单', 'BUTTON', NULL, NULL, 'system:menu:update', NULL, 2, 1, 1),
 (233, 23, '删除菜单', 'BUTTON', NULL, NULL, 'system:menu:delete', NULL, 3, 1, 1),
 (24, 2, '部门管理', 'MENU', '/system/depts', 'system/depts/index', 'system:dept:view', NULL, 4, 1, 1),
 (241, 24, '新增部门', 'BUTTON', NULL, NULL, 'system:dept:create', NULL, 1, 1, 1),
 (242, 24, '修改部门', 'BUTTON', NULL, NULL, 'system:dept:update', NULL, 2, 1, 1),
 (243, 24, '删除部门', 'BUTTON', NULL, NULL, 'system:dept:delete', NULL, 3, 1, 1),
 (25, 2, '字典管理', 'MENU', '/system/dicts', 'system/dicts/index', 'system:dict:view', NULL, 5, 1, 1),
 (251, 25, '新增字典', 'BUTTON', NULL, NULL, 'system:dict:create', NULL, 1, 1, 1),
 (252, 25, '修改字典', 'BUTTON', NULL, NULL, 'system:dict:update', NULL, 2, 1, 1),
 (253, 25, '删除字典', 'BUTTON', NULL, NULL, 'system:dict:delete', NULL, 3, 1, 1),
 (26, 2, '参数配置', 'MENU', '/system/configs', 'system/configs/index', 'system:config:view', NULL, 6, 1, 1),
 (261, 26, '保存参数', 'BUTTON', NULL, NULL, 'system:config:update', NULL, 1, 1, 1),
 (27, 2, '日志管理', 'MENU', '/system/logs', 'system/logs/index', 'system:log:view', NULL, 7, 1, 1),
 (3, 0, '基础资料', 'DIR', '/masterdata', NULL, NULL, 'Files', 20, 1, 1),
 (31, 3, '商品管理', 'MENU', '/masterdata/products', 'masterdata/products/index', 'masterdata:product:view', NULL, 1, 1, 1),
 (311, 31, '新增商品', 'BUTTON', NULL, NULL, 'masterdata:product:create', NULL, 1, 1, 1),
 (312, 31, '修改商品', 'BUTTON', NULL, NULL, 'masterdata:product:update', NULL, 2, 1, 1),
 (313, 31, '删除商品', 'BUTTON', NULL, NULL, 'masterdata:product:delete', NULL, 3, 1, 1),
 (314, 31, '导出商品', 'BUTTON', NULL, NULL, 'masterdata:product:export', NULL, 4, 1, 1),
 (32, 3, '商品分类', 'MENU', '/masterdata/categories', 'masterdata/categories/index', 'masterdata:category:view', NULL, 2, 1, 1),
 (321, 32, '新增分类', 'BUTTON', NULL, NULL, 'masterdata:category:create', NULL, 1, 1, 1),
 (322, 32, '修改分类', 'BUTTON', NULL, NULL, 'masterdata:category:update', NULL, 2, 1, 1),
 (323, 32, '删除分类', 'BUTTON', NULL, NULL, 'masterdata:category:delete', NULL, 3, 1, 1),
 (33, 3, '计量单位', 'MENU', '/masterdata/units', 'masterdata/units/index', 'masterdata:unit:view', NULL, 3, 1, 1),
 (331, 33, '新增单位', 'BUTTON', NULL, NULL, 'masterdata:unit:create', NULL, 1, 1, 1),
 (332, 33, '修改单位', 'BUTTON', NULL, NULL, 'masterdata:unit:update', NULL, 2, 1, 1),
 (333, 33, '删除单位', 'BUTTON', NULL, NULL, 'masterdata:unit:delete', NULL, 3, 1, 1),
 (34, 3, '客户管理', 'MENU', '/masterdata/customers', 'masterdata/customers/index', 'masterdata:customer:view', NULL, 4, 1, 1),
 (341, 34, '新增客户', 'BUTTON', NULL, NULL, 'masterdata:customer:create', NULL, 1, 1, 1),
 (342, 34, '修改客户', 'BUTTON', NULL, NULL, 'masterdata:customer:update', NULL, 2, 1, 1),
 (343, 34, '删除客户', 'BUTTON', NULL, NULL, 'masterdata:customer:delete', NULL, 3, 1, 1),
 (35, 3, '供应商管理', 'MENU', '/masterdata/suppliers', 'masterdata/suppliers/index', 'masterdata:supplier:view', NULL, 5, 1, 1),
 (351, 35, '新增供应商', 'BUTTON', NULL, NULL, 'masterdata:supplier:create', NULL, 1, 1, 1),
 (352, 35, '修改供应商', 'BUTTON', NULL, NULL, 'masterdata:supplier:update', NULL, 2, 1, 1),
 (353, 35, '删除供应商', 'BUTTON', NULL, NULL, 'masterdata:supplier:delete', NULL, 3, 1, 1),
 (36, 3, '仓库管理', 'MENU', '/masterdata/warehouses', 'masterdata/warehouses/index', 'masterdata:warehouse:view', NULL, 6, 1, 1),
 (361, 36, '新增仓库', 'BUTTON', NULL, NULL, 'masterdata:warehouse:create', NULL, 1, 1, 1),
 (362, 36, '修改仓库', 'BUTTON', NULL, NULL, 'masterdata:warehouse:update', NULL, 2, 1, 1),
 (363, 36, '删除仓库', 'BUTTON', NULL, NULL, 'masterdata:warehouse:delete', NULL, 3, 1, 1),
 (4, 0, '库存管理', 'DIR', '/inventory', NULL, NULL, 'Box', 30, 1, 1),
 (41, 4, '库存查询', 'MENU', '/inventory/stocks', 'inventory/stocks/index', 'inventory:stock:view', NULL, 1, 1, 1),
 (411, 41, '库存冻结', 'BUTTON', NULL, NULL, 'inventory:stock:freeze', NULL, 1, 1, 1),
 (412, 41, '其他入库', 'BUTTON', NULL, NULL, 'inventory:inbound', NULL, 2, 1, 1),
 (413, 41, '其他出库', 'BUTTON', NULL, NULL, 'inventory:outbound', NULL, 3, 1, 1),
 (42, 4, '库存流水', 'MENU', '/inventory/flows', 'inventory/flows/index', 'inventory:flow:view', NULL, 2, 1, 1),
 (43, 4, '库存调拨', 'MENU', '/inventory/transfers', 'inventory/transfers/index', 'inventory:transfer:view', NULL, 3, 1, 1),
 (431, 43, '新建调拨', 'BUTTON', NULL, NULL, 'inventory:transfer:create', NULL, 1, 1, 1),
 (432, 43, '确认调拨', 'BUTTON', NULL, NULL, 'inventory:transfer:confirm', NULL, 2, 1, 1),
 (44, 4, '库存盘点', 'MENU', '/inventory/stocktakes', 'inventory/stocktakes/index', 'inventory:stocktake:view', NULL, 4, 1, 1),
 (441, 44, '新建盘点', 'BUTTON', NULL, NULL, 'inventory:stocktake:create', NULL, 1, 1, 1),
 (442, 44, '提交盘点', 'BUTTON', NULL, NULL, 'inventory:stocktake:submit', NULL, 2, 1, 1),
 (443, 44, '审批盘点', 'BUTTON', NULL, NULL, 'inventory:stocktake:approve', NULL, 3, 1, 1),
 (45, 4, '补货建议', 'MENU', '/inventory/replenishment', 'inventory/replenishment/index', 'inventory:stock:view', NULL, 5, 1, 1),
 (5, 0, '采购管理', 'DIR', '/purchase', NULL, NULL, 'ShoppingCart', 40, 1, 1),
 (51, 5, '采购订单', 'MENU', '/purchase/orders', 'purchase/orders/index', 'purchase:order:view', NULL, 1, 1, 1),
 (511, 51, '新增采购单', 'BUTTON', NULL, NULL, 'purchase:order:create', NULL, 1, 1, 1),
 (512, 51, '修改采购单', 'BUTTON', NULL, NULL, 'purchase:order:update', NULL, 2, 1, 1),
 (513, 51, '提交审批', 'BUTTON', NULL, NULL, 'purchase:order:submit', NULL, 3, 1, 1),
 (52, 5, '收货入库', 'MENU', '/purchase/receipts', 'purchase/receipts/index', 'purchase:receipt:view', NULL, 2, 1, 1),
 (521, 52, '新建收货单', 'BUTTON', NULL, NULL, 'purchase:receipt:create', NULL, 1, 1, 1),
 (522, 52, '确认入库', 'BUTTON', NULL, NULL, 'purchase:receipt:confirm', NULL, 2, 1, 1),
 (6, 0, '销售管理', 'DIR', '/sales', NULL, NULL, 'Sell', 50, 1, 1),
 (61, 6, '销售订单', 'MENU', '/sales/orders', 'sales/orders/index', 'sales:order:view', NULL, 1, 1, 1),
 (611, 61, '新增销售单', 'BUTTON', NULL, NULL, 'sales:order:create', NULL, 1, 1, 1),
 (612, 61, '修改销售单', 'BUTTON', NULL, NULL, 'sales:order:update', NULL, 2, 1, 1),
 (613, 61, '提交审批', 'BUTTON', NULL, NULL, 'sales:order:submit', NULL, 3, 1, 1),
 (62, 6, '发货出库', 'MENU', '/sales/shipments', 'sales/shipments/index', 'sales:shipment:view', NULL, 2, 1, 1),
 (621, 62, '新建发货单', 'BUTTON', NULL, NULL, 'sales:shipment:create', NULL, 1, 1, 1),
 (622, 62, '确认出库', 'BUTTON', NULL, NULL, 'sales:shipment:confirm', NULL, 2, 1, 1),
 (7, 0, '财务管理', 'DIR', '/finance', NULL, NULL, 'Money', 60, 1, 1),
 (71, 7, '应收账款', 'MENU', '/finance/receivables', 'finance/receivables/index', 'finance:receivable:view', NULL, 1, 1, 1),
 (72, 7, '应付账款', 'MENU', '/finance/payables', 'finance/payables/index', 'finance:payable:view', NULL, 2, 1, 1),
 (73, 7, '收款管理', 'MENU', '/finance/receipts', 'finance/receipts/index', 'finance:receipt:view', NULL, 3, 1, 1),
 (731, 73, '新增收款', 'BUTTON', NULL, NULL, 'finance:receipt:create', NULL, 1, 1, 1),
 (732, 73, '收款核销', 'BUTTON', NULL, NULL, 'finance:receipt:writeoff', NULL, 2, 1, 1),
 (74, 7, '付款管理', 'MENU', '/finance/payments', 'finance/payments/index', 'finance:payment:view', NULL, 4, 1, 1),
 (741, 74, '新增付款', 'BUTTON', NULL, NULL, 'finance:payment:create', NULL, 1, 1, 1),
 (742, 74, '付款核销', 'BUTTON', NULL, NULL, 'finance:payment:writeoff', NULL, 2, 1, 1),
 (75, 7, '资金账户', 'MENU', '/finance/accounts', 'finance/accounts/index', 'finance:account:view', NULL, 5, 1, 1),
 (751, 75, '新增账户', 'BUTTON', NULL, NULL, 'finance:account:create', NULL, 1, 1, 1),
 (752, 75, '修改账户', 'BUTTON', NULL, NULL, 'finance:account:update', NULL, 2, 1, 1),
 (8, 0, '审批中心', 'DIR', '/workflow', NULL, NULL, 'Stamp', 70, 1, 1),
 (81, 8, '我的待办', 'MENU', '/workflow/todo', 'workflow/todo/index', 'workflow:task:view', NULL, 1, 1, 1),
 (82, 8, '审批实例', 'MENU', '/workflow/instances', 'workflow/instances/index', 'workflow:instance:view', NULL, 2, 1, 1),
 (83, 8, '流程定义', 'MENU', '/workflow/processes', 'workflow/processes/index', 'workflow:process:view', NULL, 3, 1, 1),
 (831, 83, '新增流程', 'BUTTON', NULL, NULL, 'workflow:process:create', NULL, 1, 1, 1),
 (832, 83, '修改流程', 'BUTTON', NULL, NULL, 'workflow:process:update', NULL, 2, 1, 1),
 (9, 0, '客户关系', 'DIR', '/crm', NULL, NULL, 'User', 80, 1, 1),
 (91, 9, '客户线索', 'MENU', '/crm/leads', 'crm/leads/index', 'crm:lead:view', NULL, 1, 1, 1),
 (911, 91, '新增线索', 'BUTTON', NULL, NULL, 'crm:lead:create', NULL, 1, 1, 1),
 (912, 91, '修改线索', 'BUTTON', NULL, NULL, 'crm:lead:update', NULL, 2, 1, 1),
 (92, 9, '商机管理', 'MENU', '/crm/opportunities', 'crm/opportunities/index', 'crm:opportunity:view', NULL, 2, 1, 1),
 (921, 92, '新增商机', 'BUTTON', NULL, NULL, 'crm:opportunity:create', NULL, 1, 1, 1),
 (922, 92, '修改商机', 'BUTTON', NULL, NULL, 'crm:opportunity:update', NULL, 2, 1, 1),
 (93, 9, '跟进记录', 'MENU', '/crm/follow-ups', 'crm/followups/index', 'crm:followup:view', NULL, 3, 1, 1),
 (931, 93, '新增跟进', 'BUTTON', NULL, NULL, 'crm:followup:create', NULL, 1, 1, 1),
 (10, 0, '报表中心', 'DIR', '/report', NULL, NULL, 'DataAnalysis', 90, 1, 1),
 (101, 10, '销售报表', 'MENU', '/report/sales', 'report/sales/index', 'report:sales:view', NULL, 1, 1, 1),
 (102, 10, '采购报表', 'MENU', '/report/purchase', 'report/purchase/index', 'report:purchase:view', NULL, 2, 1, 1),
 (103, 10, '库存报表', 'MENU', '/report/inventory', 'report/inventory/index', 'report:inventory:view', NULL, 3, 1, 1);

-- ADMIN 拥有全部菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- 其他角色菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE id IN (1,34,341,342,6,61,611,612,613,62,621,622,9,91,911,912,92,921,922,93,931,10,101);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 3, id FROM sys_menu WHERE id IN (1,34,6,61,611,612,613,62,621,622,9,91,93,931,10,101);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 4, id FROM sys_menu WHERE id IN (1,35,351,352,353,5,51,511,512,513,52,521,522,10,102);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 5, id FROM sys_menu WHERE id IN (1,35,5,51,511,512,513,52,521,522,10,102);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 6, id FROM sys_menu WHERE id IN (1,31,4,41,411,412,413,42,43,431,432,44,441,442,443,45,103);
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 7, id FROM sys_menu WHERE id IN (1,7,71,72,73,731,732,74,741,742,75,751,752,10,101,102);

-- ---------- 字典 ----------
INSERT INTO sys_dict_type (id, code, name, status) VALUES
 (1, 'customer_level', '客户等级', 1),
 (2, 'payment_method', '支付方式', 1);
INSERT INTO sys_dict_item (dict_type_id, item_label, item_value, sort, status) VALUES
 (1, 'A类客户', 'A', 1, 1), (1, 'B类客户', 'B', 2, 1), (1, 'C类客户', 'C', 3, 1),
 (2, '银行转账', 'BANK', 1, 1), (2, '现金', 'CASH', 2, 1);

INSERT INTO sys_config (config_key, config_value, group_code, remark) VALUES
 ('company.name', '示例科技有限公司', 'basic', '公司名称'),
 ('company.tax_no', '91310000XXXXXXX', 'basic', '公司税号'),
 ('inventory.warning_days', '30', 'inventory', '临期预警天数');

-- ---------- 基础资料 ----------
INSERT INTO md_unit (id, code, name, precision_num, status) VALUES
 (1, 'PCS', '个', 0, 1),
 (2, 'BOX', '箱', 0, 1);
INSERT INTO md_product_category (id, parent_id, code, name, sort, status) VALUES
 (1, 0, 'ELEC', '电子产品', 1, 1),
 (2, 0, 'OFFICE', '办公用品', 2, 1);
INSERT INTO md_product (id, code, name, category_id, spec, unit_id, tax_rate, purchase_price, sale_price, safety_stock, status) VALUES
 (1, 'P001', '无线鼠标', 1, '2.4G 黑色', 1, 0.1300, 45.0000, 89.0000, 20, 1),
 (2, 'P002', '机械键盘', 1, '87键 青轴', 1, 0.1300, 180.0000, 299.0000, 10, 1),
 (3, 'P003', 'A4复印纸', 2, '70g 500张/包', 2, 0.1300, 18.0000, 32.0000, 50, 1),
 (4, 'P004', '中性签字笔', 2, '0.5mm 黑色', 1, 0.1300, 1.2000, 3.0000, 100, 1);
INSERT INTO md_customer (id, code, name, level, contact_name, phone, email, tax_no, address, credit_limit, status) VALUES
 (1, 'C001', '上海示例科技有限公司', 'A', '陈经理', '13900000001', 'c1@example.com', '91310101AAAAAAA', '上海市浦东新区示例路1号', 500000.0000, 1),
 (2, 'C002', '杭州演示贸易有限公司', 'B', '刘经理', '13900000002', 'c2@example.com', '91330101BBBBBBB', '杭州市西湖区演示路2号', 200000.0000, 1);
INSERT INTO md_supplier (id, code, name, contact_name, phone, email, tax_no, address, status) VALUES
 (1, 'S001', '深圳示例电子有限公司', '周经理', '13700000001', 's1@example.com', '91440300CCCCCCC', '深圳市南山区示例大道8号', 1),
 (2, 'S002', '江苏演示文具制造厂', '吴经理', '13700000002', 's2@example.com', '91320100DDDDDDD', '南京市江宁区演示工业园', 1);
INSERT INTO md_warehouse (id, code, name, address, manager, status) VALUES
 (1, 'WH01', '主仓库', '上海市青浦区物流园A区', '王五', 1);
INSERT INTO md_location (warehouse_id, code, name, status) VALUES (1, 'A-01', 'A区01货位', 1);

INSERT INTO fin_account (id, code, name, type, balance, status) VALUES
 (1, 'ACC01', '招商银行基本户', 'BANK', 1000000.0000, 1),
 (2, 'ACC02', '现金账户', 'CASH', 50000.0000, 1);

-- ---------- 审批流程 ----------
INSERT INTO wf_process (id, code, name, biz_type, enabled, remark) VALUES
 (1, 'PUR_ORDER_STD', '采购订单标准审批', 'PUR_ORDER', 1, '采购经理审批'),
 (2, 'SAL_ORDER_STD', '销售订单标准审批', 'SAL_ORDER', 1, '销售经理审批'),
 (3, 'EXPENSE_STD', '费用报销审批', 'EXPENSE', 1, '金额超过1000需财务复核');
INSERT INTO wf_node (id, process_id, seq, name, approver_type, approver_id, min_amount, max_amount) VALUES
 (1, 1, 1, '采购经理审批', 'ROLE', 4, 0.0000, 0.0000),
 (2, 2, 1, '销售经理审批', 'ROLE', 2, 0.0000, 0.0000),
 (3, 3, 1, '财务复核', 'ROLE', 7, 0.0000, 0.0000);

-- ---------- 期初库存 ----------
INSERT INTO inv_stock (product_id, warehouse_id, batch_no, qty_on_hand, qty_reserved, qty_frozen, avg_cost, version) VALUES
 (1, 1, '', 100.0000, 0, 0, 45.0000, 0),
 (2, 1, '', 50.0000, 0, 0, 180.0000, 0),
 (3, 1, '', 200.0000, 0, 0, 18.0000, 0),
 (4, 1, '', 500.0000, 0, 0, 1.2000, 0);
INSERT INTO inv_flow (flow_no, biz_type, biz_no, product_id, warehouse_id, batch_no, qty_change, qty_before, qty_after, unit_cost, amount, remark, operator_id) VALUES
 ('FL-OPEN-1', 'OTHER_IN', 'OPENING', 1, 1, '', 100.0000, 0, 100.0000, 45.0000, 4500.0000, '期初库存', 1),
 ('FL-OPEN-2', 'OTHER_IN', 'OPENING', 2, 1, '', 50.0000, 0, 50.0000, 180.0000, 9000.0000, '期初库存', 1),
 ('FL-OPEN-3', 'OTHER_IN', 'OPENING', 3, 1, '', 200.0000, 0, 200.0000, 18.0000, 3600.0000, '期初库存', 1),
 ('FL-OPEN-4', 'OTHER_IN', 'OPENING', 4, 1, '', 500.0000, 0, 500.0000, 1.2000, 600.0000, '期初库存', 1);
