-- =====================================================================
-- 体育器材管理系统 数据库初始化脚本 V1：24 张表
-- 设计依据：docs/design/体育器材管理系统-设计文档.md 第 3 章
-- =====================================================================

-- ---------------------------------------------------------------------
-- 一、系统权限域
-- ---------------------------------------------------------------------

CREATE TABLE sys_dept (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID，顶级为0',
    ancestors   VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祖先路径，如 0,1,5',
    dept_name   VARCHAR(50)  NOT NULL COMMENT '部门名称',
    order_num   INT          NOT NULL DEFAULT 0 COMMENT '显示排序',
    leader      VARCHAR(50)  NULL COMMENT '负责人',
    phone       VARCHAR(20)  NULL COMMENT '联系电话',
    email       VARCHAR(100) NULL COMMENT '邮箱',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    del_flag    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by   BIGINT       NULL COMMENT '创建人',
    create_time DATETIME     NULL COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新人',
    update_time DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dept_parent (parent_id),
    KEY idx_dept_name (dept_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '部门表';

CREATE TABLE sys_user (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    dept_id           BIGINT       NULL COMMENT '部门ID',
    username          VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password          VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
    nickname          VARCHAR(50)  NULL COMMENT '昵称',
    real_name         VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    phone             VARCHAR(20)  NULL COMMENT '手机号',
    email             VARCHAR(100) NULL COMMENT '邮箱',
    gender            TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    user_type         VARCHAR(20)  NOT NULL DEFAULT 'student' COMMENT '用户类型：admin/teacher/student/other',
    avatar            VARCHAR(255) NULL COMMENT '头像地址',
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    login_ip          VARCHAR(50)  NULL COMMENT '最后登录IP',
    login_date        DATETIME     NULL COMMENT '最后登录时间',
    pwd_update_date   DATETIME     NULL COMMENT '密码最后修改时间',
    login_fail_count  INT          NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    lock_time         DATETIME     NULL COMMENT '账号锁定截止时间',
    remark            VARCHAR(500) NULL COMMENT '备注',
    del_flag          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by         BIGINT       NULL COMMENT '创建人',
    create_time       DATETIME     NULL COMMENT '创建时间',
    update_by         BIGINT       NULL COMMENT '更新人',
    update_time       DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_dept (dept_id),
    KEY idx_user_phone (phone),
    KEY idx_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    role_key    VARCHAR(50)  NOT NULL COMMENT '角色权限字符',
    role_sort   INT          NOT NULL DEFAULT 0 COMMENT '显示排序',
    data_scope  TINYINT      NOT NULL DEFAULT 1 COMMENT '数据权限：1全部 2本部门 3本部门及以下 4仅本人',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    remark      VARCHAR(500) NULL COMMENT '备注',
    del_flag    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by   BIGINT       NULL COMMENT '创建人',
    create_time DATETIME     NULL COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新人',
    update_time DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表';

CREATE TABLE sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID，顶级为0',
    order_num   INT          NOT NULL DEFAULT 0 COMMENT '显示排序',
    menu_type   CHAR(1)      NOT NULL COMMENT '类型：M目录 C菜单 F按钮',
    path        VARCHAR(200) NULL COMMENT '路由地址',
    component   VARCHAR(255) NULL COMMENT '组件路径',
    query       VARCHAR(255) NULL COMMENT '路由参数',
    perms       VARCHAR(100) NULL COMMENT '权限标识',
    icon        VARCHAR(100) NULL COMMENT '图标',
    visible     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示：0隐藏 1显示',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    create_by   BIGINT       NULL COMMENT '创建人',
    create_time DATETIME     NULL COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新人',
    update_time DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_menu_parent (parent_id),
    KEY idx_menu_perms (perms)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单/权限表';

CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户-角色关联表';

CREATE TABLE sys_role_menu (
    id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id),
    KEY idx_menu_id (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色-菜单关联表';

CREATE TABLE sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
    dict_name   VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建人',
    create_time DATETIME     NULL COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新人',
    update_time DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典类型表';

CREATE TABLE sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
    dict_sort   INT          NOT NULL DEFAULT 0 COMMENT '排序',
    dict_label  VARCHAR(100) NOT NULL COMMENT '显示标签',
    dict_value  VARCHAR(100) NOT NULL COMMENT '字典值',
    dict_type   VARCHAR(100) NOT NULL COMMENT '所属字典类型',
    css_class   VARCHAR(100) NULL COMMENT '样式类',
    list_class  VARCHAR(100) NULL COMMENT '列表样式',
    is_default  CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否默认：Y/N',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建人',
    create_time DATETIME     NULL COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新人',
    update_time DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_dict_data_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '字典数据表';

CREATE TABLE sys_config (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '参数ID',
    config_name  VARCHAR(100) NOT NULL COMMENT '参数名称',
    config_key   VARCHAR(100) NOT NULL COMMENT '参数键',
    config_value VARCHAR(500) NOT NULL COMMENT '参数值',
    config_type  CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否内置：Y内置 N自定义',
    remark       VARCHAR(500) NULL COMMENT '备注',
    create_by    BIGINT       NULL COMMENT '创建人',
    create_time  DATETIME     NULL COMMENT '创建时间',
    update_by    BIGINT       NULL COMMENT '更新人',
    update_time  DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统参数表';

-- ---------------------------------------------------------------------
-- 二、日志审计域
-- ---------------------------------------------------------------------

CREATE TABLE sys_login_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_name      VARCHAR(50)  NOT NULL COMMENT '登录账号',
    ipaddr         VARCHAR(50)  NULL COMMENT '登录IP',
    login_location VARCHAR(100) NULL COMMENT '登录地点',
    browser        VARCHAR(50)  NULL COMMENT '浏览器',
    os             VARCHAR(50)  NULL COMMENT '操作系统',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '登录状态：0失败 1成功',
    msg            VARCHAR(255) NULL COMMENT '提示消息',
    login_time     DATETIME     NOT NULL COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_login_time (login_time),
    KEY idx_login_user (user_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '登录日志表';

CREATE TABLE sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    title          VARCHAR(50)  NULL COMMENT '模块标题',
    business_type  TINYINT      NOT NULL DEFAULT 0 COMMENT '业务类型：0其它 1新增 2修改 3删除 4审核 5导出 6导入 7领用 8归还',
    method         VARCHAR(100) NULL COMMENT '方法名',
    request_method VARCHAR(10)  NULL COMMENT 'HTTP方法',
    oper_name      VARCHAR(50)  NULL COMMENT '操作人',
    dept_name      VARCHAR(50)  NULL COMMENT '操作人部门',
    oper_url       VARCHAR(255) NULL COMMENT '请求地址',
    oper_ip        VARCHAR(50)  NULL COMMENT '操作IP',
    oper_location  VARCHAR(100) NULL COMMENT '操作地点',
    oper_param     TEXT         NULL COMMENT '请求参数(JSON脱敏)',
    json_result    TEXT         NULL COMMENT '返回结果(JSON)',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '操作状态：0正常 1异常',
    error_msg      TEXT         NULL COMMENT '异常信息',
    cost_time      BIGINT       NOT NULL DEFAULT 0 COMMENT '耗时(毫秒)',
    oper_time      DATETIME     NOT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_oper_time (oper_time),
    KEY idx_oper_name (oper_name),
    KEY idx_business_type (business_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表';

-- ---------------------------------------------------------------------
-- 三、器材基础资料域
-- ---------------------------------------------------------------------

CREATE TABLE equipment_category (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '父分类ID，顶级为0',
    ancestors     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祖先路径，如 0,1,5',
    category_name VARCHAR(50)  NOT NULL COMMENT '分类名称',
    category_code VARCHAR(20)  NOT NULL COMMENT '分类编码，作为器材编码前缀',
    icon          VARCHAR(100) NULL COMMENT '图标',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    remark        VARCHAR(500) NULL COMMENT '备注',
    del_flag      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by     BIGINT       NULL COMMENT '创建人',
    create_time   DATETIME     NULL COMMENT '创建时间',
    update_by     BIGINT       NULL COMMENT '更新人',
    update_time   DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (category_code),
    KEY idx_category_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '器材分类表';

CREATE TABLE equipment (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '器材ID',
    equipment_code  VARCHAR(50)   NOT NULL COMMENT '器材编码，全局唯一',
    equipment_name  VARCHAR(100)  NOT NULL COMMENT '器材名称',
    category_id     BIGINT        NOT NULL COMMENT '分类ID',
    brand           VARCHAR(100)  NULL COMMENT '品牌',
    model           VARCHAR(100)  NULL COMMENT '型号',
    spec            VARCHAR(100)  NULL COMMENT '规格',
    unit            VARCHAR(20)   NOT NULL COMMENT '计量单位',
    purchase_price  DECIMAL(10,2) NULL COMMENT '采购单价',
    safe_stock      INT           NOT NULL DEFAULT 0 COMMENT '安全库存阈值',
    max_borrow_days INT           NULL COMMENT '最长借用天数',
    image_url       VARCHAR(255)  NULL COMMENT '主图地址',
    status          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    description     VARCHAR(500)  NULL COMMENT '描述',
    del_flag        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by       BIGINT        NULL COMMENT '创建人',
    create_time     DATETIME      NULL COMMENT '创建时间',
    update_by       BIGINT        NULL COMMENT '更新人',
    update_time     DATETIME      NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_equipment_code (equipment_code),
    KEY idx_equipment_category (category_id),
    KEY idx_equipment_name (equipment_name),
    KEY idx_equipment_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '器材信息表';

CREATE TABLE warehouse (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
    warehouse_code VARCHAR(20)  NOT NULL COMMENT '仓库编码',
    warehouse_name VARCHAR(50)  NOT NULL COMMENT '仓库名称',
    manager        BIGINT       NULL COMMENT '负责人',
    phone          VARCHAR(20)  NULL COMMENT '联系电话',
    address        VARCHAR(200) NULL COMMENT '仓库地址',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0停用 1正常',
    remark         VARCHAR(500) NULL COMMENT '备注',
    del_flag       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by      BIGINT       NULL COMMENT '创建人',
    create_time    DATETIME     NULL COMMENT '创建时间',
    update_by      BIGINT       NULL COMMENT '更新人',
    update_time    DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (warehouse_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '仓库表';

-- ---------------------------------------------------------------------
-- 四、库存域
-- ---------------------------------------------------------------------

CREATE TABLE equipment_stock (
    id              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    equipment_id    BIGINT   NOT NULL COMMENT '器材ID',
    warehouse_id    BIGINT   NOT NULL COMMENT '仓库ID',
    quantity        INT      NOT NULL DEFAULT 0 COMMENT '当前在库可用数量',
    locked_quantity INT      NOT NULL DEFAULT 0 COMMENT '锁定数量(审批通过待领用)',
    version         INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    update_by       BIGINT   NULL COMMENT '最后变更人',
    update_time     DATETIME NULL COMMENT '最后变更时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_equip_warehouse (equipment_id, warehouse_id),
    KEY idx_stock_warehouse (warehouse_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '库存表(器材×仓库)';

CREATE TABLE stock_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    equipment_id    BIGINT       NOT NULL COMMENT '器材ID',
    warehouse_id    BIGINT       NOT NULL COMMENT '仓库ID',
    change_type     TINYINT      NOT NULL COMMENT '变动类型：1入库 2借用出库 3归还入库 4报废出库 5盘盈 6盘亏 7锁定 8解锁 9领用核销',
    change_quantity INT          NOT NULL COMMENT '变动数量(正增负减)',
    before_quantity INT          NOT NULL COMMENT '变动前在库数量',
    after_quantity  INT          NOT NULL COMMENT '变动后在库数量',
    ref_order_type  VARCHAR(20)  NULL COMMENT '来源单据类型',
    ref_order_no    VARCHAR(50)  NULL COMMENT '来源单据号',
    remark          VARCHAR(255) NULL COMMENT '备注',
    create_by       BIGINT       NULL COMMENT '操作人',
    create_time     DATETIME     NOT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_record_equipment (equipment_id, create_time),
    KEY idx_record_order (ref_order_no),
    KEY idx_record_warehouse (warehouse_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '库存流水表';

-- ---------------------------------------------------------------------
-- 五、业务单据域
-- ---------------------------------------------------------------------

CREATE TABLE stock_in_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
    order_no        VARCHAR(50)   NOT NULL COMMENT '入库单号(RK前缀)',
    warehouse_id    BIGINT        NOT NULL COMMENT '目标仓库ID',
    supplier        VARCHAR(100)  NULL COMMENT '供应商',
    in_type         TINYINT       NOT NULL DEFAULT 1 COMMENT '入库类型：1采购 2退货退回 3盘盈 4其他',
    total_quantity  INT           NOT NULL DEFAULT 0 COMMENT '总数量',
    total_amount    DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '总金额',
    status          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1待审核 2已通过待验收 3已验收 4已驳回 5已作废',
    audit_by        BIGINT        NULL COMMENT '审核人',
    audit_time      DATETIME      NULL COMMENT '审核时间',
    audit_remark    VARCHAR(255)  NULL COMMENT '审核意见',
    receive_by      BIGINT        NULL COMMENT '验收人',
    receive_time    DATETIME      NULL COMMENT '验收时间',
    remark          VARCHAR(500)  NULL COMMENT '备注',
    del_flag        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by       BIGINT        NULL COMMENT '创建人',
    create_time     DATETIME      NULL COMMENT '创建时间',
    update_by       BIGINT        NULL COMMENT '更新人',
    update_time     DATETIME      NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_in_order_no (order_no),
    KEY idx_in_order_status (status, create_time),
    KEY idx_in_order_warehouse (warehouse_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '入库单主表';

CREATE TABLE stock_in_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    order_id     BIGINT        NOT NULL COMMENT '入库单ID',
    equipment_id BIGINT        NOT NULL COMMENT '器材ID',
    quantity     INT           NOT NULL COMMENT '入库数量',
    unit_price   DECIMAL(10,2) NULL COMMENT '单价',
    amount       DECIMAL(12,2) NULL COMMENT '金额',
    remark       VARCHAR(255)  NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_in_item_order (order_id),
    KEY idx_in_item_equipment (equipment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '入库单明细表';

CREATE TABLE borrow_order (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '借用单ID',
    order_no             VARCHAR(50)  NOT NULL COMMENT '借用单号(JY前缀)',
    user_id              BIGINT       NOT NULL COMMENT '借用人ID',
    dept_id              BIGINT       NULL COMMENT '借用人部门(快照)',
    borrow_type          TINYINT      NOT NULL DEFAULT 1 COMMENT '借用类型：1个人借用 2集体领用',
    purpose              VARCHAR(200) NOT NULL COMMENT '借用用途',
    expected_return_date DATE         NOT NULL COMMENT '预计归还日期',
    total_quantity       INT          NOT NULL DEFAULT 0 COMMENT '借用总数量',
    status               TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已通过待领用 2借用中 3部分归还 4已全部归还 5已驳回 6已取消',
    locked               TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已锁定库存：0否 1是',
    audit_by             BIGINT       NULL COMMENT '审核人',
    audit_time           DATETIME     NULL COMMENT '审核时间',
    audit_remark         VARCHAR(255) NULL COMMENT '审核意见',
    issue_by             BIGINT       NULL COMMENT '领用发放人',
    issue_time           DATETIME     NULL COMMENT '发放时间',
    extend_count         INT          NOT NULL DEFAULT 0 COMMENT '已续借次数',
    remark               VARCHAR(500) NULL COMMENT '备注',
    del_flag             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by            BIGINT       NULL COMMENT '创建人',
    create_time          DATETIME     NULL COMMENT '创建时间',
    update_by            BIGINT       NULL COMMENT '更新人',
    update_time          DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_borrow_order_no (order_no),
    KEY idx_borrow_user (user_id, status),
    KEY idx_borrow_status (status, create_time),
    KEY idx_borrow_expected (expected_return_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '借用单主表';

CREATE TABLE borrow_item (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    borrow_id            BIGINT       NOT NULL COMMENT '借用单ID',
    equipment_id         BIGINT       NOT NULL COMMENT '器材ID',
    warehouse_id         BIGINT       NOT NULL COMMENT '领用仓库ID',
    quantity             INT          NOT NULL COMMENT '申请数量',
    issued_quantity      INT          NOT NULL DEFAULT 0 COMMENT '实际领用数量',
    returned_quantity    INT          NOT NULL DEFAULT 0 COMMENT '已归还数量',
    expected_return_date DATE         NOT NULL COMMENT '预计归还日期(快照)',
    actual_return_date   DATE         NULL COMMENT '实际全部归还日期',
    overdue_flag         TINYINT      NOT NULL DEFAULT 0 COMMENT '是否逾期：0否 1是',
    overdue_days         INT          NOT NULL DEFAULT 0 COMMENT '逾期天数',
    status               TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待领用 1借用中 2部分归还 3已归还 4已挂失',
    remark               VARCHAR(255) NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_borrow_item_order (borrow_id),
    KEY idx_borrow_item_equipment (equipment_id, status),
    KEY idx_borrow_item_warehouse (warehouse_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '借用单明细表';

CREATE TABLE return_order (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '归还单ID',
    order_no         VARCHAR(50)   NOT NULL COMMENT '归还单号(GH前缀)',
    borrow_order_id  BIGINT        NOT NULL COMMENT '关联借用单ID',
    user_id          BIGINT        NOT NULL COMMENT '归还人ID',
    warehouse_id     BIGINT        NOT NULL COMMENT '归还仓库ID',
    total_quantity   INT           NOT NULL DEFAULT 0 COMMENT '归还总数量',
    return_type      TINYINT       NOT NULL DEFAULT 1 COMMENT '归还类型：1正常 2逾期 3损坏 4丢失',
    status           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0待确认 1已确认 2已驳回 3已作废',
    confirm_by       BIGINT        NULL COMMENT '验收确认人',
    confirm_time     DATETIME      NULL COMMENT '确认时间',
    confirm_remark   VARCHAR(255)  NULL COMMENT '确认意见',
    remark           VARCHAR(500)  NULL COMMENT '备注',
    del_flag         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by        BIGINT        NULL COMMENT '创建人',
    create_time      DATETIME      NULL COMMENT '创建时间',
    update_by        BIGINT        NULL COMMENT '更新人',
    update_time      DATETIME      NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_return_order_no (order_no),
    KEY idx_return_borrow (borrow_order_id),
    KEY idx_return_user (user_id),
    KEY idx_return_status (status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '归还单主表';

CREATE TABLE return_item (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    return_id        BIGINT        NOT NULL COMMENT '归还单ID',
    borrow_item_id   BIGINT        NOT NULL COMMENT '借用明细ID',
    equipment_id     BIGINT        NOT NULL COMMENT '器材ID',
    quantity         INT           NOT NULL COMMENT '归还数量',
    condition_status TINYINT       NOT NULL DEFAULT 1 COMMENT '器材状况：1完好 2轻微损坏 3严重损坏 4丢失',
    damage_desc      VARCHAR(500)  NULL COMMENT '损坏/丢失说明',
    is_overdue       TINYINT       NOT NULL DEFAULT 0 COMMENT '是否逾期：0否 1是',
    overdue_days     INT           NOT NULL DEFAULT 0 COMMENT '逾期天数',
    penalty_amount   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '违约金/赔偿金额',
    remark           VARCHAR(255)  NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_return_item_return (return_id),
    KEY idx_return_item_borrow (borrow_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '归还单明细表';

CREATE TABLE scrap_order (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '报废单ID',
    order_no          VARCHAR(50)   NOT NULL COMMENT '报废单号(BF前缀)',
    warehouse_id      BIGINT        NOT NULL COMMENT '所属仓库ID',
    scrap_type        TINYINT       NOT NULL DEFAULT 1 COMMENT '报废类型：1自然损耗 2损坏报废 3到期报废 4其他',
    total_quantity    INT           NOT NULL DEFAULT 0 COMMENT '报废总数量',
    total_loss_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '损失总金额',
    status            TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已审核待处置 2已处置 3已驳回 4已作废',
    audit_by          BIGINT        NULL COMMENT '审核人',
    audit_time        DATETIME      NULL COMMENT '审核时间',
    audit_remark      VARCHAR(255)  NULL COMMENT '审核意见',
    dispose_by        BIGINT        NULL COMMENT '处置执行人',
    dispose_time      DATETIME      NULL COMMENT '处置时间',
    dispose_method    TINYINT       NULL COMMENT '处置方式：1销毁 2变卖 3捐赠 4回收',
    remark            VARCHAR(500)  NULL COMMENT '备注',
    del_flag          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
    create_by         BIGINT        NULL COMMENT '创建人',
    create_time       DATETIME      NULL COMMENT '创建时间',
    update_by         BIGINT        NULL COMMENT '更新人',
    update_time       DATETIME      NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_scrap_order_no (order_no),
    KEY idx_scrap_status (status, create_time),
    KEY idx_scrap_warehouse (warehouse_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '报废单主表';

CREATE TABLE scrap_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    scrap_id     BIGINT        NOT NULL COMMENT '报废单ID',
    equipment_id BIGINT        NOT NULL COMMENT '器材ID',
    quantity     INT           NOT NULL COMMENT '报废数量',
    scrap_reason VARCHAR(255)  NOT NULL COMMENT '报废原因',
    loss_amount  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '损失金额',
    remark       VARCHAR(255)  NULL COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_scrap_item_scrap (scrap_id),
    KEY idx_scrap_item_equipment (equipment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '报废单明细表';
