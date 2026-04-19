package com.example.hospital.patient.wx.api.agent.multi.service;

import cn.hutool.core.util.IdUtil;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.dto.AgentConfirmation;
import com.example.hospital.patient.wx.api.agent.dto.AgentFlowItem;
import com.example.hospital.patient.wx.api.agent.dto.AgentResponseCard;
import com.example.hospital.patient.wx.api.agent.dto.AgentToolLog;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.memory.MultiAgentMemoryService;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.multi.trace.AgentTraceEntry;
import com.example.hospital.patient.wx.api.agent.multi.worker.AgentWorker;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import com.example.hospital.patient.wx.api.agent.support.AgentPlanStep;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MultiAgentCoordinatorService {
    private static final String PROMPT_VERSION = "multi-agent-v1";

    private final MultiAgentProperties properties;
    private final MultiAgentMemoryService memoryService;
    private final Map<MultiAgentStage, AgentWorker> workerByStage = new EnumMap<>(MultiAgentStage.class);

    public MultiAgentCoordinatorService(MultiAgentProperties properties,
                                        MultiAgentMemoryService memoryService,
                                        List<AgentWorker> workers) {
        this.properties = properties;
        this.memoryService = memoryService;
        if (workers != null) {
            for (AgentWorker worker : workers) {
                workerByStage.put(worker.stage(), worker);
            }
        }
    }

    public AgentChatResponse chat(AgentChatRequest request, Integer userId) {
        AgentChatRequest safeRequest = request == null ? new AgentChatRequest() : request;
        String sessionId = StringUtils.hasText(safeRequest.getSessionId()) ? safeRequest.getSessionId() : IdUtil.simpleUUID();
        Map<String, Object> memory = memoryService.load(sessionId);
        if (userId != null) {
            memory.put("userId", userId);
        } else {
            memory.remove("userId");
        }

        Map<String, Object> payload = composePayload(safeRequest, memory);
        AgentContext context = new AgentContext();
        context.setSessionId(sessionId);
        context.setRequestId(IdUtil.simpleUUID());
        context.setUserId(userId);
        context.setUserMessage(safeRequest.getMessage());
        context.setUserAction(safeRequest.getAction());
        context.setPayload(payload);
        context.setMemory(memory);
        context.setTrace(new ArrayList<AgentTraceEntry>());
        context.setStage(resolveStage(memory.get("stage")));

        String finalReply = null;
        MultiAgentStage finalStage = context.getStage();
        int maxHops = properties.getMaxHops() <= 0 ? 8 : properties.getMaxHops();

        for (int i = 0; i < maxHops; i++) {
            AgentWorker worker = workerByStage.get(context.getStage());
            if (worker == null) {
                finalStage = MultiAgentStage.MANUAL_FALLBACK;
                finalReply = "当前流程暂时不可用，请稍后重试。";
                break;
            }
            AgentResult result = worker.execute(context);
            if (result == null) {
                finalStage = MultiAgentStage.MANUAL_FALLBACK;
                finalReply = "当前流程暂时不可用，请稍后重试。";
                break;
            }
            if (StringUtils.hasText(result.getReply())) {
                finalReply = result.getReply();
            }
            applyMemoryPatch(memory, result.getMemoryPatch());
            appendTrace(context, result, i + 1);

            if (result.getHandoffAction() == HandoffAction.HANDOFF && result.getNextStage() != null) {
                context.setStage(result.getNextStage());
                finalStage = result.getNextStage();
                continue;
            }
            if (result.getHandoffAction() == HandoffAction.FINISH) {
                finalStage = MultiAgentStage.DONE;
                break;
            }
            if (result.getHandoffAction() == HandoffAction.FAIL) {
                finalStage = MultiAgentStage.MANUAL_FALLBACK;
                break;
            }
            finalStage = result.getNextStage() != null ? result.getNextStage() : context.getStage();
            break;
        }

        if (!StringUtils.hasText(finalReply)) {
            finalReply = fallbackReply(finalStage);
        }
        memory.put("stage", finalStage.name());
        memory.put("lastReply", finalReply);
        memory.put("traceSize", context.getTrace() == null ? 0 : context.getTrace().size());
        memoryService.save(sessionId, memory);

        AgentChatResponse response = new AgentChatResponse();
        response.setSessionId(sessionId);
        response.setSystemPromptVersion(PROMPT_VERSION);
        response.setReply(finalReply);
        response.setState(resolveState(finalStage, memory));
        response.setAgentFlows(buildAgentFlows(context.getTrace()));
        response.setToolLogs(buildToolLogs(context.getTrace()));
        response.setSteps(buildSteps(finalStage, memory));
        response.setMemory(exposeMemory(finalStage, memory));
        response.setErrorCode(stringValue(memory.get("errorCode")));
        response.setRetryable(memory.containsKey("retryable") ? booleanValue(memory.get("retryable")) : null);
        response.setErrorMessage(stringValue(memory.get("errorMessage")));

        boolean requiresLogin = booleanValue(memory.get("requiresLogin"));
        boolean awaitingConfirmation = booleanValue(memory.get("awaitingConfirmation"));
        response.setRequiresLogin(requiresLogin);
        response.setRequiresConfirmation(awaitingConfirmation);
        if (awaitingConfirmation) {
            AgentConfirmation confirmation = buildConfirmation(memory);
            response.setConfirmation(confirmation);
            appendConfirmationCard(response, confirmation, memory);
        }
        if (requiresLogin) {
            appendNavigateCard(response, "去登录", "打开个人中心登录后继续挂号", "/pages/mine/mine", "登录");
        }
        appendFallbackCards(response, memory);
        return response;
    }

    private void appendFallbackCards(AgentChatResponse response, Map<String, Object> memory) {
        String errorCode = stringValue(memory.get("errorCode"));
        if (!StringUtils.hasText(errorCode)) {
            return;
        }
        if (MultiAgentErrorCode.REGISTRATION_LOGIN_REQUIRED.equals(errorCode)) {
            appendNavigateCard(response, "去登录", "登录后继续当前挂号流程", "/pages/mine/mine", "登录");
            return;
        }
        if (MultiAgentErrorCode.REGISTRATION_USER_CARD_REQUIRED.equals(errorCode)) {
            appendNavigateCard(response, "去建卡", "先完善就诊卡信息，再继续挂号", "/user/fill_user_info/fill_user_info", "建卡");
            return;
        }
        if (MultiAgentErrorCode.REGISTRATION_SLOT_CHANGED.equals(errorCode)
                || MultiAgentErrorCode.REGISTRATION_SLOT_EXHAUSTED.equals(errorCode)
                || MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH.equals(errorCode)) {
            appendNavigateCard(response, "重新选号", "当前号源已变化，请重新选择挂号时段", "/registration/medical_dept_list/medical_dept_list", "重选");
            return;
        }
        if (MultiAgentErrorCode.REGISTRATION_DAILY_LIMIT_REACHED.equals(errorCode)
                || MultiAgentErrorCode.REGISTRATION_REPEAT_IN_DAY.equals(errorCode)) {
            appendNavigateCard(response, "查看我的挂号", "当前限制已生效，可查看已有挂号记录", "/pages/registration_list/registration_list", "结果");
            return;
        }
        if (MultiAgentErrorCode.REGISTRATION_DUPLICATE_SUBMIT.equals(errorCode)) {
            appendNavigateCard(response, "查看我的挂号", "若已提交成功，可到我的挂号查看结果", "/pages/registration_list/registration_list", "结果");
            return;
        }
        appendNavigateCard(response, "普通挂号", "多 Agent 流程未完成，可改走普通挂号流程", "/registration/notice/notice", "兜底");
    }

    private AgentConfirmation buildConfirmation(Map<String, Object> memory) {
        Object pending = memory.get("pendingOrder");
        if (!(pending instanceof Map)) {
            return null;
        }
        Map<String, Object> payload = new HashMap<>((Map<String, Object>) pending);
        payload.put("confirmed", true);
        AgentConfirmation confirmation = new AgentConfirmation();
        confirmation.setAction(AgentAction.CREATE_REGISTRATION);
        confirmation.setLabel("确认挂号");
        confirmation.setPayload(payload);
        return confirmation;
    }

    private void appendConfirmationCard(AgentChatResponse response, AgentConfirmation confirmation, Map<String, Object> memory) {
        if (confirmation == null || confirmation.getPayload() == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>(confirmation.getPayload());
        AgentResponseCard card = new AgentResponseCard();
        card.setType("action");
        card.setTitle(buildConfirmationTitle(payload));
        card.setDescription(buildConfirmationDescription(payload, memory));
        card.setBadge("待确认");
        card.setAction(confirmation.getAction());
        card.setPayload(payload);
        response.getCards().add(card);
    }

    private void appendNavigateCard(AgentChatResponse response, String title, String description, String url, String badge) {
        if (response.getCards() != null) {
            for (AgentResponseCard existing : response.getCards()) {
                if (existing == null || !"navigate".equals(existing.getAction()) || existing.getPayload() == null) {
                    continue;
                }
                Object existingUrl = existing.getPayload().get("url");
                if (title.equals(existing.getTitle()) && url.equals(existingUrl)) {
                    return;
                }
            }
        }
        AgentResponseCard card = new AgentResponseCard();
        card.setType("action");
        card.setTitle(title);
        card.setDescription(description);
        card.setBadge(badge);
        card.setAction("navigate");
        Map<String, Object> payload = new HashMap<>();
        payload.put("url", url);
        card.setPayload(payload);
        response.getCards().add(card);
    }

    private String buildConfirmationTitle(Map<String, Object> payload) {
        String doctorName = stringValue(payload.get("doctorName"));
        String slot = stringValue(payload.get("slot"));
        if (StringUtils.hasText(doctorName) && StringUtils.hasText(slot)) {
            return doctorName + " / 时段" + slot;
        }
        if (StringUtils.hasText(doctorName)) {
            return doctorName + " / 确认挂号";
        }
        return "确认挂号";
    }

    private String buildConfirmationDescription(Map<String, Object> payload, Map<String, Object> memory) {
        String deptName = firstText(payload.get("deptSubName"), payload.get("deptName"), memory.get("deptSubName"), memory.get("deptName"));
        String date = firstText(payload.get("date"), memory.get("date"));
        String amount = firstText(payload.get("amount"));
        StringBuilder builder = new StringBuilder();
        builder.append("科室：").append(StringUtils.hasText(deptName) ? deptName : "--");
        builder.append("，日期：").append(StringUtils.hasText(date) ? date : "--");
        if (StringUtils.hasText(amount)) {
            builder.append("，挂号费：").append(amount);
        }
        return builder.toString();
    }

    private String resolveState(MultiAgentStage stage, Map<String, Object> memory) {
        if (booleanValue(memory.get("requiresLogin"))) {
            return "need_login";
        }
        if (booleanValue(memory.get("awaitingConfirmation"))) {
            return "awaiting_confirmation";
        }
        if (stage == MultiAgentStage.DONE) {
            return "completed";
        }
        if (stage == MultiAgentStage.MANUAL_FALLBACK) {
            return "fallback";
        }
        return stage.name().toLowerCase();
    }

    private List<AgentToolLog> buildToolLogs(List<AgentTraceEntry> trace) {
        if (trace == null || trace.isEmpty()) {
            return Collections.emptyList();
        }
        List<AgentToolLog> logs = new ArrayList<>();
        for (AgentTraceEntry entry : trace) {
            if (!StringUtils.hasText(entry.getToolName())) {
                continue;
            }
            AgentToolLog log = new AgentToolLog();
            log.setName(toolNameText(entry.getToolName()));
            log.setStatus(entry.getHandoffAction() == HandoffAction.FAIL ? "error" : "success");
            log.setSummary(summaryText(entry.getSummary()));
            logs.add(log);
        }
        return logs;
    }

    private List<AgentFlowItem> buildAgentFlows(List<AgentTraceEntry> trace) {
        if (trace == null || trace.isEmpty()) {
            return Collections.emptyList();
        }
        List<AgentFlowItem> flows = new ArrayList<>();
        for (AgentTraceEntry entry : trace) {
            AgentFlowItem item = new AgentFlowItem();
            item.setKey("agent-flow-" + entry.getSeq());
            item.setTitle(agentTitle(entry.getAgent()));
            item.setStage(stageText(entry.getStage()));
            item.setStatus(flowStatus(entry.getHandoffAction()));
            item.setSummary(buildFlowSummary(entry));
            item.setHandoffAction(handoffText(entry.getHandoffAction()));
            item.setToolCount(StringUtils.hasText(entry.getToolName()) ? 1 : 0);
            flows.add(item);
        }
        return flows;
    }

    private List<AgentPlanStep> buildSteps(MultiAgentStage stage, Map<String, Object> memory) {
        List<AgentPlanStep> steps = new ArrayList<>();
        boolean hasPendingOrder = memory.get("pendingOrder") instanceof Map && !((Map<?, ?>) memory.get("pendingOrder")).isEmpty();
        boolean policyChecked = booleanValue(memory.get("policyChecked"));
        boolean done = stage == MultiAgentStage.DONE;
        steps.add(new AgentPlanStep("triage", "识别需求", stage == MultiAgentStage.INTENT_PARSE ? "in_progress" : "completed"));
        steps.add(new AgentPlanStep("slot", "查询号源", hasPendingOrder || done ? "completed" : "pending"));
        steps.add(new AgentPlanStep("policy", "校验条件", policyChecked || done ? "completed" : "pending"));
        steps.add(new AgentPlanStep("execute", "提交挂号", done ? "completed" : "pending"));
        return steps;
    }

    private Map<String, Object> exposeMemory(MultiAgentStage stage, Map<String, Object> memory) {
        Map<String, Object> result = new HashMap<>();
        result.put("stage", stage.name());
        result.put("deptId", memory.get("deptId"));
        result.put("deptSubId", memory.get("deptSubId"));
        result.put("date", memory.get("date"));
        result.put("doctorId", memory.get("doctorId"));
        result.put("hasPendingOrder", memory.get("pendingOrder") instanceof Map && !((Map<?, ?>) memory.get("pendingOrder")).isEmpty());
        result.put("awaitingConfirmation", booleanValue(memory.get("awaitingConfirmation")));
        result.put("requiresLogin", booleanValue(memory.get("requiresLogin")));
        result.put("errorCode", memory.get("errorCode"));
        result.put("retryable", memory.get("retryable"));
        result.put("errorMessage", memory.get("errorMessage"));
        result.put("traceSize", memory.get("traceSize"));
        return result;
    }

    private void appendTrace(AgentContext context, AgentResult result, long seq) {
        if (context.getTrace() == null) {
            context.setTrace(new ArrayList<AgentTraceEntry>());
        }
        AgentTraceEntry entry = new AgentTraceEntry();
        entry.setSeq(seq);
        entry.setAgent(result.getAgent());
        entry.setStage(context.getStage());
        entry.setHandoffAction(result.getHandoffAction());
        entry.setToolName(result.getToolName());
        entry.setSummary(result.getSummary());
        entry.setAt(LocalDateTime.now());
        entry.setObservation(result.getObservation());
        context.getTrace().add(entry);
    }

    private void applyMemoryPatch(Map<String, Object> memory, Map<String, Object> patch) {
        if (patch == null || patch.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : patch.entrySet()) {
            if (entry.getValue() == null) {
                memory.remove(entry.getKey());
            } else {
                memory.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> composePayload(AgentChatRequest request, Map<String, Object> memory) {
        Map<String, Object> payload = new HashMap<>();
        if (request != null && request.getPayload() != null) {
            payload.putAll(request.getPayload());
        }
        if (memory.get("pendingOrder") instanceof Map) {
            Map<String, Object> pendingOrder = (Map<String, Object>) memory.get("pendingOrder");
            for (Map.Entry<String, Object> entry : pendingOrder.entrySet()) {
                if (!payload.containsKey(entry.getKey()) || payload.get(entry.getKey()) == null) {
                    payload.put(entry.getKey(), entry.getValue());
                }
            }
        }
        mergeFromMemory(payload, memory, "deptId");
        mergeFromMemory(payload, memory, "deptSubId");
        mergeFromMemory(payload, memory, "date");
        mergeFromMemory(payload, memory, "doctorId");
        mergeFromMemory(payload, memory, "doctorName");
        mergeFromMemory(payload, memory, "deptName");
        mergeFromMemory(payload, memory, "deptSubName");
        if (request != null && AgentAction.CREATE_REGISTRATION.equals(request.getAction()) && payload.get("confirmed") == null) {
            payload.put("confirmed", true);
        }
        return payload;
    }

    private void mergeFromMemory(Map<String, Object> payload, Map<String, Object> memory, String key) {
        if (!payload.containsKey(key) && memory.containsKey(key)) {
            payload.put(key, memory.get(key));
        }
    }

    private MultiAgentStage resolveStage(Object value) {
        String stage = value == null ? null : String.valueOf(value);
        if (!StringUtils.hasText(stage)) {
            return MultiAgentStage.INTENT_PARSE;
        }
        try {
            MultiAgentStage resolved = MultiAgentStage.valueOf(stage);
            if (resolved == MultiAgentStage.CONFIRM_WAIT) {
                return MultiAgentStage.POLICY_CHECK;
            }
            if (resolved == MultiAgentStage.DONE || resolved == MultiAgentStage.MANUAL_FALLBACK) {
                return MultiAgentStage.INTENT_PARSE;
            }
            return resolved;
        } catch (IllegalArgumentException ignored) {
            if ("awaiting_confirmation".equals(stage) || "await_confirm".equals(stage)) {
                return MultiAgentStage.POLICY_CHECK;
            }
            return MultiAgentStage.INTENT_PARSE;
        }
    }

    private String fallbackReply(MultiAgentStage stage) {
        if (stage == MultiAgentStage.MANUAL_FALLBACK) {
            return "当前多 Agent 流程执行失败，请重新选择号源后再试。";
        }
        return "请告诉我想挂哪个科室、哪一天，我继续帮你查询号源。";
    }

    private String toolNameText(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return "工具执行";
        }
        switch (toolName) {
            case "searchScheduleSlots":
                return "查询号源时段";
            case "checkRegistrationCondition":
                return "校验挂号条件";
            case "createRegistrationOrder":
                return "提交挂号";
            default:
                return toolName;
        }
    }

    private String summaryText(String summary) {
        if (!StringUtils.hasText(summary)) {
            return "已完成";
        }
        switch (summary) {
            case "direct_create_detected":
                return "已识别确认提交动作";
            case "registration_intent_detected":
                return "已识别挂号需求";
            case "intent_not_supported":
                return "当前仅支持挂号相关操作";
            case "missing_slots_input":
                return "缺少诊室或日期信息";
            case "no_slot_available":
                return "当前没有可用号源";
            case "slot_selected":
                return "已选中可挂号源";
            case "policy_missing_order_fields":
                return "挂号参数不完整";
            case "policy_login_required":
                return "需要先登录后继续";
            case "policy_user_card_required":
                return "需要先创建就诊卡";
            case "policy_check_failed":
                return "挂号条件校验未通过";
            case "policy_waiting_confirmation":
                return "等待确认提交挂号";
            case "policy_check_passed":
                return "挂号条件校验通过";
            case "execution_login_required":
                return "未登录，无法提交挂号";
            case "execution_missing_order_fields":
                return "缺少挂号参数，无法提交";
            case "execution_order_failed":
                return "号源已不可用，请重新选择";
            case "execution_duplicate_submit":
                return "挂号请求正在处理中，请勿重复提交";
            case "execution_slot_exhausted":
                return "当前号源已满，请重新选择";
            case "execution_slot_changed":
                return "号源信息已变化，请重新选择";
            case "execution_param_mismatch":
                return "挂号参数已失效，请重新选择";
            case "execution_db_write_failed":
                return "挂号写库失败，已进入补偿处理";
            case "execution_system_error":
                return "系统忙碌，请稍后重试";
            case "execution_order_success":
                return "挂号提交成功";
            default:
                return summary;
        }
    }

    private String agentTitle(String agent) {
        if (!StringUtils.hasText(agent)) {
            return "流程节点";
        }
        switch (agent) {
            case "triage-agent":
                return "意图识别 Agent";
            case "schedule-agent":
                return "号源查询 Agent";
            case "policy-agent":
                return "条件校验 Agent";
            case "execution-agent":
                return "挂号执行 Agent";
            default:
                return agent;
        }
    }

    private String stageText(MultiAgentStage stage) {
        if (stage == null) {
            return "";
        }
        switch (stage) {
            case INTENT_PARSE:
                return "识别需求";
            case SLOT_QUERY:
                return "查询号源";
            case POLICY_CHECK:
            case CONFIRM_WAIT:
                return "校验条件";
            case EXECUTE_APPOINTMENT:
                return "提交挂号";
            case DONE:
                return "已完成";
            case MANUAL_FALLBACK:
                return "人工兜底";
            default:
                return stage.name();
        }
    }

    private String handoffText(HandoffAction action) {
        if (action == null) {
            return "";
        }
        switch (action) {
            case HANDOFF:
                return "继续流转";
            case ASK_USER:
                return "等待补充信息";
            case FINISH:
                return "执行完成";
            case FAIL:
                return "执行失败";
            default:
                return action.name();
        }
    }

    private String flowStatus(HandoffAction action) {
        if (action == null) {
            return "pending";
        }
        switch (action) {
            case FAIL:
                return "failed";
            case ASK_USER:
                return "waiting";
            case HANDOFF:
            case FINISH:
                return "completed";
            default:
                return "pending";
        }
    }

    private String buildFlowSummary(AgentTraceEntry entry) {
        String summary = summaryText(entry.getSummary());
        String stage = stageText(entry.getStage());
        String handoff = handoffText(entry.getHandoffAction());
        if (StringUtils.hasText(stage) && StringUtils.hasText(handoff)) {
            return stage + "，" + summary + "，" + handoff;
        }
        if (StringUtils.hasText(stage)) {
            return stage + "，" + summary;
        }
        return summary;
    }

    private String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
}
