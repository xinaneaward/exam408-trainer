package com.exam408.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ExamResultVO {
    private Long recordId;
    private Integer totalQuestions;
    private Integer correctCount;
    private BigDecimal score;
    private Integer durationSeconds;
    private List<QuestionResult> questions;

    @Data
    public static class QuestionResult {
        private Long questionId;
        private Integer questionNumber;
        private String content;
        private String options;
        private String correctAnswer;
        private String userAnswer;
        private Boolean isCorrect;
        private String analysis;
        private String subject;
        private String knowledgeTag;
        private String type;
        /** 该题实际得分（分） */
        private Integer score;
    }
}
