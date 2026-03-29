package com.example.hospital.api.service;

import com.example.hospital.api.common.PageUtils;

import java.util.Map;

public interface EvaluationService {
    PageUtils searchByPage(Map param);
}
