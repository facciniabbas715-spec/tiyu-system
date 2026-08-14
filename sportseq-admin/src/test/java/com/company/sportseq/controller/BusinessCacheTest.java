package com.company.sportseq.controller;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Redis 业务缓存集成测试：
 * 验证 Cache Aside 读取回填、统一 Key 命名、以及写操作后的缓存失效语义。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BusinessCacheTest {

    private static final String UUID = "cache-uuid";
    private static final String CODE = "1234";
    private static final String CATEGORY_CODE = "CACHE";
    private static final String WAREHOUSE_CODE = "WHCACHE";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void flushBusinessCaches() {
        jdbcTemplate.update("DELETE FROM stock_record WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name LIKE '缓存测试%')");
        jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name LIKE '缓存测试%')");
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name LIKE '缓存测试%'");
        jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code = ?", CATEGORY_CODE);
        jdbcTemplate.update("DELETE FROM warehouse WHERE warehouse_code = ?", WAREHOUSE_CODE);
        jdbcTemplate.update("DELETE FROM sys_config WHERE config_key = 'cache.test.key'");
        jdbcTemplate.update("DELETE FROM sys_dict_type WHERE dict_type = 'cache_test_dict'");

        String[] patterns = {
                "sportseq:category:*",
                "sportseq:equipment:detail:*",
                "sportseq:equipment:list:*",
                "sportseq:stock:page:*",
                "sportseq:statistics:usage:*",
                "sportseq:dashboard:summary"
        };
        for (String pattern : patterns) {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    @Test
    void equipmentDetail_shouldUseCacheAsideAndEvictOnUpdate() throws Exception {
        String token = loginAdmin();
        Long categoryId = null;
        Long equipmentId = null;
        try {
            categoryId = createCategory(token, "缓存测试分类-详情");
            equipmentId = createEquipment(token, categoryId, "缓存测试器材-详情");
            String detailKey = "sportseq:equipment:detail:" + equipmentId;

            mockMvc.perform(get("/api/equipment/" + equipmentId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.equipmentName").value("缓存测试器材-详情"));
            assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey(detailKey)),
                    "器材详情首次读取后应写入 Redis");

            mockMvc.perform(put("/api/equipment")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", equipmentId,
                                    "equipmentName", "缓存测试器材-详情-改名",
                                    "categoryId", categoryId,
                                    "unit", "个"))))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(detailKey)),
                    "器材更新后详情缓存应失效");

            mockMvc.perform(get("/api/equipment/" + equipmentId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data.equipmentName").value("缓存测试器材-详情-改名"));
        } finally {
            cleanupEquipment(equipmentId, categoryId);
        }
    }

    @Test
    void equipmentList_shouldCacheAndEvictOnAdd() throws Exception {
        String token = loginAdmin();
        Long categoryId = null;
        Long equipmentId = null;
        try {
            categoryId = createCategory(token, "缓存测试分类-列表");

            mockMvc.perform(get("/api/equipment/page")
                            .header("Authorization", "Bearer " + token)
                            .param("current", "1")
                            .param("size", "5"))
                    .andExpect(jsonPath("$.code").value(0));
            assertTrue(hasAnyKey("sportseq:equipment:list:*"), "器材分页查询后应写入 Redis");

            equipmentId = createEquipment(token, categoryId, "缓存测试器材-列表");
            assertFalse(hasAnyKey("sportseq:equipment:list:*"), "新增器材后分页缓存应失效");

            mockMvc.perform(get("/api/equipment/page")
                            .header("Authorization", "Bearer " + token)
                            .param("current", "1")
                            .param("size", "5"))
                    .andExpect(jsonPath("$.code").value(0));
            assertTrue(hasAnyKey("sportseq:equipment:list:*"), "再次查询应重建分页缓存");
        } finally {
            cleanupEquipment(equipmentId, categoryId);
        }
    }

    @Test
    void categoryTree_shouldCacheAndEvictOnAdd() throws Exception {
        String token = loginAdmin();
        Long categoryId = null;
        try {
            mockMvc.perform(get("/api/equipment/category/tree")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey("sportseq:category:list:all")),
                    "分类树查询后应写入 Redis");

            categoryId = createCategory(token, "缓存测试分类-树");
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey("sportseq:category:list:all")),
                    "新增分类后分类缓存应失效");

            mockMvc.perform(get("/api/equipment/category/tree")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data[*].categoryCode")
                            .value(org.hamcrest.Matchers.hasItem(CATEGORY_CODE)));
        } finally {
            deleteCategory(categoryId);
        }
    }

    @Test
    void stockPage_shouldCacheAndStayConsistentOnAdjust() throws Exception {
        String token = loginAdmin();
        Long equipmentId = null;
        Long categoryId = null;
        Long warehouseId = null;
        try {
            long[] ids = prepareStockBase(token);
            equipmentId = ids[0];
            categoryId = ids[1];
            warehouseId = ids[2];

            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", equipmentId,
                                    "warehouseId", warehouseId,
                                    "changeQuantity", 10))))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", "缓存测试器材-库存"))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(10));
            assertTrue(hasAnyKey("sportseq:stock:page:*"), "库存查询后应写入 Redis");

            mockMvc.perform(put("/api/stock/adjust")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentId", equipmentId,
                                    "warehouseId", warehouseId,
                                    "changeQuantity", 5))))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(hasAnyKey("sportseq:stock:page:*"), "库存变化后缓存应立即失效");

            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", "缓存测试器材-库存"))
                    .andExpect(jsonPath("$.data.records[0].quantity").value(15));
        } finally {
            cleanupStockBase(equipmentId, categoryId, warehouseId);
        }
    }

    @Test
    void hotEquipmentAndDashboardSummary_shouldBeCached() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(get("/api/statistics/equipment-usage")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertTrue(hasAnyKey("sportseq:statistics:usage:*"), "热门器材排行应写入 Redis");

            mockMvc.perform(get("/api/dashboard/summary")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey("sportseq:dashboard:summary")),
                    "仪表盘汇总应写入 Redis");
        } finally {
            // 仅验证缓存写入，无业务数据残留
        }
    }

    @Test
    void configAndDict_shouldUseUnifiedCacheKeys() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(post("/api/system/config")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "configName", "缓存测试参数",
                                    "configKey", "cache.test.key",
                                    "configValue", "v1"))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/system/config/key/cache.test.key")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data").value("v1"));
            assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey("sportseq:config::cache.test.key")),
                    "系统配置应使用统一命名空间缓存");

            Long configId = jdbcTemplate.queryForObject(
                    "SELECT id FROM sys_config WHERE config_key = 'cache.test.key'", Long.class);
            mockMvc.perform(put("/api/system/config")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", configId,
                                    "configName", "缓存测试参数",
                                    "configKey", "cache.test.key",
                                    "configValue", "v2"))))
                    .andExpect(jsonPath("$.code").value(0));
            assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey("sportseq:config::cache.test.key")),
                    "配置更新后缓存应失效");

            mockMvc.perform(post("/api/system/dict/type")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "dictName", "缓存测试字典",
                                    "dictType", "cache_test_dict"))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/system/dict/data/type/cache_test_dict")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey("sportseq:dict::data:cache_test_dict")),
                    "字典数据应使用统一命名空间缓存");
        } finally {
            jdbcTemplate.update("DELETE FROM sys_config WHERE config_key = 'cache.test.key'");
            jdbcTemplate.update("DELETE FROM sys_dict_type WHERE dict_type = 'cache_test_dict'");
        }
    }

    private long[] prepareStockBase(String token) throws Exception {
        Long categoryId = createCategory(token, "缓存测试分类-库存");
        Long equipmentId = createEquipment(token, categoryId, "缓存测试器材-库存");
        mockMvc.perform(post("/api/equipment/warehouse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseCode", WAREHOUSE_CODE,
                                "warehouseName", "缓存测试仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        Long warehouseId = findWarehouseId(token);
        return new long[]{equipmentId, categoryId, warehouseId};
    }

    private Long createCategory(String token, String name) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0,
                                "categoryName", name,
                                "categoryCode", CATEGORY_CODE,
                                "sortOrder", 1,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM equipment_category WHERE category_code = ?", Long.class, CATEGORY_CODE);
    }

    private Long createEquipment(String token, Long categoryId, String name) throws Exception {
        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentName", name,
                                "categoryId", categoryId,
                                "unit", "个",
                                "safeStock", 0,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM equipment WHERE equipment_name = ?", Long.class, name);
    }

    private Long findWarehouseId(String token) throws Exception {
        MvcResult list = mockMvc.perform(get("/api/equipment/warehouse/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return extractId(JSONUtil.parseObj(list.getResponse().getContentAsString())
                        .getByPath("data", List.class),
                "warehouseCode", WAREHOUSE_CODE, "仓库未创建成功");
    }

    private long extractId(List<?> items, String matchField, String matchValue, String message) {
        for (Object item : items) {
            Map<?, ?> row = (Map<?, ?>) item;
            if (matchValue.equals(row.get(matchField))) {
                return ((Number) row.get("id")).longValue();
            }
        }
        throw new AssertionError(message);
    }

    private boolean hasAnyKey(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys != null && !keys.isEmpty();
    }

    private void cleanupEquipment(Long equipmentId, Long categoryId) {
        if (equipmentId != null) {
            jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id = ?", equipmentId);
            jdbcTemplate.update("DELETE FROM equipment WHERE id = ?", equipmentId);
        }
        deleteCategory(categoryId);
    }

    private void cleanupStockBase(Long equipmentId, Long categoryId, Long warehouseId) {
        if (equipmentId != null) {
            jdbcTemplate.update("DELETE FROM stock_record WHERE equipment_id = ?", equipmentId);
            jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id = ?", equipmentId);
            jdbcTemplate.update("DELETE FROM equipment WHERE id = ?", equipmentId);
        }
        deleteCategory(categoryId);
        if (warehouseId != null) {
            jdbcTemplate.update("DELETE FROM warehouse WHERE id = ?", warehouseId);
        }
    }

    private void deleteCategory(Long categoryId) {
        if (categoryId != null) {
            jdbcTemplate.update("DELETE FROM equipment_category WHERE id = ?", categoryId);
        } else {
            jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code = ?", CATEGORY_CODE);
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
