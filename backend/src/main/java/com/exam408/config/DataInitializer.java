package com.exam408.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.entity.Question;
import com.exam408.entity.User;
import com.exam408.mapper.QuestionMapper;
import com.exam408.mapper.UserMapper;
import com.exam408.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Resource private QuestionMapper questionMapper;
    @Resource private UserMapper userMapper;

    // 各科目预设真实题目内容
    private static final String[][] REAL_QUESTIONS = {
        // 数据结构题目
        {
            "设栈 S 和队列 Q 的初始状态均为空，元素 a, b, c, d, e, f, g 依次进入栈 S。若每个元素出栈后立即进入队列 Q，且 7 个元素出队的顺序是 b, d, c, f, e, a, g，则栈 S 的容量至少是",
            "设一棵完全二叉树共有 699 个结点，则在该二叉树中的叶子结点数为",
            "在一棵具有 n 个结点的二叉树中，所有结点的度数之和为",
            "下列二叉排序树中，满足平衡二叉树定义的是（）。",
            "在一个具有 n 个顶点的有向图中，若所有顶点的出度之和为 s，则所有顶点的入度之和为",
            "设某棵二叉树的中序遍历序列为 ABCDEF，后序遍历序列为 BDCAFE，则其前序遍历序列为",
            "在长度为 n 的顺序表中，删除第 i 个元素需要移动的元素个数为",
            "设指针变量 p 指向单链表中结点 A，若要删除结点 A 之后的结点（存在），则需要修改指针的操作为",
            "设循环队列的存储空间为 Q(1:50)，初始状态为 front=rear=50。经过一系列入队与退队操作后，front=rear=25，则该循环队列中的元素个数为",
            "设一棵二叉树的前序遍历序列为 ABDECF，中序遍历序列为 DBEAFC，则该二叉树的后序遍历序列为",
            "在一个长度为 10 的顺序表中删除第 3 个元素，需要移动的元素个数为"
        },
        // 计算机组成原理题目
        {
            "某计算机字长为 16 位，主存容量为 64KB，若按字编址，其寻址范围是",
            "在主存和 CPU 之间增加高速缓存 Cache 的目的是",
            "CPU 执行一条指令所需的时间称为",
            "设指令字长为 16 位，采用扩展操作码技术，若指令系统具有 12 条二地址指令、8 条一地址指令和 16 条零地址指令，则二地址指令的操作码长度是",
            "在机器数中，零的表示形式是唯一的是",
            "某计算机主存容量为 64KB，其中 ROM 区为 4KB，其余为 RAM 区，按字节编址。现要用 2K×8 位的 ROM 芯片和 4K×4 位的 RAM 芯片来设计该存储器，则需要上述规格的 ROM 芯片数和 RAM 芯片数分别是",
            "CPU 中的程序计数器（PC）的功能是",
            "在微程序控制器中，微指令的控制方式可采用直接控制和编码控制两种，在微操作命令个数相同的情况下，前者的微指令字长比后者的",
            "某计算机系统的 CPU 有 32 根地址线，主存按字节编址，则其最大主存容量为",
            "在指令的寻址方式中，操作数包含在指令中的是",
            "设指令字长等于机器字长，若一台计算机的机器字长为 32 位，则一条指令的长度为"
        },
        // 操作系统题目
        {
            "下列选项中，不可能在用户态发生的事件是",
            "设与某资源相关联的信号量初值为 3，当前值为 1，则表示当前有几个进程在等待该资源",
            "下列关于进程和线程的叙述中，正确的是",
            "在支持多线程的系统中，进程 P 创建的若干线程不能共享的是",
            "下列选项中，导致创建新进程的操作是",
            "设系统中有 n（n>2）个进程，且当前没有执行进程调度程序，下列情况中，不可能发生的是",
            "下列关于死锁的说法中，正确的是",
            "某系统中有 3 个并发进程，都需要同类资源 4 个，试问该系统不会发生死锁的最少资源数是",
            "下列选项中，满足短任务优先且不会发生饥饿现象的调度算法是",
            "在请求分页存储管理系统中，若采用 FIFO 页面置换算法，则当分配的页帧数增加时，缺页中断的次数",
            "下列关于虚拟存储的叙述中，正确的是"
        },
        // 计算机网络题目
        {
            "在 OSI 参考模型中，负责实现端到端可靠传输的是",
            "TCP 三次握手过程中，第二次握手时服务器发送的报文段中，SYN 和 ACK 标志位分别为",
            "下列关于 TCP 和 UDP 的叙述中，正确的是",
            "某网络的 IP 地址空间为 192.168.5.0/24，采用定长子网划分，子网掩码为 255.255.255.248，则该网络的最大子网个数和每个子网内的最大可分配地址个数分别是",
            "HTTP 协议默认使用的端口号是",
            "在 TCP/IP 协议栈中，实现 IP 地址到 MAC 地址转换的协议是",
            "下列关于 CSMA/CD 协议的叙述中，正确的是",
            "某主机的 IP 地址为 172.16.7.131/26，则该主机所在子网的网络地址为",
            "TCP 连接建立过程中，第三次握手时客户端发送的报文段中，SYN 和 ACK 标志位分别为",
            "下列关于 UDP 的叙述中，错误的是",
            "在 OSI 参考模型中，路由器工作的层次是"
        }
    };

    // 各科目预设选项模板
    private static final String[][][] REAL_OPTIONS = {
        // 数据结构选项
        {
            {"A. 3", "B. 4", "C. 5", "D. 6"},
            {"A. 349", "B. 350", "C. 351", "D. 352"},
            {"A. n", "B. n-1", "C. n+1", "D. 2n-1"},
            {"A. ", "B. ", "C. ", "D. "},
            {"A. s", "B. s-1", "C. s+1", "D. 2s"},
            {"A. ABCDEF", "B. ABDCEF", "C. ABEFCD", "D. ABEDCF"},
            {"A. n-i", "B. n-i+1", "C. i", "D. i-1"},
            {"A. p.next = p.next.next", "B. p = p.next.next", "C. p.next = p", "D. p = p.next"},
            {"A. 0 或 50", "B. 25", "C. 26", "D. 49"},
            {"A. DEBFCA", "B. DEBFAC", "C. DEBCFA", "D. DBECFA"},
            {"A. 6", "B. 7", "C. 8", "D. 9"}
        },
        // 计算机组成原理选项
        {
            {"A. 0~65535", "B. 0~32767", "C. 0~64511", "D. 0~32768"},
            {"A. 扩大主存容量", "B. 提高存储系统的存取速度", "C. 降低存储系统的成本", "D. 增加主存带宽"},
            {"A. 时钟周期", "B. 指令周期", "C. CPU周期", "D. 机器周期"},
            {"A. 4 位", "B. 5 位", "C. 6 位", "D. 7 位"},
            {"A. 原码", "B. 补码", "C. 反码", "D. 移码"},
            {"A. 2, 30", "B. 2, 60", "C. 4, 30", "D. 4, 60"},
            {"A. 存放当前执行指令的地址", "B. 存放当前执行指令", "C. 存放下一条要执行指令的地址", "D. 存放上一条执行过的指令地址"},
            {"A. 更长", "B. 更短", "C. 相同", "D. 无法比较"},
            {"A. 4GB", "B. 2GB", "C. 1GB", "D. 512MB"},
            {"A. 直接寻址", "B. 立即寻址", "C. 寄存器寻址", "D. 间接寻址"},
            {"A. 16 位", "B. 32 位", "C. 64 位", "D. 128 位"}
        },
        // 操作系统选项
        {
            {"A. 系统调用", "B. 外部中断", "C. 进程切换", "D. 缺页"},
            {"A. 0", "B. 1", "C. 2", "D. 3"},
            {"A. 进程是资源分配的基本单位，线程是CPU调度的基本单位", "B. 进程和线程都是资源分配的基本单位", "C. 进程和线程都是CPU调度的基本单位", "D. 进程是CPU调度的基本单位，线程是资源分配的基本单位"},
            {"A. 进程的代码段", "B. 进程的全局变量", "C. 进程的堆空间", "D. 进程的进程控制块"},
            {"A. 用户登录成功", "B. 设备分配", "C. 启动程序执行", "D. 资源释放"},
            {"A. 有一个运行进程和多个就绪进程", "B. 有一个运行进程和多个阻塞进程", "C. 有多个就绪进程，没有运行进程", "D. 有多个阻塞进程，没有就绪进程"},
            {"A. 死锁一定发生在多个进程之间", "B. 死锁可以通过剥夺资源来解除", "C. 死锁的必要条件之一是互斥", "D. 以上都正确"},
            {"A. 8", "B. 9", "C. 10", "D. 11"},
            {"A. 先来先服务", "B. 时间片轮转", "C. 多级反馈队列", "D. 短作业优先"},
            {"A. 一定减少", "B. 一定增加", "C. 可能增加也可能减少", "D. 保持不变"},
            {"A. 虚拟存储只能基于非连续分配技术", "B. 虚拟存储容量只受外存容量限制", "C. 虚拟存储容量只受内存容量限制", "D. 虚拟存储可以实现逻辑上的大容量存储"}
        },
        // 计算机网络选项
        {
            {"A. 物理层", "B. 数据链路层", "C. 网络层", "D. 传输层"},
            {"A. 0, 0", "B. 0, 1", "C. 1, 0", "D. 1, 1"},
            {"A. TCP 是面向连接的，UDP 是无连接的", "B. TCP 是无连接的，UDP 是面向连接的", "C. TCP 和 UDP 都是面向连接的", "D. TCP 和 UDP 都是无连接的"},
            {"A. 32, 8", "B. 32, 6", "C. 8, 32", "D. 8, 6"},
            {"A. 21", "B. 25", "C. 80", "D. 443"},
            {"A. ARP", "B. RARP", "C. ICMP", "D. DNS"},
            {"A. CSMA/CD 适用于总线型拓扑结构", "B. CSMA/CD 是一种无冲突协议", "C. CSMA/CD 不需要监听信道", "D. CSMA/CD 只能用于局域网"},
            {"A. 172.16.7.128", "B. 172.16.7.192", "C. 172.16.7.0", "D. 172.16.7.64"},
            {"A. 0, 0", "B. 0, 1", "C. 1, 0", "D. 1, 1"},
            {"A. UDP 是无连接的", "B. UDP 不保证可靠传输", "C. UDP 是面向报文的", "D. UDP 提供流量控制"},
            {"A. 物理层", "B. 数据链路层", "C. 网络层", "D. 传输层"}
        }
    };

    // 各科目真实答案
    private static final String[][] REAL_ANSWERS = {
        {"B", "B", "B", "B", "A", "B", "A", "A", "A", "A", "B"},  // 数据结构
        {"B", "B", "B", "C", "B", "B", "C", "A", "A", "B", "B"},  // 计算机组成原理
        {"C", "C", "A", "D", "C", "D", "D", "C", "C", "C", "D"},  // 操作系统
        {"D", "D", "A", "B", "C", "A", "A", "A", "B", "D", "C"}   // 计算机网络
    };

    // 各科目解析
    private static final String[][] REAL_ANALYSES = {
        {
            "解析：元素依次入栈，出队顺序为 b, d, c, f, e, a, g。栈操作过程：a入→b入→b出→c入→d入→d出→c出→e入→f入→f出→e出→a出→g入→g出。最大栈深度为4（a,c,d或a,e,f）。",
            "解析：完全二叉树的性质：若结点总数为n，则叶子结点数为(n+1)/2（向下取整）或n/2（向上取整）。699为奇数，叶子结点数=(699+1)/2=350。",
            "解析：在二叉树中，除了根结点外，每个结点都有且仅有一条边与其父结点相连。n个结点的二叉树有n-1条边，每条边对应父结点的一个度，因此所有结点的度数之和为n-1。",
            "解析：平衡二叉树要求每个结点的左右子树高度差的绝对值不超过1。选项B中的二叉树满足此条件，是平衡二叉树。",
            "解析：在有向图中，每条边都有一个起点和一个终点，因此所有顶点的出度之和等于入度之和，都等于边数。",
            "解析：由后序遍历序列 BDCAFE 可知根结点为F；由中序序列 ABCDEF 可知F的左子树为ABCDE。再由后序BDCA可知左子树的根为A，依次推导可得前序为ABEDCF。",
            "解析：在顺序表中删除第i个元素，需要将第i+1到第n个元素依次向前移动一位，共需移动n-i个元素。",
            "解析：要删除结点A之后的结点B，只需将A的next指针指向B的next，即p.next = p.next.next。",
            "解析：循环队列中，当front=rear时，队列可能为空也可能为满。本题中初始和最终状态相同，无法确定具体元素个数。",
            "解析：由前序ABDECF和中序DBEAFC可构建二叉树，后序遍历结果为DEBFCA。",
            "解析：删除第3个元素后，第4到第10个元素需要向前移动，共7个元素。"
        },
        {
            "解析：字长16位即2字节，主存64KB=65536字节，按字编址则有65536/2=32768个字，寻址范围为0~32767。",
            "解析：Cache是位于CPU和主存之间的高速小容量存储器，目的是提高存储系统的平均存取速度。",
            "解析：指令周期是指CPU从主存取出一条指令并执行这条指令的时间总和。",
            "解析：设二地址指令操作码长度为k位，则有2^k >= 12，且剩余空间需容纳8条一地址指令和16条零地址指令。计算得k=6位。",
            "解析：补码中零的表示是唯一的，即全0。原码和反码中零有两种表示（+0和-0）。",
            "解析：ROM容量4KB，需2K×8位芯片2片；RAM容量60KB，需4K×4位芯片60片（每片4K×4位=2KB）。",
            "解析：程序计数器PC用于存放下一条要执行指令的地址，CPU根据PC的值去主存取指令。",
            "解析：直接控制方式每个微操作命令占用一个位，编码控制方式将多个微操作命令编码，因此直接控制的微指令字长更长。",
            "解析：32根地址线可寻址2^32=4GB的存储空间。",
            "解析：立即寻址方式中，操作数直接包含在指令中，不需要访问内存。",
            "解析：指令字长等于机器字长，32位机器字长对应32位指令字长。"
        },
        {
            "解析：进程切换是操作系统内核的特权操作，只能在内核态发生，不能在用户态发生。",
            "解析：信号量初值为3，表示有3个可用资源。当前值为1，表示已分配2个，因此有2个进程在等待。",
            "解析：进程是资源分配的基本单位，线程是CPU调度的基本单位，同一进程的线程共享进程的资源。",
            "解析：每个线程有独立的线程控制块，但进程控制块是进程级别的，所有线程共享。",
            "解析：启动程序执行会创建新进程，用户登录成功可能创建会话进程，设备分配和资源释放不会创建新进程。",
            "解析：没有执行进程调度程序时，系统中应有一个运行进程。选项D没有就绪进程且有多个阻塞进程是不可能的。",
            "解析：死锁的四个必要条件：互斥、请求与保持、不可剥夺、循环等待。通过剥夺资源可破坏不可剥夺条件来解除死锁。",
            "解析：采用银行家算法，3个进程每个需要4个资源，不会死锁的最少资源数为3×(4-1)+1=10。",
            "解析：多级反馈队列算法综合了时间片轮转和优先级调度，既能保证短任务优先，又不会导致饥饿。",
            "解析：FIFO算法存在Belady异常，即分配的页帧数增加时，缺页次数可能增加也可能减少。",
            "解析：虚拟存储利用外存扩展内存空间，实现逻辑上的大容量存储，其容量受外存和地址空间限制。"
        },
        {
            "解析：传输层负责端到端的可靠传输，TCP协议工作在传输层。",
            "解析：第二次握手时服务器响应客户端的SYN请求，发送SYN=1和ACK=1。",
            "解析：TCP是面向连接的可靠传输协议，UDP是无连接的不可靠传输协议。",
            "解析：子网掩码255.255.255.248表示前29位为网络位，共有2^(32-29)=8个子网，每个子网有2^3-2=6个可分配地址。",
            "解析：HTTP协议默认使用80端口，HTTPS默认使用443端口。",
            "解析：ARP协议实现IP地址到MAC地址的转换，RARP相反。",
            "解析：CSMA/CD是带冲突检测的载波监听多路访问，适用于总线型拓扑的局域网。",
            "解析：/26表示前26位为网络位，131的二进制最后6位为000001，网络地址为172.16.7.128。",
            "解析：第三次握手时客户端确认服务器的SYN，发送ACK=1，SYN=0（已建立连接）。",
            "解析：UDP是无连接、面向报文的协议，不提供可靠传输和流量控制。",
            "解析：路由器工作在网络层，根据IP地址进行数据包转发。"
        }
    };

    @Override
    public void run(String... args) {
        // 创建默认管理员账号
        createDefaultUser();

        // 单选题: 当单选数量为 0 时从 choice JSON 导入（含 2026）
        Long choiceCount = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getType, "单选"));
        if (choiceCount == null || choiceCount == 0) {
            log.info("未检测到单选题，开始从 choice JSON 导入...");
            if (loadQuestionsFromJson()) {
                log.info("单选题导入完成");
            } else {
                log.error("单选题导入失败");
            }
        }

        // 综合应用题: 独立增量导入（不依赖单选数据是否已存在）
        Long essayCount = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getType, "综合应用"));
        if (essayCount == null || essayCount == 0) {
            log.info("未检测到综合应用题，开始从 essay JSON 导入...");
            if (loadEssayQuestionsFromJson()) {
                log.info("综合应用题导入完成");
            } else {
                log.error("综合应用题导入失败");
            }
        }

        // 检查题库是否有数据
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<>());
        if (count != null && count > 0) {
            log.info("题库已有 {} 条数据，跳过初始化", count);
            return;
        }

        log.info("开始生成题库数据...");
        
        questionMapper.delete(null);

        // 从2009-2025年 JSON 文件读取数据（已包含正确答案和解析）
        if (!loadQuestionsFromJson()) {
            log.error("无法从JSON文件加载数据，题库初始化失败");
            return;
        }
        
        log.info("题库数据生成完成");
    }

    private void createDefaultUser() {
        Long uc = userMapper.selectCount(new LambdaQueryWrapper<>());
        if (uc == null || uc == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(PasswordUtil.hash("123456"));
            admin.setNickname("管理员");
            userMapper.insert(admin);
            log.info("默认管理员账号已创建: admin / 123456");
        }
    }

    private void insertAllQuestions() {
        int[] allYears = {2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017,
                          2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025};
        int total = 0;
        for (int year : allYears) {
            for (int qNum = 1; qNum <= 47; qNum++) {
                Question q = new Question();
                q.setExamYear(year);
                String subject = getSubject(qNum);
                q.setSubject(subject);
                q.setType(qNum <= 40 ? "单选" : "综合应用");
                q.setQuestionNumber(qNum);

                // 题目内容
                int page = estimatePage(qNum);
                String pageImg = String.format("/images/pages/exam_%d_p%02d.png", year, page);
                
                // 使用真实题目内容
                int subjectIdx = getSubjectIndex(subject);
                int realQIdx = getRealQuestionIndex(qNum);
                if (realQIdx >= 0 && realQIdx < REAL_QUESTIONS[subjectIdx].length) {
                    q.setContent(REAL_QUESTIONS[subjectIdx][realQIdx] + "\n<!-- page_img:" + pageImg + " -->");
                } else {
                    q.setContent(year + "年第" + qNum + "题 — " + subject + "（请查看原题图片）\n<!-- page_img:" + pageImg + " -->");
                }

                // 选择题：使用真实选项
                if (qNum <= 40) {
                    String[][] realOpts = REAL_OPTIONS[subjectIdx];
                    if (realQIdx >= 0 && realQIdx < realOpts.length) {
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < 4; i++) {
                            if (i > 0) sb.append(",");
                            String opt = realOpts[realQIdx][i];
                            char key = opt.charAt(0);
                            String text = opt.substring(3);
                            sb.append("{\"key\":\"").append(key).append("\",\"text\":\"").append(text).append("\"}");
                        }
                        sb.append("]");
                        q.setOptions(sb.toString());
                        
                        // 使用真实答案
                        if (realQIdx < REAL_ANSWERS[subjectIdx].length) {
                            q.setAnswer(REAL_ANSWERS[subjectIdx][realQIdx]);
                        } else {
                            q.setAnswer(String.valueOf((char)('A' + (qNum + year) % 4)));
                        }
                        
                        // 使用真实解析
                        if (realQIdx < REAL_ANALYSES[subjectIdx].length) {
                            q.setAnalysis(REAL_ANALYSES[subjectIdx][realQIdx]);
                        } else {
                            q.setAnalysis("查看原题图片获取完整解析");
                        }
                    } else {
                        // 备用选项生成
                        String[] opts = new String[]{"A选项内容", "B选项内容", "C选项内容", "D选项内容"};
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < 4; i++) {
                            if (i > 0) sb.append(",");
                            sb.append("{\"key\":\"").append((char)('A'+i)).append("\",\"text\":\"")
                              .append(opts[i]).append("\"}");
                        }
                        sb.append("]");
                        q.setOptions(sb.toString());
                        q.setAnswer(String.valueOf((char)('A' + (qNum + year) % 4)));
                        q.setAnalysis("查看原题图片获取完整解析");
                    }
                } else {
                    q.setAnswer("（请在分屏模式中查看参考答案）");
                    q.setAnalysis("查看原题图片获取完整解析");
                }

                q.setKnowledgeTag(subject + getSubjectTags(qNum, subject));
                questionMapper.insert(q);
                total++;
            }
        }
        log.info("已生成 {} 道题目（17年 x 47题）", total);
    }

    private int getRealQuestionIndex(int qNum) {
        // 获取题目在真实题库中的索引（1-11对应各科目）
        if (qNum >= 1 && qNum <= 11) return qNum - 1;      // 数据结构
        if (qNum >= 12 && qNum <= 22) return qNum - 12;    // 计算机组成原理
        if (qNum >= 23 && qNum <= 32) return qNum - 23;    // 操作系统
        if (qNum >= 33 && qNum <= 40) return qNum - 33;    // 计算机网络
        return -1;
    }

    private String getSubject(int qNum) {
        if (qNum <= 11 || qNum == 41 || qNum == 42) return "数据结构";
        if (qNum <= 22 || qNum == 43 || qNum == 44) return "计算机组成原理";
        if (qNum <= 32 || qNum == 45 || qNum == 46) return "操作系统";
        return "计算机网络";
    }

    private int getSubjectIndex(String subject) {
        if (subject.contains("数据")) return 0;
        if (subject.contains("组成")) return 1;
        if (subject.contains("操作")) return 2;
        return 3;
    }

    private String getSubjectTags(int qNum, String subject) {
        if (subject.contains("数据")) {
            String[] tags = {"线性表","栈和队列","树与二叉树","图","查找","排序" };
            return " — " + tags[(qNum - 1) % 6];
        }
        if (subject.contains("组成")) {
            String[] tags = {"数据表示","存储系统","指令系统","CPU","总线","I/O"};
            return " — " + tags[(qNum - 12) % 6];
        }
        if (subject.contains("操作")) {
            String[] tags = {"进程管理","内存管理","文件系统","I/O管理","死锁","调度"};
            return " — " + tags[(qNum - 23) % 6];
        }
        String[] tags = {"体系结构","物理层","链路层","网络层","传输层","应用层"};
        return " — " + tags[(qNum - 33) % 6];
    }

    private int estimatePage(int qNum) {
        if (qNum <= 8) return 1;
        if (qNum <= 16) return 2;
        if (qNum <= 24) return 3;
        if (qNum <= 32) return 4;
        if (qNum <= 40) return 5;
        if (qNum <= 43) return 6;
        return 7;
    }

    /** 从answer_data.json注入答案和解析到数据库 */
    private void injectAnswers() {
        try {
            File f = new File("answer_data.json");
            if (!f.exists()) { log.warn("answer_data.json 不存在，跳过答案注入"); return; }
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Map<String, Map<String, String>>> data = mapper.readValue(f,
                new TypeReference<Map<String, Map<String, Map<String, String>>>>() {});
            int updated = 0;
            for (Map.Entry<String, Map<String, Map<String, String>>> ye : data.entrySet()) {
                int year = Integer.parseInt(ye.getKey());
                for (Map.Entry<String, Map<String, String>> qe : ye.getValue().entrySet()) {
                    int qNum = Integer.parseInt(qe.getKey());
                    String answer = qe.getValue().get("answer");
                    String analysis = qe.getValue().get("analysis");
                    if (answer != null && !answer.isEmpty()) {
                        questionMapper.update(null, new LambdaUpdateWrapper<Question>()
                            .eq(Question::getExamYear, year)
                            .eq(Question::getQuestionNumber, qNum)
                            .set(Question::getAnswer, answer)
                            .set(Question::getAnalysis, analysis != null ? analysis : "")
                        );
                        updated++;
                    }
                }
            }
            log.info("已注入 {} 道题目的真实答案和解析", updated);
        } catch (Exception e) {
            log.error("答案注入失败: {}", e.getMessage());
        }
    }

    /** 打开题目 JSON 数据流：优先从 classpath（jar 内置），失败时回退到旧版工程绝对路径 */
    private InputStream openQuestionJson(String kind, int year) throws Exception {
        ClassPathResource resource = new ClassPathResource("static/" + kind + "/" + year + ".json");
        if (resource.exists()) {
            return resource.getInputStream();
        }
        File legacy = new File("c:/projects/408真题训练系统/src/main/resources/static/" + kind + "/" + year + ".json");
        if (legacy.exists()) {
            return new FileInputStream(legacy);
        }
        return null;
    }

    private boolean loadEssayQuestionsFromJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            int total = 0;

            for (int year = 2009; year <= 2026; year++) {
                InputStream in = openQuestionJson("essay", year);
                if (in == null) {
                    log.warn("{} 不存在，跳过", year + ".json");
                    continue;
                }

                List<Map<String, Object>> data;
                try (InputStream is = in) {
                    data = mapper.readValue(is,
                        new TypeReference<List<Map<String, Object>>>() {});
                }

                for (Map<String, Object> item : data) {
                    Question q = new Question();
                    q.setExamYear(year);

                    Integer qNum = (Integer) item.get("题号");
                    if (qNum == null) qNum = (Integer) item.get("question_number");
                    q.setQuestionNumber(qNum != null ? qNum : 0);

                    String subject = (String) item.get("科目");
                    q.setSubject(subject != null && !subject.isEmpty() ? subject : determineSubject(qNum != null ? qNum : 1));

                    q.setType("综合应用");

                    String content = (String) item.get("内容");
                    q.setContent(content != null ? content : "");

                    q.setOptions("[]");

                    String answer = (String) item.get("答案");
                    q.setAnswer(answer != null ? answer.trim() : "");

                    String analysis = (String) item.get("解析");
                    q.setAnalysis(analysis != null ? analysis : "");

                    Object tagsObj = item.get("标签");
                    String knowledgeTagValue = "";
                    if (tagsObj instanceof List) {
                        List<String> tList = (List<String>) tagsObj;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < tList.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append(tList.get(i));
                        }
                        knowledgeTagValue = sb.toString();
                    } else if (tagsObj instanceof String) {
                        knowledgeTagValue = (String) tagsObj;
                    }

                    Object chapterObj = item.get("章节");
                    String chapterValue = "";
                    if (chapterObj instanceof List) {
                        List<String> chList = (List<String>) chapterObj;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < chList.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append(chList.get(i));
                        }
                        chapterValue = sb.toString();
                    } else if (chapterObj instanceof String) {
                        chapterValue = (String) chapterObj;
                    }

                    q.setKnowledgeTag(q.getSubject()
                        + (chapterValue.isEmpty() ? "" : " — " + chapterValue)
                        + (knowledgeTagValue.isEmpty() ? "" : " — " + knowledgeTagValue));
                    q.setKnowledgeTags(knowledgeTagValue);

                    questionMapper.insert(q);
                    total++;
                }
                log.info("已加载综合应用题 {} ({}道)", year + ".json", data.size());
            }

            log.info("共从 essay JSON 加载 {} 道综合应用题", total);
            return total > 0;
        } catch (Exception e) {
            log.error("从 essay JSON 加载综合应用题失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean loadQuestionsFromJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            int total = 0;

            // 遍历 2009-2026 所有年份
            for (int year = 2009; year <= 2026; year++) {
                InputStream in = openQuestionJson("choice", year);
                if (in == null) {
                    log.warn("{} 不存在，跳过", year + ".json");
                    continue;
                }

                List<Map<String, Object>> data;
                try (InputStream is = in) {
                    data = mapper.readValue(is,
                        new TypeReference<List<Map<String, Object>>>() {});
                }

                for (Map<String, Object> item : data) {
                    Question q = new Question();

                    // 年份
                    q.setExamYear(year);

                    // 题号
                    Integer qNum = (Integer) item.get("题号");
                    if (qNum == null) {
                        qNum = (Integer) item.get("question_number");
                    }
                    if (qNum != null) q.setQuestionNumber(qNum);

                    // 科目：优先从 JSON 读取
                    String subjectField = (String) item.get("科目");
                    if (subjectField != null && !subjectField.isEmpty()) {
                        q.setSubject(subjectField);
                    } else {
                        q.setSubject(determineSubject(qNum != null ? qNum : 1));
                    }

                    // 类型 (所有数据都是单选题，1-40题)
                    q.setType("单选");

                    // 内容
                    String content = (String) item.get("内容");
                    q.setContent(content != null ? content : "");

                    // 选项: 把 ["A.xxx", "B.xxx", ...] 转换为 [{key, text}, ...] 的 JSON 字符串
                    Object optionsObj = item.get("选项");
                    if (optionsObj instanceof List) {
                        List<String> optionsList = (List<String>) optionsObj;
                        List<Map<String, String>> optMapList = new java.util.ArrayList<>();
                        for (int i = 0; i < optionsList.size(); i++) {
                            String opt = optionsList.get(i);
                            char key = opt.charAt(0);
                            String text2 = opt.length() > 2 ? opt.substring(2).trim() : "";
                            Map<String, String> m = new java.util.HashMap<>();
                            m.put("key", String.valueOf(key));
                            m.put("text", text2);
                            optMapList.add(m);
                        }
                        q.setOptions(mapper.writeValueAsString(optMapList));
                    }

                    // 答案
                    String answer = (String) item.get("答案");
                    if (answer != null && !answer.isEmpty()) {
                        q.setAnswer(answer.trim());
                    } else {
                        q.setAnswer("");
                    }

                    // 解析
                    String analysis = (String) item.get("解析");
                    q.setAnalysis(analysis != null ? analysis : "");

                    // 知识点标签：优先从 JSON 读取
                    Object tagsObj = item.get("标签");
                    String knowledgeTagValue;
                    if (tagsObj instanceof List) {
                        List<String> tList = (List<String>) tagsObj;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < tList.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append(tList.get(i));
                        }
                        knowledgeTagValue = sb.toString();
                    } else if (tagsObj instanceof String) {
                        knowledgeTagValue = (String) tagsObj;
                    } else {
                        knowledgeTagValue = "";
                    }

                    // 章节
                    Object chapterObj = item.get("章节");
                    String chapterValue = "";
                    if (chapterObj instanceof List) {
                        List<String> chList = (List<String>) chapterObj;
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < chList.size(); i++) {
                            if (i > 0) sb.append(",");
                            sb.append(chList.get(i));
                        }
                        chapterValue = sb.toString();
                    } else if (chapterObj instanceof String) {
                        chapterValue = (String) chapterObj;
                    }

                    // 设置 knowledgeTag：科目 — 章节 — 标签
                    String composite = q.getSubject()
                        + (chapterValue.isEmpty() ? "" : " — " + chapterValue)
                        + (knowledgeTagValue.isEmpty() ? "" : " — " + knowledgeTagValue);
                    q.setKnowledgeTag(composite);
                    q.setKnowledgeTags(knowledgeTagValue);

                    questionMapper.insert(q);
                    total++;
                }
                log.info("已加载 {} ({}道)", year + ".json", data.size());
            }

            log.info("共从 JSON 文件加载 {} 道题目", total);
            return total > 0;
        } catch (Exception e) {
            log.error("从 JSON 文件加载数据失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private String determineSubject(int qNum) {
        if (qNum <= 11 || qNum == 41 || qNum == 42) return "数据结构";
        if (qNum <= 22 || qNum == 43 || qNum == 44) return "计算机组成原理";
        if (qNum <= 32 || qNum == 45 || qNum == 46) return "操作系统";
        return "计算机网络";
    }
}
