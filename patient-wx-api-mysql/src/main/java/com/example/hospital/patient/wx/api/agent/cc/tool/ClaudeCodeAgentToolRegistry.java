package com.example.hospital.patient.wx.api.agent.cc.tool;

import com.example.hospital.patient.wx.api.agent.cc.dto.ClaudeCodeAgentToolResult;
import com.example.hospital.patient.wx.api.agent.dto.AgentResponseCard;
import com.example.hospital.patient.wx.api.agent.dto.AgentToolLog;
import com.example.hospital.patient.wx.api.agent.tool.DoctorAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.MedicalDeptAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.MessageAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.UserAgentTools;
import com.example.hospital.patient.wx.api.common.PageUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClaudeCodeAgentToolRegistry {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    private final Map<String, ToolDefinition> definitions = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        register("get_user_card_status", "Check whether the current user already has a patient card.", false);
        register("list_departments", "List outpatient departments and optionally match a department name.", false);
        register("list_sub_departments", "List clinic rooms under the selected department.", false);
        register("list_register_dates", "List bookable dates and optionally match the selected date.", false);
        register("list_doctors_in_day", "List doctors under the selected clinic room and date.", false);
        register("list_schedule_slots", "List schedule slots for the selected doctor and date.", false);
        register("check_registration_condition", "Check whether current selection passes business validation.", false);
        register("list_messages", "Load latest message center summary.", false);
        register("create_registration", "Create a registration order after user confirmation.", true);
    }

    public boolean isWriteTool(String toolName) {
        ToolDefinition definition = definitions.get(toolName);
        return definition != null && definition.writeOperation;
    }

    public boolean hasTool(String toolName) {
        return definitions.containsKey(toolName);
    }

    public AgentToolLog toToolLog(String name, String status, String summary) {
        AgentToolLog log = new AgentToolLog();
        log.setName(name);
        log.setStatus(status);
        log.setSummary(summary);
        return log;
    }

    public ClaudeCodeAgentToolResult execute(String toolName,
                                             Map<String, Object> toolInput,
                                             Integer userId,
                                             Map<String, Object> runtime) {
        switch (toolName) {
            case "get_user_card_status":
                return getUserCardStatus(userId);
            case "list_departments":
                return listDepartments(toolInput);
            case "list_sub_departments":
                return listSubDepartments(toolInput, runtime);
            case "list_register_dates":
                return listRegisterDates(toolInput, runtime);
            case "list_doctors_in_day":
                return listDoctorsInDay(toolInput, runtime);
            case "list_schedule_slots":
                return listScheduleSlots(toolInput, userId, runtime);
            case "check_registration_condition":
                return checkRegistrationCondition(toolInput, userId, runtime);
            case "list_messages":
                return listMessages(userId);
            case "create_registration":
                return createRegistration(toolInput, userId, runtime);
            default:
                ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
                result.setSuccess(false);
                result.setSummary("Unknown tool: " + toolName);
                result.setObservation(Collections.singletonMap("error", "unknown_tool"));
                return result;
        }
    }

    private ClaudeCodeAgentToolResult getUserCardStatus(Integer userId) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        if (userId == null) {
            result.setSummary("User is not logged in");
            result.setObservation(Collections.singletonMap("requiresLogin", true));
            result.getMemoryUpdates().put("hasUserCard", false);
            return result;
        }
        boolean hasUserCard = userAgentTools.hasUserCard(userId);
        result.getMemoryUpdates().put("hasUserCard", hasUserCard);
        if (!hasUserCard) {
            result.setSummary("User has not created a patient card");
            result.setObservation(Collections.singletonMap("hasUserCard", false));
            return result;
        }
        HashMap detail = userAgentTools.getUserCardDetail(userId);
        Map<String, Object> observation = new HashMap<>();
        observation.put("hasUserCard", true);
        observation.put("card", compactMap(detail, Arrays.asList("name", "tel", "pid", "insuranceType")));
        result.setSummary("User patient card is ready");
        result.setObservation(observation);
        return result;
    }

    private ClaudeCodeAgentToolResult listDepartments(Map<String, Object> toolInput) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        ArrayList<HashMap> list = medicalDeptAgentTools.searchDepartments(null, true);
        String deptName = firstString(value(toolInput, "deptName"));
        HashMap matched = matchByName(list, deptName, "name");
        if (matched != null) {
            result.getMemoryUpdates().put("deptId", matched.get("id"));
            result.getMemoryUpdates().put("deptName", matched.get("name"));
        }
        result.setSummary(matched != null ? "Matched department " + matched.get("name") : "Listed outpatient departments");
        Map<String, Object> observation = new HashMap<>();
        observation.put("departments", compactList(list, Arrays.asList("id", "name"), 10));
        observation.put("matchedDepartment", compactMap(matched, Arrays.asList("id", "name")));
        result.setObservation(observation);
        return result;
    }

    private ClaudeCodeAgentToolResult listSubDepartments(Map<String, Object> toolInput, Map<String, Object> runtime) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        Integer deptId = firstInt(value(toolInput, "deptId"), runtime.get("deptId"));
        String deptName = firstString(value(toolInput, "deptName"), runtime.get("deptName"));
        if (deptId == null && StringUtils.hasText(deptName)) {
            ArrayList<HashMap> departments = medicalDeptAgentTools.searchDepartments(null, true);
            HashMap matchedDept = matchByName(departments, deptName, "name");
            if (matchedDept != null) {
                deptId = intValue(matchedDept.get("id"));
                deptName = stringValue(matchedDept.get("name"), deptName);
            }
        }
        if (deptId == null) {
            result.setSuccess(false);
            result.setSummary("Department is required before querying clinic rooms");
            result.setObservation(Collections.singletonMap("error", "missing_department"));
            return result;
        }
        ArrayList<HashMap> list = medicalDeptAgentTools.searchSubDepartments(deptId);
        String deptSubName = firstString(value(toolInput, "deptSubName"), runtime.get("deptSubName"));
        HashMap matched = matchByName(list, deptSubName, "name");
        if (matched == null && list != null && list.size() == 1) {
            matched = list.get(0);
        }
        result.getMemoryUpdates().put("deptId", deptId);
        if (StringUtils.hasText(deptName)) {
            result.getMemoryUpdates().put("deptName", deptName);
        }
        if (matched != null) {
            result.getMemoryUpdates().put("deptSubId", matched.get("id"));
            result.getMemoryUpdates().put("deptSubName", matched.get("name"));
        }
        result.setSummary(matched != null ? "Matched clinic room " + matched.get("name") : "Listed clinic rooms");
        Map<String, Object> observation = new HashMap<>();
        observation.put("subDepartments", compactList(list, Arrays.asList("id", "name"), 10));
        observation.put("matchedSubDepartment", compactMap(matched, Arrays.asList("id", "name")));
        result.setObservation(observation);
        return result;
    }

    private ClaudeCodeAgentToolResult listRegisterDates(Map<String, Object> toolInput, Map<String, Object> runtime) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        Integer deptSubId = firstInt(value(toolInput, "deptSubId"), runtime.get("deptSubId"));
        String deptSubName = firstString(value(toolInput, "deptSubName"), runtime.get("deptSubName"));
        if (deptSubId == null) {
            result.setSuccess(false);
            result.setSummary("Clinic room is required before querying dates");
            result.setObservation(Collections.singletonMap("error", "missing_sub_department"));
            return result;
        }
        String startDate = LocalDate.now().format(DATE_FORMATTER);
        String endDate = LocalDate.now().plusDays(6).format(DATE_FORMATTER);
        ArrayList<HashMap> dates = registrationAgentTools.searchRegisterDates(deptSubId, startDate, endDate);
        String selectedDate = firstString(value(toolInput, "date"), runtime.get("date"));
        HashMap matchedDate = null;
        if (StringUtils.hasText(selectedDate)) {
            for (HashMap item : dates) {
                if (selectedDate.equals(stringValue(item.get("date"), null))) {
                    matchedDate = item;
                    break;
                }
            }
        }
        result.getMemoryUpdates().put("deptSubId", deptSubId);
        if (StringUtils.hasText(deptSubName)) {
            result.getMemoryUpdates().put("deptSubName", deptSubName);
        }
        if (StringUtils.hasText(selectedDate)) {
            result.getMemoryUpdates().put("date", selectedDate);
        }
        result.setSummary("Listed next 7 days of registration availability");
        Map<String, Object> observation = new HashMap<>();
        observation.put("dates", compactList(dates, Arrays.asList("date", "status"), 7));
        observation.put("matchedDate", compactMap(matchedDate, Arrays.asList("date", "status")));
        result.setObservation(observation);
        return result;
    }

    private ClaudeCodeAgentToolResult listDoctorsInDay(Map<String, Object> toolInput, Map<String, Object> runtime) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        Integer deptSubId = firstInt(value(toolInput, "deptSubId"), runtime.get("deptSubId"));
        String date = firstString(value(toolInput, "date"), runtime.get("date"));
        String doctorName = firstString(value(toolInput, "doctorName"), runtime.get("doctorName"));
        if (deptSubId == null || !StringUtils.hasText(date)) {
            result.setSuccess(false);
            result.setSummary("Clinic room and date are required before querying doctors");
            result.setObservation(Collections.singletonMap("error", "missing_doctor_query_condition"));
            return result;
        }
        ArrayList<HashMap> list = registrationAgentTools.searchDoctorPlansInDay(deptSubId, date);
        HashMap matched = matchByName(list, doctorName, "name");
        if (matched == null && list != null && list.size() == 1) {
            matched = list.get(0);
        }
        result.getMemoryUpdates().put("deptSubId", deptSubId);
        result.getMemoryUpdates().put("date", date);
        if (matched != null) {
            result.getMemoryUpdates().put("doctorId", matched.get("id"));
            result.getMemoryUpdates().put("doctorName", matched.get("name"));
        }
        result.setSummary(matched != null ? "Matched doctor " + matched.get("name") : "Listed doctors for the selected day");
        Map<String, Object> observation = new HashMap<>();
        observation.put("doctors", compactList(list, Arrays.asList("id", "name", "job", "price", "maximum", "num"), 10));
        observation.put("matchedDoctor", compactMap(matched, Arrays.asList("id", "name", "job", "price")));
        result.setObservation(observation);
        return result;
    }

    private ClaudeCodeAgentToolResult listScheduleSlots(Map<String, Object> toolInput, Integer userId, Map<String, Object> runtime) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        Integer doctorId = firstInt(value(toolInput, "doctorId"), runtime.get("doctorId"));
        String doctorName = firstString(value(toolInput, "doctorName"), runtime.get("doctorName"));
        String date = firstString(value(toolInput, "date"), runtime.get("date"));
        String slotLabel = firstString(value(toolInput, "slotLabel"), runtime.get("slotLabel"));
        Integer slot = firstInt(value(toolInput, "slot"), runtime.get("slot"));
        if (doctorId == null && StringUtils.hasText(doctorName)) {
            PageUtils pageUtils = doctorAgentTools.searchDoctors(1, 50);
            HashMap matchedDoctor = matchByName(castList(pageUtils == null ? null : pageUtils.getList()), doctorName, "name");
            if (matchedDoctor != null) {
                doctorId = intValue(matchedDoctor.get("id"));
                doctorName = stringValue(matchedDoctor.get("name"), doctorName);
            }
        }
        if (doctorId == null || !StringUtils.hasText(date)) {
            result.setSuccess(false);
            result.setSummary("Doctor and date are required before querying slots");
            result.setObservation(Collections.singletonMap("error", "missing_schedule_query_condition"));
            return result;
        }
        HashMap doctor = doctorAgentTools.getDoctorDetail(doctorId, userId);
        ArrayList<HashMap> schedules = registrationAgentTools.searchScheduleSlots(doctorId, date);
        List<Map<String, Object>> slots = new ArrayList<>();
        Map<String, Object> matchedSlot = null;
        for (HashMap item : schedules) {
            Map<String, Object> slotItem = new HashMap<>();
            slotItem.put("workPlanId", item.get("workPlanId"));
            slotItem.put("scheduleId", item.get("scheduleId"));
            slotItem.put("slot", item.get("slot"));
            slotItem.put("slotLabel", slotLabel(intValue(item.get("slot"))));
            slotItem.put("maximum", item.get("maximum"));
            slotItem.put("num", item.get("num"));
            slotItem.put("amount", doctor == null ? null : doctor.get("price"));
            slots.add(slotItem);
            if (matchesSlot(slotItem, slotLabel, slot)) {
                matchedSlot = slotItem;
            }
        }
        result.getMemoryUpdates().put("doctorId", doctorId);
        result.getMemoryUpdates().put("date", date);
        result.getMemoryUpdates().put("doctorName", StringUtils.hasText(doctorName) ? doctorName : doctor == null ? null : doctor.get("name"));
        if (matchedSlot != null) {
            copyIfPresent(matchedSlot, result.getMemoryUpdates(), "workPlanId", "scheduleId", "slot", "slotLabel", "amount");
        }
        result.setSummary("Listed doctor schedule slots");
        Map<String, Object> observation = new HashMap<>();
        observation.put("doctor", compactMap(doctor, Arrays.asList("id", "name", "job", "price", "avgScore", "totalCount")));
        observation.put("slots", slots);
        observation.put("matchedSlot", matchedSlot);
        result.setObservation(observation);
        return result;
    }

    private ClaudeCodeAgentToolResult checkRegistrationCondition(Map<String, Object> toolInput, Integer userId, Map<String, Object> runtime) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        Integer deptSubId = firstInt(value(toolInput, "deptSubId"), runtime.get("deptSubId"));
        String date = firstString(value(toolInput, "date"), runtime.get("date"));
        if (userId == null) {
            result.setSuccess(false);
            result.setSummary("User is not logged in");
            result.setObservation(Collections.singletonMap("requiresLogin", true));
            return result;
        }
        if (deptSubId == null || !StringUtils.hasText(date)) {
            result.setSuccess(false);
            result.setSummary("Clinic room and date are required before condition check");
            result.setObservation(Collections.singletonMap("error", "missing_check_condition"));
            return result;
        }
        String condition = registrationAgentTools.checkRegistrationCondition(userId, deptSubId, date);
        result.setSummary(condition);
        Map<String, Object> observation = new HashMap<>();
        observation.put("condition", condition);
        observation.put("conditionPassed", "满足挂号条件".equals(condition));
        result.setObservation(observation);
        return result;
    }

    private ClaudeCodeAgentToolResult listMessages(Integer userId) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        if (userId == null) {
            result.setSummary("User is not logged in");
            result.setObservation(Collections.singletonMap("requiresLogin", true));
            return result;
        }
        long unreadCount = messageAgentTools.getUnreadCount(userId);
        PageUtils pageUtils = messageAgentTools.listMessages(userId, 1, 5);
        result.getMemoryUpdates().put("unreadCount", unreadCount);
        result.setSummary("Loaded latest messages");
        Map<String, Object> observation = new HashMap<>();
        observation.put("unreadCount", unreadCount);
        observation.put("messages", compactList(castList(pageUtils == null ? null : pageUtils.getList()), Arrays.asList("id", "title", "content", "isRead"), 5));
        result.setObservation(observation);
        return result;
    }

    private ClaudeCodeAgentToolResult createRegistration(Map<String, Object> toolInput, Integer userId, Map<String, Object> runtime) {
        ClaudeCodeAgentToolResult result = new ClaudeCodeAgentToolResult();
        if (userId == null) {
            result.setSuccess(false);
            result.setSummary("User is not logged in");
            result.setObservation(Collections.singletonMap("requiresLogin", true));
            return result;
        }
        Map<String, Object> payload = new HashMap<>();
        copyIfPresent(toolInput, payload, "workPlanId", "scheduleId", "doctorId", "doctorName", "deptSubId", "deptSubName", "date", "slot", "amount");
        mergeFromMemory(payload, runtime, "workPlanId", "scheduleId", "doctorId", "doctorName", "deptSubId", "deptSubName", "date", "slot", "amount");
        HashMap response = registrationAgentTools.createRegistrationOrder(userId, payload);
        if (response == null || !response.containsKey("outTradeNo")) {
            result.setSummary("Slot is no longer available");
            result.setObservation(Collections.singletonMap("registrationCreated", false));
            result.setTerminal(true);
            result.setTerminalReply("该时段号源已满，请重新选择其他时段。");
            result.setTerminalState("slot_unavailable");
            return result;
        }
        result.setSummary("Registration created successfully");
        result.setObservation(Collections.singletonMap("registrationCreated", true));
        result.getMemoryUpdates().put("lastOrderDate", payload.get("date"));
        result.getMemoryUpdates().put("lastDoctorName", payload.get("doctorName"));
        result.getMemoryUpdates().put("lastSlot", payload.get("slot"));
        result.setTerminal(true);
        result.setTerminalReply("挂号成功，系统已经生成挂号记录并发送了成功通知。");
        result.setTerminalState("completed");
        result.getCards().add(navigateCard("查看挂号记录", "进入“我的挂号”查看结果", "/pages/registration_list/registration_list"));
        result.getCards().add(navigateCard("查看消息中心", "查看挂号成功通知", "/pages/message_list/message_list"));
        return result;
    }

    private AgentResponseCard navigateCard(String title, String description, String url) {
        AgentResponseCard card = new AgentResponseCard();
        card.setType("action");
        card.setTitle(title);
        card.setDescription(description);
        card.setAction("navigate");
        Map<String, Object> payload = new HashMap<>();
        payload.put("url", url);
        card.setPayload(payload);
        card.setBadge("跳转");
        return card;
    }

    private void register(String name, String description, boolean writeOperation) {
        definitions.put(name, new ToolDefinition(name, description, writeOperation));
    }

    private boolean matchesSlot(Map<String, Object> slotItem, String slotLabel, Integer slot) {
        if (slotItem == null) {
            return false;
        }
        if (slot != null && slot.equals(intValue(slotItem.get("slot")))) {
            return true;
        }
        return StringUtils.hasText(slotLabel) && slotLabel.equals(stringValue(slotItem.get("slotLabel"), null));
    }

    private HashMap matchByName(List<HashMap> list, String query, String field) {
        if (!StringUtils.hasText(query) || list == null) {
            return null;
        }
        for (HashMap item : list) {
            String value = stringValue(item.get(field), null);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (query.contains(value) || value.contains(query)) {
                return item;
            }
        }
        return null;
    }

    private List<Map<String, Object>> compactList(List<HashMap> list, List<String> fields, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        int max = Math.min(list.size(), limit);
        for (int i = 0; i < max; i++) {
            result.add(compactMap(list.get(i), fields));
        }
        return result;
    }

    private Map<String, Object> compactMap(HashMap source, List<String> fields) {
        if (source == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        for (String field : fields) {
            if (source.containsKey(field)) {
                result.put(field, source.get(field));
            }
        }
        return result;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String... keys) {
        if (source == null || target == null) {
            return;
        }
        for (String key : keys) {
            if (source.containsKey(key) && source.get(key) != null) {
                target.put(key, source.get(key));
            }
        }
    }

    private void mergeFromMemory(Map<String, Object> payload, Map<String, Object> runtime, String... keys) {
        for (String key : keys) {
            if (!payload.containsKey(key) && runtime.containsKey(key) && runtime.get(key) != null) {
                payload.put(key, runtime.get(key));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ArrayList<HashMap> castList(List list) {
        ArrayList<HashMap> result = new ArrayList<>();
        if (list == null) {
            return result;
        }
        for (Object item : list) {
            if (item instanceof HashMap) {
                result.add((HashMap) item);
            } else if (item instanceof Map) {
                result.add(new HashMap<>((Map) item));
            }
        }
        return result;
    }

    private Object value(Map<String, Object> map, String key) {
        return map == null ? null : map.get(key);
    }

    private String firstString(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private Integer firstInt(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            Integer integer = intValue(value);
            if (integer != null) {
                return integer;
            }
        }
        return null;
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

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String slotLabel(Integer slot) {
        String[] labels = {
                "", "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
                "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00"
        };
        return slot != null && slot > 0 && slot < labels.length ? labels[slot] : "--";
    }

    private static final class ToolDefinition {
        private final String name;
        private final String description;
        private final boolean writeOperation;

        private ToolDefinition(String name, String description, boolean writeOperation) {
            this.name = name;
            this.description = description;
            this.writeOperation = writeOperation;
        }
    }
}
