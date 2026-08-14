-- =====================================================================
-- V5：RAG 知识库（V1/V2 保持不变，仅新增）
-- knowledge_document：文档元数据（逻辑删除）；knowledge_chunk：分块文本（随文档物理删除）
-- 向量数据存 Redis Stack（索引 sportseq-knowledge-index），MySQL 不存向量
-- =====================================================================

CREATE TABLE knowledge_document (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL COMMENT '文档标题',
    file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type   VARCHAR(20)  NOT NULL COMMENT '文件类型：txt/md/docx/pdf',
    file_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '文件字节数',
    file_path   VARCHAR(500) NULL COMMENT '本地存储相对路径',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0处理中 1已就绪 2失败',
    chunk_count INT          NOT NULL DEFAULT 0 COMMENT '分块数量',
    error_msg   VARCHAR(500) NULL COMMENT '处理失败原因',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建人',
    create_time DATETIME     NULL COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新人',
    update_time DATETIME     NULL COMMENT '更新时间',
    del_flag    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    KEY idx_kd_status (del_flag, status),
    KEY idx_kd_file_type (file_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库文档';

CREATE TABLE knowledge_chunk (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '所属文档ID',
    chunk_index INT    NOT NULL COMMENT '块序号，从0开始',
    content     TEXT   NOT NULL COMMENT '分块文本',
    token_count INT    NULL COMMENT 'token 数量',
    create_time DATETIME NULL COMMENT '创建时间',
    KEY idx_kc_document (document_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '知识库文档分块';

-- 知识库管理菜单（与 sys_menu.component=knowledge/index 对应，前端动态路由按该路径映射）
INSERT INTO sys_menu (id, menu_name, parent_id, order_num, menu_type, path, component, perms, icon, visible, status, create_time)
VALUES (801, '知识库', 0, 9, 'C', '/knowledge', 'knowledge/index', 'knowledge:manage', 'Reading', 1, 1, NOW());

-- 系统管理员（role_id=1）授予知识库管理权限；其他角色由管理员在“角色管理”中按需分配
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 801);
