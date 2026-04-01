package com.example.hospital.patient.wx.api.service;

import java.util.HashMap;
import java.util.Map;
import com.example.hospital.patient.wx.api.common.PageUtils;

public interface DoctorService {

    HashMap searchDoctorInfoById(int id, Integer userId);

    PageUtils searchDoctorInfo(Map param);

    boolean favoriteDoctor(int userId, int doctorId);

    boolean unfavoriteDoctor(int userId, int doctorId);

    PageUtils searchFavoriteDoctorByPage(int userId, Map param);
}

