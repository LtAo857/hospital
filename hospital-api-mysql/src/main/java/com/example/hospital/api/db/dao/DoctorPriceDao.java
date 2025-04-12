package com.example.hospital.api.db.dao;

import com.example.hospital.api.db.pojo.DoctorEntity;
import com.example.hospital.api.db.pojo.DoctorPriceEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface DoctorPriceDao {
    public HashMap searchByDoctorId(int doctorId);
    public ArrayList<HashMap> searchByPage(Map param);
    public void update(Map param);
    public void insert(DoctorPriceEntity entity);
}




