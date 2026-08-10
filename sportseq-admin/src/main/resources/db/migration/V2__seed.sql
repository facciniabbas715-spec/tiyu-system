-- =====================================================================
-- 体育器材管理系统 种子数据 V2：字典/部门/角色/管理员/菜单/参数
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 数据字典
-- ---------------------------------------------------------------------

INSERT INTO sys_dict_type (id, dict_name, dict_type, status, create_time) VALUES
    (1, '用户类型',   'sys_user_type',    1, NOW()),
    (2, '计量单位',   'equipment_unit',   1, NOW()),
    (3, '器材状态',   'equipment_status', 1, NOW()),
    (4, '借用状态',   'borrow_status',    1, NOW()),
    (5, '归还器材状况', 'return_condition', 1, NOW()),
    (6, '库存变动类型', 'stock_change_type', 1, NOW()),
    (7, '通用状态',   'sys_status',       1, NOW());

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, list_class, is_default, status, create_time) VALUES
    (1,  '管理员', 'admin',   'sys_user_type', 'danger',  'N', 1, NOW()),
    (2,  '教师',   'teacher', 'sys_user_type', 'primary', 'N', 1, NOW()),
    (3,  '学生',   'student', 'sys_user_type', 'success', 'Y', 1, NOW()),
    (4,  '其他',   'other',   'sys_user_type', 'info',    'N', 1, NOW()),
    (1,  '个', '个', 'equipment_unit', '', 'Y', 1, NOW()),
    (2,  '副', '副', 'equipment_unit', '', 'N', 1, NOW()),
    (3,  '套', '套', 'equipment_unit', '', 'N', 1, NOW()),
    (4,  '条', '条', 'equipment_unit', '', 'N', 1, NOW()),
    (5,  '台', '台', 'equipment_unit', '', 'N', 1, NOW()),
    (6,  '箱', '箱', 'equipment_unit', '', 'N', 1, NOW()),
    (0,  '停用', '0', 'equipment_status', 'danger',  'N', 1, NOW()),
    (1,  '正常', '1', 'equipment_status', 'success', 'Y', 1, NOW()),
    (0,  '待审核',   '0', 'borrow_status', 'warning', 'N', 1, NOW()),
    (1,  '已通过',   '1', 'borrow_status', 'primary', 'N', 1, NOW()),
    (2,  '借用中',   '2', 'borrow_status', 'success', 'N', 1, NOW()),
    (3,  '部分归还', '3', 'borrow_status', '', 'N', 1, NOW()),
    (4,  '已归还',   '4', 'borrow_status', 'info',    'N', 1, NOW()),
    (5,  '已驳回',   '5', 'borrow_status', 'danger',  'N', 1, NOW()),
    (6,  '已取消',   '6', 'borrow_status', 'info',    'N', 1, NOW()),
    (1,  '完好',     '1', 'return_condition', 'success', 'Y', 1, NOW()),
    (2,  '轻微损坏', '2', 'return_condition', 'warning', 'N', 1, NOW()),
    (3,  '严重损坏', '3', 'return_condition', 'danger',  'N', 1, NOW()),
    (4,  '丢失',     '4', 'return_condition', 'danger',  'N', 1, NOW()),
    (1,  '入库',     '1', 'stock_change_type', 'success', 'N', 1, NOW()),
    (2,  '借用出库', '2', 'stock_change_type', 'primary', 'N', 1, NOW()),
    (3,  '归还入库', '3', 'stock_change_type', 'success', 'N', 1, NOW()),
    (4,  '报废出库', '4', 'stock_change_type', 'danger',  'N', 1, NOW()),
    (5,  '盘盈',     '5', 'stock_change_type', 'success', 'N', 1, NOW()),
    (6,  '盘亏',     '6', 'stock_change_type', 'warning', 'N', 1, NOW()),
    (7,  '锁定',     '7', 'stock_change_type', 'info',    'N', 1, NOW()),
    (8,  '解锁',     '8', 'stock_change_type', 'info',    'N', 1, NOW()),
    (9,  '领用核销', '9', 'stock_change_type', 'primary', 'N', 1, NOW()),
    (0,  '停用', '0', 'sys_status', 'danger',  'N', 1, NOW()),
    (1,  '正常', '1', 'sys_status', 'success', 'Y', 1, NOW());

