package com.exam408.controller;

import com.exam408.dto.ApiResponse;
import com.exam408.dto.OverviewVO;
import com.exam408.entity.User;
import com.exam408.service.OverviewService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/overview")
public class OverviewController {

    @Resource
    private OverviewService overviewService;

    @GetMapping
    public ApiResponse<OverviewVO> getOverview(
            @RequestParam(required = false) Integer year,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        Long userId = user != null ? user.getId() : null;
        return ApiResponse.success(overviewService.getOverview(userId, year));
    }
}
