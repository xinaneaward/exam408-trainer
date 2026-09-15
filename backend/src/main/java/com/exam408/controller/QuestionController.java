package com.exam408.controller;

import com.exam408.dto.ApiResponse;
import com.exam408.entity.Question;
import com.exam408.service.QuestionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/question")
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @GetMapping("/years")
    public ApiResponse<List<Integer>> getYears() {
        return ApiResponse.success(questionService.getYears());
    }

    @GetMapping("/subjects")
    public ApiResponse<List<String>> getSubjects() {
        return ApiResponse.success(questionService.getSubjects());
    }

    @GetMapping("/list")
    public ApiResponse<List<Question>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String knowledgeTag) {
        if (knowledgeTag != null && !knowledgeTag.isEmpty()) {
            if (type != null && !type.isEmpty()) {
                return ApiResponse.success(questionService.listByConditions(subject, knowledgeTag, type, null, null, null));
            }
            if (subject != null && !subject.isEmpty()) {
                return ApiResponse.success(questionService.listBySubjectAndKnowledgeTag(subject, knowledgeTag));
            }
            return ApiResponse.success(questionService.listByKnowledgeTag(knowledgeTag));
        }
        if (year != null && subject != null) {
            return ApiResponse.success(questionService.listByYearAndSubject(year, subject));
        }
        if (year != null) {
            return ApiResponse.success(questionService.listByYear(year));
        }
        if (subject != null && type != null) {
            return ApiResponse.success(questionService.listBySubjectAndType(subject, type));
        }
        if (subject != null) {
            return ApiResponse.success(questionService.listBySubject(subject));
        }
        return ApiResponse.success(questionService.listRandom(40));
    }

    @GetMapping("/filter")
    public ApiResponse<List<Question>> filter(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String knowledgeTag,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer yearStart,
            @RequestParam(required = false) Integer yearEnd,
            @RequestParam(defaultValue = "40") Integer limit) {
        limit = clampLimit(limit);
        return ApiResponse.success(questionService.listByConditions(subject, knowledgeTag, type, yearStart, yearEnd, limit));
    }

    @GetMapping("/random")
    public ApiResponse<List<Question>> random(@RequestParam(defaultValue = "40") Integer limit) {
        limit = clampLimit(limit);
        return ApiResponse.success(questionService.listRandom(limit));
    }

    /** 题目数量上限保护，防止超大数据量查询拖垮数据库 */
    private static int clampLimit(Integer limit) {
        if (limit == null) {
            return 40;
        }
        return Math.max(1, Math.min(limit, 200));
    }

    @GetMapping("/{id}")
    public ApiResponse<Question> getById(@PathVariable Long id) {
        Question q = questionService.getById(id);
        return q != null ? ApiResponse.success(q) : ApiResponse.error("题目不存在");
    }

    @GetMapping("/knowledge-tags")
    public ApiResponse<List<String>> getKnowledgeTags(@RequestParam(required = false) String subject) {
        return ApiResponse.success(questionService.getKnowledgeTags(subject));
    }

    @GetMapping("/knowledge-tree")
    public ApiResponse<JsonNode> getKnowledgeTree() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource("static/knowledge_tree.json");
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                return ApiResponse.success(mapper.readTree(in));
            }
        }
        // 兼容旧版工程布局
        java.io.File legacy = new java.io.File("c:/projects/408真题训练系统/src/main/resources/static/knowledge_tree.json");
        if (legacy.exists()) {
            return ApiResponse.success(mapper.readTree(legacy));
        }
        return ApiResponse.success(mapper.createObjectNode());
    }
}
