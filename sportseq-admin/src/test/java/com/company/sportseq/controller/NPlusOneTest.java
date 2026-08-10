package com.company.sportseq.controller;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.SqlSessionFactory;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 中危项 7：列表 VO 组装必须批量查询关联表，禁止逐行 N+1。
 * 通过 MyBatis Executor 拦截器统计单次分页请求的 SELECT 次数。
 */
@SpringBootTest
@AutoConfigureMockMvc
class NPlusOneTest {

    private static final String UUID = "nplusone-uuid";
    private static final String CODE = "1234";
    private static final String CATEGORY = "N1";
    private static final String EQUIPMENT = "批量查询测试器材";
    private static final String WAREHOUSE = "WH-N1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    private static boolean counterRegistered = false;

    @BeforeEach
    void registerCounter() {
        if (!counterRegistered) {
            sqlSessionFactory.getConfiguration().addInterceptor(new SelectCounter());
            counterRegistered = true;
        }
    }

    @Test
    void equipmentPage_shouldBatchLoadCategories() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, 4, 0);
            resetCount();
            mockMvc.perform(get("/api/equipment/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", "批量查询")
                            .param("size", "20"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
            assertTrue(queryCount() <= 4, "器材分页 SELECT 次数过多: " + queryCount());
        } finally {
            cleanup();
        }
    }

    @Test
    void stockPage_shouldBatchLoadEquipmentAndWarehouse() throws Exception {
        String token = loginAdmin();
        try {
            prepareBase(token, 4, 5);
            resetCount();
            mockMvc.perform(get("/api/stock/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", "批量查询")
                            .param("size", "20"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
            assertTrue(queryCount() <= 6, "库存分页 SELECT 次数过多: " + queryCount());
        } finally {
            cleanup();
        }
    }

    @Test
    void borrowPage_shouldBatchLoadUsersAndItems() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 3, 3);
            for (int i = 0; i < 3; i++) {
                createBorrow(token, ids, "批量查询借用" + i);
            }
            resetCount();
            mockMvc.perform(get("/api/borrow/page")
                            .header("Authorization", "Bearer " + token)
                            .param("size", "20"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
            assertTrue(queryCount() <= 7, "借用分页 SELECT 次数过多: " + queryCount());
        } finally {
            cleanup();
        }
    }

    @Test
    void returnPage_shouldBatchLoadAssociations() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 3, 3);
            for (int i = 0; i < 3; i++) {
                long borrowId = createBorrow(token, ids, "批量查询归还" + i);
                mockMvc.perform(put("/api/borrow/audit")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSONUtil.toJsonStr(Map.of(
                                        "orderId", borrowId, "pass", true))))
                        .andExpect(jsonPath("$.code").value(0));
                mockMvc.perform(put("/api/borrow/" + borrowId + "/issue")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(jsonPath("$.code").value(0));
                createReturn(token, borrowId);
            }
            resetCount();
            mockMvc.perform(get("/api/return/page")
                            .header("Authorization", "Bearer " + token)
                            .param("size", "20"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
            assertTrue(queryCount() <= 8, "归还分页 SELECT 次数过多: " + queryCount());
        } finally {
            cleanup();
        }
    }

    @Test
    void scrapPage_shouldBatchLoadAssociations() throws Exception {
        String token = loginAdmin();
        try {
            long[] ids = prepareBase(token, 1, 0);
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/api/scrap")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSONUtil.toJsonStr(Map.of(
                                        "warehouseId", ids[1],
                                        "scrapType", 2,
                                        "remark", "批量查询报废" + i,
                                        "items", List.of(Map.of(
                                                "equipmentId", ids[0],
                                                "quantity", 1,
                                                "scrapReason", "批量查询"))))))
                        .andExpect(jsonPath("$.code").value(0));
            }
            resetCount();
            mockMvc.perform(get("/api/scrap/page")
                            .header("Authorization", "Bearer " + token)
                            .param("size", "20"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.records.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
            assertTrue(queryCount() <= 6, "报废分页 SELECT 次数过多: " + queryCount());
        } finally {
            cleanup();
        }
    }

    private long[] prepareBase(String token, int equipmentCount, int stockPerEquipment) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0,
                                "categoryName", "批量查询测试分类",
                                "categoryCode", CATEGORY,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(post("/api/equipment/warehouse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseCode", WAREHOUSE,
                                "warehouseName", "批量查询测试仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        long categoryId = categoryId(token);
        long warehouseId = warehouseId(token);
        long equipmentId = 0;
        for (int i = 0; i < equipmentCount; i++) {
            String name = equipmentCount == 1 ? EQUIPMENT : EQUIPMENT + "-" + i;
            mockMvc.perform(post("/api/equipment")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentName", name,
                                    "categoryId", categoryId,
                                    "unit", "个",
                                    "safeStock", 1,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));
            equipmentId = findEquipmentId(token, name);
            if (stockPerEquipment > 0) {
                mockMvc.perform(put("/api/stock/adjust")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JSONUtil.toJsonStr(Map.of(
                                        "equipmentId", equipmentId,
                                        "warehouseId", warehouseId,
                                        "changeQuantity", stockPerEquipment))))
                        .andExpect(jsonPath("$.code").value(0));
            }
        }
        return new long[]{equipmentId, warehouseId};
    }

    private long createBorrow(String token, long[] ids, String purpose) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/borrow")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "borrowType", 1,
                                "purpose", purpose,
                                "expectedReturnDate", LocalDate.now().plusDays(7).toString(),
                                "items", List.of(Map.of(
                                        "equipmentId", ids[0],
                                        "warehouseId", ids[1],
                                        "quantity", 1))))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return ((Number) JSONUtil.parseObj(
                mockMvc.perform(get("/api/borrow/page")
                                .header("Authorization", "Bearer " + token)
                                .param("status", "0"))
                        .andReturn().getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private void createReturn(String token, long borrowId) throws Exception {
        MvcResult detail = mockMvc.perform(get("/api/borrow/" + borrowId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        long borrowItemId = ((Number) JSONUtil.parseObj(detail.getResponse().getContentAsString())
                .getByPath("data.items[0].id")).longValue();
        mockMvc.perform(post("/api/return")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "borrowOrderId", borrowId,
                                "items", List.of(Map.of(
                                        "borrowItemId", borrowItemId,
                                        "quantity", 1,
                                        "conditionStatus", 1))))))
                .andExpect(jsonPath("$.code").value(0));
    }

    private long categoryId(String token) throws Exception {
        return ((Number) JSONUtil.parseObj(mockMvc.perform(get("/api/equipment/category/tree")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())
                .getByPath("data[0].id")).longValue();
    }

    private long warehouseId(String token) throws Exception {
        MvcResult list = mockMvc.perform(get("/api/equipment/warehouse/list")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return ((Number) JSONUtil.parseObj(list.getResponse().getContentAsString())
                .getByPath("data[0].id")).longValue();
    }

    private long findEquipmentId(String token, String equipmentName) throws Exception {
        MvcResult page = mockMvc.perform(get("/api/equipment/page")
                        .header("Authorization", "Bearer " + token)
                        .param("equipmentName", equipmentName))
                .andReturn();
        return ((Number) JSONUtil.parseObj(page.getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private void resetCount() {
        SelectCounter.COUNT.set(0);
    }

    private int queryCount() {
        return SelectCounter.COUNT.get();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM stock_record WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name LIKE ?)", EQUIPMENT + "%");
        jdbcTemplate.update("DELETE ri FROM return_item ri JOIN return_order ro ON ro.id = ri.return_id " +
                "JOIN borrow_order bo ON bo.id = ro.borrow_order_id WHERE bo.purpose LIKE '批量查询%'");
        jdbcTemplate.update("DELETE ro FROM return_order ro JOIN borrow_order bo ON bo.id = ro.borrow_order_id " +
                "WHERE bo.purpose LIKE '批量查询%'");
        jdbcTemplate.update("DELETE bi FROM borrow_item bi JOIN borrow_order bo ON bo.id = bi.borrow_id " +
                "WHERE bo.purpose LIKE '批量查询%'");
        jdbcTemplate.update("DELETE FROM borrow_order WHERE purpose LIKE '批量查询%'");
        jdbcTemplate.update("DELETE si FROM scrap_item si JOIN scrap_order so ON so.id = si.scrap_id " +
                "WHERE so.remark LIKE '批量查询%'");
        jdbcTemplate.update("DELETE FROM scrap_order WHERE remark LIKE '批量查询%'");
        jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name LIKE ?)", EQUIPMENT + "%");
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name LIKE ?", EQUIPMENT + "%");
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

    @org.apache.ibatis.plugin.Intercepts({
            @Signature(type = Executor.class, method = "query",
                    args = {org.apache.ibatis.mapping.MappedStatement.class, Object.class,
                            org.apache.ibatis.session.RowBounds.class,
                            org.apache.ibatis.session.ResultHandler.class}),
            @Signature(type = Executor.class, method = "query",
                    args = {org.apache.ibatis.mapping.MappedStatement.class, Object.class,
                            org.apache.ibatis.session.RowBounds.class,
                            org.apache.ibatis.session.ResultHandler.class,
                            org.apache.ibatis.cache.CacheKey.class,
                            org.apache.ibatis.mapping.BoundSql.class}),
            @Signature(type = Executor.class, method = "queryCursor",
                    args = {org.apache.ibatis.mapping.MappedStatement.class, Object.class,
                            org.apache.ibatis.session.RowBounds.class})
    })
    static class SelectCounter implements Interceptor {

        static final ThreadLocal<Integer> COUNT = ThreadLocal.withInitial(() -> 0);

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            COUNT.set(COUNT.get() + 1);
            return invocation.proceed();
        }

        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target, this);
        }

        @Override
        public void setProperties(Properties properties) {
        }
    }
}
