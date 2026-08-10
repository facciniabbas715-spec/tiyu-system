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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据权限集成测试：本部门（data_scope=2）与仅本人（data_scope=4）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class DataScopeTest {

    private static final String CODE = "1234";
    private static final String CAT = "DS";
    private static final String EQUIP = "数据权限测试器材";
    private static final String WH = "WH-DS";
    private static final String DEPT_A = "数据权限测试A部";
    private static final String DEPT_B = "数据权限测试B部";
    private static final String ROLE_DEPT = "scope_dept";
    private static final String ROLE_SELF = "scope_self";
    private static final String USER_A = "scope_a";
    private static final String USER_C = "scope_c";
    private static final String PWD = "Test1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deptScope_shouldOnlySeeOwnDeptData() throws Exception {
        String admin = login("admin", "ds-uuid-admin");
        try {
            long[] ids = prepareBase(admin);
            long deptA = createDept(admin, DEPT_A);
            long deptB = createDept(admin, DEPT_B);
            long roleDept = createRole(admin, ROLE_DEPT, 2);
            createUser(admin, USER_A, deptA, roleDept);
            createUser(admin, USER_C, deptB, roleDept);

            long adminBorrow = createBorrow(admin, "DS-ADMIN", 2, ids);
            mockMvc.perform(put("/api/borrow/audit")
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", adminBorrow, "pass", true))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/borrow/" + adminBorrow + "/issue")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(jsonPath("$.code").value(0));
            long adminReturn = createReturn(admin, adminBorrow,
                    borrowItemId(admin, adminBorrow), 2);

            String userA = login(USER_A, "ds-uuid-a");
            long userABorrow = createBorrow(userA, "DS-A", 1, ids);

            // 本部门用户只能看到本部门借用单
            List<String> purposes = borrowPurposes(userA);
            assertTrue(purposes.contains("DS-A"), "应能看到本部门借用单");
            assertFalse(purposes.contains("DS-ADMIN"), "不应看到总部借用单");
            assertFalse(purposes.contains("DS-C"), "不应看到其他部门借用单");

            // 本部门用户看不到总部借用单详情/无法操作
            mockMvc.perform(get("/api/borrow/" + adminBorrow)
                            .header("Authorization", "Bearer " + userA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
            mockMvc.perform(get("/api/borrow/" + userABorrow)
                            .header("Authorization", "Bearer " + userA))
                    .andExpect(jsonPath("$.code").value(0));

            // 归还单列表同样过滤
            List<Long> returnIds = returnIds(userA);
            assertFalse(returnIds.contains(adminReturn), "不应看到总部归还单");

            // 用户列表只看本部门
            List<String> usernames = userPageUsernames(userA);
            assertTrue(usernames.contains(USER_A));
            assertFalse(usernames.contains("admin"));
            assertFalse(usernames.contains(USER_C));
        } finally {
            cleanup();
        }
    }

    @Test
    void selfScope_shouldOnlySeeOwnData() throws Exception {
        String admin = login("admin", "ds-uuid-admin2");
        try {
            long[] ids = prepareBase(admin);
            long deptB = createDept(admin, DEPT_B);
            long roleSelf = createRole(admin, ROLE_SELF, 4);
            createUser(admin, USER_C, deptB, roleSelf);

            long adminBorrow = createBorrow(admin, "DS-ADMIN", 2, ids);
            mockMvc.perform(put("/api/borrow/audit")
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of("orderId", adminBorrow, "pass", true))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(put("/api/borrow/" + adminBorrow + "/issue")
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(jsonPath("$.code").value(0));

            String userC = login(USER_C, "ds-uuid-c");
            long userCBorrow = createBorrow(userC, "DS-C", 1, ids);

            List<String> purposes = borrowPurposes(userC);
            assertTrue(purposes.contains("DS-C"));
            assertFalse(purposes.contains("DS-ADMIN"), "仅本人不应看到他人借用单");

            mockMvc.perform(get("/api/borrow/" + adminBorrow)
                            .header("Authorization", "Bearer " + userC))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
            mockMvc.perform(get("/api/borrow/" + userCBorrow)
                            .header("Authorization", "Bearer " + userC))
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            cleanup();
        }
    }

    @Test
    void rolePage_shouldReturnDataScope() throws Exception {
        String admin = login("admin", "ds-uuid-admin3");
        try {
            createRole(admin, ROLE_DEPT, 2);
            MvcResult page = mockMvc.perform(get("/api/system/role/page")
                            .header("Authorization", "Bearer " + admin)
                            .param("size", "200"))
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            List<Map<String, Object>> records = JSONUtil.parseObj(page.getResponse().getContentAsString())
                    .getJSONObject("data").getJSONArray("records").stream()
                    .map(row -> (Map<String, Object>) row)
                    .toList();
            Map<String, Object> role = records.stream()
                    .filter(r -> ROLE_DEPT.equals(r.get("roleKey")))
                    .findFirst().orElseThrow(() -> new AssertionError("角色未创建"));
            assertTrue(((Number) role.get("dataScope")).intValue() == 2, "角色应返回 dataScope=2");
        } finally {
            cleanup();
        }
    }

    private long[] prepareBase(String token) throws Exception {
        mockMvc.perform(post("/api/equipment/category")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 0, "categoryName", "数据权限测试分类",
                                "categoryCode", CAT, "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        long categoryId = findId(token,
                "/api/equipment/category/tree", "categoryCode", CAT);
        mockMvc.perform(post("/api/equipment")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentName", EQUIP, "categoryId", categoryId,
                                "unit", "个", "purchasePrice", 100, "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        long equipmentId = findId(token,
                "/api/equipment/page?current=1&size=50&equipmentName=数据权限", "equipmentName", EQUIP);
        mockMvc.perform(post("/api/equipment/warehouse")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "warehouseCode", WH, "warehouseName", "数据权限测试仓库", "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        long warehouseId = findId(token, "/api/equipment/warehouse/list", "warehouseCode", WH);
        mockMvc.perform(put("/api/stock/adjust")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "equipmentId", equipmentId, "warehouseId", warehouseId,
                                "changeQuantity", 10))))
                .andExpect(jsonPath("$.code").value(0));
        return new long[]{equipmentId, warehouseId};
    }

    private long createDept(String token, String deptName) throws Exception {
        mockMvc.perform(post("/api/system/dept")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", 100, "deptName", deptName, "orderNum", 99, "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        return findId(token, "/api/system/dept/tree", "deptName", deptName);
    }

    private long createRole(String token, String roleKey, int dataScope) throws Exception {
        mockMvc.perform(post("/api/system/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "roleName", "数据权限角色-" + roleKey,
                                "roleKey", roleKey,
                                "roleSort", 99,
                                "dataScope", dataScope,
                                "status", 1,
                                "menuIds", List.of(100L, 400L, 4001L, 401L, 4011L)))))
                .andExpect(jsonPath("$.code").value(0));
        MvcResult page = mockMvc.perform(get("/api/system/role/page")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "200"))
                .andReturn();
        return records(page).stream()
                .filter(r -> roleKey.equals(r.get("roleKey")))
                .map(r -> ((Number) r.get("id")).longValue())
                .findFirst().orElseThrow(() -> new AssertionError("角色未创建"));
    }

    private void createUser(String token, String username, long deptId, long roleId) throws Exception {
        mockMvc.perform(post("/api/system/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "username", username,
                                "password", PWD,
                                "realName", "数据权限-" + username,
                                "deptId", deptId,
                                "userType", "teacher",
                                "status", 1,
                                "roleIds", List.of(roleId)))))
                .andExpect(jsonPath("$.code").value(0));
    }

    private long createBorrow(String token, String purpose, int quantity, long[] ids) throws Exception {
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
                                        "quantity", quantity))))))
                .andExpect(jsonPath("$.code").value(0));
        MvcResult page = mockMvc.perform(get("/api/borrow/page")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "200"))
                .andReturn();
        return records(page).stream()
                .filter(r -> purpose.equals(r.get("purpose")))
                .map(r -> ((Number) r.get("id")).longValue())
                .findFirst().orElseThrow(() -> new AssertionError("借用单未创建"));
    }

    private long borrowItemId(String token, long borrowId) throws Exception {
        MvcResult detail = mockMvc.perform(get("/api/borrow/" + borrowId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return ((Number) JSONUtil.parseObj(detail.getResponse().getContentAsString())
                .getByPath("data.items[0].id")).longValue();
    }

    private long createReturn(String token, long borrowId, long borrowItemId, int quantity) throws Exception {
        mockMvc.perform(post("/api/return")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "borrowOrderId", borrowId,
                                "items", List.of(Map.of(
                                        "borrowItemId", borrowItemId,
                                        "quantity", quantity,
                                        "conditionStatus", 1))))))
                .andExpect(jsonPath("$.code").value(0));
        MvcResult page = mockMvc.perform(get("/api/return/page")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "200"))
                .andReturn();
        return records(page).stream()
                .filter(r -> ((Number) r.get("borrowOrderId")).longValue() == borrowId)
                .map(r -> ((Number) r.get("id")).longValue())
                .findFirst().orElseThrow(() -> new AssertionError("归还单未创建"));
    }

    private List<String> borrowPurposes(String token) throws Exception {
        MvcResult page = mockMvc.perform(get("/api/borrow/page")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "200"))
                .andReturn();
        return records(page).stream()
                .map(r -> (String) r.get("purpose"))
                .toList();
    }

    private List<Long> returnIds(String token) throws Exception {
        MvcResult page = mockMvc.perform(get("/api/return/page")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "200"))
                .andReturn();
        return records(page).stream()
                .map(r -> ((Number) r.get("id")).longValue())
                .toList();
    }

    private List<String> userPageUsernames(String token) throws Exception {
        MvcResult page = mockMvc.perform(get("/api/system/user/page")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "200"))
                .andReturn();
        return records(page).stream()
                .map(r -> (String) r.get("username"))
                .toList();
    }

    private List<Map<String, Object>> records(MvcResult result) throws Exception {
        return JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getJSONObject("data").getJSONArray("records").stream()
                .map(row -> (Map<String, Object>) row)
                .toList();
    }

    private long findId(String token, String url, String key, String value) throws Exception {
        MvcResult result = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        cn.hutool.json.JSONObject body = JSONUtil.parseObj(result.getResponse().getContentAsString());
        Object dataNode = body.get("data");
        List<Map<String, Object>> data;
        if (dataNode instanceof cn.hutool.json.JSONArray array) {
            data = array.stream().map(row -> (Map<String, Object>) row).toList();
        } else if (dataNode instanceof cn.hutool.json.JSONObject object) {
            data = object.getJSONArray("records").stream()
                    .map(row -> (Map<String, Object>) row)
                    .toList();
        } else {
            throw new AssertionError("接口返回结构异常: " + url);
        }
        Set<Long> ids = new HashSet<>();
        collectIds(data, ids);
        return ids.stream()
                .filter(id -> matchesValue(data, id, key, value))
                .findFirst().orElseThrow(() -> new AssertionError("未找到 " + key + "=" + value));
    }

    private void collectIds(List<Map<String, Object>> nodes, Set<Long> ids) {
        for (Map<String, Object> node : nodes) {
            ids.add(((Number) node.get("id")).longValue());
            Object children = node.get("children");
            if (children instanceof List<?> list && !list.isEmpty()) {
                collectIds(list.stream().map(c -> (Map<String, Object>) c).toList(), ids);
            }
        }
    }

    private boolean matchesValue(List<Map<String, Object>> nodes, long id, String key, String value) {
        for (Map<String, Object> node : nodes) {
            if (((Number) node.get("id")).longValue() == id && value.equals(node.get(key))) {
                return true;
            }
            Object children = node.get("children");
            if (children instanceof List<?> list && !list.isEmpty()
                    && matchesValue(list.stream().map(c -> (Map<String, Object>) c).toList(), id, key, value)) {
                return true;
            }
        }
        return false;
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE ri FROM return_item ri JOIN return_order ro ON ro.id = ri.return_id " +
                "JOIN borrow_order bo ON bo.id = ro.borrow_order_id WHERE bo.purpose LIKE 'DS-%'");
        jdbcTemplate.update("DELETE ro FROM return_order ro JOIN borrow_order bo ON bo.id = ro.borrow_order_id " +
                "WHERE bo.purpose LIKE 'DS-%'");
        jdbcTemplate.update("DELETE bi FROM borrow_item bi JOIN borrow_order bo ON bo.id = bi.borrow_id " +
                "WHERE bo.purpose LIKE 'DS-%'");
        jdbcTemplate.update("DELETE FROM borrow_order WHERE purpose LIKE 'DS-%'");
        jdbcTemplate.update("DELETE FROM stock_record WHERE ref_order_no LIKE 'JY%'");
        jdbcTemplate.update("DELETE FROM equipment_stock WHERE equipment_id IN " +
                "(SELECT id FROM equipment WHERE equipment_name = ?)", EQUIP);
        jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name = ?", EQUIP);
        jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code = ?", CAT);
        jdbcTemplate.update("DELETE FROM warehouse WHERE warehouse_code = ?", WH);
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id IN " +
                "(SELECT id FROM sys_user WHERE username IN (?, ?))", USER_A, USER_C);
        jdbcTemplate.update("DELETE FROM sys_user WHERE username IN (?, ?)", USER_A, USER_C);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN " +
                "(SELECT id FROM sys_role WHERE role_key IN (?, ?))", ROLE_DEPT, ROLE_SELF);
        jdbcTemplate.update("DELETE FROM sys_role WHERE role_key IN (?, ?)", ROLE_DEPT, ROLE_SELF);
        jdbcTemplate.update("DELETE FROM sys_dept WHERE dept_name IN (?, ?)", DEPT_A, DEPT_B);
    }

    private String login(String username, String uuid) throws Exception {
        redisTemplate.opsForValue().set(
                CacheConstants.CAPTCHA_CODE_KEY + uuid, CODE, Duration.ofMinutes(5));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "username", username,
                                "password", "admin".equals(username) ? "admin123" : PWD,
                                "code", CODE,
                                "uuid", uuid))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (String) JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getByPath("data.token");
    }
}
