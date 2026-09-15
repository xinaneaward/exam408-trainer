package com.exam408.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer examYear;

    private String subject;

    private String type;

    private Integer questionNumber;

    private String content;

    /** JSON格式选项 [{"key":"A","text":"..."}] */
    private String options;

    private String answer;

    private String analysis;

    private String knowledgeTag;

    @TableField("knowledge_tags")
    private String knowledgeTags;

    /** 来源：real=真题 / ai=AI生成 */
    private String source;

    /** AI 题是否通过自检 */
    private Boolean verified;
}
