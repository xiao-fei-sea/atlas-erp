-- =============================================================
-- V4 审批增强：会签、转交、超时提醒、按部门审批条件
-- =============================================================

ALTER TABLE wf_node
  ADD COLUMN sign_type VARCHAR(8) NOT NULL DEFAULT 'OR' COMMENT 'OR或签 AND会签' AFTER approver_id,
  ADD COLUMN approver_ids VARCHAR(255) DEFAULT NULL COMMENT '多审批人ID，逗号分隔' AFTER sign_type,
  ADD COLUMN dept_id BIGINT DEFAULT NULL COMMENT '限定发起人部门，NULL/0表示不限' AFTER approver_ids,
  ADD COLUMN timeout_hours INT NOT NULL DEFAULT 0 COMMENT '审批超时小时数，0表示不限' AFTER dept_id;

ALTER TABLE wf_task
  ADD COLUMN sign_type VARCHAR(8) NOT NULL DEFAULT 'OR' COMMENT 'OR或签 AND会签' AFTER node_name,
  ADD COLUMN origin_approver_id BIGINT DEFAULT NULL COMMENT '原审批人ID' AFTER approver_role,
  ADD COLUMN transferred_by BIGINT DEFAULT NULL COMMENT '转交操作人ID' AFTER origin_approver_id,
  ADD COLUMN transferred_at DATETIME DEFAULT NULL COMMENT '转交时间' AFTER transferred_by,
  ADD COLUMN due_at DATETIME DEFAULT NULL COMMENT '审批截止时间' AFTER transferred_at,
  ADD COLUMN overdue TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1超时' AFTER due_at;

ALTER TABLE wf_instance
  ADD COLUMN submitter_dept_id BIGINT DEFAULT NULL COMMENT '发起人部门ID' AFTER submitter_id;

-- 转交任务权限
INSERT IGNORE INTO sys_menu (id, parent_id, name, type, permission, sort, visible, status) VALUES
 (811, 81, '转交任务', 'BUTTON', 'workflow:task:transfer', 2, 1, 1);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, 811
  FROM sys_role r
 WHERE r.code IN ('ADMIN', 'SALES_MANAGER', 'SALES', 'PURCHASE_MANAGER', 'PURCHASE', 'WAREHOUSE', 'FINANCE');
