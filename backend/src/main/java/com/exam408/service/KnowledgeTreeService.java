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
    private static final Object[][] DS_STRUCTURE = {
        {"第一章 绪论", new String[]{"数据结构基本概念","算法与算法评价","时间复杂度","空间复杂度"}},
        {"第二章 线性表", new String[]{"线性表定义与操作","顺序存储结构","链式存储结构","单链表","双链表","循环链表","顺序表与链表比较"}},
        {"第三章 栈和队列", new String[]{"栈的定义与操作","顺序栈","链栈","队列的定义与操作","循环队列","链队列","栈与队列的应用"}},
        {"第四章 树与二叉树", new String[]{"树的基本概念","二叉树定义与性质","二叉树的存储结构","二叉树的遍历","线索二叉树","树与森林","哈夫曼树与哈夫曼编码","并查集"}},
        {"第五章 图", new String[]{"图的基本概念","图的存储结构","图的遍历","最小生成树","最短路径","拓扑排序","关键路径"}},
        {"第六章 查找", new String[]{"查找基本概念","顺序查找与折半查找","分块查找","B树与B+树","散列表","散列函数","冲突处理方法","KMP算法"}},
        {"第七章 排序", new String[]{"排序基本概念","插入排序","希尔排序","冒泡排序","快速排序","选择排序","堆排序","归并排序","基数排序","各种排序算法比较"}},
    };

    private static final Object[][] CO_STRUCTURE = {
        {"第一章 计算机系统概述", new String[]{"计算机发展历程","计算机系统层次结构","计算机性能指标","Amdahl定律"}},
        {"第二章 数据的表示和运算", new String[]{"数制与编码","定点数表示","浮点数表示","IEEE 754标准","算术逻辑单元","定点数运算","浮点数运算"}},
        {"第三章 存储系统", new String[]{"存储器分类","存储器层次结构","随机存取存储器","只读存储器","主存与CPU连接","双口RAM","Cache基本原理","Cache与主存映射","替换算法","写策略","虚拟存储器","页式虚拟存储器"}},
        {"第四章 指令系统", new String[]{"指令格式","指令寻址方式","数据寻址方式","CISC与RISC"}},
        {"第五章 中央处理器", new String[]{"CPU功能与结构","指令执行过程","数据通路","控制器设计","指令流水线","流水线冒险与处理","超标量流水线"}},
        {"第六章 总线", new String[]{"总线概述","总线仲裁","总线操作与定时","总线标准"}},
        {"第七章 输入输出系统", new String[]{"I/O系统概述","外部设备","I/O接口","程序查询方式","程序中断方式","DMA方式","通道方式"}},
    };

    private static final Object[][] OS_STRUCTURE = {
        {"第一章 操作系统概述", new String[]{"操作系统的概念与特征","操作系统的发展","操作系统的运行环境","系统调用"}},
        {"第二章 进程管理", new String[]{"进程的概念与特征","进程的状态与转换","进程控制","进程通信","线程","处理机调度","进程同步","临界区","信号量","管程","死锁","银行家算法"}},
        {"第三章 内存管理", new String[]{"内存管理概念","覆盖与交换","连续分配管理方式","非连续分配管理方式","分页存储管理","分段存储管理","段页式管理","虚拟内存","请求分页管理","页面置换算法","抖动与工作集"}},
        {"第四章 文件管理", new String[]{"文件系统基础","文件系统实现","目录结构","文件共享与保护","文件存储空间管理","磁盘组织与管理"}},
        {"第五章 输入输出管理", new String[]{"I/O管理概述","I/O控制方式","I/O软件层次结构","设备独立性","SPOOLing技术","缓冲管理","设备分配"}},
    };

    private static final Object[][] CN_STRUCTURE = {
        {"第一章 计算机网络体系结构", new String[]{"计算机网络概述","计算机网络体系结构","OSI参考模型","TCP/IP模型","计算机网络性能指标"}},
        {"第二章 物理层", new String[]{"通信基础","奈奎斯特定理","香农定理","编码与调制","电路交换/报文交换/分组交换","传输介质","物理层设备"}},
        {"第三章 数据链路层", new String[]{"数据链路层功能","组帧","差错控制","流量控制与可靠传输","停止-等待协议","后退N帧协议","选择重传协议","介质访问控制","CSMA/CD","CSMA/CA","局域网与广域网","以太网","PPP协议","数据链路层设备"}},
        {"第四章 网络层", new String[]{"网络层功能","路由算法","IPv4","IPv6","子网划分与CIDR","ARP/DHCP/ICMP","路由协议","IP组播","移动IP","网络层设备"}},
        {"第五章 传输层", new String[]{"传输层功能","UDP协议","TCP协议","TCP连接管理","TCP可靠传输","TCP流量控制","TCP拥塞控制"}},
        {"第六章 应用层", new String[]{"网络应用模型","DNS系统","FTP协议","电子邮件","WWW与HTTP","SNMP"}},
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
        for (Question q : allQuestions) yearsSet.add(q.getExamYear());
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
                    .filter(q -> q.getExamYear().equals(year) && q.getSubject() != null && q.getSubject().contains(name.substring(0,2)))
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

    private KnowledgeTreeVO.SubjectNode buildSubject(String name, String code, String color, Object[][] structure, Long userId) {
        KnowledgeTreeVO.SubjectNode sn = new KnowledgeTreeVO.SubjectNode();
        sn.setName(name); sn.setCode(code); sn.setColor(color);
        List<KnowledgeTreeVO.ChapterNode> chapters = new ArrayList<>();
        int totalQ = 0, masteredQ = 0;

        // 获取该科所有题目
        String subjectPrefix = name.substring(0,2);
        List<Question> subjQs = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .isNotNull(Question::getSubject)
                .likeRight(Question::getSubject, subjectPrefix)
        );

        for (Object[] ch : structure) {
            String chName = (String) ch[0];
            String[] points = (String[]) ch[1];
            KnowledgeTreeVO.ChapterNode cn = new KnowledgeTreeVO.ChapterNode();
            cn.setName(chName);
            List<KnowledgeTreeVO.KnowledgePoint> kps = new ArrayList<>();
            for (int i = 0; i < points.length; i++) {
                final int idx = i;
                KnowledgeTreeVO.KnowledgePoint kp = new KnowledgeTreeVO.KnowledgePoint();
                kp.setName(points[idx]);
                // 匹配相关题目
                final String pname = points[idx];
                List<Question> matched = subjQs.stream()
                    .filter(q -> q.getKnowledgeTag() != null && q.getKnowledgeTag().contains(pname.substring(0, Math.min(2, pname.length()))))
                    .collect(Collectors.toList());
                if (matched.isEmpty()) {
                    // 均匀分配一些题目
                    int start = idx * (subjQs.size() / Math.max(1, points.length));
                    int end = (idx + 1) * (subjQs.size() / Math.max(1, points.length));
                    matched = subjQs.subList(Math.min(start, subjQs.size()), Math.min(end, subjQs.size()));
                }
                kp.setQuestionCount(matched.size());
                kp.setQuestionIds(matched.stream().map(Question::getId).collect(Collectors.toList()));
                List<int[]> yqs = new ArrayList<>();
                for (Question q : matched) {
                    if (q.getQuestionNumber() != null) {
                        yqs.add(new int[]{q.getExamYear(), q.getQuestionNumber()});
                    }
                }
                kp.setYearQnums(yqs);
                // 状态
                int correct = 0, wrong = 0;
                if (userId != null) {
                    List<UserAnswer> answers = userAnswerMapper.selectList(
                        new LambdaQueryWrapper<UserAnswer>().eq(UserAnswer::getUserId, userId)
                            .in(UserAnswer::getQuestionId, kp.getQuestionIds())
                    );
                    for (UserAnswer a : answers) {
                        if (a.getIsCorrect() != null && a.getIsCorrect()) correct++;
                        else wrong++;
                    }
                }
                if (correct + wrong == 0) kp.setStatus("undone");
                else if (correct > wrong * 2) kp.setStatus("mastered");
                else if (wrong >= 2) kp.setStatus("unfamiliar");
                else kp.setStatus("unknown");

                totalQ += matched.size();
                if ("mastered".equals(kp.getStatus())) masteredQ++;
                kps.add(kp);
            }
            cn.setPoints(kps);
            chapters.add(cn);
        }
        sn.setChapters(chapters);
        sn.setTotalQuestions(totalQ);
        sn.setMasteryPercent(totalQ > 0 ? (double) masteredQ / totalQ * 100 : 0);
        return sn;
    }
}
