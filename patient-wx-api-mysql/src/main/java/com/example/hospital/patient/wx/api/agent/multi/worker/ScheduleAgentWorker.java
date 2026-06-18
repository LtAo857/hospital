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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

@Component
public class ScheduleAgentWorker implements AgentWorker {
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
        QueryState state = buildTrustedState(safeMap(context.getPayload()), safeMap(context.getMemory()));
        // Resolve relative date to actual yyyy-MM-dd
        if (StringUtils.hasText(state.getDate())) {
            String resolved = resolveDate(state.getDate());
            if (resolved != null) {
                state.setDate(resolved);
            }
        }
        Map<String, Object> observation = new HashMap<>();

        // 1. Match department by name → get deptId
        if (state.getDeptId() == null) {
            if (!StringUtils.hasText(state.getDeptName())) {
                return buildAskUserResult(state, observation, "请告诉我想挂哪个科室，例如：明天口腔科。");
            }
            TerminalOutcome outcome = matchDepartment(state, observation);
            if (outcome == TerminalOutcome.ASK_USER) {
                return buildAskUserResult(state, observation,
                        "没有找到" + state.getDeptName() + "这个科室，当前可选：口腔科、呼吸内科、消化内科、心内科、皮肤科、眼科、耳鼻喉科、儿科、骨科、妇科、神经内科。");
            }
            if (outcome == TerminalOutcome.TOOL_FAILURE) {
                return buildToolFailureResult(state, observation);
            }
        }

        // 2. Search sub-departments → get deptSubId
        if (state.getDeptSubId() == null) {
            TerminalOutcome outcome = searchSubDepartments(state, observation);
            if (outcome == TerminalOutcome.ASK_USER) {
                return buildAskUserResult(state, observation,
                        StringUtils.hasText(state.getDeptName())
                                ? state.getDeptName() + "下暂无可选诊室，请换一个科室试试。"
                                : "暂无可选诊室，请先指定科室。");
            }
            if (outcome == TerminalOutcome.TOOL_FAILURE) {
                return buildToolFailureResult(state, observation);
            }
        }

        // 3. Date is required to continue
        if (!StringUtils.hasText(state.getDate())) {
            return buildAskUserResult(state, observation, "请告诉我你想挂哪天的号，例如：明天、下周一、2026-06-20。");
        }

        // 4. Search doctor plans for the day
        if (state.getDoctors() == null) {
            TerminalOutcome outcome = searchDoctors(state, observation);
            if (outcome == TerminalOutcome.NO_SLOT) {
                return buildNoSlotResult(state, observation);
            }
            if (outcome == TerminalOutcome.ASK_USER) {
                return buildAskUserResult(state, observation, "未查询到该诊室当天的医生排班，请换个日期试试。");
            }
            if (outcome == TerminalOutcome.TOOL_FAILURE) {
                return buildToolFailureResult(state, observation);
            }
        }

