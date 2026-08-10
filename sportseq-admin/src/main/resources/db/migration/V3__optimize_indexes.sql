-- =====================================================================
-- 体育器材管理系统 数据库优化 V3：统计分析/仪表盘查询索引
-- 阶段 11：报表聚合与今日统计均为大表范围查询，补充时间与状态索引
-- =====================================================================

-- 借用单：领用时间（借用趋势/使用率/部门统计）、创建时间（今日统计）
ALTER TABLE borrow_order
    ADD KEY idx_borrow_issue_time (issue_time),
    ADD KEY idx_borrow_create_time (create_time);

-- 借用明细：状态 + 领用数量（使用率/部门统计过滤）
ALTER TABLE borrow_item
    ADD KEY idx_borrow_item_status (status, issued_quantity);

-- 归还单：确认时间（归还趋势/逾期统计）、创建时间（今日统计）
ALTER TABLE return_order
    ADD KEY idx_return_confirm_time (confirm_time),
    ADD KEY idx_return_create_time (create_time);

-- 入库单：创建时间（今日统计）
ALTER TABLE stock_in_order
    ADD KEY idx_in_create_time (create_time);

-- 报废单：创建时间（今日统计/待审统计）
ALTER TABLE scrap_order
    ADD KEY idx_scrap_create_time (create_time);

-- 库存流水：变动类型 + 操作时间（流水按类型过滤）
ALTER TABLE stock_record
    ADD KEY idx_record_change_type (change_type, create_time);
