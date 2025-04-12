package com.example.hospital.patient.wx.api.db.dao;

import com.example.hospital.patient.wx.api.common.PageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface DoctorDao {

//    public HashMap searchUserInfo(int userId);

    public HashMap searchDoctorInfoById(int id);
    public ArrayList<HashMap> searchDoctorInfo(Map param);

}




