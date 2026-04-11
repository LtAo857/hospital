package com.example.hospital.patient.wx.api.agent.react.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.dto.AgentConfirmation;
import com.example.hospital.patient.wx.api.agent.dto.AgentResponseCard;
import com.example.hospital.patient.wx.api.agent.react.config.TraditionalAgentPromptCatalog;
import com.example.hospital.patient.wx.api.agent.react.config.TraditionalAgentProperties;
import com.example.hospital.patient.wx.api.agent.react.dto.TraditionalAgentModelDecision;
import com.example.hospital.patient.wx.api.agent.react.dto.TraditionalAgentToolResult;
import com.example.hospital.patient.wx.api.agent.react.memory.TraditionalAgentMemoryService;
import com.example.hospital.patient.wx.api.agent.support.AgentPlanStep;
import com.example.hospital.patient.wx.api.agent.react.tool.TraditionalAgentToolRegistry;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TraditionalAgentExecutorService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DEPT_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{1,8}科)");
    private static final Pattern DEPT_SUB_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{1,8}(?:[一二三四五六七八九十0-9]+诊|门诊|诊室))");
    private static final Pattern DOCTOR_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(主任医师|医生|医师)");
    private static final Pattern SLOT_COLON_PATTERN = Pattern.compile("(上午|中午|下午|晚上)?\\s*(\\d{1,2}):(\\d{2})");
    private static final Pattern SLOT_CLOCK_PATTERN = Pattern.compile("(上午|中午|下午|晚上)?\\s*(\\d{1,2})点(?:(半)|(\\d{1,2})分?)?");
    @Resource
    private TraditionalAgentProperties properties;

    @Resource
    private TraditionalAgentMemoryService memoryService;

    @Resource
    private TraditionalAgentLlmService llmService;

    @Resource
    private TraditionalAgentToolRegistry toolRegistry;

    public AgentChatResponse chat(AgentChatRequest request, Integer userId) {
        AgentChatRequest safeRequest = request == null ? new AgentChatRequest() : request;
        String sessionId = StringUtils.hasText(safeRequest.getSessionId()) ? safeRequest.getSessionId() : IdUtil.simpleUUID();
        Map<String, Object> memory = memoryService.load(sessionId);
        AgentChatResponse response = new AgentChatResponse();
        response.setSessionId(sessionId);
        response.setSystemPromptVersion(TraditionalAgentPromptCatalog.SYSTEM_PROMPT_VERSION);

        if (userId != null) {
            memory.put("userId", userId);
        } else {
            memory.remove("userId");
        }
        hydrateMemoryFromRequest(safeRequest, memory);

        try {
            if (handleBuiltInActions(safeRequest, response, memory, userId)) {
                finalizeResponse(response, memory, sessionId);
                return response;
            }

            if (handlePendingWriteConfirmation(safeRequest, response, memory, userId)) {
                finalizeResponse(response, memory, sessionId);
                return response;
            }

            if (!StringUtils.hasText(safeRequest.getMessage()) && !StringUtils.hasText(safeRequest.getAction())) {
                fillWelcomeResponse(response);
                finalizeResponse(response, memory, sessionId);
                return response;
            }

            List<Map<String, Object>> trace = new ArrayList<>();
            TraditionalAgentToolResult lastToolResult = null;
            for (int step = 1; step <= properties.getMaxSteps(); step++) {
                TraditionalAgentModelDecision decision = llmService.decide(safeRequest, memory, trace, toolRegistry.describeTools());
                TraditionalAgentModelDecision guardedDecision = guardDecision(safeRequest, decision, memory);
                if (guardedDecision != decision) {
                    response.getToolLogs().add(toolRegistry.toToolLog(
                            "react_tool_guard_" + step,
                            "success",
                            "Unsafe tool selection was rewritten to an earlier discovery step."
                    ));
                    decision = guardedDecision;
                }
                response.getToolLogs().add(toolRegistry.toToolLog(
                        "react_decision_" + step,
                        "success",
                        summarizeDecision(decision)
                ));

                if (decision == null || !StringUtils.hasText(decision.getAction())) {
                    response.setReply("传统 Agent 没有生成有效决策，请稍后重试。");
                    response.setState("error");
                    break;
                }

                if ("finish".equals(decision.getAction()) && lastToolResult == null) {
                    TraditionalAgentModelDecision fallbackDecision = llmService.fallbackDecision(safeRequest, memory);
                    if (fallbackDecision != null && "tool".equals(fallbackDecision.getAction()) && toolRegistry.hasTool(fallbackDecision.getToolName())) {
                        decision = fallbackDecision;
                        response.getToolLogs().add(toolRegistry.toToolLog(
                                "react_finish_guard_" + step,
                                "success",
                                "Finish was ignored because no tool had run yet; switched to fallback tool."
                        ));
                    } else {
                        response.setReply(buildNoObservationReply(memory));
                        response.setState("completed");
                        break;
                    }
                }

                if ("finish".equals(decision.getAction()) && lastToolResult != null) {
                    response.setReply(buildGroundedReply(lastToolResult, memory));
                    response.setState("completed");
                    break;
                }

                if ("finish".equals(decision.getAction())) {
                    response.setReply(StringUtils.hasText(decision.getFinalAnswer())
                            ? decision.getFinalAnswer()
                            : "我已经完成本轮分析。");
                    response.setState("completed");
                    break;
                }

                if (!"tool".equals(decision.getAction()) || !toolRegistry.hasTool(decision.getToolName())) {
                    response.setReply("传统 Agent 生成了无效工具动作，请重新输入。");
                    response.setState("error");
                    break;
                }

                Map<String, Object> toolInput = mergeToolInput(decision.getToolInput(), safeRequest.getPayload(), memory);
                if (shouldRequireConfirmation(decision.getToolName(), toolInput)) {
                    prepareWriteConfirmation(response, memory, decision.getToolName(), toolInput);
                    break;
                }

                lastToolResult = toolRegistry.execute(decision.getToolName(), toolInput, userId, memory);
                response.getToolLogs().add(toolRegistry.toToolLog(
                        decision.getToolName(),
                        lastToolResult.isSuccess() ? "success" : "error",
                        lastToolResult.getSummary()
                ));
                mergeMemory(memory, lastToolResult.getMemoryUpdates());
                if (lastToolResult.getObservation() != null) {
                    memory.put("lastObservation", lastToolResult.getObservation());
                }
                trace.add(buildTraceStep(step, decision, lastToolResult));

                if (lastToolResult.isTerminal()) {
                    response.setReply(lastToolResult.getTerminalReply());
                    response.setState(StringUtils.hasText(lastToolResult.getTerminalState()) ? lastToolResult.getTerminalState() : "completed");
                    response.getCards().addAll(lastToolResult.getCards());
                    clearPendingWrite(memory);
                    break;
                }

                if (shouldReplyAfterTool(lastToolResult)) {
                    response.setReply(buildGroundedReply(lastToolResult, memory));
                    response.setState("completed");
                    break;
                }

                if (step == properties.getMaxSteps()) {
                    response.setReply(lastToolResult == null ? buildFallbackReply(null, memory) : buildGroundedReply(lastToolResult, memory));
                    response.setState("completed");
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Traditional agent execution failed", e);
            response.setReply("第二套传统 Agent 架构暂时不可用，请稍后再试。");
            response.setState("error");
            response.getToolLogs().add(toolRegistry.toToolLog("traditional_agent", "error", e.getMessage()));
        }

        finalizeResponse(response, memory, sessionId);
        return response;
    }

    private boolean handleBuiltInActions(AgentChatRequest request,
                                         AgentChatResponse response,
                                         Map<String, Object> memory,
                                         Integer userId) {
        String action = request.getAction();
        if (!StringUtils.hasText(action)) {
            return false;
        }
        if ("welcome".equals(action)) {
            fillWelcomeResponse(response);
            return true;
        }
        if ("view_messages".equals(action)) {
            TraditionalAgentToolResult result = toolRegistry.execute("list_messages", request.getPayload(), userId, memory);
            mergeMemory(memory, result.getMemoryUpdates());
            response.getToolLogs().add(toolRegistry.toToolLog("list_messages", result.isSuccess() ? "success" : "error", result.getSummary()));
            response.setReply(renderImmediateReply("消息中心", result.getObservation(), "已查询最近消息。"));
            response.setState("message");
            return true;
        }
        if ("view_user_card".equals(action)) {
            TraditionalAgentToolResult result = toolRegistry.execute("get_user_card_status", request.getPayload(), userId, memory);
            mergeMemory(memory, result.getMemoryUpdates());
            response.getToolLogs().add(toolRegistry.toToolLog("get_user_card_status", result.isSuccess() ? "success" : "error", result.getSummary()));
            response.setReply(renderImmediateReply("就诊卡状态", result.getObservation(), "已查询就诊卡状态。"));
            response.setState("user_card");
            return true;
        }
        if ("start_registration".equals(action) || "view_departments".equals(action)) {
            TraditionalAgentToolResult result = toolRegistry.execute("list_departments", request.getPayload(), userId, memory);
            mergeMemory(memory, result.getMemoryUpdates());
            response.getToolLogs().add(toolRegistry.toToolLog("list_departments", result.isSuccess() ? "success" : "error", result.getSummary()));
            response.setReply(renderImmediateReply("可挂号科室", result.getObservation(), "已查询门诊科室。"));
            response.setState("registration");
            return true;
        }
        return false;
    }

    private boolean handlePendingWriteConfirmation(AgentChatRequest request,
                                                   AgentChatResponse response,
                                                   Map<String, Object> memory,
                                                   Integer userId) {
        if (!hasPendingWrite(memory)) {
            return false;
        }
        if (isCancelConfirmation(request)) {
            clearPendingWrite(memory);
            response.setReply("已取消本次挂号提交，你可以继续查询其他号源。");
            response.setState("cancelled");
            return true;
        }
        if (!isConfirmRequest(request)) {
            return false;
        }

        String toolName = stringValue(memory.get("pendingToolName"), null);
        Map<String, Object> toolInput = mapValue(memory.get("pendingToolInput"));
        toolInput.put("confirmed", true);
        TraditionalAgentToolResult result = toolRegistry.execute(toolName, toolInput, userId, memory);
        mergeMemory(memory, result.getMemoryUpdates());
        response.getToolLogs().add(toolRegistry.toToolLog(toolName, result.isSuccess() ? "success" : "error", result.getSummary()));
        if (result.isTerminal()) {
            response.setReply(result.getTerminalReply());
            response.setState(StringUtils.hasText(result.getTerminalState()) ? result.getTerminalState() : "completed");
            response.getCards().addAll(result.getCards());
        } else {
            response.setReply("确认后已执行写操作。");
            response.setState("completed");
        }
        clearPendingWrite(memory);
        return true;
    }

    private boolean shouldRequireConfirmation(String toolName, Map<String, Object> toolInput) {
        return properties.isConfirmRequiredForWrite()
                && toolRegistry.isWriteTool(toolName)
                && !Boolean.TRUE.equals(toolInput.get("confirmed"));
    }

    private void prepareWriteConfirmation(AgentChatResponse response,
                                          Map<String, Object> memory,
                                          String toolName,
                                          Map<String, Object> toolInput) {
        memory.put("pendingToolName", toolName);
        memory.put("pendingToolInput", new HashMap<>(toolInput));
        response.setRequiresConfirmation(true);
        response.setState("awaiting_confirmation");
        response.setReply("传统 Agent 已经准备好提交挂号，请确认是否继续。");
        AgentConfirmation confirmation = new AgentConfirmation();
        confirmation.setAction(AgentAction.CREATE_REGISTRATION);
        confirmation.setLabel("确认挂号");
        Map<String, Object> payload = new HashMap<>(toolInput);
        payload.put("confirmed", true);
        confirmation.setPayload(payload);
        response.setConfirmation(confirmation);
        response.getCards().add(actionCard("确认挂号", "确认后将执行写操作提交挂号", AgentAction.CREATE_REGISTRATION, payload, "确认"));
    }

    private Map<String, Object> buildTraceStep(int step,
                                               TraditionalAgentModelDecision decision,
                                               TraditionalAgentToolResult toolResult) {
        Map<String, Object> traceStep = new LinkedHashMap<>();
        traceStep.put("step", step);
        traceStep.put("thought", decision.getThought());
        traceStep.put("tool", decision.getToolName());
        traceStep.put("observation", toolResult.getObservation());
        return traceStep;
    }

    private Map<String, Object> mergeToolInput(Map<String, Object> modelInput,
                                               Map<String, Object> requestPayload,
                                               Map<String, Object> memory) {
        Map<String, Object> result = new HashMap<>();
        if (requestPayload != null) {
            result.putAll(requestPayload);
        }
        copyFromMemoryIfAbsent(result, memory, "deptId", "deptName", "deptSubId", "deptSubName", "date", "doctorId", "doctorName",
                "slot", "workPlanId", "scheduleId", "amount");
        // In the traditional agent, entity parameters must come from user input, prior tool observations, or explicit card payloads.
        // The model only decides the next tool and must not invent doctor/clinic/slot identifiers.
        if (modelInput != null && Boolean.TRUE.equals(modelInput.get("confirmed"))) {
            result.put("confirmed", true);
        }
        return result;
    }

    private TraditionalAgentModelDecision guardDecision(AgentChatRequest request,
                                                        TraditionalAgentModelDecision decision,
                                                        Map<String, Object> memory) {
        Map<String, Object> trustedInput = mergeToolInput(null, request == null ? null : request.getPayload(), memory);
        if (shouldForceCreateRegistration(trustedInput, memory)) {
            return createRegistrationDecision();
        }
        if (decision == null || !"tool".equals(decision.getAction()) || !StringUtils.hasText(decision.getToolName())) {
            return decision;
        }
        if (!requiresEarlierDiscovery(decision.getToolName(), trustedInput)) {
            return decision;
        }
        TraditionalAgentModelDecision fallbackDecision = llmService.fallbackDecision(request, memory);
        if (fallbackDecision != null
                && "tool".equals(fallbackDecision.getAction())
                && toolRegistry.hasTool(fallbackDecision.getToolName())
                && !requiresEarlierDiscovery(fallbackDecision.getToolName(), trustedInput)) {
            return fallbackDecision;
        }
        return decision;
    }

    private boolean shouldForceCreateRegistration(Map<String, Object> trustedInput, Map<String, Object> memory) {
        return Boolean.TRUE.equals(memory.get("slotSelectionPendingConfirmation"))
                && intValue(trustedInput.get("workPlanId")) != null
                && intValue(trustedInput.get("scheduleId")) != null;
    }

    private TraditionalAgentModelDecision createRegistrationDecision() {
        TraditionalAgentModelDecision decision = new TraditionalAgentModelDecision();
        decision.setThought("A concrete slot has been selected, so move to guarded registration submission.");
        decision.setAction("tool");
        decision.setToolName("create_registration");
        decision.setToolInput(new HashMap<>());
        return decision;
    }

    private boolean requiresEarlierDiscovery(String toolName, Map<String, Object> trustedInput) {
        if (!StringUtils.hasText(toolName)) {
            return false;
        }
        Integer deptId = intValue(trustedInput.get("deptId"));
        String deptName = stringValue(trustedInput.get("deptName"), null);
        Integer deptSubId = intValue(trustedInput.get("deptSubId"));
        String deptSubName = stringValue(trustedInput.get("deptSubName"), null);
        Integer doctorId = intValue(trustedInput.get("doctorId"));
        String doctorName = stringValue(trustedInput.get("doctorName"), null);
        String date = stringValue(trustedInput.get("date"), null);
        Integer workPlanId = intValue(trustedInput.get("workPlanId"));
        Integer scheduleId = intValue(trustedInput.get("scheduleId"));

        if ("list_sub_departments".equals(toolName)) {
            return deptId == null && !StringUtils.hasText(deptName);
        }
        if ("list_register_dates".equals(toolName)) {
            return deptSubId == null && !StringUtils.hasText(deptSubName);
        }
        if ("list_doctors_in_day".equals(toolName)) {
            return (deptSubId == null && !StringUtils.hasText(deptSubName)) || !StringUtils.hasText(date);
        }
        if ("list_schedule_slots".equals(toolName)) {
            return (doctorId == null && !StringUtils.hasText(doctorName)) || !StringUtils.hasText(date);
        }
        if ("create_registration".equals(toolName)) {
            return workPlanId == null || scheduleId == null;
        }
        return false;
    }

    private void mergeMemory(Map<String, Object> memory, Map<String, Object> updates) {
        if (updates != null && !updates.isEmpty()) {
            memory.putAll(updates);
        }
    }

    private void copyFromMemoryIfAbsent(Map<String, Object> target, Map<String, Object> memory, String... keys) {
        for (String key : keys) {
            if (!target.containsKey(key) && memory.containsKey(key) && memory.get(key) != null) {
                target.put(key, memory.get(key));
            }
        }
    }

    private boolean isConfirmRequest(AgentChatRequest request) {
        if (request == null) {
            return false;
        }
        if (request.getPayload() != null && Boolean.TRUE.equals(request.getPayload().get("confirmed"))) {
            return true;
        }
        if ("create_registration".equals(request.getAction())) {
            return true;
        }
        String message = request.getMessage();
        return StringUtils.hasText(message) && (message.contains("确认") || message.contains("继续提交"));
    }

    private boolean isCancelConfirmation(AgentChatRequest request) {
        if (request == null) {
            return false;
        }
        String message = request.getMessage();
        return StringUtils.hasText(message) && (message.contains("取消") || message.contains("不要挂") || message.contains("先不挂"));
    }

    private boolean hasPendingWrite(Map<String, Object> memory) {
        return memory.get("pendingToolName") != null && memory.get("pendingToolInput") != null;
    }

    private void clearPendingWrite(Map<String, Object> memory) {
        memory.remove("pendingToolName");
        memory.remove("pendingToolInput");
        memory.remove("slotSelectionPendingConfirmation");
    }

    private void finalizeResponse(AgentChatResponse response, Map<String, Object> memory, String sessionId) {
        if (!StringUtils.hasText(response.getReply())) {
            response.setReply("第二套传统 Agent 架构已完成当前轮次。");
        }
        if (!StringUtils.hasText(response.getState())) {
            response.setState(hasPendingWrite(memory) ? "awaiting_confirmation" : "completed");
        }
        response.setMemory(exposeMemory(memory));
        response.setSteps(buildSteps(memory, response.getState()));
        memoryService.save(sessionId, memory);
    }

    private Map<String, Object> exposeMemory(Map<String, Object> memory) {
        Map<String, Object> result = new HashMap<>();
        result.put("deptName", memory.get("deptName"));
        result.put("deptSubName", memory.get("deptSubName"));
        result.put("date", memory.get("date"));
        result.put("doctorName", memory.get("doctorName"));
        result.put("hasUserCard", memory.get("hasUserCard"));
        result.put("hasPendingWrite", hasPendingWrite(memory));
        result.put("lastObservation", memory.get("lastObservation"));
        return result;
    }

    private List<AgentPlanStep> buildSteps(Map<String, Object> memory, String state) {
        List<AgentPlanStep> steps = new ArrayList<>();
        steps.add(new AgentPlanStep("observe", "理解问题", "completed"));
        boolean queried = memory.get("lastObservation") != null;
        steps.add(new AgentPlanStep("tool", "调用工具", queried ? "completed" : "pending"));
        steps.add(new AgentPlanStep("reason", "整理结论", "completed".equals(state) ? "completed" : queried ? "in_progress" : "pending"));
        steps.add(new AgentPlanStep("confirm", "确认写操作", hasPendingWrite(memory) ? "in_progress" : "completed".equals(state) ? "completed" : "pending"));
        return steps;
    }

    private void fillWelcomeResponse(AgentChatResponse response) {
        response.setReply("这里是第二套传统 Agent 架构。你可以直接告诉我想挂哪个科室、哪天、哪位医生，我会通过工具一步步查询，并在确认后提交挂号。");
        response.setState("idle");
        response.getCards().add(actionCard("开始挂号", "从查询科室开始", "start_registration", null, "挂号"));
        response.getCards().add(actionCard("查看消息", "查询最近消息与提醒", "view_messages", null, "消息"));
        response.getCards().add(actionCard("查看就诊卡", "检查实名与就诊卡状态", "view_user_card", null, "实名"));
    }

    private AgentResponseCard actionCard(String title,
                                         String description,
                                         String action,
                                         Map<String, Object> payload,
                                         String badge) {
        AgentResponseCard card = new AgentResponseCard();
        card.setType("action");
        card.setTitle(title);
        card.setDescription(description);
        card.setAction(action);
        card.setPayload(payload == null ? new HashMap<String, Object>() : payload);
        card.setBadge(badge);
        return card;
    }

    private String renderImmediateReply(String title, Object observation, String fallback) {
        if (observation == null) {
            return fallback;
        }
        return title + "：" + JSONUtil.toJsonStr(observation);
    }

    private String summarizeDecision(TraditionalAgentModelDecision decision) {
        if (decision == null) {
            return "No decision";
        }
        if ("finish".equals(decision.getAction())) {
            return "finish: " + stringValue(decision.getFinalAnswer(), "");
        }
        return "tool: " + stringValue(decision.getToolName(), "") + ", thought: " + stringValue(decision.getThought(), "");
    }

    private String buildFallbackReply(TraditionalAgentToolResult lastToolResult, Map<String, Object> memory) {
        if (lastToolResult != null && StringUtils.hasText(lastToolResult.getSummary())) {
            return "传统 Agent 已完成多步查询，当前结果是：" + lastToolResult.getSummary();
        }
        if (memory.get("lastObservation") != null) {
            return "传统 Agent 已完成查询，你可以继续补充更具体的日期、医生或时段。";
        }
        return "传统 Agent 已结束本轮，但信息还不够完整。你可以继续补充科室、日期或医生。";
    }

    private boolean shouldReplyAfterTool(TraditionalAgentToolResult lastToolResult) {
        if (lastToolResult == null || lastToolResult.getObservation() == null) {
            return false;
        }
        Map<String, Object> observationMap = mapValue(lastToolResult.getObservation());
        if (observationMap.isEmpty()) {
            return false;
        }
        if (observationMap.containsKey("error")
                || observationMap.containsKey("slots")
                || observationMap.containsKey("hasUserCard")
                || observationMap.containsKey("unreadCount")) {
            return true;
        }
        if (observationMap.containsKey("dates") && observationMap.get("matchedDate") == null) {
            return true;
        }
        if (observationMap.containsKey("departments") && observationMap.get("matchedDepartment") == null) {
            return true;
        }
        if (observationMap.containsKey("subDepartments") && observationMap.get("matchedSubDepartment") == null) {
            return true;
        }
        if (observationMap.containsKey("doctors") && observationMap.get("matchedDoctor") == null) {
            return true;
        }
        return false;
    }

    private String buildGroundedReply(TraditionalAgentToolResult lastToolResult, Map<String, Object> memory) {
        if (lastToolResult == null) {
            return buildNoObservationReply(memory);
        }
        Object observation = lastToolResult.getObservation();
        if (!(observation instanceof Map)) {
            return buildFallbackReply(lastToolResult, memory);
        }
        Map<String, Object> observationMap = mapValue(observation);

        if (observationMap.containsKey("slots")) {
            List<Map<String, Object>> slots = listValue(observationMap.get("slots"));
            List<String> available = new ArrayList<>();
            for (Map<String, Object> slot : slots) {
                Integer maximum = intValue(slot.get("maximum"));
                Integer num = intValue(slot.get("num"));
                int remain = maximum != null && num != null ? Math.max(maximum - num, 0) : 0;
                if (remain > 0) {
                    available.add(stringValue(slot.get("slotLabel"), "--") + "（余" + remain + "）");
                }
            }
            String doctorName = stringValue(memory.get("doctorName"), "该医生");
            String date = stringValue(memory.get("date"), "所选日期");
            if (!available.isEmpty()) {
                return doctorName + "在" + date + "还有可挂时段：" + String.join("、", available) + "。如果你要，我可以继续提交挂号。";
            }
            return doctorName + "在" + date + "当前没有可挂时段，你可以换个日期或医生继续查询。";
        }

        if (observationMap.containsKey("doctors")) {
            List<Map<String, Object>> doctors = listValue(observationMap.get("doctors"));
            if (!doctors.isEmpty()) {
                List<String> doctorNames = new ArrayList<>();
                for (Map<String, Object> doctor : doctors) {
                    doctorNames.add(stringValue(doctor.get("name"), "--"));
                }
                return stringValue(memory.get("date"), "所选日期") + "可出诊的医生有：" + String.join("、", doctorNames) + "。你可以继续告诉我想挂哪位医生。";
            }
            return "当前没有查询到出诊医生，你可以换个日期或诊室继续查询。";
        }

        if (observationMap.containsKey("dates")) {
            List<Map<String, Object>> dates = listValue(observationMap.get("dates"));
            List<String> availableDates = new ArrayList<>();
            for (Map<String, Object> date : dates) {
                if ("出诊".equals(stringValue(date.get("status"), ""))) {
                    availableDates.add(stringValue(date.get("date"), "--"));
                }
            }
            if (!availableDates.isEmpty()) {
                return "我查到接下来可挂号的日期有：" + String.join("、", availableDates) + "。你可以继续指定其中一天。";
            }
            return "接下来几天暂时没有可挂号日期，你可以稍后再试。";
        }

        if (observationMap.containsKey("subDepartments")) {
            List<Map<String, Object>> subDepartments = listValue(observationMap.get("subDepartments"));
            if (!subDepartments.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Map<String, Object> subDepartment : subDepartments) {
                    names.add(stringValue(subDepartment.get("name"), "--"));
                }
                return stringValue(memory.get("deptName"), "该科室") + "下的诊室有：" + String.join("、", names) + "。你可以继续告诉我想查哪个诊室。";
            }
            return "这个科室下暂时没有查到诊室信息。";
        }

        if (observationMap.containsKey("departments")) {
            List<Map<String, Object>> departments = listValue(observationMap.get("departments"));
            if (!departments.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Map<String, Object> department : departments) {
                    names.add(stringValue(department.get("name"), "--"));
                }
                return "我查到当前可挂号的科室有：" + String.join("、", names) + "。你可以继续告诉我具体想挂哪个科室。";
            }
            return "当前没有查询到可挂号科室。";
        }

        if (observationMap.containsKey("hasUserCard")) {
            Boolean hasUserCard = booleanValue(observationMap.get("hasUserCard"));
            return Boolean.TRUE.equals(hasUserCard)
                    ? "你已经创建就诊卡，可以继续查询号源或提交挂号。"
                    : "你还没有创建就诊卡，挂号前需要先完成实名登记。";
        }

        if (observationMap.containsKey("unreadCount")) {
            return "我已经查询了消息中心，当前未读消息数为" + stringValue(observationMap.get("unreadCount"), "0") + "。";
        }

        return buildFallbackReply(lastToolResult, memory);
    }

    private String buildNoObservationReply(Map<String, Object> memory) {
        if (memory.get("deptName") != null && memory.get("date") != null) {
            return "我已经识别到你的挂号条件，但还需要继续查询真实号源。";
        }
        if (memory.get("deptName") != null) {
            return "我已经识别到科室，但还需要继续查询诊室和号源。";
        }
        return "我需要先通过工具查询真实业务数据，再给你最终答复。你可以继续告诉我科室、日期或医生。";
    }

    private void hydrateMemoryFromRequest(AgentChatRequest request, Map<String, Object> memory) {
        if (request == null) {
            return;
        }
        if (request.getPayload() != null && !request.getPayload().isEmpty()) {
            copyIfPresent(request.getPayload(), memory, "deptId", "deptName", "deptSubId", "deptSubName", "date", "doctorId", "doctorName", "slot", "workPlanId", "scheduleId", "amount");
        }
        String message = request.getMessage();
        if (!StringUtils.hasText(message)) {
            return;
        }
        String normalizedDate = parseDateHint(message);
        if (StringUtils.hasText(normalizedDate)) {
            memory.put("date", normalizedDate);
        }
        String deptName = parseDeptHint(message);
        if (StringUtils.hasText(deptName) && memory.get("deptId") == null) {
            memory.put("deptName", deptName);
        }
        String deptSubName = parseDeptSubHint(message);
        if (StringUtils.hasText(deptSubName) && memory.get("deptSubId") == null) {
            memory.put("deptSubName", deptSubName);
        }
        String doctorName = parseDoctorHint(message);
        if (StringUtils.hasText(doctorName) && memory.get("doctorId") == null) {
            memory.put("doctorName", doctorName);
        }
        Map<String, Object> selectedSlot = parseSlotSelection(message, memory);
        if (!selectedSlot.isEmpty()) {
            copyIfPresent(selectedSlot, memory, "slot", "slotLabel", "workPlanId", "scheduleId", "amount");
            memory.put("slotSelectionPendingConfirmation", true);
        }
    }

    private String parseDateHint(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        if (message.contains("今天")) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
        if (message.contains("明天")) {
            return LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        }
        if (message.contains("后天")) {
            return LocalDate.now().plusDays(2).format(DATE_FORMATTER);
        }
        Matcher fullMatcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(message);
        if (fullMatcher.find()) {
            return fullMatcher.group(1);
        }
        Matcher monthDayMatcher = Pattern.compile("(\\d{1,2})月(\\d{1,2})日").matcher(message);
        if (monthDayMatcher.find()) {
            int month = Integer.parseInt(monthDayMatcher.group(1));
            int day = Integer.parseInt(monthDayMatcher.group(2));
            return LocalDate.now().withMonth(month).withDayOfMonth(day).format(DATE_FORMATTER);
        }
        return null;
    }

    private String parseDeptHint(String message) {
        Matcher matcher = DEPT_PATTERN.matcher(message == null ? "" : message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String parseDeptSubHint(String message) {
        Matcher matcher = DEPT_SUB_PATTERN.matcher(message == null ? "" : message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String parseDoctorHint(String message) {
        Matcher matcher = DOCTOR_PATTERN.matcher(message == null ? "" : message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Map<String, Object> parseSlotSelection(String message, Map<String, Object> memory) {
        Map<String, Object> result = new HashMap<>();
        if (!StringUtils.hasText(message) || memory == null) {
            return result;
        }
        Map<String, Object> lastObservation = mapValue(memory.get("lastObservation"));
        List<Map<String, Object>> slots = listValue(lastObservation.get("slots"));
        if (slots.isEmpty()) {
            return result;
        }
        String targetLabel = parseSlotLabel(message);
        if (!StringUtils.hasText(targetLabel)) {
            return result;
        }
        for (Map<String, Object> slot : slots) {
            if (targetLabel.equals(stringValue(slot.get("slotLabel"), null))) {
                result.putAll(slot);
                result.put("slotLabel", targetLabel);
                return result;
            }
        }
        return result;
    }

    private String parseSlotLabel(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher colonMatcher = SLOT_COLON_PATTERN.matcher(message);
        if (colonMatcher.find()) {
            return normalizeHourMinuteLabel(colonMatcher.group(1), colonMatcher.group(2), colonMatcher.group(3));
        }
        Matcher clockMatcher = SLOT_CLOCK_PATTERN.matcher(message);
        if (clockMatcher.find()) {
            String minute = clockMatcher.group(3) != null ? "30" : clockMatcher.group(4);
            if (!StringUtils.hasText(minute)) {
                minute = "00";
            }
            return normalizeHourMinuteLabel(clockMatcher.group(1), clockMatcher.group(2), minute);
        }
        return null;
    }

    private String normalizeHourMinuteLabel(String period, String hourText, String minuteText) {
        if (!StringUtils.hasText(hourText) || !StringUtils.hasText(minuteText)) {
            return null;
        }
        int hour;
        int minute;
        try {
            hour = Integer.parseInt(hourText);
            minute = Integer.parseInt(minuteText);
        } catch (Exception e) {
            return null;
        }
        if ("下午".equals(period) || "晚上".equals(period)) {
            if (hour < 12) {
                hour += 12;
            }
        } else if ("中午".equals(period) && hour < 11) {
            hour += 12;
        }
        return String.format("%02d:%02d", hour, minute);
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String... keys) {
        if (source == null || target == null || keys == null) {
            return;
        }
        for (String key : keys) {
            if (source.containsKey(key) && source.get(key) != null) {
                target.put(key, source.get(key));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listValue(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!(value instanceof List)) {
            return result;
        }
        List list = (List) value;
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(new HashMap<>((Map<String, Object>) item));
            }
        }
        return result;
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map) {
            return new HashMap<>((Map<String, Object>) value);
        }
        return new HashMap<>();
    }

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
