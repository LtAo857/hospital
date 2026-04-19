package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.tool.MedicalDeptAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ScheduleAgentWorker implements AgentWorker {
    private static final Pattern DATE_HINT_PATTERN = Pattern.compile("(\\u4eca\\u5929|\\u660e\\u5929|\\u540e\\u5929|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}\\u6708\\d{1,2}\\u65e5)");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MedicalDeptAgentTools medicalDeptAgentTools;
    private final RegistrationAgentTools registrationAgentTools;

    public ScheduleAgentWorker(MedicalDeptAgentTools medicalDeptAgentTools,
                               RegistrationAgentTools registrationAgentTools) {
        this.medicalDeptAgentTools = medicalDeptAgentTools;
        this.registrationAgentTools = registrationAgentTools;
    }

    @Override
    public MultiAgentStage stage() {
        return MultiAgentStage.SLOT_QUERY;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        Map<String, Object> payload = safeMap(context.getPayload());
        Map<String, Object> memory = safeMap(context.getMemory());
        Map<String, Object> patch = new HashMap<>();
        Map<String, Object> observation = new HashMap<>();

        Integer deptId = firstInt(payload.get("deptId"), memory.get("deptId"));
        Integer deptSubId = firstInt(payload.get("deptSubId"), memory.get("deptSubId"));
        Integer doctorId = firstInt(payload.get("doctorId"), memory.get("doctorId"));
        String deptName = firstText(payload.get("deptName"), memory.get("deptName"));
        String deptSubName = firstText(payload.get("deptSubName"), memory.get("deptSubName"));
        String date = firstText(payload.get("date"), memory.get("date"));
        if (!StringUtils.hasText(date)) {
            date = parseDateHint(context.getUserMessage());
        }

        if (deptId == null && StringUtils.hasText(deptName)) {
            deptId = matchDeptIdByName(deptName);
        }
        if (deptSubId == null && deptId != null) {
            ArrayList<HashMap> subDepartments = medicalDeptAgentTools.searchSubDepartments(deptId);
            if (!subDepartments.isEmpty()) {
                HashMap selectedSub = subDepartments.get(0);
                deptSubId = intValue(selectedSub.get("id"));
                if (!StringUtils.hasText(deptSubName)) {
                    deptSubName = stringValue(selectedSub.get("name"));
                }
                if (!StringUtils.hasText(deptName)) {
                    deptName = stringValue(selectedSub.get("deptName"));
                }
                observation.put("subDepartments", subDepartments);
            }
        }

        if (deptSubId == null || !StringUtils.hasText(date)) {
            AgentResult askResult = buildResult("schedule-agent", HandoffAction.ASK_USER, MultiAgentStage.SLOT_QUERY);
            askResult.setReply("Please provide deptSubId and date (for example: tomorrow).");
            askResult.setSummary("missing_slots_input");
            askResult.setObservation(observation);
            askResult.setMemoryPatch(patch);
            return askResult;
        }

        Candidate candidate = findCandidate(deptSubId, doctorId, date);
        if (candidate == null) {
            AgentResult askResult = buildResult("schedule-agent", HandoffAction.ASK_USER, MultiAgentStage.SLOT_QUERY);
            askResult.setReply("No available slot found. Please change date or clinic.");
            askResult.setSummary("no_slot_available");
            askResult.setToolName("searchScheduleSlots");
            askResult.setObservation(observation);
            askResult.setMemoryPatch(patch);
            return askResult;
        }

        Map<String, Object> order = candidate.toOrderPayload();
        order.put("deptSubId", deptSubId);
        order.put("deptId", deptId);
        if (StringUtils.hasText(deptName)) {
            order.put("deptName", deptName);
        }
        if (StringUtils.hasText(deptSubName)) {
            order.put("deptSubName", deptSubName);
        }

        patch.put("deptId", deptId);
        patch.put("deptName", deptName);
        patch.put("deptSubId", deptSubId);
        patch.put("deptSubName", deptSubName);
        patch.put("date", date);
        patch.put("doctorId", candidate.getDoctorId());
        patch.put("doctorName", candidate.getDoctorName());
        patch.put("pendingOrder", order);
        patch.put("awaitingConfirmation", true);
        patch.put("requiresLogin", false);

        observation.put("selectedOrder", order);

        AgentResult handoffResult = buildResult("schedule-agent", HandoffAction.HANDOFF, MultiAgentStage.POLICY_CHECK);
        handoffResult.setReply("Slot selected. Move to policy check.");
        handoffResult.setSummary("slot_selected");
        handoffResult.setToolName("searchScheduleSlots");
        handoffResult.setObservation(observation);
        handoffResult.setMemoryPatch(patch);
        Map<String, Object> toolInput = new HashMap<>();
        toolInput.put("deptSubId", deptSubId);
        toolInput.put("doctorId", candidate.getDoctorId());
        toolInput.put("date", date);
        handoffResult.setToolInput(toolInput);
        return handoffResult;
    }

    private Candidate findCandidate(Integer deptSubId, Integer doctorId, String date) {
        ArrayList<HashMap> doctors = registrationAgentTools.searchDoctorPlansInDay(deptSubId, date);
        if (doctors == null || doctors.isEmpty()) {
            return null;
        }
        if (doctorId != null) {
            for (HashMap doctor : doctors) {
                if (doctorId.equals(intValue(doctor.get("id")))) {
                    return findAvailableByDoctor(doctor, date);
                }
            }
            return null;
        }
        for (HashMap doctor : doctors) {
            Candidate candidate = findAvailableByDoctor(doctor, date);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private Candidate findAvailableByDoctor(HashMap doctor, String date) {
        Integer doctorId = intValue(doctor.get("id"));
        if (doctorId == null) {
            return null;
        }
        ArrayList<HashMap> schedules = registrationAgentTools.searchScheduleSlots(doctorId, date);
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }
        for (HashMap schedule : schedules) {
            Integer remain = remainCount(schedule);
            if (remain != null && remain > 0) {
                Candidate candidate = new Candidate();
                candidate.setDoctorId(doctorId);
                candidate.setDoctorName(stringValue(doctor.get("name")));
                candidate.setAmount(stringValue(doctor.get("price")));
                candidate.setDate(date);
                candidate.setWorkPlanId(intValue(schedule.get("workPlanId")));
                candidate.setScheduleId(intValue(schedule.get("scheduleId")));
                candidate.setSlot(intValue(schedule.get("slot")));
                return candidate;
            }
        }
        return null;
    }

    private Integer remainCount(HashMap schedule) {
        Integer maximum = intValue(schedule.get("maximum"));
        Integer num = intValue(schedule.get("num"));
        if (maximum == null || num == null) {
            return null;
        }
        return maximum - num;
    }

    private Integer matchDeptIdByName(String deptName) {
        ArrayList<HashMap> departments = medicalDeptAgentTools.searchDepartments(null, true);
        for (HashMap department : departments) {
            String name = stringValue(department.get("name"));
            if (!StringUtils.hasText(name)) {
                continue;
            }
            if (name.contains(deptName) || deptName.contains(name)) {
                return intValue(department.get("id"));
            }
        }
        return null;
    }

    private AgentResult buildResult(String agent, HandoffAction action, MultiAgentStage nextStage) {
        AgentResult result = new AgentResult();
        result.setAgent(agent);
        result.setHandoffAction(action);
        result.setNextStage(nextStage);
        result.setConfidence(0.8d);
        return result;
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

    private Integer firstInt(Object first, Object second) {
        Integer firstValue = intValue(first);
        return firstValue != null ? firstValue : intValue(second);
    }

    private String firstText(Object first, Object second) {
        String value = stringValue(first);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return stringValue(second);
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

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> safeMap(Map<String, Object> map) {
        return map == null ? new HashMap<String, Object>() : map;
    }

    private static class Candidate {
        private Integer doctorId;
        private String doctorName;
        private String amount;
        private String date;
        private Integer slot;
        private Integer workPlanId;
        private Integer scheduleId;

        Map<String, Object> toOrderPayload() {
            Map<String, Object> order = new HashMap<>();
            order.put("doctorId", doctorId);
            order.put("doctorName", doctorName);
            order.put("amount", amount);
            order.put("date", date);
            order.put("slot", slot);
            order.put("workPlanId", workPlanId);
            order.put("scheduleId", scheduleId);
            return order;
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
}
