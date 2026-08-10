package com.company.sportseq.controller;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import org.hamcrest.Matchers;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限管理集成测试：动态路由、用户/角色/部门 CRUD、按钮权限 403。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysPermissionTest {

    private static final String UUID = "perm-uuid";
    private static final String CODE = "1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void routers_shouldReturnMenuTree() throws Exception {
        String token = loginAdmin();
        mockMvc.perform(get("/api/system/menu/routers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].path").value("/system"))
                .andExpect(jsonPath("$.data[0].component").value("Layout"));
    }

    @Test
    void userPage_shouldReturnAdmin() throws Exception {
        String token = loginAdmin();
        mockMvc.perform(get("/api/system/user/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    @Test
    void userAddAndRemove_shouldWork() throws Exception {
        String token = loginAdmin();
        try {
            String body = JSONUtil.toJsonStr(Map.of(
                    "username", "testuser",
                    "password", "Test1234",
                    "realName", "测试用户",
                    "userType", "other",
                    "roleIds", List.of(5L)));
            mockMvc.perform(post("/api/system/user")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            MvcResult page = mockMvc.perform(get("/api/system/user/page")
                            .header("Authorization", "Bearer " + token)
                            .param("username", "testuser"))
                    .andExpect(jsonPath("$.data.records[0].username").value("testuser"))
                    .andReturn();
            Object id = JSONUtil.parseObj(page.getResponse().getContentAsString())
                    .getByPath("data.records[0].id");

            mockMvc.perform(delete("/api/system/user/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            cleanupUser("testuser");
        }
    }

    @Test
    void roleAssignMenus_shouldPersist() throws Exception {
        String token = loginAdmin();
        String body = JSONUtil.toJsonStr(List.of(1L, 100L, 1001L));
        mockMvc.perform(put("/api/system/role/2/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/system/role/2/menus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void deptTree_shouldReturnHierarchy() throws Exception {
        String token = loginAdmin();
        mockMvc.perform(get("/api/system/dept/tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].deptName").value("总部"))
                .andExpect(jsonPath("$.data[0].children[0].deptName").value("体育部"));
    }

    @Test
    void commonUser_shouldGet403OnAdminApi() throws Exception {
        try {
            cleanupUser("testcommon");
            String adminToken = loginAdmin();
            String body = JSONUtil.toJsonStr(Map.of(
                    "username", "testcommon",
                    "password", "Test1234",
                    "realName", "普通测试用户",
                    "userType", "other",
                    "roleIds", List.of(5L)));
            mockMvc.perform(post("/api/system/user")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(jsonPath("$.code").value(0));

            String commonToken = login("testcommon", "Test1234");
            mockMvc.perform(get("/api/system/user/page")
                            .header("Authorization", "Bearer " + commonToken))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(403));
        } finally {
            cleanupUser("testcommon");
        }
    }

    private String loginAdmin() throws Exception {
        return login("admin", "admin123");
    }

    private String login(String username, String password) throws Exception {
        redisTemplate.opsForValue().set(
                CacheConstants.CAPTCHA_CODE_KEY + UUID, CODE, Duration.ofMinutes(5));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "username", username,
                                "password", password,
                                "code", CODE,
                                "uuid", UUID))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return (String) JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getByPath("data.token");
    }

    private void cleanupUser(String username) {
        jdbcTemplate.update("DELETE ur FROM sys_user_role ur " +
                "INNER JOIN sys_user u ON ur.user_id = u.id WHERE u.username = ?", username);
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = ?", username);
    }
}
