package com.company.sportseq.controller;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 仪表盘汇总缓存（sportseq:dashboard:summary）失效语义集成测试。
 *
 * <p>汇总包含当日借用/归还/入库数、库存预警、待报废数、器材总数与库存总价值，
 * 任何会改变这些口径的写操作都必须让汇总缓存失效。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardSummaryEvictionTest {

    private static final String UUID = "dash-uuid";
    private static final String CODE = "1234";
    private static final String CATEGORY_CODE = "DASH";
    private static final String EQUIPMENT_NAME = "汇总失效测试器材";
    private static final String IMPORT_EQUIPMENT_NAME = "汇总导入测试器材";
    private static final String WAREHOUSE_CODE = "WH-DASH";
    private static final String SUPPLIER = "汇总失效测试供应商";
    private static final String BORROW_PURPOSE = "汇总失效测试借用";
    private static final String SCRAP_REMARK = "汇总失效测试报废";
    private static final String SUMMARY_KEY = CacheConstants.DASHBOARD_SUMMARY_KEY;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long equipmentId;
    private Long warehouseId;

    @BeforeEach
    void resetState() {
        redisTemplate.delete(SUMMARY_KEY);
        equipmentId = null;
        warehouseId = null;
    }

    @Test
    void equipmentAdd_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            createCategory(token);
            seedSummary(token);
            mockMvc.perform(post("/api/equipment")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentName", EQUIPMENT_NAME,
                                    "categoryId", categoryId(token),
                                    "unit", "个",
                                    "safeStock", 0,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));
            equipmentId = equipmentId(token);
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "新增器材后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void equipmentUpdate_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            createCategory(token);
            createEquipment(token);
            seedSummary(token);
            mockMvc.perform(put("/api/equipment")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", equipmentId,
                                    "equipmentName", EQUIPMENT_NAME + "-改名",
                                    "categoryId", categoryId(token),
                                    "unit", "个",
                                    "purchasePrice", 100,
                                    "safeStock", 1,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "器材修改（价格/安全库存）后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void equipmentRemove_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            createCategory(token);
            createEquipment(token);
            seedSummary(token);
            mockMvc.perform(delete("/api/equipment/" + equipmentId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "删除器材后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void equipmentImport_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            createCategory(token);
            seedSummary(token);
            mockMvc.perform(multipart("/api/equipment/import")
                            .file(importExcel())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "Excel 导入器材后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void scrapCreate_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, 5);
            seedSummary(token);
            createScrap(token, 2);
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "新增待审报废单后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void scrapAudit_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, 5);
            long orderId = createScrap(token, 2);
            seedSummary(token);
            mockMvc.perform(put("/api/scrap/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", true))))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "报废单审核后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void scrapCancel_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, 5);
            long orderId = createScrap(token, 2);
            seedSummary(token);
            mockMvc.perform(put("/api/scrap/" + orderId + "/cancel")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "报废单作废后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void stockInCreate_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, null);
            seedSummary(token);
            createStockIn(token);
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "新增入库单后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void stockInAuditReject_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, null);
            long orderId = createStockIn(token);
            mockMvc.perform(put("/api/stock/in/" + orderId + "/submit")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            seedSummary(token);
            mockMvc.perform(put("/api/stock/in/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", false))))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "入库单审核驳回后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void stockInCancel_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, null);
            long orderId = createStockIn(token);
            seedSummary(token);
            mockMvc.perform(put("/api/stock/in/" + orderId + "/cancel")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "入库单作废后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void stockInRemove_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, null);
            long orderId = createStockIn(token);
            seedSummary(token);
            mockMvc.perform(delete("/api/stock/in/" + orderId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "删除草稿入库单后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void returnCreate_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, 10);
            long orderId = createBorrowIssued(token);
            seedSummary(token);
            createReturn(token, orderId, borrowItemId(token, orderId), 3);
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "归还登记后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    @Test
    void returnReject_shouldEvictDashboardSummary() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, 10);
            long orderId = createBorrowIssued(token);
            long returnId = createReturn(token, orderId, borrowItemId(token, orderId), 3);
            seedSummary(token);
            mockMvc.perform(put("/api/return/" + returnId + "/reject")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                    "归还单驳回后仪表盘汇总缓存应失效");
        } finally {
            cleanup();
        }
    }

    private void seedSummary(String token) throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(SUMMARY_KEY)),
                "前置条件：仪表盘汇总应已写入 Redis");
    }

    private void prepareBase(String token, Integer stockQuantity) throws Exception {
        createCategory(token);
        createEquipment(token);
        createWarehouse(token);
        if (stockQuantity != null) {
            adjustStock(token, stockQuantity);
        }
    }

    private void createCategory(String token) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0,
                                "categoryName", "汇总失效测试分类",
                                "categoryCode", CATEGORY_CODE,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
    }

    private void createEquipment(String token) throws Exception {
        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentName", EQUIPMENT_NAME,
                                "categoryId", categoryId(token),
                                "unit", "个",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        equipmentId = equipmentId(token);
    }

    private void createWarehouse(String token) throws Exception {
        mockMvc.perform(post("/api/equipment/warehouse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseCode", WAREHOUSE_CODE,
                                "warehouseName", "汇总失效测试仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        warehouseId = warehouseId(token);
    }

    private void adjustStock(String token, int quantity) throws Exception {
        mockMvc.perform(put("/api/stock/adjust")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentId", equipmentId,
                                "warehouseId", warehouseId,
                                "changeQuantity", quantity))))
                .andExpect(jsonPath("$.code").value(0));
    }

    private long createBorrowIssued(String token) throws Exception {
        mockMvc.perform(post("/api/borrow")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "borrowType", 1,
                                "purpose", BORROW_PURPOSE,
                                "expectedReturnDate", LocalDate.now().plusDays(7).toString(),
                                "items", List.of(Map.of(
                                        "equipmentId", equipmentId,
                                        "warehouseId", warehouseId,
                                        "quantity", 3))))))
                .andExpect(jsonPath("$.code").value(0));
        long orderId = firstId(token, "/api/borrow/page", "status", "0");
        mockMvc.perform(put("/api/borrow/audit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", true))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(put("/api/borrow/" + orderId + "/issue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
        return orderId;
    }

    private long borrowItemId(String token, long borrowOrderId) throws Exception {
        return ((Number) JSONUtil.parseObj(
                mockMvc.perform(get("/api/borrow/" + borrowOrderId)
                                .header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString())
                .getByPath("data.items[0].id")).longValue();
    }

    private long createReturn(String token, long borrowOrderId, long borrowItemId, int quantity) throws Exception {
        mockMvc.perform(post("/api/return")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "borrowOrderId", borrowOrderId,
                                "items", List.of(Map.of(
                                        "borrowItemId", borrowItemId,
                                        "quantity", quantity,
                                        "conditionStatus", 1))))))
                .andExpect(jsonPath("$.code").value(0));
        return firstId(token, "/api/return/page", "status", "0");
    }

    private long createScrap(String token, int quantity) throws Exception {
        mockMvc.perform(post("/api/scrap")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseId", warehouseId,
                                "scrapType", 2,
                                "remark", SCRAP_REMARK,
                                "items", List.of(Map.of(
                                        "equipmentId", equipmentId,
                                        "quantity", quantity,
                                        "scrapReason", "汇总失效测试"))))))
                .andExpect(jsonPath("$.code").value(0));
        return firstId(token, "/api/scrap/page", "status", "0");
    }

    private long createStockIn(String token) throws Exception {
        mockMvc.perform(post("/api/stock/in")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseId", warehouseId,
                                "inType", 1,
                                "supplier", SUPPLIER,
                                "items", List.of(Map.of(
                                        "equipmentId", equipmentId,
                                        "quantity", 10,
                                        "unitPrice", 50.00))))))
                .andExpect(jsonPath("$.code").value(0));
        return firstId(token, "/api/stock/in/page", "status", "0");
    }

    private long firstId(String token, String url, String param, String value) throws Exception {
        MvcResult result = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token)
                        .param(param, value))
                .andReturn();
        return ((Number) JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private long categoryId(String token) throws Exception {
        return ((Number) JSONUtil.parseObj(mockMvc.perform(get("/api/equipment/category/tree")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .getByPath("data[0].id")).longValue();
    }

    private long equipmentId(String token) throws Exception {
        return ((Number) JSONUtil.parseObj(mockMvc.perform(get("/api/equipment/page")
                        .header("Authorization", "Bearer " + token)
                        .param("equipmentName", EQUIPMENT_NAME))
                .andReturn().getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private long warehouseId(String token) throws Exception {
        return ((Number) JSONUtil.parseObj(mockMvc.perform(get("/api/equipment/warehouse/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .getByPath("data[0].id")).longValue();
    }

    private MockMultipartFile importExcel() throws Exception {
        String[] headers = {"器材名称", "分类编码", "品牌", "型号", "规格", "计量单位", "采购单价", "安全库存", "最长借用天数", "描述"};
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue(IMPORT_EQUIPMENT_NAME);
            data.createCell(1).setCellValue(CATEGORY_CODE);
            workbook.write(out);
            return new MockMultipartFile("file", "import.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM return_item WHERE return_id IN " +
                "(SELECT id FROM return_order WHERE borrow_order_id IN " +
                "(SELECT id FROM borrow_order WHERE purpose = ?))", BORROW_PURPOSE);
        jdbcTemplate.update("DELETE FROM return_order WHERE borrow_order_id IN " +
                "(SELECT id FROM borrow_order WHERE purpose = ?)", BORROW_PURPOSE);
        jdbcTemplate.update("DELETE FROM scrap_item WHERE scrap_id IN " +
                "(SELECT id FROM scrap_order WHERE remark = ?)", SCRAP_REMARK);
        jdbcTemplate.update("DELETE FROM scrap_order WHERE remark = ?", SCRAP_REMARK);
        jdbcTemplate.update("DELETE FROM borrow_item WHERE borrow_id IN " +
                "(SELECT id FROM borrow_order WHERE purpose = ?)", BORROW_PURPOSE);
        jdbcTemplate.update("DELETE FROM borrow_order WHERE purpose = ?", BORROW_PURPOSE);
        jdbcTemplate.update("DELETE FROM stock_in_item WHERE order_id IN " +
                "(SELECT id FROM stock_in_order WHERE supplier = ?)", SUPPLIER);
        jdbcTemplate.update("DELETE FROM stock_in_order WHERE supplier = ?", SUPPLIER);
        if (equipmentId != null) {
            jdbcTemplate.update("DELETE FROM stock_record WHERE equipment_id = ?", equipmentId);
            jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id = ?", equipmentId);
            jdbcTemplate.update("DELETE FROM equipment WHERE id = ?", equipmentId);
        }
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name = ?", IMPORT_EQUIPMENT_NAME);
        jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code = ?", CATEGORY_CODE);
        if (warehouseId != null) {
            jdbcTemplate.update("DELETE FROM warehouse WHERE id = ?", warehouseId);
        }
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
