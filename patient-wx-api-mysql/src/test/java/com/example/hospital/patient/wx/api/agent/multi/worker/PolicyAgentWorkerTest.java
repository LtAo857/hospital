package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.UserAgentTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class PolicyAgentWorkerTest {

    @Test
    void shouldAskLoginWhenUserMissing() {
        UserAgentTools userAgentTools = Mockito.mock(UserAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        PolicyAgentWorker worker = new PolicyAgentWorker(userAgentTools, registrationAgentTools);

        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("deptSubId", 10);
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.POLICY_CHECK, result.getNextStage());
        Assertions.assertEquals("policy_login_required", result.getSummary());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals(Boolean.TRUE, result.getMemoryPatch().get("requiresLogin"));
    }

    @Test
    void shouldAskCreateUserCardWhenMissing() {
        UserAgentTools userAgentTools = Mockito.mock(UserAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(userAgentTools.hasUserCard(1001)).thenReturn(false);
        PolicyAgentWorker worker = new PolicyAgentWorker(userAgentTools, registrationAgentTools);

        AgentContext context = new AgentContext();
        context.setUserId(1001);
        context.setPayload(new HashMap<String, Object>() {{
            put("deptSubId", 10);
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals("policy_user_card_required", result.getSummary());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_USER_CARD_REQUIRED, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals(Boolean.FALSE, result.getMemoryPatch().get("retryable"));
    }

    @Test
    void shouldFailWhenRegistrationConditionRejected() {
        UserAgentTools userAgentTools = Mockito.mock(UserAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(userAgentTools.hasUserCard(1001)).thenReturn(true);
        Mockito.when(registrationAgentTools.checkRegistrationCondition(1001, 10, "2026-04-20"))
                .thenReturn("已经达到当天挂号上限");
        PolicyAgentWorker worker = new PolicyAgentWorker(userAgentTools, registrationAgentTools);

        AgentContext context = new AgentContext();
        context.setUserId(1001);
        context.setPayload(new HashMap<String, Object>() {{
            put("deptSubId", 10);
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FAIL, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.MANUAL_FALLBACK, result.getNextStage());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_DAILY_LIMIT_REACHED, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals("已经达到当天挂号上限", result.getReply());
    }

    @Test
    void shouldWaitForConfirmationWhenConditionPassed() {
        UserAgentTools userAgentTools = Mockito.mock(UserAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(userAgentTools.hasUserCard(1001)).thenReturn(true);
        Mockito.when(registrationAgentTools.checkRegistrationCondition(1001, 10, "2026-04-20"))
                .thenReturn("满足挂号条件");
        PolicyAgentWorker worker = new PolicyAgentWorker(userAgentTools, registrationAgentTools);

        Map<String, Object> pendingOrder = new HashMap<>();
        pendingOrder.put("deptSubId", 10);
        pendingOrder.put("date", "2026-04-20");

        AgentContext context = new AgentContext();
        context.setUserId(1001);
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>() {{
            put("pendingOrder", pendingOrder);
        }});

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.CONFIRM_WAIT, result.getNextStage());
        Assertions.assertEquals(Boolean.TRUE, result.getMemoryPatch().get("awaitingConfirmation"));
        Assertions.assertNull(result.getMemoryPatch().get("errorCode"));
    }

    @Test
    void shouldHandoffToExecutionWhenConfirmed() {
        UserAgentTools userAgentTools = Mockito.mock(UserAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(userAgentTools.hasUserCard(1001)).thenReturn(true);
        Mockito.when(registrationAgentTools.checkRegistrationCondition(1001, 10, "2026-04-20"))
                .thenReturn("满足挂号条件");
        PolicyAgentWorker worker = new PolicyAgentWorker(userAgentTools, registrationAgentTools);

        AgentContext context = new AgentContext();
        context.setUserId(1001);
        context.setUserAction(AgentAction.CREATE_REGISTRATION);
        context.setPayload(new HashMap<String, Object>() {{
            put("deptSubId", 10);
            put("date", "2026-04-20");
            put("confirmed", true);
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);
        Map<String, Object> patch = result.getMemoryPatch();

        Assertions.assertEquals(HandoffAction.HANDOFF, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.EXECUTE_APPOINTMENT, result.getNextStage());
        Assertions.assertEquals(Boolean.FALSE, patch.get("awaitingConfirmation"));
        Assertions.assertNull(patch.get("errorCode"));
    }
}
