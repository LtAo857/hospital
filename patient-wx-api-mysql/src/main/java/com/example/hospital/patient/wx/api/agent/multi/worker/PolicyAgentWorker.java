package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.UserAgentTools;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class PolicyAgentWorker implements AgentWorker {
    private static final String PASS_KEYWORD = "\u6ee1\u8db3\u6302\u53f7\u6761\u4ef6";

    private final UserAgentTools userAgentTools;
    private final RegistrationAgentTools registrationAgentTools;

    public PolicyAgentWorker(UserAgentTools userAgentTools,
                             RegistrationAgentTools registrationAgentTools) {
        this.userAgentTools = userAgentTools;
        this.registrationAgentTools = registrationAgentTools;
    }

    @Override
    public MultiAgentStage stage() {
        return MultiAgentStage.POLICY_CHECK;
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

        Integer deptSubId = intValue(order.get("deptSubId"));
        String date = stringValue(order.get("date"));
        Integer userId = context.getUserId();

        AgentResult result = new AgentResult();
        result.setAgent("policy-agent");
        result.setMemoryPatch(patch);
        result.setConfidence(0.85d);

        if (deptSubId == null || !StringUtils.hasText(date)) {
            result.setHandoffAction(HandoffAction.ASK_USER);
            result.setNextStage(MultiAgentStage.POLICY_CHECK);
            result.setReply("Missing deptSubId or date. Please select slot first.");
            result.setSummary("policy_missing_order_fields");
            patch.put("policyChecked", false);
            return result;
        }

        if (userId == null) {
            result.setHandoffAction(HandoffAction.ASK_USER);
            result.setNextStage(MultiAgentStage.POLICY_CHECK);
            result.setReply("Please login before submitting registration.");
            result.setSummary("policy_login_required");
            patch.put("requiresLogin", true);
            patch.put("awaitingConfirmation", false);
            return result;
        }

        boolean hasUserCard = userAgentTools.hasUserCard(userId);
        patch.put("hasUserCard", hasUserCard);
        patch.put("requiresLogin", false);
        if (!hasUserCard) {
            result.setHandoffAction(HandoffAction.ASK_USER);
            result.setNextStage(MultiAgentStage.POLICY_CHECK);
            result.setReply("A valid patient card is required before registration.");
            result.setSummary("policy_user_card_required");
            patch.put("awaitingConfirmation", false);
            return result;
        }

        String condition = registrationAgentTools.checkRegistrationCondition(userId, deptSubId, date);
        patch.put("policyChecked", true);
        result.setToolName("checkRegistrationCondition");
        Map<String, Object> toolInput = new HashMap<>();
        toolInput.put("userId", userId);
        toolInput.put("deptSubId", deptSubId);
        toolInput.put("date", date);
        result.setToolInput(toolInput);
        Map<String, Object> observation = new HashMap<>();
        observation.put("condition", condition);
        result.setObservation(observation);

        if (!StringUtils.hasText(condition) || !condition.contains(PASS_KEYWORD)) {
            result.setHandoffAction(HandoffAction.FAIL);
            result.setNextStage(MultiAgentStage.MANUAL_FALLBACK);
            result.setReply(StringUtils.hasText(condition) ? condition : "Policy check failed. Please adjust and retry.");
            result.setSummary("policy_check_failed");
            patch.put("awaitingConfirmation", false);
            return result;
        }

        boolean confirmed = booleanValue(order.get("confirmed"))
                || AgentAction.CREATE_REGISTRATION.equals(context.getUserAction());
        patch.put("pendingOrder", order);
        if (!confirmed) {
            result.setHandoffAction(HandoffAction.ASK_USER);
            result.setNextStage(MultiAgentStage.CONFIRM_WAIT);
            result.setReply("Policy check passed. Please confirm submission.");
            result.setSummary("policy_waiting_confirmation");
            patch.put("awaitingConfirmation", true);
            return result;
        }

        patch.put("awaitingConfirmation", false);
        result.setHandoffAction(HandoffAction.HANDOFF);
        result.setNextStage(MultiAgentStage.EXECUTE_APPOINTMENT);
        result.setReply("Policy check passed. Start execution.");
        result.setSummary("policy_check_passed");
        return result;
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
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
