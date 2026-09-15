package com.exam408;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 只读冒烟测试：验证应用在 MySQL 上能正常提供核心接口。
 * 运行前提：本机 MySQL 已启动并已导入迁移数据（question=846）。
 * 不写库、不调用 DataInitializer（题库非空自动跳过）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiSmokeTest {

    @Resource
    private TestRestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode getJson(String path) {
        ResponseEntity<String> resp = rest.getForEntity(path, String.class);
        assertTrue(resp.getStatusCode().is2xxSuccessful(), "HTTP状态码异常: " + path);
        assertNotNull(resp.getBody());
        try {
            return mapper.readTree(resp.getBody());
        } catch (Exception e) {
            throw new AssertionError("响应非JSON: " + path, e);
        }
    }

    @Test
    void yearsAvailable() {
        JsonNode root = getJson("/api/question/years");
        assertEquals(200, root.path("code").asInt());
        assertTrue(root.path("data").isArray());
        assertFalse(root.path("data").isEmpty(), "年份列表不应为空");
    }

    @Test
    void subjectsAvailable() {
        JsonNode root = getJson("/api/question/subjects");
        assertEquals(200, root.path("code").asInt());
        assertEquals(4, root.path("data").size(), "应有四个科目");
    }

    @Test
    void questionDataLoaded() {
        JsonNode root = getJson("/api/question/random?limit=5");
        assertEquals(200, root.path("code").asInt());
        int n = root.path("data").size();
        assertTrue(n > 0 && n <= 5, "题目数量异常: " + n);
        assertFalse(root.path("data").get(0).path("content").asText().isEmpty(), "题目内容不应为空");
    }

    @Test
    void filterLimitIsClamped() {
        JsonNode root = getJson("/api/question/filter?limit=99999");
        assertEquals(200, root.path("code").asInt());
        int n = root.path("data").size();
        assertTrue(n > 0 && n <= 200, "limit 未生效，返回数量: " + n);
    }

    @Test
    void knowledgeTreeLoaded() {
        JsonNode root = getJson("/api/question/knowledge-tree");
        assertEquals(200, root.path("code").asInt());
        assertFalse(root.path("data").isEmpty(), "知识点树不应为空");
    }
}