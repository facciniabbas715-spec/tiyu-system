-- =====================================================================
-- V4：AI 智能客服菜单（V1/V2 保持不变，仅新增）
-- =====================================================================

INSERT INTO sys_menu (id, menu_name, parent_id, order_num, menu_type, path, component, perms, icon, visible, status, create_time)
VALUES (800, 'AI客服', 0, 8, 'C', '/ai', 'ai/index', 'ai:chat', 'ChatDotRound', 1, 1, NOW());

-- 系统管理员（role_id=1）授予 AI 客服权限；其他角色由管理员在“角色管理”中按需分配
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 800);
