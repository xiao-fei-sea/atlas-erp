-- =============================================================
-- V3 审批中心权限补齐：业务角色可查看待办与实例
-- =============================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.id IN (8, 81, 82)
 WHERE r.code IN ('SALES_MANAGER', 'SALES', 'PURCHASE_MANAGER', 'PURCHASE', 'WAREHOUSE', 'FINANCE');
