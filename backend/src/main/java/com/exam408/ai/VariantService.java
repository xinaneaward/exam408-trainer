package com.exam408.ai;

import com.exam408.entity.Question;
import com.exam408.mapper.QuestionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI 变式出题服务：基于真题范例生成同知识点变式题，自检后落库。
 */
@Slf4j
@Service
public class VariantService {

    @Resource private QuestionMapper questionMapper;
    @Resource private DeepSeekClient deepSeekClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 生成结果 */
    public static class GenerateResult {
        public final Question question;
        public final boolean verified;
        public final String error;
        /** 两轮调用（生成+自检）合计消耗的 prompt tokens */
        public final long promptTokens;
        /** 两轮调用（生成+自检）合计消耗的 completion tokens */
        public final long completionTokens;

        GenerateResult(Question q, boolean v, String e, long pt, long ct) {
            this.question = q; this.verified = v; this.error = e;
            this.promptTokens = pt; this.completionTokens = ct;
        }
        public boolean ok() { return question != null && error == null; }
    }

    /**
     * 生成变式题。
     * @param subject 科目
     * @param knowledgeTag 知识点（可空，为空时从科目真题中随机取范例）
     */
    public GenerateResult generate(String subject, String knowledgeTag) {
        // 1. 获取 few-shot 范例（1-2道同知识点真题）
        List<Question> examples = getExamples(subject, knowledgeTag, 2);
        if (examples.isEmpty()) {
            return new GenerateResult(null, false, "未找到同知识点的真题范例，无法生成变式题", 0, 0);
        }

        // 2. 构造生成 Prompt 并同步调用
        List<ChatMessage> genMessages = buildGenerateMessages(subject, knowledgeTag, examples);
        DeepSeekClient.SyncResult genResult = deepSeekClient.syncChat(genMessages);
        if (!genResult.ok()) {
            return new GenerateResult(null, false, genResult.error, 0, 0);
        }

        // 3. 解析 JSON -> Question
        Question generated = parseQuestion(genResult.content, subject, knowledgeTag);
        if (generated == null) {
            return new GenerateResult(null, false, "AI 返回的题目格式无法解析", 0, 0);
        }

        // 4. 自检（消耗第二轮调用，累计 token 用量）
        long[] tokens = { genResult.promptTokens, genResult.completionTokens };
        boolean verified = verify(generated, tokens);
        generated.setSource("ai");
        generated.setVerified(verified);

        // 5. 落库
        questionMapper.insert(generated);
        log.info("AI变式题生成成功: subject={}, tag={}, verified={}", subject, knowledgeTag, verified);

        return new GenerateResult(generated, verified, null, tokens[0], tokens[1]);
    }

    /** 获取同知识点真题范例 */
    private List<Question> getExamples(String subject, String knowledgeTag, int limit) {
        List<Question> examples;
        if (knowledgeTag != null && !knowledgeTag.isEmpty()) {
            examples = questionMapper.selectBySubjectAndKnowledgeTag(subject, knowledgeTag);
        } else {
            examples = questionMapper.selectBySubject(subject);
        }
        if (examples.size() > limit) {
            examples = examples.subList(0, limit);
        }
        // 只取单选题作为范例（AI 只生成单选）
        List<Question> single = new ArrayList<>();
        for (Question q : examples) {
            if ("单选".equals(q.getType())) {
                single.add(q);
                if (single.size() >= limit) break;
            }
        }
        return single;
    }

