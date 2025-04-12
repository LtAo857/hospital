package com.example.hospital.patient.wx.api.db.dao;

import com.example.hospital.patient.wx.api.db.pojo.PetEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface PetDao {
    public ArrayList<HashMap> searchByPage(Map param);
    public ArrayList<HashMap> searchByPageAndId(Map param);
    public void insert(PetEntity entity);
    public void update(Map param);
    public HashMap searchById(int id);
    public void deleteByIds(Integer[]ids);
}
