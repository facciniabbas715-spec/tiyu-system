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
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 报废管理集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScrapTest {

    private static final String UUID = "scrap-uuid";
    private static final String CODE = "1234";
    private static final String TEST_CATEGORY = "SCR";
    private static final String TEST_EQUIPMENT = "报废测试器材";
    private static final String TEST_WAREHOUSE = "WH-SCRAP";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scrapFlow_shouldDisposeAndDeductStock() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 5);
            long orderId = createScrap(token, ids, 2);
            mockMvc.perform(put("/api/scrap/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", true))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/scrap/dispose")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "disposeMethod", 1))))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(3));
            mockMvc.perform(get("/api/stock/record/page")
                            .header("Authorization", "Bearer " + token)
                            .param("changeType", "4"))
                    .andExpect(jsonPath("$.data.total").value(1));

            // 已处置不能再处置
            mockMvc.perform(put("/api/scrap/dispose")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "disposeMethod", 1))))
                    .andExpect(jsonPath("$.code").value(3002));
        } finally {
            cleanup();
        }
    }

    @Test
    void scrapReject_shouldBlockDispose() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 5);
            long orderId = createScrap(token, ids, 1);
            mockMvc.perform(put("/api/scrap/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", false))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/scrap/dispose")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "disposeMethod", 1))))
                    .andExpect(jsonPath("$.code").value(3002));
        } finally {
            cleanup();
        }
    }

    @Test
    void scrapOverStock_shouldReturn3001() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 2);
            long orderId = createScrap(token, ids, 5);
            mockMvc.perform(put("/api/scrap/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", true))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/scrap/dispose")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "disposeMethod", 1))))
                    .andExpect(jsonPath("$.code").value(3001));
        } finally {
            cleanup();
        }
    }

    private long createScrap(String token, long[] ids, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/scrap")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseId", ids[1],
                                "scrapType", 2,
                                "items", List.of(Map.of(
                                        "equipmentId", ids[0],
                                        "quantity", quantity,
                                        "scrapReason", "集成测试报废"))))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return ((Number) JSONUtil.parseObj(
                mockMvc.perform(get("/api/scrap/page")
                                .header("Authorization", "Bearer " + token)
                                .param("status", "0"))
                        .andReturn().getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private long[] prepareBase(String token, int stockQuantity) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0,
                                "categoryName", "报废测试分类",
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
                                "purchasePrice", 100,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/equipment/warehouse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseCode", TEST_WAREHOUSE,
                                "warehouseName", "报废测试仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        long[] ids = new long[]{equipmentId(token), warehouseId(token)};
        mockMvc.perform(put("/api/stock/adjust")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentId", ids[0],
                                "warehouseId", ids[1],
                                "changeQuantity", stockQuantity))))
                .andExpect(jsonPath("$.code").value(0));
        return ids;
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
        jdbcTemplate.update("DELETE FROM scrap_item");
        jdbcTemplate.update("DELETE FROM scrap_order");
        jdbcTemplate.update("DELETE FROM stock_record");
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
