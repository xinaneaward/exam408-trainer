package com.exam408.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** AI(DeepSeek) 配置项，对应 application.yml 的 ai 前缀 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String baseUrl = "https://api.deepseek.com";

    /** 从环境变量 DEEPSEEK_API_KEY 读取 */
    private String apiKey;

    private String model = "deepseek-chat";

    private double temperature = 0.7;

    private int maxTokens = 2048;

    private int timeoutSeconds = 120;

    /** 各功能每用户每日调用上限，对应 ai.limits 配置，缺失时使用默认值 */
    private Map<String, Integer> limits = new HashMap<>();

    private static final int DEFAULT_DAILY_LIMIT = 30;

    /** 单个功能每日上限（explain/chat/assistant/diagnosis/variant） */
    public int getDailyLimit(String feature) {
        Integer v = limits.get(feature);
        return v == null ? DEFAULT_DAILY_LIMIT : v;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
