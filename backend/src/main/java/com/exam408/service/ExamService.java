package com.exam408.service;

import com.exam408.dto.ExamResultVO;
import com.exam408.dto.SubmitAnswerRequest;
import com.exam408.entity.ExamRecord;

import java.util.List;
import java.util.Map;

public interface ExamService {
    /** 提交答题并自动判分 */
    ExamResultVO submitAnswers(Long userId, SubmitAnswerRequest request);

    /** 获取历史刷题记录 */
    List<ExamRecord> getHistory(Long userId);

    /** 获取单次刷题详情 */
    ExamResultVO getExamDetail(Long userId, Long recordId);

    /** 获取各科正确率 */
    Map<String, Double> getSubjectAccuracy(Long userId);

    /** 保存单题答案（用于练习模式） */
    void saveSingleAnswer(Long userId, SubmitAnswerRequest request);
}
