package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentParser;
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class TriageAgentWorkerTest {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Test
    void shouldRouteToSlotQueryWhenRegistrationIntentDetected() {
        TriageAgentWorker worker = new TriageAgentWorker();
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("我明天想挂号");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.HANDOFF, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.SLOT_QUERY, result.getNextStage());
        Assertions.assertEquals(LocalDate.now().plusDays(1).format(DATE_FORMATTER), result.getMemoryPatch().get("date"));
    }

    @Test
    void shouldRouteToSlotQueryWhenOnlyDeptAndDateProvided() {
        TriageAgentWorker worker = new TriageAgentWorker();
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("明天的骨科");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.HANDOFF, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.SLOT_QUERY, result.getNextStage());
        Assertions.assertEquals(LocalDate.now().plusDays(1).format(DATE_FORMATTER), result.getMemoryPatch().get("date"));
    }

    @Test
    void shouldExposeMessageViewIntent() {
        TriageAgentWorker worker = new TriageAgentWorker();
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("查看消息");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FINISH, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.DONE, result.getNextStage());
        Assertions.assertEquals("view_messages_requested", result.getSummary());
        Assertions.assertEquals("view_messages", result.getMemoryPatch().get("requestedView"));
    }

    @Test
    void shouldExposeRegistrationListIntent() {
        TriageAgentWorker worker = new TriageAgentWorker();
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("查看我的挂号");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FINISH, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.DONE, result.getNextStage());
        Assertions.assertEquals("view_registrations_requested", result.getSummary());
        Assertions.assertEquals("view_registrations", result.getMemoryPatch().get("requestedView"));
    }

    @Test
    void shouldExplainRecommendationWhenAsked() {
        TriageAgentWorker worker = new TriageAgentWorker();
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>() {{
            put("deptSubName", "口腔门诊");
            put("date", "2026-04-20");
        }});
        context.setUserMessage("为什么推荐这个");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FINISH, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.DONE, result.getNextStage());
        Assertions.assertEquals("explanation_requested", result.getSummary());
        Assertions.assertEquals("我先结合当前挂号上下文和知识库为你解释一下。", result.getReply());
    }
    @Test
    void shouldUseModelIntentAsOptionalRegistrationSignal() {
        TriageAgentWorker worker = new TriageAgentWorker();
        ModelIntentResult modelResult = new ModelIntentResult();
        modelResult.setIntent("registration");
        modelResult.setConfidence(0.91d);
        modelResult.setSource("demo_rules");
        modelResult.setModel("hospital-nlu-demo-v1");
        modelResult.setSlots(new HashMap<String, Object>() {{
            put("department", "\u53e3\u8154\u79d1");
            put("date", "\u660e\u5929");
            put("symptom", "\u7259\u75bc");
        }});
        ReflectionTestUtils.setField(worker, "modelIntentParser", (ModelIntentParser) (text, sessionId) -> Optional.of(modelResult));

        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());
        context.setSessionId("test-session");
        context.setUserMessage("\u7259\u75bc");

        AgentResult result = worker.execute(context);
        Map<String, Object> patch = result.getMemoryPatch();

        Assertions.assertEquals(HandoffAction.HANDOFF, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.SLOT_QUERY, result.getNextStage());
        Assertions.assertEquals("\u53e3\u8154\u79d1", patch.get("deptSubName"));
        Assertions.assertEquals("\u7259\u75bc", patch.get("symptom"));
        Assertions.assertEquals(LocalDate.now().plusDays(1).format(DATE_FORMATTER), patch.get("date"));
        Assertions.assertEquals("registration", patch.get("nluIntent"));
        Assertions.assertEquals("demo_rules", patch.get("nluSource"));
    }
}
