package com.exam408.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.dto.OverviewVO;
import com.exam408.entity.*;
import com.exam408.mapper.*;
import com.exam408.service.OverviewService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OverviewServiceImpl implements OverviewService {

    @Resource private QuestionMapper questionMapper;
    @Resource private UserAnswerMapper userAnswerMapper;
    @Resource private WrongQuestionMapper wrongQuestionMapper;

    // 408各科目知识点标签 + 每题对应的标签（基于题号映射）
    private static final String[] DS_TAGS = {
        "线性表", "栈和队列", "栈和队列", "树与二叉树", "树与二叉树", "树与二叉树",
        "图", "图", "查找", "查找", "排序",
        "算法设计", "算法设计"
    };
    private static final String[] CO_TAGS = {
        "数据的表示和运算", "数据的表示和运算", "存储系统", "存储系统", "存储系统",
        "指令系统", "中央处理器", "中央处理器", "总线", "输入输出系统", "输入输出系统",
        "存储系统设计", "指令系统设计"
    };
    private static final String[] OS_TAGS = {
        "进程管理", "进程管理", "进程管理", "内存管理", "内存管理",
        "文件管理", "文件管理", "输入输出管理", "输入输出管理", "操作系统概述",
        "进程同步设计", "内存管理设计"
    };
    private static final String[] CN_TAGS = {
        "计算机网络体系结构", "物理层", "数据链路层", "网络层", "网络层",
        "传输层", "传输层", "应用层",
        "网络层设计", "传输层设计"
    };

    private static final List<String> ALL_TAGS_DS = Arrays.asList(
        "线性表", "栈和队列", "树与二叉树", "图", "查找", "排序", "算法设计");
    private static final List<String> ALL_TAGS_CO = Arrays.asList(
        "数据的表示和运算", "存储系统", "指令系统", "中央处理器", "总线", "输入输出系统", "存储系统设计", "指令系统设计");
    private static final List<String> ALL_TAGS_OS = Arrays.asList(
        "操作系统概述", "进程管理", "内存管理", "文件管理", "输入输出管理", "进程同步设计", "内存管理设计");
    private static final List<String> ALL_TAGS_CN = Arrays.asList(
        "计算机网络体系结构", "物理层", "数据链路层", "网络层", "传输层", "应用层", "网络层设计", "传输层设计");

    @Override
    public OverviewVO getOverview(Long userId, Integer year) {
        OverviewVO vo = new OverviewVO();

        // 1. 构建科目知识标签
        vo.setSubjects(buildSubjectTags());

        // 2. 查询该年的题目
        List<Question> questions;
        if (year != null) {
            questions = questionMapper.selectByYear(year);
        } else {
            questions = questionMapper.selectList(new LambdaQueryWrapper<>());
        }

        // 3. 获取用户答题状态
        Set<Long> answeredIds = new HashSet<>();
        Set<Long> wrongIds = new HashSet<>();
        Set<Long> correctedIds = new HashSet<>();
        if (userId != null) {
            List<UserAnswer> answers = userAnswerMapper.selectList(
                    new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId));
            answeredIds = answers.stream().map(UserAnswer::getQuestionId).collect(Collectors.toSet());

            List<WrongQuestion> wqs = wrongQuestionMapper.selectList(
                    new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
            for (WrongQuestion wq : wqs) {
                wrongIds.add(wq.getQuestionId());
                if (wq.getIsReviewed() != null && wq.getIsReviewed()) {
                    correctedIds.add(wq.getQuestionId());
                }
            }
        }

        // 4. 构建年份×题号矩阵
        Map<Integer, Map<Integer, OverviewVO.CellInfo>> grid = new LinkedHashMap<>();

        // 确定要展示的年份范围
        Set<Integer> years = questions.stream().map(Question::getExamYear).collect(Collectors.toCollection(TreeSet::new));
        if (year != null) years = Collections.singleton(year);

        for (Integer y : years) {
            Map<Integer, OverviewVO.CellInfo> row = new LinkedHashMap<>();
            List<Question> yearQs = questions.stream()
                    .filter(q -> q.getExamYear().equals(y))
                    .collect(Collectors.toList());

            for (int qNum = 1; qNum <= 47; qNum++) {
                final int num = qNum;
                OverviewVO.CellInfo cell = new OverviewVO.CellInfo();
                Question q = yearQs.stream()
                        .filter(x -> x.getQuestionNumber() != null && x.getQuestionNumber() == num)
                        .findFirst().orElse(null);

                if (q != null) {
                    cell.setQuestionId(q.getId());
                    cell.setType(q.getType());
                    cell.setKnowledgeTag(getTag(qNum, q.getSubject()));
                }
                // 状态
                if (q != null && correctedIds.contains(q.getId())) cell.setStatus("mastered");
                else if (q != null && wrongIds.contains(q.getId())) cell.setStatus("wrong");
                else if (q != null && answeredIds.contains(q.getId())) cell.setStatus("done");
                else cell.setStatus("undone");

                row.put(qNum, cell);
            }
            grid.put(y, row);
        }
        vo.setGrid(grid);

        return vo;
    }

    private List<OverviewVO.SubjectTags> buildSubjectTags() {
        List<OverviewVO.SubjectTags> list = new ArrayList<>();
        list.add(createSubject("数据结构", "DS", ALL_TAGS_DS, new int[]{1,2,2,3,3,3,4,4,5,5,6,7,7}));
        list.add(createSubject("计算机组成原理", "CO", ALL_TAGS_CO, new int[]{12,12,13,13,13,14,15,15,16,17,17,18,18}));
        list.add(createSubject("操作系统", "OS", ALL_TAGS_OS, new int[]{23,23,23,24,24,25,25,26,26,27,28,29}));
        list.add(createSubject("计算机网络", "CN", ALL_TAGS_CN, new int[]{33,34,35,36,36,37,37,38,39,40}));
        return list;
    }

    private OverviewVO.SubjectTags createSubject(String name, String code, List<String> tagNames, int[] tagQuestionNums) {
        OverviewVO.SubjectTags st = new OverviewVO.SubjectTags();
        st.setName(name);
        st.setCode(code);
        List<OverviewVO.TagInfo> tags = new ArrayList<>();
        for (int i = 0; i < tagNames.size(); i++) {
            OverviewVO.TagInfo ti = new OverviewVO.TagInfo();
            ti.setName(tagNames.get(i));
            ti.setQuestionCount(1);
            ti.setQuestionNumbers(Collections.singletonList(tagQuestionNums[Math.min(i, tagQuestionNums.length - 1)]));
            tags.add(ti);
        }
        st.setTags(tags);
        return st;
    }

    private String getTag(int qNum, String subject) {
        if (subject == null) return "";
        if (subject.contains("数据结构")) {
            if (qNum <= 11) return DS_TAGS[qNum - 1];
            if (qNum <= 42) return "算法设计";
            return "";
        }
        if (subject.contains("组成原理")) {
            if (qNum >= 12 && qNum <= 22) return CO_TAGS[qNum - 12];
            if (qNum >= 43 && qNum <= 44) return "存储/指令设计";
            return "";
        }
        if (subject.contains("操作系统")) {
            if (qNum >= 23 && qNum <= 32) return OS_TAGS[qNum - 23];
            if (qNum >= 45 && qNum <= 46) return "进程/内存设计";
            return "";
        }
        if (subject.contains("计算机网络")) {
            if (qNum >= 33 && qNum <= 40) return CN_TAGS[qNum - 33];
            if (qNum == 47) return "网络设计";
            return "";
        }
        return "";
    }
}
