package com.example.hospital.patient.wx.api.agent.multi.service;

import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MultiAgentRequestGuardServiceTest {

    @Test
    void shouldBlockTooFrequentRequests() {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setGuardMinIntervalMillis(10_000L);
        properties.setGuardRequestLimitPerMinute(10);
        MultiAgentRequestGuardService service = new MultiAgentRequestGuardService(properties);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");

        service.guard(request, null);
        Assertions.assertThrows(HospitalException.class, () -> service.guard(request, null));
    }

    @Test
    void shouldBlockWhenPerMinuteLimitExceeded() {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setGuardMinIntervalMillis(0L);
        properties.setGuardRequestLimitPerMinute(2);
        MultiAgentRequestGuardService service = new MultiAgentRequestGuardService(properties);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-2");

        service.guard(request, null);
        service.guard(request, null);
        Assertions.assertThrows(HospitalException.class, () -> service.guard(request, null));
    }
}
