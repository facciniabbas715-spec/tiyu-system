package com.company.sportseq.controller;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 统计分析模块集成测试：仪表盘汇总 / 各报表数据 / Excel 导出。
 */
@SpringBootTest
@AutoConfigureMockMvc
class StatisticTest {

    private static final String UUID = "stat-uuid";
    private static final String CODE = "1234";
    private static final String CAT_A = "STAT-A";
    private static final String CAT_B = "STAT-B";
    private static final String EQ_A = "EQ-STAT-A";
    private static final String EQ_B = "EQ-STAT-B";
    private static final String WH_A = "WH-STAT-A";
    private static final String WH_B = "WH-STAT-B";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void summary_shouldReturnTodayCountsAndTotalValue() throws Exception {
        String token = loginAdmin();
        try {
            seed();
            MvcResult result = mockMvc.perform(get("/api/dashboard/summary")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            Map<String, Object> data = JSONUtil.parseObj(result.getResponse().getContentAsString())
                    .get("data", Map.class);
            assertEquals(2L, ((Number) data.get("todayBorrowCount")).longValue());
            assertEquals(2L, ((Number) data.get("todayReturnCount")).longValue());
            assertEquals(3L, ((Number) data.get("todayStockInCount")).longValue());
            assertEquals(1L, ((Number) data.get("warningStockCount")).longValue());
            assertEquals(1L, ((Number) data.get("pendingScrapCount")).longValue());
            assertEquals(2L, ((Number) data.get("equipmentTotal")).longValue());
            assertEquals(0, new BigDecimal(data.get("stockTotalValue").toString()).compareTo(new BigDecimal("900.00")));
        } finally {
            cleanup();
        }
    }

    @Test
    void reports_shouldReturnChartData() throws Exception {
        String token = loginAdmin();
        try {
            seed();

            // 库存分布：分类B 12 > 分类A 3
            MvcResult category = mockMvc.perform(get("/api/statistics/category-stock")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            List<Map<String, Object>> categoryRows = JSONUtil.parseObj(category.getResponse().getContentAsString())
                    .getJSONArray("data").stream().map(item -> (Map<String, Object>) item).toList();
            assertEquals(2, categoryRows.size());
            assertEquals(12L, ((Number) categoryRows.get(0).get("quantity")).longValue());
            assertEquals(3L, ((Number) categoryRows.get(1).get("quantity")).longValue());

            // 各仓库库存量：仓库A 9 > 仓库B 6
            MvcResult warehouse = mockMvc.perform(get("/api/statistics/warehouse-stock")
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            List<Map<String, Object>> warehouseRows = JSONUtil.parseObj(warehouse.getResponse().getContentAsString())
                    .getJSONArray("data").stream().map(item -> (Map<String, Object>) item).toList();
            assertEquals(9L, ((Number) warehouseRows.get(0).get("quantity")).longValue());
            assertEquals(6L, ((Number) warehouseRows.get(1).get("quantity")).longValue());

            // 借用趋势：默认近12个月，本月借用3/归还4，上月借用2/归还2
            MvcResult trend = mockMvc.perform(get("/api/statistics/borrow-trend")
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            List<Map<String, Object>> trendRows = JSONUtil.parseObj(trend.getResponse().getContentAsString())
                    .getJSONArray("data").stream().map(item -> (Map<String, Object>) item).toList();
            assertEquals(12, trendRows.size());
            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            Map<String, Object> current = trendRows.stream()
                    .filter(row -> currentMonth.equals(row.get("month")))
                    .findFirst().orElseThrow();
            Map<String, Object> last = trendRows.stream()
                    .filter(row -> lastMonth.equals(row.get("month")))
                    .findFirst().orElseThrow();
            assertEquals(4L, ((Number) current.get("borrowQuantity")).longValue());
            assertEquals(4L, ((Number) current.get("returnQuantity")).longValue());
            assertEquals(2L, ((Number) last.get("borrowQuantity")).longValue());
            assertEquals(2L, ((Number) last.get("returnQuantity")).longValue());

            // 器材使用率 TOP10：篮球3次 > 足球2次
            MvcResult usage = mockMvc.perform(get("/api/statistics/equipment-usage")
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            List<Map<String, Object>> usageRows = JSONUtil.parseObj(usage.getResponse().getContentAsString())
                    .getJSONArray("data").stream().map(item -> (Map<String, Object>) item).toList();
            assertEquals(EQ_A, usageRows.get(0).get("equipmentCode"));
            assertEquals(3L, ((Number) usageRows.get(0).get("borrowCount")).longValue());
            assertEquals(1L, ((Number) usageRows.get(1).get("borrowCount")).longValue());

            // 部门借用统计：本月 2 单 / 4 件，在借 2 件
            String start = LocalDate.now().withDayOfMonth(1).format(DAY);
            String end = LocalDate.now().format(DAY);
            MvcResult dept = mockMvc.perform(get("/api/statistics/dept-borrow")
                            .header("Authorization", "Bearer " + token)
                            .param("startDate", start).param("endDate", end))
                    .andReturn();
            List<Map<String, Object>> deptRows = JSONUtil.parseObj(dept.getResponse().getContentAsString())
                    .getJSONArray("data").stream().map(item -> (Map<String, Object>) item).toList();
            assertEquals(2L, ((Number) deptRows.get(0).get("borrowCount")).longValue());
            assertEquals(4L, ((Number) deptRows.get(0).get("borrowQuantity")).longValue());
            assertEquals(3L, ((Number) deptRows.get(0).get("outstandingQuantity")).longValue());

            // 逾期统计：1 单 / 2 明细 / 平均3天 / 违约金30
            MvcResult overdue = mockMvc.perform(get("/api/statistics/overdue")
                            .header("Authorization", "Bearer " + token))
                    .andReturn();
            Map<String, Object> overdueData = JSONUtil.parseObj(overdue.getResponse().getContentAsString())
                    .get("data", Map.class);
            assertEquals(1L, ((Number) overdueData.get("overdueOrderCount")).longValue());
            assertEquals(2L, ((Number) overdueData.get("overdueItemCount")).longValue());
            assertEquals(0, new BigDecimal(overdueData.get("avgOverdueDays").toString())
                    .compareTo(new BigDecimal("3.0")));
            assertEquals(0, new BigDecimal(overdueData.get("penaltyTotal").toString())
                    .compareTo(new BigDecimal("30")));
        } finally {
            cleanup();
        }
    }

    @Test
    void export_shouldReturnExcelFile() throws Exception {
        String token = loginAdmin();
        try {
            seed();
            MvcResult result = mockMvc.perform(get("/api/statistics/export")
                            .header("Authorization", "Bearer " + token)
                            .param("type", "usage"))
                    .andReturn();
            String contentType = result.getResponse().getContentType();
            assertTrue(contentType != null && contentType.contains("spreadsheetml"));
            assertTrue(result.getResponse().getContentAsByteArray().length > 1000);
        } finally {
            cleanup();
        }
    }

    @Test
    void exportLog_shouldSkipServletResponse() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(get("/api/statistics/export")
                            .header("Authorization", "Bearer " + token)
                            .param("type", "usage"))
                    .andReturn();
            Thread.sleep(500); // 等待异步日志落库
            String param = jdbcTemplate.queryForObject(
                    "SELECT oper_param FROM sys_oper_log WHERE title = '统计报表导出' ORDER BY id DESC LIMIT 1",
                    String.class);
            if (param == null || param.length() > 500 || param.contains("PK") || !param.contains("usage")) {
                throw new AssertionError("操作日志不应包含二进制导出内容: " + param);
            }
        } finally {
            jdbcTemplate.update(
                    "DELETE FROM sys_oper_log WHERE title = '统计报表导出' AND oper_param LIKE '%usage%'");
        }
    }

    private void seed() {
        long catA = insertCategory(CAT_A, "统计测试-球类");
        long catB = insertCategory(CAT_B, "统计测试-田径");
        long eqA = insertEquipment(EQ_A, "统计测试-篮球", catA, "100.00", 5);
        long eqB = insertEquipment(EQ_B, "统计测试-足球", catB, "50.00", 5);
        long whA = insertWarehouse(WH_A, "统计测试-仓库A");
        long whB = insertWarehouse(WH_B, "统计测试-仓库B");

        // 库存：A 3 个（预警），B 6+6=12 个（不预警）
        jdbcTemplate.update("""
                INSERT INTO equipment_stock (equipment_id, warehouse_id, quantity, locked_quantity, version, update_time)
                VALUES (?, ?, ?, 0, 0, NOW())""", eqA, whA, 3);
        jdbcTemplate.update("""
                INSERT INTO equipment_stock (equipment_id, warehouse_id, quantity, locked_quantity, version, update_time)
                VALUES (?, ?, ?, 0, 0, NOW())""", eqB, whA, 6);
        jdbcTemplate.update("""
                INSERT INTO equipment_stock (equipment_id, warehouse_id, quantity, locked_quantity, version, update_time)
                VALUES (?, ?, ?, 0, 0, NOW())""", eqB, whB, 6);

        // 入库单：今日3单（1待审核/2已通过/3已验收），昨日1单不计入
        for (int i = 1; i <= 3; i++) {
            jdbcTemplate.update("""
                    INSERT INTO stock_in_order (order_no, warehouse_id, in_type, total_quantity, total_amount, status, del_flag, create_time)
                    VALUES (?, ?, 1, 1, 100, ?, 0, NOW())""", "RK-STAT-" + i, whA, i);
        }
        jdbcTemplate.update("""
                INSERT INTO stock_in_order (order_no, warehouse_id, in_type, total_quantity, total_amount, status, del_flag, create_time)
                VALUES (?, ?, 1, 1, 100, 1, 0, ?)""", "RK-STAT-OLD", whA,
                LocalDateTime.now().minusDays(1));

        // 报废单：今日1单待审核，1单已处置不计入
        jdbcTemplate.update("""
                INSERT INTO scrap_order (order_no, warehouse_id, scrap_type, total_quantity, total_loss_amount, status, del_flag, create_time)
                VALUES (?, ?, 2, 1, 0, 0, 0, NOW())""", "BF-STAT-1", whA);
        jdbcTemplate.update("""
                INSERT INTO scrap_order (order_no, warehouse_id, scrap_type, total_quantity, total_loss_amount, status, del_flag, create_time)
                VALUES (?, ?, 2, 1, 0, 2, 0, NOW())""", "BF-STAT-2", whA);

        // 借用单：B1 今日2件器材(2+1)、B2 今日1件(已归还)、B3 已驳回、B4 上月2件(已全部归还)
        long b1 = insertBorrowOrder("JY-STAT-1", 100, 2, "借用中", LocalDateTime.now(), 3);
        long b2 = insertBorrowOrder("JY-STAT-2", 100, 3, "部分归还", LocalDateTime.now(), 1);
        insertBorrowOrder("JY-STAT-3", 100, 5, "已驳回", LocalDateTime.now(), 0);
        long b4 = insertBorrowOrder("JY-STAT-4", 101, 4, "已全部归还",
                LocalDateTime.now().minusMonths(1).withDayOfMonth(15), 2);

        long bi1 = insertBorrowItem(b1, eqA, whA, 2, 0, 0, 1);
        long bi2 = insertBorrowItem(b1, eqB, whA, 1, 0, 0, 1);
        long bi3 = insertBorrowItem(b2, eqA, whA, 1, 1, 1, 3);
        long bi4 = insertBorrowItem(b4, eqA, whB, 2, 2, 2, 3);

        // 归还单：R1 今日已确认（正常），R2 今日已确认（2条逾期），R3 已驳回
        long r1 = insertReturnOrder("GH-STAT-1", b2, whA, 1, LocalDateTime.now());
        long r2 = insertReturnOrder("GH-STAT-2", b1, whA, 3, LocalDateTime.now());
        insertReturnOrder("GH-STAT-3", b1, whA, 3, LocalDateTime.now());
        jdbcTemplate.update("UPDATE return_order SET status = 2 WHERE order_no = 'GH-STAT-3'");
        insertReturnItem(r1, bi3, eqA, 1, 0, 0, "0.00");
        insertReturnItem(r2, bi1, eqA, 2, 1, 2, "10.00");
        insertReturnItem(r2, bi2, eqB, 1, 1, 4, "20.00");

        // 上月已确认归还 2 件
        long r4 = insertReturnOrder("GH-STAT-4", b4, whB, 2,
                LocalDateTime.now().minusMonths(1).withDayOfMonth(16));
        insertReturnItem(r4, bi4, eqB, 2, 0, 0, "0.00");
    }

    private long insertCategory(String code, String name) {
        jdbcTemplate.update("""
                INSERT INTO equipment_category (parent_id, ancestors, category_name, category_code, sort_order, status, del_flag, create_time)
                VALUES (0, '0', ?, ?, 0, 1, 0, NOW())""", name, code);
        return queryId("SELECT id FROM equipment_category WHERE category_code = ?", code);
    }

    private long insertEquipment(String code, String name, long categoryId, String price, int safeStock) {
        jdbcTemplate.update("""
                INSERT INTO equipment (equipment_code, equipment_name, category_id, unit, purchase_price, safe_stock, status, del_flag, create_time, update_time)
                VALUES (?, ?, ?, '个', ?, ?, 1, 0, NOW(), NOW())""", code, name, categoryId, price, safeStock);
        return queryId("SELECT id FROM equipment WHERE equipment_code = ?", code);
    }

    private long insertWarehouse(String code, String name) {
        jdbcTemplate.update("""
                INSERT INTO warehouse (warehouse_code, warehouse_name, status, del_flag, create_time)
                VALUES (?, ?, 1, 0, NOW())""", code, name);
        return queryId("SELECT id FROM warehouse WHERE warehouse_code = ?", code);
    }

    private long insertBorrowOrder(String orderNo, long deptId, int status, String purpose,
                                   LocalDateTime issueTime, int totalQuantity) {
        jdbcTemplate.update("""
                INSERT INTO borrow_order (order_no, user_id, dept_id, borrow_type, purpose, expected_return_date,
                    total_quantity, status, locked, issue_time, del_flag, create_time, update_time)
                VALUES (?, 1, ?, 1, ?, CURDATE() + INTERVAL 7 DAY, ?, ?, 1, ?, 0, ?, NOW())""",
                orderNo, deptId, purpose, totalQuantity, status, issueTime, issueTime);
        return queryId("SELECT id FROM borrow_order WHERE order_no = ?", orderNo);
    }

    private long insertBorrowItem(long borrowId, long equipmentId, long warehouseId, int issued,
                                  int returned, int overdueDays, int status) {
        jdbcTemplate.update("""
                INSERT INTO borrow_item (borrow_id, equipment_id, warehouse_id, quantity, issued_quantity,
                    returned_quantity, expected_return_date, overdue_flag, overdue_days, status)
                VALUES (?, ?, ?, ?, ?, ?, CURDATE() + INTERVAL 7 DAY, ?, ?, ?)""",
                borrowId, equipmentId, warehouseId, issued, issued, returned,
                overdueDays > 0 ? 1 : 0, overdueDays, status);
        return queryId("SELECT id FROM borrow_item WHERE borrow_id = ? AND equipment_id = ?",
                borrowId, equipmentId);
    }

    private long insertReturnOrder(String orderNo, long borrowOrderId, long warehouseId,
                                   int totalQuantity, LocalDateTime confirmTime) {
        jdbcTemplate.update("""
                INSERT INTO return_order (order_no, borrow_order_id, user_id, warehouse_id, total_quantity,
                    return_type, status, confirm_by, confirm_time, del_flag, create_time, update_time)
                VALUES (?, ?, 1, ?, ?, 1, 1, 1, ?, 0, ?, NOW())""",
                orderNo, borrowOrderId, warehouseId, totalQuantity, confirmTime, confirmTime);
        return queryId("SELECT id FROM return_order WHERE order_no = ?", orderNo);
    }

    private void insertReturnItem(long returnId, long borrowItemId, long equipmentId, int quantity,
                                  int isOverdue, int overdueDays, String penalty) {
        jdbcTemplate.update("""
                INSERT INTO return_item (return_id, borrow_item_id, equipment_id, quantity, condition_status,
                    is_overdue, overdue_days, penalty_amount)
                VALUES (?, ?, ?, ?, 1, ?, ?, ?)""",
                returnId, borrowItemId, equipmentId, quantity, isOverdue, overdueDays, penalty);
    }

    private long queryId(String sql, Object... args) {
        Number id = jdbcTemplate.queryForObject(sql, Number.class, args);
        return id.longValue();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM return_item WHERE return_id IN (SELECT id FROM return_order WHERE order_no LIKE 'GH-STAT-%')");
        jdbcTemplate.update("DELETE FROM return_order WHERE order_no LIKE 'GH-STAT-%'");
        jdbcTemplate.update("DELETE FROM borrow_item WHERE borrow_id IN (SELECT id FROM borrow_order WHERE order_no LIKE 'JY-STAT-%')");
        jdbcTemplate.update("DELETE FROM borrow_order WHERE order_no LIKE 'JY-STAT-%'");
        jdbcTemplate.update("DELETE FROM stock_in_order WHERE order_no LIKE 'RK-STAT-%'");
        jdbcTemplate.update("DELETE FROM scrap_order WHERE order_no LIKE 'BF-STAT-%'");
        jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id IN (SELECT id FROM equipment WHERE equipment_code IN (?, ?))",
                EQ_A, EQ_B);
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_code IN (?, ?)", EQ_A, EQ_B);
        jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code IN (?, ?)", CAT_A, CAT_B);
        jdbcTemplate.update("DELETE FROM warehouse WHERE warehouse_code IN (?, ?)", WH_A, WH_B);
    }

    private String loginAdmin() throws Exception {
        redisTemplate.opsForValue().set(
                CacheConstants.CAPTCHA_CODE_KEY + UUID, CODE, Duration.ofMinutes(5));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "username", "admin",
                                "password", "admin123",
                                "code", CODE,
                                "uuid", UUID))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (String) JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getByPath("data.token");
    }
}