-- ---------------------------------------------------------------------
-- 2. 部门与角色
-- ---------------------------------------------------------------------

INSERT INTO sys_dept (id, parent_id, ancestors, dept_name, order_num, status, create_time) VALUES
    (100, 0, '0', '总部', 0, 1, NOW()),
    (101, 100, '0,100', '体育部', 1, 1, NOW()),
    (102, 100, '0,100', '后勤部', 2, 1, NOW());

INSERT INTO sys_role (id, role_name, role_key, role_sort, data_scope, status, create_time) VALUES
    (1, '系统管理员', 'admin',            1, 1, 1, NOW()),
    (2, '器材管理员', 'equipment_admin',  2, 1, 1, NOW()),
    (3, '仓库管理员', 'warehouse_keeper', 3, 1, 1, NOW()),
    (4, '审批人员',   'approver',         4, 1, 1, NOW()),
    (5, '普通用户',   'common',           5, 2, 1, NOW());

-- 管理员账号：admin / admin123（BCrypt）
INSERT INTO sys_user (id, dept_id, username, password, real_name, user_type, status, pwd_update_date, create_time) VALUES
    (1, 100, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', 'admin', 1, NOW(), NOW());

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- ---------------------------------------------------------------------
-- 3. 菜单与权限
-- ---------------------------------------------------------------------

INSERT INTO sys_menu (id, menu_name, parent_id, order_num, menu_type, path, component, perms, icon, visible, status, create_time) VALUES
    -- 目录
    (1, '系统管理', 0, 1, 'M', '/system',    NULL, NULL, 'Setting',      1, 1, NOW()),
    (2, '器材管理', 0, 2, 'M', '/equipment', NULL, NULL, 'Trophy',       1, 1, NOW()),
    (3, '库存管理', 0, 3, 'M', '/stock',     NULL, NULL, 'Box',          1, 1, NOW()),
    (4, '借用归还', 0, 4, 'M', '/borrow',    NULL, NULL, 'Document',     1, 1, NOW()),
    (5, '报废管理', 0, 5, 'M', '/scrap',     NULL, NULL, 'Delete',       1, 1, NOW()),
    (6, '统计报表', 0, 6, 'M', '/statistics', NULL, NULL, 'DataAnalysis', 1, 1, NOW()),
    (7, '日志管理', 0, 7, 'M', '/monitor',   NULL, NULL, 'List',         1, 1, NOW()),
    -- 系统管理-菜单
    (100, '用户管理', 1, 1, 'C', 'user',   'system/user/index',   'system:user:list',   'User',   1, 1, NOW()),
    (101, '角色管理', 1, 2, 'C', 'role',   'system/role/index',   'system:role:list',   'UserFilled', 1, 1, NOW()),
    (102, '菜单管理', 1, 3, 'C', 'menu',   'system/menu/index',   'system:menu:list',   'Menu',   1, 1, NOW()),
    (103, '部门管理', 1, 4, 'C', 'dept',   'system/dept/index',   'system:dept:list',   'OfficeBuilding', 1, 1, NOW()),
    (104, '字典管理', 1, 5, 'C', 'dict',   'system/dict/index',   'system:dict:list',   'Notebook', 1, 1, NOW()),
    (105, '参数设置', 1, 6, 'C', 'config', 'system/config/index', 'system:config:list', 'Tools',  1, 1, NOW()),
    -- 器材管理-菜单
    (200, '器材分类', 2, 1, 'C', 'category', 'equipment/category/index', 'equipment:category:list', 'FolderOpened', 1, 1, NOW()),
    (201, '器材档案', 2, 2, 'C', 'equipment', 'equipment/equipment/index', 'equipment:list', 'TrophyBase', 1, 1, NOW()),
    (202, '仓库管理', 2, 3, 'C', 'warehouse', 'equipment/warehouse/index', 'equipment:warehouse:list', 'House', 1, 1, NOW()),
    -- 库存管理-菜单
    (300, '库存查询', 3, 1, 'C', 'stock',    'stock/stock/index',      'stock:list', 'Box', 1, 1, NOW()),
    (301, '库存流水', 3, 2, 'C', 'record',   'stock/stockRecord/index', 'stock:record:list', 'List', 1, 1, NOW()),
    (302, '入库单管理', 3, 3, 'C', 'in',     'stock/stockIn/index',    'stock:in:list', 'Download', 1, 1, NOW()),
    -- 借用归还-菜单
    (400, '借用单', 4, 1, 'C', 'borrow', 'business/borrow/index', 'borrow:list', 'Document', 1, 1, NOW()),
    (401, '归还单', 4, 2, 'C', 'return', 'business/return/index', 'return:list', 'Back', 1, 1, NOW()),
    -- 报废管理-菜单
    (500, '报废单', 5, 1, 'C', 'scrap', 'business/scrap/index', 'scrap:list', 'Delete', 1, 1, NOW()),
    -- 统计报表-菜单
    (600, '数据仪表盘', 6, 1, 'C', 'dashboard', 'dashboard/index',    'dashboard:view', 'Odometer', 1, 1, NOW()),
    (601, '统计报表', 6, 2, 'C', 'report',     'statistics/index',    'statistics:view', 'DataAnalysis', 1, 1, NOW()),
    -- 日志管理-菜单
    (700, '操作日志', 7, 1, 'C', 'operlog',   'monitor/operlog/index',   'monitor:operlog:list',  'EditPen', 1, 1, NOW()),
    (701, '登录日志', 7, 2, 'C', 'loginlog',  'monitor/loginlog/index',  'monitor:loginlog:list', 'Key', 1, 1, NOW());

-- 按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, order_num, menu_type, perms, status, create_time) VALUES
    (1001, '用户新增', 100, 1, 'F', 'system:user:add',       1, NOW()),
    (1002, '用户修改', 100, 2, 'F', 'system:user:edit',      1, NOW()),
    (1003, '用户删除', 100, 3, 'F', 'system:user:remove',    1, NOW()),
    (1004, '重置密码', 100, 4, 'F', 'system:user:resetPwd',  1, NOW()),
    (1011, '角色新增', 101, 1, 'F', 'system:role:add',       1, NOW()),
    (1012, '角色修改', 101, 2, 'F', 'system:role:edit',      1, NOW()),
    (1013, '角色删除', 101, 3, 'F', 'system:role:remove',    1, NOW()),
    (1014, '分配权限', 101, 4, 'F', 'system:role:assignMenu', 1, NOW()),
    (1021, '菜单新增', 102, 1, 'F', 'system:menu:add',       1, NOW()),
    (1022, '菜单修改', 102, 2, 'F', 'system:menu:edit',      1, NOW()),
    (1023, '菜单删除', 102, 3, 'F', 'system:menu:remove',    1, NOW()),
    (1031, '部门新增', 103, 1, 'F', 'system:dept:add',       1, NOW()),
    (1032, '部门修改', 103, 2, 'F', 'system:dept:edit',      1, NOW()),
    (1033, '部门删除', 103, 3, 'F', 'system:dept:remove',    1, NOW()),
    (1041, '字典新增', 104, 1, 'F', 'system:dict:add',       1, NOW()),
    (1042, '字典修改', 104, 2, 'F', 'system:dict:edit',      1, NOW()),
    (1043, '字典删除', 104, 3, 'F', 'system:dict:remove',    1, NOW()),
    (1051, '参数新增', 105, 1, 'F', 'system:config:add',     1, NOW()),
    (1052, '参数修改', 105, 2, 'F', 'system:config:edit',    1, NOW()),
    (1053, '参数删除', 105, 3, 'F', 'system:config:remove',  1, NOW()),
    (2001, '分类新增', 200, 1, 'F', 'equipment:category:add',    1, NOW()),
    (2002, '分类修改', 200, 2, 'F', 'equipment:category:edit',   1, NOW()),
    (2003, '分类删除', 200, 3, 'F', 'equipment:category:remove', 1, NOW()),
    (2011, '器材新增', 201, 1, 'F', 'equipment:add',    1, NOW()),
    (2012, '器材修改', 201, 2, 'F', 'equipment:edit',   1, NOW()),
    (2013, '器材删除', 201, 3, 'F', 'equipment:remove', 1, NOW()),
    (2014, '器材导入', 201, 4, 'F', 'equipment:import', 1, NOW()),
    (2015, '器材导出', 201, 5, 'F', 'equipment:export', 1, NOW()),
    (2021, '仓库新增', 202, 1, 'F', 'equipment:warehouse:add',    1, NOW()),
    (2022, '仓库修改', 202, 2, 'F', 'equipment:warehouse:edit',   1, NOW()),
    (2023, '仓库删除', 202, 3, 'F', 'equipment:warehouse:remove', 1, NOW()),
    (3001, '库存调整', 300, 1, 'F', 'stock:adjust',      1, NOW()),
    (3021, '入库单新增', 302, 1, 'F', 'stock:in:add',     1, NOW()),
    (3022, '入库单审核', 302, 2, 'F', 'stock:in:audit',   1, NOW()),
    (3023, '入库验收',   302, 3, 'F', 'stock:in:receive', 1, NOW()),
    (3024, '入库单删除', 302, 4, 'F', 'stock:in:remove',  1, NOW()),
    (4001, '借用申请', 400, 1, 'F', 'borrow:add',    1, NOW()),
    (4002, '借用审核', 400, 2, 'F', 'borrow:audit',  1, NOW()),
    (4003, '领用发放', 400, 3, 'F', 'borrow:issue',  1, NOW()),
    (4004, '续借申请', 400, 4, 'F', 'borrow:extend', 1, NOW()),
    (4005, '取消借用', 400, 5, 'F', 'borrow:cancel', 1, NOW()),
    (4011, '归还登记', 401, 1, 'F', 'return:add',     1, NOW()),
    (4012, '归还确认', 401, 2, 'F', 'return:confirm', 1, NOW()),
    (4013, '归还单删除', 401, 3, 'F', 'return:remove', 1, NOW()),
    (5001, '报废申请', 500, 1, 'F', 'scrap:add',     1, NOW()),
    (5002, '报废审核', 500, 2, 'F', 'scrap:audit',   1, NOW()),
    (5003, '报废处置', 500, 3, 'F', 'scrap:dispose', 1, NOW()),
    (6011, '报表导出', 601, 1, 'F', 'statistics:export', 1, NOW());

-- 系统管理员（role_id=1）授予全部菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

-- ---------------------------------------------------------------------
-- 4. 系统参数
-- ---------------------------------------------------------------------

INSERT INTO sys_config (config_name, config_key, config_value, config_type, remark, create_time) VALUES
    ('默认最长借用天数', 'borrow.max.days',       '15', 'Y', '器材未单独设置借用期限时使用', NOW()),
    ('逾期违约金(元/天)', 'borrow.overdue.penalty', '5',  'Y', '归还结算时计算违约金',        NOW()),
    ('续借次数上限',     'borrow.extend.limit',   '1',  'Y', '借用单累计续借次数',          NOW()),
    ('密码最小长度',     'sys.pwd.min.length',    '8',  'Y', '用户密码强度校验',            NOW()),
    ('登录验证码开关',   'sys.captcha.enabled',   'true', 'Y', '登录是否启用图形验证码',     NOW());
