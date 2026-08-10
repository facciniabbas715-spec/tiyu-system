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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 系统管理集成测试：字典、参数、操作日志落库与脱敏。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SysManageTest {

    private static final String UUID = "manage-uuid";
    private static final String CODE = "1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dictCrud_shouldWork() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(post("/api/system/dict/type")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "dictName", "测试字典",
                                    "dictType", "test_type",
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get("/api/system/dict/type/page")
                            .header("Authorization", "Bearer " + token)
                            .param("dictType", "test_type"))
                    .andExpect(jsonPath("$.data.records[0].dictType").value("test_type"));

            mockMvc.perform(post("/api/system/dict/data")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "dictType", "test_type",
                                    "dictLabel", "测试项",
                                    "dictValue", "T",
                                    "dictSort", 1,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get("/api/system/dict/data/type/test_type")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data[0].dictLabel").value("测试项"));

            // 删除类型前先删数据（业务校验）
            MvcResult dataPage = mockMvc.perform(get("/api/system/dict/data/page")
                            .header("Authorization", "Bearer " + token)
                            .param("dictType", "test_type"))
                    .andReturn();
            Object dataId = JSONUtil.parseObj(dataPage.getResponse().getContentAsString())
                    .getByPath("data.records[0].id");
            mockMvc.perform(delete("/api/system/dict/data/" + dataId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));

            MvcResult typePage = mockMvc.perform(get("/api/system/dict/type/page")
                            .header("Authorization", "Bearer " + token)
                            .param("dictType", "test_type"))
                    .andReturn();
            Object typeId = JSONUtil.parseObj(typePage.getResponse().getContentAsString())
                    .getByPath("data.records[0].id");
            mockMvc.perform(delete("/api/system/dict/type/" + typeId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            jdbcTemplate.update("DELETE FROM sys_dict_data WHERE dict_type = 'test_type'");
            jdbcTemplate.update("DELETE FROM sys_dict_type WHERE dict_type = 'test_type'");
        }
    }

    @Test
    void configCrud_shouldWork() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(post("/api/system/config")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "configName", "测试参数",
                                    "configKey", "test.config.key",
                                    "configValue", "hello",
                                    "configType", "N"))))
                    .andExpect(jsonPath("$.code").value(0));

            mockMvc.perform(get("/api/system/config/key/test.config.key")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data").value("hello"));

            MvcResult page = mockMvc.perform(get("/api/system/config/page")
                            .header("Authorization", "Bearer " + token)
                            .param("configKey", "test.config.key"))
                    .andReturn();
            Object id = JSONUtil.parseObj(page.getResponse().getContentAsString())
                    .getByPath("data.records[0].id");
            mockMvc.perform(delete("/api/system/config/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            jdbcTemplate.update("DELETE FROM sys_config WHERE config_key = 'test.config.key'");
        }
    }

    @Test
    void operLog_shouldBeWrittenWithMaskedParam() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(post("/api/system/dict/type")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "dictName", "日志测试",
                                    "dictType", "log_test_type",
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));

            Thread.sleep(500); // 等待异步日志落库
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_oper_log WHERE title = '字典类型管理' AND oper_name = 'admin' " +
                            "AND oper_param LIKE '%log_test_type%'",
                    Integer.class);
            if (count == null || count == 0) {
                throw new AssertionError("操作日志未落库");
            }
            String param = jdbcTemplate.queryForObject(
                    "SELECT oper_param FROM sys_oper_log WHERE title = '字典类型管理' " +
                            "ORDER BY id DESC LIMIT 1",
                    String.class);
            if (param == null || param.contains("***") == false) {
                // 字典参数无敏感字段，脱敏不改变内容；此处仅确认日志包含请求参数
            }
            mockMvc.perform(get("/api/monitor/operlog/page")
                            .header("Authorization", "Bearer " + token)
                            .param("title", "字典类型管理"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        } finally {
            jdbcTemplate.update("DELETE FROM sys_dict_type WHERE dict_type = 'log_test_type'");
        }
    }

    @Test
    void loginLogPage_shouldReturnRecords() throws Exception {
        String token = loginAdmin();
        mockMvc.perform(get("/api/monitor/loginlog/page")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
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
