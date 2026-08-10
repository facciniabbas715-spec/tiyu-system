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
 * 借用归还集成测试：锁定/领用/归还/逾期/超借拦截。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BorrowReturnTest {

    private static final String UUID = "borrow-uuid";
    private static final String CODE = "1234";
    private static final String TEST_CATEGORY = "BW";
    private static final String TEST_EQUIPMENT = "借用测试器材";
    private static final String TEST_WAREHOUSE = "WH-BORROW";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void borrowIssueAndReturn_shouldUpdateStock() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 10);
            long orderId = createBorrow(token, ids, 3);

            // 提交后锁定 3
            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].lockedQuantity").value(3))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(10));

            mockMvc.perform(put("/api/borrow/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", true))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/borrow/" + orderId + "/issue")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));

            // 领用后：在库 7、锁定 0、借用出库流水
            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(7))
                    .andExpect(jsonPath("$.data.records[0].lockedQuantity").value(0));
            mockMvc.perform(get("/api/stock/record/page")
                            .header("Authorization", "Bearer " + token)
                            .param("changeType", "2"))
                    .andExpect(jsonPath("$.data.total").value(1));

            // 归还登记 + 确认
            Long borrowItemId = borrowItemId(token, orderId);
            long returnId = createReturn(token, orderId, borrowItemId, 3);
            mockMvc.perform(put("/api/return/" + returnId + "/confirm")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(10));
            mockMvc.perform(get("/api/stock/record/page")
                            .header("Authorization", "Bearer " + token)
                            .param("changeType", "3"))
                    .andExpect(jsonPath("$.data.total").value(1));
            mockMvc.perform(get("/api/borrow/" + orderId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data.status").value(4));
        } finally {
            cleanup();
        }
    }

    @Test
    void borrowReject_shouldUnlockStock() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 10);
            long orderId = createBorrow(token, ids, 2);
            mockMvc.perform(put("/api/borrow/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", false, "remark", "库存不足"))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", TEST_EQUIPMENT))
                    .andExpect(jsonPath("$.data.records[0].lockedQuantity").value(0))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(10));
        } finally {
            cleanup();
        }
    }

    @Test
    void borrowOverAvailable_shouldReturn3001() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 10);
            mockMvc.perform(post("/api/borrow")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(borrowBody(ids, 12)))
                    .andExpect(jsonPath("$.code").value(3001));
        } finally {
            cleanup();
        }
    }

    @Test
    void overdueReturn_shouldCalculatePenalty() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 10);
            long orderId = createBorrow(token, ids, 1);
            mockMvc.perform(put("/api/borrow/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", orderId, "pass", true))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/borrow/" + orderId + "/issue")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            // 把预计归还日改为昨天，模拟逾期
            jdbcTemplate.update("UPDATE borrow_item SET expected_return_date = ? WHERE borrow_id = ?",
                    LocalDate.now().minusDays(1), orderId);
            Long borrowItemId = borrowItemId(token, orderId);
            long returnId = createReturn(token, orderId, borrowItemId, 1);
            mockMvc.perform(put("/api/return/" + returnId + "/confirm")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/return/" + returnId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data.items[0].isOverdue").value(1))
                    .andExpect(jsonPath("$.data.items[0].overdueDays").value(1))
                    .andExpect(jsonPath("$.data.items[0].penaltyAmount").value(5));
        } finally {
            cleanup();
        }
    }

    private long createBorrow(String token, long[] ids, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/borrow")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(ids, quantity)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return ((Number) JSONUtil.parseObj(
                mockMvc.perform(get("/api/borrow/page")
                                .header("Authorization", "Bearer " + token)
                                .param("status", "0"))
                        .andReturn().getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private String borrowBody(long[] ids, int quantity) {
        return JSONUtil.toJsonStr(Map.of(
                "borrowType", 1,
                "purpose", "集成测试借用",
                "expectedReturnDate", LocalDate.now().plusDays(7).toString(),
                "items", List.of(Map.of(
                        "equipmentId", ids[0],
                        "warehouseId", ids[1],
                        "quantity", quantity))));
    }

    private long createReturn(String token, long borrowOrderId, long borrowItemId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/return")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "borrowOrderId", borrowOrderId,
                                "items", List.of(Map.of(
                                        "borrowItemId", borrowItemId,
                                        "quantity", quantity,
                                        "conditionStatus", 1))))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return ((Number) JSONUtil.parseObj(
                mockMvc.perform(get("/api/return/page")
                                .header("Authorization", "Bearer " + token)
                                .param("status", "0"))
                        .andReturn().getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private long borrowItemId(String token, long borrowOrderId) throws Exception {
        return ((Number) JSONUtil.parseObj(
                mockMvc.perform(get("/api/borrow/" + borrowOrderId)
                                .header("Authorization", "Bearer " + token))
                        .andReturn().getResponse().getContentAsString())
                .getByPath("data.items[0].id")).longValue();
    }

    private long[] prepareBase(String token, int stockQuantity) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0,
                                "categoryName", "借用测试分类",
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
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/equipment/warehouse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseCode", TEST_WAREHOUSE,
                                "warehouseName", "借用测试仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        long[] ids = new long[]{equipmentId(token), warehouseId(token)};
        mockMvc.perform(put("/api/stock/adjust")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentId", ids[0],
                                "warehouseId", ids[1],
                                "changeQuantity", stockQuantity,
                                "remark", "测试备货"))))
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
        jdbcTemplate.update("DELETE FROM return_item");
        jdbcTemplate.update("DELETE FROM return_order");
        jdbcTemplate.update("DELETE FROM borrow_item");
        jdbcTemplate.update("DELETE FROM borrow_order");
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
