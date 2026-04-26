package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
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
    private static final int MAX_REACT_STEPS = 4;
    private static final Pattern DATE_HINT_PATTERN = Pattern.compile("(\\u4eca\\u5929|\\u660e\\u5929|\\u540e\\u5929|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}\\u6708\\d{1,2}\\u65e5)");
    private static final Pattern DOCTOR_HINT_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,4})(主任医师|医生|医师)");
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
        QueryState state = buildTrustedState(safeMap(context.getPayload()), safeMap(context.getMemory()), context.getUserMessage());
        Map<String, Object> observation = new HashMap<>();

        for (int step = 0; step < MAX_REACT_STEPS; step++) {
            ToolDecision decision = guardDecision(decideNextTool(state), state);
            TerminalOutcome outcome = runToolStep(decision, state, observation);
            if (outcome == TerminalOutcome.SUCCESS) {
                return buildSuccessResult(state, observation);
            }
            if (outcome == TerminalOutcome.NO_SLOT) {
                return buildNoSlotResult(state, observation);
            }
            if (outcome == TerminalOutcome.ASK_USER) {
                return buildAskUserResult(state, observation);
            }
            if (outcome == TerminalOutcome.TOOL_FAILURE) {
                return buildToolFailureResult(state, observation);
            }
        }

        if (state.getCandidate() != null) {
            return buildSuccessResult(state, observation);
        }
        if (state.getDeptSubId() == null || !StringUtils.hasText(state.getDate())) {
            return buildAskUserResult(state, observation);
        }
        return buildNoSlotResult(state, observation);
    }

    private QueryState buildTrustedState(Map<String, Object> payload, Map<String, Object> memory, String userMessage) {
        QueryState state = new QueryState();
        state.setDeptId(firstInt(payload.get("deptId"), memory.get("deptId")));
        state.setDeptSubId(firstInt(payload.get("deptSubId"), memory.get("deptSubId")));
        state.setDoctorId(firstInt(payload.get("doctorId"), memory.get("doctorId")));
        state.setDeptName(firstText(payload.get("deptName"), memory.get("deptName")));
        if (!StringUtils.hasText(state.getDeptName())) {
            state.setDeptName(extractDeptName(userMessage));
        }
        state.setDeptSubName(firstText(payload.get("deptSubName"), memory.get("deptSubName")));
        state.setDoctorName(firstText(payload.get("doctorName"), memory.get("doctorName")));
        if (!StringUtils.hasText(state.getDoctorName())) {
            state.setDoctorName(extractDoctorName(userMessage));
        }
        state.setDate(firstText(payload.get("date"), memory.get("date")));
        if (!StringUtils.hasText(state.getDate())) {
            state.setDate(parseDateHint(userMessage));
        }
        return state;
    }

    private ToolDecision decideNextTool(QueryState state) {
        if (state.getCandidate() != null) {
            return ToolDecision.FINISH;
        }
        if (state.getDeptSubId() == null) {
            if (state.getDeptId() == null) {
                return StringUtils.hasText(state.getDeptName()) ? ToolDecision.MATCH_DEPARTMENT : ToolDecision.ASK_USER;
            }
            return ToolDecision.SEARCH_SUB_DEPARTMENTS;
        }
        if (!StringUtils.hasText(state.getDate())) {
            return ToolDecision.ASK_USER;
        }
        if (state.getDoctors() == null) {
            return ToolDecision.SEARCH_DOCTORS;
        }
        return ToolDecision.SEARCH_SLOTS;
    }

    private ToolDecision guardDecision(ToolDecision decision, QueryState state) {
        if (decision == ToolDecision.SEARCH_SLOTS && state.getDoctors() == null) {
            return ToolDecision.SEARCH_DOCTORS;
        }
        if ((decision == ToolDecision.SEARCH_DOCTORS || decision == ToolDecision.SEARCH_SLOTS) && !StringUtils.hasText(state.getDate())) {
            return ToolDecision.ASK_USER;
        }
        if ((decision == ToolDecision.SEARCH_DOCTORS || decision == ToolDecision.SEARCH_SLOTS) && state.getDeptSubId() == null) {
            if (state.getDeptId() != null) {
                return ToolDecision.SEARCH_SUB_DEPARTMENTS;
            }
            if (StringUtils.hasText(state.getDeptName())) {
                return ToolDecision.MATCH_DEPARTMENT;
            }
            return ToolDecision.ASK_USER;
        }
        if (decision == ToolDecision.SEARCH_SUB_DEPARTMENTS && state.getDeptId() == null) {
            return StringUtils.hasText(state.getDeptName()) ? ToolDecision.MATCH_DEPARTMENT : ToolDecision.ASK_USER;
        }
        return decision;
    }

    private TerminalOutcome runToolStep(ToolDecision decision, QueryState state, Map<String, Object> observation) {
        switch (decision) {
            case MATCH_DEPARTMENT:
                return matchDepartment(state, observation);
            case SEARCH_SUB_DEPARTMENTS:
                return searchSubDepartments(state, observation);
            case SEARCH_DOCTORS:
                return searchDoctors(state, observation);
            case SEARCH_SLOTS:
                return searchSlots(state, observation);
            case FINISH:
                return TerminalOutcome.SUCCESS;
            case ASK_USER:
            default:
                return TerminalOutcome.ASK_USER;
        }
    }

    private TerminalOutcome matchDepartment(QueryState state, Map<String, Object> observation) {
        try {
            ArrayList<HashMap> departments = callWithRetry("searchDepartments", new ReadToolCall<ArrayList<HashMap>>() {
                @Override
                public ArrayList<HashMap> call() {
                    return medicalDeptAgentTools.searchDepartments(null, true);
                }
            });
            state.setDepartments(departments);
            observation.put("departments", departments);
            HashMap matchedDepartment = matchDepartmentByName(state.getDeptName(), departments);
            if (matchedDepartment == null) {
                return TerminalOutcome.ASK_USER;
            }
            state.setDeptId(intValue(matchedDepartment.get("id")));
            state.setDeptName(stringValue(matchedDepartment.get("name")));
            return TerminalOutcome.CONTINUE;
        } catch (Exception e) {
            markToolFailure(state, "searchDepartments");
            return TerminalOutcome.TOOL_FAILURE;
        }
    }

    private TerminalOutcome searchSubDepartments(QueryState state, Map<String, Object> observation) {
        try {
            ArrayList<HashMap> subDepartments = callWithRetry("searchSubDepartments", new ReadToolCall<ArrayList<HashMap>>() {
                @Override
                public ArrayList<HashMap> call() {
                    return medicalDeptAgentTools.searchSubDepartments(state.getDeptId());
                }
            });
            state.setSubDepartments(subDepartments);
            observation.put("subDepartments", subDepartments);
            if (subDepartments == null || subDepartments.isEmpty()) {
                return TerminalOutcome.ASK_USER;
            }
            HashMap selectedSub = matchSubDepartment(state.getDeptSubId(), state.getDeptSubName(), subDepartments);
            if (selectedSub == null) {
                selectedSub = subDepartments.get(0);
            }
            state.setDeptSubId(intValue(selectedSub.get("id")));
            state.setDeptSubName(stringValue(selectedSub.get("name")));
            if (!StringUtils.hasText(state.getDeptName())) {
                state.setDeptName(stringValue(selectedSub.get("deptName")));
            }
            return TerminalOutcome.CONTINUE;
        } catch (Exception e) {
            markToolFailure(state, "searchSubDepartments");
            return TerminalOutcome.TOOL_FAILURE;
        }
    }

    private TerminalOutcome searchDoctors(QueryState state, Map<String, Object> observation) {
        try {
            ArrayList<HashMap> doctors = callWithRetry("searchDoctorPlansInDay", new ReadToolCall<ArrayList<HashMap>>() {
                @Override
                public ArrayList<HashMap> call() {
                    return registrationAgentTools.searchDoctorPlansInDay(state.getDeptSubId(), state.getDate());
                }
            });
            state.setDoctors(doctors);
            observation.put("doctors", doctors);
            if (doctors == null || doctors.isEmpty()) {
                return TerminalOutcome.NO_SLOT;
            }
            if (state.getDoctorId() != null || StringUtils.hasText(state.getDoctorName())) {
                HashMap matchedDoctor = matchDoctor(state.getDoctorId(), state.getDoctorName(), doctors);
                if (matchedDoctor == null) {
                    return TerminalOutcome.NO_SLOT;
                }
                state.setDoctorId(intValue(matchedDoctor.get("id")));
                state.setDoctorName(stringValue(matchedDoctor.get("name")));
            }
            return TerminalOutcome.CONTINUE;
        } catch (Exception e) {
            markToolFailure(state, "searchDoctorPlansInDay");
            return TerminalOutcome.TOOL_FAILURE;
        }
    }

    private TerminalOutcome searchSlots(QueryState state, Map<String, Object> observation) {
        Candidate candidate = selectCandidate(state.getDoctors(), state.getDoctorId(), state.getDoctorName(), state.getDate(), state);
        if (state.hasToolFailure()) {
            return TerminalOutcome.TOOL_FAILURE;
        }
        if (candidate == null) {
            return TerminalOutcome.NO_SLOT;
        }
        state.setCandidate(candidate);
        state.setDoctorId(candidate.getDoctorId());
        state.setDoctorName(candidate.getDoctorName());
        observation.put("selectedOrder", buildOrderPayload(state));
        return TerminalOutcome.SUCCESS;
    }

    private Candidate selectCandidate(ArrayList<HashMap> doctors, Integer doctorId, String doctorName, String date, QueryState state) {
        if (doctors == null || doctors.isEmpty()) {
            return null;
        }
        if (doctorId != null || StringUtils.hasText(doctorName)) {
            HashMap matchedDoctor = matchDoctor(doctorId, doctorName, doctors);
            return matchedDoctor == null ? null : findAvailableByDoctor(matchedDoctor, date, state);
        }
        for (HashMap doctor : doctors) {
            Candidate candidate = findAvailableByDoctor(doctor, date, state);
            if (candidate != null) {
                return candidate;
            }
            if (state.hasToolFailure()) {
                return null;
            }
        }
        return null;
    }

    private HashMap matchDepartmentByName(String deptName, ArrayList<HashMap> departments) {
        if (!StringUtils.hasText(deptName) || departments == null || departments.isEmpty()) {
            return null;
        }
        for (HashMap department : departments) {
            String name = stringValue(department.get("name"));
            if (matchesName(name, deptName)) {
                return department;
            }
        }
        return null;
    }

    private HashMap matchSubDepartment(Integer deptSubId, String deptSubName, ArrayList<HashMap> subDepartments) {
        if (subDepartments == null || subDepartments.isEmpty()) {
            return null;
        }
        if (deptSubId != null) {
            for (HashMap subDepartment : subDepartments) {
                if (deptSubId.equals(intValue(subDepartment.get("id")))) {
                    return subDepartment;
                }
            }
        }
        if (!StringUtils.hasText(deptSubName)) {
            return null;
        }
        for (HashMap subDepartment : subDepartments) {
            String name = stringValue(subDepartment.get("name"));
            if (matchesName(name, deptSubName)) {
                return subDepartment;
            }
        }
        return null;
    }

    private HashMap matchDoctor(Integer doctorId, String doctorName, ArrayList<HashMap> doctors) {
        if (doctors == null || doctors.isEmpty()) {
            return null;
        }
        if (doctorId != null) {
            for (HashMap doctor : doctors) {
                if (doctorId.equals(intValue(doctor.get("id")))) {
                    return doctor;
                }
            }
        }
        if (!StringUtils.hasText(doctorName)) {
            return null;
        }
        for (HashMap doctor : doctors) {
            String name = stringValue(doctor.get("name"));
            if (matchesName(name, doctorName)) {
                return doctor;
            }
        }
        return null;
    }

    private AgentResult buildAskUserResult(QueryState state, Map<String, Object> observation) {
        AgentResult result = buildResult("schedule-agent", HandoffAction.ASK_USER, MultiAgentStage.SLOT_QUERY);
        result.setReply("请先告诉我诊室和日期，例如：明天口腔科。");
        result.setSummary("missing_slots_input");
        result.setObservation(observation);
        result.setMemoryPatch(buildQueryPatch(state, false));
        return result;
    }

    private AgentResult buildNoSlotResult(QueryState state, Map<String, Object> observation) {
        AgentResult result = buildResult("schedule-agent", HandoffAction.ASK_USER, MultiAgentStage.SLOT_QUERY);
        result.setReply("暂时没有找到可用号源，请换个日期或诊室再试。");
        result.setSummary("no_slot_available");
        result.setToolName("searchScheduleSlots");
        result.setObservation(observation);
        result.setMemoryPatch(buildQueryPatch(state, false));
        return result;
    }

    private AgentResult buildToolFailureResult(QueryState state, Map<String, Object> observation) {
        AgentResult result = buildResult("schedule-agent", HandoffAction.ASK_USER, MultiAgentStage.SLOT_QUERY);
        result.setReply("查询号源时出现波动，请稍后重试或改走普通挂号。");
        result.setSummary("no_slot_available");
        result.setToolName(state.getLastFailedTool());
        result.setObservation(observation);
        Map<String, Object> patch = buildQueryPatch(state, false);
        patch.put("errorCode", MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR);
        patch.put("retryable", true);
        patch.put("errorMessage", "查询号源时出现波动，请稍后重试或改走普通挂号。");
        patch.put("badCaseType", "schedule_tool_failed");
        result.setMemoryPatch(patch);
        return result;
    }

    private AgentResult buildSuccessResult(QueryState state, Map<String, Object> observation) {
        AgentResult result = buildResult("schedule-agent", HandoffAction.HANDOFF, MultiAgentStage.POLICY_CHECK);
        result.setReply("已为你选中可挂号源，开始校验挂号条件。");
        result.setSummary("slot_selected");
        result.setToolName("searchScheduleSlots");
        result.setObservation(observation);
        result.setMemoryPatch(buildQueryPatch(state, true));
        Map<String, Object> toolInput = new HashMap<>();
        toolInput.put("deptSubId", state.getDeptSubId());
        toolInput.put("doctorId", state.getDoctorId());
        toolInput.put("date", state.getDate());
        result.setToolInput(toolInput);
        return result;
    }

    private Map<String, Object> buildQueryPatch(QueryState state, boolean selected) {
        Map<String, Object> patch = new HashMap<>();
        patch.put("deptId", state.getDeptId());
        patch.put("deptName", state.getDeptName());
        patch.put("deptSubId", state.getDeptSubId());
        patch.put("deptSubName", state.getDeptSubName());
        patch.put("date", state.getDate());
        patch.put("doctorId", state.getDoctorId());
        patch.put("doctorName", state.getDoctorName());
        patch.put("requiresLogin", false);
        if (selected) {
            patch.put("pendingOrder", buildOrderPayload(state));
            patch.put("awaitingConfirmation", true);
            return patch;
        }
        patch.put("pendingOrder", null);
        patch.put("awaitingConfirmation", false);
        return patch;
    }

    private Map<String, Object> buildOrderPayload(QueryState state) {
        Map<String, Object> order = state.getCandidate().toOrderPayload();
        order.put("deptSubId", state.getDeptSubId());
        order.put("deptId", state.getDeptId());
        if (StringUtils.hasText(state.getDeptName())) {
            order.put("deptName", state.getDeptName());
        }
        if (StringUtils.hasText(state.getDeptSubName())) {
            order.put("deptSubName", state.getDeptSubName());
        }
        return order;
    }

    private Candidate findAvailableByDoctor(HashMap doctor, String date, QueryState state) {
        Integer doctorId = intValue(doctor.get("id"));
        if (doctorId == null) {
            return null;
        }
        ArrayList<HashMap> schedules;
        try {
            schedules = callWithRetry("searchScheduleSlots", new ReadToolCall<ArrayList<HashMap>>() {
                @Override
                public ArrayList<HashMap> call() {
                    return registrationAgentTools.searchScheduleSlots(doctorId, date);
                }
            });
        } catch (Exception e) {
            markToolFailure(state, "searchScheduleSlots");
            return null;
        }
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

    private String extractDeptName(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        try {
            ArrayList<HashMap> departments = callWithRetry("searchDepartments", new ReadToolCall<ArrayList<HashMap>>() {
                @Override
                public ArrayList<HashMap> call() {
                    return medicalDeptAgentTools.searchDepartments(null, true);
                }
            });
            for (HashMap department : departments) {
                String name = stringValue(department.get("name"));
                if (matchesName(name, message)) {
                    return name;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractDoctorName(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        Matcher matcher = DOCTOR_HINT_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean matchesName(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && (left.contains(right) || right.contains(left));
    }

    private <T> T callWithRetry(String toolName, ReadToolCall<T> call) {
        RuntimeException last = null;
        for (int i = 0; i < 2; i++) {
            try {
                return call.call();
            } catch (RuntimeException e) {
                last = e;
            }
        }
        throw last == null ? new RuntimeException(toolName + " failed") : last;
    }

    private void markToolFailure(QueryState state, String toolName) {
        state.setHasToolFailure(true);
        state.setLastFailedTool(toolName);
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

    private enum ToolDecision {
        MATCH_DEPARTMENT,
        SEARCH_SUB_DEPARTMENTS,
        SEARCH_DOCTORS,
        SEARCH_SLOTS,
        ASK_USER,
        FINISH
    }

    private enum TerminalOutcome {
        CONTINUE,
        SUCCESS,
        NO_SLOT,
        ASK_USER,
        TOOL_FAILURE
    }

    private interface ReadToolCall<T> {
        T call();
    }

    private static class QueryState {
        private Integer deptId;
        private String deptName;
        private Integer deptSubId;
        private String deptSubName;
        private Integer doctorId;
        private String doctorName;
        private String date;
        private ArrayList<HashMap> departments;
        private ArrayList<HashMap> subDepartments;
        private ArrayList<HashMap> doctors;
        private Candidate candidate;
        private boolean hasToolFailure;
        private String lastFailedTool;

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

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public ArrayList<HashMap> getDepartments() {
            return departments;
        }

        public void setDepartments(ArrayList<HashMap> departments) {
            this.departments = departments;
        }

        public ArrayList<HashMap> getSubDepartments() {
            return subDepartments;
        }

        public void setSubDepartments(ArrayList<HashMap> subDepartments) {
            this.subDepartments = subDepartments;
        }

        public ArrayList<HashMap> getDoctors() {
            return doctors;
        }

        public void setDoctors(ArrayList<HashMap> doctors) {
            this.doctors = doctors;
        }

        public Candidate getCandidate() {
            return candidate;
        }

        public void setCandidate(Candidate candidate) {
            this.candidate = candidate;
        }

        public boolean hasToolFailure() {
            return hasToolFailure;
        }

        public void setHasToolFailure(boolean hasToolFailure) {
            this.hasToolFailure = hasToolFailure;
        }

        public String getLastFailedTool() {
            return lastFailedTool;
        }

        public void setLastFailedTool(String lastFailedTool) {
            this.lastFailedTool = lastFailedTool;
        }
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
