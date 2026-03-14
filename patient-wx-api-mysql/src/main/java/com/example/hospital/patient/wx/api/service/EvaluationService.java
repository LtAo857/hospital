package com.example.hospital.patient.wx.api.service;

import com.example.hospital.patient.wx.api.common.PageUtils;

import java.util.HashMap;
import java.util.Map;

public interface EvaluationService {
    public void insert(Map param);
    public boolean hasEvaluated(Map param);
    public PageUtils searchEvaluationByPage(Map param);
    public HashMap searchDoctorEvaluation(Map param);
}
