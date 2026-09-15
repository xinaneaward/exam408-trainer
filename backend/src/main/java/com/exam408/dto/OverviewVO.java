package com.exam408.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class OverviewVO {
    /** 各科知识标签 */
    private List<SubjectTags> subjects;
    /** 年份×题号矩阵: year -> questionNumber -> cell info */
    private Map<Integer, Map<Integer, CellInfo>> grid;

    @Data
    public static class SubjectTags {
        private String name;
        private String code;
        private List<TagInfo> tags;
    }

    @Data
    public static class TagInfo {
        private String name;
        private Integer questionCount;
        private List<Integer> questionNumbers; // 该标签对应的题号
    }

    @Data
    public static class CellInfo {
        private Long questionId;
        private String knowledgeTag;
        private String type;       // 单选/综合应用
        private String status;     // undone/done/wrong/mastered
    }
}
