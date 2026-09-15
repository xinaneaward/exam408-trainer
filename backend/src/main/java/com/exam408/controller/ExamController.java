package com.exam408.controller;

import com.exam408.dto.ApiResponse;
import com.exam408.dto.ExamResultVO;
import com.exam408.dto.SubmitAnswerRequest;
import com.exam408.entity.ExamRecord;
import com.exam408.entity.User;
import com.exam408.service.ExamService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
@RequestMapping("/api/exam")
public class ExamController {

    @Resource
    private ExamService examService;

    @PostMapping("/submit")
    public ApiResponse<ExamResultVO> submit(@RequestBody SubmitAnswerRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        try {
            ExamResultVO result = examService.submitAnswers(user.getId(), request);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ApiResponse<List<ExamRecord>> history(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(examService.getHistory(user.getId()));
    }

    @GetMapping("/detail/{recordId}")
    public ApiResponse<ExamResultVO> detail(@PathVariable Long recordId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        try {
            return ApiResponse.success(examService.getExamDetail(user.getId(), recordId));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/save-answer")
    public ApiResponse<Void> saveAnswer(@RequestBody SubmitAnswerRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        try {
            examService.saveSingleAnswer(user.getId(), request);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
