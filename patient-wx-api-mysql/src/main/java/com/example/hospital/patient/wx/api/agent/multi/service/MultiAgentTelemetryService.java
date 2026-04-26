package com.example.hospital.patient.wx.api.agent.multi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MultiAgentTelemetryService {
    private final Map<String, Long> localCounter = new ConcurrentHashMap<String, Long>();

    @Autowired(required = false)
    private RedisTemplate<Object, Object> redisTemplate;

    public void recordChat(String sessionId, Map<String, Object> metrics) {
        increment("chat_total");
        if (metrics != null && StringUtils.hasText(stringValue(metrics.get("finalState")))) {
            increment("chat_state:" + metrics.get("finalState"));
        }
        if (metrics != null && StringUtils.hasText(stringValue(metrics.get("errorCode")))) {
            increment("chat_error:" + metrics.get("errorCode"));
        }
        if (metrics != null && StringUtils.hasText(stringValue(metrics.get("badCaseType")))) {
            increment("chat_bad_case:" + metrics.get("badCaseType"));
        }
        if (metrics != null && metrics.get("retryable") != null) {
            increment("chat_retryable:" + metrics.get("retryable"));
        }
        if (metrics != null && StringUtils.hasText(stringValue(metrics.get("replayDecision")))) {
            increment("chat_replay:" + metrics.get("replayDecision"));
        }
        log.info("multi-agent chat metrics, sessionId={}, metrics={}", sessionId, metrics);
    }

    public void recordGuardBlocked(String sessionId, String reason) {
        increment("guard_blocked");
        log.warn("multi-agent guard blocked, sessionId={}, reason={}", sessionId, reason);
    }

    private void increment(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        if (redisTemplate != null) {
            String redisKey = "multi_agent_metrics:" + key;
            redisTemplate.opsForValue().increment(redisKey, 1);
            redisTemplate.expire(redisKey, 1, TimeUnit.DAYS);
            return;
        }
        localCounter.put(key, localCounter.containsKey(key) ? localCounter.get(key) + 1L : 1L);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
