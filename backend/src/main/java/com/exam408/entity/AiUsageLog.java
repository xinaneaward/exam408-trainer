package com.exam408.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_usage_log")
public class AiUsageLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** explain / chat */
    private String feature;

    private Integer promptTokens;

    private Integer completionTokens;

    private LocalDateTime createdAt;
}
