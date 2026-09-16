package com.exam408.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wrong_question")
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private Integer wrongCount;

    private LocalDateTime lastWrongAt;

    private Boolean isReviewed;

    /** 间隔重复复习阶段：0起，做对一次+1，做错重置为0 */
    private Integer reviewStage;

    /** 下次复习时间：空则视为到期待复习 */
    private LocalDateTime nextReviewAt;

    /** 错误原因归类：概念不清/粗心/计算错/审题不清/其他 */
    private String reason;
}
