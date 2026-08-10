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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中危项 1：删除 / 重置密码 / 修改角色后，旧会话必须被踢出（Redis 会话失效）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserSessionKickTest {

    private static final String CODE = "1234";
    private static final String USERNAME = "kick_user";
    private static final String PASSWORD = "Test1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void resetPassword_shouldKickOldSessions() throws Exception {
        String admin = login("admin", "admin123", "kick-uuid-admin-1");
        try {
            long userId = createUser(admin);
            String oldToken = login(USERNAME, PASSWORD, "kick-uuid-user-1");
            assertSessionValid(oldToken);

            mockMvc.perform(put("/api/system/user/resetPassword")
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "userId", userId,
                                    "password", "Newpass123"))))
                    .andExpect(jsonPath("$.code").value(0));

            assertSessionKicked(oldToken);
            String newToken = login(USERNAME, "Newpass123", "kick-uuid-user-2");
            assertSessionValid(newToken);
        } finally {
            cleanup();
        }
    }

    @Test
    void updateRole_shouldKickOldSessions() throws Exception {
        String admin = login("admin", "admin123", "kick-uuid-admin-2");
        try {
            long userId = createUser(admin);
            String oldToken = login(USERNAME, PASSWORD, "kick-uuid-user-3");
            assertSessionValid(oldToken);

            mockMvc.perform(put("/api/system/user")
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "id", userId,
                                    "username", USERNAME,
                                    "realName", "踢会话测试用户",
                                    "userType", "other",
                                    "status", 1,
                                    "roleIds", List.of(4L)))))
                    .andExpect(jsonPath("$.code").value(0));

            assertSessionKicked(oldToken);
        } finally {
            cleanup();
        }
    }

    @Test
    void removeUser_shouldKickOldSessions() throws Exception {
        String admin = login("admin", "admin123", "kick-uuid-admin-3");
        try {
            long userId = createUser(admin);
            String oldToken = login(USERNAME, PASSWORD, "kick-uuid-user-4");
            assertSessionValid(oldToken);

            mockMvc.perform(delete("/api/system/user/" + userId)
                            .header("Authorization", "Bearer " + admin))
                    .andExpect(jsonPath("$.code").value(0));

            assertSessionKicked(oldToken);
        } finally {
            cleanup();
        }
    }

    private long createUser(String adminToken) throws Exception {
        mockMvc.perform(post("/api/system/user")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "username", USERNAME,
                                "password", PASSWORD,
                                "realName", "踢会话测试用户",
                                "userType", "other",
                                "status", 1,
                                "roleIds", List.of(5L)))))
                .andExpect(jsonPath("$.code").value(0));
        MvcResult page = mockMvc.perform(get("/api/system/user/page")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("username", USERNAME))
                .andReturn();
        return ((Number) JSONUtil.parseObj(page.getResponse().getContentAsString())
                .getByPath("data.records[0].id")).longValue();
    }

    private void assertSessionValid(String token) throws Exception {
        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void assertSessionKicked(String token) throws Exception {
        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
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

    private void cleanup() {
        jdbcTemplate.update("DELETE ur FROM sys_user_role ur " +
                "INNER JOIN sys_user u ON ur.user_id = u.id WHERE u.username = ?", USERNAME);
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = ?", USERNAME);
    }
}
