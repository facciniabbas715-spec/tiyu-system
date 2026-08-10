package com.company.sportseq.controller;

import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.entity.SysUser;
import com.company.sportseq.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证全链路集成测试：验证码 -> 登录 -> info -> 登出 -> 改密（依赖本地 Redis 与 MySQL）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String UUID = "test-uuid";
    private static final String CODE = "1234";
    private static final String ADMIN_PWD_HASH =
            "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SysUserMapper userMapper;

    @Test
    void loginWithValidCaptcha_shouldReturnTokenAndInfo() throws Exception {
        seedCaptcha();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "admin123", CODE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andReturn();

        String token = extractToken(result);
        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    void loginWithWrongCaptcha_shouldReturnCaptchaError() throws Exception {
        seedCaptcha();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "admin123", "0000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    void loginWithWrongPassword_shouldReturnUnauthorized() throws Exception {
        seedCaptcha();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "wrong-password", CODE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2001));
    }

    @Test
    void logout_shouldInvalidateSession() throws Exception {
        seedCaptcha();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "admin123", CODE)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String token = extractToken(result);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/auth/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updatePassword_shouldTakeEffectAndSelfHeal() throws Exception {
        seedCaptcha();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "admin123", CODE)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String token = extractToken(result);

        mockMvc.perform(put("/api/auth/updatePassword")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"admin123\",\"newPassword\":\"Admin1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 新密码可登录
        seedCaptcha();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "Admin1234", CODE)))
                .andExpect(jsonPath("$.code").value(0));

        // 恢复种子密码，保证测试可重复
        SysUser restore = new SysUser();
        restore.setId(1L);
        restore.setPassword(ADMIN_PWD_HASH);
        userMapper.updateById(restore);
    }

    private void seedCaptcha() {
        redisTemplate.opsForValue().set(
                CacheConstants.CAPTCHA_CODE_KEY + UUID, CODE, Duration.ofMinutes(5));
    }

    private String loginBody(String username, String password, String code) {
        return JSONUtil.toJsonStr(Map.of(
                "username", username,
                "password", password,
                "code", code,
                "uuid", UUID));
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return (String) JSONUtil.parseObj(body).getByPath("data.token");
    }
}
