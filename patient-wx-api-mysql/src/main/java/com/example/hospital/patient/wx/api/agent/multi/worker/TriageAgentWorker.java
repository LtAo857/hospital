package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import com.example.hospital.patient.wx.api.agent.support.AgentUiAction;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TriageAgentWorker implements AgentWorker {
    private static final Pattern DATE_HINT_PATTERN = Pattern.compile("(\\u4eca\\u5929|\\u660e\\u5929|\\u540e\\u5929|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}\\u6708\\d{1,2}\\u65e5)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
        patch.put("errorCode", null);
        patch.put("retryable", null);
        patch.put("errorMessage", null);

        AgentResult result = new AgentResult();
        result.setAgent("triage-agent");
        result.setMemoryPatch(patch);
        result.setConfidence(0.78d);
        if (isDirectCreate(action, payload)) {
            patch.put("confirmed", true);
            result.setHandoffAction(HandoffAction.HANDOFF);
            result.setNextStage(MultiAgentStage.POLICY_CHECK);
            result.setReply("已识别到确认提交，开始校验挂号条件。");
            result.setSummary("direct_create_detected");
            return result;
        }

        if (isRegistrationIntent(action, message, payload, memory)) {
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

    private boolean isRegistrationIntent(String action, String message, Map<String, Object> payload, Map<String, Object> memory) {
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

    private String firstText(Object first, String second) {
        if (first instanceof String && StringUtils.hasText((String) first)) {
            return (String) first;
        }
        return second;
    }

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? new HashMap<String, Object>() : map;
    }
}
