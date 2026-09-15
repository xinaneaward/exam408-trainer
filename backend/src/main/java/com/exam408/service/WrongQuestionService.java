package com.exam408.service;

import com.exam408.entity.WrongQuestion;
import com.exam408.entity.Question;
import java.util.List;
import java.util.Map;

public interface WrongQuestionService {
    /** 获取用户错题列表 */
    List<WrongQuestion> listByUser(Long userId);
    /** 按科目筛选错题 */
    List<WrongQuestion> listByUserAndSubject(Long userId, String subject);
    /** 按知识点筛选错题 */
    List<WrongQuestion> listByUserAndTag(Long userId, String tag);
    /** 获取错题对应的题目详情 */
    Question getQuestionById(Long questionId);
    /** 移除错题(已掌握) */
    void removeWrongQuestion(Long userId, Long wrongQuestionId);
    /** 标记为已复习 */
    void markReviewed(Long userId, Long wrongQuestionId);
    /** 错题重练结果：做对推进间隔重复阶段，做错重置 */
    void recordReviewResult(Long userId, Long wrongQuestionId, boolean correct);
    /** 获取各科错题数量 */
    Map<String, Integer> countBySubject(Long userId);
    /** 获取错题总数 */
    Integer countByUser(Long userId);
}
