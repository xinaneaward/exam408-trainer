package com.exam408.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.entity.WrongQuestion;
import com.exam408.entity.Question;
import com.exam408.mapper.QuestionMapper;
import com.exam408.mapper.WrongQuestionMapper;
import com.exam408.service.WrongQuestionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class WrongQuestionServiceImpl implements WrongQuestionService {

    /** 间隔重复复习间隔（天）：第1/2/3/4/5次做对后的下次复习时间 */
    private static final int[] INTERVALS_DAYS = {1, 3, 7, 14, 30};
    /** 达到该阶段视为已掌握 */
    private static final int MASTER_STAGE = INTERVALS_DAYS.length + 1;

    @Resource
    private WrongQuestionMapper wrongQuestionMapper;
    @Resource
    private QuestionMapper questionMapper;

    @Override
    public List<WrongQuestion> listByUser(Long userId) {
        LambdaQueryWrapper<WrongQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WrongQuestion::getUserId, userId)
               .orderByDesc(WrongQuestion::getLastWrongAt);
        return wrongQuestionMapper.selectList(wrapper);
    }

    @Override
    public List<WrongQuestion> listByUserAndSubject(Long userId, String subject) {
        return wrongQuestionMapper.selectByUserIdAndSubject(userId, subject);
    }

    @Override
    public List<WrongQuestion> listByUserAndTag(Long userId, String tag) {
        return wrongQuestionMapper.selectByUserIdAndTag(userId, tag);
    }

    @Override
    public Question getQuestionById(Long questionId) {
        return questionMapper.selectById(questionId);
    }

    @Override
    public void removeWrongQuestion(Long userId, Long wrongQuestionId) {
        WrongQuestion wq = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wq != null && wq.getUserId().equals(userId)) {
            wrongQuestionMapper.deleteById(wrongQuestionId);
        }
    }

    @Override
    public void markReviewed(Long userId, Long wrongQuestionId) {
        WrongQuestion wq = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wq != null && wq.getUserId().equals(userId)) {
            wq.setIsReviewed(true);
            wrongQuestionMapper.updateById(wq);
        }
    }

    @Override
    public void recordReviewResult(Long userId, Long wrongQuestionId, boolean correct) {
        WrongQuestion wq = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wq == null || !wq.getUserId().equals(userId)) {
            return;
        }
        if (correct) {
            int stage = (wq.getReviewStage() == null ? 0 : wq.getReviewStage()) + 1;
            wq.setReviewStage(stage);
            if (stage >= MASTER_STAGE) {
                wq.setIsReviewed(true);
                wq.setNextReviewAt(null);
            } else {
                wq.setIsReviewed(false);
                wq.setNextReviewAt(LocalDateTime.now().plusDays(INTERVALS_DAYS[Math.min(stage - 1, INTERVALS_DAYS.length - 1)]));
            }
        } else {
            // 重新做错：阶段与复习计划重置（错题已由提交接口再次收录更新）
            wq.setReviewStage(0);
            wq.setNextReviewAt(null);
            wq.setIsReviewed(false);
        }
        wrongQuestionMapper.updateById(wq);
    }

    @Override
    public Map<String, Integer> countBySubject(Long userId) {
        List<Map<String, Object>> list = wrongQuestionMapper.countGroupBySubject(userId);
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, Object> row : list) {
            String subject = (String) row.get("subject");
            Number cnt = (Number) row.get("cnt");
            if (subject != null && cnt != null) {
                result.put(subject, cnt.intValue());
            }
        }
        return result;
    }

    @Override
    public Integer countByUser(Long userId) {
        Integer count = wrongQuestionMapper.countByUserId(userId);
        return count != null ? count : 0;
    }
}
