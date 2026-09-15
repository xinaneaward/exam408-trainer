package com.exam408.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.dto.ExamResultVO;
import com.exam408.dto.SubmitAnswerRequest;
import com.exam408.entity.*;
import com.exam408.mapper.*;
import com.exam408.service.ExamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamServiceImpl implements ExamService {

    @Resource
    private ExamRecordMapper examRecordMapper;
    @Resource
    private UserAnswerMapper userAnswerMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private WrongQuestionMapper wrongQuestionMapper;

    @Override
    @Transactional
    public ExamResultVO submitAnswers(Long userId, SubmitAnswerRequest request) {
        // 1. 创建考试记录
        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setYear(request.getYear());
        record.setSubject(request.getSubject());
        record.setMode(request.getMode() != null ? request.getMode() : "year");
        record.setTotalQuestions(request.getAnswers().size());
        record.setDurationSeconds(request.getDurationSeconds());

        // 2. 逐个判分（综合应用题展示答案不计分，仅统计单选题）
        int correctCount = 0;
        List<ExamResultVO.QuestionResult> questionResults = new ArrayList<>();
        Set<Long> wrongQuestionIds = new HashSet<>();
        BigDecimal scoreSum = BigDecimal.ZERO;

        for (int i = 0; i < request.getAnswers().size(); i++) {
            SubmitAnswerRequest.AnswerItem item = request.getAnswers().get(i);
            Question question = questionMapper.selectById(item.getQuestionId());
            if (question == null) continue;

            boolean isCorrect = false;
            Integer questionScore = 0;

            if ("单选".equals(question.getType())) {
                // 选择题: 精确匹配
                isCorrect = question.getAnswer().trim().equalsIgnoreCase(
                        item.getUserAnswer() != null ? item.getUserAnswer().trim() : "");
                questionScore = isCorrect ? 2 : 0;

                if (isCorrect) correctCount++;
                scoreSum = scoreSum.add(BigDecimal.valueOf(questionScore));

                // 保存答题明细（仅选择题）
                UserAnswer ua = new UserAnswer();
                ua.setUserId(userId);
                ua.setQuestionId(item.getQuestionId());
                ua.setUserAnswer(item.getUserAnswer());
                ua.setIsCorrect(isCorrect);
                ua.setScore(questionScore);
                ua.setExamRecordId(record.getId());
                userAnswerMapper.insert(ua);

                // 错题收录（仅选择题）
                if (!isCorrect) {
                    wrongQuestionIds.add(item.getQuestionId());
                }
            }
            // 综合应用题：不可作答、只展示参考答案，不计分、不保存、不收录错题

            // 组装结果
            ExamResultVO.QuestionResult qr = new ExamResultVO.QuestionResult();
            qr.setQuestionId(question.getId());
            qr.setQuestionNumber(question.getQuestionNumber());
            qr.setContent(question.getContent());
            qr.setOptions(question.getOptions());
            qr.setCorrectAnswer(question.getAnswer());
            qr.setUserAnswer(item.getUserAnswer());
            qr.setIsCorrect(isCorrect);
            qr.setScore(questionScore);
            qr.setAnalysis(question.getAnalysis());
            qr.setSubject(question.getSubject());
            qr.setKnowledgeTag(question.getKnowledgeTag());
            qr.setType(question.getType());
            questionResults.add(qr);
        }

        // 4. 得分
        record.setCorrectCount(correctCount);
        record.setScore(scoreSum.setScale(1, RoundingMode.HALF_UP));
        examRecordMapper.insert(record);

        // 5. 更新错题本
        for (Long qid : wrongQuestionIds) {
            LambdaQueryWrapper<WrongQuestion> wqWrapper = new LambdaQueryWrapper<>();
            wqWrapper.eq(WrongQuestion::getUserId, userId)
                     .eq(WrongQuestion::getQuestionId, qid);
            WrongQuestion wq = wrongQuestionMapper.selectOne(wqWrapper);
            if (wq != null) {
                wq.setWrongCount(wq.getWrongCount() + 1);
                wq.setLastWrongAt(LocalDateTime.now());
                wq.setIsReviewed(false);
                // 再做错：间隔重复阶段重置
                wq.setReviewStage(0);
                wq.setNextReviewAt(null);
                wrongQuestionMapper.updateById(wq);
            } else {
                wq = new WrongQuestion();
                wq.setUserId(userId);
                wq.setQuestionId(qid);
                wq.setWrongCount(1);
                wq.setLastWrongAt(LocalDateTime.now());
                wq.setIsReviewed(false);
                wq.setReviewStage(0);
                wq.setNextReviewAt(null);
                wrongQuestionMapper.insert(wq);
            }
        }

        // 6. 组装返回结果
        ExamResultVO result = new ExamResultVO();
        result.setRecordId(record.getId());
        result.setTotalQuestions(request.getAnswers().size());
        result.setCorrectCount(correctCount);
        result.setScore(record.getScore());
        result.setDurationSeconds(request.getDurationSeconds());
        result.setQuestions(questionResults);
        return result;
    }

    @Override
    public List<ExamRecord> getHistory(Long userId) {
        return examRecordMapper.selectByUserId(userId);
    }

    @Override
    public ExamResultVO getExamDetail(Long userId, Long recordId) {
        List<UserAnswer> answers = userAnswerMapper.selectByExamRecordId(recordId);
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new RuntimeException("记录不存在");
        }

        List<ExamResultVO.QuestionResult> questionResults = new ArrayList<>();
        for (UserAnswer ua : answers) {
            Question question = questionMapper.selectById(ua.getQuestionId());
            if (question == null) continue;

            ExamResultVO.QuestionResult qr = new ExamResultVO.QuestionResult();
            qr.setQuestionId(question.getId());
            qr.setQuestionNumber(question.getQuestionNumber());
            qr.setContent(question.getContent());
            qr.setOptions(question.getOptions());
            qr.setCorrectAnswer(question.getAnswer());
            qr.setUserAnswer(ua.getUserAnswer());
            qr.setIsCorrect(ua.getIsCorrect());
            qr.setScore(ua.getScore());
            qr.setAnalysis(question.getAnalysis());
            qr.setSubject(question.getSubject());
            qr.setKnowledgeTag(question.getKnowledgeTag());
            qr.setType(question.getType());
            questionResults.add(qr);
        }

        ExamResultVO result = new ExamResultVO();
        result.setRecordId(record.getId());
        result.setTotalQuestions(record.getTotalQuestions());
        result.setCorrectCount(record.getCorrectCount());
        result.setScore(record.getScore());
        result.setDurationSeconds(record.getDurationSeconds());
        result.setQuestions(questionResults);
        return result;
    }

    @Override
    public Map<String, Double> getSubjectAccuracy(Long userId) {
        Map<String, Double> accuracyMap = new LinkedHashMap<>();
        String[] subjects = {"数据结构", "计算机组成原理", "操作系统", "计算机网络"};
        for (String subject : subjects) {
            List<UserAnswer> answers = getUserAnswersBySubject(userId, subject);
            if (answers.isEmpty()) {
                accuracyMap.put(subject, 0.0);
            } else {
                long correct = answers.stream().filter(UserAnswer::getIsCorrect).count();
                accuracyMap.put(subject, correct * 100.0 / answers.size());
            }
        }
        return accuracyMap;
    }

    @Override
    @Transactional
    public void saveSingleAnswer(Long userId, SubmitAnswerRequest request) {
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return;
        }

        SubmitAnswerRequest.AnswerItem item = request.getAnswers().get(0);
        Question question = questionMapper.selectById(item.getQuestionId());
        if (question == null) return;

        // 综合应用题：不可作答、只展示参考答案，不判分、不保存、不收录错题
        if (!"单选".equals(question.getType())) {
            return;
        }

        boolean isCorrect = question.getAnswer().trim().equalsIgnoreCase(
                item.getUserAnswer() != null ? item.getUserAnswer().trim() : "");
        Integer questionScore = isCorrect ? 2 : 0;

        LambdaQueryWrapper<UserAnswer> uaWrapper = new LambdaQueryWrapper<>();
        uaWrapper.eq(UserAnswer::getUserId, userId)
                 .eq(UserAnswer::getQuestionId, item.getQuestionId());
        UserAnswer existing = userAnswerMapper.selectOne(uaWrapper);

        if (existing != null) {
            existing.setUserAnswer(item.getUserAnswer());
            existing.setIsCorrect(isCorrect);
            existing.setScore(questionScore);
            userAnswerMapper.updateById(existing);
        } else {
            UserAnswer ua = new UserAnswer();
            ua.setUserId(userId);
            ua.setQuestionId(item.getQuestionId());
            ua.setUserAnswer(item.getUserAnswer());
            ua.setIsCorrect(isCorrect);
            ua.setScore(questionScore);
            userAnswerMapper.insert(ua);
        }

        if (!isCorrect) {
            LambdaQueryWrapper<WrongQuestion> wqWrapper = new LambdaQueryWrapper<>();
            wqWrapper.eq(WrongQuestion::getUserId, userId)
                     .eq(WrongQuestion::getQuestionId, item.getQuestionId());
            WrongQuestion wq = wrongQuestionMapper.selectOne(wqWrapper);
            if (wq != null) {
                wq.setWrongCount(wq.getWrongCount() + 1);
                wq.setLastWrongAt(LocalDateTime.now());
                wq.setIsReviewed(false);
                wq.setReviewStage(0);
                wq.setNextReviewAt(null);
                wrongQuestionMapper.updateById(wq);
            } else {
                wq = new WrongQuestion();
                wq.setUserId(userId);
                wq.setQuestionId(item.getQuestionId());
                wq.setWrongCount(1);
                wq.setLastWrongAt(LocalDateTime.now());
                wq.setIsReviewed(false);
                wq.setReviewStage(0);
                wq.setNextReviewAt(null);
                wrongQuestionMapper.insert(wq);
            }
        }
    }

    private List<UserAnswer> getUserAnswersBySubject(Long userId, String subject) {
        LambdaQueryWrapper<UserAnswer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAnswer::getUserId, userId);
        List<UserAnswer> allAnswers = userAnswerMapper.selectList(wrapper);
        return allAnswers.stream()
                .filter(a -> {
                    Question q = questionMapper.selectById(a.getQuestionId());
                    return q != null && subject.equals(q.getSubject());
                })
                .collect(Collectors.toList());
    }
}
