package com.example.hospital.patient.wx.api.agent.react.memory;

import java.util.Map;

public interface TraditionalAgentMemoryService {
    Map<String, Object> load(String sessionId);

    void save(String sessionId, Map<String, Object> memory);

    void clear(String sessionId);
}
