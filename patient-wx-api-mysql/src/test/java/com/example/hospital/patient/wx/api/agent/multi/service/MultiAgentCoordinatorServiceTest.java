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
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentParser;
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentResult;
import com.example.hospital.patient.wx.api.agent.multi.worker.AgentWorker;
import com.example.hospital.patient.wx.api.agent.multi.service.MultiAgentDepartmentCatalogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class MultiAgentCoordinatorServiceTest {

    @Test
    void shouldCompleteFlowAndPersistDoneState() throws Exception {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(6);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-1")).thenReturn(new HashMap<String, Object>());

        List<AgentWorker> workers = Arrays.asList(
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

        MultiAgentCoordinatorService service = buildCoordinator(properties, memoryService, workers);
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
    void shouldExposeFallbackCardForLoginRequiredFailure() throws Exception {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(2);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-2")).thenReturn(new HashMap<String, Object>());

        List<AgentWorker> workers = Arrays.asList(
                fixedWorker(MultiAgentStage.SLOT_QUERY, HandoffAction.FAIL, MultiAgentStage.MANUAL_FALLBACK, "请先登录", new HashMap<String, Object>() {{
                    put("errorCode", MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED);
                    put("retryable", false);
                    put("errorMessage", "确认挂号前请先登录小程序。");
                    put("requiresLogin", true);
                }})
        );

        MultiAgentCoordinatorService service = buildCoordinator(properties, memoryService, workers);
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
    void shouldBuildToolLogsAndFlowsForScheduleWorkerContract() throws Exception {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(3);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-3")).thenReturn(new HashMap<String, Object>());

        List<AgentWorker> workers = Arrays.asList(
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

        MultiAgentCoordinatorService service = buildCoordinator(properties, memoryService, workers);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-3");
        request.setMessage("明天口腔科");

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertEquals("awaiting_confirmation", response.getState());
        Assertions.assertFalse(response.getToolLogs().isEmpty());
        Assertions.assertEquals("查询号源时段", response.getToolLogs().get(0).getName());
        Assertions.assertEquals("已选中可挂号源", response.getToolLogs().get(0).getSummary());
        Assertions.assertFalse(response.getAgentFlows().isEmpty());
        Assertions.assertEquals("号源查询 Agent", response.getAgentFlows().get(0).getTitle());
        Assertions.assertTrue(response.getAgentFlows().get(0).getSummary().contains("已选中可挂号源"));
    }

    @Test
    void shouldAppendRequestedViewCardsAndExplainCard() throws Exception {
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
                fixedWorker(MultiAgentStage.SLOT_QUERY, HandoffAction.FINISH, MultiAgentStage.DONE, "已为你准备入口", null)
        );

        MultiAgentCoordinatorService service = buildCoordinator(properties, memoryService, workers, "registration", null, null);
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
    void shouldUseRagAnswerForExplanationRequest() throws Exception {
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
                fixedWorker(MultiAgentStage.SLOT_QUERY, HandoffAction.FINISH, MultiAgentStage.DONE, "我先结合当前挂号上下文和知识库为你解释一下。", null)
        );

        MultiAgentCoordinatorService service = buildCoordinator(properties, memoryService, workers, "explain_recommendation", null, null);
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-5");
        request.setMessage("为什么推荐这个");

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertTrue(response.getReply().contains("真实候选号源") || response.getReply().contains("推荐逻辑") || response.getReply().contains("完整思考链"));
        Assertions.assertNotNull(response.getMemory().get("ragSources"));
        Assertions.assertTrue(String.valueOf(response.getMemory().get("ragSources")).contains("Multi-Agent 实现说明"));
        Assertions.assertTrue(response.getCards().stream().anyMatch(card -> "知识来源".equals(card.getTitle())));
    }

    @Test
    void shouldRejectForgedConfirmationBeforeCallingWorker() throws Exception {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(2);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-6")).thenReturn(new HashMap<String, Object>());

        AgentWorker worker = Mockito.mock(AgentWorker.class);
        Mockito.when(worker.stage()).thenReturn(MultiAgentStage.SLOT_QUERY);

        MultiAgentCoordinatorService service = buildCoordinator(properties, memoryService, Arrays.asList(worker));
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-6");
        request.setAction("create_registration");
        request.setPayload(new HashMap<String, Object>() {{
            put("confirmed", true);
            put("deptSubId", 10);
            put("date", "2026-04-20");
            put("workPlanId", 101);
            put("scheduleId", 202);
            put("doctorId", 303);
            put("slot", 1);
            put("amount", "10");
        }});

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, response.getErrorCode());
        Assertions.assertEquals("confirmation_mismatch", response.getMemory().get("badCaseType"));
        Assertions.assertTrue(String.valueOf(response.getErrorMessage()).contains("确认信息已失效"));
        Mockito.verify(worker, Mockito.never()).execute(Mockito.any(AgentContext.class));
    }

    @Test
    void shouldRejectInvalidChatPayloadBeforeCallingWorker() throws Exception {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(2);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        Mockito.when(memoryService.load("session-7")).thenReturn(new HashMap<String, Object>());

        AgentWorker worker = Mockito.mock(AgentWorker.class);
        Mockito.when(worker.stage()).thenReturn(MultiAgentStage.SLOT_QUERY);

        MultiAgentCoordinatorService service = buildCoordinator(properties, memoryService, Arrays.asList(worker));
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionId("session-7");
        request.setPayload(new HashMap<String, Object>() {{
            put("deptSubId", "abc");
            put("date", "2026-02-30");
        }});

        AgentChatResponse response = service.chat(request, 1001);

        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, response.getErrorCode());
        Assertions.assertEquals("payload_invalid", response.getMemory().get("badCaseType"));
        Assertions.assertNotNull(response.getMemory().get("badFields"));
        Mockito.verify(worker, Mockito.never()).execute(Mockito.any(AgentContext.class));
    }

    private MultiAgentCoordinatorService buildCoordinator(MultiAgentProperties properties,
                                                           MultiAgentMemoryService memoryService,
                                                           List<AgentWorker> workers) throws Exception {
        return buildCoordinator(properties, memoryService, workers, "registration", "口腔科", null);
    }

    private MultiAgentCoordinatorService buildCoordinator(MultiAgentProperties properties,
                                                           MultiAgentMemoryService memoryService,
                                                           List<AgentWorker> workers,
                                                           String intent, String deptName, String date) throws Exception {
        MultiAgentCoordinatorService service = new MultiAgentCoordinatorService(properties, memoryService,
                buildTestRagService(), null, workers);
        injectModelIntentParser(service, intent, deptName, date);
        return service;
    }

    private void injectModelIntentParser(MultiAgentCoordinatorService service) throws Exception {
        injectModelIntentParser(service, "registration", "口腔科", null);
    }

    private void injectModelIntentParser(MultiAgentCoordinatorService service, String intent, String deptName, String date) throws Exception {
        ModelIntentParser parser = Mockito.mock(ModelIntentParser.class);
        Map<String, Object> slots = new HashMap<>();
        if (deptName != null) slots.put("department", deptName);
        if (date != null) slots.put("date", date);
        ModelIntentResult result = new ModelIntentResult();
        result.setIntent(intent);
        result.setSlots(slots);
        result.setConfidence(0.9);
        result.setSource("llm");
        Mockito.when(parser.parse(Mockito.anyString(), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(Optional.of(result));
        Field field = MultiAgentCoordinatorService.class.getDeclaredField("modelIntentParser");
        field.setAccessible(true);
        field.set(service, parser);
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

    @Test
    void shouldResolveFreshDateWhenPreviousQueryHasDifferentDate() throws Exception {
        MultiAgentProperties properties = new MultiAgentProperties();
        properties.setMaxHops(4);

        MultiAgentMemoryService memoryService = Mockito.mock(MultiAgentMemoryService.class);
        final Map<String, Object> persistedMemory = new HashMap<>();
        Mockito.when(memoryService.load("session-date")).thenAnswer(inv -> new HashMap<>(persistedMemory));
        Mockito.doAnswer(inv -> {
            persistedMemory.clear();
            persistedMemory.putAll(inv.getArgument(1));
            return null;
        }).when(memoryService).save(Mockito.eq("session-date"), Mockito.anyMap());

        // First query "明天"
        List<AgentWorker> workers1 = Arrays.asList(
                fixedWorker(MultiAgentStage.SLOT_QUERY, HandoffAction.HANDOFF, MultiAgentStage.POLICY_CHECK,
                        "2026-06-20 骨科 暂无可挂号医生", new HashMap<String, Object>() {{
                            put("deptName", "骨科");
                            put("date", "2026-06-20");
                            put("pendingOrder", null);
                            put("awaitingConfirmation", false);
                        }}, "schedule-agent", "searchScheduleSlots", "no_slot_available", null)
        );
        MultiAgentCoordinatorService service1 = buildCoordinator(properties, memoryService, workers1, "query_doctor", "骨科", "明天");
        AgentChatRequest req1 = new AgentChatRequest();
        req1.setSessionId("session-date");
        req1.setMessage("骨科明天哪些医生有号源");
        service1.chat(req1, 1001);

        // Second query "今天" — must not reuse stale 2026-06-20
        List<AgentWorker> workers2 = Arrays.asList(
                fixedWorker(MultiAgentStage.SLOT_QUERY, HandoffAction.HANDOFF, MultiAgentStage.POLICY_CHECK,
                        "2026-06-19 骨科 暂无可挂号医生", new HashMap<String, Object>() {{
                            put("deptName", "骨科");
                            put("date", "2026-06-19");
                            put("pendingOrder", null);
                            put("awaitingConfirmation", false);
                        }}, "schedule-agent", "searchScheduleSlots", "no_slot_available", null)
        );
        MultiAgentCoordinatorService service2 = buildCoordinator(properties, memoryService, workers2, "query_doctor", "骨科", "今天");
        AgentChatRequest req2 = new AgentChatRequest();
        req2.setSessionId("session-date");
        req2.setMessage("骨科今天哪些医生有号源");
        AgentChatResponse r2 = service2.chat(req2, 1001);

        Assertions.assertTrue(r2.getReply().contains("2026-06-19"),
                "Second reply should resolve 今天 to 2026-06-19, not reuse stale 2026-06-20. Got: " + r2.getReply());
    }
}
