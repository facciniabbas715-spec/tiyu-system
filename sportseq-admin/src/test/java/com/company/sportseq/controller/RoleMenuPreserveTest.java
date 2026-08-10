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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 复查：角色编辑表单不带 menuIds 时不得清空已分配菜单，admin 角色编辑不得被拦截。
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoleMenuPreserveTest {

    private static final String UUID = "role-menu-preserve-uuid";
    private static final String CODE = "1234";
    private static final String ROLE = "preserve_role";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updateRole_withoutMenuIds_shouldKeepMenuAssignments() throws Exception {
        String token = loginAdmin();
        try {
            long roleId = createRole(token);
            assertMenus(token, roleId, List.of(400L, 4001L));

            mockMvc.perform(put("/api/system/role")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", roleId,
                                    "roleName", "保留菜单测试角色-改",
                                    "roleKey", ROLE,
                                    "roleSort", 99,
                                    "dataScope", 1,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));
            assertMenus(token, roleId, List.of(400L, 4001L));
        } finally {
            cleanup();
        }
    }

    @Test
    void updateAdminRole_withoutMenuIds_shouldSucceedAndKeepAllMenus() throws Exception {
        String token = loginAdmin();
        try {
            List<Long> before = currentMenuIds(token, 1L);
            mockMvc.perform(put("/api/system/role")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", 1L,
                                    "roleName", "系统管理员-复查",
                                    "roleKey", "admin",
                                    "roleSort", 1,
                                    "dataScope", 1,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));
            assertTrue(currentMenuIds(token, 1L).size() == before.size(),
                    "admin 角色菜单被改动");
        } finally {
            restoreAdminRole(token);
        }
    }

    private long createRole(String token) throws Exception {
        mockMvc.perform(post("/api/system/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "roleName", "保留菜单测试角色",
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

    private void assertMenus(String token, long roleId, List<Long> expected) throws Exception {
        List<Long> actual = currentMenuIds(token, roleId);
        if (!actual.containsAll(expected) || actual.size() != expected.size()) {
            throw new AssertionError("角色菜单被改动: expected=" + expected + ", actual=" + actual);
        }
    }

    private List<Long> currentMenuIds(String token, long roleId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/system/role/" + roleId + "/menus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getJSONArray("data").stream()
                .map(v -> ((Number) v).longValue())
                .toList();
    }

    private void restoreAdminRole(String token) throws Exception {
        List<Long> menuIds = currentMenuIds(token, 1L);
        mockMvc.perform(put("/api/system/role")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "id", 1L,
                                "roleName", "系统管理员",
                                "roleKey", "admin",
                                "roleSort", 1,
                                "dataScope", 1,
                                "status", 1,
                                "menuIds", menuIds))))
                .andExpect(jsonPath("$.code").value(0));
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id IN " +
                "(SELECT id FROM sys_role WHERE role_key = ?)", ROLE);
        jdbcTemplate.update("DELETE FROM sys_role WHERE role_key = ?", ROLE);
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
