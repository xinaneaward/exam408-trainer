package com.exam408.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    /** 用户昵称 */
    private String nickname;
    /** 已做题数 */
    private Integer answeredCount;
    /** 总题目数 */
    private Integer totalQuestions;
    /** 完成度百分比 */
    private BigDecimal completionRate;
    /** 总正确率 */
    private BigDecimal totalAccuracy;
    /** 做题天数 */
    private Integer studyDays;
    /** 连续做题天数 */
    private Integer consecutiveDays;
    /** 最近90天做题日历 (日期 -> 做题数) */
    private Map<String, Integer> recentActivity;
    /** 各科掌握情况 */
    private List<SubjectMastery> subjects;

    @Data
    public static class SubjectMastery {
        private String name;          // 科目名
        private Integer total;         // 总题数
        private Integer answered;      // 已做
        private BigDecimal progress;   // 做题进度%
        private BigDecimal mastery;    // 掌握率%
        private Integer mastered;      // 掌握
        private Integer unfamiliar;    // 不熟
        private Integer unknown;       // 不会
        private Integer undone;        // 未做
    }
}
