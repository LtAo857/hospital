package com.example.hospital.patient.wx.api.agent.multi.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentAuditStatus;
import com.example.hospital.patient.wx.api.db.dao.MultiAgentRegistrationAuditDao;
import com.example.hospital.patient.wx.api.db.pojo.MultiAgentRegistrationAuditEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class MultiAgentRegistrationAuditService {
    @Resource
    private MultiAgentRegistrationAuditDao auditDao;

    public HashMap searchByRequestId(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return null;
        }
        return auditDao.searchByRequestId(requestId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void prepare(String requestId, String sessionId, Integer userId, Map<String, Object> payload, String traceJson) {
        if (!StringUtils.hasText(requestId)) {
            return;
        }
        HashMap existing = auditDao.searchByRequestId(requestId);
        if (existing == null) {
            MultiAgentRegistrationAuditEntity entity = new MultiAgentRegistrationAuditEntity();
            entity.setSessionId(sessionId);
            entity.setRequestId(requestId);
            entity.setUserId(userId);
            entity.setWorkPlanId(MapUtil.getInt(payload, "workPlanId"));
            entity.setScheduleId(MapUtil.getInt(payload, "scheduleId"));
            entity.setDoctorId(MapUtil.getInt(payload, "doctorId"));
            entity.setDeptSubId(MapUtil.getInt(payload, "deptSubId"));
            entity.setDate(MapUtil.getStr(payload, "date"));
            entity.setSlot(MapUtil.getInt(payload, "slot"));
            entity.setAmount(parseAmount(payload.get("amount")));
            entity.setStatus(MultiAgentAuditStatus.INIT);
            entity.setTraceJson(traceJson);
            auditDao.insert(entity);
            return;
        }
        HashMap<String, Object> update = new HashMap<>();
        update.put("requestId", requestId);
        update.put("sessionId", sessionId);
        update.put("userId", userId);
        update.put("workPlanId", MapUtil.getInt(payload, "workPlanId"));
        update.put("scheduleId", MapUtil.getInt(payload, "scheduleId"));
        update.put("doctorId", MapUtil.getInt(payload, "doctorId"));
        update.put("deptSubId", MapUtil.getInt(payload, "deptSubId"));
        update.put("date", MapUtil.getStr(payload, "date"));
        update.put("slot", MapUtil.getInt(payload, "slot"));
        update.put("amount", parseAmount(payload.get("amount")));
        update.put("status", MultiAgentAuditStatus.INIT);
        update.put("errorCode", null);
        update.put("errorMessage", null);
        update.put("registrationId", null);
        update.put("outTradeNo", null);
        update.put("traceJson", traceJson);
        auditDao.updateByRequestId(update);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markReserved(String requestId, String outTradeNo, String traceJson) {
        update(requestId, MultiAgentAuditStatus.RESERVED, null, null, null, outTradeNo, traceJson);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRepairPending(String requestId, String errorCode, String errorMessage, String traceJson) {
        update(requestId, MultiAgentAuditStatus.RESERVED, errorCode, errorMessage, null, null, traceJson);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String requestId, Integer registrationId, String outTradeNo, String traceJson) {
        update(requestId, MultiAgentAuditStatus.SUCCESS, null, null, registrationId, outTradeNo, traceJson);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFail(String requestId, String errorCode, String errorMessage, String traceJson) {
        update(requestId, MultiAgentAuditStatus.FAIL, errorCode, errorMessage, null, null, traceJson);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensated(String requestId, String errorCode, String errorMessage, String traceJson) {
        update(requestId, MultiAgentAuditStatus.COMPENSATED, errorCode, errorMessage, null, null, traceJson);
    }

    public ArrayList<HashMap> searchRepairCandidatesBeforeMinutes(int minutes) {
        return auditDao.searchRepairCandidatesBeforeMinutes(minutes);
    }

    private void update(String requestId,
                        String status,
                        String errorCode,
                        String errorMessage,
                        Integer registrationId,
                        String outTradeNo,
                        String traceJson) {
        if (!StringUtils.hasText(requestId)) {
            return;
        }
        HashMap<String, Object> update = new HashMap<>();
        update.put("requestId", requestId);
        update.put("status", status);
        update.put("errorCode", errorCode);
        update.put("errorMessage", errorMessage);
        update.put("registrationId", registrationId);
        update.put("outTradeNo", outTradeNo);
        update.put("traceJson", traceJson);
        auditDao.updateByRequestId(update);
    }

    public String toTraceJson(Map<String, Object> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return null;
        }
        try {
            return JSONUtil.toJsonStr(snapshot);
        } catch (Exception e) {
            log.warn("serialize multi-agent audit trace failed", e);
            return null;
        }
    }

    private BigDecimal parseAmount(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
