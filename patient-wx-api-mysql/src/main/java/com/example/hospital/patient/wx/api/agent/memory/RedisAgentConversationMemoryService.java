package com.example.hospital.patient.wx.api.agent.memory;

import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisAgentConversationMemoryService implements AgentConversationMemoryService {
    private static final String KEY_PREFIX = "agent_session:";

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private AgentProperties agentProperties;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> load(String sessionId) {
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
        if (value instanceof Map) {
            return new HashMap<>((Map<String, Object>) value);
        }
        return new HashMap<>();
    }

    @Override
    public void save(String sessionId, Map<String, Object> memory) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + sessionId,
                new HashMap<>(memory),
                agentProperties.getSessionTtlMinutes(),
                TimeUnit.MINUTES
        );
    }

    @Override
    public void clear(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }
}
