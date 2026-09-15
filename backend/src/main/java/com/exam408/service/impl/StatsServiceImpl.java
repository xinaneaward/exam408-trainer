package com.exam408.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.dto.DashboardVO;
import com.exam408.dto.StatsVO;
import com.exam408.entity.*;
import com.exam408.mapper.*;
import com.exam408.service.ExamService;
import com.exam408.service.StatsService;
import com.exam408.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    // 每题约11题单选+2题综合
    private static final int[] QUESTION_COUNTS = {233, 235, 216, 162}; // 合计846

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

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP);
    }
}
