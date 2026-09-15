package com.exam408.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class KnowledgeTreeVO {
    /** 四科知识点树 */
    private List<SubjectNode> subjects;
    /** 年份×科目热力图矩阵 year -> subjectCode -> mastery info */
    private Map<Integer, Map<String, SubjectCell>> heatmap;
    /** 可用年份列表 */
    private List<Integer> years;

    @Data
    public static class SubjectNode {
        private String name;      // 科目名
        private String code;      // DS/CO/OS/CN
        private String color;     // 主题色
        private Integer totalQuestions;
        private Double masteryPercent;
        private List<ChapterNode> chapters;
    }

    @Data
    public static class ChapterNode {
        private String name;      // 章节名
        private List<KnowledgePoint> points;
    }

    @Data
    public static class KnowledgePoint {
        private String name;      // 知识点名
        private String status;    // mastered/unfamiliar/unknown/undone
        private Integer questionCount;
        private List<Long> questionIds;
        private List<int[]> yearQnums; // [{year, qNum}] pairs
    }

    @Data
    public static class SubjectCell {
        private String name;       // 科目名
        private Integer totalQuestions;
        private Integer answeredCount;
        private Integer correctCount;
        private Double mastery;
        private String color;      // 展示色
    }
}
