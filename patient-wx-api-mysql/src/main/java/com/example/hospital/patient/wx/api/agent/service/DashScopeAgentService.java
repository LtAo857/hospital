package com.example.hospital.patient.wx.api.agent.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.hospital.patient.wx.api.agent.config.AgentProperties;
import com.example.hospital.patient.wx.api.agent.config.AgentPromptCatalog;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentModelDecision;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class DashScopeAgentService {
    @Resource
    private AgentProperties agentProperties;

    public AgentModelDecision decide(AgentChatRequest request, Map<String, Object> memory) {
        if (!agentProperties.isLlmEnabled()) {
            return null;
        }
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            return null;
        }
        if (StringUtils.hasText(request.getAction())) {
            return null;
        }
        if (!StringUtils.hasText(agentProperties.getApiKey())) {
            throw new HospitalException("Agent LLM is enabled, but DashScope API Key is missing");
        }

        JSONObject body = buildRequestBody(request, memory);
        HttpResponse httpResponse = HttpRequest.post(agentProperties.getBaseUrl())
                .header("Authorization", "Bearer " + agentProperties.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(agentProperties.getTimeoutMillis())
                .body(body.toString())
                .execute();

        if (httpResponse.getStatus() < 200 || httpResponse.getStatus() >= 300) {
            throw new HospitalException("DashScope request failed, HTTP " + httpResponse.getStatus() + " - " + httpResponse.body());
        }

        JSONObject root = JSONUtil.parseObj(httpResponse.body());
        String content = extractContent(root);
        if (!StringUtils.hasText(content)) {
            throw new HospitalException("DashScope returned empty content");
        }

        AgentModelDecision decision = JSONUtil.toBean(extractJson(content), AgentModelDecision.class);
        if (!StringUtils.hasText(decision.getReply())) {
            decision.setReply("I can keep helping with the next registration step.");
        }
        if (!StringUtils.hasText(decision.getAction())) {
            decision.setAction("none");
        }
        return decision;
    }

    private JSONObject buildRequestBody(AgentChatRequest request, Map<String, Object> memory) {
        JSONObject body = new JSONObject();
        body.set("model", agentProperties.getModel());
        body.set("temperature", agentProperties.getTemperature());

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject()
                .set("role", "system")
                .set("content", AgentPromptCatalog.getSystemPrompt()));
        messages.add(new JSONObject()
                .set("role", "user")
                .set("content", buildUserPrompt(request, memory)));
        body.set("messages", messages);
        body.set("response_format", new JSONObject().set("type", "json_object"));
        return body;
    }

    private String buildUserPrompt(AgentChatRequest request, Map<String, Object> memory) {
        StringBuilder builder = new StringBuilder();
        builder.append("Current page: ")
                .append(request == null ? "" : safe(request.getCurrentPage()))
                .append("\n");
        builder.append("User message: ")
                .append(request == null ? "" : safe(request.getMessage()))
                .append("\n");
        builder.append("User action: ")
                .append(request == null ? "" : safe(request.getAction()))
                .append("\n");
        builder.append("Conversation memory: ")
                .append(JSONUtil.toJsonStr(memory))
                .append("\n");
        builder.append("Priority rules:\n");
        builder.append("1. Use memory.stage to continue the current flow. Do not reset to start_registration or view_departments just because the user mentions registration or doctors again.\n");
        builder.append("2. If the user wants registration and already provided department plus date, prefer select_doctor or select_slot so the system can auto-select the earliest available slot.\n");
        builder.append("3. If the user provided a department but not a date, prefer select_date and ask briefly for the date.\n");
        builder.append("4. If memory.stage=choose_sub_department, prefer select_sub_dept.\n");
        builder.append("5. If memory.stage=choose_date and the user mentions today, tomorrow, the day after tomorrow, or a concrete date, prefer select_doctor and fill payload.date in yyyy-MM-dd.\n");
        builder.append("6. If memory.stage=choose_doctor and the user asks which doctors are available, prefer select_doctor.\n");
        builder.append("7. If memory.stage=choose_slot and the user specifies a doctor or keeps asking about availability, prefer select_slot.\n");
        builder.append("8. If the user mentions a body part or symptom without an explicit department, infer the closest department when it is obvious. Example: \u53E3\u8154/\u7259\u75DB/\u7259\u9F88/\u667A\u9F7F -> \u53E3\u8154\u79D1.\n");
        builder.append("9. If the user mentions a date, fill payload.date in yyyy-MM-dd.\n");
        builder.append("10. If the user mentions a department, doctor, or clinic room, fill payload.deptName / payload.doctorName / payload.deptSubName whenever possible.\n");
        builder.append("11. If the input text is empty but action exists, generate a friendly reply for that action and keep the same action.\n");
        builder.append("12. If the action is unclear, return action=none.\n");
        builder.append("Return JSON only. Example: {\"reply\":\"I will check the earliest available slot for orthopedics tomorrow.\",\"action\":\"select_doctor\",\"confidence\":0.92,\"reason\":\"The user provided department and date\",\"payload\":{\"deptName\":\"Orthopedics\",\"date\":\"2026-04-10\"}}");
        return builder.toString();
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
