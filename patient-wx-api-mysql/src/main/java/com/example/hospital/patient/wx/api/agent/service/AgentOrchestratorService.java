package com.example.hospital.patient.wx.api.agent.service;

import cn.hutool.core.util.IdUtil;
import com.example.hospital.patient.wx.api.agent.config.AgentPromptCatalog;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.dto.AgentConfirmation;
import com.example.hospital.patient.wx.api.agent.dto.AgentModelDecision;
import com.example.hospital.patient.wx.api.agent.dto.AgentResponseCard;
import com.example.hospital.patient.wx.api.agent.dto.AgentToolLog;
import com.example.hospital.patient.wx.api.agent.memory.AgentConversationMemoryService;
import com.example.hospital.patient.wx.api.agent.support.AgentAction;
import com.example.hospital.patient.wx.api.agent.support.AgentPlanStep;
import com.example.hospital.patient.wx.api.agent.support.AgentUiAction;
import com.example.hospital.patient.wx.api.agent.tool.DoctorAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.MedicalDeptAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.MessageAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.UserAgentTools;
import com.example.hospital.patient.wx.api.common.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AgentOrchestratorService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DATE_HINT_PATTERN = Pattern.compile("(今天|明天|后天|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}月\\d{1,2}日)");
    private static final Map<String, List<String>> DEPT_KEYWORD_RULES = new LinkedHashMap<>();
    private static final Map<Integer, String> SLOT_TIME = new LinkedHashMap<>();

    static {
        DEPT_KEYWORD_RULES.put("\u53E3\u8154\u79D1", Arrays.asList(
                "\u53E3\u8154",
                "\u7259\u75DB",
                "\u7259\u75BC",
                "\u7259\u9F7F",
                "\u7259\u9F88",
                "\u667A\u9F7F",
                "\u86C0\u7259",
                "\u7259\u5468"
        ));
        SLOT_TIME.put(1, "08:00");
        SLOT_TIME.put(2, "08:30");
        SLOT_TIME.put(3, "09:00");
        SLOT_TIME.put(4, "09:30");
        SLOT_TIME.put(5, "10:00");
        SLOT_TIME.put(6, "10:30");
        SLOT_TIME.put(7, "11:00");
        SLOT_TIME.put(8, "11:30");
        SLOT_TIME.put(9, "13:00");
        SLOT_TIME.put(10, "13:30");
        SLOT_TIME.put(11, "14:00");
        SLOT_TIME.put(12, "14:30");
        SLOT_TIME.put(13, "15:00");
        SLOT_TIME.put(14, "15:30");
        SLOT_TIME.put(15, "16:00");
    }

    @Resource
    private NoModelAgentEngine noModelAgentEngine;

    @Resource
    private DashScopeAgentService dashScopeAgentService;

    @Resource
    private AgentConversationMemoryService memoryService;

    @Resource
    private UserAgentTools userAgentTools;

    @Resource
    private MedicalDeptAgentTools medicalDeptAgentTools;

    @Resource
    private DoctorAgentTools doctorAgentTools;

    @Resource
    private RegistrationAgentTools registrationAgentTools;

    @Resource
    private MessageAgentTools messageAgentTools;

    public AgentChatResponse chat(AgentChatRequest request, Integer userId) {
        AgentChatRequest safeRequest = request == null ? new AgentChatRequest() : request;
        String sessionId = StringUtils.hasText(safeRequest.getSessionId()) ? safeRequest.getSessionId() : IdUtil.simpleUUID();
        Map<String, Object> memory = memoryService.load(sessionId);
        AgentChatResponse response = new AgentChatResponse();
        response.setSessionId(sessionId);
        response.setSystemPromptVersion(AgentPromptCatalog.SYSTEM_PROMPT_VERSION);

        if (userId != null) {
            memory.put("userId", userId);
            memory.put("hasUserCard", userAgentTools.hasUserCard(userId));
        } else {
            memory.remove("userId");
            memory.remove("hasUserCard");
        }

        String modelReply = null;
        try {
            String action = noModelAgentEngine.resolveAction(safeRequest, memory);
            AgentModelDecision modelDecision = dashScopeAgentService.decide(safeRequest, memory);
            if (modelDecision != null) {
                appendToolLog(response, "dashscope", "success", "已完成意图识别");
                modelReply = modelDecision.getReply();
                String modelAction = normalizeAction(modelDecision.getAction());
                if (StringUtils.hasText(modelAction) && !"none".equals(modelAction)) {
                    action = modelAction;
                    memory.put("modelAction", modelAction);
                    memory.put("modelReason", stringValue(modelDecision.getReason(), null));
                }
            }
            Map<String, Object> payload = safePayload(safeRequest.getPayload());
            if (modelDecision != null) {
                mergeModelPayload(memory, payload, safePayload(modelDecision.getPayload()));
                action = upgradeActionByPayload(action, payload);
            }
            enrichPayloadByMessage(memory, payload, safeRequest.getMessage());
            autoFillPayloadFromMemory(memory, payload);
            action = resolveActionByStage(action, memory, payload);
            boolean autoHandled = tryHandleAutoRegistration(response, memory, payload, safeRequest, userId, action);
            if (!autoHandled) {
                switch (action) {
                    case AgentUiAction.START_REGISTRATION:
                    case AgentUiAction.VIEW_DEPARTMENTS:
                        handleViewDepartments(response, memory, userId);
                        break;
                    case AgentUiAction.SELECT_SUB_DEPT:
                        handleSelectSubDept(response, memory, payload);
                        break;
                    case AgentUiAction.SELECT_DATE:
                        handleSelectDate(response, memory, payload);
                        break;
                    case AgentUiAction.SELECT_DOCTOR:
                        handleSelectDoctor(response, memory, payload);
                        break;
                    case AgentUiAction.SELECT_SLOT:
                        handleSelectSlot(response, memory, payload, userId);
                        break;
                    case AgentUiAction.VIEW_MESSAGES:
                        handleViewMessages(response, memory, userId);
                        break;
                    case AgentUiAction.VIEW_USER_CARD:
                        handleViewUserCard(response, memory, userId);
                        break;
                    case AgentAction.CREATE_REGISTRATION:
                        handleCreateRegistration(response, memory, payload, userId);
                        break;
                    case AgentUiAction.WELCOME:
                    default:
                        handleWelcome(response, memory, userId);
                        break;
                }
            }
        } catch (Exception e) {
            log.error("Agent 编排执行失败", e);
            response.setReply("当前助手暂时忙碌，你可以稍后重试，或者先通过现有挂号页面完成操作。");
            response.setState("error");
            appendToolLog(response, "agent_orchestrator", "error", e.getMessage());
            appendNavigateCard(response, "去挂号页", "直接打开现有挂号流程", "/registration/medical_dept_list/medical_dept_list");
        }

        response.setMemory(exposeMemory(memory));
        response.setSteps(buildSteps(memory, userId));
        if (!StringUtils.hasText(response.getReply()) && StringUtils.hasText(modelReply)) {
            response.setReply(modelReply);
        }
        if (!StringUtils.hasText(response.getReply())) {
            response.setReply(noModelAgentEngine.fallbackReply());
        }
        if (!StringUtils.hasText(response.getState())) {
            response.setState(stringValue(memory.get("stage"), "idle"));
        }
        memoryService.save(sessionId, memory);
        return response;
    }

    private void handleWelcome(AgentChatResponse response, Map<String, Object> memory, Integer userId) {
        memory.put("stage", "idle");
        if (!StringUtils.hasText(response.getReply())) {
            response.setReply(userId == null
                    ? "你好，我是一期 AI 挂号助手骨架版。现在可以帮你查科室、看医生、查号源，并在确认后帮你挂号。"
                    : "你好，我可以帮你查科室、医生、号源，并在你确认后发起挂号。请选择要继续的事项。");
        }
        appendActionCard(response, "开始挂号", "按科室 → 日期 → 医生 → 时段逐步完成挂号", AgentUiAction.START_REGISTRATION, null, "挂号");
        appendActionCard(response, "查看科室", "浏览当前可挂号的科室与诊室", AgentUiAction.VIEW_DEPARTMENTS, null, "查询");
        appendActionCard(response, "我的就诊卡", "检查是否已完成实名登记与就诊卡创建", AgentUiAction.VIEW_USER_CARD, null, "实名");
        appendActionCard(response, "消息中心", "查看挂号提醒和系统消息", AgentUiAction.VIEW_MESSAGES, null, "消息");
    }

    private boolean tryHandleAutoRegistration(AgentChatResponse response,
                                              Map<String, Object> memory,
                                              Map<String, Object> payload,
                                              AgentChatRequest request,
                                              Integer userId,
                                              String action) {
        if (!shouldTryAutoRegistration(request, payload, action)) {
            return false;
        }
        if (userId == null) {
            requireLogin(response, "确认挂号前请先登录小程序。", "/pages/mine/mine");
            return true;
        }
        boolean hasUserCard = userAgentTools.hasUserCard(userId);
        memory.put("hasUserCard", hasUserCard);
        if (!hasUserCard) {
            response.setReply("挂号前需要先创建就诊卡。");
            appendNavigateCard(response, "去创建就诊卡", "打开实名登记页面", "/user/fill_user_info/fill_user_info");
            response.setState("need_user_card");
            return true;
        }
        String date = firstString(payload.get("date"), memory.get("date"));
        if (!StringUtils.hasText(date)) {
            memory.put("stage", "choose_date");
            memory.remove("pendingOrder");
            response.setReply("我可以直接帮你自动选最早可挂时段，请先告诉我要挂哪一天，例如今天、明天或具体日期。");
            return true;
        }
        String deptName = firstString(payload.get("deptName"), memory.get("deptName"));
        Integer deptId = firstInt(payload.get("deptId"), memory.get("deptId"));
        if (deptId == null && StringUtils.hasText(deptName)) {
            deptId = matchDeptIdByName(deptName);
            if (deptId != null) {
                payload.put("deptId", deptId);
                memory.put("deptId", deptId);
                memory.put("deptName", deptName);
            }
        }
        if (deptId == null) {
            return false;
        }
        AutoRegistrationCandidate candidate = findEarliestRegistrationCandidate(deptId, deptName, date, firstString(payload.get("doctorName"), memory.get("doctorName")), response);
        if (candidate == null) {
            memory.put("stage", "choose_sub_department");
            memory.put("selectionMode", "auto_earliest");
            String autoFailureReason = hasExpiredAvailableSlot(deptId, date, firstString(payload.get("doctorName"), memory.get("doctorName")))
                    ? "today_slots_expired"
                    : "no_candidate";
            memory.put("lastAutoFailureReason", autoFailureReason);
            memory.remove("pendingOrder");
            if (!StringUtils.hasText(response.getReply())) {
                response.setReply("today_slots_expired".equals(autoFailureReason)
                        ? "今天的剩余可挂时段已经结束了，你可以换个日期，或者我带你继续手动选择诊室和医生。"
                        : "暂时没有找到可直接为你挂上的号源，你可以换个日期，或者我带你继续手动选择诊室和医生。");
            }
            appendSubDepartmentCards(response, deptId, deptName);
            appendNavigateCard(response, "打开原挂号页", "进入现有挂号流程手动选择", "/registration/medical_dept_list/medical_dept_list");
            return true;
        }
        Map<String, Object> orderPayload = candidate.toPayload();
        String condition = registrationAgentTools.checkRegistrationCondition(userId, candidate.getDeptSubId(), candidate.getDate());
        appendToolLog(response, "checkRegistrationCondition", "success", condition);
        if (!"满足挂号条件".equals(condition)) {
            memory.put("stage", "choose_slot");
            memory.put("lastAutoFailureReason", "condition_failed");
            memory.remove("pendingOrder");
            response.setReply(condition);
            appendNavigateCard(response, "打开原挂号页", "进入现有挂号流程重新选择", "/registration/medical_dept_list/medical_dept_list");
            response.setState("check_failed");
            return true;
        }
        prepareRegistrationConfirmation(response, memory, orderPayload, "已为你自动选择最早可挂时段，请确认是否提交挂号。", "自动选择");
        memory.put("selectionMode", "auto_earliest");
        memory.put("autoSelectionReason", "earliest_available");
        memory.put("lastAutoFailureReason", null);
        memory.put("deptId", candidate.getDeptId());
        memory.put("deptName", candidate.getDeptName());
        memory.put("deptSubId", candidate.getDeptSubId());
        memory.put("deptSubName", candidate.getDeptSubName());
        memory.put("doctorId", candidate.getDoctorId());
        memory.put("doctorName", candidate.getDoctorName());
        memory.put("date", candidate.getDate());
        return true;
    }

    private boolean shouldTryAutoRegistration(AgentChatRequest request, Map<String, Object> payload, String action) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            return false;
        }
        if (!detectRegistrationIntent(request.getMessage(), action)) {
            return false;
        }
        return payload.get("deptId") != null || StringUtils.hasText(stringValue(payload.get("deptName"), null));
    }

    private boolean isRegistrationIntent(String message, String action) {
        if (AgentAction.CREATE_REGISTRATION.equals(action)) {
            return true;
        }
        if (AgentUiAction.START_REGISTRATION.equals(action) || AgentUiAction.SELECT_DATE.equals(action)
                || AgentUiAction.SELECT_DOCTOR.equals(action) || AgentUiAction.SELECT_SLOT.equals(action)) {
            return true;
        }
        if (!StringUtils.hasText(message)) {
            return false;
        }
        return message.contains("挂号") || message.contains("预约") || message.contains("挂") || message.contains("号");
    }

    private void enrichPayloadByMessage(Map<String, Object> memory, Map<String, Object> payload, String message) {
        String normalizedDate = parseDateHint(message);
        if (StringUtils.hasText(normalizedDate)) {
            payload.put("date", normalizedDate);
            memory.put("date", normalizedDate);
        }
        if (payload.get("deptId") == null && !StringUtils.hasText(stringValue(payload.get("deptName"), null))) {
            Map<String, Object> dept = inferDeptByMessage(message);
            if (!dept.isEmpty()) {
                payload.put("deptId", dept.get("deptId"));
                payload.put("deptName", dept.get("deptName"));
                memory.put("deptId", dept.get("deptId"));
                memory.put("deptName", dept.get("deptName"));
            }
        }
    }

    private boolean detectRegistrationIntent(String message, String action) {
        if (AgentAction.CREATE_REGISTRATION.equals(action)) {
            return true;
        }
        if (AgentUiAction.START_REGISTRATION.equals(action)
                || AgentUiAction.SELECT_DATE.equals(action)
                || AgentUiAction.SELECT_DOCTOR.equals(action)
                || AgentUiAction.SELECT_SLOT.equals(action)) {
            return true;
        }
        return containsAny(message, "挂号", "预约", "挂", "号", "科室", "诊室", "医生", "号源", "口腔", "牙");
    }

    private String parseDateHint(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = Pattern.compile("(今天|明天|后天|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}月\\d{1,2}日)").matcher(message);
        if (!matcher.find()) {
            return null;
        }
        String token = matcher.group(1);
        if ("今天".equals(token)) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
        if ("明天".equals(token)) {
            return LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        }
        if ("后天".equals(token)) {
            return LocalDate.now().plusDays(2).format(DATE_FORMATTER);
        }
        if (token.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return token;
        }
        String[] parts = token.replace("日", "").split("月");
        int month = Integer.parseInt(parts[0]);
        int day = Integer.parseInt(parts[1]);
        return LocalDate.of(LocalDate.now().getYear(), month, day).format(DATE_FORMATTER);
    }

    private Map<String, Object> inferDeptByMessage(String message) {
        return resolveDepartment(message);
    }

    private String normalizeDateHint(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = DATE_HINT_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        String token = matcher.group(1);
        if ("今天".equals(token)) {
            return LocalDate.now().format(DATE_FORMATTER);
        }
        if ("明天".equals(token)) {
            return LocalDate.now().plusDays(1).format(DATE_FORMATTER);
        }
        if ("后天".equals(token)) {
            return LocalDate.now().plusDays(2).format(DATE_FORMATTER);
        }
        if (token.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return token;
        }
        if (token.matches("\\d{1,2}月\\d{1,2}日")) {
            String[] parts = token.replace("日", "").split("月");
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            return LocalDate.of(LocalDate.now().getYear(), month, day).format(DATE_FORMATTER);
        }
        return null;
    }

    private AutoRegistrationCandidate findEarliestRegistrationCandidate(Integer deptId,
                                                                        String deptName,
                                                                        String date,
                                                                        String doctorNameHint,
                                                                        AgentChatResponse response) {
        ArrayList<HashMap> subDepartments = medicalDeptAgentTools.searchSubDepartments(deptId);
        appendToolLog(response, "searchSubDepartments", "success", "已为自动挂号查询诊室列表");
        AutoRegistrationCandidate bestCandidate = null;
        for (HashMap subDept : subDepartments) {
            Integer deptSubId = intValue(subDept.get("id"));
            if (deptSubId == null) {
                continue;
            }
            ArrayList<HashMap> dates = registrationAgentTools.searchRegisterDates(deptSubId, date, date);
            if (!hasAvailableDate(dates, date)) {
                continue;
            }
            ArrayList<HashMap> doctors = registrationAgentTools.searchDoctorPlansInDay(deptSubId, date);
            for (HashMap doctor : doctors) {
                if (!matchDoctorName(doctorNameHint, stringValue(doctor.get("name"), null))) {
                    continue;
                }
                Integer doctorId = intValue(doctor.get("id"));
                if (doctorId == null) {
                    continue;
                }
                Integer maximum = intValue(doctor.get("maximum"));
                Integer num = intValue(doctor.get("num"));
                if (maximum != null && num != null && maximum - num <= 0) {
                    continue;
                }
                ArrayList<HashMap> schedules = registrationAgentTools.searchScheduleSlots(doctorId, date);
                for (HashMap schedule : schedules) {
                    AutoRegistrationCandidate candidate = buildCandidate(deptId, deptName, subDept, doctor, schedule, date);
                    if (candidate == null) {
                        continue;
                    }
                    if (bestCandidate == null || candidate.isEarlierThan(bestCandidate)) {
                        bestCandidate = candidate;
                    }
                }
            }
        }
        if (bestCandidate != null) {
            appendToolLog(response, "autoSelectEarliestSlot", "success", "已自动锁定最早可挂号源");
        }
        return bestCandidate;
    }

    private boolean hasAvailableDate(ArrayList<HashMap> dates, String targetDate) {
        for (HashMap item : dates) {
            if (targetDate.equals(stringValue(item.get("date"), null)) && "出诊".equals(stringValue(item.get("status"), null))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchDoctorName(String doctorNameHint, String doctorName) {
        if (!StringUtils.hasText(doctorNameHint)) {
            return true;
        }
        if (!StringUtils.hasText(doctorName)) {
            return false;
        }
        return doctorNameHint.contains(doctorName) || doctorName.contains(doctorNameHint);
    }

    private AutoRegistrationCandidate buildCandidate(Integer deptId,
                                                     String deptName,
                                                     HashMap subDept,
                                                     HashMap doctor,
                                                     HashMap schedule,
                                                     String date) {
        Integer slot = intValue(schedule.get("slot"));
        Integer maximum = intValue(schedule.get("maximum"));
        Integer num = intValue(schedule.get("num"));
        Integer doctorId = intValue(doctor.get("id"));
        Integer deptSubId = intValue(subDept.get("id"));
        Integer workPlanId = intValue(schedule.get("workPlanId"));
        Integer scheduleId = intValue(schedule.get("scheduleId"));
        if (slot == null || doctorId == null || deptSubId == null || workPlanId == null || scheduleId == null) {
            return null;
        }
        if (isPastSlot(date, slot)) {
            return null;
        }
        int remain = maximum != null && num != null ? maximum - num : 0;
        if (remain <= 0) {
            return null;
        }
        AutoRegistrationCandidate candidate = new AutoRegistrationCandidate();
        candidate.setDeptId(deptId);
        candidate.setDeptName(deptName);
        candidate.setDeptSubId(deptSubId);
        candidate.setDeptSubName(stringValue(subDept.get("name"), "诊室"));
        candidate.setDoctorId(doctorId);
        candidate.setDoctorName(stringValue(doctor.get("name"), "医生"));
        candidate.setAmount(stringValue(doctor.get("price"), "0"));
        candidate.setDate(date);
        candidate.setSlot(slot);
        candidate.setWorkPlanId(workPlanId);
        candidate.setScheduleId(scheduleId);
        return candidate;
    }

    private void prepareRegistrationConfirmation(AgentChatResponse response,
                                                 Map<String, Object> memory,
                                                 Map<String, Object> orderPayload,
                                                 String reply,
                                                 String badge) {
        memory.put("pendingOrder", new HashMap<>(orderPayload));
        memory.put("stage", "awaiting_confirmation");
        response.setReply(reply);
        AgentConfirmation confirmation = new AgentConfirmation();
        confirmation.setAction(AgentAction.CREATE_REGISTRATION);
        confirmation.setLabel("确认挂号");
        Map<String, Object> confirmPayload = new HashMap<>(orderPayload);
        confirmPayload.put("confirmed", true);
        confirmation.setPayload(confirmPayload);
        response.setConfirmation(confirmation);
        response.setRequiresConfirmation(true);
        appendActionCard(
                response,
                stringValue(orderPayload.get("doctorName"), "医生") + " / " + slotLabel(intValue(orderPayload.get("slot"))),
                "科室：" + stringValue(orderPayload.get("deptSubName"), stringValue(orderPayload.get("deptName"), "--"))
                        + "，日期：" + stringValue(orderPayload.get("date"), "--")
                        + "，挂号费：" + stringValue(orderPayload.get("amount"), "--"),
                AgentAction.CREATE_REGISTRATION,
                confirmPayload,
                badge
        );
        response.setState("awaiting_confirmation");
    }

    private void handleViewDepartments(AgentChatResponse response, Map<String, Object> memory, Integer userId) {
        ArrayList<HashMap> departments = medicalDeptAgentTools.searchDepartments(null, true);
        appendToolLog(response, "searchDepartments", "success", "已查询门诊科室");
        memory.put("stage", "choose_department");
        response.setReply(userId == null
                ? "先为你展示可挂号科室。真正提交挂号前需要登录并完成就诊卡。"
                : "请选择要挂号的科室，我会继续帮你展开诊室和号源。"
        );
        for (HashMap department : departments) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("deptId", department.get("id"));
            payload.put("deptName", department.get("name"));
            appendActionCard(
                    response,
                    stringValue(department.get("name"), "科室"),
                    stringValue(department.get("description"), "进入后查看诊室与可挂号日期"),
                    AgentUiAction.SELECT_SUB_DEPT,
                    payload,
                    "科室"
            );
        }
        if (departments.isEmpty()) {
            response.setReply("暂未查询到可挂号科室，请稍后重试。");
        }
        appendNavigateCard(response, "打开原挂号页", "直接进入现有挂号流程", "/registration/medical_dept_list/medical_dept_list");
    }

    private void handleSelectSubDept(AgentChatResponse response, Map<String, Object> memory, Map<String, Object> payload) {
        Integer deptId = firstInt(payload.get("deptId"), memory.get("deptId"));
        if (deptId == null) {
            response.setReply("请先选择科室。");
            handleViewDepartments(response, memory, intValue(memory.get("userId")));
            return;
        }
        String deptName = firstString(payload.get("deptName"), memory.get("deptName"));
        ArrayList<HashMap> subDepartments = medicalDeptAgentTools.searchSubDepartments(deptId);
        appendToolLog(response, "searchSubDepartments", "success", "已查询诊室列表");
        memory.put("deptId", deptId);
        memory.put("deptName", deptName);
        memory.remove("deptSubId");
        memory.remove("deptSubName");
        memory.remove("date");
        memory.remove("doctorId");
        memory.remove("doctorName");
        memory.remove("pendingOrder");
        memory.put("stage", "choose_sub_department");
        response.setReply(StringUtils.hasText(deptName)
                ? "你选择了“" + deptName + "”，请选择具体诊室。"
                : "请选择具体诊室。"
        );
        for (HashMap subDept : subDepartments) {
            Map<String, Object> nextPayload = new HashMap<>();
            nextPayload.put("deptId", deptId);
            nextPayload.put("deptName", deptName);
            nextPayload.put("deptSubId", subDept.get("id"));
            nextPayload.put("deptSubName", subDept.get("name"));
            appendActionCard(
                    response,
                    stringValue(subDept.get("name"), "诊室"),
                    "查看未来 7 天可挂号日期",
                    AgentUiAction.SELECT_DATE,
                    nextPayload,
                    "诊室"
            );
        }
        if (subDepartments.isEmpty()) {
            response.setReply("该科室下暂未查询到诊室，请换一个科室试试。");
        }
    }

    private void appendSubDepartmentCards(AgentChatResponse response, Integer deptId, String deptName) {
        if (deptId == null) {
            return;
        }
        ArrayList<HashMap> subDepartments = medicalDeptAgentTools.searchSubDepartments(deptId);
        appendToolLog(response, "searchSubDepartments", "success", "已查询诊室列表");
        for (HashMap subDept : subDepartments) {
            Map<String, Object> nextPayload = new HashMap<>();
            nextPayload.put("deptId", deptId);
            nextPayload.put("deptName", deptName);
            nextPayload.put("deptSubId", subDept.get("id"));
            nextPayload.put("deptSubName", subDept.get("name"));
            appendActionCard(
                    response,
                    stringValue(subDept.get("name"), "诊室"),
                    "查看未来 7 天可挂号日期",
                    AgentUiAction.SELECT_DATE,
                    nextPayload,
                    "诊室"
            );
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSelectDate(AgentChatResponse response, Map<String, Object> memory, Map<String, Object> payload) {
        Integer deptSubId = firstInt(payload.get("deptSubId"), memory.get("deptSubId"));
        if (deptSubId == null) {
            response.setReply("请先选择诊室。");
            handleViewDepartments(response, memory, intValue(memory.get("userId")));
            return;
        }
        String deptSubName = firstString(payload.get("deptSubName"), memory.get("deptSubName"));
        String startDate = LocalDate.now().format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusDays(6).format(DATE_FORMATTER);
        ArrayList<HashMap> dates = registrationAgentTools.searchRegisterDates(deptSubId, startDate, endDate);
        appendToolLog(response, "searchRegisterDates", "success", "已查询近 7 天可挂号日期");
        memory.put("deptSubId", deptSubId);
        memory.put("deptSubName", deptSubName);
        memory.remove("date");
        memory.remove("doctorId");
        memory.remove("doctorName");
        memory.remove("pendingOrder");
        memory.put("stage", "choose_date");
        response.setReply(StringUtils.hasText(deptSubName)
                ? "下面是“" + deptSubName + "”近 7 天的号源情况，请选择日期。"
                : "请选择要挂号的日期。"
        );
        for (HashMap item : dates) {
            String status = stringValue(item.get("status"), "无号");
            Map<String, Object> nextPayload = new HashMap<>();
            nextPayload.put("deptSubId", deptSubId);
            nextPayload.put("deptSubName", deptSubName);
            nextPayload.put("date", item.get("date"));
            appendActionCard(
                    response,
                    stringValue(item.get("date"), "日期"),
                    "出诊状态：" + status,
                    "出诊".equals(status) ? AgentUiAction.SELECT_DOCTOR : "navigate",
                    "出诊".equals(status) ? nextPayload : new HashMap<String, Object>() {{
                        put("url", "/registration/medical_dept_list/medical_dept_list");
                    }},
                    status
            );
        }
        if (dates.isEmpty()) {
            response.setReply("近 7 天暂未查询到可挂号日期，请稍后重试。");
        }
    }

    private void handleSelectDoctor(AgentChatResponse response, Map<String, Object> memory, Map<String, Object> payload) {
        Integer deptSubId = firstInt(payload.get("deptSubId"), memory.get("deptSubId"));
        String date = firstString(payload.get("date"), memory.get("date"));
        String deptSubName = firstString(payload.get("deptSubName"), memory.get("deptSubName"));
        String doctorNameHint = firstString(payload.get("doctorName"), memory.get("doctorName"));
        if (deptSubId == null || !StringUtils.hasText(date)) {
            response.setReply("请先选择诊室和日期。");
            return;
        }
        ArrayList<HashMap> doctors = registrationAgentTools.searchDoctorPlansInDay(deptSubId, date);
        appendToolLog(response, "searchDoctorPlansInDay", "success", "已查询当日出诊医生");
        memory.put("deptSubId", deptSubId);
        memory.put("deptSubName", deptSubName);
        memory.put("date", date);
        memory.remove("doctorId");
        memory.remove("doctorName");
        memory.remove("pendingOrder");
        memory.put("stage", "choose_doctor");

        HashMap matchedDoctor = null;
        if (StringUtils.hasText(doctorNameHint)) {
            for (HashMap doctor : doctors) {
                String doctorName = stringValue(doctor.get("name"), null);
                if (!StringUtils.hasText(doctorName)) {
                    continue;
                }
                if (doctorNameHint.contains(doctorName) || doctorName.contains(doctorNameHint)) {
                    matchedDoctor = doctor;
                    break;
                }
            }
        }

        if (matchedDoctor != null) {
            memory.put("doctorId", matchedDoctor.get("id"));
            memory.put("doctorName", matchedDoctor.get("name"));
            payload.put("doctorId", matchedDoctor.get("id"));
            payload.put("doctorName", matchedDoctor.get("name"));
            payload.put("deptSubId", deptSubId);
            payload.put("deptSubName", deptSubName);
            payload.put("date", date);
            response.setReply("已为你定位到医生“" + stringValue(matchedDoctor.get("name"), "") + "”，继续为你查询可挂号时段。");
            handleSelectSlot(response, memory, payload, intValue(memory.get("userId")));
            return;
        }

        response.setReply(StringUtils.hasText(deptSubName)
                ? "以下是“" + deptSubName + "”在 " + date + " 的出诊医生。"
                : "以下是 " + date + " 的出诊医生。"
        );
        for (HashMap doctor : doctors) {
            Integer maximum = intValue(doctor.get("maximum"));
            Integer num = intValue(doctor.get("num"));
            int remain = maximum != null && num != null ? Math.max(maximum - num, 0) : 0;
            Map<String, Object> nextPayload = new HashMap<>();
            nextPayload.put("deptSubId", deptSubId);
            nextPayload.put("deptSubName", deptSubName);
            nextPayload.put("date", date);
            nextPayload.put("doctorId", doctor.get("id"));
            nextPayload.put("doctorName", doctor.get("name"));
            nextPayload.put("amount", doctor.get("price"));
            appendActionCard(
                    response,
                    stringValue(doctor.get("name"), "医生") + " " + stringValue(doctor.get("job"), ""),
                    "挂号费：" + stringValue(doctor.get("price"), "--") + "，剩余号源：" + remain,
                    remain > 0 ? AgentUiAction.SELECT_SLOT : "navigate",
                    remain > 0 ? nextPayload : new HashMap<String, Object>() {{
                        put("url", "/registration/medical_dept_list/medical_dept_list");
                    }},
                    remain > 0 ? "可挂号" : "无号"
            );
        }
        if (doctors.isEmpty()) {
            response.setReply("这一天暂无医生出诊，请重新选择日期。");
        }
    }

    private void handleSelectSlot(AgentChatResponse response, Map<String, Object> memory, Map<String, Object> payload, Integer userId) {
        Integer doctorId = firstInt(payload.get("doctorId"), memory.get("doctorId"));
        Integer deptSubId = firstInt(payload.get("deptSubId"), memory.get("deptSubId"));
        String date = firstString(payload.get("date"), memory.get("date"));
        if (doctorId == null || deptSubId == null || !StringUtils.hasText(date)) {
            response.setReply("请先选好诊室、日期和医生。");
            return;
        }
        HashMap doctor = doctorAgentTools.getDoctorDetail(doctorId, userId);
        if (doctor == null || doctor.isEmpty()) {
            response.setReply("没有查询到该医生信息，请重新选择医生。");
            return;
        }
        ArrayList<HashMap> schedules = registrationAgentTools.searchScheduleSlots(doctorId, date);
        appendToolLog(response, "searchScheduleSlots", "success", "已查询医生出诊时段");
        memory.put("doctorId", doctorId);
        memory.put("doctorName", doctor.get("name"));
        memory.put("date", date);
        memory.put("deptSubId", deptSubId);
        memory.remove("pendingOrder");
        memory.put("stage", "choose_slot");
        response.setReply("请继续选择号源时段。确认时我会再次检查挂号条件。" + (userId == null ? "提交挂号前需要先登录。" : ""));
        for (HashMap schedule : schedules) {
            Integer slot = intValue(schedule.get("slot"));
            Integer maximum = intValue(schedule.get("maximum"));
            Integer num = intValue(schedule.get("num"));
            if (slot == null || isPastSlot(date, slot)) {
                continue;
            }
            int remain = maximum != null && num != null ? Math.max(maximum - num, 0) : 0;
            Map<String, Object> nextPayload = new HashMap<>();
            nextPayload.put("workPlanId", schedule.get("workPlanId"));
            nextPayload.put("scheduleId", schedule.get("scheduleId"));
            nextPayload.put("slot", slot);
            nextPayload.put("date", date);
            nextPayload.put("doctorId", doctorId);
            nextPayload.put("doctorName", doctor.get("name"));
            nextPayload.put("deptSubId", deptSubId);
            nextPayload.put("deptSubName", memory.get("deptSubName"));
            nextPayload.put("amount", doctor.get("price"));
            appendActionCard(
                    response,
                    slotLabel(slot),
                    "医生：" + stringValue(doctor.get("name"), "--") + "，剩余号源：" + remain,
                    remain > 0 ? AgentAction.CREATE_REGISTRATION : "",
                    nextPayload,
                    remain > 0 ? "待确认" : "已满"
            );
        }
        if (response.getCards().isEmpty()) {
            response.setReply("这个医生当天没有可用时段，请返回上一步选择其他医生或日期。");
        }
    }

    private void handleViewMessages(AgentChatResponse response, Map<String, Object> memory, Integer userId) {
        if (userId == null) {
            requireLogin(response, "查看消息前请先登录小程序。", "/pages/mine/mine");
            return;
        }
        long unreadCount = messageAgentTools.getUnreadCount(userId);
        PageUtils pageUtils = messageAgentTools.listMessages(userId, 1, 5);
        appendToolLog(response, "listMessages", "success", "已查询消息列表");
        memory.put("stage", "message");
        response.setReply(unreadCount > 0
                ? "你当前有 " + unreadCount + " 条未读消息，下面展示最近消息。"
                : "当前没有未读消息，下面展示最近消息。"
        );
        List list = pageUtils.getList();
        for (Object item : list) {
            if (!(item instanceof HashMap)) {
                continue;
            }
            HashMap map = (HashMap) item;
            appendActionCard(
                    response,
                    stringValue(map.get("title"), "系统消息"),
                    stringValue(map.get("content"), "查看详情请进入消息中心"),
                    "navigate",
                    new HashMap<String, Object>() {{
                        put("url", "/pages/message_list/message_list");
                    }},
                    Boolean.TRUE.equals(map.get("isRead")) ? "已读" : "未读"
            );
        }
        appendNavigateCard(response, "打开消息中心", "进入现有消息页面查看全部消息", "/pages/message_list/message_list");
    }

    private void handleViewUserCard(AgentChatResponse response, Map<String, Object> memory, Integer userId) {
        if (userId == null) {
            requireLogin(response, "查看就诊卡前请先登录小程序。", "/pages/mine/mine");
            return;
        }
        boolean hasUserCard = userAgentTools.hasUserCard(userId);
        memory.put("hasUserCard", hasUserCard);
        appendToolLog(response, "getUserCardStatus", "success", hasUserCard ? "已存在就诊卡" : "未创建就诊卡");
        if (!hasUserCard) {
            response.setReply("你还没有创建就诊卡。挂号前需要先完成实名登记。"
            );
            appendNavigateCard(response, "去创建就诊卡", "打开实名登记页面", "/user/fill_user_info/fill_user_info");
            response.setState("need_user_card");
            return;
        }
        HashMap card = userAgentTools.getUserCardDetail(userId);
        if (card == null || card.isEmpty()) {
            response.setReply("系统检测到就诊卡状态异常，请先进入实名登记页检查信息。");
            appendNavigateCard(response, "去实名登记", "打开实名登记页面", "/user/fill_user_info/fill_user_info");
            response.setState("need_user_card");
            return;
        }
        response.setReply("已查询到你的就诊卡信息。"
        );
        appendActionCard(
                response,
                stringValue(card.get("name"), "就诊人") + " / " + stringValue(card.get("tel"), "未绑定电话"),
                "证件号：" + stringValue(card.get("pid"), "--") + "，医保类型：" + stringValue(card.get("insuranceType"), "--"),
                "navigate",
                new HashMap<String, Object>() {{
                    put("url", "/user/user_info_card_detail");
                }},
                "已实名"
        );
        appendNavigateCard(response, "查看就诊卡详情", "进入现有就诊卡详情页", "/user/user_info_card_detail");
        response.setState("user_card_ready");
    }

    private void handleCreateRegistration(AgentChatResponse response, Map<String, Object> memory, Map<String, Object> payload, Integer userId) {
        if (userId == null) {
            requireLogin(response, "确认挂号前请先登录小程序。", "/pages/mine/mine");
            return;
        }
        boolean hasUserCard = userAgentTools.hasUserCard(userId);
        memory.put("hasUserCard", hasUserCard);
        if (!hasUserCard) {
            response.setReply("挂号前需要先创建就诊卡。"
            );
            appendNavigateCard(response, "去创建就诊卡", "打开实名登记页面", "/user/fill_user_info/fill_user_info");
            response.setState("need_user_card");
            return;
        }

        Map<String, Object> orderPayload = payload.isEmpty() ? mapValue(memory.get("pendingOrder")) : new HashMap<>(payload);
        if (orderPayload.isEmpty()) {
            response.setReply("请先选择医生和号源时段，再执行挂号。"
            );
            return;
        }

        Integer deptSubId = intValue(orderPayload.get("deptSubId"));
        String date = stringValue(orderPayload.get("date"), null);
        if (deptSubId == null || !StringUtils.hasText(date)) {
            response.setReply("挂号参数不完整，请重新选择日期和号源时段。"
            );
            return;
        }

        String condition = registrationAgentTools.checkRegistrationCondition(userId, deptSubId, date);
        appendToolLog(response, "checkRegistrationCondition", "success", condition);
        if (!"满足挂号条件".equals(condition)) {
            memory.put("stage", "choose_slot");
            memory.remove("pendingOrder");
            response.setReply(condition);
            appendNavigateCard(response, "重新选号源", "返回原挂号页面重新选择日期和时段", "/registration/medical_dept_list/medical_dept_list");
            response.setState("check_failed");
            return;
        }

        boolean confirmed = booleanValue(orderPayload.get("confirmed"));
        if (!confirmed) {
            prepareRegistrationConfirmation(response, memory, orderPayload,
                    Boolean.TRUE.equals(orderPayload.get("autoSelected"))
                            ? "已为你自动选择最早可挂时段，请确认是否提交挂号。"
                            : "挂号条件校验通过。请确认是否提交挂号。",
                    Boolean.TRUE.equals(orderPayload.get("autoSelected")) ? "自动选择" : "待确认");
            return;
        }

        HashMap result = registrationAgentTools.createRegistrationOrder(userId, orderPayload);
        appendToolLog(response, "createRegistrationOrder", "success", result == null ? "号源已满" : "挂号成功");
        if (result == null || !result.containsKey("outTradeNo")) {
            memory.put("stage", "choose_slot");
            memory.remove("pendingOrder");
            response.setReply("该时段号源已满，请重新选择其他时段。"
            );
            response.setState("slot_unavailable");
            return;
        }

        memory.remove("pendingOrder");
        memory.put("stage", "completed");
        memory.put("lastOrderDate", orderPayload.get("date"));
        memory.put("lastDoctorName", orderPayload.get("doctorName"));
        memory.put("lastSlot", orderPayload.get("slot"));
        response.setReply("挂号成功，系统已为你生成挂号记录，并发送了挂号成功消息。"
        );
        appendNavigateCard(response, "查看挂号记录", "进入“我的挂号”查看结果", "/pages/registration_list/registration_list");
        appendNavigateCard(response, "查看消息中心", "查看挂号成功通知", "/pages/message_list/message_list");
        response.setState("completed");
    }

    private void requireLogin(AgentChatResponse response, String reply, String url) {
        response.setReply(reply);
        response.setRequiresLogin(true);
        response.setState("need_login");
        appendNavigateCard(response, "去登录", "打开个人中心进行登录", url);
    }

    private Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new HashMap<>() : new HashMap<>(payload);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map) {
            return new HashMap<>((Map<String, Object>) value);
        }
        return new HashMap<>();
    }

    private void appendToolLog(AgentChatResponse response, String name, String status, String summary) {
        AgentToolLog log = new AgentToolLog();
        log.setName(name);
        log.setStatus(status);
        log.setSummary(summary);
        response.getToolLogs().add(log);
    }

    private void appendNavigateCard(AgentChatResponse response, String title, String description, String url) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("url", url);
        appendActionCard(response, title, description, "navigate", payload, "跳转");
    }

    private void appendActionCard(AgentChatResponse response, String title, String description, String action, Map<String, Object> payload, String badge) {
        AgentResponseCard card = new AgentResponseCard();
        card.setType("action");
        card.setTitle(title);
        card.setDescription(description);
        card.setAction(action);
        card.setPayload(payload == null ? new HashMap<>() : payload);
        card.setBadge(badge);
        response.getCards().add(card);
    }

    private List<AgentPlanStep> buildSteps(Map<String, Object> memory, Integer userId) {
        List<AgentPlanStep> steps = new ArrayList<>();
        steps.add(new AgentPlanStep("login", "登录状态", userId == null ? "pending" : "completed"));

        Object hasUserCard = memory.get("hasUserCard");
        String cardStatus = userId == null ? "pending" : Boolean.TRUE.equals(hasUserCard) ? "completed" : "pending";
        steps.add(new AgentPlanStep("userCard", "就诊卡", cardStatus));

        steps.add(new AgentPlanStep("dept", "选择诊室", memory.get("deptSubId") != null ? "completed" : "pending"));
        steps.add(new AgentPlanStep("date", "选择日期", memory.get("date") != null ? "completed" : "pending"));
        boolean doctorReady = memory.get("doctorId") != null;
        boolean slotReady = hasPendingOrder(memory) || memory.get("lastSlot") != null;
        steps.add(new AgentPlanStep("slot", "选择号源时段", doctorReady && slotReady ? "completed" : doctorReady ? "in_progress" : "pending"));

        String stage = stringValue(memory.get("stage"), "idle");
        String confirmStatus = "completed".equals(stage) ? "completed" : hasPendingOrder(memory) ? "in_progress" : "pending";
        steps.add(new AgentPlanStep("confirm", "确认挂号", confirmStatus));
        return steps;
    }

    private Map<String, Object> exposeMemory(Map<String, Object> memory) {
        Map<String, Object> result = new HashMap<>();
        result.put("stage", memory.get("stage"));
        result.put("deptName", memory.get("deptName"));
        result.put("deptSubName", memory.get("deptSubName"));
        result.put("date", memory.get("date"));
        result.put("doctorName", memory.get("doctorName"));
        result.put("hasPendingOrder", hasPendingOrder(memory));
        result.put("modelAction", memory.get("modelAction"));
        result.put("modelReason", memory.get("modelReason"));
        return result;
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            return null;
        }
        String value = action.trim();
        switch (value) {
            case AgentUiAction.WELCOME:
            case AgentUiAction.START_REGISTRATION:
            case AgentUiAction.VIEW_DEPARTMENTS:
            case AgentUiAction.SELECT_SUB_DEPT:
            case AgentUiAction.SELECT_DATE:
            case AgentUiAction.SELECT_DOCTOR:
            case AgentUiAction.SELECT_SLOT:
            case AgentUiAction.VIEW_MESSAGES:
            case AgentUiAction.VIEW_USER_CARD:
            case AgentAction.CREATE_REGISTRATION:
            case "none":
                return value;
            default:
                return null;
        }
    }

    private String upgradeActionByPayload(String action, Map<String, Object> payload) {
        if (payload.get("doctorId") != null && payload.get("date") != null && payload.get("deptSubId") != null) {
            return AgentUiAction.SELECT_SLOT;
        }
        if (payload.get("doctorName") != null && payload.get("date") != null && payload.get("deptSubId") != null) {
            return AgentUiAction.SELECT_DOCTOR;
        }
        if (payload.get("date") != null && payload.get("deptSubId") != null) {
            return AgentUiAction.SELECT_DOCTOR;
        }
        if (payload.get("deptSubId") != null) {
            return AgentUiAction.SELECT_DATE;
        }
        if (payload.get("deptId") != null) {
            return AgentUiAction.SELECT_SUB_DEPT;
        }
        return action;
    }

    private String resolveActionByStage(String action, Map<String, Object> memory, Map<String, Object> payload) {
        if (AgentAction.CREATE_REGISTRATION.equals(action)) {
            return AgentAction.CREATE_REGISTRATION;
        }
        if (booleanValue(payload.get("confirmed")) || (payload.get("workPlanId") != null && payload.get("scheduleId") != null && payload.get("slot") != null)) {
            return AgentAction.CREATE_REGISTRATION;
        }
        String stage = stringValue(memory.get("stage"), "idle");
        if ("awaiting_confirmation".equals(stage) && hasPendingOrder(memory)) {
            return AgentAction.CREATE_REGISTRATION;
        }
        if ("choose_sub_department".equals(stage) || "choose_date".equals(stage) || "choose_doctor".equals(stage) || "choose_slot".equals(stage) || "awaiting_confirmation".equals(stage)) {
            if (payload.get("doctorId") != null && payload.get("date") != null && payload.get("deptSubId") != null) {
                return AgentUiAction.SELECT_SLOT;
            }
            if (payload.get("date") != null && payload.get("deptSubId") != null) {
                return AgentUiAction.SELECT_DOCTOR;
            }
            if (payload.get("deptSubId") != null) {
                return AgentUiAction.SELECT_DATE;
            }
            if (payload.get("deptId") != null) {
                return AgentUiAction.SELECT_SUB_DEPT;
            }
            if ("choose_slot".equals(stage)) {
                return AgentUiAction.SELECT_SLOT;
            }
            if ("choose_doctor".equals(stage)) {
                return AgentUiAction.SELECT_DOCTOR;
            }
            if ("choose_date".equals(stage)) {
                return AgentUiAction.SELECT_DATE;
            }
            if ("choose_sub_department".equals(stage)) {
                return AgentUiAction.SELECT_SUB_DEPT;
            }
        }
        return action;
    }

    private void mergeModelPayload(Map<String, Object> memory, Map<String, Object> payload, Map<String, Object> modelPayload) {
        if (modelPayload.isEmpty()) {
            return;
        }
        putIfPresent(payload, "deptName", modelPayload.get("deptName"));
        putIfPresent(payload, "deptSubName", modelPayload.get("deptSubName"));
        putIfPresent(payload, "date", modelPayload.get("date"));
        putIfPresent(payload, "doctorName", modelPayload.get("doctorName"));

        Integer deptId = matchDeptIdByName(stringValue(payload.get("deptName"), null));
        if (deptId != null) {
            payload.put("deptId", deptId);
            memory.put("deptId", deptId);
            memory.put("deptName", payload.get("deptName"));
        }

        Integer deptSubId = matchDeptSubIdByName(deptId, stringValue(payload.get("deptSubName"), null));
        if (deptSubId != null) {
            payload.put("deptSubId", deptSubId);
            memory.put("deptSubId", deptSubId);
            memory.put("deptSubName", payload.get("deptSubName"));
        }

        Integer doctorId = matchDoctorIdByName(deptSubId, stringValue(payload.get("date"), null), stringValue(payload.get("doctorName"), null));
        if (doctorId != null) {
            payload.put("doctorId", doctorId);
            memory.put("doctorId", doctorId);
            memory.put("doctorName", payload.get("doctorName"));
        }

        if (payload.get("date") != null) {
            memory.put("date", payload.get("date"));
        }
    }

    private void autoFillPayloadFromMemory(Map<String, Object> memory, Map<String, Object> payload) {
        if (payload.get("deptId") == null && memory.get("deptId") != null) {
            payload.put("deptId", memory.get("deptId"));
        }
        if (payload.get("deptName") == null && memory.get("deptName") != null) {
            payload.put("deptName", memory.get("deptName"));
        }
        if (payload.get("deptSubId") == null && memory.get("deptSubId") != null) {
            payload.put("deptSubId", memory.get("deptSubId"));
        }
        if (payload.get("deptSubName") == null && memory.get("deptSubName") != null) {
            payload.put("deptSubName", memory.get("deptSubName"));
        }
        if (payload.get("date") == null && memory.get("date") != null) {
            payload.put("date", memory.get("date"));
        }
        if (payload.get("doctorId") == null && memory.get("doctorId") != null) {
            payload.put("doctorId", memory.get("doctorId"));
        }
        if (payload.get("doctorName") == null && memory.get("doctorName") != null) {
            payload.put("doctorName", memory.get("doctorName"));
        }
    }

    private Integer matchDeptIdByName(String deptName) {
        return intValue(resolveDepartment(deptName).get("deptId"));
    }

    private Map<String, Object> resolveDepartment(String hint) {
        Map<String, Object> result = new HashMap<>();
        if (!StringUtils.hasText(hint)) {
            return result;
        }
        ArrayList<HashMap> departments = medicalDeptAgentTools.searchDepartments(null, true);
        HashMap matchedDepartment = matchDepartmentByHint(departments, hint);
        if (matchedDepartment == null) {
            return result;
        }
        result.put("deptId", matchedDepartment.get("id"));
        result.put("deptName", matchedDepartment.get("name"));
        return result;
    }

    private HashMap matchDepartmentByHint(ArrayList<HashMap> departments, String hint) {
        if (!StringUtils.hasText(hint)) {
            return null;
        }
        for (HashMap department : departments) {
            String name = stringValue(department.get("name"), null);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (hint.contains(name) || name.contains(hint)) {
                return department;
            }
        }
        String canonicalDeptName = matchDeptCanonicalNameByKeyword(hint);
        if (!StringUtils.hasText(canonicalDeptName)) {
            return null;
        }
        for (HashMap department : departments) {
            String name = stringValue(department.get("name"), null);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (canonicalDeptName.contains(name) || name.contains(canonicalDeptName)) {
                return department;
            }
        }
        return null;
    }

    private String matchDeptCanonicalNameByKeyword(String hint) {
        if (!StringUtils.hasText(hint)) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : DEPT_KEYWORD_RULES.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (hint.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private Integer matchDeptSubIdByName(Integer deptId, String deptSubName) {
        if (deptId == null || !StringUtils.hasText(deptSubName)) {
            return null;
        }
        ArrayList<HashMap> subDepartments = medicalDeptAgentTools.searchSubDepartments(deptId);
        for (HashMap subDept : subDepartments) {
            String name = stringValue(subDept.get("name"), null);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (deptSubName.contains(name) || name.contains(deptSubName)) {
                return intValue(subDept.get("id"));
            }
        }
        return null;
    }

    private Integer matchDoctorIdByName(Integer deptSubId, String date, String doctorName) {
        if (deptSubId == null || !StringUtils.hasText(date) || !StringUtils.hasText(doctorName)) {
            return null;
        }
        ArrayList<HashMap> doctors = registrationAgentTools.searchDoctorPlansInDay(deptSubId, date);
        for (HashMap doctor : doctors) {
            String name = stringValue(doctor.get("name"), null);
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (doctorName.contains(name) || name.contains(doctorName)) {
                return intValue(doctor.get("id"));
            }
        }
        return null;
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value instanceof String) {
            if (StringUtils.hasText((String) value)) {
                payload.put(key, value);
            }
            return;
        }
        if (value != null) {
            payload.put(key, value);
        }
    }

    private static class AutoRegistrationCandidate {
        private Integer deptId;
        private String deptName;
        private Integer deptSubId;
        private String deptSubName;
        private Integer doctorId;
        private String doctorName;
        private String amount;
        private String date;
        private Integer slot;
        private Integer workPlanId;
        private Integer scheduleId;

        Map<String, Object> toPayload() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("workPlanId", workPlanId);
            payload.put("scheduleId", scheduleId);
            payload.put("date", date);
            payload.put("doctorId", doctorId);
            payload.put("doctorName", doctorName);
            payload.put("deptSubId", deptSubId);
            payload.put("deptSubName", deptSubName);
            payload.put("deptName", deptName);
            payload.put("amount", amount);
            payload.put("slot", slot);
            payload.put("autoSelected", true);
            payload.put("selectionReason", "earliest_available");
            return payload;
        }

        boolean isEarlierThan(AutoRegistrationCandidate other) {
            if (other == null) {
                return true;
            }
            String currentDate = date == null ? "" : date;
            String otherDate = other.date == null ? "" : other.date;
            int dateCompare = currentDate.compareTo(otherDate);
            if (dateCompare != 0) {
                return dateCompare < 0;
            }
            int slotCompare = Integer.compare(slot == null ? Integer.MAX_VALUE : slot, other.slot == null ? Integer.MAX_VALUE : other.slot);
            if (slotCompare != 0) {
                return slotCompare < 0;
            }
            int deptSubCompare = Integer.compare(deptSubId == null ? Integer.MAX_VALUE : deptSubId, other.deptSubId == null ? Integer.MAX_VALUE : other.deptSubId);
            if (deptSubCompare != 0) {
                return deptSubCompare < 0;
            }
            return Integer.compare(doctorId == null ? Integer.MAX_VALUE : doctorId, other.doctorId == null ? Integer.MAX_VALUE : other.doctorId) < 0;
        }

        public Integer getDeptId() {
            return deptId;
        }

        public void setDeptId(Integer deptId) {
            this.deptId = deptId;
        }

        public String getDeptName() {
            return deptName;
        }

        public void setDeptName(String deptName) {
            this.deptName = deptName;
        }

        public Integer getDeptSubId() {
            return deptSubId;
        }

        public void setDeptSubId(Integer deptSubId) {
            this.deptSubId = deptSubId;
        }

        public String getDeptSubName() {
            return deptSubName;
        }

        public void setDeptSubName(String deptSubName) {
            this.deptSubName = deptSubName;
        }

        public Integer getDoctorId() {
            return doctorId;
        }

        public void setDoctorId(Integer doctorId) {
            this.doctorId = doctorId;
        }

        public String getDoctorName() {
            return doctorName;
        }

        public void setDoctorName(String doctorName) {
            this.doctorName = doctorName;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Integer getSlot() {
            return slot;
        }

        public void setSlot(Integer slot) {
            this.slot = slot;
        }

        public Integer getWorkPlanId() {
            return workPlanId;
        }

        public void setWorkPlanId(Integer workPlanId) {
            this.workPlanId = workPlanId;
        }

        public Integer getScheduleId() {
            return scheduleId;
        }

        public void setScheduleId(Integer scheduleId) {
            this.scheduleId = scheduleId;
        }
    }

    private boolean hasPendingOrder(Map<String, Object> memory) {
        return memory.get("pendingOrder") instanceof Map && !((Map<?, ?>) memory.get("pendingOrder")).isEmpty();
    }

    private boolean hasExpiredAvailableSlot(Integer deptId, String date, String doctorNameHint) {
        if (deptId == null || !StringUtils.hasText(date)) {
            return false;
        }
        LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
        if (!targetDate.equals(LocalDate.now())) {
            return false;
        }
        ArrayList<HashMap> subDepartments = medicalDeptAgentTools.searchSubDepartments(deptId);
        for (HashMap subDept : subDepartments) {
            Integer deptSubId = intValue(subDept.get("id"));
            if (deptSubId == null) {
                continue;
            }
            ArrayList<HashMap> doctors = registrationAgentTools.searchDoctorPlansInDay(deptSubId, date);
            for (HashMap doctor : doctors) {
                if (!matchDoctorName(doctorNameHint, stringValue(doctor.get("name"), null))) {
                    continue;
                }
                Integer doctorId = intValue(doctor.get("id"));
                if (doctorId == null) {
                    continue;
                }
                ArrayList<HashMap> schedules = registrationAgentTools.searchScheduleSlots(doctorId, date);
                for (HashMap schedule : schedules) {
                    Integer slot = intValue(schedule.get("slot"));
                    Integer maximum = intValue(schedule.get("maximum"));
                    Integer num = intValue(schedule.get("num"));
                    int remain = maximum != null && num != null ? maximum - num : 0;
                    if (slot != null && remain > 0 && isPastSlot(date, slot)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPastSlot(String date, Integer slot) {
        String time = SLOT_TIME.get(slot);
        if (!StringUtils.hasText(date) || !StringUtils.hasText(time)) {
            return false;
        }
        LocalDate slotDate = LocalDate.parse(date, DATE_FORMATTER);
        if (!slotDate.equals(LocalDate.now())) {
            return false;
        }
        return LocalDateTime.of(slotDate, LocalTime.parse(time)).isBefore(LocalDateTime.now());
    }

    private String slotLabel(Integer slot) {
        if (slot == null) {
            return "时段";
        }
        return SLOT_TIME.getOrDefault(slot, "时段 " + slot);
    }

    private Integer firstInt(Object first, Object second) {
        Integer value = intValue(first);
        return value != null ? value : intValue(second);
    }

    private String firstString(Object first, Object second) {
        String value = stringValue(first, null);
        return StringUtils.hasText(value) ? value : stringValue(second, null);
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return Integer.parseInt((String) value);
        }
        return null;
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

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
