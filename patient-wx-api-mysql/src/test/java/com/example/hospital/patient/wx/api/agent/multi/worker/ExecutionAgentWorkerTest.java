package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class ExecutionAgentWorkerTest {

    @Test
    void shouldRejectWhenUserNotLoggedIn() {
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        ExecutionAgentWorker worker = new ExecutionAgentWorker(registrationAgentTools);

        AgentContext context = new AgentContext();
        context.setSessionId("session-1");
        context.setPayload(new HashMap<String, Object>() {{
            put("deptSubId", 10);
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FAIL, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.MANUAL_FALLBACK, result.getNextStage());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals(Boolean.TRUE, result.getMemoryPatch().get("requiresLogin"));
    }

    @Test
    void shouldFailWhenParametersMissing() {
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        ExecutionAgentWorker worker = new ExecutionAgentWorker(registrationAgentTools);

        AgentContext context = new AgentContext();
        context.setUserId(1001);
        context.setSessionId("session-1");
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FAIL, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals(Boolean.FALSE, result.getMemoryPatch().get("requiresLogin"));
        Assertions.assertEquals(Boolean.FALSE, result.getMemoryPatch().get("awaitingConfirmation"));
    }

    @Test
    void shouldFinishWhenRegistrationCreatedSuccessfully() {
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(registrationAgentTools.createRegistrationOrder(Mockito.eq(1001), Mockito.anyMap()))
                .thenReturn(new HashMap() {{
                    put("outTradeNo", "OT-100");
                }});
        ExecutionAgentWorker worker = new ExecutionAgentWorker(registrationAgentTools);

        AgentContext context = buildContext();

        AgentResult result = worker.execute(context);
        Map<String, Object> patch = result.getMemoryPatch();
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(registrationAgentTools).createRegistrationOrder(Mockito.eq(1001), captor.capture());

        Assertions.assertEquals(HandoffAction.FINISH, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.DONE, result.getNextStage());
        Assertions.assertEquals("OT-100", patch.get("lastOrderNo"));
        Assertions.assertNull(patch.get("errorCode"));
        Assertions.assertEquals("session-1", captor.getValue().get("sessionId"));
        Assertions.assertNotNull(captor.getValue().get("requestId"));
    }

    @Test
    void shouldRejectWhenConfirmationMissing() {
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        ExecutionAgentWorker worker = new ExecutionAgentWorker(registrationAgentTools);

        AgentContext context = buildContext();
        context.setPayload(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FAIL, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals("confirmation_mismatch", result.getMemoryPatch().get("badCaseType"));
        Mockito.verify(registrationAgentTools, Mockito.never()).createRegistrationOrder(Mockito.anyInt(), Mockito.anyMap());
    }

    @Test
    void shouldRejectWhenConfirmationPayloadDiffersFromPendingOrder() {
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        ExecutionAgentWorker worker = new ExecutionAgentWorker(registrationAgentTools);

        AgentContext context = buildContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("confirmed", true);
            put("amount", "11");
        }});

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FAIL, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals("confirmation_mismatch", result.getMemoryPatch().get("badCaseType"));
        Mockito.verify(registrationAgentTools, Mockito.never()).createRegistrationOrder(Mockito.anyInt(), Mockito.anyMap());
    }

    @Test
    void shouldReturnStructuredFailureWhenServiceReturnsBusinessError() {
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(registrationAgentTools.createRegistrationOrder(Mockito.eq(1001), Mockito.anyMap()))
                .thenReturn(new HashMap() {{
                    put("errorCode", MultiAgentErrorCode.REGISTRATION_SLOT_CHANGED);
                    put("message", "该号源已不可用，请重新选择其他时段。");
                    put("retryable", true);
                }});
        ExecutionAgentWorker worker = new ExecutionAgentWorker(registrationAgentTools);

        AgentContext context = buildContext();
        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FAIL, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_SLOT_CHANGED, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals(Boolean.TRUE, result.getMemoryPatch().get("retryable"));
        Assertions.assertNull(result.getMemoryPatch().get("pendingOrder"));
    }

    @Test
    void shouldConvertHospitalExceptionToStructuredFailure() {
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(registrationAgentTools.createRegistrationOrder(Mockito.eq(1001), Mockito.anyMap()))
                .thenThrow(new HospitalException("数据库写失败"));
        ExecutionAgentWorker worker = new ExecutionAgentWorker(registrationAgentTools);

        AgentContext context = buildContext();
        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FAIL, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals("数据库写失败", result.getReply());
    }

    @Test
    void shouldConvertUnknownExceptionToStructuredFailure() {
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(registrationAgentTools.createRegistrationOrder(Mockito.eq(1001), Mockito.anyMap()))
                .thenThrow(new RuntimeException("boom"));
        ExecutionAgentWorker worker = new ExecutionAgentWorker(registrationAgentTools);

        AgentContext context = buildContext();
        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.FAIL, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals("挂号提交失败，请稍后重试。", result.getReply());
    }

    private AgentContext buildContext() {
        Map<String, Object> pendingOrder = new HashMap<>();
        pendingOrder.put("workPlanId", 101);
        pendingOrder.put("scheduleId", 202);
        pendingOrder.put("doctorId", 303);
        pendingOrder.put("deptSubId", 10);
        pendingOrder.put("date", "2026-04-20");
        pendingOrder.put("slot", 1);
        pendingOrder.put("amount", "10");
        pendingOrder.put("doctorName", "张医生");

        AgentContext context = new AgentContext();
        context.setUserId(1001);
        context.setSessionId("session-1");
        context.setMemory(new HashMap<String, Object>() {{
            put("pendingOrder", pendingOrder);
        }});
        context.setPayload(new HashMap<String, Object>() {{
            put("confirmed", true);
        }});
        return context;
    }
}
