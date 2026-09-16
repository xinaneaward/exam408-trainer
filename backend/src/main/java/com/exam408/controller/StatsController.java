package com.exam408.controller;

import com.exam408.dto.ApiResponse;
import com.exam408.dto.DashboardVO;
import com.exam408.dto.StatsVO;
import com.exam408.entity.User;
import com.exam408.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Resource
    private StatsService statsService;

    @GetMapping("/overview")
    public ApiResponse<StatsVO> overview(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(statsService.getUserStats(user.getId()));
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardVO> dashboard(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(statsService.getDashboard(user.getId()));
    }

    @GetMapping("/heatmap")
    public ApiResponse<Map<String, Object>> heatmap(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(statsService.getHeatmap());
    }

    @GetMapping("/mastery")
    public ApiResponse<Map<String, Object>> mastery(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(statsService.getMastery(user.getId()));
    }

    @GetMapping("/calendar")
    public ApiResponse<Map<String, Object>> calendar(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(statsService.getCalendar(user.getId()));
    }

    @GetMapping("/countdown")
    public ApiResponse<Map<String, Object>> countdown(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(statsService.getCountdown());
    }

    @GetMapping("/monthly-report")
    public ApiResponse<Map<String, Object>> monthlyReport(
            @RequestParam(required = false) String month,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(statsService.getMonthlyReport(user.getId(), month));
    }
}
