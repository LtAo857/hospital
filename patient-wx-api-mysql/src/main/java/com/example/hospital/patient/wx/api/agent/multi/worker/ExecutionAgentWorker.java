package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class ExecutionAgentWorker implements AgentWorker {
    private final RegistrationAgentTools registrationAgentTools;

    public ExecutionAgentWorker(RegistrationAgentTools registrationAgentTools) {
        this.registrationAgentTools = registrationAgentTools;
    }

    @Override
    public MultiAgentStage stage() {
        return MultiAgentStage.EXECUTE_APPOINTMENT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AgentResult execute(AgentContext context) {
        Map<String, Object> memory = safeMap(context.getMemory());
        Map<String, Object> payload = safeMap(context.getPayload());
        Map<String, Object> patch = new HashMap<>();

        Map<String, Object> order = new HashMap<>();
        if (memory.get("pendingOrder") instanceof Map) {
            order.putAll((Map<String, Object>) memory.get("pendingOrder"));
        }
        order.putAll(payload);

        AgentResult result = new AgentResult();
        result.setAgent("execution-agent");
        result.setMemoryPatch(patch);
        result.setToolName("createRegistrationOrder");
        result.setToolInput(new HashMap<>(order));
        result.setConfidence(0.88d);

        if (context.getUserId() == null) {
            result.setHandoffAction(HandoffAction.FAIL);
            result.setNextStage(MultiAgentStage.MANUAL_FALLBACK);
            result.setReply("Not logged in. Cannot execute registration.");
            result.setSummary("execution_login_required");
            patch.put("requiresLogin", true);
            return result;
        }
        if (intValue(order.get("deptSubId")) == null || !StringUtils.hasText(stringValue(order.get("date")))) {
            result.setHandoffAction(HandoffAction.FAIL);
            result.setNextStage(MultiAgentStage.MANUAL_FALLBACK);
            result.setReply("Registration payload is incomplete. Please re-select slot.");
            result.setSummary("execution_missing_order_fields");
            return result;
        }

        HashMap createResult = registrationAgentTools.createRegistrationOrder(context.getUserId(), order);
        Map<String, Object> observation = new HashMap<>();
        observation.put("result", createResult);
        result.setObservation(observation);

        if (createResult == null || !createResult.containsKey("outTradeNo")) {
            result.setHandoffAction(HandoffAction.FAIL);
            result.setNextStage(MultiAgentStage.MANUAL_FALLBACK);
            result.setReply("Slot is no longer available. Please choose another one.");
            result.setSummary("execution_order_failed");
            patch.put("pendingOrder", null);
            patch.put("awaitingConfirmation", false);
            return result;
        }

        patch.put("pendingOrder", null);
        patch.put("awaitingConfirmation", false);
        patch.put("lastOrderNo", createResult.get("outTradeNo"));
        patch.put("lastOrderDate", order.get("date"));
        patch.put("lastDoctorName", order.get("doctorName"));
        patch.put("lastSlot", order.get("slot"));
        patch.put("requiresLogin", false);

        result.setHandoffAction(HandoffAction.FINISH);
        result.setNextStage(MultiAgentStage.DONE);
        result.setReply("Registration completed successfully.");
        result.setSummary("execution_order_success");
        return result;
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return Integer.parseInt((String) value);
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? new HashMap<String, Object>() : map;
    }
}
