package com.exam408.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam408.dto.ApiResponse;
import com.exam408.entity.Question;
import com.exam408.entity.User;
import com.exam408.entity.UserAnswer;
import com.exam408.entity.WrongQuestion;
import com.exam408.mapper.UserAnswerMapper;
import com.exam408.service.WrongQuestionService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wrong")
public class WrongQuestionController {

    @Resource
    private WrongQuestionService wrongQuestionService;
    @Resource
    private UserAnswerMapper userAnswerMapper;

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String tag,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");

        List<WrongQuestion> wqList;
        if (tag != null) {
            wqList = wrongQuestionService.listByUserAndTag(user.getId(), tag);
        } else if (subject != null) {
            wqList = wrongQuestionService.listByUserAndSubject(user.getId(), subject);
        } else {
            wqList = wrongQuestionService.listByUser(user.getId());
        }

        // 待复习（到期且未掌握）优先，其余按最近出错时间降序
        LocalDateTime now = LocalDateTime.now();
        wqList.sort((a, b) -> {
            boolean aDue = isDue(a, now);
            boolean bDue = isDue(b, now);
            if (aDue != bDue) return aDue ? -1 : 1;
            return compareNullable(b.getLastWrongAt(), a.getLastWrongAt());
        });

        List<Map<String, Object>> result = wqList.stream().map(wq -> {
            Map<String, Object> map = new LinkedHashMap<>();
            Question q = wrongQuestionService.getQuestionById(wq.getQuestionId());
            map.put("wrongId", wq.getId());
            map.put("questionId", wq.getQuestionId());
            map.put("wrongCount", wq.getWrongCount());
            map.put("lastWrongAt", wq.getLastWrongAt());
            map.put("isReviewed", wq.getIsReviewed());
            map.put("reviewStage", wq.getReviewStage());
            map.put("nextReviewAt", wq.getNextReviewAt());
            map.put("reason", wq.getReason());
            map.put("isDue", !wq.getIsReviewed() && (wq.getNextReviewAt() == null || !wq.getNextReviewAt().isAfter(now)));
            if (q != null) {
                map.put("content", q.getContent());
                map.put("answer", q.getAnswer());
                map.put("analysis", q.getAnalysis());
                map.put("subject", q.getSubject());
                map.put("type", q.getType());
                map.put("year", q.getExamYear());
                map.put("knowledgeTag", q.getKnowledgeTag());
                map.put("options", q.getOptions());
            }
            // 最近一次的作答记录，供AI个性化讲解使用
            LambdaQueryWrapper<UserAnswer> uaW = new LambdaQueryWrapper<>();
            uaW.eq(UserAnswer::getUserId, user.getId())
               .eq(UserAnswer::getQuestionId, wq.getQuestionId())
               .orderByDesc(UserAnswer::getId)
               .last("LIMIT 1");
            UserAnswer ua = userAnswerMapper.selectOne(uaW);
            map.put("userAnswer", ua != null ? ua.getUserAnswer() : null);
            return map;
        }).collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    /**
     * 生成一张"错题重练"试卷：覆盖用户全部错题，
     * 按错题次数加权排序（做错次数越多越靠前），结合科目筛选。
     */
    @GetMapping("/paper")
    public ApiResponse<List<Map<String, Object>>> paper(
            @RequestParam(required = false) String subject,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");

        List<WrongQuestion> wqList = subject == null || subject.trim().isEmpty()
                ? wrongQuestionService.listByUser(user.getId())
                : wrongQuestionService.listByUserAndSubject(user.getId(), subject);

        // 按错次降序（再犯加权），再按最近出错时间降序
        wqList.sort((a, b) -> {
            int c = Integer.compare(
                    b.getWrongCount() == null ? 0 : b.getWrongCount(),
                    a.getWrongCount() == null ? 0 : a.getWrongCount());
            if (c != 0) return c;
            return compareNullable(b.getLastWrongAt(), a.getLastWrongAt());
        });

        List<Map<String, Object>> result = new ArrayList<>();
        for (WrongQuestion wq : wqList) {
            Question q = wrongQuestionService.getQuestionById(wq.getQuestionId());
            if (q == null) continue;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", q.getId());
            map.put("examYear", q.getExamYear());
            map.put("subject", q.getSubject());
            map.put("type", q.getType());
            map.put("questionNumber", q.getQuestionNumber());
            map.put("content", q.getContent());
            map.put("options", q.getOptions());
            map.put("answer", q.getAnswer());
            map.put("analysis", q.getAnalysis());
            map.put("knowledgeTag", q.getKnowledgeTag());
            map.put("wrongId", wq.getId());
            map.put("wrongCount", wq.getWrongCount());
            result.add(map);
        }
        return ApiResponse.success(result);
    }

