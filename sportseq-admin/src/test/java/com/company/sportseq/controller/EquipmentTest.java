package com.company.sportseq.controller;

import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.dto.EquipmentImportDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 器材基础资料集成测试：分类树、器材编码、Excel 导入、仓库。
 */
@SpringBootTest
@AutoConfigureMockMvc
class EquipmentTest {

    private static final String UUID = "equip-uuid";
    private static final String CODE = "1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void categoryAndEquipmentCrud_shouldWork() throws Exception {
        String token = loginAdmin();
        try {
            MvcResult catResult = mockMvc.perform(post("/api/equipment/category")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "parentId", 0,
                                    "categoryName", "球类测试",
                                    "categoryCode", "QX",
                                    "sortOrder", 1,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            Long categoryId = ((Number) JSONUtil.parseObj(
                    mockMvc.perform(get("/api/equipment/category/tree")
                                    .header("Authorization", "Bearer " + token))
                            .andExpect(jsonPath("$.data[0].categoryCode").value("QX"))
                            .andReturn().getResponse().getContentAsString())
                    .getByPath("data[0].id")).longValue();

            mockMvc.perform(post("/api/equipment")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "equipmentName", "测试篮球",
                                    "categoryId", categoryId,
                                    "brand", "Spalding",
                                    "unit", "个",
                                    "purchasePrice", 199.99,
                                    "safeStock", 10,
                                    "maxBorrowDays", 7,
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));

            MvcResult page = mockMvc.perform(get("/api/equipment/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", "测试篮球"))
                    .andExpect(jsonPath("$.data.records[0].equipmentCode")
                            .value(org.hamcrest.Matchers.matchesPattern("QX-\\d{4}-\\d{6}")))
                    .andExpect(jsonPath("$.data.records[0].categoryName").value("球类测试"))
                    .andReturn();
            Long equipmentId = ((Number) JSONUtil.parseObj(page.getResponse().getContentAsString())
                    .getByPath("data.records[0].id")).longValue();

            mockMvc.perform(delete("/api/equipment/" + equipmentId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(delete("/api/equipment/category/" + categoryId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name = '测试篮球'");
            jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code IN ('QX')");
        }
    }

    @Test
    void excelImport_shouldInsertEquipment() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(post("/api/equipment/category")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "parentId", 0,
                                    "categoryName", "瑜伽测试",
                                    "categoryCode", "YJ",
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));

            EquipmentImportDTO row = new EquipmentImportDTO();
            row.setEquipmentName("测试瑜伽垫");
            row.setCategoryCode("YJ");
            row.setUnit("张");
            row.setPurchasePrice(new BigDecimal("59.90"));
            row.setSafeStock(5);
            row.setMaxBorrowDays(10);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            EasyExcel.write(out, EquipmentImportDTO.class).sheet("器材").doWrite(List.of(row));
            MockMultipartFile file = new MockMultipartFile(
                    "file", "equipment.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());

            mockMvc.perform(multipart("/api/equipment/import")
                            .file(file)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.successCount").value(1))
                    .andExpect(jsonPath("$.data.failCount").value(0));

            mockMvc.perform(get("/api/equipment/page")
                            .header("Authorization", "Bearer " + token)
                            .param("equipmentName", "测试瑜伽垫"))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].equipmentCode")
                            .value(org.hamcrest.Matchers.matchesPattern("YJ-\\d{4}-\\d{6}")));
        } finally {
            jdbcTemplate.update("DELETE FROM equipment WHERE equipment_name = '测试瑜伽垫'");
            jdbcTemplate.update("DELETE FROM equipment_category WHERE category_code = 'YJ'");
        }
    }

    @Test
    void warehouseCrud_shouldWork() throws Exception {
        String token = loginAdmin();
        try {
            mockMvc.perform(post("/api/equipment/warehouse")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONUtil.toJsonStr(Map.of(
                                    "warehouseCode", "WH-TEST",
                                    "warehouseName", "测试仓库",
                                    "status", 1))))
                    .andExpect(jsonPath("$.code").value(0));

            MvcResult list = mockMvc.perform(get("/api/equipment/warehouse/list")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.data[0].warehouseCode").value("WH-TEST"))
                    .andReturn();
            Long id = ((Number) JSONUtil.parseObj(list.getResponse().getContentAsString())
                    .getByPath("data[0].id")).longValue();
            mockMvc.perform(delete("/api/equipment/warehouse/" + id)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            jdbcTemplate.update("DELETE FROM warehouse WHERE warehouse_code = 'WH-TEST'");
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
