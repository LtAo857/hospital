package com.example.hospital.patient.wx.api.job;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentAuditStatus;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.service.MultiAgentRegistrationAuditService;
import com.example.hospital.patient.wx.api.db.dao.DoctorWorkPlanDao;
import com.example.hospital.patient.wx.api.db.dao.DoctorWorkPlanScheduleDao;
import com.example.hospital.patient.wx.api.db.dao.MedicalRegistrationDao;
import com.example.hospital.patient.wx.api.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
public class MultiAgentRegistrationRepairJob extends QuartzJobBean {
    private static final int REPAIR_THRESHOLD_MINUTES = 2;

    @Resource
    private MultiAgentRegistrationAuditService auditService;

    @Resource
    private MedicalRegistrationDao medicalRegistrationDao;

    @Resource
    private DoctorWorkPlanDao doctorWorkPlanDao;

    @Resource
    private DoctorWorkPlanScheduleDao doctorWorkPlanScheduleDao;

    @Resource
    private RedisTemplate<Object, Object> redisTemplate;

    @Resource
    private MessageService messageService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        ArrayList<HashMap> list = auditService.searchRepairCandidatesBeforeMinutes(REPAIR_THRESHOLD_MINUTES);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (HashMap item : list) {
            try {
                repairOne(item);
            } catch (Exception e) {
                log.error("repair multi-agent registration failed, requestId={}", MapUtil.getStr(item, "requestId"), e);
            }
        }
    }

    private void repairOne(HashMap item) {
        String requestId = MapUtil.getStr(item, "requestId");
        String status = MapUtil.getStr(item, "status");
        String outTradeNo = MapUtil.getStr(item, "outTradeNo");
        Integer userId = MapUtil.getInt(item, "userId");
        Integer workPlanId = MapUtil.getInt(item, "workPlanId");
        Integer scheduleId = MapUtil.getInt(item, "scheduleId");
        String traceJson = MapUtil.getStr(item, "traceJson");
        if (!StringUtils.hasText(requestId)) {
            return;
        }

        HashMap registration = StringUtils.hasText(outTradeNo) ? medicalRegistrationDao.searchByOutTradeNo(outTradeNo) : null;
        if (registration != null && !registration.isEmpty()) {
            Integer registrationId = MapUtil.getInt(registration, "id");
            String finalOutTradeNo = MapUtil.getStr(registration, "outTradeNo");
            auditService.markSuccess(requestId, registrationId, finalOutTradeNo, traceJson);
            return;
        }

        boolean compensated = true;
        if (StringUtils.hasText(outTradeNo)) {
            if (workPlanId != null) {
                compensated = doctorWorkPlanDao.releaseNumByOutTradeNo(outTradeNo) > 0;
            }
            if (scheduleId != null) {
                boolean dbReleased = doctorWorkPlanScheduleDao.releaseNumByOutTradeNo(outTradeNo) > 0;
                boolean redisReleased = ensureRedisScheduleRollback(scheduleId);
                compensated = compensated && dbReleased && redisReleased;
            }
        } else if (MultiAgentAuditStatus.RESERVED.equals(status)) {
            compensated = false;
        }

        if (compensated) {
            auditService.markCompensated(requestId, MultiAgentErrorCode.REGISTRATION_DB_WRITE_FAILED, "巡检已完成号源补偿。", traceJson);
            notifyUser(userId, "挂号提交未完成，系统已自动恢复号源，可重新选择。", requestId);
            return;
        }
        auditService.markFail(requestId, MultiAgentErrorCode.REGISTRATION_DB_WRITE_FAILED, "巡检发现异常，请人工核对挂号记录。", traceJson);
        notifyUser(userId, "挂号提交状态异常，请稍后查看挂号记录或重新尝试。", requestId);
    }

    private boolean ensureRedisScheduleRollback(Integer scheduleId) {
        if (scheduleId == null) {
            return false;
        }
        String key = "doctor_schedule_" + scheduleId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return false;
        }
        Object numValue = redisTemplate.opsForHash().get(key, "num");
        if (numValue == null) {
            return false;
        }
        int num = Integer.parseInt(String.valueOf(numValue));
        if (num <= 0) {
            return true;
        }
        redisTemplate.opsForHash().increment(key, "num", -1);
        return true;
    }

    private void notifyUser(Integer userId, String content, String requestId) {
        if (userId == null) {
            return;
        }
        try {
            messageService.sendMessage(userId, (byte) 1, "挂号状态提醒", content, 0);
        } catch (Exception e) {
            log.warn("send repair notification failed, requestId={}", requestId, e);
        }
    }
}
