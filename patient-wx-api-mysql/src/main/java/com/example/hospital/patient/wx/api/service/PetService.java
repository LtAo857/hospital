package com.example.hospital.patient.wx.api.service;

import com.example.hospital.patient.wx.api.common.PageUtils;

import java.util.HashMap;
import java.util.Map;

public interface PetService {

    public PageUtils searchByPage(Map param);
    public PageUtils searchByPageAndId(Map param);
    public void insert(Map param);
    public void update(Map param);
    public HashMap searchById(int id);
    public void deleteByIds(Integer[]ids);
}

