package com.example.hospital.patient.wx.api.agent.multi.service;

import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.memory.MultiAgentMemoryService;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.multi.worker.AgentWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MultiAgentCoordinatorServiceTest {

    @Test
    void shouldCompleteFlowAndPersistDoneState() {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(6);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-1")).thenReturn(new HashMap<String, Object>());

        List<AgentWorker> workers = Arrays.asList(
                fixedWorker(MultiAgentStage.INTENT_PARSE, HandoffAction.HANDOFF, MultiAgentStage.SLOT_QUERY, "triage ok", null),
                fixedWorker(MultiAgentStage.SLOT_QUERY, HandoffAction.HANDOFF, MultiAgentStage.POLICY_CHECK, "slot ok", new HashMap<String, Object>() {{
                    put("pendingOrder", new HashMap<String, Object>() {{
                        put("deptSubId", 10);
                        put("date", "2026-04-20");
                    }});
                }}),
                fixedWorker(MultiAgentStage.POLICY_CHECK, HandoffAction.HANDOFF, MultiAgentStage.EXECUTE_APPOINTMENT, "policy ok", new HashMap<String, Object>() {{
                    put("policyChecked", true);
                }}),
                fixedWorker(MultiAgentStage.EXECUTE_APPOINTMENT, HandoffAction.FINISH, MultiAgentStage.DONE, "done", new HashMap<String, Object>() {{
                    put("pendingOrder", null);
                    put("awaitingConfirmation", false);
                    put("lastOrderNo", "OT-1");
                }})
        );

        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(properties, memoryService, workers);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setMessage("\u6302\u53f7");

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertEquals("completed", response.getState());
        Assertions.assertEquals("done", response.getReply());
        Assertions.assertEquals(4, response.getSteps().size());
        Assertions.assertEquals("DONE", response.getMemory().get("stage"));
        Assertions.assertFalse(response.isRequiresConfirmation());
        Assertions.assertFalse(response.isRequiresLogin());

        ArgumentCaptor<Map> memoryCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(memoryService, Mockito.times(1)).save(Mockito.eq("session-1"), memoryCaptor.capture());
        Map savedMemory = memoryCaptor.getValue();
        Assertions.assertEquals("DONE", savedMemory.get("stage"));
        Assertions.assertEquals("OT-1", savedMemory.get("lastOrderNo"));
    }

    private AgentWorker fixedWorker(final MultiAgentStage stage,
                                    final HandoffAction action,
                                    final MultiAgentStage nextStage,
                                    final String reply,
                                    final Map<String, Object> patch) {
        return new AgentWorker() {
            @Override
            public MultiAgentStage stage() {
                return stage;
            }

            @Override
            public AgentResult execute(AgentContext context) {
                AgentResult result = new AgentResult();
                result.setAgent("test-" + stage.name());
                result.setHandoffAction(action);
                result.setNextStage(nextStage);
                result.setReply(reply);
                result.setMemoryPatch(patch);
                return result;
            }
        };
    }
}
