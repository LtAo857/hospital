package com.example.hospital.patient.wx.api.agent.service;

import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.dto.AgentModelDecision;
import com.example.hospital.patient.wx.api.agent.memory.AgentConversationMemoryService;
import com.example.hospital.patient.wx.api.agent.tool.DoctorAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.MedicalDeptAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.MessageAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.UserAgentTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class AgentOrchestratorServiceTelemetryTest {

    @Test
    void shouldExposeModelTelemetryWhenDegraded() throws Exception {
        AgentConversationMemoryService memoryService = Mockito.mock(AgentConversationMemoryService.class);
        Mockito.when(memoryService.load("session-telemetry")).thenReturn(new HashMap<String, Object>());

        NoModelAgentEngine engine = new NoModelAgentEngine();
        DashScopeAgentService dashScopeAgentService = Mockito.mock(DashScopeAgentService.class);
        AgentModelDecision degraded = new AgentModelDecision();
        degraded.setAction("none");
        degraded.setDegraded(true);
        degraded.setFallbackReason("http_500");
        degraded.setProvider("dashscope");
        degraded.setHttpStatus(500);
        degraded.setRetryCount(1);
        degraded.setLatencyMs(23L);
        degraded.setPromptTokens(0);
        degraded.setCompletionTokens(0);
        degraded.setTotalTokens(0);
        Mockito.when(dashScopeAgentService.decide(Mockito.any(AgentChatRequest.class), Mockito.anyMap())).thenReturn(degraded);

        UserAgentTools userTools = Mockito.mock(UserAgentTools.class);
        Mockito.when(userTools.hasUserCard(1001)).thenReturn(true);
        MedicalDeptAgentTools deptTools = Mockito.mock(MedicalDeptAgentTools.class);
        Mockito.when(deptTools.searchDepartments(Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(new ArrayList<HashMap>());
        DoctorAgentTools doctorTools = Mockito.mock(DoctorAgentTools.class);
        RegistrationAgentTools registrationTools = Mockito.mock(RegistrationAgentTools.class);
        MessageAgentTools messageTools = Mockito.mock(MessageAgentTools.class);
        Mockito.when(messageTools.listMessages(Mockito.eq(1001), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new com.example.hospital.patient.wx.api.common.PageUtils(new ArrayList<HashMap>(), 0, 1, 10));

        AgentOrchestratorService service = new AgentOrchestratorService();
        setField(service, "noModelAgentEngine", engine);
        setField(service, "dashScopeAgentService", dashScopeAgentService);
        setField(service, "memoryService", memoryService);
        setField(service, "userAgentTools", userTools);
        setField(service, "medicalDeptAgentTools", deptTools);
        setField(service, "doctorAgentTools", doctorTools);
        setField(service, "registrationAgentTools", registrationTools);
        setField(service, "messageAgentTools", messageTools);

        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-telemetry");
        request.setMessage("查看消息");

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertNotNull(response.getMemory());
        Assertions.assertEquals(Boolean.TRUE, response.getMemory().get("modelDegraded"));
        Assertions.assertEquals("http_500", response.getMemory().get("modelFallbackReason"));
        Assertions.assertEquals("dashscope", response.getMemory().get("modelProvider"));
        Assertions.assertEquals(500, response.getMemory().get("modelHttpStatus"));
        Assertions.assertEquals(1, response.getMemory().get("modelRetryCount"));
        Assertions.assertFalse(response.getToolLogs().isEmpty());
        Assertions.assertEquals("degraded", response.getToolLogs().get(0).getStatus());
        Assertions.assertTrue(response.getToolLogs().get(0).getSummary().contains("降级"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = AgentOrchestratorService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
