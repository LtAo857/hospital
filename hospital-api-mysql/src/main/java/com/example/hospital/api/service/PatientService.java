package com.example.hospital.api.service;

import com.example.hospital.api.db.pojo.PatientInfoCardEntity;

import java.util.ArrayList;
import java.util.HashMap;

public interface PatientService {
    public ArrayList<HashMap> searchAllPatient();

    public void insert(PatientInfoCardEntity entity);

    public HashMap searchUserInfoCard(int userId);
    public void update(PatientInfoCardEntity entity);

    public boolean hasUserInfoCard(int userId);
}
