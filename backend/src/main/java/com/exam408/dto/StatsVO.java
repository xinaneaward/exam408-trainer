package com.exam408.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StatsVO {
    /** 总刷题数 */
    private Integer totalQuestions;
    /** 总正确数 */
    private Integer totalCorrect;
    /** 总正确率 */
    private BigDecimal totalAccuracy;
    /** 错题数 */
    private Integer wrongCount;
    /** 数据结构正确率 */
    private BigDecimal dsAccuracy;
    /** 计算机组成原理正确率 */
    private BigDecimal coAccuracy;
    /** 操作系统正确率 */
    private BigDecimal osAccuracy;
    /** 计算机网络正确率 */
    private BigDecimal cnAccuracy;
    /** 总刷题次数 */
    private Integer examCount;
}
