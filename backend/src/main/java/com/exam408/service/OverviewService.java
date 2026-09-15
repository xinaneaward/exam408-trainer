package com.exam408.service;

import com.exam408.dto.OverviewVO;

public interface OverviewService {
    OverviewVO getOverview(Long userId, Integer year);
}
