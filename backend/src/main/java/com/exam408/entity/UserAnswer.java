package com.exam408.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_answer")
public class UserAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private Long examRecordId;

    private String userAnswer;

    private Boolean isCorrect;

    private Boolean isSelfScored;

    /** 该题得分（应用题AI评分，0-100） */
    private Integer score;

    /** 应用题AI评分反馈 */
    private String aiFeedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
