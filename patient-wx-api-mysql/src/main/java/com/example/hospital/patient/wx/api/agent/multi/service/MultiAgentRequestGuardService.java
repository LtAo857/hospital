package com.example.hospital.patient.wx.api.agent.multi.service;

import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class MultiAgentRequestGuardService {
    private final Map<String, Long> localLastRequest = new ConcurrentHashMap<String, Long>();
    private final Map<String, Integer> localMinuteCounter = new ConcurrentHashMap<String, Integer>();

    @Resource
    private MultiAgentProperties properties;

    @Autowired(required = false)
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired(required = false)
    private MultiAgentTelemetryService telemetryService;

    public MultiAgentRequestGuardService() {
        this.properties = new MultiAgentProperties();
    }

    public MultiAgentRequestGuardService(MultiAgentProperties properties) {
        this.properties = properties == null ? new MultiAgentProperties() : properties;
    }

    public void guard(AgentChatRequest request, Integer userId) {
        String sessionId = request == null ? null : request.getSessionId();
        String guardKey = buildGuardKey(sessionId, userId);
        long now = System.currentTimeMillis();
        if (isTooFrequent(guardKey, now)) {
            recordBlocked(sessionId, "min_interval");
            throw new HospitalException("请求过于频繁，请稍后再试。");
        }
        if (isOverLimit(guardKey, now)) {
            recordBlocked(sessionId, "rate_limit");
            throw new HospitalException("当前请求过于频繁，请稍后再试。");
        }
    }

    private boolean isTooFrequent(String guardKey, long now) {
        long minInterval = properties == null ? 1000L : properties.getGuardMinIntervalMillis();
        if (redisTemplate != null) {
            String key = "multi_agent_guard:last:" + guardKey;
            Object last = redisTemplate.opsForValue().get(key);
            redisTemplate.opsForValue().set(key, String.valueOf(now), 5, TimeUnit.MINUTES);
            return last != null && now - Long.parseLong(String.valueOf(last)) < minInterval;
        }
        Long last = localLastRequest.get(guardKey);
        localLastRequest.put(guardKey, now);
        return last != null && now - last < minInterval;
    }

    private boolean isOverLimit(String guardKey, long now) {
        int limit = properties == null ? 12 : properties.getGuardRequestLimitPerMinute();
        String minuteKey = guardKey + ":" + (now / 60000L);
        if (redisTemplate != null) {
            String key = "multi_agent_guard:minute:" + minuteKey;
            Long count = redisTemplate.opsForValue().increment(key, 1);
            redisTemplate.expire(key, 2, TimeUnit.MINUTES);
            return count != null && count > limit;
        }
        Integer count = localMinuteCounter.get(minuteKey);
        int next = count == null ? 1 : count + 1;
        localMinuteCounter.put(minuteKey, next);
        return next > limit;
    }

    private String buildGuardKey(String sessionId, Integer userId) {
        if (userId != null) {
            return "user:" + userId;
        }
        return "session:" + (StringUtils.hasText(sessionId) ? sessionId : "anonymous");
    }

    private void recordBlocked(String sessionId, String reason) {
        if (telemetryService != null) {
            telemetryService.recordGuardBlocked(sessionId, reason);
        }
    }
}
