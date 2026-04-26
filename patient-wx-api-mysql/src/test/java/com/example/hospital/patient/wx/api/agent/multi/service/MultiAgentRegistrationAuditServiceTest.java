package com.example.hospital.patient.wx.api.agent.multi.service;

import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentAuditStatus;
import com.example.hospital.patient.wx.api.db.dao.MultiAgentRegistrationAuditDao;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

class MultiAgentRegistrationAuditServiceTest {

    @Test
    void shouldMarkRepairPendingAsReservedStatus() {
        MultiAgentRegistrationAuditDao auditDao = Mockito.mock(MultiAgentRegistrationAuditDao.class);
        MultiAgentRegistrationAuditService service = new MultiAgentRegistrationAuditService();
        injectDao(service, auditDao);

        service.markRepairPending("REQ-1", "REGISTRATION_DB_WRITE_FAILED", "待巡检补偿", "{}");

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(auditDao).updateByRequestId(captor.capture());
        Assertions.assertEquals("REQ-1", captor.getValue().get("requestId"));
        Assertions.assertEquals(MultiAgentAuditStatus.RESERVED, captor.getValue().get("status"));
        Assertions.assertEquals("REGISTRATION_DB_WRITE_FAILED", captor.getValue().get("errorCode"));
        Assertions.assertEquals("待巡检补偿", captor.getValue().get("errorMessage"));
        Assertions.assertNull(captor.getValue().get("outTradeNo"));
    }

    @Test
    void shouldUpdateTraceOnly() {
        MultiAgentRegistrationAuditDao auditDao = Mockito.mock(MultiAgentRegistrationAuditDao.class);
        MultiAgentRegistrationAuditService service = new MultiAgentRegistrationAuditService();
        injectDao(service, auditDao);

        service.updateTrace("REQ-2", "{\"trace\":true}");

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(auditDao).updateByRequestId(captor.capture());
        Assertions.assertEquals("REQ-2", captor.getValue().get("requestId"));
        Assertions.assertEquals("{\"trace\":true}", captor.getValue().get("traceJson"));
    }

    private void injectDao(MultiAgentRegistrationAuditService service, MultiAgentRegistrationAuditDao auditDao) {
        try {
            java.lang.reflect.Field field = MultiAgentRegistrationAuditService.class.getDeclaredField("auditDao");
            field.setAccessible(true);
            field.set(service, auditDao);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
