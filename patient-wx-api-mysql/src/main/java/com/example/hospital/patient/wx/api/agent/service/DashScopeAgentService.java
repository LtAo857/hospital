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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashScopeAgentService {
    @Resource
    private AgentProperties agentProperties;

    public DashScopeAgentService() {
    }

    public DashScopeAgentService(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    public AgentModelDecision decide(AgentChatRequest request, Map<String, Object> memory) {
        AgentProperties current = agentProperties == null ? new AgentProperties() : agentProperties;
        if (!current.isLlmEnabled()) {
            return null;
        }
        if (request == null || (!StringUtils.hasText(request.getMessage()) && !StringUtils.hasText(request.getAction()))) {
            return null;
        }
        long startedAt = System.currentTimeMillis();
        if (!StringUtils.hasText(current.getApiKey()) || !StringUtils.hasText(current.getBaseUrl())) {
            return buildDegradedDecision("missing_config", startedAt, null, 0);
        }

        JSONObject body = buildRequestBody(request, memory, current);
        int attempts = Math.max(0, current.getHttpRetryCount()) + 1;
        for (int i = 0; i < attempts; i++) {
            try {
                HttpCallResult callResult = executeChatCompletion(body, current);
                int status = callResult == null ? 0 : callResult.getStatus();
                if (status < 200 || status >= 300) {
                    if (isRetryableStatus(status) && i < attempts - 1) {
                        continue;
                    }
                    return buildDegradedDecision("http_" + status, startedAt, status, i);
                }
                JSONObject root = JSONUtil.parseObj(callResult.getBody());
                String content = extractContent(root);
                if (!StringUtils.hasText(content)) {
                    return buildDegradedDecision("empty_content", startedAt, status, i);
                }
                AgentModelDecision decision = JSONUtil.toBean(extractJson(content), AgentModelDecision.class);
                if (decision == null) {
                    return buildDegradedDecision("invalid_json", startedAt, status, i);
                }
                if (!StringUtils.hasText(decision.getReply())) {
                    decision.setReply("我可以继续帮你完成下一步挂号操作。");
                }
                if (!StringUtils.hasText(decision.getAction())) {
                    decision.setAction("none");
                }
                fillSuccessMetrics(decision, root, body.toString(), decision.getReply(), startedAt, status, i);
                return decision;
            } catch (RuntimeException e) {
                if (i < attempts - 1) {
                    continue;
                }
                return buildDegradedDecision("request_failed", startedAt, null, i);
            }
        }
        return buildDegradedDecision("request_failed", startedAt, null, attempts - 1);
    }

    private JSONObject buildRequestBody(AgentChatRequest request,
                                        Map<String, Object> memory,
                                        AgentProperties current) {
        JSONObject body = new JSONObject();
        body.set("model", current.getModel());
        body.set("temperature", current.getTemperature());

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject()
                .set("role", "system")
                .set("content", AgentPromptCatalog.getSystemPrompt()));
        messages.add(new JSONObject()
                .set("role", "user")
                .set("content", buildUserPrompt(request, memory, current)));
        body.set("messages", messages);
        body.set("response_format", new JSONObject().set("type", "json_object"));
        return body;
    }

    protected HttpCallResult executeChatCompletion(JSONObject body, AgentProperties current) {
        HttpRequest request = HttpRequest.post(current.getBaseUrl())
                .header("Content-Type", "application/json")
                .timeout(current.getTimeoutMillis())
                .body(body.toString());
        if (StringUtils.hasText(current.getApiKey())) {
            request.header("Authorization", "Bearer " + current.getApiKey());
        }
        HttpResponse response = request.execute();
        return new HttpCallResult(response.getStatus(), response.body());
    }

    private String buildUserPrompt(AgentChatRequest request,
                                   Map<String, Object> memory,
                                   AgentProperties current) {
        int maxPromptChars = current.getMaxPromptChars() <= 0 ? 1200 : current.getMaxPromptChars();
        String safeMessage = truncate(safe(request == null ? null : request.getMessage()), Math.max(160, maxPromptChars / 4));
        String safeAction = truncate(safe(request == null ? null : request.getAction()), 64);
        String safePage = truncate(safe(request == null ? null : request.getCurrentPage()), 64);
        String safeMemory = truncate(JSONUtil.toJsonStr(sanitizeMemory(memory)), Math.max(240, maxPromptChars / 2));
        StringBuilder builder = new StringBuilder();
        builder.append("Current page: ")
                .append(safePage)
                .append("\n");
        builder.append("User message: ")
                .append(safeMessage)
                .append("\n");
        builder.append("User action: ")
                .append(safeAction)
                .append("\n");
        builder.append("Conversation memory: ")
                .append(safeMemory)
                .append("\n");
        builder.append("Priority rules:\n");
        builder.append("1. Use memory.stage to continue the current flow. Do not reset to start_registration or view_departments just because the user mentions registration or doctors again.\n");
        builder.append("2. If the user wants registration and already provided department plus date, prefer select_doctor or select_slot so the system can auto-select the earliest available slot.\n");
        builder.append("3. If the user provided a department but not a date, prefer select_date and ask briefly for the date.\n");
        builder.append("4. If memory.stage=choose_sub_department, prefer select_sub_dept.\n");
        builder.append("5. If memory.stage=choose_date and the user mentions today, tomorrow, the day after tomorrow, or a concrete date, prefer select_doctor and fill payload.date in yyyy-MM-dd.\n");
        builder.append("6. If memory.stage=choose_doctor and the user asks which doctors are available, prefer select_doctor.\n");
        builder.append("7. If memory.stage=choose_slot and the user specifies a doctor or keeps asking about availability, prefer select_slot.\n");
        builder.append("8. If the user mentions a body part or symptom without an explicit department, infer the closest department when it is obvious. Example: 口腔/牙痛/牙龈/智齿 -> 口腔科.\n");
        builder.append("9. If the user mentions a date, fill payload.date in yyyy-MM-dd.\n");
        builder.append("10. If the user mentions a department, doctor, or clinic room, fill payload.deptName / payload.doctorName / payload.deptSubName whenever possible.\n");
        builder.append("11. If the input text is empty but action exists, generate a friendly reply for that action and keep the same action.\n");
        builder.append("12. If the action is unclear, return action=none.\n");
        builder.append("Return JSON only. Example: {\"reply\":\"我先帮你看明天骨科的最早号源。\",\"action\":\"select_doctor\",\"confidence\":0.92,\"reason\":\"The user provided department and date\",\"payload\":{\"deptName\":\"Orthopedics\",\"date\":\"2026-04-10\"}}");
        return truncate(builder.toString(), maxPromptChars);
    }

    private Map<String, Object> sanitizeMemory(Map<String, Object> memory) {
        Map<String, Object> safe = new LinkedHashMap<String, Object>();
        if (memory == null || memory.isEmpty()) {
            return safe;
        }
        putIfPresent(safe, "stage", memory.get("stage"));
        putIfPresent(safe, "deptId", memory.get("deptId"));
        putIfPresent(safe, "deptName", memory.get("deptName"));
        putIfPresent(safe, "deptSubId", memory.get("deptSubId"));
        putIfPresent(safe, "deptSubName", memory.get("deptSubName"));
        putIfPresent(safe, "doctorId", memory.get("doctorId"));
        putIfPresent(safe, "doctorName", memory.get("doctorName"));
        putIfPresent(safe, "date", memory.get("date"));
        putIfPresent(safe, "hasUserCard", memory.get("hasUserCard"));
        putIfPresent(safe, "selectionMode", memory.get("selectionMode"));
        putIfPresent(safe, "lastAutoFailureReason", memory.get("lastAutoFailureReason"));
        if (memory.get("pendingOrder") instanceof Map) {
            Map<?, ?> pendingOrder = (Map<?, ?>) memory.get("pendingOrder");
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            putIfPresent(summary, "deptSubName", pendingOrder.get("deptSubName"));
            putIfPresent(summary, "doctorName", pendingOrder.get("doctorName"));
            putIfPresent(summary, "date", pendingOrder.get("date"));
            putIfPresent(summary, "slot", pendingOrder.get("slot"));
            if (!summary.isEmpty()) {
                safe.put("pendingOrder", summary);
            }
        }
        return safe;
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null && (!(value instanceof String) || StringUtils.hasText((String) value))) {
            map.put(key, value);
        }
    }

    private void fillSuccessMetrics(AgentModelDecision decision,
                                    JSONObject root,
                                    String promptText,
                                    String replyText,
                                    long startedAt,
                                    Integer status,
                                    int retryCount) {
        JSONObject usage = root == null ? null : root.getJSONObject("usage");
        int promptTokens = usage == null ? estimateTokens(promptText) : usage.getInt("prompt_tokens", estimateTokens(promptText));
        int completionTokens = usage == null ? estimateTokens(replyText) : usage.getInt("completion_tokens", estimateTokens(replyText));
        int totalTokens = usage == null ? promptTokens + completionTokens : usage.getInt("total_tokens", promptTokens + completionTokens);
        decision.setPromptTokens(promptTokens);
        decision.setCompletionTokens(completionTokens);
        decision.setTotalTokens(totalTokens);
        decision.setLatencyMs(System.currentTimeMillis() - startedAt);
        decision.setDegraded(false);
        decision.setFallbackReason(null);
        decision.setProvider("dashscope");
        decision.setHttpStatus(status);
        decision.setRetryCount(retryCount);
    }

    private AgentModelDecision buildDegradedDecision(String fallbackReason,
                                                     long startedAt,
                                                     Integer status,
                                                     int retryCount) {
        AgentModelDecision decision = new AgentModelDecision();
        decision.setAction("none");
        decision.setDegraded(true);
        decision.setFallbackReason(fallbackReason);
        decision.setProvider("dashscope");
        decision.setHttpStatus(status);
        decision.setRetryCount(retryCount);
        decision.setLatencyMs(System.currentTimeMillis() - startedAt);
        decision.setPromptTokens(0);
        decision.setCompletionTokens(0);
        decision.setTotalTokens(0);
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

    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }

    private String truncate(String text, int limit) {
        if (!StringUtils.hasText(text) || limit <= 0 || text.length() <= limit) {
            return text == null ? "" : text;
        }
        return text.substring(0, limit);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    protected static class HttpCallResult {
        private final int status;
        private final String body;

        protected HttpCallResult(int status, String body) {
            this.status = status;
            this.body = body;
        }

        protected int getStatus() {
            return status;
        }

        protected String getBody() {
            return body;
        }
    }
}
