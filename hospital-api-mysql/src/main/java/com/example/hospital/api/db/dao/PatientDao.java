package com.example.hospital.api.db.dao;

import com.example.hospital.api.db.pojo.PatientInfoCardEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface PatientDao {
    public ArrayList<HashMap> searchAllPatient();

    public String searchUserTel(int userId);

    public void insert(PatientInfoCardEntity entity);

    public HashMap searchUserInfoCard(int userId);
    public int update(PatientInfoCardEntity entity);

    public Integer hasUserInfoCard(int userId);

    public Boolean searchExistFaceModel(int userId);

    public void updateExistFaceModel(Map param);
    public Integer searchIdByUserId(int userId);
}
