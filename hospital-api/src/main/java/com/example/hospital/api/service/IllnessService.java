package com.example.hospital.api.service;

import com.example.hospital.api.common.PageUtils;
import com.example.hospital.api.db.pojo.IllnessEntity;

import java.util.HashMap;
import java.util.Map;

public interface IllnessService {

    public PageUtils searchIllnessInfo(Map param);
    public void insert(Map param);
    public void update(Map param);
    public HashMap searchById(int id);
    public void deleteByIds(Integer[]ids);
}

