package com.example.hospital.patient.wx.api.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentAuditStatus;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentErrorCode;
import com.example.hospital.patient.wx.api.agent.multi.model.MultiAgentStage;
import com.example.hospital.patient.wx.api.agent.multi.service.MultiAgentRegistrationAuditService;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.db.dao.DoctorWorkPlanDao;
import com.example.hospital.patient.wx.api.db.dao.DoctorWorkPlanScheduleDao;
import com.example.hospital.patient.wx.api.db.dao.MedicalRegistrationDao;
import com.example.hospital.patient.wx.api.db.dao.UserDao;
import com.example.hospital.patient.wx.api.db.pojo.MedicalRegistrationEntity;
import com.example.hospital.patient.wx.api.exception.HospitalException;
import com.example.hospital.patient.wx.api.service.MessageService;
import com.example.hospital.patient.wx.api.service.RegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {
    private static final String PASS_RESULT = "满足挂号条件";
    private static final byte ACTIVE_PAYMENT_STATUS = 2;
    private static final int DAILY_LIMIT = 3;
    private static final long SUBMIT_LOCK_TTL_SECONDS = 15L;

    @Resource
    private DoctorWorkPlanDao doctorWorkPlanDao;

    @Resource
    private MedicalRegistrationDao medicalRegistrationDao;

    @Resource
    private DoctorWorkPlanScheduleDao doctorWorkPlanScheduleDao;

    @Resource
    private UserDao userDao;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private MessageService messageService;

    @Resource
    private MultiAgentRegistrationAuditService multiAgentRegistrationAuditService;

    @Override
    public ArrayList<String> searchCanRegisterInDateRange(Map param) {
        ArrayList<String> list = doctorWorkPlanDao.searchCanRegisterInDateRange(param);
        DateTime startDate = DateUtil.parse(MapUtil.getStr(param, "startDate"));
        DateTime endDate = DateUtil.parse(MapUtil.getStr(param, "endDate"));

        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
        ArrayList result = new ArrayList();
        while (range.hasNext()) {
            String date = range.next().toDateStr();
            if (list.contains(date)) {
                result.add(new HashMap() {{
                    put("date", date);
                    put("status", "出诊");
                }});
            } else {
                result.add(new HashMap() {{
                    put("date", date);
                    put("status", "无号");
                }});
            }
        }
        return result;
    }

    @Override
    public ArrayList<HashMap> searchDeptSubDoctorPlanInDay(Map param) {
        return doctorWorkPlanDao.searchDeptSubDoctorPlanInDay(param);
    }

    @Override
    public String checkRegisterCondition(Map param) {
        param.put("today", DateUtil.today());
        long count = medicalRegistrationDao.searchRegistrationCountInToday(param);
        if (count >= DAILY_LIMIT) {
            return "已经达到当天挂号上限";
        }

        Integer id = medicalRegistrationDao.hasRegisterRecordInDay(param);
        if (id != null) {
            return "已经挂过该诊室的号";
        }

        return PASS_RESULT;
    }

    @Override
    public ArrayList<HashMap> searchDoctorWorkPlanSchedule(Map param) {
        ArrayList<HashMap> list = doctorWorkPlanScheduleDao.searchDoctorWorkPlanSchedule(param);
        if (list == null || list.isEmpty()) {
            return list;
        }
        for (HashMap schedule : list) {
            Integer scheduleId = intValue(schedule.get("scheduleId"));
            if (scheduleId == null) {
                continue;
            }
            String key = "doctor_schedule_" + scheduleId;
            HashMap<String, Object> cache = new HashMap<>();
            cache.put("maximum", intValue(schedule.get("maximum")));
            cache.put("num", intValue(schedule.get("num")));
            redisTemplate.opsForHash().putAll(key, cache);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HashMap registerMedicalAppointment(Map param) {
        Integer userId = intValue(param.get("userId"));
        Integer workPlanId = intValue(param.get("workPlanId"));
        Integer scheduleId = intValue(param.get("scheduleId"));
        Integer doctorId = intValue(param.get("doctorId"));
        Integer deptSubId = intValue(param.get("deptSubId"));
        Integer slot = intValue(param.get("slot"));
        String date = stringValue(param.get("date"));
        BigDecimal inputAmount = decimalValue(param.get("amount"));
        String sessionId = stringValue(param.get("sessionId"));
        String requestId = stringValue(param.get("requestId"));
        boolean auditEnabled = StringUtils.hasText(sessionId) || StringUtils.hasText(requestId);
        if (auditEnabled && !StringUtils.hasText(requestId)) {
            requestId = IdUtil.simpleUUID();
            param.put("requestId", requestId);
        }
        String lockOwner = StringUtils.hasText(requestId) ? requestId : IdUtil.simpleUUID();
        String submitLockKey = buildSubmitLockKey(userId, scheduleId, date, slot);
        String scheduleKey = scheduleId == null ? null : "doctor_schedule_" + scheduleId;
        Map<String, Object> auditSnapshot = buildAuditSnapshot(param);
        String replayDecision = null;
        String replayPriorStatus = null;
        String traceJson = auditEnabled ? multiAgentRegistrationAuditService.toTraceJson(buildTraceSnapshot(auditSnapshot, null, null, null, null, null)) : null;
        if (auditEnabled) {
            HashMap replay = decideReplay(requestId, auditSnapshot);
            if (replay != null) {
                replayDecision = stringValue(replay.get("decision"));
                replayPriorStatus = stringValue(replay.get("status"));
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot,
                        replayDecision,
                        replayPriorStatus,
                        stringValue(replay.get("errorCode")),
                        stringValue(replay.get("message")),
                        null);
                if (Boolean.TRUE.equals(replay.get("terminal"))) {
                    return (HashMap) replay.get("result");
                }
            }
            multiAgentRegistrationAuditService.prepare(requestId, sessionId, userId, param, traceJson);
        }

        if (userId == null || workPlanId == null || scheduleId == null || doctorId == null || deptSubId == null
                || slot == null || !StringUtils.hasText(date) || inputAmount == null) {
            traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                    MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, "挂号参数不完整，请重新选择号源。", null);
            return failResult(auditEnabled, requestId, MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, "挂号参数不完整，请重新选择号源。", true, traceJson);
        }

        if (!tryAcquireSubmitLock(submitLockKey, lockOwner)) {
            traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, "duplicate_submit", replayPriorStatus,
                    MultiAgentErrorCode.REGISTRATION_DUPLICATE_SUBMIT, "挂号请求正在处理中，请勿重复提交。", null);
            return failResult(auditEnabled, requestId, MultiAgentErrorCode.REGISTRATION_DUPLICATE_SUBMIT, "挂号请求正在处理中，请勿重复提交。", true, traceJson);
        }

        boolean redisReserved = false;
        String outTradeNo = null;
        try {
            HashMap userMap = userDao.searchOpenId(userId);
            Integer patientCardId = userMap == null ? null : MapUtil.getInt(userMap, "patientCardId");
            if (patientCardId == null) {
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                        MultiAgentErrorCode.REGISTRATION_USER_CARD_REQUIRED, "挂号前需要先创建就诊卡。", null);
                return failResult(auditEnabled, requestId, MultiAgentErrorCode.REGISTRATION_USER_CARD_REQUIRED, "挂号前需要先创建就诊卡。", false, traceJson);
            }

            String condition = checkRegisterCondition(new HashMap<String, Object>() {{
                put("userId", userId);
                put("deptSubId", deptSubId);
                put("date", date);
            }});
            if (!PASS_RESULT.equals(condition)) {
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus, null, condition, null);
                return failByCondition(auditEnabled, requestId, condition, traceJson);
            }

            HashMap snapshot = doctorWorkPlanScheduleDao.searchScheduleSnapshot(new HashMap<String, Object>() {{
                put("workPlanId", workPlanId);
                put("scheduleId", scheduleId);
            }});
            HashMap snapshotFailure = validateSnapshot(snapshot, doctorId, deptSubId, date, slot, inputAmount);
            if (snapshotFailure != null) {
                String errorCode = MapUtil.getStr(snapshotFailure, "errorCode");
                String message = MapUtil.getStr(snapshotFailure, "message");
                Boolean retryable = MapUtil.getBool(snapshotFailure, "retryable", true);
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus, errorCode, message, null);
                return failResult(auditEnabled, requestId, errorCode, message, retryable, traceJson);
            }

            if (!redisTemplate.hasKey(scheduleKey)) {
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                        MultiAgentErrorCode.REGISTRATION_SLOT_EXHAUSTED, "该时段号源已满，请重新选择。", null);
                return failResult(auditEnabled, requestId, MultiAgentErrorCode.REGISTRATION_SLOT_EXHAUSTED, "该时段号源已满，请重新选择。", true, traceJson);
            }
            redisReserved = reserveScheduleSlot(scheduleKey);
            if (!redisReserved) {
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                        MultiAgentErrorCode.REGISTRATION_SLOT_EXHAUSTED, "该时段号源已满，请重新选择。", null);
                return failResult(auditEnabled, requestId, MultiAgentErrorCode.REGISTRATION_SLOT_EXHAUSTED, "该时段号源已满，请重新选择。", true, traceJson);
            }

            outTradeNo = IdUtil.simpleUUID().toUpperCase();
            traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                    null, null, outTradeNo);
            if (auditEnabled) {
                multiAgentRegistrationAuditService.markReserved(requestId, outTradeNo, traceJson);
            }

            MedicalRegistrationEntity entity = new MedicalRegistrationEntity();
            entity.setWorkPlanId(workPlanId);
            entity.setDoctorScheduleId(scheduleId);
            entity.setPatientCardId(patientCardId);
            entity.setDoctorId(doctorId);
            entity.setDeptSubId(deptSubId);
            entity.setDate(date);
            entity.setSlot(slot);
            entity.setAmount(decimalValue(snapshot.get("amount")));
            entity.setOutTradeNo(outTradeNo);
            entity.setPrepayId("1");
            entity.setPaymentStatus(ACTIVE_PAYMENT_STATUS);
            medicalRegistrationDao.insert(entity);

            doctorWorkPlanDao.updateNumById(new HashMap<String, Object>() {{
                put("id", workPlanId);
                put("n", 1);
            }});
            doctorWorkPlanScheduleDao.updateNumById(new HashMap<String, Object>() {{
                put("id", scheduleId);
                put("n", 1);
            }});

            try {
                messageService.sendMessage(userId, (byte) 1, "挂号成功", "您已成功挂号，就诊日期：" + date, entity.getId());
            } catch (Exception e) {
                log.error("send registration success message failed", e);
            }

            if (auditEnabled) {
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                        null, null, outTradeNo);
                multiAgentRegistrationAuditService.markSuccess(requestId, entity.getId(), outTradeNo, traceJson);
            }
            return successResult(entity.getId(), outTradeNo);
        } catch (HospitalException e) {
            if (redisReserved && scheduleKey != null) {
                boolean compensated = releaseScheduleReservation(scheduleKey);
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                        MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, e.getMsg(), outTradeNo);
                markCompensation(auditEnabled, requestId, traceJson, e.getMsg(), compensated, MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR);
            } else if (auditEnabled) {
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                        MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, e.getMsg(), outTradeNo);
                multiAgentRegistrationAuditService.markFail(requestId, MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, e.getMsg(), traceJson);
            }
            throw e;
        } catch (Exception e) {
            log.error("register medical appointment failed", e);
            if (redisReserved && scheduleKey != null) {
                boolean compensated = releaseScheduleReservation(scheduleKey);
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                        MultiAgentErrorCode.REGISTRATION_DB_WRITE_FAILED, "挂号提交失败，请稍后重试。", outTradeNo);
                markCompensation(auditEnabled, requestId, traceJson, "挂号提交失败，请稍后重试。", compensated, MultiAgentErrorCode.REGISTRATION_DB_WRITE_FAILED);
            } else if (auditEnabled) {
                traceJson = updateTrace(auditEnabled, requestId, auditSnapshot, replayDecision, replayPriorStatus,
                        MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, "挂号提交失败，请稍后重试。", outTradeNo);
                multiAgentRegistrationAuditService.markFail(requestId, MultiAgentErrorCode.REGISTRATION_SYSTEM_ERROR, "挂号提交失败，请稍后重试。", traceJson);
            }
            throw new HospitalException("挂号提交失败，请稍后重试。", e);
        } finally {
            releaseSubmitLock(submitLockKey, lockOwner);
        }
    }

    @Override
    public PageUtils searchRegistrationByPage(Map param) {
        ArrayList list;
        long count = medicalRegistrationDao.searchRegistrationCount(param);
        if (count > 0) {
            list = medicalRegistrationDao.searchRegistrationByPage(param);
        } else {
            list = new ArrayList();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        return new PageUtils(list, count, page, length);
    }

    @Override
    public HashMap searchRegistrationInfo(Map param) {
        return medicalRegistrationDao.searchRegistrationInfo(param);
    }

    private HashMap failByCondition(boolean auditEnabled, String requestId, String condition, String traceJson) {
        if ("已经达到当天挂号上限".equals(condition)) {
            return failResult(auditEnabled, requestId, MultiAgentErrorCode.REGISTRATION_DAILY_LIMIT_REACHED, condition, false, traceJson);
        }
        if ("已经挂过该诊室的号".equals(condition)) {
            return failResult(auditEnabled, requestId, MultiAgentErrorCode.REGISTRATION_REPEAT_IN_DAY, condition, false, traceJson);
        }
        return failResult(auditEnabled, requestId, MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, condition, true, traceJson);
    }

    private HashMap validateSnapshot(HashMap snapshot,
                                     Integer doctorId,
                                     Integer deptSubId,
                                     String date,
                                     Integer slot,
                                     BigDecimal inputAmount) {
        if (snapshot == null || snapshot.isEmpty()) {
            return buildErrorResult(MultiAgentErrorCode.REGISTRATION_SLOT_CHANGED, "该号源已变化，请重新选择。", true);
        }
        if (!equalsNumber(snapshot.get("doctorId"), doctorId)
                || !equalsNumber(snapshot.get("deptSubId"), deptSubId)
                || !equalsNumber(snapshot.get("slot"), slot)
                || !equalsText(snapshot.get("date"), date)) {
            return buildErrorResult(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, "挂号信息已变化，请重新选择号源。", true);
        }
        BigDecimal actualAmount = decimalValue(snapshot.get("amount"));
        if (actualAmount == null || inputAmount == null || actualAmount.compareTo(inputAmount) != 0) {
            return buildErrorResult(MultiAgentErrorCode.REGISTRATION_SLOT_CHANGED, "号源费用已变化，请重新选择。", true);
        }
        return null;
    }

    private boolean reserveScheduleSlot(String key) {
        Object execute = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.watch(key);
                Map entry = operations.opsForHash().entries(key);
                if (entry == null || entry.isEmpty() || entry.get("maximum") == null || entry.get("num") == null) {
                    operations.unwatch();
                    return null;
                }
                int maximum = Integer.parseInt(String.valueOf(entry.get("maximum")));
                int num = Integer.parseInt(String.valueOf(entry.get("num")));
                if (num >= maximum) {
                    operations.unwatch();
                    return null;
                }
                operations.multi();
                operations.opsForHash().increment(key, "num", 1);
                return operations.exec();
            }
        });
        if (execute == null) {
            return false;
        }
        if (execute instanceof List) {
            return !((List) execute).isEmpty();
        }
        return true;
    }

    private boolean releaseScheduleReservation(String key) {
        try {
            Object execute = redisTemplate.execute(new SessionCallback() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    operations.watch(key);
                    Map entry = operations.opsForHash().entries(key);
                    if (entry == null || entry.isEmpty() || entry.get("num") == null) {
                        operations.unwatch();
                        return null;
                    }
                    int num = Integer.parseInt(String.valueOf(entry.get("num")));
                    if (num <= 0) {
                        operations.unwatch();
                        return Boolean.TRUE;
                    }
                    operations.multi();
                    operations.opsForHash().increment(key, "num", -1);
                    return operations.exec();
                }
            });
            if (execute == null) {
                return false;
            }
            if (execute instanceof List) {
                return !((List) execute).isEmpty();
            }
            return true;
        } catch (Exception e) {
            log.error("release schedule reservation failed", e);
            return false;
        }
    }

    private HashMap successResult(Integer registrationId, String outTradeNo) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("registrationId", registrationId);
        result.put("outTradeNo", outTradeNo);
        result.put("paymentStatus", ACTIVE_PAYMENT_STATUS);
        return result;
    }

    private HashMap failResult(boolean auditEnabled,
                               String requestId,
                               String errorCode,
                               String message,
                               boolean retryable,
                               String traceJson) {
        if (auditEnabled) {
            multiAgentRegistrationAuditService.markFail(requestId, errorCode, message, traceJson);
        }
        return buildErrorResult(errorCode, message, retryable);
    }

    private HashMap buildErrorResult(String errorCode, String message, boolean retryable) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("errorCode", errorCode);
        result.put("message", message);
        result.put("retryable", retryable);
        return result;
    }

    private void markCompensation(boolean auditEnabled,
                                  String requestId,
                                  String traceJson,
                                  String message,
                                  boolean compensated,
                                  String errorCode) {
        if (!auditEnabled) {
            return;
        }
        if (compensated) {
            multiAgentRegistrationAuditService.markCompensated(requestId, errorCode, message, traceJson);
            return;
        }
        multiAgentRegistrationAuditService.markRepairPending(requestId, errorCode, message, traceJson);
    }

    private HashMap decideReplay(String requestId, Map<String, Object> auditSnapshot) {
        HashMap existing = multiAgentRegistrationAuditService.searchByRequestId(requestId);
        if (existing == null || existing.isEmpty()) {
            return null;
        }
        String status = stringValue(existing.get("status"));
        if (!StringUtils.hasText(status) || MultiAgentAuditStatus.INIT.equals(status)) {
            return null;
        }
        if (!matchesAuditSnapshot(existing, auditSnapshot)) {
            HashMap<String, Object> result = buildErrorResult(MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH, "当前请求与原挂号信息不一致，请重新选择号源。", true);
            result.put("replayDecision", "reject_mismatch");
            return new HashMap<String, Object>() {{
                put("terminal", true);
                put("decision", "reject_mismatch");
                put("status", status);
                put("errorCode", MultiAgentErrorCode.REGISTRATION_PARAM_MISMATCH);
                put("message", "当前请求与原挂号信息不一致，请重新选择号源。");
                put("result", result);
            }};
        }
        if (MultiAgentAuditStatus.SUCCESS.equals(status)) {
            HashMap<String, Object> result = successResult(intValue(existing.get("registrationId")), stringValue(existing.get("outTradeNo")));
            result.put("replayDecision", "reuse_success");
            return new HashMap<String, Object>() {{
                put("terminal", true);
                put("decision", "reuse_success");
                put("status", status);
                put("result", result);
            }};
        }
        if (MultiAgentAuditStatus.RESERVED.equals(status)) {
            HashMap<String, Object> result = buildErrorResult(MultiAgentErrorCode.REGISTRATION_DUPLICATE_SUBMIT, "挂号请求正在处理中，请勿重复提交。", true);
            result.put("replayDecision", "reserved_pending");
            return new HashMap<String, Object>() {{
                put("terminal", true);
                put("decision", "reserved_pending");
                put("status", status);
                put("errorCode", MultiAgentErrorCode.REGISTRATION_DUPLICATE_SUBMIT);
                put("message", "挂号请求正在处理中，请勿重复提交。");
                put("result", result);
            }};
        }
        if (MultiAgentAuditStatus.FAIL.equals(status) || MultiAgentAuditStatus.COMPENSATED.equals(status)) {
            return new HashMap<String, Object>() {{
                put("terminal", false);
                put("decision", "retry_after_failure");
                put("status", status);
            }};
        }
        return null;
    }

    private String updateTrace(boolean auditEnabled,
                               String requestId,
                               Map<String, Object> requestSnapshot,
                               String replayDecision,
                               String priorStatus,
                               String errorCode,
                               String message,
                               String outTradeNo) {
        if (!auditEnabled) {
            return null;
        }
        String traceJson = multiAgentRegistrationAuditService.toTraceJson(buildTraceSnapshot(requestSnapshot,
                replayDecision,
                priorStatus,
                errorCode,
                message,
                outTradeNo));
        multiAgentRegistrationAuditService.updateTrace(requestId, traceJson);
        return traceJson;
    }

    private Map<String, Object> buildTraceSnapshot(Map<String, Object> requestSnapshot,
                                                   String replayDecision,
                                                   String priorStatus,
                                                   String errorCode,
                                                   String message,
                                                   String outTradeNo) {
        HashMap<String, Object> trace = new HashMap<>();
        trace.put("request", requestSnapshot == null ? new HashMap<String, Object>() : new HashMap<String, Object>(requestSnapshot));
        trace.put("confirmation", new HashMap<String, Object>());
        trace.put("failure", new HashMap<String, Object>() {{
            put("stage", MultiAgentStage.EXECUTE_APPOINTMENT.name());
            put("errorCode", errorCode);
            put("message", message);
        }});
        trace.put("replay", new HashMap<String, Object>() {{
            put("requestId", requestSnapshot == null ? null : requestSnapshot.get("requestId"));
            put("priorStatus", priorStatus);
            put("replayDecision", replayDecision);
            put("outTradeNo", outTradeNo);
        }});
        return trace;
    }

    private boolean matchesAuditSnapshot(Map<String, Object> auditRecord, Map<String, Object> requestSnapshot) {
        return equalsNumber(auditRecord.get("userId"), intValue(requestSnapshot.get("userId")))
                && equalsNumber(auditRecord.get("workPlanId"), intValue(requestSnapshot.get("workPlanId")))
                && equalsNumber(auditRecord.get("scheduleId"), intValue(requestSnapshot.get("scheduleId")))
                && equalsNumber(auditRecord.get("doctorId"), intValue(requestSnapshot.get("doctorId")))
                && equalsNumber(auditRecord.get("deptSubId"), intValue(requestSnapshot.get("deptSubId")))
                && equalsText(auditRecord.get("date"), stringValue(requestSnapshot.get("date")))
                && equalsNumber(auditRecord.get("slot"), intValue(requestSnapshot.get("slot")))
                && equalsDecimal(auditRecord.get("amount"), decimalValue(requestSnapshot.get("amount")));
    }

    private Map<String, Object> buildAuditSnapshot(Map param) {
        HashMap<String, Object> snapshot = new HashMap<>();
        snapshot.put("requestId", stringValue(param.get("requestId")));
        snapshot.put("sessionId", stringValue(param.get("sessionId")));
        snapshot.put("userId", intValue(param.get("userId")));
        snapshot.put("workPlanId", intValue(param.get("workPlanId")));
        snapshot.put("scheduleId", intValue(param.get("scheduleId")));
        snapshot.put("doctorId", intValue(param.get("doctorId")));
        snapshot.put("deptSubId", intValue(param.get("deptSubId")));
        snapshot.put("date", stringValue(param.get("date")));
        snapshot.put("slot", intValue(param.get("slot")));
        snapshot.put("amount", stringValue(param.get("amount")));
        return snapshot;
    }

    private boolean tryAcquireSubmitLock(String key, String owner) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(owner)) {
            return false;
        }
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, owner, SUBMIT_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(locked);
    }

    private void releaseSubmitLock(String key, String owner) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(owner)) {
            return;
        }
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && owner.equals(String.valueOf(value))) {
                redisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("release submit lock failed", e);
        }
    }

    private String buildSubmitLockKey(Integer userId, Integer scheduleId, String date, Integer slot) {
        if (userId == null || scheduleId == null || !StringUtils.hasText(date) || slot == null) {
            return null;
        }
        return "agent_register_submit:" + userId + ":" + scheduleId + ":" + date + ":" + slot;
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

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean equalsNumber(Object left, Integer right) {
        Integer leftInt = intValue(left);
        return leftInt != null && leftInt.equals(right);
    }

    private boolean equalsText(Object left, String right) {
        String leftText = stringValue(left);
        return StringUtils.hasText(leftText) && leftText.equals(right);
    }

    private boolean equalsDecimal(Object left, BigDecimal right) {
        BigDecimal leftDecimal = decimalValue(left);
        return leftDecimal != null && right != null && leftDecimal.compareTo(right) == 0;
    }
}
