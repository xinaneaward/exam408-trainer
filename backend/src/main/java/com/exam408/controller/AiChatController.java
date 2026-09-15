package com.exam408.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.ai.AiChatService;
import com.exam408.ai.ChatMessage;
import com.exam408.ai.DeepSeekClient;
import com.exam408.ai.DiagnosisService;
import com.exam408.ai.PromptTemplates;
import com.exam408.dto.ApiResponse;
import com.exam408.entity.AiChatMessage;
import com.exam408.entity.AiChatSession;
import com.exam408.entity.Question;
import com.exam408.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 对话接口（错题精讲 + 追问），SSE 流式输出。
 * 事件数据为JSON字符串，含 type 字段：meta/delta/done/error。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final ExecutorService STREAM_POOL = Executors.newCachedThreadPool();
    private static final long SSE_TIMEOUT_MS = 180_000L;

    @Resource
    private AiChatService aiChatService;
    @Resource
    private DeepSeekClient deepSeekClient;
    @Resource
    private com.exam408.ai.AiProperties aiProperties;
    @Resource
    private DiagnosisService diagnosisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 错题精讲：创建会话并流式生成讲解 */
    @PostMapping("/explain/stream")
    public SseEmitter explain(@RequestBody Map<String, Object> body, HttpSession session) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        User user = (User) session.getAttribute("user");
        if (user == null) return reject(emitter, "请先登录");

        Long questionId = toLong(body.get("questionId"));
        String userAnswer = body.get("userAnswer") == null ? "" : String.valueOf(body.get("userAnswer"));

        Question q = aiChatService.getQuestion(questionId);
        if (q == null) return reject(emitter, "题目不存在");
        String feature = "explain";
        int dailyLimit = aiProperties.getDailyLimit(feature);
        if (!aiChatService.checkDailyLimit(user.getId(), feature)) {
            return reject(emitter, "今日 AI 使用次数已达上限（每日" + dailyLimit + "次），请明天再来");
        }

        // 创建会话并写入首条用户消息
        AiChatSession cs = aiChatService.createSession(user.getId(), q.getId(), buildTitle(q));
        aiChatService.saveMessage(cs.getId(), "user",
                PromptTemplates.explainUserMessage(q, userAnswer), 0, 0);

        List<ChatMessage> llmMessages = PromptTemplates.buildMessages(
                PromptTemplates.systemPrompt(q),
                aiChatService.toLlmMessages(aiChatService.listMessages(cs.getId())));

        STREAM_POOL.execute(() -> streamTo(emitter, cs, user, "explain", llmMessages));
        return emitter;
    }

    /** 追问：基于已有会话继续对话 */
    @PostMapping("/chat/stream")
    public SseEmitter chat(@RequestBody Map<String, Object> body, HttpSession session) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        User user = (User) session.getAttribute("user");
        if (user == null) return reject(emitter, "请先登录");

        Long sessionId = toLong(body.get("sessionId"));
        String message = body.get("message") == null ? "" : String.valueOf(body.get("message")).trim();
        if (sessionId == null || message.isEmpty()) return reject(emitter, "参数不完整");

        AiChatSession cs = aiChatService.getOwnedSession(sessionId, user.getId());
        if (cs == null) return reject(emitter, "会话不存在");
        String feature = "chat";
        int dailyLimit = aiProperties.getDailyLimit(feature);
        if (!aiChatService.checkDailyLimit(user.getId(), feature)) {
            return reject(emitter, "今日 AI 使用次数已达上限（每日" + dailyLimit + "次），请明天再来");
        }
        // 题目可能为空（全局答疑会话），此时使用通用答疑 prompt
        Question q = aiChatService.getQuestion(cs.getQuestionId());

        aiChatService.saveMessage(cs.getId(), "user", message, 0, 0);
        List<ChatMessage> llmMessages = PromptTemplates.buildMessages(
                PromptTemplates.systemPromptOf(q),
                aiChatService.toLlmMessages(aiChatService.listMessages(cs.getId())));

        STREAM_POOL.execute(() -> streamTo(emitter, cs, user, "chat", llmMessages));
        return emitter;
    }

    /** 全局答疑助手：创建会话（题目上下文可选）并发送首条用户消息 */
    @PostMapping("/assistant/stream")
    public SseEmitter assistant(@RequestBody Map<String, Object> body, HttpSession session) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        User user = (User) session.getAttribute("user");
        if (user == null) return reject(emitter, "请先登录");

        String message = body.get("message") == null ? "" : String.valueOf(body.get("message")).trim();
        if (message.isEmpty()) return reject(emitter, "请输入问题");
        String feature = "assistant";
        int limit = aiProperties.getDailyLimit(feature);
        if (!aiChatService.checkDailyLimit(user.getId(), feature)) {
            return reject(emitter, "今日 AI 使用次数已达上限（每日" + limit + "次），请明天再来");
        }

        // 题目上下文可选：路由感知时前端会带上 questionId
        Long questionId = toLong(body.get("questionId"));
        Question q = aiChatService.getQuestion(questionId);
        String title = buildAssistantTitle(message, q);
        AiChatSession cs = aiChatService.createSession(user.getId(), questionId, title);
        aiChatService.saveMessage(cs.getId(), "user", message, 0, 0);

        List<ChatMessage> llmMessages = PromptTemplates.buildMessages(
                PromptTemplates.systemPromptOf(q),
                aiChatService.toLlmMessages(aiChatService.listMessages(cs.getId())));

        STREAM_POOL.execute(() -> streamTo(emitter, cs, user, "assistant", llmMessages));
        return emitter;
    }

    /** 会话历史回显 */
    @GetMapping("/sessions/{id}/messages")
    public ApiResponse<List<Map<String, Object>>> messages(@PathVariable("id") Long sessionId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        AiChatSession cs = aiChatService.getOwnedSession(sessionId, user.getId());
        if (cs == null) return ApiResponse.error(404, "会话不存在");

        List<Map<String, Object>> result = new ArrayList<>();
        for (AiChatMessage m : aiChatService.listMessages(sessionId)) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("role", m.getRole());
            map.put("content", m.getContent());
            map.put("createdAt", m.getCreatedAt());
            result.add(map);
        }
        return ApiResponse.success(result);
    }

    /** 今日 AI 用量（按功能），供前端展示剩余次数 */
    @GetMapping("/usage")
    public ApiResponse<List<Map<String, Object>>> usage(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        String[] features = {"chat", "explain", "assistant", "diagnosis", "variant"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (String f : features) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("feature", f);
            m.put("limit", aiProperties.getDailyLimit(f));
            m.put("used", aiChatService.getTodayUsageCount(user.getId(), f));
            m.put("remaining", aiChatService.getRemaining(user.getId(), f));
            result.add(m);
        }
        return ApiResponse.success(result);
    }

    /** AI 学情诊断：聚合数据 → AI 分析 → 返回结构化 JSON（解析失败降级纯文本） */
    @PostMapping("/diagnosis")
    public ApiResponse<Map<String, Object>> diagnosis(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        String feature = "diagnosis";
        int dailyLimit = aiProperties.getDailyLimit(feature);
        if (!aiChatService.checkDailyLimit(user.getId(), feature)) {
            return ApiResponse.error(429, "今日 AI 使用次数已达上限（每日" + dailyLimit + "次），请明天再来");
        }

        List<ChatMessage> messages = diagnosisService.buildDiagnosisMessages(user.getId());
        DeepSeekClient.SyncResult result = deepSeekClient.syncChat(messages);

        if (!result.ok()) {
            return ApiResponse.error(503, result.error);
        }

        // 记录用量
        aiChatService.logUsage(user.getId(), "diagnosis", result.promptTokens, result.completionTokens);

        // 尝试解析 JSON（去掉可能的 markdown 代码块包裹）
        String content = stripCodeFence(result.content);
        try {
            JsonNode root = objectMapper.readTree(content);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("structured", true);
            data.put("summary", root.path("summary").asText(""));
            data.put("weakPoints", objectMapper.convertValue(root.path("weakPoints"), List.class));
            data.put("studyPlan", objectMapper.convertValue(root.path("studyPlan"), List.class));
            data.put("recommendFilter", objectMapper.convertValue(root.path("recommendFilter"), Map.class));
            return ApiResponse.success(data);
        } catch (Exception e) {
            // JSON 解析失败，降级为纯文本展示
            log.warn("诊断结果JSON解析失败，降级纯文本: {}", e.getMessage());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("structured", false);
            data.put("rawText", result.content);
            return ApiResponse.success(data);
        }
    }

    /** 去掉 AI 返回中可能的 ```json ... ``` 代码块包裹 */
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

    // ---------- 内部方法 ----------

    /** 执行流式生成并把增量转发给前端（在STREAM_POOL线程执行） */
    private void streamTo(SseEmitter emitter, AiChatSession cs, User user,
                          String feature, List<ChatMessage> llmMessages) {
        StringBuilder sb = new StringBuilder();
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("sessionId", cs.getId());
            meta.put("title", cs.getTitle());
            sendEvent(emitter, "meta", meta);
        } catch (Exception ignore) {
        }

        deepSeekClient.streamChat(llmMessages, new DeepSeekClient.StreamHandler() {
            @Override
            public void onDelta(String text) {
                sb.append(text);
                try {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("text", text);
                    sendEvent(emitter, "delta", data);
                } catch (Exception ignore) {
                }
            }

            @Override
            public void onDone(long promptTokens, long completionTokens) {
                String content = sb.length() > 0 ? sb.toString() : "(AI 未返回内容)";
                aiChatService.saveMessage(cs.getId(), "assistant", content, promptTokens, completionTokens);
                aiChatService.logUsage(user.getId(), feature, promptTokens, completionTokens);
                try {
                    sendEvent(emitter, "done", Collections.singletonMap("sessionId", cs.getId()));
                    emitter.complete();
                } catch (Exception ignore) {
                }
            }

            @Override
            public void onError(String message) {
                log.warn("AI stream error, sessionId={}: {}", cs.getId(), message);
                // 已生成的部分内容也保存，避免丢失
                if (sb.length() > 0) {
                    aiChatService.saveMessage(cs.getId(), "assistant",
                            sb.append("\n\n[回答中断：").append(message).append("]").toString(), 0, 0);
                }
                try {
                    sendEvent(emitter, "error", Collections.singletonMap("message", message));
                    emitter.complete();
                } catch (Exception ignore) {
                }
            }
        });
    }

    /** 发送一个SSE事件（Map经Jackson序列化为JSON，保证中文UTF-8） */
    private void sendEvent(SseEmitter emitter, String type, Map<String, Object> data) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>(data);
        m.put("type", type);
        emitter.send(SseEmitter.event().data(m, MediaType.APPLICATION_JSON));
    }

    /** 校验失败时直接发error事件并结束 */
    private SseEmitter reject(SseEmitter emitter, String message) {
        STREAM_POOL.execute(() -> {
            try {
                sendEvent(emitter, "error", Collections.singletonMap("message", message));
                emitter.complete();
            } catch (Exception ignore) {
            }
        });
        return emitter;
    }

    private String buildTitle(Question q) {
        StringBuilder sb = new StringBuilder();
        if (q.getExamYear() != null) sb.append(q.getExamYear()).append("年");
        if (q.getSubject() != null) sb.append(q.getSubject());
        if (q.getQuestionNumber() != null) sb.append(" 第").append(q.getQuestionNumber()).append("题");
        return sb.length() > 0 ? sb.toString() : "AI讲解";
    }

    private String buildAssistantTitle(String message, Question q) {
        String prefix = q == null ? "答疑" : buildTitle(q);
        String tail = message.length() > 20 ? message.substring(0, 20) + "…" : message;
        return prefix + "：" + tail;
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
