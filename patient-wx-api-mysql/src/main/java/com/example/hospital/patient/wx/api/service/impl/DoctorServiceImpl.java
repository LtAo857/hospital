package com.example.hospital.patient.wx.api.service.impl;

import cn.hutool.core.map.MapUtil;
import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.db.dao.DoctorDao;
import com.example.hospital.patient.wx.api.db.dao.EvaluationDao;
import com.example.hospital.patient.wx.api.db.dao.PatientDoctorFavoriteDao;
import com.example.hospital.patient.wx.api.db.dao.UserInfoCardDao;
import com.example.hospital.patient.wx.api.service.DoctorService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class DoctorServiceImpl implements DoctorService {
    @Resource
    private DoctorDao doctorDao;

    @Resource
    private UserInfoCardDao userInfoCardDao;

    @Resource
    private PatientDoctorFavoriteDao patientDoctorFavoriteDao;

    @Resource
    private EvaluationDao evaluationDao;


    @Override
    public HashMap searchDoctorInfoById(int id, Integer userId) {
        HashMap map = doctorDao.searchDoctorInfoById(id);
        HashMap avgScore = evaluationDao.searchDoctorAvgScore(id);
        map.put("avgScore", avgScore != null ? avgScore.get("avgScore") : 0);
        map.put("totalCount", avgScore != null ? avgScore.get("totalCount") : 0);

        boolean isFavorite = false;
        if (userId != null) {
            Integer patientCardId = userInfoCardDao.searchIdByUserId(userId);
            if (patientCardId != null) {
                Integer favoriteId = patientDoctorFavoriteDao.searchIdByPatientCardIdAndDoctorId(patientCardId, id);
                isFavorite = favoriteId != null;
            }
        }
        map.put("isFavorite", isFavorite);
        return map;
    }

    @Override
    public PageUtils searchDoctorInfo(Map param) {
        ArrayList list =doctorDao.searchDoctorInfo(param);

        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        PageUtils pageUtils = new PageUtils(list, list.size(), page, length);

        return pageUtils;
    }

    @Override
    public boolean favoriteDoctor(int userId, int doctorId) {
        Integer patientCardId = userInfoCardDao.searchIdByUserId(userId);
        if (patientCardId == null) {
            return false;
        }
        Integer favoriteId = patientDoctorFavoriteDao.searchIdByPatientCardIdAndDoctorId(patientCardId, doctorId);
        if (favoriteId == null) {
            patientDoctorFavoriteDao.insert(patientCardId, doctorId);
        }
        return true;
    }

    @Override
    public boolean unfavoriteDoctor(int userId, int doctorId) {
        Integer patientCardId = userInfoCardDao.searchIdByUserId(userId);
        if (patientCardId == null) {
            return false;
        }
        patientDoctorFavoriteDao.delete(patientCardId, doctorId);
        return true;
    }

    @Override
    public PageUtils searchFavoriteDoctorByPage(int userId, Map param) {
        Integer patientCardId = userInfoCardDao.searchIdByUserId(userId);
        int page = MapUtil.getInt(param, "page");
        int length = MapUtil.getInt(param, "length");
        if (patientCardId == null) {
            return new PageUtils(new ArrayList(), 0, page, length);
        }
        param.put("patientCardId", patientCardId);
        long count = patientDoctorFavoriteDao.searchFavoriteDoctorCount(patientCardId);
        ArrayList<HashMap> list = count > 0 ? patientDoctorFavoriteDao.searchFavoriteDoctorByPage(param) : new ArrayList<>();
        return new PageUtils(list, count, page, length);
    }
}
