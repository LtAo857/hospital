package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.multi.support.MultiAgentRegistrationPayloadValidator;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ExecutionAgentWorker implements AgentWorker {
    private final RegistrationAgentTools registrationAgentTools;
    private final MultiAgentRegistrationPayloadValidator payloadValidator = new MultiAgentRegistrationPayloadValidator();

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
        Map<String, Object> pendingOrder = memory.get("pendingOrder") instanceof Map
                ? new HashMap<>((Map<String, Object>) memory.get("pendingOrder"))
                : new HashMap<String, Object>();

        Map<String, Object> order = new HashMap<>();
        order.putAll(pendingOrder);
        order.putAll(payload);
        if (StringUtils.hasText(context.getSessionId())) {
            order.put("sessionId", context.getSessionId());
        }
        String requestId = buildRequestId(context, order);
        if (StringUtils.hasText(requestId)) {
            order.put("requestId", requestId);
        }

        AgentResult result = new AgentResult();
        result.setAgent("execution-agent");
        result.setMemoryPatch(patch);
        result.setToolName("createRegistrationOrder");
        result.setToolInput(new HashMap<>(order));
        result.setConfidence(0.88d);

        if (context.getUserId() == null) {
            return fail(result, patch, MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED, "未登录，暂时无法提交挂号。", false, false, true);
        }
        if (pendingOrder.isEmpty() || !booleanValue(order.get("confirmed"), false)) {
            patch.put("badCaseType", "confirmation_mismatch");
            return fail(result, patch, MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, "当前确认信息已失效，请重新选择号源后再试。", true, true, false);
        }
        MultiAgentRegistrationPayloadValidator.ValidationResult validationResult = payloadValidator.validateExecutionOrder(order);
        if (!validationResult.isValid()) {
            patch.put("badCaseType", validationResult.getBadCaseType());
            patch.put("badFields", validationResult.getBadFields());
            return fail(result, patch, MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, validationResult.getMessage(), true, true, false);
        }
        order.clear();
        order.putAll(validationResult.getNormalized());
        if (!payloadValidator.matchesPendingOrder(order, pendingOrder)) {
            patch.put("badCaseType", "confirmation_mismatch");
            return fail(result, patch, MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, "当前确认信息已变化，请重新选择号源后再试。", true, true, false);
        }
        if (StringUtils.hasText(context.getSessionId())) {
            order.put("sessionId", context.getSessionId());
        }
        requestId = buildRequestId(context, order);
        if (StringUtils.hasText(requestId)) {
            order.put("requestId", requestId);
        }
        result.setToolInput(new HashMap<>(order));

        try {
            HashMap createResult = registrationAgentTools.createRegistrationOrder(context.getUserId(), order);
            Map<String, Object> observation = new HashMap<>();
            observation.put("result", createResult);
            result.setObservation(observation);

            if (createResult == null) {
                return fail(result, patch, MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, "挂号提交失败，请稍后重试。", true, false, false);
            }
            if (createResult.containsKey("outTradeNo")) {
                patch.put("pendingOrder", null);
                patch.put("awaitingConfirmation", false);
                patch.put("lastOrderNo", createResult.get("outTradeNo"));
                patch.put("lastOrderDate", order.get("date"));
                patch.put("lastDoctorName", order.get("doctorName"));
                patch.put("lastSlot", order.get("slot"));
                patch.put("requiresLogin", false);
                patch.put("errorCode", null);
                patch.put("retryable", null);
                patch.put("errorMessage", null);

                result.setHandoffAction(HandoffAction.FINISH);
                result.setNextStage(MultiAgentStage.DONE);
                result.setReply("挂号成功，系统已为你生成挂号记录。");
                result.setSummary("execution_order_success");
                return result;
            }
            String errorCode = stringValue(createResult.get("errorCode"));
            String message = stringValue(createResult.get("message"));
            boolean retryable = booleanValue(createResult.get("retryable"), true);
            if (!StringUtils.hasText(errorCode)) {
                errorCode = MultiAgentErrorCode.REGISTRATION_SLOT_CHANGED;
            }
            if (!StringUtils.hasText(message)) {
                message = "该号源已不可用，请重新选择其他时段。";
            }
            return fail(result, patch, errorCode, message, retryable, shouldClearPendingOrder(errorCode), false);
        } catch (HospitalException e) {
            return fail(result, patch, MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, StringUtils.hasText(e.getMsg()) ? e.getMsg() : "挂号提交失败，请稍后重试。", true, false, false);
        } catch (Exception e) {
            return fail(result, patch, MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, "挂号提交失败，请稍后重试。", true, false, false);
        }
    }

    private AgentResult fail(AgentResult result,
                             Map<String, Object> patch,
                             String errorCode,
                             String message,
                             boolean retryable,
                             boolean clearPendingOrder,
                             boolean requiresLogin) {
        result.setHandoffAction(HandoffAction.FAIL);
        result.setNextStage(MultiAgentStage.MANUAL_FALLBACK);
        result.setReply(message);
        result.setSummary(summaryByErrorCode(errorCode));
        patch.put("errorCode", errorCode);
        patch.put("retryable", retryable);
        patch.put("errorMessage", message);
        patch.put("requiresLogin", requiresLogin);
        patch.put("awaitingConfirmation", false);
        if (clearPendingOrder) {
            patch.put("pendingOrder", null);
        }
        return result;
    }

    private boolean shouldClearPendingOrder(String errorCode) {
        return !MultiAgentErrorCode.REGISTRATION_DUPLICATE_SUBMIT.equals(errorCode)
                && !MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED.equals(errorCode)
                && !MultiAgentErrorCode.REGISTRATION_USER_CARD_REQUIRED.equals(errorCode);
    }

    private String summaryByErrorCode(String errorCode) {
        if (MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED.equals(errorCode)) {
            return "execution_login_required";
        }
        if (MultiAgentErrorCode.REGISTRATION_DUPLICATE_SUBMIT.equals(errorCode)) {
            return "execution_duplicate_submit";
        }
        if (MultiAgentErrorCode.REGISTRATION_SLOT_EXHAUSTED.equals(errorCode)) {
            return "execution_slot_exhausted";
        }
        if (MultiAgentErrorCode.REGISTRATION_SLOT_CHANGED.equals(errorCode)) {
            return "execution_slot_changed";
        }
        if (MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH.equals(errorCode)) {
            return "execution_param_mismatch";
        }
        if (MultiAgentErrorCode.REGISTRATION_DB_WRITE_FAILED.equals(errorCode)) {
            return "execution_db_write_failed";
        }
        if (MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR.equals(errorCode)) {
            return "execution_system_error";
        }
        return "execution_order_failed";
    }

    private String buildRequestId(AgentContext context, Map<String, Object> order) {
        String existing = stringValue(order.get("requestId"));
        if (StringUtils.hasText(existing)) {
            return existing;
        }
        Integer userId = context.getUserId();
        Integer scheduleId = intValue(order.get("scheduleId"));
        String date = stringValue(order.get("date"));
        Integer slot = intValue(order.get("slot"));
        if (userId == null || scheduleId == null || !StringUtils.hasText(date) || slot == null || !StringUtils.hasText(context.getSessionId())) {
            return context.getRequestId();
        }
        String seed = context.getSessionId() + "|" + userId + "|" + scheduleId + "|" + date + "|" + slot;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "").toUpperCase();
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
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
