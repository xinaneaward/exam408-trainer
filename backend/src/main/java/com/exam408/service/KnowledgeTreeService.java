package com.exam408.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.dto.KnowledgeTreeVO;
import com.exam408.entity.*;
import com.exam408.mapper.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeTreeService {

    @Resource private QuestionMapper questionMapper;
    @Resource private UserAnswerMapper userAnswerMapper;
    @Resource private WrongQuestionMapper wrongQuestionMapper;

    // 408四科章节知识点结构
    // 说明：知识点名称与题库 question.knowledge_tags 的标签口径保持一致，
    // 这样每个知识点都能精确匹配到题目，掌握度统计才不会失真。
    private static final Object[][] DS_STRUCTURE = {
        {"线性结构", new String[]{"线性表","栈和队列","串","数组","广义表"}},
        {"树与图", new String[]{"树与二叉树","图"}},
        {"查找与排序", new String[]{"查找","排序"}},
        {"算法基础", new String[]{"算法"}},
    };

    private static final Object[][] CO_STRUCTURE = {
        {"数据表示与运算", new String[]{"数据表示"}},
        {"存储系统", new String[]{"存储系统"}},
        {"指令系统与CPU", new String[]{"指令系统","CPU"}},
        {"总线与输入输出", new String[]{"总线","I/O"}},
    };

    private static final Object[][] OS_STRUCTURE = {
        {"进程与调度", new String[]{"进程管理","调度","死锁"}},
        {"内存管理", new String[]{"内存管理"}},
        {"文件与输入输出", new String[]{"文件系统","I/O管理"}},
    };

    private static final Object[][] CN_STRUCTURE = {
        {"体系结构", new String[]{"体系结构"}},
        {"物理层与数据链路层", new String[]{"物理层","链路层"}},
        {"网络层", new String[]{"网络层"}},
        {"传输层与应用层", new String[]{"传输层","应用层"}},
    };

    public KnowledgeTreeVO getTree(Long userId) {
        KnowledgeTreeVO vo = new KnowledgeTreeVO();

        // 构建四科知识树
        List<KnowledgeTreeVO.SubjectNode> subjects = new ArrayList<>();
        subjects.add(buildSubject("数据结构", "DS", "#4caf50", DS_STRUCTURE, userId));
        subjects.add(buildSubject("计算机组成原理", "CO", "#2196f3", CO_STRUCTURE, userId));
        subjects.add(buildSubject("操作系统", "OS", "#ff9800", OS_STRUCTURE, userId));
        subjects.add(buildSubject("计算机网络", "CN", "#9c27b0", CN_STRUCTURE, userId));
        vo.setSubjects(subjects);

        // 构建热力图: year -> subjectCode -> cell
        List<Question> allQuestions = questionMapper.selectList(new LambdaQueryWrapper<>());
        Set<Integer> yearsSet = new TreeSet<>(Collections.reverseOrder());
        for (Question q : allQuestions) {
            // AI 变式题的 exam_year 为 NULL，需跳过，否则 TreeSet 会抛 NPE
            if (q.getExamYear() != null) yearsSet.add(q.getExamYear());
        }
        vo.setYears(new ArrayList<>(yearsSet));

        // 获取用户答题状态
        Map<Long, Boolean> correctMap = new HashMap<>();
        Map<Long, Boolean> wrongMap = new HashMap<>();
        if (userId != null) {
            List<UserAnswer> answers = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId)
            );
            for (UserAnswer a : answers) {
                correctMap.put(a.getQuestionId(), a.getIsCorrect() != null && a.getIsCorrect());
            }
            List<WrongQuestion> wqs = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId)
            );
            for (WrongQuestion wq : wqs) {
                wrongMap.put(wq.getQuestionId(), true);
            }
        }

        Map<Integer, Map<String, KnowledgeTreeVO.SubjectCell>> heatmap = new LinkedHashMap<>();
        for (Integer year : yearsSet) {
            Map<String, KnowledgeTreeVO.SubjectCell> row = new LinkedHashMap<>();
            for (String[] subj : new String[][]{{"DS","数据结构","#4caf50"},{"CO","计算机组成原理","#2196f3"},{"OS","操作系统","#ff9800"},{"CN","计算机网络","#9c27b0"}}) {
                String code = subj[0], name = subj[1], color = subj[2];
                List<Question> yearSubjQs = allQuestions.stream()
                    .filter(q -> year.equals(q.getExamYear()) && q.getSubject() != null && q.getSubject().contains(name.substring(0,2)))
                    .collect(Collectors.toList());

                KnowledgeTreeVO.SubjectCell cell = new KnowledgeTreeVO.SubjectCell();
                cell.setName(name);
                cell.setColor(color);
                cell.setTotalQuestions(yearSubjQs.size());
                int answered = 0, correct = 0;
                for (Question q : yearSubjQs) {
                    if (correctMap.containsKey(q.getId()) || wrongMap.containsKey(q.getId())) answered++;
                    if (correctMap.getOrDefault(q.getId(), false)) correct++;
                }
                cell.setAnsweredCount(answered);
                cell.setCorrectCount(correct);
                cell.setMastery(cell.getTotalQuestions() > 0 ? (double) correct / cell.getTotalQuestions() * 100 : 0);
                row.put(code, cell);
            }
            heatmap.put(year, row);
        }
        vo.setHeatmap(heatmap);

        return vo;
    }

    /**
     * 判断题目是否属于某个知识点。
     * 优先按细分标签 knowledge_tags（逗号分隔）精确比对，
     * 兼容旧数据时回退到 knowledge_tag 字符串包含。
     */
    private boolean matchesTag(Question q, String pointName) {
        if (q == null || pointName == null || pointName.isEmpty()) return false;
        String tags = q.getKnowledgeTags();
        if (tags != null && !tags.trim().isEmpty()) {
            for (String t : tags.split("[,，]")) {
                if (pointName.equals(t.trim())) return true;
            }
        }
        String tag = q.getKnowledgeTag();
        return tag != null && tag.contains(pointName);
    }

    private KnowledgeTreeVO.SubjectNode buildSubject(String name, String code, String color, Object[][] structure, Long userId) {
        KnowledgeTreeVO.SubjectNode sn = new KnowledgeTreeVO.SubjectNode();
        sn.setName(name); sn.setCode(code); sn.setColor(color);
        List<KnowledgeTreeVO.ChapterNode> chapters = new ArrayList<>();

        // 获取该科所有题目
        List<Question> subjQs = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .eq(Question::getSubject, name)
        );

        // 一次性取出该用户全部答题记录，避免逐知识点查询（N+1）
        Map<Long, Boolean> answerMap = new HashMap<>();
        if (userId != null && userId > 0) {
            List<UserAnswer> answers = userAnswerMapper.selectList(
                new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId)
            );
            for (UserAnswer a : answers) {
                if (a.getQuestionId() == null) continue;
                boolean correct = a.getIsCorrect() != null && a.getIsCorrect();
                // 同一题多次作答：有一次答对即视为已掌握
                answerMap.merge(a.getQuestionId(), correct, (oldV, newV) -> oldV || newV);
            }
        }

        // 统计知识点总数，用于无标签时的均匀切片
        int pointTotal = 0;
        for (Object[] ch : structure) {
            pointTotal += ((String[]) ch[1]).length;
        }
        int pointIndex = 0;
        Set<Long> correctIds = new HashSet<>();

        for (Object[] ch : structure) {
            String chName = (String) ch[0];
            String[] points = (String[]) ch[1];
            KnowledgeTreeVO.ChapterNode cn = new KnowledgeTreeVO.ChapterNode();
            cn.setName(chName);
            List<KnowledgeTreeVO.KnowledgePoint> kps = new ArrayList<>();
            for (int i = 0; i < points.length; i++) {
                KnowledgeTreeVO.KnowledgePoint kp = new KnowledgeTreeVO.KnowledgePoint();
                kp.setName(points[i]);
                // 匹配相关题目
                final String pname = points[i];
                List<Question> matched = subjQs.stream()
                    .filter(q -> matchesTag(q, pname))
                    .collect(Collectors.toList());
                if (matched.isEmpty()) {
                    // 极少数没有标签的题目：按序号均匀切片，保证题目不遗漏
                    int size = subjQs.size();
                    int start = size == 0 ? 0 : (int) Math.floor((double) pointIndex * size / Math.max(1, pointTotal));
                    int end = size == 0 ? 0 : (int) Math.floor((double) (pointIndex + 1) * size / Math.max(1, pointTotal));
                    end = Math.max(start, Math.min(end, size));
                    matched = new ArrayList<>(subjQs.subList(Math.min(start, size), end)).stream()
                        .filter(q -> q.getKnowledgeTags() == null || q.getKnowledgeTags().trim().isEmpty())
                        .collect(Collectors.toList());
                }
                pointIndex++;
                kp.setQuestionCount(matched.size());
                kp.setQuestionIds(matched.stream().map(Question::getId).collect(Collectors.toList()));
                List<int[]> yqs = new ArrayList<>();
                for (Question q : matched) {
                    if (q.getQuestionNumber() != null) {
                        yqs.add(new int[]{q.getExamYear(), q.getQuestionNumber()});
                    }
                }
                kp.setYearQnums(yqs);

                // 状态：基于该用户在本知识点的真实作答
                int correct = 0, wrong = 0;
                for (Question q : matched) {
                    Boolean isCorrect = answerMap.get(q.getId());
                    if (isCorrect == null) continue;
                    if (isCorrect) { correct++; correctIds.add(q.getId()); }
                    else wrong++;
                }
                if (correct + wrong == 0) kp.setStatus("undone");
                else if (correct > wrong) kp.setStatus("mastered");
                else if (wrong >= 2) kp.setStatus("unfamiliar");
                else kp.setStatus("unknown");

                kps.add(kp);
            }
            cn.setPoints(kps);
            chapters.add(cn);
        }
        sn.setChapters(chapters);
        int total = subjQs.size();
        sn.setTotalQuestions(total);
        // 掌握度 = 该科目答对题目数 / 该科目题目总数（与热力图口径一致）
        sn.setMasteryPercent(total > 0 ? (double) correctIds.size() / total * 100 : 0);
        return sn;
    }
}