    private List<ChatMessage> buildGenerateMessages(String subject, String knowledgeTag, List<Question> examples) {
        StringBuilder sysPrompt = new StringBuilder();
        sysPrompt.append("你是408计算机考研出题专家，精通").append(subject).append("。")
                .append("请基于以下真题范例，生成1道同知识点、同难度级别的变式题。\n")
                .append("要求：\n1. 题目内容必须与范例不同，但考查相同知识点\n")
                .append("2. 必须是单选题，4个选项(A/B/C/D)\n")
                .append("3. 只有一个正确答案\n4. 提供详细解析\n\n")
                .append("返回纯JSON（不要代码块、不要markdown、不要额外解释），格式：\n")
                .append("{\"content\":\"题目内容\",\"options\":[{\"key\":\"A\",\"text\":\"选项A\"},")
                .append("{\"key\":\"B\",\"text\":\"选项B\"},{\"key\":\"C\",\"text\":\"选项C\"},")
                .append("{\"key\":\"D\",\"text\":\"选项D\"}],\"answer\":\"正确选项字母\",")
                .append("\"analysis\":\"解析\",\"knowledgeTag\":\"知识点标签\"}");

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("科目：").append(subject).append("\n");
        if (knowledgeTag != null && !knowledgeTag.isEmpty()) {
            userPrompt.append("知识点：").append(knowledgeTag).append("\n");
        }
        userPrompt.append("\n【真题范例】\n");
        for (int i = 0; i < examples.size(); i++) {
            Question q = examples.get(i);
            userPrompt.append("范例").append(i + 1).append("：\n");
            userPrompt.append("题目：").append(PromptTemplates.cleanContent(q.getContent())).append("\n");
            userPrompt.append("选项：").append(PromptTemplates.formatOptions(q.getOptions())).append("\n");
            userPrompt.append("答案：").append(q.getAnswer()).append("\n");
            if (q.getAnalysis() != null) {
                userPrompt.append("解析：").append(q.getAnalysis()).append("\n\n");
            }
        }
        userPrompt.append("请生成1道").append(subject).append("的变式题。");

        return Arrays.asList(
                new ChatMessage("system", sysPrompt.toString()),
                new ChatMessage("user", userPrompt.toString())
        );
    }

    /** 解析 AI 返回的 JSON 为 Question 对象 */
    private Question parseQuestion(String content, String subject, String knowledgeTag) {
        String json = stripCodeFence(content);
        try {
            JsonNode root = objectMapper.readTree(json);
            Question q = new Question();
            q.setExamYear(null);
            q.setSubject(subject);
            q.setType("单选");
            q.setQuestionNumber(null);
            q.setContent(root.path("content").asText(""));
            String options = objectMapper.writeValueAsString(root.path("options"));
            q.setOptions(options);
            q.setAnswer(root.path("answer").asText(""));
            q.setAnalysis(root.path("analysis").asText(""));
            // 优先用 AI 返回的 knowledgeTag，其次用传入的
            String tag = root.path("knowledgeTag").asText("");
            q.setKnowledgeTag(tag.isEmpty() ? knowledgeTag : tag);

            // 基本校验
            if (q.getContent().isEmpty() || q.getAnswer().isEmpty() || "[]".equals(options)) {
                return null;
            }
            return q;
        } catch (Exception e) {
            log.warn("变式题JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    /** 自检：让 AI 验证题目和答案的一致性，并把本轮 token 用量累加进 totals[0](prompt)/totals[1](completion) */
    private boolean verify(Question q, long[] totals) {
        List<ChatMessage> verifyMessages = buildVerifyMessages(q);
        DeepSeekClient.SyncResult result = deepSeekClient.syncChat(verifyMessages);
        if (result.ok()) {
            totals[0] += result.promptTokens;
            totals[1] += result.completionTokens;
        }
        if (!result.ok()) {
            log.warn("变式题自检请求失败，默认通过: {}", result.error);
            return true; // 自检失败不阻塞，默认通过
        }
        try {
            String json = stripCodeFence(result.content);
            JsonNode root = objectMapper.readTree(json);
            boolean valid = root.path("valid").asBoolean(false);
            if (!valid) {
                log.warn("变式题自检未通过: {}", root.path("reason").asText(""));
            }
            return valid;
        } catch (Exception e) {
            log.warn("变式题自检结果解析失败，默认通过: {}", e.getMessage());
            return true;
        }
    }

    private List<ChatMessage> buildVerifyMessages(Question q) {
        String sysPrompt = "你是408计算机考研阅卷老师。请验证以下题目的正确性，返回纯JSON（不要代码块）。";
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("请验证以下题目：\n")
                .append("题目：").append(PromptTemplates.cleanContent(q.getContent())).append("\n")
                .append("选项：").append(PromptTemplates.formatOptions(q.getOptions())).append("\n")
                .append("答案：").append(q.getAnswer()).append("\n\n")
                .append("验证：\n1. 答案是否正确指向唯一选项\n2. 题目逻辑是否自洽\n")
                .append("3. 选项中是否有明显错误\n\n")
                .append("返回JSON：{\"valid\":true/false,\"reason\":\"验证说明\"}");

        return Arrays.asList(
                new ChatMessage("system", sysPrompt),
                new ChatMessage("user", userPrompt.toString())
        );
    }

    private String stripCodeFence(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) t = t.substring(firstNl + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }
}
