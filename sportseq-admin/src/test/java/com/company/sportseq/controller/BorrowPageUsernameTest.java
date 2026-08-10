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
 * 中危项 6：借用分页按借用人姓名/账号过滤必须由 SQL 完成，total 准确。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BorrowPageUsernameTest {

    private static final String UUID = "borrow-page-uuid";
    private static final String CODE = "1234";
    private static final String CATEGORY = "BP";
    private static final String EQUIPMENT = "借用分页测试器材";
    private static final String WAREHOUSE = "WH-BP";
    private static final String ROLE = "borrow_page_role";
    private static final String USERNAME = "borrow_page_user";
    private static final String PASSWORD = "Test1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void borrowPageByUsername_shouldFilterInSqlAndReturnAccurateTotal() throws Exception {
        String admin = login("admin", "admin123", "borrow-page-admin");
        try {
            long[] ids = prepareBase(admin, 5);
            // 前 2 条管理员借用
            createBorrow(admin, ids, "管理员借用A");
            createBorrow(admin, ids, "管理员借用B");
            // 中间 1 条测试用户借用
            long roleId = createRole(admin);
            createUser(admin, roleId);
            String userToken = login(USERNAME, PASSWORD, "borrow-page-user");
            createBorrow(userToken, ids, "测试用户借用");
            // 后 2 条管理员借用，保证 SQL 首页最新记录是管理员的
            createBorrow(admin, ids, "管理员借用C");
            createBorrow(admin, ids, "管理员借用D");

            mockMvc.perform(get("/api/borrow/page")
                            .header("Authorization", "Bearer " + admin)
                            .param("current", "1")
                            .param("size", "1")
                            .param("username", "borrow_page"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].purpose").value("测试用户借用"));

            mockMvc.perform(get("/api/borrow/page")
                            .header("Authorization", "Bearer " + admin)
                            .param("size", "10")
                            .param("username", "不存在的用户"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(0));
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
                                "categoryName", "借用分页测试分类",
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
                                "warehouseName", "借用分页测试仓库",
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        long equipmentId = equipmentId(token);
        long warehouseId = warehouseId(token);
        mockMvc.perform(put("/api/stock/adjust")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentId", equipmentId,
                                "warehouseId", warehouseId,
                                "changeQuantity", stockQuantity))))
                .andExpect(jsonPath("$.code").value(0));
        return new long[]{equipmentId, warehouseId};
    }

    private long createRole(String token) throws Exception {
        mockMvc.perform(post("/api/system/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "roleName", "借用分页测试角色",
                                "roleKey", ROLE,
                                "roleSort", 99,
                                "dataScope", 1,
                                "status", 1,
                                "menuIds", List.of(400L, 4001L)))))
                .andExpect(jsonPath("$.code").value(0));
        MvcResult page = mockMvc.perform(get("/api/system/role/page")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "200"))
                .andReturn();
        return JSONUtil.parseObj(page.getResponse().getContentAsString())
                .getJSONObject("data").getJSONArray("records").stream()
                .map(row -> (Map<String, Object>) row)
                .filter(r -> ROLE.equals(r.get("roleKey")))
                .map(r -> ((Number) r.get("id")).longValue())
                .findFirst().orElseThrow(() -> new AssertionError("角色未创建"));
    }

    private void createUser(String token, long roleId) throws Exception {
        mockMvc.perform(post("/api/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "username", USERNAME,
                                "password", PASSWORD,
                                "realName", "借用分页测试用户",
                                "userType", "other",
                                "status", 1,
                                "roleIds", List.of(roleId)))))
                .andExpect(jsonPath("$.code").value(0));
    }

    private void createBorrow(String token, long[] ids, String purpose) throws Exception {
        mockMvc.perform(post("/api/borrow")
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
                .andExpect(jsonPath("$.code").value(0));
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
        jdbcTemplate.update("DELETE FROM stock_record WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name = ?)", EQUIPMENT);
        jdbcTemplate.update("DELETE bi FROM borrow_item bi JOIN borrow_order bo ON bo.id = bi.borrow_id " +
                "WHERE bo.purpose IN ('管理员借用A','管理员借用B','管理员借用C','管理员借用D','测试用户借用')");
        jdbcTemplate.update("DELETE FROM borrow_order WHERE purpose IN " +
                "('管理员借用A','管理员借用B','管理员借用C','管理员借用D','测试用户借用')");
        jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name = ?)", EQUIPMENT);
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name = ?", EQUIPMENT);
        jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code = ?", CATEGORY);
        jdbcTemplate.update("DELETE FROM warehouse WHERE warehouse_code = ?", WAREHOUSE);
        jdbcTemplate.update("DELETE ur FROM sys_user_role ur " +
                "INNER JOIN sys_user u ON ur.user_id = u.id WHERE u.username = ?", USERNAME);
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = ?", USERNAME);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN " +
                "(SELECT id FROM sys_role WHERE role_key = ?)", ROLE);
        jdbcTemplate.update("DELETE FROM sys_role WHERE role_key = ?", ROLE);
    }

    private String login(String username, String password, String uuid) throws Exception {
        redisTemplate.opsForValue().set(
                CacheConstants.CAPTCHA_CODE_KEY + uuid, CODE, Duration.ofMinutes(5));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "username", username,
                                "password", password,
                                "code", CODE,
                                "uuid", uuid))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (String) JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getByPath("data.token");
    }
}
