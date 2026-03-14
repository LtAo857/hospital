package com.example.hospital.patient.wx.api.service.impl;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.db.dao.EvaluationDao;
import com.example.hospital.patient.wx.api.db.dao.UserInfoCardDao;
import com.example.hospital.patient.wx.api.db.pojo.EvaluationEntity;
import com.example.hospital.patient.wx.api.service.EvaluationService;
import com.example.hospital.patient.wx.api.service.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class EvaluationServiceImpl implements EvaluationService {
    @Resource
    private EvaluationDao evaluationDao;

    @Resource
    private UserInfoCardDao userInfoCardDao;

    @Resource
    private MessageService messageService;

    @Override
    @Transactional
    public void insert(Map param) {
        int userId = MapUtil.getInt(param, "userId");
        int doctorId = MapUtil.getInt(param, "doctorId");
        byte score = MapUtil.getInt(param, "score").byteValue();
        String comment = MapUtil.getStr(param, "comment");
        Integer registrationId = MapUtil.getInt(param, "registrationId");
        Integer videoDiagnoseId = MapUtil.getInt(param, "videoDiagnoseId");

        // 查询患者就诊卡ID
        int patientCardId = userInfoCardDao.searchIdByUserId(userId);

        EvaluationEntity entity = new EvaluationEntity();
        entity.setDoctorId(doctorId);
        entity.setPatientCardId(patientCardId);
        entity.setRegistrationId(registrationId);
        entity.setVideoDiagnoseId(videoDiagnoseId);
        entity.setScore(score);
        entity.setComment(comment);
        evaluationDao.insert(entity);

        // 发送评价成功消息
        messageService.sendMessage(userId, (byte) 4, "评价成功", "您的评价已提交成功，感谢您的反馈", entity.getId());
    }

    @Override
    public boolean hasEvaluated(Map param) {
        int userId = MapUtil.getInt(param, "userId");
        int patientCardId = userInfoCardDao.searchIdByUserId(userId);
        param.put("patientCardId", patientCardId);
        Integer id = evaluationDao.hasEvaluated(param);
        return id != null;
    }

    @Override
    public PageUtils searchEvaluationByPage(Map param) {
        long count = evaluationDao.searchEvaluationCount(param);
        ArrayList list;
        if (count > 0) {
            list = evaluationDao.searchEvaluationByPage(param);
        } else {
            list = new ArrayList();
        }
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        return new PageUtils(list, count, page, length);
    }

    @Override
    public HashMap searchDoctorEvaluation(Map param) {
        int doctorId = MapUtil.getInt(param, "doctorId");
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        int start = (page - 1) * length;
        param.put("start", start);

        HashMap avgScore = evaluationDao.searchDoctorAvgScore(doctorId);
        long count = evaluationDao.searchDoctorEvaluationCount(doctorId);
        ArrayList list;
        if (count > 0) {
            list = evaluationDao.searchDoctorEvaluationByPage(param);
        } else {
            list = new ArrayList();
        }
        PageUtils pageUtils = new PageUtils(list, count, page, length);

        HashMap result = new HashMap();
        result.put("avgScore", avgScore != null ? avgScore.get("avgScore") : 0);
        result.put("totalCount", avgScore != null ? avgScore.get("totalCount") : 0);
        result.put("page", pageUtils);
        return result;
    }
}
