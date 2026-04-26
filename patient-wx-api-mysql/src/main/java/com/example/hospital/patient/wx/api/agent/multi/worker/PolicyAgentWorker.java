package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
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
            result.setReply("缺少诊室或日期信息，请先选择号源后再继续。");
            result.setSummary("policy_missing_order_fields");
            patch.put("policyChecked", false);
            patch.put("errorCode", MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH);
            patch.put("retryable", true);
            patch.put("errorMessage", "缺少诊室或日期信息，请先选择号源后再继续。");
            return result;
        }

        if (userId == null) {
            result.setHandoffAction(HandoffAction.ASK_USER);
            result.setNextStage(MultiAgentStage.POLICY_CHECK);
            result.setReply("确认挂号前请先登录小程序。");
            result.setSummary("policy_login_required");
            patch.put("requiresLogin", true);
            patch.put("awaitingConfirmation", false);
            patch.put("errorCode", MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED);
            patch.put("retryable", false);
            patch.put("errorMessage", "确认挂号前请先登录小程序。");
            return result;
        }

        boolean hasUserCard = userAgentTools.hasUserCard(userId);
        patch.put("hasUserCard", hasUserCard);
        patch.put("requiresLogin", false);
        if (!hasUserCard) {
            result.setHandoffAction(HandoffAction.ASK_USER);
            result.setNextStage(MultiAgentStage.POLICY_CHECK);
            result.setReply("挂号前需要先创建就诊卡。");
            result.setSummary("policy_user_card_required");
            patch.put("awaitingConfirmation", false);
            patch.put("errorCode", MultiAgentErrorCode.REGISTRATION_USER_CARD_REQUIRED);
            patch.put("retryable", false);
            patch.put("errorMessage", "挂号前需要先创建就诊卡。");
            return result;
        }

        String condition;
        try {
            condition = callWithRetry(new ReadToolCall<String>() {
                @Override
                public String call() {
                    return registrationAgentTools.checkRegistrationCondition(userId, deptSubId, date);
                }
            });
        } catch (Exception e) {
            result.setHandoffAction(HandoffAction.FAIL);
            result.setNextStage(MultiAgentStage.MANUAL_FALLBACK);
            result.setReply("挂号条件校验失败，请稍后重试。");
            result.setSummary("policy_check_failed");
            patch.put("policyChecked", false);
            patch.put("awaitingConfirmation", false);
            patch.put("errorCode", MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR);
            patch.put("retryable", true);
            patch.put("errorMessage", "挂号条件校验失败，请稍后重试。");
            patch.put("badCaseType", "policy_tool_failed");
            return result;
        }
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
            result.setReply(StringUtils.hasText(condition) ? condition : "挂号条件校验未通过，请调整后重试。");
            result.setSummary("policy_check_failed");
            patch.put("awaitingConfirmation", false);
            patch.put("errorCode", resolveConditionErrorCode(condition));
            patch.put("retryable", false);
            patch.put("errorMessage", StringUtils.hasText(condition) ? condition : "挂号条件校验未通过，请调整后重试。");
            return result;
        }

        boolean confirmed = booleanValue(order.get("confirmed"));
        boolean awaitingConfirmation = booleanValue(memory.get("awaitingConfirmation"));
        patch.put("pendingOrder", order);
        if (!confirmed) {
            result.setHandoffAction(HandoffAction.ASK_USER);
            result.setNextStage(MultiAgentStage.CONFIRM_WAIT);
            result.setReply("挂号条件校验通过，请确认是否提交挂号。");
            result.setSummary("policy_waiting_confirmation");
            patch.put("awaitingConfirmation", true);
            patch.put("errorCode", null);
            patch.put("retryable", null);
            patch.put("errorMessage", null);
            return result;
        }
        if (!awaitingConfirmation) {
            result.setHandoffAction(HandoffAction.FAIL);
            result.setNextStage(MultiAgentStage.MANUAL_FALLBACK);
            result.setReply("当前确认信息已失效，请重新选择号源后再试。");
            result.setSummary("policy_check_failed");
            patch.put("awaitingConfirmation", false);
            patch.put("pendingOrder", null);
            patch.put("errorCode", MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH);
            patch.put("retryable", true);
            patch.put("errorMessage", "当前确认信息已失效，请重新选择号源后再试。");
            patch.put("badCaseType", "confirmation_mismatch");
            return result;
        }

        patch.put("awaitingConfirmation", false);
        patch.put("errorCode", null);
        patch.put("retryable", null);
        patch.put("errorMessage", null);
        result.setHandoffAction(HandoffAction.HANDOFF);
        result.setNextStage(MultiAgentStage.EXECUTE_APPOINTMENT);
        result.setReply("挂号条件校验通过，开始为你提交挂号。");
        result.setSummary("policy_check_passed");
        return result;
    }

    private String resolveConditionErrorCode(String condition) {
        if ("已经达到当天挂号上限".equals(condition)) {
            return MultiAgentErrorCode.REGISTRATION_DAILY_LIMIT_REACHED;
        }
        if ("已经挂过该诊室的号".equals(condition)) {
            return MultiAgentErrorCode.REGISTRATION_REPEAT_IN_DAY;
        }
        return MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH;
    }

    private interface ReadToolCall<T> {
        T call();
    }

    private <T> T callWithRetry(ReadToolCall<T> call) {
        RuntimeException last = null;
        for (int i = 0; i < 2; i++) {
            try {
                return call.call();
            } catch (RuntimeException e) {
                last = e;
            }
        }
        throw last == null ? new RuntimeException("policy read tool failed") : last;
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
