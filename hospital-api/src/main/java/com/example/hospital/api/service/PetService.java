package com.example.hospital.api.service;

import com.example.hospital.api.common.PageUtils;

import java.util.ArrayList;
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

