package com.example.hospital.patient.wx.api.agent.memory;

import java.util.Map;

public interface AgentConversationMemoryService {
    Map<String, Object> load(String sessionId);

    void save(String sessionId, Map<String, Object> memory);

    void clear(String sessionId);
}
