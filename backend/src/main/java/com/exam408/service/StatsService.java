package com.exam408.service;

import com.exam408.dto.DashboardVO;
import com.exam408.dto.StatsVO;

import java.util.Map;

public interface StatsService {
    StatsVO getUserStats(Long userId);
    DashboardVO getDashboard(Long userId);
    /** 考点(章节)×年份 出题热力图数据 */
    Map<String, Object> getHeatmap();
    /** 各科掌握度(雷达)与覆盖率 */
    Map<String, Object> getMastery(Long userId);
    /** 复习打卡日历：最近90天每日作答数 + 连续天数 */
    Map<String, Object> getCalendar(Long userId);
    /** 考试倒计时与备考阶段 */
    Map<String, Object> getCountdown();
    /** 月学习报告聚合数据 */
    Map<String, Object> getMonthlyReport(Long userId, String month);
}