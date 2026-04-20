package com.example.hospital.patient.wx.api.agent.service;

import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentModelDecision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class DashScopeAgentServiceTest {

    @Test
    void shouldRetryAndExtractUsage() {
        AgentProperties properties = new AgentProperties();
        properties.setLlmEnabled(true);
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://test.local");
        properties.setHttpRetryCount(1);
        properties.setMaxPromptChars(800);
        DashScopeAgentService service = new DashScopeAgentService(properties) {
            private final AtomicInteger count = new AtomicInteger();

            @Override
            protected HttpCallResult executeChatCompletion(cn.hutool.json.JSONObject body, AgentProperties current) {
                if (count.getAndIncrement() == 0) {
                    return new HttpCallResult(429, "{\"error\":\"rate_limited\"}");
                }
                return new HttpCallResult(200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"reply\\\":\\\"我先帮你看明天骨科。\\\",\\\"action\\\":\\\"select_doctor\\\",\\\"reason\\\":\\\"date and dept provided\\\"}\"}}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":8,\"total_tokens\":20}}");
            }
        };
        AgentChatRequest request = new AgentChatRequest();
        request.setMessage("明天骨科");
        Map<String, Object> memory = new HashMap<String, Object>();
        memory.put("stage", "choose_date");
        memory.put("deptName", "骨科");

        AgentModelDecision decision = service.decide(request, memory);

        Assertions.assertNotNull(decision);
        Assertions.assertEquals("select_doctor", decision.getAction());
        Assertions.assertEquals("我先帮你看明天骨科。", decision.getReply());
        Assertions.assertEquals(Integer.valueOf(12), decision.getPromptTokens());
        Assertions.assertEquals(Integer.valueOf(8), decision.getCompletionTokens());
        Assertions.assertEquals(Integer.valueOf(20), decision.getTotalTokens());
        Assertions.assertEquals(Integer.valueOf(1), decision.getRetryCount());
        Assertions.assertEquals(Boolean.FALSE, decision.getDegraded());
        Assertions.assertTrue(decision.getLatencyMs() >= 0);
    }

    @Test
    void shouldDegradeWhenHttpStillFails() {
        AgentProperties properties = new AgentProperties();
        properties.setLlmEnabled(true);
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://test.local");
        properties.setHttpRetryCount(1);
        DashScopeAgentService service = new DashScopeAgentService(properties) {
            @Override
            protected HttpCallResult executeChatCompletion(cn.hutool.json.JSONObject body, AgentProperties current) {
                return new HttpCallResult(500, "server error");
            }
        };
        AgentChatRequest request = new AgentChatRequest();
        request.setMessage("挂号");

        AgentModelDecision decision = service.decide(request, new HashMap<String, Object>());

        Assertions.assertNotNull(decision);
        Assertions.assertEquals("none", decision.getAction());
        Assertions.assertEquals(Boolean.TRUE, decision.getDegraded());
        Assertions.assertEquals("http_500", decision.getFallbackReason());
        Assertions.assertEquals(Integer.valueOf(500), decision.getHttpStatus());
        Assertions.assertEquals(Integer.valueOf(1), decision.getRetryCount());
    }
}
