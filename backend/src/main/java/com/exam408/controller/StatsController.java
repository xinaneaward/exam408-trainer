package com.exam408.controller;

import com.exam408.dto.ApiResponse;
import com.exam408.dto.DashboardVO;
import com.exam408.dto.StatsVO;
import com.exam408.entity.User;
import com.exam408.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

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
}
