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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 中危项 5：菜单父级不能指向自身或其子孙，避免 routers() 递归栈溢出。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MenuCycleTest {

    private static final String UUID = "menu-cycle-uuid";
    private static final String CODE = "1234";
    private static final String A = "环校验菜单A";
    private static final String B = "环校验菜单B";
    private static final String C = "环校验菜单C";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updateMenu_toOwnDescendant_shouldBeRejected() throws Exception {
        String token = loginAdmin();
        try {
            long a = createMenu(token, A, 0);
            long b = createMenu(token, B, a);
            long c = createMenu(token, C, b);

            // A 挂到子级 B：成环 A->B->A
            mockMvc.perform(put("/api/system/menu")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(menuBody(a, b, A)))
                    .andExpect(jsonPath("$.code").value(1001));
            // A 挂到孙级 C：成环 A->B->C->A
            mockMvc.perform(put("/api/system/menu")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(menuBody(a, c, A)))
                    .andExpect(jsonPath("$.code").value(1001));
            // 自身为父级（原有防护）
            mockMvc.perform(put("/api/system/menu")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(menuBody(a, a, A)))
                    .andExpect(jsonPath("$.code").value(1001));

            // 合法移动：B 挂到根
            mockMvc.perform(put("/api/system/menu")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(menuBody(b, 0, B)))
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            cleanup();
        }
    }

    private String menuBody(long id, long parentId, String menuName) {
        return JSONUtil.toJsonStr(Map.of(
                "id", id,
                "parentId", parentId,
                "menuName", menuName,
                "orderNum", 99,
                "menuType", "M",
                "path", "/cycle-" + id,
                "visible", 1,
                "status", 1));
    }

    private long createMenu(String token, String menuName, long parentId) throws Exception {
        mockMvc.perform(post("/api/system/menu")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(Map.of(
                                "parentId", parentId,
                                "menuName", menuName,
                                "orderNum", 99,
                                "menuType", "M",
                                "path", "/cycle-" + menuName,
                                "visible", 1,
                                "status", 1))))
                .andExpect(jsonPath("$.code").value(0));
        return findMenuId(token, menuName);
    }

    private long findMenuId(String token, String menuName) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/system/menu/tree")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        List<Map<String, Object>> tree = JSONUtil.parseObj(result.getResponse().getContentAsString())
                .getJSONArray("data").stream()
                .map(row -> (Map<String, Object>) row)
                .toList();
        Set<Long> visited = new HashSet<>();
        return findInTree(tree, menuName, visited)
                .orElseThrow(() -> new AssertionError("菜单未创建: " + menuName));
    }

    private java.util.Optional<Long> findInTree(List<Map<String, Object>> nodes,
                                                String menuName, Set<Long> visited) {
        for (Map<String, Object> node : nodes) {
            long id = ((Number) node.get("id")).longValue();
            if (!visited.add(id)) {
                continue;
            }
            if (menuName.equals(node.get("menuName"))) {
                return java.util.Optional.of(id);
            }
            Object children = node.get("children");
            if (children instanceof List<?> list && !list.isEmpty()) {
                java.util.Optional<Long> found = findInTree(
                        list.stream().map(c -> (Map<String, Object>) c).toList(), menuName, visited);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return java.util.Optional.empty();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM sys_menu WHERE menu_name IN (?, ?, ?)", A, B, C);
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
