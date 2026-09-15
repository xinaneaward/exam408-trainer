package com.exam408.dto;

import lombok.Data;
import java.util.List;

@Data
public class SubmitAnswerRequest {
    /** 刷题模式 */
    private String mode;
    /** 年份 */
    private Integer year;
    /** 科目 */
    private String subject;
    /** 答题列表 */
    private List<AnswerItem> answers;
    /** 用时(秒) */
    private Integer durationSeconds;

    @Data
    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;
    }
}
