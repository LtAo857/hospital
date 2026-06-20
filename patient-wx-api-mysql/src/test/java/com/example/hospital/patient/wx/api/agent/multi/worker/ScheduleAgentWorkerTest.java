package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.tool.MedicalDeptAgentTools;
import com.example.hospital.patient.wx.api.agent.tool.RegistrationAgentTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ScheduleAgentWorkerTest {

    @Test
    void shouldAskUserWhenDeptOrDateMissing() {
        MedicalDeptAgentTools medicalDeptAgentTools = Mockito.mock(MedicalDeptAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(medicalDeptAgentTools.searchDepartments(Mockito.isNull(), Mockito.eq(true)))
                .thenReturn(new ArrayList<HashMap>());

        ScheduleAgentWorker worker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>());
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("帮我挂号");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.SLOT_QUERY, result.getNextStage());
        Assertions.assertEquals("missing_slots_input", result.getSummary());
        Assertions.assertEquals("请告诉我想挂哪个科室、哪一天，我来帮你查询号源。", result.getReply());
    }

    @Test
    void shouldResolveDeptThenAskWhenDateMissing() {
        MedicalDeptAgentTools medicalDeptAgentTools = Mockito.mock(MedicalDeptAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        ArrayList<HashMap> departments = new ArrayList<>();
        departments.add(new HashMap() {{
            put("id", 1);
            put("name", "口腔科");
        }});
        ArrayList<HashMap> subDepartments = new ArrayList<>();
        subDepartments.add(new HashMap() {{
            put("id", 10);
            put("name", "口腔门诊");
            put("deptName", "口腔科");
        }});
        Mockito.when(medicalDeptAgentTools.searchDepartments(Mockito.isNull(), Mockito.eq(true))).thenReturn(departments);
        Mockito.when(medicalDeptAgentTools.searchSubDepartments(1)).thenReturn(subDepartments);

        ScheduleAgentWorker worker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("deptName", "口腔科");
        }});
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("口腔科挂号");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals("missing_slots_input", result.getSummary());
        Assertions.assertEquals(1, result.getMemoryPatch().get("deptId"));
        Assertions.assertEquals(10, result.getMemoryPatch().get("deptSubId"));
        Assertions.assertEquals("口腔门诊", result.getMemoryPatch().get("deptSubName"));
    }

    @Test
    void shouldNotSearchSlotDirectlyWhenDoctorProvidedButDateMissing() {
        MedicalDeptAgentTools medicalDeptAgentTools = Mockito.mock(MedicalDeptAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);

        ScheduleAgentWorker worker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("deptSubId", 10);
            put("doctorId", 9);
        }});
        context.setMemory(new HashMap<String, Object>());
        context.setUserMessage("挂张医生的号");

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals("missing_slots_input", result.getSummary());
        Mockito.verify(registrationAgentTools, Mockito.never()).searchScheduleSlots(Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(registrationAgentTools, Mockito.never()).searchDoctorPlansInDay(Mockito.anyInt(), Mockito.anyString());
    }

    @Test
    void shouldAskUserWhenNoSlotAvailable() {
        MedicalDeptAgentTools medicalDeptAgentTools = Mockito.mock(MedicalDeptAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(registrationAgentTools.searchDoctorPlansInDay(10, "2026-04-20"))
                .thenReturn(new ArrayList<HashMap>());

        ScheduleAgentWorker worker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("deptId", 1);
            put("deptSubId", 10);
            put("deptName", "口腔科");
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.SLOT_QUERY, result.getNextStage());
        Assertions.assertEquals("no_slot_available", result.getSummary());
        Assertions.assertEquals("searchScheduleSlots", result.getToolName());
        Assertions.assertEquals("2026-04-20 口腔科 暂无可挂号医生，建议换个日期或尝试其他相关科室。", result.getReply());
    }

    @Test
    void shouldReturnNoSlotWhenSpecifiedDoctorHasNoAvailableSlot() {
        MedicalDeptAgentTools medicalDeptAgentTools = Mockito.mock(MedicalDeptAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        ArrayList<HashMap> doctors = new ArrayList<>();
        doctors.add(new HashMap() {{
            put("id", 9);
            put("name", "张医生");
            put("price", "18.00");
        }});
        ArrayList<HashMap> schedules = new ArrayList<>();
        schedules.add(new HashMap() {{
            put("workPlanId", 101);
            put("scheduleId", 202);
            put("slot", 1);
            put("maximum", 20);
            put("num", 20);
        }});
        Mockito.when(registrationAgentTools.searchDoctorPlansInDay(10, "2026-04-20")).thenReturn(doctors);
        Mockito.when(registrationAgentTools.searchScheduleSlots(9, "2026-04-20")).thenReturn(schedules);

        ScheduleAgentWorker worker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("deptId", 1);
            put("deptSubId", 10);
            put("doctorId", 9);
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals("no_slot_available", result.getSummary());
    }

    @Test
    void shouldRetryAndDegradeWhenDoctorSearchFails() {
        MedicalDeptAgentTools medicalDeptAgentTools = Mockito.mock(MedicalDeptAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        Mockito.when(registrationAgentTools.searchDoctorPlansInDay(10, "2026-04-20"))
                .thenThrow(new RuntimeException("boom"));

        ScheduleAgentWorker worker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("deptId", 1);
            put("deptSubId", 10);
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals("schedule_tool_failed", result.getMemoryPatch().get("badCaseType"));
        Assertions.assertEquals(Boolean.FALSE, result.getMemoryPatch().get("awaitingConfirmation"));
        Mockito.verify(registrationAgentTools, Mockito.times(2)).searchDoctorPlansInDay(10, "2026-04-20");
    }

    @Test
    void shouldRetryAndDegradeWhenSlotSearchFails() {
        MedicalDeptAgentTools medicalDeptAgentTools = Mockito.mock(MedicalDeptAgentTools.class);
        RegistrationAgentTools registrationAgentTools = Mockito.mock(RegistrationAgentTools.class);
        ArrayList<HashMap> doctors = new ArrayList<>();
        doctors.add(new HashMap() {{
            put("id", 9);
            put("name", "张医生");
            put("price", "18.00");
        }});
        Mockito.when(registrationAgentTools.searchDoctorPlansInDay(10, "2026-04-20")).thenReturn(doctors);
        Mockito.when(registrationAgentTools.searchScheduleSlots(9, "2026-04-20"))
                .thenThrow(new RuntimeException("boom"));

        ScheduleAgentWorker worker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("deptId", 1);
            put("deptSubId", 10);
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, result.getMemoryPatch().get("errorCode"));
        Assertions.assertEquals("searchScheduleSlots", result.getToolName());
        Mockito.verify(registrationAgentTools, Mockito.times(2)).searchScheduleSlots(9, "2026-04-20");
    }
}
