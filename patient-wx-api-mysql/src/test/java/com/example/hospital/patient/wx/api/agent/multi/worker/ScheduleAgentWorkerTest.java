package com.example.hospital.patient.wx.api.agent.multi.worker;

import com.example.hospital.patient.wx.api.agent.multi.model.AgentContext;
import com.example.hospital.patient.wx.api.agent.multi.model.AgentResult;
import com.example.hospital.patient.wx.api.agent.multi.model.HandoffAction;
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
        Assertions.assertEquals("请先告诉我诊室和日期，例如：明天口腔科。", result.getReply());
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
        context.setPayload(new HashMap<String, Object>());
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
            put("deptSubId", 10);
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);

        Assertions.assertEquals(HandoffAction.ASK_USER, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.SLOT_QUERY, result.getNextStage());
        Assertions.assertEquals("no_slot_available", result.getSummary());
        Assertions.assertEquals("searchScheduleSlots", result.getToolName());
        Assertions.assertEquals("暂时没有找到可用号源，请换个日期或诊室再试。", result.getReply());
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
    void shouldSelectCandidateWhenSlotAvailable() {
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
            put("num", 5);
        }});
        Mockito.when(registrationAgentTools.searchDoctorPlansInDay(10, "2026-04-20"))
                .thenReturn(doctors);
        Mockito.when(registrationAgentTools.searchScheduleSlots(9, "2026-04-20"))
                .thenReturn(schedules);

        ScheduleAgentWorker worker = new ScheduleAgentWorker(medicalDeptAgentTools, registrationAgentTools);
        AgentContext context = new AgentContext();
        context.setPayload(new HashMap<String, Object>() {{
            put("deptId", 1);
            put("deptName", "内科");
            put("deptSubId", 10);
            put("deptSubName", "呼吸内科");
            put("date", "2026-04-20");
        }});
        context.setMemory(new HashMap<String, Object>());

        AgentResult result = worker.execute(context);
        Map<String, Object> patch = result.getMemoryPatch();
        Map<String, Object> pendingOrder = (Map<String, Object>) patch.get("pendingOrder");

        Assertions.assertEquals(HandoffAction.HANDOFF, result.getHandoffAction());
        Assertions.assertEquals(MultiAgentStage.POLICY_CHECK, result.getNextStage());
        Assertions.assertEquals("slot_selected", result.getSummary());
        Assertions.assertEquals("张医生", patch.get("doctorName"));
        Assertions.assertEquals(Boolean.TRUE, patch.get("awaitingConfirmation"));
        Assertions.assertEquals(9, pendingOrder.get("doctorId"));
        Assertions.assertEquals(202, pendingOrder.get("scheduleId"));
        Assertions.assertEquals(101, pendingOrder.get("workPlanId"));
        Assertions.assertEquals(1, pendingOrder.get("slot"));
        Assertions.assertEquals("18.00", pendingOrder.get("amount"));
    }
}
