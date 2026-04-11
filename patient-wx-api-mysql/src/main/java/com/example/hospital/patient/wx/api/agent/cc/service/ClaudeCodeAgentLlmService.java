package com.example.hospital.patient.wx.api.agent.cc.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.cc.config.ClaudeCodeAgentPromptCatalog;
import com.example.hospital.patient.wx.api.agent.cc.config.ClaudeCodeAgentProperties;
import com.example.hospital.patient.wx.api.agent.react.dto.TraditionalAgentModelDecision;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeCodeAgentLlmService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private ClaudeCodeAgentProperties properties;

    @Resource
    private AgentProperties legacyAgentProperties;

    public TraditionalAgentModelDecision decide(AgentChatRequest request,
                                                Map<String, Object> memory,
                                                List<Map<String, Object>> trace,
                                                List<Map<String, Object>> tools) {
        if (!properties.isLlmEnabled() || !StringUtils.hasText(resolveApiKey())) {
            return buildFallbackDecision(request, memory);
        }
        try {
            JSONObject body = buildRequestBody(request, memory, trace, tools);
            HttpResponse httpResponse = HttpRequest.post(resolveBaseUrl())
                    .header("Authorization", "Bearer " + resolveApiKey())
                    .header("Content-Type", "application/json")
                    .timeout(resolveTimeoutMillis())
                    .body(body.toString())
                    .execute();
            if (httpResponse.getStatus() < 200 || httpResponse.getStatus() >= 300) {
                return buildFallbackDecision(request, memory);
            }
            JSONObject root = JSONUtil.parseObj(httpResponse.body());
            String content = extractContent(root);
            if (!StringUtils.hasText(content)) {
                return buildFallbackDecision(request, memory);
            }
            return JSONUtil.toBean(extractJson(content), TraditionalAgentModelDecision.class);
        } catch (Exception e) {
            return buildFallbackDecision(request, memory);
        }
    }

    public TraditionalAgentModelDecision fallbackDecision(AgentChatRequest request, Map<String, Object> memory) {
        return buildFallbackDecision(request, memory);
    }

    private JSONObject buildRequestBody(AgentChatRequest request,
                                        Map<String, Object> memory,
                                        List<Map<String, Object>> trace,
                                        List<Map<String, Object>> tools) {
        JSONObject body = new JSONObject();
        body.set("model", resolveModel());
        body.set("temperature", resolveTemperature());
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().set("role", "system").set("content", ClaudeCodeAgentPromptCatalog.getSystemPrompt()));
        messages.add(new JSONObject().set("role", "user").set("content", buildUserPrompt(request, memory, trace, tools)));
        body.set("messages", messages);
        body.set("response_format", new JSONObject().set("type", "json_object"));
        return body;
    }

    private String buildUserPrompt(AgentChatRequest request,
                                   Map<String, Object> memory,
                                   List<Map<String, Object>> trace,
                                   List<Map<String, Object>> tools) {
        StringBuilder builder = new StringBuilder();
        builder.append("User message: ").append(request == null ? "" : safe(request.getMessage())).append("\n");
        builder.append("User action: ").append(request == null ? "" : safe(request.getAction())).append("\n");
        builder.append("User payload: ").append(request == null ? "{}" : JSONUtil.toJsonStr(request.getPayload())).append("\n");
        builder.append("Current memory: ").append(JSONUtil.toJsonStr(memory)).append("\n");
        builder.append("Available tools: ").append(JSONUtil.toJsonStr(tools)).append("\n");
        builder.append("Trace so far: ").append(JSONUtil.toJsonStr(trace)).append("\n");
        builder.append("Date hint conversion: today=").append(LocalDate.now().format(DATE_FORMATTER))
                .append(", tomorrow=").append(LocalDate.now().plusDays(1).format(DATE_FORMATTER))
                .append(", day_after_tomorrow=").append(LocalDate.now().plusDays(2).format(DATE_FORMATTER)).append("\n");
        builder.append("Rules:\n");
        builder.append("1. Think in CC plan mode: choose only the next grounded tool or finish.\n");
        builder.append("2. Never reveal thoughts. Only grounded tool selection matters.\n");
        builder.append("3. If the user asks for messages, use list_messages.\n");
        builder.append("4. If the user asks for identity or patient card, use get_user_card_status.\n");
        builder.append("5. If memory contains deptSubId and date but not doctorId, prefer list_doctors_in_day.\n");
        builder.append("6. If memory contains doctorId and date, prefer list_schedule_slots.\n");
        builder.append("7. Avoid create_registration unless enough slot information is already known or the user is confirming.\n");
        builder.append("8. Never finish before at least one real tool call has happened in this round.\n");
        builder.append("9. finalAnswer must be grounded in the latest tool observation.\n");
        builder.append("10. Do not invent deptName, deptSubName, doctorName, doctorId, workPlanId, or scheduleId.\n");
        return builder.toString();
    }

    private TraditionalAgentModelDecision buildFallbackDecision(AgentChatRequest request, Map<String, Object> memory) {
        TraditionalAgentModelDecision decision = new TraditionalAgentModelDecision();
        String action = request == null ? null : request.getAction();
        String message = request == null ? null : request.getMessage();
        Map<String, Object> toolInput = new HashMap<>();
        if (request != null && request.getPayload() != null) {
            toolInput.putAll(request.getPayload());
        }
        if ("view_messages".equals(action) || containsAny(message, "消息", "通知", "提醒")) {
            decision.setThought("The user wants to see messages.");
            decision.setAction("tool");
            decision.setToolName("list_messages");
            decision.setToolInput(toolInput);
            return decision;
        }
        if ("view_user_card".equals(action) || containsAny(message, "就诊卡", "实名", "身份")) {
            decision.setThought("The user wants patient card status.");
            decision.setAction("tool");
            decision.setToolName("get_user_card_status");
            decision.setToolInput(toolInput);
            return decision;
        }
        if ("create_registration".equals(action) || Boolean.TRUE.equals(toolInput.get("confirmed"))) {
            decision.setThought("The user is confirming registration.");
            decision.setAction("tool");
            decision.setToolName("create_registration");
            decision.setToolInput(toolInput);
            return decision;
        }
        if ("start_registration".equals(action) || "view_departments".equals(action)) {
            decision.setThought("Registration starts with departments.");
            decision.setAction("tool");
            decision.setToolName("list_departments");
            decision.setToolInput(toolInput);
            return decision;
        }
        if (Boolean.TRUE.equals(memory.get("slotSelectionPendingConfirmation"))
                && memory.get("workPlanId") != null
                && memory.get("scheduleId") != null) {
            decision.setThought("A concrete slot was selected, so proceed to guarded registration submission.");
            decision.setAction("tool");
            decision.setToolName("create_registration");
            decision.setToolInput(toolInput);
            return decision;
        }
        if (memory.get("doctorId") != null && memory.get("date") != null) {
            decision.setThought("Doctor and date are known, query slots.");
            decision.setAction("tool");
            decision.setToolName("list_schedule_slots");
            decision.setToolInput(toolInput);
            return decision;
        }
        if (memory.get("deptSubId") != null && memory.get("date") != null) {
            decision.setThought("Clinic room and date are known, query doctors.");
            decision.setAction("tool");
            decision.setToolName("list_doctors_in_day");
            decision.setToolInput(toolInput);
            return decision;
        }
        if (memory.get("deptSubId") != null) {
            decision.setThought("Clinic room is known, query dates.");
            decision.setAction("tool");
            decision.setToolName("list_register_dates");
            decision.setToolInput(toolInput);
            return decision;
        }
        if (memory.get("deptId") != null) {
            decision.setThought("Department is known, query clinic rooms.");
            decision.setAction("tool");
            decision.setToolName("list_sub_departments");
            decision.setToolInput(toolInput);
            return decision;
        }
        if (memory.get("deptName") != null) {
            decision.setThought("Department name is known, resolve clinic rooms.");
            decision.setAction("tool");
            decision.setToolName("list_sub_departments");
            decision.setToolInput(toolInput);
            return decision;
        }
        if (containsAny(message, "挂号", "预约", "科室", "医生")) {
            decision.setThought("Registration intent detected, start with departments.");
            decision.setAction("tool");
            decision.setToolName("list_departments");
            decision.setToolInput(toolInput);
            return decision;
        }
        decision.setThought("No tool required.");
        decision.setAction("finish");
        decision.setFinalAnswer("我是第三套 CC Agent 架构，目前可以帮助你查询科室、诊室、医生、时段、消息和就诊卡状态，也可以在确认后提交挂号。");
        return decision;
    }

    private String extractContent(JSONObject root) {
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        JSONObject first = choices.getJSONObject(0);
        if (first == null) {
            return null;
        }
        JSONObject message = first.getJSONObject("message");
        return message == null ? null : message.getStr("content");
    }

    private String extractJson(String content) {
        String text = content == null ? "" : content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
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

    private String resolveBaseUrl() {
        return StringUtils.hasText(properties.getBaseUrl()) ? properties.getBaseUrl() : legacyAgentProperties.getBaseUrl();
    }

    private String resolveApiKey() {
        return StringUtils.hasText(properties.getApiKey()) ? properties.getApiKey() : legacyAgentProperties.getApiKey();
    }

    private String resolveModel() {
        return StringUtils.hasText(properties.getModel()) ? properties.getModel() : legacyAgentProperties.getModel();
    }

    private double resolveTemperature() {
        return properties.getTemperature() > 0 ? properties.getTemperature() : legacyAgentProperties.getTemperature();
    }

    private int resolveTimeoutMillis() {
        return properties.getTimeoutMillis() > 0 ? properties.getTimeoutMillis() : legacyAgentProperties.getTimeoutMillis();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
