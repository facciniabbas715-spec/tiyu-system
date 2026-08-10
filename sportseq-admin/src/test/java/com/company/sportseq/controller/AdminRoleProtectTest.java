package com.company.sportseq.controller;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
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
 * 中危项 4：内置 admin 角色禁止修改角色标识/状态与清空核心菜单。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminRoleProtectTest {

    private static final String UUID = "admin-role-uuid";
    private static final String CODE = "1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void updateAdminRole_shouldRejectRoleKeyOrStatusChange() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(put("/api/system/role")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", 1L,
                                    "roleName", "系统管理员",
                                    "roleKey", "hacker",
                                    "roleSort", 1,
                                    "dataScope", 1,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(1001));

            mockMvc.perform(put("/api/system/role")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", 1L,
                                    "roleName", "系统管理员",
                                    "roleKey", "admin",
                                    "roleSort", 1,
                                    "dataScope", 1,
                                    "status", 0))))
                    .andExpect(jsonPath("$.code").value(1001));

            assertRoleKeyAndStatus(token, "admin", 1);
        } finally {
            // 不改动数据库，无需清理
        }
    }

    @Test
    void assignMenusToAdminRole_shouldRejectClearingMenus() throws Exception {
        String token = loginAdmin();
        List<Long> initialMenuIds = currentAdminMenuIds(token);
        try {
            mockMvc.perform(put("/api/system/role/1/menus")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[]"))
                    .andExpect(jsonPath("$.code").value(1001));
            mockMvc.perform(put("/api/system/role/1/menus")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(List.of(1L))))
                    .andExpect(jsonPath("$.code").value(1001));

            mockMvc.perform(get("/api/system/role/1/menus")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(10)))
                    .andExpect(jsonPath("$.data[?(@ == 1)]").isNotEmpty())
                    .andExpect(jsonPath("$.data[?(@ == 2)]").isNotEmpty())
                    .andExpect(jsonPath("$.data[?(@ == 7)]").isNotEmpty());
        } finally {
            mockMvc.perform(put("/api/system/role/1/menus")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(initialMenuIds)))
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Test
    void updateAdminRole_withFullMenus_shouldSucceed() throws Exception {
        String token = loginAdmin();
        try {
            List<Long> currentMenuIds = currentAdminMenuIds(token);
            mockMvc.perform(put("/api/system/role")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", 1L,
                                    "roleName", "系统管理员-测试更新",
                                    "roleKey", "admin",
                                    "roleSort", 1,
                                    "dataScope", 1,
                                    "status", 1,
                                    "menuIds", currentMenuIds))))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/system/role/1/menus")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data.length()").value(currentMenuIds.size()));
        } finally {
            restoreAdminRole(token);
        }
    }

    private void assertRoleKeyAndStatus(String token, String roleKey, int status) throws Exception {
        MvcResult page = mockMvc.perform(get("/api/system/role/page")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "100"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        var records = JSONUtil.parseObj(page.getResponse().getContentAsString())
                .getJSONObject("data").getJSONArray("records");
        for (Object row : records) {
            Map<String, Object> role = (Map<String, Object>) row;
            if (((Number) role.get("id")).longValue() == 1L) {
                if (!roleKey.equals(role.get("roleKey")) || ((Number) role.get("status")).intValue() != status) {
                    throw new AssertionError("内置 admin 角色被篡改: " + role);
                }
                return;
            }
        }
        throw new AssertionError("未找到内置 admin 角色");
    }

    private List<Long> currentAdminMenuIds(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/system/role/1/menus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getJSONArray("data").stream()
                .map(v -> ((Number) v).longValue())
                .toList();
    }

    private void restoreAdminRole(String token) throws Exception {
        List<Long> menuIds = currentAdminMenuIds(token);
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
