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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 中危项 2：器材删除必须校验库存与未完结借用/报废业务。
 */
@SpringBootTest
@AutoConfigureMockMvc
class EquipmentRemoveTest {

    private static final String UUID = "equip-remove-uuid";
    private static final String CODE = "1234";
    private static final String CATEGORY = "ER";
    private static final String EQUIPMENT = "删除校验测试器材";
    private static final String WAREHOUSE = "WH-ER";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void removeEquipment_withPositiveStock_shouldBeRejected() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 5);
            mockMvc.perform(delete("/api/equipment/" + ids[0])
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(3004));
            assertEquipmentExists(token, ids[0]);
        } finally {
            cleanup();
        }
    }

    @Test
    void removeEquipment_withUnfinishedBorrow_shouldBeRejected() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 1);
            long borrowId = createBorrow(token, ids, 1);
            mockMvc.perform(put("/api/borrow/audit")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", borrowId, "pass", true))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/borrow/" + borrowId + "/issue")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(delete("/api/equipment/" + ids[0])
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(3004));
            assertEquipmentExists(token, ids[0]);
        } finally {
            cleanup();
        }
    }

    @Test
    void removeEquipment_withUnfinishedScrap_shouldBeRejected() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 0);
            mockMvc.perform(post("/api/scrap")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "warehouseId", ids[1],
                                    "scrapType", 2,
                                    "remark", "删除校验报废单",
                                    "items", List.of(Map.of(
                                            "equipmentId", ids[0],
                                            "quantity", 1,
                                            "scrapReason", "删除校验报废单"))))))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(delete("/api/equipment/" + ids[0])
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(3004));
            assertEquipmentExists(token, ids[0]);
        } finally {
            cleanup();
        }
    }

    @Test
    void removeEquipment_withoutReferences_shouldSucceed() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 0);
            mockMvc.perform(delete("/api/equipment/" + ids[0])
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            cleanup();
        }
    }

    private long[] prepareBase(String token, int stockQuantity) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0,
                                "categoryName", "删除校验测试分类",
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
                                "warehouseName", "删除校验测试仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        long equipmentId = equipmentId(token);
        long warehouseId = warehouseId(token);
        if (stockQuantity > 0) {
            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", equipmentId,
                                    "warehouseId", warehouseId,
                                    "changeQuantity", stockQuantity))))
                    .andExpect(jsonPath("$.code").value(0));
        }
        return new long[]{equipmentId, warehouseId};
    }

    private long createBorrow(String token, long[] ids, int quantity) throws Exception {
        mockMvc.perform(post("/api/borrow")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "borrowType", 1,
                                "purpose", "删除校验借用",
                                "expectedReturnDate", LocalDate.now().plusDays(7).toString(),
                                "items", List.of(Map.of(
                                        "equipmentId", ids[0],
                                        "warehouseId", ids[1],
                                        "quantity", quantity))))))
                .andExpect(jsonPath("$.code").value(0));
        MvcResult page = mockMvc.perform(get("/api/borrow/page")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "0"))
                .andReturn();
        return ((Number) JSONUtil.parseObj(page.getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
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
        return ((Number) JSONUtil.parseObj(mockMvc.perform(get("/api/equipment/warehouse/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .getByPath("data[0].id")).longValue();
    }

    private void assertEquipmentExists(String token, long equipmentId) throws Exception {
        mockMvc.perform(get("/api/equipment/" + equipmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0));
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM stock_record WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name = ?)", EQUIPMENT);
        jdbcTemplate.update("DELETE ri FROM return_item ri JOIN return_order ro ON ro.id = ri.return_id " +
                "JOIN borrow_order bo ON bo.id = ro.borrow_order_id WHERE bo.purpose = '删除校验借用'");
        jdbcTemplate.update("DELETE ro FROM return_order ro JOIN borrow_order bo ON bo.id = ro.borrow_order_id " +
                "WHERE bo.purpose = '删除校验借用'");
        jdbcTemplate.update("DELETE bi FROM borrow_item bi JOIN borrow_order bo ON bo.id = bi.borrow_id " +
                "WHERE bo.purpose = '删除校验借用'");
        jdbcTemplate.update("DELETE FROM borrow_order WHERE purpose = '删除校验借用'");
        jdbcTemplate.update("DELETE si FROM scrap_item si JOIN scrap_order so ON so.id = si.scrap_id " +
                "WHERE so.remark = '删除校验报废单'");
        jdbcTemplate.update("DELETE FROM scrap_order WHERE remark = '删除校验报废单'");
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
