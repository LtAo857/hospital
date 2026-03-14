package com.example.hospital.patient.wx.api.db.dao;

import com.example.hospital.patient.wx.api.db.pojo.EvaluationEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface EvaluationDao {
    public int insert(EvaluationEntity entity);
    public Integer hasEvaluated(Map param);
    public long searchEvaluationCount(Map param);
    public ArrayList<HashMap> searchEvaluationByPage(Map param);
    public long searchDoctorEvaluationCount(int doctorId);
    public ArrayList<HashMap> searchDoctorEvaluationByPage(Map param);
    public HashMap searchDoctorAvgScore(int doctorId);
}