        // 5. Search available schedule slots
        TerminalOutcome outcome = searchSlots(state, observation);
        if (outcome == TerminalOutcome.SUCCESS) {
            return buildSuccessResult(state, observation);
        }
        if (outcome == TerminalOutcome.NO_SLOT) {
            return buildNoSlotResult(state, observation);
        }
        if (outcome == TerminalOutcome.TOOL_FAILURE) {
            return buildToolFailureResult(state, observation);
        }
        return buildNoSlotResult(state, observation);
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern DIGIT_DATE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})$");

    private String resolveDate(String raw) {
        if (raw == null) return null;
        // Already in yyyy-MM-dd format
        if (DIGIT_DATE.matcher(raw).matches()) return raw;
        LocalDate today = LocalDate.now();
        // Relative days
        switch (raw) {
            case "今天": return today.format(DATE_FMT);
            case "明天": return today.plusDays(1).format(DATE_FMT);
            case "后天": return today.plusDays(2).format(DATE_FMT);
            case "大后天": return today.plusDays(3).format(DATE_FMT);
        }
        // "下周X" / "本周X"
        String weekPrefix = null;
        if (raw.startsWith("下周")) weekPrefix = "下周";
        else if (raw.startsWith("本周")) weekPrefix = "本周";
        if (weekPrefix != null) {
            String dayName = raw.substring(2);
            DayOfWeek target = dayOfWeek(dayName);
            if (target == null) return null;
            LocalDate base = "下周".equals(weekPrefix) ? today.plusWeeks(1) : today;
            return base.with(TemporalAdjusters.nextOrSame(target)).format(DATE_FMT);
        }
        // "周一"/"周二"/...
        DayOfWeek target = dayOfWeek(raw);
        if (target != null) {
            return today.with(TemporalAdjusters.nextOrSame(target)).format(DATE_FMT);
        }
        // "X月X日" / "X月X号"
        java.util.regex.Matcher m = Pattern.compile("(\\d{1,2})[月/-](\\d{1,2})[日号]?").matcher(raw);
        if (m.find()) {
            int month = Integer.parseInt(m.group(1));
            int day = Integer.parseInt(m.group(2));
            try {
                LocalDate d = LocalDate.of(today.getYear(), month, day);
                if (d.isBefore(today)) d = d.plusYears(1);
                return d.format(DATE_FMT);
            } catch (Exception e) { return null; }
        }
        return null;
    }

    private DayOfWeek dayOfWeek(String name) {
        if (name == null) return null;
        switch (name) {
            case "周一": case "星期一": return DayOfWeek.MONDAY;
            case "周二": case "星期二": return DayOfWeek.TUESDAY;
            case "周三": case "星期三": return DayOfWeek.WEDNESDAY;
            case "周四": case "星期四": return DayOfWeek.THURSDAY;
            case "周五": case "星期五": return DayOfWeek.FRIDAY;
            case "周六": case "星期六": return DayOfWeek.SATURDAY;
            case "周日": case "星期日": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }

    private QueryState buildTrustedState(Map<String, Object> payload, Map<String, Object> memory) {
        QueryState state = new QueryState();
        state.setDeptId(firstInt(payload.get("deptId"), memory.get("deptId")));
        state.setDeptSubId(firstInt(payload.get("deptSubId"), memory.get("deptSubId")));
        state.setDoctorId(firstInt(payload.get("doctorId"), memory.get("doctorId")));
        state.setDeptName(firstText(payload.get("deptName"), memory.get("deptName")));
        state.setDeptSubName(firstText(payload.get("deptSubName"), memory.get("deptSubName")));
        state.setDoctorName(firstText(payload.get("doctorName"), memory.get("doctorName")));
        state.setDate(firstText(payload.get("date"), memory.get("date")));
        state.setDoctorGender(firstText(payload.get("doctorGender"), memory.get("doctorGender")));
        state.setDoctorAgePreference(firstText(payload.get("doctorAgePreference"), memory.get("doctorAgePreference")));
        state.setPatientGender(firstText(payload.get("patientGender"), memory.get("patientGender")));
        return state;
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

    private static final Map<String, Integer> JOB_RANK = new java.util.LinkedHashMap<>();

    static {
        JOB_RANK.put("主任医师", 5);
        JOB_RANK.put("副主任医师", 4);
        JOB_RANK.put("主治医师", 3);
        JOB_RANK.put("住院医师", 2);
    }

    private TerminalOutcome searchSlots(QueryState state, Map<String, Object> observation) {
        // Collect ALL candidates with available slots
        List<Candidate> allCandidates = selectAllCandidates(state.getDoctors(), state.getDoctorId(), state.getDoctorName(), state.getDate(), state);
        if (state.hasToolFailure()) {
            return TerminalOutcome.TOOL_FAILURE;
        }
        if (allCandidates.isEmpty()) {
            return TerminalOutcome.NO_SLOT;
        }
        state.setAllCandidates(allCandidates);
        // Default pick: first (best-match) candidate
        Candidate best = allCandidates.get(0);
        state.setCandidate(best);
        state.setDoctorId(best.getDoctorId());
        state.setDoctorName(best.getDoctorName());
        observation.put("allCandidates", buildCandidatesObservations(allCandidates));
        observation.put("selectedOrder", buildOrderPayload(state));
        return TerminalOutcome.SUCCESS;
    }

    private List<Candidate> selectAllCandidates(ArrayList<HashMap> doctors, Integer doctorId, String doctorName, String date, QueryState state) {
        if (doctors == null || doctors.isEmpty()) {
            return Collections.emptyList();
        }
        // If user specified a doctor, find only that one
        if (doctorId != null || StringUtils.hasText(doctorName)) {
            HashMap matchedDoctor = matchDoctor(doctorId, doctorName, doctors);
            if (matchedDoctor == null) return Collections.emptyList();
            Candidate candidate = findAvailableByDoctor(matchedDoctor, date, state);
            return candidate != null ? Collections.singletonList(candidate) : Collections.emptyList();
        }
        // Collect all doctors with available slots
        List<Candidate> available = new ArrayList<>();
        for (HashMap doctor : doctors) {
            Candidate candidate = findAvailableByDoctor(doctor, date, state);
            if (candidate != null) {
                candidate.setJob(stringValue(doctor.get("job")));
                candidate.setSex(stringValue(doctor.get("sex")));
                available.add(candidate);
            }
            if (state.hasToolFailure()) return Collections.emptyList();
        }
        // Sort: gender preference → job rank desc → remaining slots desc
        sortCandidates(available, state.getDoctorGender(), state.getPatientGender());
        return available;
    }

    private void sortCandidates(List<Candidate> list, String doctorGenderPref, String patientGender) {
        if (list.size() <= 1) return;
        // Resolve target gender: explicit doctorGender > implied from patientGender
        String targetGender = StringUtils.hasText(doctorGenderPref) ? doctorGenderPref
                : ("女".equals(patientGender) ? "女" : null);
        if (!StringUtils.hasText(targetGender)) {
            // No gender preference: sort by job rank desc then slots desc
            list.sort((a, b) -> {
                int cmp = Integer.compare(jobRank(b.getJob()), jobRank(a.getJob()));
                if (cmp != 0) return cmp;
                return Integer.compare(b.getSlotsRemaining(), a.getSlotsRemaining());
            });
            return;
        }
        list.sort((a, b) -> {
            // Level 1: gender match — target first, unknown middle, non-target last
            int genderCmp = Integer.compare(genderScore(b.getSex(), targetGender), genderScore(a.getSex(), targetGender));
            if (genderCmp != 0) return genderCmp;
            // Level 2: job rank desc
            int jobCmp = Integer.compare(jobRank(b.getJob()), jobRank(a.getJob()));
            if (jobCmp != 0) return jobCmp;
            // Level 3: remaining slots desc
            return Integer.compare(b.getSlotsRemaining(), a.getSlotsRemaining());
        });
    }

    private int genderScore(String sex, String target) {
        if (target.equals(sex)) return 2;       // match
        if (sex == null || sex.isEmpty()) return 1; // unknown
        return 0;                               // non-match
    }

    private int jobRank(String job) {
        if (job == null) return 1;
        return JOB_RANK.getOrDefault(job, 1);
    }

    private List<Map<String, Object>> buildCandidatesObservations(List<Candidate> candidates) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Candidate c : candidates) {
            Map<String, Object> item = new HashMap<>(c.toOrderPayload());
            item.put("job", c.getJob());
            item.put("sex", c.getSex());
            result.add(item);
        }
        return result;
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
        return buildAskUserResult(state, observation, "请告诉我想挂哪个科室、哪天，例如：明天口腔科。");
    }

    private AgentResult buildAskUserResult(QueryState state, Map<String, Object> observation, String reply) {
        AgentResult result = buildResult("schedule-agent", HandoffAction.ASK_USER, MultiAgentStage.SLOT_QUERY);
        result.setReply(reply);
        result.setSummary("missing_slots_input");
        result.setObservation(observation);
        result.setMemoryPatch(buildQueryPatch(state, false));
        return result;
    }

    private AgentResult buildNoSlotResult(QueryState state, Map<String, Object> observation) {
        AgentResult result = buildResult("schedule-agent", HandoffAction.ASK_USER, MultiAgentStage.SLOT_QUERY);
        String dept = StringUtils.hasText(state.getDeptName()) ? state.getDeptName() : "该科室";
        String date = StringUtils.hasText(state.getDate()) ? state.getDate() : "指定日期";
        result.setReply(date + " " + dept + " 暂无可挂号医生，建议换个日期或尝试其他相关科室。");
        result.setSummary("no_slot_available");
        result.setToolName("searchScheduleSlots");
        result.setObservation(observation);
        result.setMemoryPatch(buildQueryPatch(state, false));
        return result;
    }

    private AgentResult buildToolFailureResult(QueryState state, Map<String, Object> observation) {
        AgentResult result = buildResult("schedule-agent", HandoffAction.ASK_USER, MultiAgentStage.SLOT_QUERY);
        result.setReply("号源查询暂时出现波动，请稍后重试或改走普通挂号。");
        result.setSummary("no_slot_available");
        result.setToolName(state.getLastFailedTool());
        result.setObservation(observation);
        Map<String, Object> patch = buildQueryPatch(state, false);
        patch.put("errorCode", MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR);
        patch.put("retryable", true);
        patch.put("errorMessage", "号源查询暂时出现波动，请稍后重试或改走普通挂号。");
        patch.put("badCaseType", "schedule_tool_failed");
        result.setMemoryPatch(patch);
        return result;
    }

    private AgentResult buildSuccessResult(QueryState state, Map<String, Object> observation) {
        AgentResult result = buildResult("schedule-agent", HandoffAction.HANDOFF, MultiAgentStage.POLICY_CHECK);
        result.setToolName("searchScheduleSlots");
        result.setObservation(observation);
        result.setMemoryPatch(buildQueryPatch(state, true));
        Map<String, Object> toolInput = new HashMap<>();
        toolInput.put("deptSubId", state.getDeptSubId());
        toolInput.put("doctorId", state.getDoctorId());
        toolInput.put("date", state.getDate());
        result.setToolInput(toolInput);

        // Build doctor list reply
        List<Candidate> candidates = state.getAllCandidates();
        String deptName = StringUtils.hasText(state.getDeptName()) ? state.getDeptName() : "该科室";
        String dateText = StringUtils.hasText(state.getDate()) ? state.getDate() : "该日";
        String targetGender = StringUtils.hasText(state.getDoctorGender()) ? state.getDoctorGender()
                : ("女".equals(state.getPatientGender()) ? "女" : null);
        boolean hasFemale = false;
        StringBuilder listBuilder = new StringBuilder();
        listBuilder.append("已为您找到 ").append(dateText).append(" ").append(deptName).append(" 的可挂号医生");
        if (StringUtils.hasText(targetGender)) {
            listBuilder.append("（女医生优先）");
        }
        listBuilder.append("：\n");
        Map<Integer, String> slotNames = new LinkedHashMap<>();
        slotNames.put(1, "上午");
        slotNames.put(2, "下午");
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            String sexLabel = "女".equals(c.getSex()) ? "女" : ("男".equals(c.getSex()) ? "男" : "--");
            if ("女".equals(c.getSex())) hasFemale = true;
            String jobText = StringUtils.hasText(c.getJob()) ? c.getJob() : "";
            String slotText = slotNames.getOrDefault(c.getSlot(), "时段" + c.getSlot());
            listBuilder.append("· ").append(StringUtils.hasText(c.getDoctorName()) ? c.getDoctorName() : "--");
            listBuilder.append("(").append(sexLabel).append(")");
            if (StringUtils.hasText(jobText)) listBuilder.append(" ").append(jobText);
            listBuilder.append(" ").append(slotText).append("有号");
            if (StringUtils.hasText(c.getAmount())) listBuilder.append(" ¥").append(c.getAmount());
            listBuilder.append("\n");
        }
        // Note if no female doctors when preference is female
        if (StringUtils.hasText(targetGender) && !hasFemale) {
            listBuilder.append(deptName).append("当前无女医生出诊，以上为全部可挂号医生。\n");
        }
        Candidate best = candidates.get(0);
        if (StringUtils.hasText(targetGender) && targetGender.equals(best.getSex())) {
            listBuilder.append("\n已按").append(targetGender).append("医生偏好优先展示，默认选中").append(best.getDoctorName()).append("。如需换其他医生请告诉我。");
        } else if (StringUtils.hasText(targetGender) && hasFemale) {
            listBuilder.append("\n已优先展示").append(targetGender).append("医生，默认选中").append(best.getDoctorName()).append("。如需换其他医生请告诉我。");
        } else {
            listBuilder.append("\n已默认选中").append(best.getDoctorName()).append("。如需换其他医生请告诉我。");
        }

        result.setReply(listBuilder.toString());
        result.setSummary("slot_selected");
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
                candidate.setSlotsRemaining(remain);
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
        private String doctorGender;
        private String doctorAgePreference;
        private ArrayList<HashMap> departments;
        private ArrayList<HashMap> subDepartments;
        private ArrayList<HashMap> doctors;
        private Candidate candidate;
        private List<Candidate> allCandidates;
        private String patientGender;
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

        public String getDoctorGender() {
            return doctorGender;
        }

        public void setDoctorGender(String doctorGender) {
            this.doctorGender = doctorGender;
        }

        public String getDoctorAgePreference() {
            return doctorAgePreference;
        }

        public void setDoctorAgePreference(String doctorAgePreference) {
            this.doctorAgePreference = doctorAgePreference;
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

        public List<Candidate> getAllCandidates() {
            return allCandidates;
        }

        public void setAllCandidates(List<Candidate> allCandidates) {
            this.allCandidates = allCandidates;
        }

        public String getPatientGender() {
            return patientGender;
        }

        public void setPatientGender(String patientGender) {
            this.patientGender = patientGender;
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
        private String job;
        private String sex;
        private int slotsRemaining;

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

        public String getJob() { return job; }
        public void setJob(String job) { this.job = job; }
        public String getSex() { return sex; }
        public void setSex(String sex) { this.sex = sex; }
        public int getSlotsRemaining() { return slotsRemaining; }
        public void setSlotsRemaining(int slotsRemaining) { this.slotsRemaining = slotsRemaining; }
    }
}
