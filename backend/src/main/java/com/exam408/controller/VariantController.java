package com.exam408.controller;

import com.exam408.ai.AiChatService;
import com.exam408.ai.VariantService;
import com.exam408.dto.ApiResponse;
import com.exam408.entity.Question;
import com.exam408.entity.User;
import com.exam408.mapper.QuestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 变式出题接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/variant")
public class VariantController {

    @Resource private VariantService variantService;
    @Resource private AiChatService aiChatService;
    @Resource private QuestionMapper questionMapper;

    /** 生成变式题：{subject, knowledgeTag?} 或 {questionId} */
    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        String feature = "variant";
        int dailyLimit = aiChatService.getDailyLimit(feature);
        if (!aiChatService.checkDailyLimit(user.getId(), feature)) {
            return ApiResponse.error(429, "今日 AI 使用次数已达上限（每日" + dailyLimit + "次），请明天再来");
        }

        String subject = body.get("subject") == null ? "" : String.valueOf(body.get("subject")).trim();
        String knowledgeTag = body.get("knowledgeTag") == null ? "" : String.valueOf(body.get("knowledgeTag")).trim();

        // 如果传了 questionId，从题目获取科目和知识点
        Long questionId = toLong(body.get("questionId"));
        if (questionId != null && subject.isEmpty()) {
            Question q = questionMapper.selectById(questionId);
            if (q == null) return ApiResponse.error(404, "题目不存在");
            subject = q.getSubject();
            if (knowledgeTag.isEmpty() && q.getKnowledgeTag() != null) {
                knowledgeTag = q.getKnowledgeTag();
            }
        }
        if (subject.isEmpty()) return ApiResponse.error(400, "请指定科目");

        VariantService.GenerateResult result = variantService.generate(subject, knowledgeTag);
        if (!result.ok()) {
            return ApiResponse.error(500, result.error);
        }

        // 记录用量：两轮调用（生成+自检）合计，计入 variant 功能每日限额
        aiChatService.logUsage(user.getId(), "variant", result.promptTokens, result.completionTokens);

        Map<String, Object> data = toVo(result.question);
        data.put("verified", result.verified);
        if (!result.verified) {
            data.put("warning", "该题自检未通过，请谨慎参考");
        }
        return ApiResponse.success(data);
    }

    /** 查询 AI 生成题列表 */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String subject,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");

        List<Question> questions = (subject != null && !subject.isEmpty())
                ? questionMapper.selectAiQuestionsBySubject(subject)
                : questionMapper.selectAiQuestions();

        List<Map<String, Object>> result = questions.stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        return ApiResponse.success(result);
    }

    private Map<String, Object> toVo(Question q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("subject", q.getSubject());
        m.put("type", q.getType());
        m.put("content", q.getContent());
        m.put("options", q.getOptions());
        m.put("answer", q.getAnswer());
        m.put("analysis", q.getAnalysis());
        m.put("knowledgeTag", q.getKnowledgeTag());
        m.put("source", q.getSource());
        m.put("verified", q.getVerified());
        return m;
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        try {
            return Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
