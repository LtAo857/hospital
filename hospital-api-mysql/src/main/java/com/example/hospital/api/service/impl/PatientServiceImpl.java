package com.example.hospital.api.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.hospital.api.db.dao.PatientDao;
import com.example.hospital.api.db.dao.PetDao;
import com.example.hospital.api.db.pojo.PatientInfoCardEntity;
import com.example.hospital.api.service.PatientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class PatientServiceImpl implements PatientService {
    @Resource
    private PatientDao patientDao;
    @Override
    public ArrayList<HashMap> searchAllPatient() {
        return patientDao.searchAllPatient();
    }
    @Override
    @Transactional
    public void insert(PatientInfoCardEntity entity) {

        patientDao.insert(entity);
    }
    @Override
    public HashMap searchUserInfoCard(int userId) {
        HashMap map = patientDao.searchUserInfoCard(userId);
        if (map != null) {
            String medicalHistory = MapUtil.getStr(map, "medicalHistory");
            JSONArray array = JSONUtil.parseArray(medicalHistory);
            map.replace("medicalHistory", array);
        }
        return map;
    }

    @Override
    @Transactional
    public void update(PatientInfoCardEntity entity) {
        patientDao.update(entity);
    }

    @Override
    public boolean hasUserInfoCard(int userId) {
        Integer id = patientDao.hasUserInfoCard(userId);
        boolean bool = (id != null);
        return bool;
    }
}
