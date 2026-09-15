package com.exam408.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.entity.*;
import com.exam408.mapper.ExamRecordMapper;
import com.exam408.mapper.QuestionMapper;
import com.exam408.mapper.UserAnswerMapper;
import com.exam408.mapper.WrongQuestionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学情诊断服务：聚合用户答题/错题数据，构造诊断 Prompt 供 AI 分析。
 */
@Service
public class DiagnosisService {

    @Resource private UserAnswerMapper userAnswerMapper;
    @Resource private WrongQuestionMapper wrongQuestionMapper;
    @Resource private QuestionMapper questionMapper;
    @Resource private ExamRecordMapper examRecordMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] SUBJECTS = {"数据结构", "计算机组成原理", "操作系统", "计算机网络"};

    /**
     * 构造学情诊断的 Prompt（含聚合数据）。
     * 返回 System + User 两条消息。
     */
    public List<ChatMessage> buildDiagnosisMessages(Long userId) {
        Map<String, Object> data = aggregateUserData(userId);
        String userDataJson = toJson(data);
        return Arrays.asList(
                new ChatMessage("system", buildSystemPrompt()),
                new ChatMessage("user", buildUserPrompt(userDataJson))
        );
    }

    /** 聚合用户学情数据 */
    private Map<String, Object> aggregateUserData(Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();

        // 各科目正确率
        List<UserAnswer> answers = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId));
        Map<String, Object> accuracyBySubject = new LinkedHashMap<>();
        for (String subj : SUBJECTS) {
            List<UserAnswer> subjAnswers = answers.stream()
                    .filter(a -> {
                        Question q = questionMapper.selectById(a.getQuestionId());
                        return q != null && subj.equals(q.getSubject());
                    })
                    .collect(Collectors.toList());
            long total = subjAnswers.stream().map(UserAnswer::getQuestionId).distinct().count();
            long correct = subjAnswers.stream()
                    .filter(a -> a.getIsCorrect() != null && a.getIsCorrect())
                    .map(UserAnswer::getQuestionId).distinct().count();
            int acc = total > 0 ? (int) (correct * 100 / total) : 0;
            accuracyBySubject.put(subj, acc);
        }
        data.put("accuracyBySubject", accuracyBySubject);

        // 错题按知识点分布
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
        Map<String, List<Map<String, Object>>> wrongBySubject = new LinkedHashMap<>();
        Map<String, Integer> tagCount = new LinkedHashMap<>();
        for (WrongQuestion wq : wrongs) {
            Question q = questionMapper.selectById(wq.getQuestionId());
            if (q == null) continue;
            String subj = q.getSubject();
            wrongBySubject.computeIfAbsent(subj, k -> new ArrayList<>());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tag", q.getKnowledgeTag());
            item.put("wrongCount", wq.getWrongCount());
            item.put("reviewed", wq.getIsReviewed());
            wrongBySubject.get(subj).add(item);
            String tag = q.getKnowledgeTag();
            if (tag != null) tagCount.merge(tag, 1, Integer::sum);
        }
        data.put("wrongDistribution", wrongBySubject);

        // 错题最多的知识点 Top5
        List<Map<String, Object>> topWeakTags = tagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("tag", e.getKey());
                    m.put("wrongCount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        data.put("topWeakTags", topWeakTags);

        // 近期做题趋势（最近14天）
        List<ExamRecord> records = examRecordMapper.selectByUserId(userId);
        Map<String, int[]> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String key = d.format(DT_FMT);
            int total = 0, correct = 0;
            for (ExamRecord r : records) {
                if (r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(d)) {
                    total += r.getTotalQuestions() != null ? r.getTotalQuestions() : 0;
                    correct += r.getCorrectCount() != null ? r.getCorrectCount() : 0;
                }
            }
            if (total > 0) trend.put(key, new int[]{total, correct});
        }
        data.put("recentTrend", trend);

        data.put("totalAnswered", answers.size());
        data.put("totalWrong", wrongs.size());

        return data;
    }

    private String buildSystemPrompt() {
        return "你是一位408计算机考研学习规划师。基于学生的学情数据，分析薄弱点并给出刷题推荐。"
                + "请返回纯JSON（不要代码块、不要markdown格式、不要额外解释），格式如下：\n"
                + "{\n"
                + "  \"summary\": \"一句话总结学情（30字内）\",\n"
                + "  \"weakPoints\": [\n"
                + "    {\"subject\":\"操作系统\", \"tag\":\"页置换算法\", \"accuracy\":35, \"reason\":\"对LRU和FIFO的区别不清晰\"}\n"
                + "  ],\n"
                + "  \"studyPlan\": [\n"
                + "    {\"day\":1, \"task\":\"复习虚拟内存知识点，做5道相关真题\"}\n"
                + "  ],\n"
                + "  \"recommendFilter\": {\n"
                + "    \"subject\":\"操作系统\",\n"
                + "    \"tag\":\"虚拟内存\",\n"
                + "    \"reason\":\"重点突破最薄弱的知识点\"\n"
                + "  }\n"
                + "}\n"
                + "要求：weakPoints最多3个，studyPlan最多7天，reason简明扼要。";
    }

    private String buildUserPrompt(String userDataJson) {
        return "以下是我的学情数据（JSON），请分析并返回推荐：\n" + userDataJson;
    }

    /** 安全序列化（失败时返回原始 map 的 toString） */
    private String toJson(Object o) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }
}
