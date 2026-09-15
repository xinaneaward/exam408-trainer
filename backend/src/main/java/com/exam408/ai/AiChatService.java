package com.exam408.ai;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.entity.AiChatMessage;
import com.exam408.entity.AiChatSession;
import com.exam408.entity.AiUsageLog;
import com.exam408.entity.Question;
import com.exam408.mapper.AiChatMessageMapper;
import com.exam408.mapper.AiChatSessionMapper;
import com.exam408.mapper.AiUsageLogMapper;
import com.exam408.mapper.QuestionMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/** AI 对话服务：会话/消息持久化、限流、用量统计 */
@Service
public class AiChatService {

    /** 追问时携带的最大历史消息条数，控制 token 消耗 */
    private static final int MAX_HISTORY = 20;

    @Resource
    private AiChatSessionMapper sessionMapper;
    @Resource
    private AiChatMessageMapper messageMapper;
    @Resource
    private AiUsageLogMapper usageLogMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private AiProperties aiProperties;

    public Question getQuestion(Long questionId) {
        if (questionId == null) return null;
        return questionMapper.selectById(questionId);
    }

    /** 今日某功能已用次数（按 ai_usage_log 落库统计，重启不丢失） */
    public long getTodayUsageCount(Long userId, String feature) {
        return usageLogMapper.selectCount(lambdaQuery(AiUsageLog.class)
                .eq(AiUsageLog::getUserId, userId)
                .eq(feature != null, AiUsageLog::getFeature, feature)
                .ge(AiUsageLog::getCreatedAt, java.time.LocalDate.now().atStartOfDay()));
    }

    /** 单个功能每日限流检查：已用次数 >= 上限则拒绝 */
    public boolean checkDailyLimit(Long userId, String feature) {
        long limit = aiProperties.getDailyLimit(feature);
        return getTodayUsageCount(userId, feature) < limit;
    }

    /** 单个功能每日上限 */
    public int getDailyLimit(String feature) {
        return aiProperties.getDailyLimit(feature);
    }

    /** 单个功能剩余可用次数 */
    public long getRemaining(Long userId, String feature) {
        long limit = aiProperties.getDailyLimit(feature);
        return Math.max(0, limit - getTodayUsageCount(userId, feature));
    }

    public AiChatSession createSession(Long userId, Long questionId, String title) {
        AiChatSession s = new AiChatSession();
        s.setUserId(userId);
        s.setQuestionId(questionId);
        s.setTitle(title);
        sessionMapper.insert(s);
        return s;
    }

    /** 校验会话归属，非本人会话返回 null */
    public AiChatSession getOwnedSession(Long sessionId, Long userId) {
        if (sessionId == null) return null;
        AiChatSession s = sessionMapper.selectById(sessionId);
        if (s == null || !s.getUserId().equals(userId)) return null;
        return s;
    }

    public List<AiChatMessage> listMessages(Long sessionId) {
        LambdaQueryWrapper<AiChatMessage> w = new LambdaQueryWrapper<>();
        w.eq(AiChatMessage::getSessionId, sessionId).orderByAsc(AiChatMessage::getId);
        List<AiChatMessage> list = messageMapper.selectList(w);
        // 超长历史只保留最近MAX_HISTORY条（保证首轮system+题目上下文始终在开头）
        if (list.size() > MAX_HISTORY) {
            list = list.subList(list.size() - MAX_HISTORY, list.size());
        }
        return list;
    }

    public void saveMessage(Long sessionId, String role, String content, long promptTokens, long completionTokens) {
        AiChatMessage m = new AiChatMessage();
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        m.setPromptTokens((int) promptTokens);
        m.setCompletionTokens((int) completionTokens);
        messageMapper.insert(m);
    }

    /** 把DB历史转为LLM消息（只取 user/assistant 角色） */
    public List<ChatMessage> toLlmMessages(List<AiChatMessage> history) {
        java.util.List<ChatMessage> result = new java.util.ArrayList<>();
        for (AiChatMessage m : history) {
            if ("user".equals(m.getRole()) || "assistant".equals(m.getRole())) {
                result.add(new ChatMessage(m.getRole(), m.getContent()));
            }
        }
        return result;
    }

    public void logUsage(Long userId, String feature, long promptTokens, long completionTokens) {
        AiUsageLog log = new AiUsageLog();
        log.setUserId(userId);
        log.setFeature(feature);
        log.setPromptTokens((int) promptTokens);
        log.setCompletionTokens((int) completionTokens);
        usageLogMapper.insert(log);
    }
}
