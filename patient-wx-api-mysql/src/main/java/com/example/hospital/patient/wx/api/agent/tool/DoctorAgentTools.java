package com.example.hospital.patient.wx.api.agent.tool;

import com.example.hospital.patient.wx.api.common.PageUtils;
import com.example.hospital.patient.wx.api.service.DoctorService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class DoctorAgentTools {
    @Resource
    private DoctorService doctorService;

    public HashMap getDoctorDetail(int doctorId, Integer userId) {
        return doctorService.searchDoctorInfoById(doctorId, userId);
    }

    public PageUtils searchDoctors(int page, int length) {
        Map<String, Object> param = new HashMap<>();
        param.put("page", page);
        param.put("length", length);
        param.put("start", (page - 1) * length);
        return doctorService.searchDoctorInfo(param);
    }
}
