package com.example.hospital.patient.wx.api.service;

import java.util.HashMap;
import java.util.Map;
import com.example.hospital.patient.wx.api.common.PageUtils;

public interface DoctorService {

    public HashMap searchDoctorInfoById(int id);

    public PageUtils searchDoctorInfo(Map param);
}

