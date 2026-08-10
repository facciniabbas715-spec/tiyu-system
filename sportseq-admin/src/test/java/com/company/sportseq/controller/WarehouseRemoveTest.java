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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 中危项 3：仓库删除必须校验库存/入库/归还/报废引用。
 */
@SpringBootTest
@AutoConfigureMockMvc
class WarehouseRemoveTest {

    private static final String UUID = "wh-remove-uuid";
    private static final String CODE = "1234";
    private static final String CATEGORY = "WR";
    private static final String EQUIPMENT = "仓库删除校验器材";
    private static final String WAREHOUSE = "WH-REMOVE";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void removeWarehouse_withStock_shouldBeRejected() throws Exception {
        String token = loginAdmin();
        try {
            long warehouseId = prepareBase(token, 3);
            assertRejected(token, warehouseId);
        } finally {
            cleanup();
        }
    }

    @Test
    void removeWarehouse_withStockInOrder_shouldBeRejected() throws Exception {
        String token = loginAdmin();
        try {
            long warehouseId = prepareBase(token, 0);
            jdbcTemplate.update(
                    "INSERT INTO stock_in_order (order_no, warehouse_id, in_type, total_quantity, total_amount, " +
                            "status, create_time) VALUES ('RK-REMOVE-TEST', ?, 1, 1, 0, 0, NOW())",
                    warehouseId);
            assertRejected(token, warehouseId);
        } finally {
            cleanup();
        }
    }

    @Test
    void removeWarehouse_withReturnOrder_shouldBeRejected() throws Exception {
        String token = loginAdmin();
        try {
            long warehouseId = prepareBase(token, 0);
            jdbcTemplate.update(
                    "INSERT INTO return_order (order_no, borrow_order_id, user_id, warehouse_id, total_quantity, " +
                            "return_type, status, create_time) VALUES ('GH-REMOVE-TEST', 0, 1, ?, 1, 1, 0, NOW())",
                    warehouseId);
            assertRejected(token, warehouseId);
        } finally {
            cleanup();
        }
    }

    @Test
    void removeWarehouse_withScrapOrder_shouldBeRejected() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBaseWithEquipment(token);
            mockMvc.perform(post("/api/scrap")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "warehouseId", ids[1],
                                    "scrapType", 2,
                                    "remark", "仓库删除校验报废单",
                                    "items", List.of(Map.of(
                                            "equipmentId", ids[0],
                                            "quantity", 1,
                                            "scrapReason", "仓库删除校验"))))))
                    .andExpect(jsonPath("$.code").value(0));
            assertRejected(token, ids[1]);
        } finally {
            cleanup();
        }
    }

    @Test
    void removeWarehouse_withoutReferences_shouldSucceed() throws Exception {
        String token = loginAdmin();
        try {
            long warehouseId = prepareBase(token, 0);
            mockMvc.perform(delete("/api/equipment/warehouse/" + warehouseId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            cleanup();
        }
    }

    private long prepareBase(String token, int stockQuantity) throws Exception {
        long[] ids = prepareBaseWithEquipment(token);
        if (stockQuantity > 0) {
            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", ids[0],
                                    "warehouseId", ids[1],
                                    "changeQuantity", stockQuantity))))
                    .andExpect(jsonPath("$.code").value(0));
        }
        return ids[1];
    }

    private long[] prepareBaseWithEquipment(String token) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0,
                                "categoryName", "仓库删除校验分类",
                                "categoryCode", CATEGORY,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentName", EQUIPMENT,
                                "categoryId", categoryId(token),
                                "unit", "个",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/equipment/warehouse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseCode", WAREHOUSE,
                                "warehouseName", "仓库删除校验仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        return new long[]{equipmentId(token), warehouseId(token)};
    }

    private void assertRejected(String token, long warehouseId) throws Exception {
        mockMvc.perform(delete("/api/equipment/warehouse/" + warehouseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(1001));
    }

    private long categoryId(String token) throws Exception {
        return ((Number) JSONUtil.parseObj(mockMvc.perform(get("/api/equipment/category/tree")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .getByPath("data[0].id")).longValue();
    }

    private long equipmentId(String token) throws Exception {
        MvcResult page = mockMvc.perform(get("/api/equipment/page")
                        .header("Authorization", "Bearer " + token)
                        .param("equipmentName", EQUIPMENT))
                .andReturn();
        return ((Number) JSONUtil.parseObj(page.getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private long warehouseId(String token) throws Exception {
        MvcResult list = mockMvc.perform(get("/api/equipment/warehouse/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return ((Number) JSONUtil.parseObj(list.getResponse().getContentAsString())
                .getByPath("data[0].id")).longValue();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM stock_in_order WHERE order_no = 'RK-REMOVE-TEST'");
        jdbcTemplate.update("DELETE FROM return_order WHERE order_no = 'GH-REMOVE-TEST'");
        jdbcTemplate.update("DELETE si FROM scrap_item si JOIN scrap_order so ON so.id = si.scrap_id " +
                "WHERE so.remark = '仓库删除校验报废单'");
        jdbcTemplate.update("DELETE FROM scrap_order WHERE remark = '仓库删除校验报废单'");
        jdbcTemplate.update("DELETE FROM stock_record WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name = ?)", EQUIPMENT);
        jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name = ?)", EQUIPMENT);
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name = ?", EQUIPMENT);
        jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code = ?", CATEGORY);
        jdbcTemplate.update("DELETE FROM warehouse WHERE warehouse_code = ?", WAREHOUSE);
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
