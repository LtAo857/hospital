package com.example.hospital.patient.wx.api.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.hospital.patient.wx.api.db.dao.DoctorPrescriptionDao;
import com.example.hospital.patient.wx.api.service.PrescriptionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {
    @Resource
    private DoctorPrescriptionDao doctorPrescriptionDao;

    @Override
    public HashMap searchPrescriptionByRegistrationId(Map param) {
        HashMap map = doctorPrescriptionDao.searchPrescriptionByRegistrationId(param);
        if (map == null) {
            return null;
        }

        String patientBirthday = MapUtil.getStr(map, "patientBirthday");
        if (patientBirthday != null && patientBirthday.length() > 0) {
            map.put("patientAge", DateUtil.ageOfNow(patientBirthday));
        }
        else {
            map.put("patientAge", null);
        }

        String rp = MapUtil.getStr(map, "rp");
        JSONArray rpList;
        if (rp != null && rp.trim().length() > 0) {
            rpList = JSONUtil.parseArray(rp);
        }
        else {
            rpList = new JSONArray();
        }
        map.put("rpList", rpList);
        return map;
    }
}
