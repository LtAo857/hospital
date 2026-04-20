package com.example.hospital.patient.wx.api.agent.multi.service;

import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.memory.MultiAgentMemoryService;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.multi.rag.MultiAgentKnowledgeBase;
import com.example.hospital.patient.wx.api.agent.multi.rag.MultiAgentRagService;
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

        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(properties, memoryService, buildTestRagService(), workers);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-1");
        request.setMessage("挂号");

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

    @Test
    void shouldExposeFallbackCardForLoginRequiredFailure() {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(2);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-2")).thenReturn(new HashMap<String, Object>());

        List<AgentWorker> workers = Arrays.asList(
                fixedWorker(MultiAgentStage.INTENT_PARSE, HandoffAction.FAIL, MultiAgentStage.MANUAL_FALLBACK, "请先登录", new HashMap<String, Object>() {{
                    put("errorCode", MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED);
                    put("retryable", false);
                    put("errorMessage", "确认挂号前请先登录小程序。");
                    put("requiresLogin", true);
                }})
        );

        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(properties, memoryService, buildTestRagService(), workers);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-2");
        request.setMessage("挂号");

        AgentChatResponse response = service.chat(request, null);

        Assertions.assertEquals("need_login", response.getState());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED, response.getErrorCode());
        Assertions.assertEquals(Boolean.FALSE, response.getRetryable());
        Assertions.assertEquals("确认挂号前请先登录小程序。", response.getErrorMessage());
        Assertions.assertFalse(response.getCards().isEmpty());
        Assertions.assertEquals("去登录", response.getCards().get(0).getTitle());
        Assertions.assertEquals("navigate", response.getCards().get(0).getAction());
        Assertions.assertEquals("/pages/mine/mine", response.getCards().get(0).getPayload().get("url"));
    }

    @Test
    void shouldBuildToolLogsAndFlowsForScheduleWorkerContract() {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(3);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-3")).thenReturn(new HashMap<String, Object>());

        List<AgentWorker> workers = Arrays.asList(
                fixedWorker(MultiAgentStage.INTENT_PARSE, HandoffAction.HANDOFF, MultiAgentStage.SLOT_QUERY, "识别完成", null),
                fixedWorker(MultiAgentStage.SLOT_QUERY, HandoffAction.HANDOFF, MultiAgentStage.POLICY_CHECK, "已选中号源", new HashMap<String, Object>() {{
                    put("pendingOrder", new HashMap<String, Object>() {{
                        put("deptSubId", 10);
                        put("date", "2026-04-20");
                    }});
                    put("awaitingConfirmation", true);
                }}, "schedule-agent", "searchScheduleSlots", "slot_selected", new HashMap<String, Object>() {{
                    put("selectedOrder", new HashMap<String, Object>() {{
                        put("doctorId", 9);
                    }});
                }})
        );

        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(properties, memoryService, buildTestRagService(), workers);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-3");
        request.setMessage("明天口腔科");

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertEquals("awaiting_confirmation", response.getState());
        Assertions.assertFalse(response.getToolLogs().isEmpty());
        Assertions.assertEquals("查询号源时段", response.getToolLogs().get(0).getName());
        Assertions.assertEquals("已选中可挂号源", response.getToolLogs().get(0).getSummary());
        Assertions.assertFalse(response.getAgentFlows().isEmpty());
        Assertions.assertEquals("号源查询 Agent", response.getAgentFlows().get(1).getTitle());
        Assertions.assertTrue(response.getAgentFlows().get(1).getSummary().contains("已选中可挂号源"));
    }

    @Test
    void shouldAppendRequestedViewCardsAndExplainCard() {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(2);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-4")).thenReturn(new HashMap<String, Object>() {{
            put("requestedView", "view_registrations");
            put("pendingOrder", new HashMap<String, Object>() {{
                put("doctorName", "张医生");
            }});
        }});

        List<AgentWorker> workers = Arrays.asList(
                fixedWorker(MultiAgentStage.INTENT_PARSE, HandoffAction.FINISH, MultiAgentStage.DONE, "已为你准备入口", null)
        );

        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(properties, memoryService, buildTestRagService(), workers);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-4");
        request.setMessage("查看我的挂号");

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertFalse(response.getCards().isEmpty());
        Assertions.assertEquals("查看我的挂号", response.getCards().get(0).getTitle());
        Assertions.assertTrue(response.getCards().stream().anyMatch(card -> "navigate".equals(card.getAction()) && "/pages/registration_list/registration_list".equals(card.getPayload().get("url"))));
        Assertions.assertTrue(response.getCards().stream().anyMatch(card -> "explain_recommendation".equals(card.getAction())));
    }

    @Test
    void shouldUseRagAnswerForExplanationRequest() {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(2);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-5")).thenReturn(new HashMap<String, Object>() {{
            put("requestedView", "explain_recommendation");
            put("ragQuestion", "为什么推荐这个");
            put("pendingOrder", new HashMap<String, Object>() {{
                put("doctorName", "张医生");
            }});
        }});

        List<AgentWorker> workers = Arrays.asList(
                fixedWorker(MultiAgentStage.INTENT_PARSE, HandoffAction.FINISH, MultiAgentStage.DONE, "我先结合当前挂号上下文和知识库为你解释一下。", null)
        );

        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(properties, memoryService, buildTestRagService(), workers);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-5");
        request.setMessage("为什么推荐这个");

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertTrue(response.getReply().contains("真实候选号源") || response.getReply().contains("推荐逻辑") || response.getReply().contains("完整思考链"));
        Assertions.assertNotNull(response.getMemory().get("ragSources"));
        Assertions.assertTrue(String.valueOf(response.getMemory().get("ragSources")).contains("Multi-Agent 实现说明"));
        Assertions.assertTrue(response.getCards().stream().anyMatch(card -> "知识来源".equals(card.getTitle())));
    }

    private MultiAgentRagService buildTestRagService() {
        AgentProperties agentProperties = new AgentProperties();
        agentProperties.setLlmEnabled(false);
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setRagEmbeddingEnabled(false);
        return new MultiAgentRagService(agentProperties, properties, new MultiAgentKnowledgeBase(properties, null));
    }

    private AgentWorker fixedWorker(final MultiAgentStage stage,
                                    final HandoffAction action,
                                    final MultiAgentStage nextStage,
                                    final String reply,
                                    final Map<String, Object> patch) {
        return fixedWorker(stage, action, nextStage, reply, patch, "test-" + stage.name(), null, null, null);
    }

    private AgentWorker fixedWorker(final MultiAgentStage stage,
                                    final HandoffAction action,
                                    final MultiAgentStage nextStage,
                                    final String reply,
                                    final Map<String, Object> patch,
                                    final String agent,
                                    final String toolName,
                                    final String summary,
                                    final Map<String, Object> observation) {
        return new AgentWorker() {
            @Override
            public MultiAgentStage stage() {
                return stage;
            }

            @Override
            public AgentResult execute(AgentContext context) {
                AgentResult result = new AgentResult();
                result.setAgent(agent);
                result.setHandoffAction(action);
                result.setNextStage(nextStage);
                result.setReply(reply);
                result.setMemoryPatch(patch);
                result.setToolName(toolName);
                result.setSummary(summary);
                result.setObservation(observation);
                return result;
            }
        };
    }
}
