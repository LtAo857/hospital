package com.example.hospital.patient.wx.api.service.impl;

import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentAuditStatus;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.service.MultiAgentRegistrationAuditService;
import com.example.hospital.patient.wx.api.db.dao.DoctorWorkPlanDao;
import com.example.hospital.patient.wx.api.db.dao.DoctorWorkPlanScheduleDao;
import com.example.hospital.patient.wx.api.db.dao.MedicalRegistrationDao;
import com.example.hospital.patient.wx.api.db.dao.UserDao;
import com.example.hospital.patient.wx.api.service.MessageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

class RegistrationServiceImplTest {

    @Test
    void shouldReusePreviousSuccessWhenRequestIdReplayed() {
        RegistrationServiceImpl service = new RegistrationServiceImpl();
        MultiAgentRegistrationAuditService auditService = Mockito.mock(MultiAgentRegistrationAuditService.class);
        MedicalRegistrationDao medicalRegistrationDao = Mockito.mock(MedicalRegistrationDao.class);
        inject(service, "multiAgentRegistrationAuditService", auditService);
        inject(service, "medicalRegistrationDao", medicalRegistrationDao);
        inject(service, "doctorWorkPlanDao", Mockito.mock(DoctorWorkPlanDao.class));
        inject(service, "doctorWorkPlanScheduleDao", Mockito.mock(DoctorWorkPlanScheduleDao.class));
        inject(service, "userDao", Mockito.mock(UserDao.class));
        inject(service, "redisTemplate", Mockito.mock(RedisTemplate.class));
        inject(service, "messageService", Mockito.mock(MessageService.class));

        Mockito.when(auditService.searchByRequestId("REQ-1")).thenReturn(new HashMap<String, Object>() {{
            put("requestId", "REQ-1");
            put("status", MultiAgentAuditStatus.SUCCESS);
            put("userId", 1001);
            put("workPlanId", 101);
            put("scheduleId", 202);
            put("doctorId", 303);
            put("deptSubId", 10);
            put("date", "2026-04-20");
            put("slot", 1);
            put("amount", "10.00");
            put("registrationId", 9001);
            put("outTradeNo", "OT-9001");
        }});

        HashMap result = service.registerMedicalAppointment(buildParam("REQ-1", "10"));

        Assertions.assertEquals(9001, result.get("registrationId"));
        Assertions.assertEquals("OT-9001", result.get("outTradeNo"));
        Assertions.assertEquals("reuse_success", result.get("replayDecision"));
        Mockito.verify(medicalRegistrationDao, Mockito.never()).insert(Mockito.any());
    }

    @Test
    void shouldRejectReplayWhenPayloadDiffersFromExistingAudit() {
        RegistrationServiceImpl service = new RegistrationServiceImpl();
        MultiAgentRegistrationAuditService auditService = Mockito.mock(MultiAgentRegistrationAuditService.class);
        MedicalRegistrationDao medicalRegistrationDao = Mockito.mock(MedicalRegistrationDao.class);
        inject(service, "multiAgentRegistrationAuditService", auditService);
        inject(service, "medicalRegistrationDao", medicalRegistrationDao);
        inject(service, "doctorWorkPlanDao", Mockito.mock(DoctorWorkPlanDao.class));
        inject(service, "doctorWorkPlanScheduleDao", Mockito.mock(DoctorWorkPlanScheduleDao.class));
        inject(service, "userDao", Mockito.mock(UserDao.class));
        inject(service, "redisTemplate", Mockito.mock(RedisTemplate.class));
        inject(service, "messageService", Mockito.mock(MessageService.class));

        Mockito.when(auditService.searchByRequestId("REQ-2")).thenReturn(new HashMap<String, Object>() {{
            put("requestId", "REQ-2");
            put("status", MultiAgentAuditStatus.SUCCESS);
            put("userId", 1001);
            put("workPlanId", 101);
            put("scheduleId", 202);
            put("doctorId", 303);
            put("deptSubId", 10);
            put("date", "2026-04-20");
            put("slot", 1);
            put("amount", "10.00");
            put("registrationId", 9001);
            put("outTradeNo", "OT-9001");
        }});

        HashMap result = service.registerMedicalAppointment(buildParam("REQ-2", "12"));

        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, result.get("errorCode"));
        Assertions.assertEquals("reject_mismatch", result.get("replayDecision"));
        Mockito.verify(medicalRegistrationDao, Mockito.never()).insert(Mockito.any());
    }

    @Test
    void shouldReturnDuplicateSubmitWhenReplayStillReserved() {
        RegistrationServiceImpl service = new RegistrationServiceImpl();
        MultiAgentRegistrationAuditService auditService = Mockito.mock(MultiAgentRegistrationAuditService.class);
        MedicalRegistrationDao medicalRegistrationDao = Mockito.mock(MedicalRegistrationDao.class);
        inject(service, "multiAgentRegistrationAuditService", auditService);
        inject(service, "medicalRegistrationDao", medicalRegistrationDao);
        inject(service, "doctorWorkPlanDao", Mockito.mock(DoctorWorkPlanDao.class));
        inject(service, "doctorWorkPlanScheduleDao", Mockito.mock(DoctorWorkPlanScheduleDao.class));
        inject(service, "userDao", Mockito.mock(UserDao.class));
        inject(service, "redisTemplate", Mockito.mock(RedisTemplate.class));
        inject(service, "messageService", Mockito.mock(MessageService.class));

        Mockito.when(auditService.searchByRequestId("REQ-3")).thenReturn(new HashMap<String, Object>() {{
            put("requestId", "REQ-3");
            put("status", MultiAgentAuditStatus.RESERVED);
            put("userId", 1001);
            put("workPlanId", 101);
            put("scheduleId", 202);
            put("doctorId", 303);
            put("deptSubId", 10);
            put("date", "2026-04-20");
            put("slot", 1);
            put("amount", "10.00");
        }});

        HashMap result = service.registerMedicalAppointment(buildParam("REQ-3", "10"));

        Assertions.assertEquals(MultiAgentErrorCode.REGISTRATION_DUPLICATE_SUBMIT, result.get("errorCode"));
        Assertions.assertEquals("reserved_pending", result.get("replayDecision"));
        Mockito.verify(medicalRegistrationDao, Mockito.never()).insert(Mockito.any());
    }

    private Map<String, Object> buildParam(String requestId, String amount) {
        Map<String, Object> param = new HashMap<>();
        param.put("requestId", requestId);
        param.put("sessionId", "session-1");
        param.put("userId", 1001);
        param.put("workPlanId", 101);
        param.put("scheduleId", 202);
        param.put("doctorId", 303);
        param.put("deptSubId", 10);
        param.put("date", "2026-04-20");
        param.put("slot", 1);
        param.put("amount", amount);
        return param;
    }

    private void inject(Object target, String fieldName, Object value) {
        try {
            Field field = RegistrationServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
