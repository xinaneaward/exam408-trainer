package com.exam408.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.dto.DashboardVO;
import com.exam408.dto.StatsVO;
import com.exam408.entity.*;
import com.exam408.mapper.*;
import com.exam408.service.ExamService;
import com.exam408.service.StatsService;
import com.exam408.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatsServiceImpl implements StatsService {

    @Resource private ExamRecordMapper examRecordMapper;
    @Resource private UserAnswerMapper userAnswerMapper;
    @Resource private WrongQuestionMapper wrongQuestionMapper;
    @Resource private QuestionMapper questionMapper;
    @Resource private ExamService examService;
    @Resource private UserService userService;

    private static final String[] SUBJECTS = {"数据结构", "计算机组成原理", "操作系统", "计算机网络"};
    // 各科题库总数（与 question 表实际一致）合计846
    private static final int[] QUESTION_COUNTS = {234, 234, 216, 162};
    // 惯例错题原因归类
    private static final String[] REASONS = {"概念不清", "粗心", "计算错", "审题不清", "其他"};

    @Value("${exam.target-date:2026-12-20}")
    private String targetDateStr;
    @Value("${exam.phase-1-end:2026-07-01}")
    private String phase1EndStr;
    @Value("${exam.phase-2-end:2026-10-15}")
    private String phase2EndStr;

    @Override
    public StatsVO getUserStats(Long userId) {
        StatsVO vo = new StatsVO();
        int total = userAnswerMapper.countTotalByUserId(userId);
        int correct = userAnswerMapper.countCorrectByUserId(userId);
        int wrong = wrongQuestionMapper.countByUserId(userId);
        int examCount = examRecordMapper.countByUserId(userId);
        vo.setTotalQuestions(total);
        vo.setTotalCorrect(correct);
        vo.setTotalAccuracy(total > 0
                ? BigDecimal.valueOf(correct * 100.0 / total).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setWrongCount(wrong);
        vo.setExamCount(examCount);
        Map<String, Double> accMap = examService.getSubjectAccuracy(userId);
        vo.setDsAccuracy(bd(accMap.getOrDefault("数据结构", 0.0)));
        vo.setCoAccuracy(bd(accMap.getOrDefault("计算机组成原理", 0.0)));
        vo.setOsAccuracy(bd(accMap.getOrDefault("操作系统", 0.0)));
        vo.setCnAccuracy(bd(accMap.getOrDefault("计算机网络", 0.0)));
        return vo;
    }

    @Override
    public DashboardVO getDashboard(Long userId) {
        DashboardVO vo = new DashboardVO();

        // 用户信息
        User user = userService.getById(userId);
        vo.setNickname(user != null ? user.getNickname() : "同学");

        // 总题数
        Long totalQ = questionMapper.selectCount(new LambdaQueryWrapper<>());
        vo.setTotalQuestions(totalQ != null ? totalQ.intValue() : 846);

        // 已做题数（去重）
        int answered = userAnswerMapper.countTotalByUserId(userId);
        vo.setAnsweredCount(answered);
        vo.setCompletionRate(vo.getTotalQuestions() > 0
                ? bd(answered * 100.0 / vo.getTotalQuestions()) : BigDecimal.ZERO);

        // 总正确率
        int correct = userAnswerMapper.countCorrectByUserId(userId);
        vo.setTotalAccuracy(answered > 0 ? bd(correct * 100.0 / answered) : BigDecimal.ZERO);

        // 做题天数
        List<ExamRecord> records = examRecordMapper.selectByUserId(userId);
        Set<String> days = new HashSet<>();
        for (ExamRecord r : records) {
            if (r.getCreatedAt() != null) {
                days.add(r.getCreatedAt().toLocalDate().toString());
            }
        }
        vo.setStudyDays(days.size());

        // 连续天数（简化：统计今天往回有多少连续天数）
        int consecutive = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 90; i++) {
            if (days.contains(today.minusDays(i).toString())) {
                consecutive++;
            } else if (i > 0) {
                break;
            }
        }
        vo.setConsecutiveDays(consecutive);

        // 最近90天做题热力图数据
        Map<String, Integer> activity = new LinkedHashMap<>();
        for (int i = 89; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            // 统计该天的做题数
            long count = records.stream()
                    .filter(r -> r.getCreatedAt() != null &&
                            r.getCreatedAt().toLocalDate().toString().equals(date))
                    .count();
            activity.put(date, (int) count);
        }
        vo.setRecentActivity(activity);

        // 各科掌握情况
        List<DashboardVO.SubjectMastery> subjects = new ArrayList<>();
        for (int i = 0; i < SUBJECTS.length; i++) {
            String subj = SUBJECTS[i];
            int total = QUESTION_COUNTS[i];
            DashboardVO.SubjectMastery sm = new DashboardVO.SubjectMastery();
            sm.setName(subj);
            sm.setTotal(total);

            // 该科已做题数
            int subjAnswered = countSubjectAnswers(userId, subj);
            sm.setAnswered(subjAnswered);
            sm.setProgress(total > 0 ? bd(subjAnswered * 100.0 / total) : BigDecimal.ZERO);

            // 掌握/不熟/不会统计
            int mastered = countWrongByMastery(userId, subj, "mastered");
            int unfamiliar = countWrongByMastery(userId, subj, "unfamiliar");
            int unknown = countWrongByMastery(userId, subj, "unknown");
            int undone = total - subjAnswered;
            sm.setMastered(subjAnswered - unfamiliar - unknown);
            sm.setUnfamiliar(unfamiliar);
            sm.setUnknown(unknown);
            sm.setUndone(Math.max(0, undone));
            sm.setMastery(total > 0 ? bd(sm.getMastered() * 100.0 / total) : BigDecimal.ZERO);

            subjects.add(sm);
        }
        vo.setSubjects(subjects);

        return vo;
    }

    @Override
    public Map<String, Object> getHeatmap() {
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>().isNotNull(Question::getExamYear).orderByAsc(Question::getExamYear));
        List<Integer> years = questions.stream().map(Question::getExamYear).distinct().collect(Collectors.toList());

        // subject -> chapter -> year 计数
        Map<String, LinkedHashMap<String, Map<Integer, Integer>>> bySubject = new LinkedHashMap<>();
        for (Question q : questions) {
            String subject = q.getSubject() != null ? q.getSubject() : "未分类";
            String chapter = extractChapter(q.getKnowledgeTag());
            Integer year = q.getExamYear();
            LinkedHashMap<String, Map<Integer, Integer>> chapters =
                    bySubject.computeIfAbsent(subject, k -> new LinkedHashMap<>());
            Map<Integer, Integer> yearCounts = chapters.computeIfAbsent(chapter, k -> new LinkedHashMap<>());
            yearCounts.merge(year, 1, Integer::sum);
        }

        List<Map<String, Object>> subjectList = new ArrayList<>();
        int maxCount = 1;
        for (Map.Entry<String, LinkedHashMap<String, Map<Integer, Integer>>> se : bySubject.entrySet()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map.Entry<String, Map<Integer, Integer>> ce : se.getValue().entrySet()) {
                List<Integer> counts = new ArrayList<>();
                for (Integer y : years) {
                    Integer c = ce.getValue().getOrDefault(y, 0);
                    counts.add(c);
                    if (c > maxCount) maxCount = c;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("label", ce.getKey());
                row.put("counts", counts);
                rows.add(row);
            }
            Map<String, Object> subject = new LinkedHashMap<>();
            subject.put("name", se.getKey());
            subject.put("rows", rows);
            subjectList.add(subject);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("years", years);
        result.put("subjects", subjectList);
        result.put("maxCount", maxCount);
        return result;
    }

    @Override
    public Map<String, Object> getMastery(Long userId) {
        List<Question> all = questionMapper.selectList(new LambdaQueryWrapper<>());
        // questionId -> subject
        Map<Long, String> qSubject = new HashMap<>();
        Map<String, Integer> subjectTotal = new LinkedHashMap<>();
        for (Question q : all) {
            qSubject.put(q.getId(), q.getSubject());
            subjectTotal.merge(q.getSubject(), 1, Integer::sum);
        }

        List<UserAnswer> answers = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId));
        Map<String, Set<Long>> answeredBySubject = new LinkedHashMap<>();
        Map<String, Set<Long>> correctBySubject = new LinkedHashMap<>();
        Set<Long> answeredAll = new HashSet<>();
        Set<Long> correctAll = new HashSet<>();
        for (UserAnswer a : answers) {
            Long qid = a.getQuestionId();
            String subject = qSubject.get(qid);
            if (subject == null) continue;
            answeredBySubject.computeIfAbsent(subject, k -> new HashSet<>()).add(qid);
            answeredAll.add(qid);
            if (Boolean.TRUE.equals(a.getIsCorrect())) {
                correctBySubject.computeIfAbsent(subject, k -> new HashSet<>()).add(qid);
                correctAll.add(qid);
            }
        }

        int totalAll = all.size();
        List<Map<String, Object>> subjectList = new ArrayList<>();
        for (String subject : SUBJECTS) {
            int total = subjectTotal.getOrDefault(subject, 0);
            int answered = answeredBySubject.getOrDefault(subject, Collections.emptySet()).size();
            int correct = correctBySubject.getOrDefault(subject, Collections.emptySet()).size();
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", subject);
            s.put("total", total);
            s.put("answered", answered);
            s.put("correct", correct);
            s.put("accuracy", answered > 0 ? round1(correct * 100.0 / answered) : 0.0);
            s.put("mastery", total > 0 ? round1(correct * 100.0 / total) : 0.0);
            subjectList.add(s);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", totalAll);
        result.put("answered", answeredAll.size());
        result.put("correct", correctAll.size());
        result.put("coverage", totalAll > 0 ? round1(answeredAll.size() * 100.0 / totalAll) : 0.0);
        result.put("subjects", subjectList);
        return result;
    }

    @Override
    public Map<String, Object> getCalendar(Long userId) {
        List<UserAnswer> answers = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId));
        Map<LocalDate, Integer> daily = new HashMap<>();
        for (UserAnswer a : answers) {
            if (a.getCreatedAt() == null) continue;
            daily.merge(a.getCreatedAt().toLocalDate(), 1, Integer::sum);
        }

        LocalDate today = LocalDate.now();
        LinkedHashMap<String, Integer> days = new LinkedHashMap<>();
        for (int i = 89; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            days.put(d.toString(), daily.getOrDefault(d, 0));
        }

        int consecutive = 0;
        LocalDate cursor = today;
        if (daily.getOrDefault(today, 0) == 0) {
            cursor = today.minusDays(1);
        }
        while (daily.getOrDefault(cursor, 0) > 0) {
            consecutive++;
            cursor = cursor.minusDays(1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("today", today.toString());
        result.put("days", days);
        result.put("consecutiveDays", consecutive);
        result.put("totalActiveDays", daily.size());
        return result;
    }

    @Override
    public Map<String, Object> getCountdown() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate target = parseDate(targetDateStr);
        String phase1End = parseDate(phase1EndStr).toString();
        String phase2End = parseDate(phase2EndStr).toString();

        long daysLeft = Math.max(0, ChronoUnit.DAYS.between(today, target));
        String phase;
        if (today.isAfter(parseDate(phase2EndStr))) {
            phase = "冲刺";
        } else if (today.isAfter(parseDate(phase1EndStr))) {
            phase = "强化";
        } else {
            phase = "基础";
        }

        result.put("targetDate", target.toString());
        result.put("daysLeft", daysLeft);
        result.put("passed", today.isAfter(target));
        result.put("phase", phase);
        result.put("phase1End", phase1End);
        result.put("phase2End", phase2End);
        result.put("today", today.toString());
        return result;
    }

    @Override
    public Map<String, Object> getMonthlyReport(Long userId, String month) {
        String m = (month == null || month.trim().isEmpty()) ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")) : month.trim();
        LocalDate monthStart;
        try {
            monthStart = LocalDate.parse(m + "-01");
        } catch (Exception e) {
            monthStart = LocalDate.now().withDayOfMonth(1);
        }
        LocalDate monthEnd = monthStart.plusMonths(1);
        LocalDateTime start = monthStart.atStartOfDay();
        LocalDateTime endExclusive = monthEnd.atStartOfDay();

        List<Question> all = questionMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, String> qSubject = new HashMap<>();
        for (Question q : all) {
            qSubject.put(q.getId(), q.getSubject());
        }

        List<UserAnswer> answers = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId));
        List<UserAnswer> monthAnswers = new ArrayList<>();
        for (UserAnswer a : answers) {
            if (a.getCreatedAt() != null && !a.getCreatedAt().isBefore(start) && a.getCreatedAt().isBefore(endExclusive)) {
                monthAnswers.add(a);
            }
        }

        int submissions = monthAnswers.size();
        Set<Long> answeredIds = new HashSet<>();
        Set<Long> correctIds = new HashSet<>();
        Map<String, Set<Long>> answeredBySubject = new LinkedHashMap<>();
        Map<String, Set<Long>> correctBySubject = new LinkedHashMap<>();
        Map<LocalDate, int[]> dayTrend = new TreeMap<>();
        for (UserAnswer a : monthAnswers) {
            String subject = qSubject.get(a.getQuestionId());
            answeredIds.add(a.getQuestionId());
            boolean correct = Boolean.TRUE.equals(a.getIsCorrect());
            if (correct) {
                correctIds.add(a.getQuestionId());
            }
            if (subject != null) {
                answeredBySubject.computeIfAbsent(subject, k -> new HashSet<>()).add(a.getQuestionId());
                if (correct) {
                    correctBySubject.computeIfAbsent(subject, k -> new HashSet<>()).add(a.getQuestionId());
                }
            }
            if (a.getCreatedAt() != null) {
                int[] cell = dayTrend.computeIfAbsent(a.getCreatedAt().toLocalDate(), k -> new int[2]);
                cell[0]++;
                if (correct) cell[1]++;
            }
        }

        List<Map<String, Object>> subjectList = new ArrayList<>();
        for (String subject : SUBJECTS) {
            int answered = answeredBySubject.getOrDefault(subject, Collections.emptySet()).size();
            int correct = correctBySubject.getOrDefault(subject, Collections.emptySet()).size();
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", subject);
            s.put("answered", answered);
            s.put("correct", correct);
            s.put("accuracy", answered > 0 ? round1(correct * 100.0 / answered) : 0.0);
            subjectList.add(s);
        }

        // 本月新增错题（按 last_wrong_at 落在本月内）
        List<WrongQuestion> wrongAll = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
        int wrongAdded = 0;
        Map<String, Integer> wrongReason = new LinkedHashMap<>();
        for (String reason : REASONS) {
            wrongReason.put(reason, 0);
        }
        int wrongReasonTagged = 0;
        int wrongReasonUntagged = 0;
        for (WrongQuestion wq : wrongAll) {
            if (wq.getLastWrongAt() != null && !wq.getLastWrongAt().isBefore(start) && wq.getLastWrongAt().isBefore(endExclusive)) {
                wrongAdded++;
                String reason = wq.getReason();
                if (reason == null || reason.trim().isEmpty()) {
                    wrongReasonUntagged++;
                } else {
                    wrongReasonTagged++;
                    wrongReason.merge(reason.trim(), 1, Integer::sum);
                }
            }
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> e : dayTrend.entrySet()) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("date", e.getKey().toString());
            t.put("submissions", e.getValue()[0]);
            t.put("correct", e.getValue()[1]);
            trend.add(t);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("month", monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        result.put("submissions", submissions);
        result.put("answered", answeredIds.size());
        result.put("correct", correctIds.size());
        result.put("accuracy", answeredIds.size() > 0 ? round1(correctIds.size() * 100.0 / answeredIds.size()) : 0.0);
        result.put("studyDays", dayTrend.size());
        result.put("wrongAdded", wrongAdded);
        result.put("wrongTotal", wrongAll.size());
        result.put("wrongReasonTagged", wrongReasonTagged);
        result.put("wrongReasonUntagged", wrongReasonUntagged);
        result.put("wrongReason", wrongReason);
        result.put("subjects", subjectList);
        result.put("trend", trend);
        return result;
    }

    private int countSubjectAnswers(Long userId, String subject) {
        LambdaQueryWrapper<UserAnswer> uw = new LambdaQueryWrapper<>();
        uw.eq(UserAnswer::getUserId, userId);
        List<UserAnswer> answers = userAnswerMapper.selectList(uw);
        return (int) answers.stream()
                .filter(a -> {
                    Question q = questionMapper.selectById(a.getQuestionId());
                    return q != null && subject.equals(q.getSubject());
                })
                .map(UserAnswer::getQuestionId)
                .distinct()
                .count();
    }

    private int countWrongByMastery(Long userId, String subject, String level) {
        LambdaQueryWrapper<WrongQuestion> ww = new LambdaQueryWrapper<>();
        ww.eq(WrongQuestion::getUserId, userId);
        List<WrongQuestion> wqs = wrongQuestionMapper.selectList(ww);
        return (int) wqs.stream()
                .filter(wq -> {
                    Question q = questionMapper.selectById(wq.getQuestionId());
                    if (q == null || !subject.equals(q.getSubject())) return false;
                    switch (level) {
                        case "mastered": return wq.getIsReviewed() != null && wq.getIsReviewed();
                        case "unfamiliar": return wq.getWrongCount() != null && wq.getWrongCount() >= 2;
                        case "unknown": return wq.getWrongCount() != null && wq.getWrongCount() == 1
                                && (wq.getIsReviewed() == null || !wq.getIsReviewed());
                        default: return false;
                    }
                })
                .count();
    }

    /** 从知识点标签中提取章节：形如 “计算机组成原理 — 存储系统” */
    private String extractChapter(String knowledgeTag) {
        if (knowledgeTag == null || knowledgeTag.trim().isEmpty()) return "未分类";
        String tag = knowledgeTag.trim();
        String[] parts = tag.split("[—–-]");
        if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
            return parts[1].trim();
        }
        return tag;
    }

    private LocalDate parseDate(String str) {
        try {
            return LocalDate.parse(str);
        } catch (Exception e) {
            return LocalDate.now().plusMonths(3);
        }
    }

    private double round1(double val) {
        return Math.round(val * 10) / 10.0;
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP);
    }
}