package com.example.hospital.api.service;

import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.db.pojo.DoctorPriceEntity;

import java.util.HashMap;
import java.util.Map;

public interface DoctorPriceService {
    public HashMap searchByDoctorId(int doctorId);
    public void update(Map param);
    public void insert(Map param);
    PageUtils searchByPage(Map param);
}
