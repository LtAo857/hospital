package com.example.hospital.patient.wx.api.agent.service;

import cn.hutool.core.util.IdUtil;
import com.example.hospital.patient.wx.api.agent.config.AgentPromptCatalog;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatRequest;
import com.example.hospital.patient.wx.api.agent.dto.AgentChatResponse;
import com.example.hospital.patient.wx.api.agent.dto.AgentConfirmation;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AgentOrchestratorService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<Integer, String> SLOT_TIME = new LinkedHashMap<>();

    static {
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

        try {
            String action = noModelAgentEngine.resolveAction(safeRequest, memory);
            Map<String, Object> payload = safePayload(safeRequest.getPayload());
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
        } catch (Exception e) {
            log.error("Agent 编排执行失败", e);
            response.setReply("当前助手暂时忙碌，你可以稍后重试，或者先通过现有挂号页面完成操作。");
            response.setState("error");
            appendToolLog(response, "agent_orchestrator", "error", e.getMessage());
            appendNavigateCard(response, "去挂号页", "直接打开现有挂号流程", "/registration/medical_dept_list/medical_dept_list");
        }

        response.setMemory(exposeMemory(memory));
        response.setSteps(buildSteps(memory, userId));
        if (!StringUtils.hasText(response.getState())) {
            response.setState(stringValue(memory.get("stage"), "idle"));
        }
        memoryService.save(sessionId, memory);
        return response;
    }

    private void handleWelcome(AgentChatResponse response, Map<String, Object> memory, Integer userId) {
        memory.put("stage", "idle");
        response.setReply(userId == null
                ? "你好，我是一期 AI 挂号助手骨架版。现在可以帮你查科室、看医生、查号源，并在确认后帮你挂号。"
                : "你好，我可以帮你查科室、医生、号源，并在你确认后发起挂号。请选择要继续的事项。");
        appendActionCard(response, "开始挂号", "按科室 → 日期 → 医生 → 时段逐步完成挂号", AgentUiAction.START_REGISTRATION, null, "挂号");
        appendActionCard(response, "查看科室", "浏览当前可挂号的科室与诊室", AgentUiAction.VIEW_DEPARTMENTS, null, "查询");
        appendActionCard(response, "我的就诊卡", "检查是否已完成实名登记与就诊卡创建", AgentUiAction.VIEW_USER_CARD, null, "实名");
        appendActionCard(response, "消息中心", "查看挂号提醒和系统消息", AgentUiAction.VIEW_MESSAGES, null, "消息");
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
            memory.put("pendingOrder", new HashMap<>(orderPayload));
            memory.put("stage", "awaiting_confirmation");
            response.setReply("挂号条件校验通过。请确认是否提交挂号。");
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
                    "日期：" + stringValue(orderPayload.get("date"), "--") + "，挂号费：" + stringValue(orderPayload.get("amount"), "--"),
                    AgentAction.CREATE_REGISTRATION,
                    confirmPayload,
                    "待确认"
            );
            response.setState("awaiting_confirmation");
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
        return result;
    }

    private boolean hasPendingOrder(Map<String, Object> memory) {
        return memory.get("pendingOrder") instanceof Map && !((Map<?, ?>) memory.get("pendingOrder")).isEmpty();
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

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }
}
