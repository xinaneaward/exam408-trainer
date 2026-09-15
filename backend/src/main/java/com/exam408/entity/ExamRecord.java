package com.exam408.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField("exam_year")
    private Integer year;

    private String subject;

    /** year/subject/random/wrong */
    private String mode;

    private Integer totalQuestions;

    private Integer correctCount;

    private BigDecimal score;

    private Integer durationSeconds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
