package com.exam408.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek 客户端（OpenAI兼容接口）。
 * 支持流式调用（SSE）和同步调用（用于需要解析 JSON 的场景）。
 */
@Component
public class DeepSeekClient {

    /** 流式回调 */
    public interface StreamHandler {
        /** 收到增量文本 */
        void onDelta(String text);

        /** 正常结束，携带 token 用量 */
        void onDone(long promptTokens, long completionTokens);

        /** 出错（可能是网络、鉴权、限流等） */
        void onError(String message);
    }

    /** 同步调用结果 */
    public static class SyncResult {
        public final String content;
        public final long promptTokens;
        public final long completionTokens;
        public final String error;

        SyncResult(String content, long pt, long ct, String err) {
            this.content = content;
            this.promptTokens = pt;
            this.completionTokens = ct;
            this.error = err;
        }
        public boolean ok() { return error == null; }
    }

    @Resource
    private AiProperties aiProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(aiProperties.getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发起流式对话。回调在 OkHttp 工作线程执行，调用方需自行保证线程安全。
     */
    public void streamChat(List<ChatMessage> messages, StreamHandler handler) {
        if (!aiProperties.isConfigured()) {
            handler.onError("AI 服务未配置：请先设置环境变量 DEEPSEEK_API_KEY");
            return;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", aiProperties.getModel());
            body.put("stream", true);
            body.put("temperature", aiProperties.getTemperature());
            body.put("max_tokens", aiProperties.getMaxTokens());
            // 请求最后一个chunk携带token用量
            body.putObject("stream_options").put("include_usage", true);
            ArrayNode arr = body.putArray("messages");
            for (ChatMessage m : messages) {
                ObjectNode n = arr.addObject();
                n.put("role", m.getRole());
                n.put("content", m.getContent());
            }

            Request request = new Request.Builder()
                    .url(aiProperties.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .post(RequestBody.create(
                            objectMapper.writeValueAsBytes(body),
                            MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handler.onError("AI 请求失败: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody rb = response.body()) {
                        if (!response.isSuccessful()) {
                            String msg = rb != null ? rb.string() : response.message();
                            handler.onError("AI 服务返回错误 " + response.code() + ": " + abbreviate(msg, 300));
                            return;
                        }
                        long promptTokens = 0;
                        long completionTokens = 0;
                        BufferedSource source = rb.source();
                        while (true) {
                            String line = source.readUtf8Line();
                            if (line == null) break;
                            if (!line.startsWith("data:")) continue;
                            String payload = line.substring(5).trim();
                            if ("[DONE]".equals(payload)) break;
                            try {
                                JsonNode node = objectMapper.readTree(payload);
                                JsonNode usage = node.get("usage");
                                if (usage != null && usage.isObject()) {
                                    promptTokens = usage.path("prompt_tokens").asLong(0);
                                    completionTokens = usage.path("completion_tokens").asLong(0);
                                }
                                JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                                if (delta.isTextual() && !delta.asText().isEmpty()) {
                                    handler.onDelta(delta.asText());
                                }
                            } catch (Exception ignore) {
                                // 单个chunk解析失败不影响整体流
                            }
                        }
                        handler.onDone(promptTokens, completionTokens);
                    } catch (Exception e) {
                        handler.onError("AI 响应读取失败: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            handler.onError("AI 请求构建失败: " + e.getMessage());
        }
    }

    private String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * 同步调用（非流式）。阻塞当前线程直到拿到完整响应。
     * 用于学情诊断等需要解析结构化 JSON 的场景。
     */
    public SyncResult syncChat(List<ChatMessage> messages) {
        if (!aiProperties.isConfigured()) {
            return new SyncResult(null, 0, 0, "AI 服务未配置：请先设置环境变量 DEEPSEEK_API_KEY");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", aiProperties.getModel());
            body.put("stream", false);
            body.put("temperature", aiProperties.getTemperature());
            body.put("max_tokens", aiProperties.getMaxTokens());
            ArrayNode arr = body.putArray("messages");
            for (ChatMessage m : messages) {
                ObjectNode n = arr.addObject();
                n.put("role", m.getRole());
                n.put("content", m.getContent());
            }

            Request request = new Request.Builder()
                    .url(aiProperties.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .post(RequestBody.create(
                            objectMapper.writeValueAsBytes(body),
                            MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String msg = response.body() != null ? response.body().string() : response.message();
                    return new SyncResult(null, 0, 0, "AI 服务返回错误 " + response.code() + ": " + abbreviate(msg, 300));
                }
                JsonNode root = objectMapper.readTree(response.body().string());
                long pt = root.path("usage").path("prompt_tokens").asLong(0);
                long ct = root.path("usage").path("completion_tokens").asLong(0);
                String content = root.path("choices").path(0).path("message").path("content").asText("");
                return new SyncResult(content, pt, ct, null);
            }
        } catch (Exception e) {
            return new SyncResult(null, 0, 0, "AI 同步请求失败: " + e.getMessage());
        }
    }
}
