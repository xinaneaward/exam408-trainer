package com.exam408.controller;

import com.exam408.dto.ApiResponse;
import com.exam408.dto.KnowledgeTreeVO;
import com.exam408.entity.User;
import com.exam408.service.KnowledgeTreeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/knowledge-tree")
public class KnowledgeTreeController {

    @Resource
    private KnowledgeTreeService knowledgeTreeService;

    @GetMapping
    public ApiResponse<KnowledgeTreeVO> getTree(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Long userId = user != null ? user.getId() : 0L;
        return ApiResponse.success(knowledgeTreeService.getTree(userId));
    }
}
