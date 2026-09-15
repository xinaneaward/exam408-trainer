package com.exam408.ai;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiPropertiesLimitTest {

    @Test
    void fallbackToDefaultLimitWhenFeatureMissing() {
        AiProperties props = new AiProperties();
        props.setLimits(new HashMap<>());
        assertEquals(30, props.getDailyLimit("chat"));
        assertEquals(30, props.getDailyLimit("unknown"));
    }

    @Test
    void limitIsPerFeature() {
        AiProperties props = new AiProperties();
        Map<String, Integer> limits = new HashMap<>();
        limits.put("chat", 30);
        limits.put("variant", 10);
        limits.put("diagnosis", 5);
        props.setLimits(limits);
        assertEquals(30, props.getDailyLimit("chat"));
        assertEquals(10, props.getDailyLimit("variant"));
        assertEquals(5, props.getDailyLimit("diagnosis"));
        assertEquals(30, props.getDailyLimit("assistant"));
    }
}