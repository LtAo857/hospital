package com.example.hospital.api.db.dao;

import com.example.hospital.api.db.pojo.DoctorEntity;
import com.example.hospital.api.db.pojo.IllnessEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface IllnessDao {
    public ArrayList<HashMap> searchIllnessInfo(Map param);
    public void insert(IllnessEntity entity);
    public void update(Map param);
    public HashMap searchById(int id);
    public void deleteByIds(Integer[]ids);
}