    private int compareNullable(java.time.LocalDateTime a, java.time.LocalDateTime b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    private boolean isDue(WrongQuestion wq, LocalDateTime now) {
        return !wq.getIsReviewed() && (wq.getNextReviewAt() == null || !wq.getNextReviewAt().isAfter(now));
    }

    @DeleteMapping("/{wrongId}")
    public ApiResponse<Void> remove(@PathVariable Long wrongId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        wrongQuestionService.removeWrongQuestion(user.getId(), wrongId);
        return ApiResponse.success();
    }

    @PutMapping("/{wrongId}/review")
    public ApiResponse<Void> markReviewed(@PathVariable Long wrongId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        wrongQuestionService.markReviewed(user.getId(), wrongId);
        return ApiResponse.success();
    }

    /** 错题重练单题结果：做对推进间隔重复阶段，做错重置 */
    @PostMapping("/{wrongId}/review-result")
    public ApiResponse<Void> reviewResult(@PathVariable Long wrongId,
                                          @RequestBody(required = false) Map<String, Object> body,
                                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        boolean correct = body != null && Boolean.TRUE.equals(body.get("correct"));
        wrongQuestionService.recordReviewResult(user.getId(), wrongId, correct);
        return ApiResponse.success();
    }

    @GetMapping("/count")
    public ApiResponse<Map<String, Object>> count(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        Map<String, Object> result = new LinkedHashMap<>();
        Integer total = wrongQuestionService.countByUser(user.getId());
        Map<String, Integer> bySubject = wrongQuestionService.countBySubject(user.getId());
        result.put("total", total);
        result.put("bySubject", bySubject);
        return ApiResponse.success(result);
    }

    /** 为错题打原因标签：概念不清/粗心/计算错/审题不清/其他 */
    @PutMapping("/{wrongId}/reason")
    public ApiResponse<Void> setReason(@PathVariable Long wrongId,
                                       @RequestBody(required = false) Map<String, Object> body,
                                       HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        String reason = body != null ? String.valueOf(body.get("reason")) : null;
        wrongQuestionService.setReason(user.getId(), wrongId, "null".equalsIgnoreCase(reason) ? null : reason);
        return ApiResponse.success();
    }

    /** 错题原因分布统计 */
    @GetMapping("/reason-stats")
    public ApiResponse<Map<String, Object>> reasonStats(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(wrongQuestionService.countByReason(user.getId()));
    }

    /** 今日待复习错题：到期（next_review_at 为空或已到）且未掌握 */
    @GetMapping("/review-today")
    public ApiResponse<Map<String, Object>> reviewToday(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");

        List<WrongQuestion> due = wrongQuestionService.listDueToday(user.getId());
        List<Map<String, Object>> items = new ArrayList<>();
        for (WrongQuestion wq : due) {
            Question q = wrongQuestionService.getQuestionById(wq.getQuestionId());
            if (q == null) continue;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("wrongId", wq.getId());
            map.put("questionId", wq.getQuestionId());
            map.put("subject", q.getSubject());
            map.put("type", q.getType());
            map.put("year", q.getExamYear());
            map.put("knowledgeTag", q.getKnowledgeTag());
            map.put("chapter", extractChapter(q.getKnowledgeTag()));
            map.put("wrongCount", wq.getWrongCount());
            map.put("reviewStage", wq.getReviewStage());
            map.put("nextReviewAt", wq.getNextReviewAt());
            map.put("reason", wq.getReason());
            items.add(map);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("items", items);
        return ApiResponse.success(result);
    }

    /** 从知识点标签中提取章节：形如 “计算机组成原理 — 存储系统” */
    private String extractChapter(String knowledgeTag) {
        if (knowledgeTag == null || knowledgeTag.trim().isEmpty()) return "未分类";
        String tag = knowledgeTag.trim();
        String[] parts = tag.split("[—–-]");
        if (parts.length >= 2 && !parts[1].trim().isEmpty()) {
            return parts[1].trim();
        }
        return tag;
    }
}
