package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentParser;
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentResult;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import com.example.hospital.patient.wx.api.agent.support.AgentUiAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TriageAgentWorker implements AgentWorker {
    private static final Pattern DATE_HINT_PATTERN = Pattern.compile("(\\u4eca\\u5929|\\u660e\\u5929|\\u540e\\u5929|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}\\u6708\\d{1,2}\\u65e5)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired(required = false)
    private ModelIntentParser modelIntentParser;

    @Override
    public MultiAgentStage stage() {
        return MultiAgentStage.INTENT_PARSE;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Map<String, Object> payload = safeMap(context.getPayload());
        Map<String, Object> memory = safeMap(context.getMemory());
        Map<String, Object> patch = new HashMap<>();

        String message = context.getUserMessage();
        String action = context.getUserAction();
        String date = firstText(payload.get("date"), parseDateHint(message));
        if (StringUtils.hasText(date)) {
            patch.put("date", date);
        }
        Optional<ModelIntentResult> modelIntent = parseModelIntent(message, context.getSessionId(), patch);
        patch.put("errorCode", null);
        patch.put("retryable", null);
        patch.put("errorMessage", null);
        patch.put("requestedView", null);

        AgentResult result = new AgentResult();
        result.setAgent("triage-agent");
        result.setMemoryPatch(patch);
        result.setConfidence(0.78d);

        if (hasModelIntent(modelIntent, "dangerous")) {
            result.setHandoffAction(HandoffAction.FINISH);
            result.setNextStage(MultiAgentStage.DONE);
            result.setReply("无法处理该请求，请重新输入。");
            result.setSummary("dangerous_intent_blocked");
            result.setConfidence(0.99d);
            return result;
        }

        if (isDirectCreate(action, payload)) {
            patch.put("confirmed", true);
            result.setHandoffAction(HandoffAction.HANDOFF);
            result.setNextStage(MultiAgentStage.POLICY_CHECK);
            result.setReply("已识别到确认提交，开始校验挂号条件。");
            result.setSummary("direct_create_detected");
            return result;
        }

        if (isViewMessagesIntent(action, message, modelIntent)) {
            result.setHandoffAction(HandoffAction.FINISH);
            result.setNextStage(MultiAgentStage.DONE);
            result.setReply("可以直接去消息中心查看挂号提醒和系统通知。若你想继续挂号，也可以告诉我科室和日期。");
            result.setSummary("view_messages_requested");
            patch.put("requestedView", AgentUiAction.VIEW_MESSAGES);
            return result;
        }

        if (isViewUserCardIntent(action, message, modelIntent)) {
            result.setHandoffAction(HandoffAction.FINISH);
            result.setNextStage(MultiAgentStage.DONE);
            result.setReply("可以先查看就诊卡状态；如果还没建卡，我也会给你补建卡入口。");
            result.setSummary("view_user_card_requested");
            patch.put("requestedView", AgentUiAction.VIEW_USER_CARD);
            return result;
        }

        if (isViewRegistrationsIntent(action, message)) {
            result.setHandoffAction(HandoffAction.FINISH);
            result.setNextStage(MultiAgentStage.DONE);
            result.setReply("可以直接去“我的挂号”查看已有预约记录。若要继续挂号，也可以告诉我科室和日期。 ");
            result.setSummary("view_registrations_requested");
            patch.put("requestedView", AgentUiAction.VIEW_REGISTRATIONS);
            return result;
        }

        if (isExplainIntent(action, message, memory, modelIntent)) {
            result.setHandoffAction(HandoffAction.FINISH);
            result.setNextStage(MultiAgentStage.DONE);
            result.setReply("我先结合当前挂号上下文和知识库为你解释一下。");
            result.setSummary("explanation_requested");
            patch.put("requestedView", AgentUiAction.EXPLAIN_RECOMMENDATION);
            patch.put("ragQuestion", firstText(message, "为什么推荐当前结果"));
            return result;
        }

        if (isRegistrationIntent(action, message, payload, memory, modelIntent)) {
            result.setHandoffAction(HandoffAction.HANDOFF);
            result.setNextStage(MultiAgentStage.SLOT_QUERY);
            result.setReply("已识别挂号需求，开始为你查询号源。");
            result.setSummary("registration_intent_detected");
            return result;
        }

        result.setHandoffAction(HandoffAction.ASK_USER);
        result.setNextStage(MultiAgentStage.INTENT_PARSE);
        result.setReply("当前多 Agent 仅支持挂号相关操作，请告诉我科室和日期。");
        result.setSummary("intent_not_supported");
        result.setConfidence(0.5d);
        return result;
    }

    private boolean isRegistrationIntent(String action, String message, Map<String, Object> payload, Map<String, Object> memory, Optional<ModelIntentResult> modelIntent) {
        if (hasModelIntent(modelIntent, "registration", "query_doctor")) {
            return true;
        }
        if (AgentAction.CREATE_REGISTRATION.equals(action)
                || AgentUiAction.START_REGISTRATION.equals(action)
                || AgentUiAction.SELECT_SUB_DEPT.equals(action)
                || AgentUiAction.SELECT_DATE.equals(action)
                || AgentUiAction.SELECT_DOCTOR.equals(action)
                || AgentUiAction.SELECT_SLOT.equals(action)) {
            return true;
        }
        if (payload.get("deptSubId") != null || payload.get("deptId") != null || payload.get("doctorId") != null) {
            return true;
        }
        if (memory.get("pendingOrder") instanceof Map && !((Map<?, ?>) memory.get("pendingOrder")).isEmpty()) {
            return true;
        }
        return containsAny(message, "registration", "register", "挂号", "预约", "科室", "诊室", "医生", "号源", "内科", "外科", "骨科", "儿科", "口腔科", "眼科", "耳鼻喉科", "皮肤科", "妇科", "产科", "神经内科", "神经外科", "肿瘤科", "康复科");
    }

    private boolean isViewMessagesIntent(String action, String message, Optional<ModelIntentResult> modelIntent) {
        if (AgentUiAction.VIEW_MESSAGES.equals(action)) {
            return true;
        }
        if (hasModelIntent(modelIntent, "query_message")) {
            return true;
        }
        return containsAny(message, "消息", "通知", "提醒", "消息中心");
    }

    private boolean isViewUserCardIntent(String action, String message, Optional<ModelIntentResult> modelIntent) {
        if (AgentUiAction.VIEW_USER_CARD.equals(action)) {
            return true;
        }
        if (hasModelIntent(modelIntent, "query_user_card")) {
            return true;
        }
        return containsAny(message, "就诊卡", "建卡", "实名", "身份信息");
    }

    private boolean isViewRegistrationsIntent(String action, String message) {
        if (AgentUiAction.VIEW_REGISTRATIONS.equals(action)) {
            return true;
        }
        return containsAny(message, "我的挂号", "挂号记录", "预约记录", "查看挂号", "我的预约");
    }

    private boolean isExplainIntent(String action, String message, Map<String, Object> memory, Optional<ModelIntentResult> modelIntent) {
        if (AgentUiAction.EXPLAIN_RECOMMENDATION.equals(action)) {
            return true;
        }
        if (hasModelIntent(modelIntent, "explain_recommendation")) {
            return true;
        }
        if (containsAny(message, "挂号规则", "就诊须知", "医保", "普通挂号", "为什么推荐", "推荐理由", "解释一下", "怎么看出来")) {
            return true;
        }
        if (!(memory.get("pendingOrder") instanceof Map) && memory.get("doctorName") == null && memory.get("deptSubName") == null) {
            return false;
        }
        return containsAny(message, "为什么", "为啥", "推荐理由", "为什么推荐", "解释一下", "怎么看出来");
    }

    private boolean isDirectCreate(String action, Map<String, Object> payload) {
        if (AgentAction.CREATE_REGISTRATION.equals(action)) {
            return true;
        }
        if (booleanValue(payload.get("confirmed"))) {
            return true;
        }
        return payload.get("workPlanId") != null
                && payload.get("scheduleId") != null
                && payload.get("doctorId") != null
                && payload.get("deptSubId") != null
                && payload.get("date") != null;
    }

    private Optional<ModelIntentResult> parseModelIntent(String message, String sessionId, Map<String, Object> patch) {
        if (modelIntentParser == null || !StringUtils.hasText(message)) {
            return Optional.empty();
        }
        Optional<ModelIntentResult> parsed = modelIntentParser.parse(message, sessionId);
        if (!parsed.isPresent()) {
            return Optional.empty();
        }
        ModelIntentResult result = parsed.get();
        patch.put("nluIntent", result.getIntent());
        patch.put("nluConfidence", result.getConfidence());
        patch.put("nluSource", firstText(result.getSource(), result.getEngine(), "model"));
        patch.put("nluModel", result.getModel());
        patch.put("nluLatencyMs", result.getLatencyMs());

        Map<String, Object> slots = safeMap(result.getSlots());
        putTextIfAbsent(patch, "symptom", slots.get("symptom"));
        putTextIfAbsent(patch, "deptSubName", slots.get("department"));
        putTextIfAbsent(patch, "doctorName", slots.get("doctorName"));
        putTextIfAbsent(patch, "date", normalizeDate(firstText(slots.get("date"))));
        putTextIfAbsent(patch, "timePreference", slots.get("timePreference"));
        return parsed;
    }

    private boolean hasModelIntent(Optional<ModelIntentResult> modelIntent, String... intents) {
        if (!modelIntent.isPresent() || intents == null) {
            return false;
        }
        String intent = modelIntent.get().getIntent();
        if (!StringUtils.hasText(intent)) {
            return false;
        }
        for (String expected : intents) {
            if (intent.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private void putTextIfAbsent(Map<String, Object> patch, String key, Object value) {
        if (patch.get(key) != null) {
            return;
        }
        String text = firstText(value);
        if (StringUtils.hasText(text)) {
            patch.put(key, text);
        }
    }

    private String normalizeDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String parsed = parseDateHint(value);
        return StringUtils.hasText(parsed) ? parsed : value;
    }

    private String parseDateHint(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = DATE_HINT_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        String token = matcher.group(1);
        if ("\u4eca\u5929".equals(token)) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
        if ("\u660e\u5929".equals(token)) {
            return LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        }
        if ("\u540e\u5929".equals(token)) {
            return LocalDate.now().plusDays(2).format(DATE_FORMATTER);
        }
        if (token.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return token;
        }
        String[] parts = token.replace("\u65e5", "").split("\u6708");
        if (parts.length != 2) {
            return null;
        }
        int month = Integer.parseInt(parts[0]);
        int day = Integer.parseInt(parts[1]);
        return LocalDate.of(LocalDate.now().getYear(), month, day).format(DATE_FORMATTER);
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? new HashMap<String, Object>() : map;
    }
}
