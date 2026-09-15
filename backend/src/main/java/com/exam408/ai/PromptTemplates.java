package com.exam408.ai;

import com.exam408.entity.Question;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/** 各功能的 Prompt 模板 */
public final class PromptTemplates {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private PromptTemplates() {
    }

    /** 系统提示词：注入题目上下文 */
    public static String systemPrompt(Question q) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位408计算机考研辅导老师，精通数据结构、计算机组成原理、操作系统、计算机网络。")
          .append("请基于下面这道真题的信息来回答学生的问题。\n")
          .append("只回答计算机考研相关问题；如果学生的问题与本题及408无关，请礼貌引导回学习话题。\n\n")
          .append("【题目信息】\n")
          .append("年份：").append(q.getExamYear()).append("年  ")
          .append("科目：").append(q.getSubject()).append("  ")
          .append("题型：").append(q.getType());
        if (q.getQuestionNumber() != null) {
            sb.append("  第").append(q.getQuestionNumber()).append("题");
        }
        if (q.getKnowledgeTag() != null && !q.getKnowledgeTag().isEmpty()) {
            sb.append("  知识点：").append(q.getKnowledgeTag());
        }
        sb.append("\n题目内容：").append(cleanContent(q.getContent()));
        String options = formatOptions(q.getOptions());
        if (!options.isEmpty()) {
            sb.append("\n选项：\n").append(options);
        }
        sb.append("\n正确答案：").append(q.getAnswer() == null ? "无" : q.getAnswer());
        if (q.getAnalysis() != null && !q.getAnalysis().isEmpty()) {
            sb.append("\n官方解析：").append(q.getAnalysis());
        }
        return sb.toString();
    }

    /** 错题精讲的首条用户消息 */
    public static String explainUserMessage(Question q, String userAnswer) {
        StringBuilder sb = new StringBuilder();
        if (userAnswer != null && !userAnswer.trim().isEmpty()) {
            sb.append("这道题我答错了，我的答案是：").append(userAnswer.trim()).append("。\n");
            sb.append("请按以下结构讲解：\n")
              .append("1. 指出我的答案错在哪里，分析常见的错误思路；\n")
              .append("2. 分步骤讲解正确的解题思路；\n")
              .append("3. 总结这道题涉及的核心知识点和易错点。");
        } else {
            sb.append("请给我详细讲解这道题：\n")
              .append("1. 分析题目考点；\n")
              .append("2. 分步骤讲解解题思路；\n")
              .append("3. 总结涉及的核心知识点和易错点。");
        }
        return sb.toString();
    }

    /** 把选项JSON格式化为 A. xxx 的多行文本 */
    public static String formatOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return "";
        }
        try {
            JsonNode arr = objectMapper.readTree(optionsJson);
            if (!arr.isArray() || arr.size() == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (JsonNode n : arr) {
                String key = n.path("key").asText("");
                String text = n.path("text").asText("");
                if (!key.isEmpty()) {
                    sb.append(key).append(". ").append(text).append("\n");
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            // 选项格式异常时原样返回
            return optionsJson;
        }
    }

    /** 去掉题目内容里的内嵌图片注释等无效信息，控制token */
    public static String cleanContent(String content) {
        if (content == null) return "";
        return content.replaceAll("<!--\\s*page_img:.*?-->", "")
                      .replaceAll("<[^>]+>", " ")
                      .replaceAll("\\s{2,}", " ")
                      .trim();
    }

    /** 构造带历史上下文的完整消息列表（system + 最近历史） */
    public static List<ChatMessage> buildMessages(String systemPrompt, List<ChatMessage> history) {
        List<ChatMessage> list = new ArrayList<>();
        list.add(new ChatMessage("system", systemPrompt));
        list.addAll(history);
        return list;
    }

    /** 全局答疑的通用系统提示词（无具体题目上下文） */
    public static String assistantSystemPrompt() {
        return "你是一位408计算机考研辅导助手，精通数据结构、计算机组成原理、操作系统、计算机网络四门科目。"
                + "请耐心解答学生提出的计算机考研相关问题，讲解时尽量分步骤、举例说明、联系真题考点。"
                + "如果问题与408计算机考研无关，请礼貌引导回学习话题。";
    }

    /** 构造系统提示词：有题目用题目上下文，无题目用通用答疑 */
    public static String systemPromptOf(Question q) {
        return q == null ? assistantSystemPrompt() : systemPrompt(q);
    }
}
