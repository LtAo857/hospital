package com.example.hospital.patient.wx.api.agent.multi.service;

import cn.hutool.core.util.IdUtil;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.dto.AgentConfirmation;
import com.example.hospital.patient.wx.api.agent.dto.AgentToolLog;
import com.example.hospital.patient.wx.api.agent.multi.config.MultiAgentProperties;
import com.example.hospital.patient.wx.api.agent.multi.memory.MultiAgentMemoryService;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
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
                finalReply = "No worker available for stage: " + context.getStage().name();
                break;
            }
            AgentResult result = worker.execute(context);
            if (result == null) {
                finalStage = MultiAgentStage.MANUAL_FALLBACK;
                finalReply = "Empty result from worker: " + worker.getClass().getSimpleName();
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
        response.setToolLogs(buildToolLogs(context.getTrace()));
        response.setSteps(buildSteps(finalStage, memory));
        response.setMemory(exposeMemory(finalStage, memory));

        boolean requiresLogin = booleanValue(memory.get("requiresLogin"));
        boolean awaitingConfirmation = booleanValue(memory.get("awaitingConfirmation"));
        response.setRequiresLogin(requiresLogin);
        response.setRequiresConfirmation(awaitingConfirmation);
        if (awaitingConfirmation) {
            AgentConfirmation confirmation = buildConfirmation(memory);
            response.setConfirmation(confirmation);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private AgentConfirmation buildConfirmation(Map<String, Object> memory) {
        Object pending = memory.get("pendingOrder");
        if (!(pending instanceof Map)) {
            return null;
        }
        Map<String, Object> payload = new HashMap<>((Map<String, Object>) pending);
        payload.put("confirmed", true);
        AgentConfirmation confirmation = new AgentConfirmation();
        confirmation.setAction(AgentAction.CREATE_REGISTRATION);
        confirmation.setLabel("Confirm registration");
        confirmation.setPayload(payload);
        return confirmation;
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
            log.setName(entry.getToolName());
            log.setStatus(entry.getHandoffAction() == HandoffAction.FAIL ? "error" : "success");
            log.setSummary(entry.getSummary());
            logs.add(log);
        }
        return logs;
    }

    private List<AgentPlanStep> buildSteps(MultiAgentStage stage, Map<String, Object> memory) {
        List<AgentPlanStep> steps = new ArrayList<>();
        boolean hasPendingOrder = memory.get("pendingOrder") instanceof Map && !((Map<?, ?>) memory.get("pendingOrder")).isEmpty();
        boolean policyChecked = booleanValue(memory.get("policyChecked"));
        boolean done = stage == MultiAgentStage.DONE;
        steps.add(new AgentPlanStep("triage", "Intent Parse", stage == MultiAgentStage.INTENT_PARSE ? "in_progress" : "completed"));
        steps.add(new AgentPlanStep("slot", "Slot Query", hasPendingOrder || done ? "completed" : "pending"));
        steps.add(new AgentPlanStep("policy", "Policy Check", policyChecked || done ? "completed" : "pending"));
        steps.add(new AgentPlanStep("execute", "Execute", done ? "completed" : "pending"));
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
            return "The multi-agent flow failed. Please retry from slot selection.";
        }
        return "Please provide department and date so I can continue registration.";
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
