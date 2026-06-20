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
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentParser;
import com.example.hospital.patient.wx.api.agent.multi.nlu.ModelIntentResult;
import com.example.hospital.patient.wx.api.agent.multi.rag.MultiAgentRagService;
import com.example.hospital.patient.wx.api.agent.multi.support.MultiAgentRegistrationPayloadValidator;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import com.example.hospital.patient.wx.api.agent.support.AgentPlanStep;
import com.example.hospital.patient.wx.api.agent.support.AgentUiAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MultiAgentCoordinatorService {
    private static final String PROMPT_VERSION = "multi-agent-v1";

    private final MultiAgentProperties properties;
    private final MultiAgentMemoryService memoryService;
    private final MultiAgentRagService ragService;
    private final MultiAgentRegistrationPayloadValidator payloadValidator;
    private final MultiAgentDepartmentCatalogService departmentCatalogService;
    @Autowired(required = false)
    private MultiAgentTelemetryService telemetryService;
    @Autowired(required = false)
    private MedicalConsultService medicalConsultService;
    @Autowired(required = false)
    private ModelIntentParser modelIntentParser;
    private final Map<MultiAgentStage, AgentWorker> workerByStage = new EnumMap<>(MultiAgentStage.class);

    public MultiAgentCoordinatorService(MultiAgentProperties properties,
                                        MultiAgentMemoryService memoryService,
                                        MultiAgentRagService ragService,
                                        MultiAgentDepartmentCatalogService departmentCatalogService,
                                        List<AgentWorker> workers) {
        this.properties = properties;
        this.memoryService = memoryService;
        this.ragService = ragService;
        this.payloadValidator = new MultiAgentRegistrationPayloadValidator();
        this.departmentCatalogService = departmentCatalogService;
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

        long chatStartedAt = System.currentTimeMillis();
        PreparedPayload preparedPayload = composePayload(safeRequest, memory);
        if (preparedPayload.isTerminalFailure()) {
            return buildResponse(sessionId, preparedPayload.getReply(), MultiAgentStage.MANUAL_FALLBACK, memory, Collections.<AgentTraceEntry>emptyList(), chatStartedAt);
        }
        Map<String, Object> payload = preparedPayload.getPayload();
        AgentContext context = new AgentContext();
        context.setSessionId(sessionId);
        context.setRequestId(IdUtil.simpleUUID());
        context.setUserId(userId);
        context.setUserMessage(safeRequest.getMessage());
        context.setUserAction(safeRequest.getAction());
        context.setPayload(payload);
        context.setMemory(memory);
        context.setTrace(new ArrayList<AgentTraceEntry>());
        // NLU: parse intent and slots from user message
        Map<String, Object> nluPatch = new HashMap<>();
        Optional<ModelIntentResult> modelIntent = parseNlu(safeRequest.getMessage(), sessionId, nluPatch, memory);
        // When symptom changes, clear stale registration data to prevent dirty cache leaking across topics
        if (booleanValue(nluPatch.get("nluSymptomFresh"))) {
            String newSymptom = stringValue(nluPatch.get("symptom"));
            String oldSymptom = stringValue(memory.get("symptom"));
            if (StringUtils.hasText(newSymptom) && !newSymptom.equals(oldSymptom)) {
                String oldDept = stringValue(memory.get("deptName"));
                if (StringUtils.hasText(oldDept)) {
                    List<String> expectedDepts = SYMPTOM_EXPECTED_DEPTS.get(newSymptom);
                    if (expectedDepts != null && !expectedDepts.isEmpty() && !expectedDepts.contains(oldDept)) {
                        nluPatch.put("deptName", null);
                        nluPatch.put("deptId", null);
                        nluPatch.put("deptSubId", null);
                        nluPatch.put("deptSubName", null);
                    }
                }
                nluPatch.put("recommendedDeptName", null);
            }
            if (!nluPatch.containsKey("symptomDeptGraph")) {
                nluPatch.put("symptomDeptGraph", null);
            }
            if (!nluPatch.containsKey("symptomDiseaseInfo")) {
                nluPatch.put("symptomDiseaseInfo", null);
            }
        }
        // When user starts a new query with a different dept/date, clear stale pipeline state
        // from a previous conversation. Two cases:
        // 1) pendingOrder exists with different dept/date → reset to INTENT_PARSE
        // 2) no pendingOrder but memory has stale resolved date (e.g. "2026-06-20") that
        //    differs from the NLU raw value ("今天") → force-clear so putTextIfAbsent re-homes
        boolean newQueryHasParams = StringUtils.hasText(stringValue(nluPatch.get("deptName")))
                || StringUtils.hasText(stringValue(nluPatch.get("date")));
        if (newQueryHasParams) {
            String newDept = stringValue(nluPatch.get("deptName"));
            String newDate = stringValue(nluPatch.get("date"));
            boolean needReset = false;

            if (memory.get("pendingOrder") instanceof Map) {
                Map<?, ?> pending = (Map<?, ?>) memory.get("pendingOrder");
                String oldDept = stringValue(pending.get("deptSubName"));
                String oldDate = stringValue(pending.get("date"));
                if ((StringUtils.hasText(newDept) && !newDept.equals(oldDept))
                        || (StringUtils.hasText(newDate) && !newDate.equals(oldDate))) {
                    needReset = true;
                }
            }

            // Also detect stale resolved date/dept in memory itself (e.g. "今天" vs "2026-06-20")
            if (!needReset) {
                String memDept = stringValue(memory.get("deptName"));
                String memDate = stringValue(memory.get("date"));
                boolean deptStale = StringUtils.hasText(newDept) && !newDept.equals(memDept);
                boolean dateStale = StringUtils.hasText(newDate) && !newDate.equals(memDate);
                if (deptStale || dateStale) {
                    // Force-overwrite only the keys that actually changed; don't null out
                    // deptName when the NLU only provided a fresh date (e.g. "今天呢").
                    if (deptStale) nluPatch.put("deptName", newDept);
                    if (dateStale) nluPatch.put("date", newDate);
                    needReset = true;
                }
            }

            if (needReset) {
                nluPatch.put("stage", MultiAgentStage.INTENT_PARSE.name());
                nluPatch.put("pendingOrder", null);
                nluPatch.put("confirmed", null);
                nluPatch.put("awaitingConfirmation", null);
                nluPatch.put("deptId", null);
                nluPatch.put("deptSubId", null);
                nluPatch.put("deptSubName", null);
                nluPatch.put("doctorId", null);
            }
        }
        applyMemoryPatch(memory, nluPatch);

        // Sync NLU-fresh values back into payload so buildTrustedState doesn't pick up
        // stale pre-NLU values that composePayload copied from old memory.
        if (StringUtils.hasText(stringValue(nluPatch.get("date")))) {
            payload.put("date", nluPatch.get("date"));
        }
        if (StringUtils.hasText(stringValue(nluPatch.get("deptName")))) {
            payload.put("deptName", nluPatch.get("deptName"));
        }

        // Non-registration intents are handled directly without entering the worker pipeline
        String nluDirectReply = handleNluDirectIntent(modelIntent, safeRequest, payload, memory);
        if (nluDirectReply != null) {
            MultiAgentStage directStage = MultiAgentStage.DONE;
            if (booleanValue(memory.get("requestedView")) && !AgentUiAction.EXPLAIN_RECOMMENDATION.equals(stringValue(memory.get("requestedView")))) {
                directStage = MultiAgentStage.DONE;
            }
            return buildResponse(sessionId, nluDirectReply, directStage, memory, context.getTrace(), chatStartedAt);
        }

        // Registration intent — enter worker pipeline
        MultiAgentStage resolved = resolveStage(memory.get("stage"));
        MultiAgentStage startStage = (resolved == MultiAgentStage.INTENT_PARSE) ? MultiAgentStage.SLOT_QUERY : resolved;
        // Direct create with all params + confirmation skips slot query
        if (booleanValue(memory.get("confirmed")) && memory.get("pendingOrder") instanceof Map) {
            startStage = MultiAgentStage.POLICY_CHECK;
        }
        context.setStage(startStage);

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

            // query_doctor / query_department / context follow-up: stop after ScheduleAgentWorker
            // shows the doctor list or department suggestion instead of proceeding to POLICY_CHECK.
            if (isQueryOnlyIntent(memory, context)
                    && result.getHandoffAction() == HandoffAction.HANDOFF
                    && MultiAgentStage.POLICY_CHECK.equals(result.getNextStage())) {
                memory.put("awaitingConfirmation", false);
                memory.put("pendingOrder", null);
                finalStage = MultiAgentStage.DONE;
                break;
            }

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

        return buildResponse(sessionId, finalReply, finalStage, memory, context.getTrace(), chatStartedAt);
    }

    private AgentChatResponse buildResponse(String sessionId,
                                            String finalReply,
                                            MultiAgentStage finalStage,
                                            Map<String, Object> memory,
                                            List<AgentTraceEntry> trace,
                                            long chatStartedAt) {
        if (!StringUtils.hasText(finalReply)) {
            finalReply = fallbackReply(finalStage);
        }
        finalReply = enrichReplyByRag(finalReply, memory);
        memory.put("stage", finalStage.name());
        memory.put("lastReply", finalReply);
        memory.put("traceSize", trace == null ? 0 : trace.size());
        memory.put("chatLatencyMs", System.currentTimeMillis() - chatStartedAt);
        memory.put("finalState", resolveState(finalStage, memory));
        memoryService.save(sessionId, memory);

        AgentChatResponse response = new AgentChatResponse();
        response.setSessionId(sessionId);
        response.setSystemPromptVersion(PROMPT_VERSION);
        response.setReply(finalReply);
        response.setState(resolveState(finalStage, memory));
        response.setAgentFlows(buildAgentFlows(trace));
        response.setToolLogs(buildToolLogs(trace));
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
        appendRequestedViewCards(response, memory);
        appendFallbackCards(response, memory);
        appendSuggestedCards(response, memory);
        appendRagSourceCard(response, memory);
        if (telemetryService != null) {
            telemetryService.recordChat(sessionId, response.getMemory());
        }
        return response;
    }

    private String applyPreparedFailure(Map<String, Object> memory,
                                        String message,
                                        String badCaseType,
                                        Map<String, String> badFields,
                                        boolean clearPendingOrder,
                                        boolean clearAwaitingConfirmation) {
        memory.put("errorCode", MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH);
        memory.put("retryable", true);
        memory.put("errorMessage", message);
        memory.put("badCaseType", badCaseType);
        memory.put("badCaseStage", MultiAgentStage.POLICY_CHECK.name());
        memory.put("badFields", badFields == null ? Collections.emptyMap() : new HashMap<>(badFields));
        if (clearAwaitingConfirmation) {
            memory.put("awaitingConfirmation", false);
        }
        if (clearPendingOrder) {
            memory.remove("pendingOrder");
        }
        return message;
    }

    private void appendRagSourceCard(AgentChatResponse response, Map<String, Object> memory) {
        String ragSources = stringValue(memory.get("ragSources"));
        if (!StringUtils.hasText(ragSources)) {
            return;
        }
        appendActionCard(response, "知识来源", "本次解释参考了：" + ragSources, AgentUiAction.EXPLAIN_RECOMMENDATION, null, "RAG");
    }

    private void appendRequestedViewCards(AgentChatResponse response, Map<String, Object> memory) {
        String requestedView = stringValue(memory.get("requestedView"));
        if (!StringUtils.hasText(requestedView)) {
            return;
        }
        if (AgentUiAction.VIEW_MESSAGES.equals(requestedView)) {
            appendNavigateCard(response, "查看消息", "进入消息中心查看挂号提醒和系统通知", "/pages/message_list/message_list", "消息");
            return;
        }
        if (AgentUiAction.VIEW_USER_CARD.equals(requestedView)) {
            if (booleanValue(memory.get("hasUserCard"))) {
                appendNavigateCard(response, "查看就诊卡", "进入就诊卡详情页查看实名信息", "/user/user_info_card_detail", "实名");
            } else {
                appendNavigateCard(response, "去建卡", "当前还未建卡，先完善就诊卡信息", "/user/fill_user_info/fill_user_info", "建卡");
            }
            return;
        }
        if (AgentUiAction.MEDICAL_CONSULT.equals(requestedView)) {
            String riskLevel = stringValue(memory.get("medicalConsultRiskLevel"));
            String recommendedDeptName = stringValue(memory.get("recommendedDeptName"));
            if ("urgent".equals(riskLevel)) {
                appendNavigateCard(response, "尽快就医", StringUtils.hasText(recommendedDeptName)
                        ? "胸痛存在红旗征象，建议尽快线下就医，优先 " + recommendedDeptName
                        : "胸痛存在红旗征象，建议尽快线下就医", "/registration/medical_dept_list/medical_dept_list", "紧急");
            } else {
                appendNavigateCard(response, "继续挂号", StringUtils.hasText(recommendedDeptName)
                        ? "我先建议你优先看 " + recommendedDeptName + "，也可以继续进入挂号流程"
                        : "我先帮你继续进入挂号流程", "/registration/medical_dept_list/medical_dept_list", "推荐");
            }
            return;
        }
        if (AgentUiAction.VIEW_REGISTRATIONS.equals(requestedView)) {
            appendNavigateCard(response, "查看我的挂号", "进入“我的挂号”查看已有预约记录", "/pages/registration_list/registration_list", "结果");
            return;
        }
        if (AgentUiAction.EXPLAIN_RECOMMENDATION.equals(requestedView)) {
            appendNavigateCard(response, "普通挂号", "如果不想继续当前推荐，也可以改走普通挂号流程", "/registration/notice/notice", "兜底");
        }
    }

    private void appendSuggestedCards(AgentChatResponse response, Map<String, Object> memory) {
        if (booleanValue(memory.get("awaitingConfirmation"))) {
            appendActionCard(response, "为什么推荐这个号源", "解释当前推荐路径和继续确认的原因", AgentUiAction.EXPLAIN_RECOMMENDATION, null, "解释");
            appendNavigateCard(response, "普通挂号", "若不想继续当前推荐，可切换到普通挂号流程", "/registration/notice/notice", "兜底");
            appendNavigateCard(response, "查看我的挂号", "若你已经提交过，也可以直接查看已有挂号记录", "/pages/registration_list/registration_list", "结果");
            return;
        }
        if (memory.get("pendingOrder") instanceof Map) {
            appendActionCard(response, "为什么推荐当前结果", "查看当前诊室、日期和号源的推荐原因", AgentUiAction.EXPLAIN_RECOMMENDATION, null, "解释");
        }
    }

    private static final Map<String, List<String>> SYMPTOM_EXPECTED_DEPTS = new LinkedHashMap<>();

    static {
        SYMPTOM_EXPECTED_DEPTS.put("胸疼", Arrays.asList("心内科", "胸外科", "呼吸内科"));
        SYMPTOM_EXPECTED_DEPTS.put("胸痛", Arrays.asList("心内科", "胸外科", "呼吸内科"));
        SYMPTOM_EXPECTED_DEPTS.put("胸闷", Arrays.asList("心内科", "呼吸内科"));
        SYMPTOM_EXPECTED_DEPTS.put("头疼", Arrays.asList("神经内科"));
        SYMPTOM_EXPECTED_DEPTS.put("牙疼", Arrays.asList("口腔科"));
        SYMPTOM_EXPECTED_DEPTS.put("咳嗽", Arrays.asList("呼吸内科"));
        SYMPTOM_EXPECTED_DEPTS.put("胃疼", Arrays.asList("消化内科"));
        SYMPTOM_EXPECTED_DEPTS.put("腹痛", Arrays.asList("消化内科"));
        SYMPTOM_EXPECTED_DEPTS.put("皮疹", Arrays.asList("皮肤科"));
        SYMPTOM_EXPECTED_DEPTS.put("骨折", Arrays.asList("骨科"));
    }

    @SuppressWarnings("unchecked")
    private String enrichReplyByRag(String reply, Map<String, Object> memory) {
        String requestedView = stringValue(memory.get("requestedView"));

        // ── Symptom analysis (non-intrusive, prepended to registration reply) ──
        boolean symptomFresh = booleanValue(memory.get("nluSymptomFresh"));
        List<String> symptoms = null;
        Object symptomsObj = memory.get("symptoms");
        if (symptomsObj instanceof List) {
            symptoms = (List<String>) symptomsObj;
        }
        if (symptomFresh && symptoms != null && !symptoms.isEmpty() && ragService != null) {
            String deptName = firstText(memory.get("deptName"), memory.get("recommendedDeptName"));
            boolean hasConsultDemand = booleanValue(memory.get("hasConsultDemand"));
            StringBuilder symptomAnalysis = new StringBuilder();
            StringBuilder crossDeptReminders = new StringBuilder();
            StringBuilder deptSuggestions = new StringBuilder();

            // Dynamic graph data from Python NLU (takes priority); static table as fallback
            @SuppressWarnings("unchecked")
            Map<String, List<String>> dynamicGraph = memory.get("symptomDeptGraph") instanceof Map
                    ? (Map<String, List<String>>) memory.get("symptomDeptGraph")
                    : null;

            // Graph-retrieved disease facts (replaces LLM RAG for symptom analysis)
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, String>>> diseaseInfo = memory.get("symptomDiseaseInfo") instanceof Map
                    ? (Map<String, List<Map<String, String>>>) memory.get("symptomDiseaseInfo")
                    : null;

            for (int i = 0; i < symptoms.size(); i++) {
                String sym = symptoms.get(i);
                if (!StringUtils.hasText(sym)) continue;

                // Build symptom analysis from graph disease facts (no LLM hallucination risk)
                StringBuilder symText = new StringBuilder();
                if (diseaseInfo != null && diseaseInfo.containsKey(sym)) {
                    List<Map<String, String>> diseases = diseaseInfo.get(sym);
                    for (int j = 0; j < diseases.size() && j < 2; j++) {
                        Map<String, String> d = diseases.get(j);
                        if (j == 0) {
                            symText.append("「").append(sym).append("」");
                        }
                        String desc = d.get("description");
                        if (StringUtils.hasText(desc)) {
                            symText.append(" 常见相关疾病：").append(d.get("name")).append("——").append(desc);
                        }
                        if (hasConsultDemand) {
                            String cause = d.get("cause");
                            if (StringUtils.hasText(cause)) {
                                symText.append(" ").append(cause);
                            }
                        }
                    }
                }
                if (symText.length() == 0 && ragService != null) {
                    // Fallback to RAG only when Neo4j has no disease data for this symptom
                    String ragQuestion = hasConsultDemand
                            ? sym + "是什么原因引起的，常见注意事项和就医建议是什么？"
                            : sym + "是什么原因引起的，常见注意事项是什么？";
                    MultiAgentRagService.RagAnswer symAnswer = ragService.answer(ragQuestion, memory);
                    if (symAnswer != null && StringUtils.hasText(symAnswer.getAnswer())) {
                        symText.append(symAnswer.getAnswer().trim());
                    }
                }
                if (symText.length() > 0) {
                    if (i > 0) symptomAnalysis.append("\n");
                    symptomAnalysis.append(symText.toString().trim());
                }

                // Resolve recommended departments for this symptom
                List<String> recommendedDepts = null;
                if (dynamicGraph != null && dynamicGraph.containsKey(sym)) {
                    recommendedDepts = dynamicGraph.get(sym);
                }
                if (recommendedDepts == null || recommendedDepts.isEmpty()) {
                    recommendedDepts = SYMPTOM_EXPECTED_DEPTS.get(sym);
                }

                if (!StringUtils.hasText(deptName)) {
                    // No department specified → suggest departments based on medical knowledge (not availability guarantee)
                    if (recommendedDepts != null && !recommendedDepts.isEmpty()) {
                        if (deptSuggestions.length() == 0) {
                            deptSuggestions.append("根据您的症状，以下科室可能适合（具体号源请确认日期后查询）：");
                        }
                        for (String dept : recommendedDepts) {
                            deptSuggestions.append("\n· ").append(dept);
                        }
                    }
                } else {
                    // Department specified → cross-department mismatch check
                    if (recommendedDepts != null && !recommendedDepts.isEmpty() && !recommendedDepts.contains(deptName)) {
                        crossDeptReminders.append(sym).append("优先推荐");
                        crossDeptReminders.append(String.join("、", recommendedDepts));
                        crossDeptReminders.append("，您选择").append(deptName).append("可排查骨骼、软骨等相关问题。");
                    }
                }
            }

            if (deptSuggestions.length() > 0) {
                if (symptomAnalysis.length() > 0) {
                    symptomAnalysis.append("\n\n");
                }
                symptomAnalysis.append(deptSuggestions);
            }
            if (symptomAnalysis.length() > 0) {
                if (crossDeptReminders.length() > 0) {
                    symptomAnalysis.append("\n\n【科室提醒】").append(crossDeptReminders);
                }
                // Chest pain always gets a safety overlay regardless of hasConsultDemand,
                // so that even bare "胸痛" without consult keywords triggers a warning.
                boolean hasChestPain = false;
                for (String sym : symptoms) {
                    if (containsAny(sym, "胸痛", "胸疼", "胸闷")) {
                        hasChestPain = true;
                        break;
                    }
                }
                if (hasChestPain) {
                    String chestWarning = "【安全提醒】胸痛可能是心脏急症的警示信号。如伴有气短、大汗、放射痛或晕厥，请立即前往急诊，不要仅依赖线上判断。";
                    symptomAnalysis.insert(0, chestWarning + "\n\n");
                }
                memory.put("symptomAnalysisGenerated", true);
                String analysis = symptomAnalysis.toString();
                // When analysis has meaningful content and the pipeline reply is just
                // asking for department/date, don't concatenate — it's confusing.
                boolean isAskDeptPrompt = StringUtils.hasText(reply)
                        && (reply.contains("请告诉我想挂哪个科室"));
                if (isAskDeptPrompt) {
                    reply = analysis + "\n\n告诉我日期，我来查号源。";
                } else {
                    reply = analysis + (StringUtils.hasText(reply) ? "\n\n" + reply : "");
                }
            }
        }

        // ── Existing: explain recommendation ──
        if (!AgentUiAction.EXPLAIN_RECOMMENDATION.equals(requestedView) || ragService == null) {
            return reply;
        }
        String question = firstText(memory.get("ragQuestion"), reply, "为什么推荐当前结果");
        MultiAgentRagService.RagAnswer ragAnswer = ragService.answer(question, memory);
        if (ragAnswer == null || !StringUtils.hasText(ragAnswer.getAnswer())) {
            return reply;
        }
        if (ragAnswer.getSnippets() != null && !ragAnswer.getSnippets().isEmpty()) {
            StringBuilder sourceBuilder = new StringBuilder();
            for (int i = 0; i < ragAnswer.getSnippets().size(); i++) {
                if (i > 0) {
                    sourceBuilder.append("、");
                }
                sourceBuilder.append(ragAnswer.getSnippets().get(i).getTitle());
            }
            memory.put("ragSources", sourceBuilder.toString());
        } else {
            memory.remove("ragSources");
        }
        memory.put("ragAnswerGenerated", ragAnswer.isLlmGenerated());
        memory.put("ragMode", ragAnswer.getMode());
        memory.put("ragHitCount", ragAnswer.getHitCount());
        memory.put("ragScoreMax", ragAnswer.getMaxScore());
        memory.put("ragFallbackReason", ragAnswer.getFallbackReason());
        memory.put("ragLatencyMs", ragAnswer.getLatencyMs());
        memory.put("ragPromptTokens", ragAnswer.getPromptTokens());
        memory.put("ragCompletionTokens", ragAnswer.getCompletionTokens());
        memory.put("ragCacheHit", ragAnswer.isCacheHit());
        return ragAnswer.getAnswer();
    }

    // ── Medical QA: Neo4j → LLM synthesis ──

    private String handleMedicalQa(String question, Map<String, Object> memory) {
        @SuppressWarnings("unchecked")
        Map<String, Object> diseaseInfo = memory.get("diseaseInfo") instanceof Map
                ? (Map<String, Object>) memory.get("diseaseInfo")
                : null;

        if (diseaseInfo == null || diseaseInfo.isEmpty()) {
            return "抱歉，我目前的知识库中暂时没有查到该疾病的相关信息。你可以尝试询问具体的疾病名称，例如：感冒吃什么药？";
        }

        // Answer already formatted by Python NLU from Neo4j data — no second LLM needed.
        String pythonAnswer = stringValue(memory.get("medicalQaAnswer"));
        if (StringUtils.hasText(pythonAnswer)) {
            return pythonAnswer;
        }

        if (ragService == null) {
            return formatDiseaseInfoText(diseaseInfo, question);
        }

        try {
            String synthesized = ragService.synthesizeFromGraph(diseaseInfo, question);
            if (StringUtils.hasText(synthesized)) {
                return synthesized;
            }
        } catch (Exception e) {
            // LLM synthesis failed — use structured fallback
        }
        return formatDiseaseInfoText(diseaseInfo, question);
    }

    private String formatDiseaseInfoText(Map<String, Object> d, String question) {
        StringBuilder sb = new StringBuilder();
        String name = stringValue(d.get("name"));
        sb.append("关于「").append(name).append("」：\n");
        String desc = stringValue(d.get("desc"));
        if (StringUtils.hasText(desc)) {
            sb.append("简介：").append(desc).append("\n");
        }
        String drugs = stringValue(d.get("drugs"));
        if (StringUtils.hasText(drugs)) {
            sb.append("常用药物：").append(drugs).append("\n");
        }
        String cureWay = stringValue(d.get("cure_way"));
        if (StringUtils.hasText(cureWay)) {
            sb.append("治疗方式：").append(cureWay).append("\n");
        }
        String prevent = stringValue(d.get("prevent"));
        if (StringUtils.hasText(prevent)) {
            sb.append("预防建议：").append(prevent).append("\n");
        }
        String doEat = stringValue(d.get("do_eat"));
        if (StringUtils.hasText(doEat)) {
            sb.append("推荐饮食：").append(doEat).append("\n");
        }
        String notEat = stringValue(d.get("not_eat"));
        if (StringUtils.hasText(notEat)) {
            sb.append("忌食：").append(notEat).append("\n");
        }
        return sb.toString();
    }

    private void appendFallbackCards(AgentChatResponse response, Map<String, Object> memory) {
        if (booleanValue(memory.get("nluUnavailable"))) {
            appendNavigateCard(response, "普通挂号", "NLU 服务暂不可用，请改走普通挂号流程", "/registration/notice/notice", "兜底");
            return;
        }
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

    private void appendActionCard(AgentChatResponse response, String title, String description, String action, Map<String, Object> payload, String badge) {
        if (response.getCards() != null) {
            for (AgentResponseCard existing : response.getCards()) {
                if (existing == null || existing.getPayload() == null && payload != null) {
                    continue;
                }
                if (title.equals(existing.getTitle()) && action.equals(existing.getAction())) {
                    return;
                }
            }
        }
        AgentResponseCard card = new AgentResponseCard();
        card.setType("action");
        card.setTitle(title);
        card.setDescription(description);
        card.setBadge(badge);
        card.setAction(action);
        card.setPayload(payload == null ? new HashMap<String, Object>() : new HashMap<>(payload));
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
        steps.add(new AgentPlanStep("triage", "识别需求", "completed"));
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
        result.put("doctorGender", memory.get("doctorGender"));
        result.put("doctorAgePreference", memory.get("doctorAgePreference"));
        result.put("patientGender", memory.get("patientGender"));
        result.put("recommendedDeptName", memory.get("recommendedDeptName"));
        result.put("medicalConsultRiskLevel", memory.get("medicalConsultRiskLevel"));
        result.put("medicalConsultAdvice", memory.get("medicalConsultAdvice"));
        result.put("medicalConsultRecommendation", memory.get("medicalConsultRecommendation"));
        result.put("medicalConsultRagSources", memory.get("medicalConsultRagSources"));
        result.put("medicalConsultDoctorGenderHint", memory.get("medicalConsultDoctorGenderHint"));
        result.put("hasPendingOrder", memory.get("pendingOrder") instanceof Map && !((Map<?, ?>) memory.get("pendingOrder")).isEmpty());
        result.put("awaitingConfirmation", booleanValue(memory.get("awaitingConfirmation")));
        result.put("requiresLogin", booleanValue(memory.get("requiresLogin")));
        result.put("errorCode", memory.get("errorCode"));
        result.put("retryable", memory.get("retryable"));
        result.put("errorMessage", memory.get("errorMessage"));
        result.put("badCaseType", memory.get("badCaseType"));
        result.put("badCaseStage", memory.get("badCaseStage"));
        result.put("badFields", memory.get("badFields"));
        result.put("replayDecision", memory.get("replayDecision"));
        result.put("requestedView", memory.get("requestedView"));
        result.put("ragSources", memory.get("ragSources"));
        result.put("ragAnswerGenerated", memory.get("ragAnswerGenerated"));
        result.put("ragMode", memory.get("ragMode"));
        result.put("ragHitCount", memory.get("ragHitCount"));
        result.put("ragScoreMax", memory.get("ragScoreMax"));
        result.put("ragFallbackReason", memory.get("ragFallbackReason"));
        result.put("ragLatencyMs", memory.get("ragLatencyMs"));
        result.put("ragPromptTokens", memory.get("ragPromptTokens"));
        result.put("ragCompletionTokens", memory.get("ragCompletionTokens"));
        result.put("ragCacheHit", memory.get("ragCacheHit"));
        result.put("chatLatencyMs", memory.get("chatLatencyMs"));
        result.put("nluIntent", memory.get("nluIntent"));
        result.put("nluSource", memory.get("nluSource"));
        result.put("nluModel", memory.get("nluModel"));
        result.put("nluConfidence", memory.get("nluConfidence"));
        result.put("nluLatencyMs", memory.get("nluLatencyMs"));
        result.put("finalState", memory.get("finalState"));
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

    private void applyConsultPatch(Map<String, Object> memory, MedicalConsultService.ConsultResult consultResult) {
        if (consultResult == null) {
            return;
        }
        applyMemoryPatch(memory, consultResult.getMemoryPatch());
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
    private PreparedPayload composePayload(AgentChatRequest request, Map<String, Object> memory) {
        Map<String, Object> rawPayload = new HashMap<>();
        if (request != null && request.getPayload() != null) {
            rawPayload.putAll(request.getPayload());
        }
        MultiAgentRegistrationPayloadValidator.ValidationResult normalizedChatPayload = payloadValidator.normalizeChatPayload(rawPayload);
        if (!normalizedChatPayload.isValid()) {
            return PreparedPayload.failure(applyPreparedFailure(memory,
                    normalizedChatPayload.getMessage(),
                    normalizedChatPayload.getBadCaseType(),
                    normalizedChatPayload.getBadFields(),
                    true,
                    true));
        }
        Map<String, Object> payload = new HashMap<>(normalizedChatPayload.getNormalized());
        Map<String, Object> pendingOrder = memory.get("pendingOrder") instanceof Map
                ? new HashMap<>((Map<String, Object>) memory.get("pendingOrder"))
                : null;
        if (pendingOrder != null) {
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
        boolean confirmationAction = request != null && AgentAction.CREATE_REGISTRATION.equals(request.getAction());
        boolean confirmed = booleanValue(payload.get("confirmed"));
        if (!confirmationAction && !confirmed) {
            return PreparedPayload.success(payload);
        }
        if (!booleanValue(memory.get("awaitingConfirmation")) || pendingOrder == null || pendingOrder.isEmpty()) {
            return PreparedPayload.failure(applyPreparedFailure(memory,
                    "当前确认信息已失效，请重新选择号源后再试。",
                    "confirmation_mismatch",
                    Collections.singletonMap("confirmed", "当前不处于待确认状态"),
                    true,
                    true));
        }
        MultiAgentRegistrationPayloadValidator.ValidationResult confirmationPayload = payloadValidator.normalizeConfirmationPayload(payload);
        if (!confirmationPayload.isValid() || !payloadValidator.matchesPendingOrder(confirmationPayload.getNormalized(), pendingOrder)) {
            Map<String, String> badFields = new LinkedHashMap<>(confirmationPayload.getBadFields());
            if (confirmationPayload.isValid()) {
                payloadValidator.buildMismatchFields(confirmationPayload.getNormalized(), pendingOrder, badFields);
            }
            return PreparedPayload.failure(applyPreparedFailure(memory,
                    "当前确认信息已变化，请重新选择号源后再试。",
                    "confirmation_mismatch",
                    badFields,
                    true,
                    true));
        }
        payload.put("confirmed", true);
        return PreparedPayload.success(payload);
    }

    private void mergeFromMemory(Map<String, Object> payload, Map<String, Object> memory, String key) {
        if (!payload.containsKey(key) && memory.containsKey(key)) {
            payload.put(key, memory.get(key));
        }
    }

    private MultiAgentStage resolveStage(Object value) {
        String stage = value == null ? null : String.valueOf(value);
        if (!StringUtils.hasText(stage)) {
            return MultiAgentStage.SLOT_QUERY;
        }
        try {
            MultiAgentStage resolved = MultiAgentStage.valueOf(stage);
            if (resolved == MultiAgentStage.CONFIRM_WAIT) {
                return MultiAgentStage.POLICY_CHECK;
            }
            if (resolved == MultiAgentStage.DONE || resolved == MultiAgentStage.MANUAL_FALLBACK || resolved == MultiAgentStage.INTENT_PARSE) {
                return MultiAgentStage.SLOT_QUERY;
            }
            return resolved;
        } catch (IllegalArgumentException ignored) {
            if ("awaiting_confirmation".equals(stage) || "await_confirm".equals(stage)) {
                return MultiAgentStage.POLICY_CHECK;
            }
            return MultiAgentStage.SLOT_QUERY;
        }
    }

    // ── NLU ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Optional<ModelIntentResult> parseNlu(String message, String sessionId, Map<String, Object> patch, Map<String, Object> memory) {
        if (modelIntentParser == null || !StringUtils.hasText(message)) {
            return Optional.empty();
        }
        List<String> departments = departmentCatalogService == null ? Collections.<String>emptyList() : departmentCatalogService.getDepartmentNames();
        Optional<ModelIntentResult> parsed = modelIntentParser.parse(message, sessionId, departments);
        if (!parsed.isPresent()) {
            // No NLU result — clear stale symptom data
            patch.put("nluSymptomFresh", false);
            patch.put("symptom", null);
            patch.put("symptoms", null);
            return Optional.empty();
        }
        ModelIntentResult result = parsed.get();
        patch.put("nluIntent", result.getIntent());
        patch.put("nluConfidence", result.getConfidence());
        patch.put("nluSource", firstText(result.getSource(), result.getEngine(), "model"));
        patch.put("nluModel", result.getModel());
        patch.put("nluLatencyMs", result.getLatencyMs());
        Map<String, Object> slots = result.getSlots() != null ? new HashMap<>(result.getSlots()) : new HashMap<>();

        // Multi-symptom extraction: support both symptom (single) and symptoms (list)
        Object symptomsObj = slots.get("symptoms");
        List<String> symptomsList = null;
        if (symptomsObj instanceof List) {
            symptomsList = (List<String>) symptomsObj;
        } else if (symptomsObj instanceof String) {
            symptomsList = Collections.singletonList((String) symptomsObj);
        }
        Object symptomObj = slots.get("symptom");
        String firstSymptom = symptomObj instanceof String ? (String) symptomObj : null;
        if (symptomsList == null || symptomsList.isEmpty()) {
            if (StringUtils.hasText(firstSymptom)) {
                symptomsList = Collections.singletonList(firstSymptom);
            } else {
                symptomsList = Collections.emptyList();
            }
        }
        if (!StringUtils.hasText(firstSymptom) && !symptomsList.isEmpty()) {
            firstSymptom = symptomsList.get(0);
        }

        boolean hasSymptoms = !symptomsList.isEmpty();
        patch.put("nluSymptomFresh", hasSymptoms);
        if (hasSymptoms) {
            patch.put("symptom", firstSymptom);
            patch.put("symptoms", new ArrayList<>(symptomsList));
        } else {
            patch.put("symptom", null);
            patch.put("symptoms", null);
        }

        putTextIfAbsent(patch, "patientGender", slots.get("patientGender"));
        putTextIfAbsent(patch, "deptName", slots.get("department"));
        putTextIfAbsent(patch, "doctorName", slots.get("doctorName"));
        putTextIfAbsent(patch, "date", slots.get("date"));
        putTextIfAbsent(patch, "timePreference", slots.get("timePreference"));
        putTextIfAbsent(patch, "doctorGender", slots.get("doctorGender"));
        putTextIfAbsent(patch, "doctorAgePreference", slots.get("doctorAgePreference"));
        putTextIfAbsent(patch, "population", slots.get("population"));
        if (slots.get("hasConsultDemand") instanceof Boolean) {
            patch.put("hasConsultDemand", slots.get("hasConsultDemand"));
        }
        if (result.getSymptomDeptGraph() != null && !result.getSymptomDeptGraph().isEmpty()) {
            patch.put("symptomDeptGraph", new LinkedHashMap<>(result.getSymptomDeptGraph()));
        } else if (hasSymptoms) {
            patch.put("symptomDeptGraph", null); // clear stale graph from previous topic
        }
        if (result.getSymptomDiseaseInfo() != null && !result.getSymptomDiseaseInfo().isEmpty()) {
            patch.put("symptomDiseaseInfo", new LinkedHashMap<>(result.getSymptomDiseaseInfo()));
        } else if (hasSymptoms) {
            patch.put("symptomDiseaseInfo", null);
        }
        if (result.getDiseaseInfo() != null && !result.getDiseaseInfo().isEmpty()) {
            patch.put("diseaseInfo", new LinkedHashMap<>(result.getDiseaseInfo()));
        } else {
            patch.put("diseaseInfo", null);
        }
        if (StringUtils.hasText(result.getMedicalQaAnswer())) {
            patch.put("medicalQaAnswer", result.getMedicalQaAnswer());
        } else {
            patch.put("medicalQaAnswer", null);
        }
        return parsed;
    }

    private String handleCardAction(AgentChatRequest request, Map<String, Object> payload, Map<String, Object> memory) {
        String action = request.getAction();
        if (!StringUtils.hasText(action)) {
            return null;
        }
        if (AgentUiAction.EXPLAIN_RECOMMENDATION.equals(action)) {
            memory.put("requestedView", AgentUiAction.EXPLAIN_RECOMMENDATION);
            memory.put("ragQuestion", firstText(request.getMessage(), "为什么推荐当前结果"));
            memory.put("awaitingConfirmation", false);
            return "我先结合当前挂号上下文和知识库为你解释一下。";
        }
        if (AgentUiAction.VIEW_REGISTRATIONS.equals(action)) {
            memory.put("requestedView", AgentUiAction.VIEW_REGISTRATIONS);
            return "可以直接查看我的挂号记录。若你想继续挂号，也可以告诉我科室和日期。";
        }
        if (AgentUiAction.VIEW_MESSAGES.equals(action)) {
            memory.put("requestedView", AgentUiAction.VIEW_MESSAGES);
            return "可以直接去消息中心查看挂号提醒和系统通知。";
        }
        if (AgentUiAction.VIEW_USER_CARD.equals(action)) {
            memory.put("requestedView", AgentUiAction.VIEW_USER_CARD);
            return "可以先查看就诊卡状态；如果还没建卡，我也会给你补建卡入口。";
        }
        if (AgentUiAction.MEDICAL_CONSULT.equals(action)) {
            if (medicalConsultService == null) {
                memory.put("errorCode", null);
                return "我先帮你做一个保守的胸痛分诊判断，但当前咨询服务暂时不可用。";
            }
            MedicalConsultService.ConsultResult consultResult = medicalConsultService.consult(request.getMessage(), memory);
            applyConsultPatch(memory, consultResult);
            memory.put("requestedView", AgentUiAction.MEDICAL_CONSULT);
            return consultResult == null || !StringUtils.hasText(consultResult.getReply())
                    ? "我先帮你做一个保守的胸痛分诊判断。"
                    : consultResult.getReply();
        }
        return null;
    }

    private String handleNluDirectIntent(Optional<ModelIntentResult> modelIntent, AgentChatRequest request,
                                          Map<String, Object> payload, Map<String, Object> memory) {
        Map<String, Object> r = new HashMap<>(payload);
        r.put("confirmed", memory.get("awaitingConfirmation") != null && booleanValue(memory.get("awaitingConfirmation")));

        // Direct create: explicit card action with all registration params + confirmation.
        // Free-text messages (no action) must go through NLU first so intents like
        // "explain_recommendation" / "query_message" can short-circuit before the pipeline.
        if (StringUtils.hasText(request.getAction()) && isDirectCreate(request.getAction(), r)) {
            memory.put("confirmed", true);
            return null; // proceed to worker pipeline (will go to POLICY_CHECK)
        }

        // Card-driven actions that don't need NLU
        String cardDrivenReply = handleCardAction(request, payload, memory);
        if (cardDrivenReply != null) {
            return cardDrivenReply;
        }

        if (!modelIntent.isPresent()) {
            if (!StringUtils.hasText(request.getMessage())) {
                memory.put("errorCode", null);
                return "你好，我是挂号助手。请告诉我你想挂哪个科室、哪一天，我来帮你查询号源。";
            }
            memory.put("errorCode", null);
            memory.put("nluUnavailable", true);
            return "NLU 服务暂不可用，请改走普通挂号流程。";
        }

        String intent = modelIntent.get().getIntent();

        // Dangerous intent — block immediately
        if ("dangerous".equals(intent)) {
            memory.put("errorCode", null);
            return "无法处理该请求，请重新输入。";
        }

        // Medical QA: Neo4j graph → LLM synthesis
        if ("medical_qa".equals(intent)) {
            return handleMedicalQa(request.getMessage(), memory);
        }

        // Direct non-registration intents
        if ("query_message".equals(intent)) {
            memory.put("requestedView", AgentUiAction.VIEW_MESSAGES);
            return "可以直接去消息中心查看挂号提醒和系统通知。若你想继续挂号，也可以告诉我科室和日期。";
        }
        if ("query_user_card".equals(intent)) {
            memory.put("requestedView", AgentUiAction.VIEW_USER_CARD);
            return "可以先查看就诊卡状态；如果还没建卡，我也会给你补建卡入口。";
        }
        if ("explain_recommendation".equals(intent)) {
            memory.put("requestedView", AgentUiAction.EXPLAIN_RECOMMENDATION);
            memory.put("ragQuestion", firstText(request.getMessage(), "为什么推荐当前结果"));
            // Don't show the confirm button while the user is asking "why" —
            // keep pendingOrder for RAG context but clear the awaiting flag.
            memory.put("awaitingConfirmation", false);
            return "我先结合当前挂号上下文和知识库为你解释一下。";
        }
        if ("unsupported".equals(intent) || "unknown".equals(intent)) {
            // If NLU already extracted a deptName, proceed to pipeline despite intent label
            if (StringUtils.hasText(stringValue(memory.get("deptName")))) {
                return null;
            }
            memory.put("errorCode", null);
            return "当前多 Agent 仅支持挂号相关操作，请告诉我科室和日期。";
        }

        // registration / query_doctor / query_department → proceed to worker pipeline.
        // For registration: if the user is only consulting (hasConsultDemand=true, no
        // registration action words like 挂/预约), skip the pipeline and let
        // enrichReplyByRag produce the symptom analysis directly.
        if ("registration".equals(intent)) {
            boolean hasRegKeyword = containsAny(request.getMessage(), "挂", "预约", "号", "号源", "看诊", "就诊");
            if (hasRegKeyword) {
                memory.put("hasConsultDemand", false);
            } else if (booleanValue(memory.get("hasConsultDemand"))) {
                return "我先结合你的症状信息为你分析一下。";
            }
        }
        return null;
    }

    private boolean isDirectCreate(String action, Map<String, Object> payload) {
        if (AgentAction.CREATE_REGISTRATION.equals(action)) return true;
        if (booleanValue(payload.get("confirmed"))) return true;
        return payload.get("workPlanId") != null
                && payload.get("scheduleId") != null
                && payload.get("doctorId") != null
                && payload.get("deptSubId") != null
                && payload.get("date") != null;
    }

    private void putTextIfAbsent(Map<String, Object> patch, String key, Object value) {
        if (patch.get(key) != null) return;
        String text = firstText(value);
        if (StringUtils.hasText(text)) patch.put(key, text);
    }

    private boolean isQueryOnlyIntent(Map<String, Object> memory, AgentContext context) {
        String intent = stringValue(memory.get("nluIntent"));
        // Explicit doctor/slot/department query
        if ("query_doctor".equals(intent) || "query_department".equals(intent)) return true;
        // NLU couldn't classify ("今天呢", "后天呢", etc.) but user isn't asking to register —
        // they're just changing a parameter in the current context.
        if ("unknown".equals(intent)
                && context != null
                && !containsAny(context.getUserMessage(), "挂", "预约")
                && StringUtils.hasText(stringValue(memory.get("deptName")))) {
            return true;
        }
        return false;
    }

    private static boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
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
            case "view_messages_requested":
                return "已切换到消息中心入口";
            case "view_user_card_requested":
                return "已切换到就诊卡入口";
            case "view_registrations_requested":
                return "已切换到挂号记录入口";
            case "explanation_requested":
                return "已说明当前推荐原因";
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

    private static class PreparedPayload {
        private final Map<String, Object> payload;
        private final boolean terminalFailure;
        private final String reply;

        private PreparedPayload(Map<String, Object> payload, boolean terminalFailure, String reply) {
            this.payload = payload;
            this.terminalFailure = terminalFailure;
            this.reply = reply;
        }

        private static PreparedPayload success(Map<String, Object> payload) {
            return new PreparedPayload(payload, false, null);
        }

        private static PreparedPayload failure(String reply) {
            return new PreparedPayload(new HashMap<String, Object>(), true, reply);
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public boolean isTerminalFailure() {
            return terminalFailure;
        }

        public String getReply() {
            return reply;
        }
    }
}
