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

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 库存与入库集成测试：调整、预警、入库全流程。
 */
@SpringBootTest
@AutoConfigureMockMvc
class StockTest {

    private static final String UUID = "stock-uuid";
    private static final String CODE = "1234";
    private static final String TEST_CATEGORY = "STK";
    private static final String TEST_EQUIPMENT = "库存测试器材";
    private static final String TEST_WAREHOUSE = "WH-STOCK";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void adjustStock_shouldUpdateStockAndWriteRecords() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token);
            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", ids[0],
                                    "warehouseId", ids[1],
                                    "changeQuantity", 5,
                                    "remark", "盘盈"))))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", ids[0],
                                    "warehouseId", ids[1],
                                    "changeQuantity", -2))))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(3));

            mockMvc.perform(get("/api/stock/record/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentId", String.valueOf(ids[0])))
                    .andExpect(jsonPath("$.data.total").value(2));

            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", ids[0],
                                    "warehouseId", ids[1],
                                    "changeQuantity", -10))))
                    .andExpect(jsonPath("$.code").value(3001));
        } finally {
            cleanup();
        }
    }

    @Test
    void stockInFlow_shouldReceiveAndIncreaseStock() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token);
            MvcResult createResult = mockMvc.perform(post("/api/stock/in")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "warehouseId", ids[1],
                                    "inType", 1,
                                    "supplier", "测试供应商",
                                    "items", List.of(Map.of(
                                            "equipmentId", ids[0],
                                            "quantity", 10,
                                            "unitPrice", 50.00))))))
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            Long orderId = ((Number) JSONUtil.parseObj(
                    mockMvc.perform(get("/api/stock/in/page")
                                    .header("Authorization", "Bearer " + token)
                                    .param("status", "0"))
                            .andExpect(jsonPath("$.data.records[0].orderNo")
                                    .value(org.hamcrest.Matchers.matchesPattern("RK\\d{14}")))
                            .andReturn().getResponse().getContentAsString())
                    .getByPath("data.records[0].id")).longValue();

            mockMvc.perform(put("/api/stock/in/" + orderId + "/submit")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/stock/in/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "orderId", orderId,
                                    "pass", true,
                                    "remark", "同意"))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/stock/in/" + orderId + "/receive")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(10));
            mockMvc.perform(get("/api/stock/record/page")
                            .header("Authorization", "Bearer " + token)
                            .param("changeType", "1"))
                    .andExpect(jsonPath("$.data.total").value(1));

            // 已验收后再次验收/审核被拦截
            mockMvc.perform(put("/api/stock/in/" + orderId + "/receive")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(3002));
            mockMvc.perform(put("/api/stock/in/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "orderId", orderId,
                                    "pass", false))))
                    .andExpect(jsonPath("$.code").value(3002));
        } finally {
            cleanup();
        }
    }

    @Test
    void warning_shouldListLowStock() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 5);
            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", ids[0],
                                    "warehouseId", ids[1],
                                    "changeQuantity", 3))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/stock/warning")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data[0].equipmentName").value(TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data[0].warning").value(true));
        } finally {
            cleanup();
        }
    }

    @Test
    void adjustNegative_shouldNotConsumeLockedStock() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token);
            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", ids[0],
                                    "warehouseId", ids[1],
                                    "changeQuantity", 10))))
                    .andExpect(jsonPath("$.code").value(0));
            // 借用申请锁定 6 件（可用仅 4 件）
            mockMvc.perform(post("/api/borrow")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "borrowType", 1,
                                    "purpose", "库存测试锁定借用",
                                    "expectedReturnDate", LocalDate.now().plusDays(7).toString(),
                                    "items", List.of(Map.of(
                                            "equipmentId", ids[0],
                                            "warehouseId", ids[1],
                                            "quantity", 6))))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(10))
                    .andExpect(jsonPath("$.data.records[0].lockedQuantity").value(6));

            // 盘亏 5 件超过可用 4 件，必须被拦截
            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", ids[0],
                                    "warehouseId", ids[1],
                                    "changeQuantity", -5))))
                    .andExpect(jsonPath("$.code").value(3001));
            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(10))
                    .andExpect(jsonPath("$.data.records[0].lockedQuantity").value(6));

            // 盘亏 3 件在可用范围内，允许
            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", ids[0],
                                    "warehouseId", ids[1],
                                    "changeQuantity", -3))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(7))
                    .andExpect(jsonPath("$.data.records[0].lockedQuantity").value(6));
        } finally {
            cleanup();
        }
    }

    private long[] prepareBase(String token) throws Exception {
        return prepareBase(token, 0);
    }

    private long[] prepareBase(String token, int safeStock) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0,
                                "categoryName", "库存测试分类",
                                "categoryCode", TEST_CATEGORY,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentName", TEST_EQUIPMENT,
                                "categoryId", categoryId(token),
                                "unit", "个",
                                "safeStock", safeStock,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/equipment/warehouse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseCode", TEST_WAREHOUSE,
                                "warehouseName", "库存测试仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        return new long[]{equipmentId(token), warehouseId(token)};
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
                        .param("equipmentName", TEST_EQUIPMENT))
                .andReturn().getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private long warehouseId(String token) throws Exception {
        return ((Number) JSONUtil.parseObj(mockMvc.perform(get("/api/equipment/warehouse/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .getByPath("data[0].id")).longValue();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM borrow_item WHERE borrow_id IN " +
                "(SELECT id FROM borrow_order WHERE purpose = '库存测试锁定借用')");
        jdbcTemplate.update("DELETE FROM borrow_order WHERE purpose = '库存测试锁定借用'");
        jdbcTemplate.update("DELETE FROM stock_record");
        jdbcTemplate.update("DELETE FROM stock_in_item");
        jdbcTemplate.update("DELETE FROM stock_in_order");
        jdbcTemplate.update("DELETE FROM equipment_stock");
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name = ?", TEST_EQUIPMENT);
        jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code = ?", TEST_CATEGORY);
        jdbcTemplate.update("DELETE FROM warehouse WHERE warehouse_code = ?", TEST_WAREHOUSE);
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
