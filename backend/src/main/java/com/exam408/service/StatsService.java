package com.exam408.service;

import com.exam408.dto.DashboardVO;
import com.exam408.dto.StatsVO;

public interface StatsService {
    StatsVO getUserStats(Long userId);
    DashboardVO getDashboard(Long userId);
}
