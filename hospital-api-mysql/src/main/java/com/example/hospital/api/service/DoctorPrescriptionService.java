package com.example.hospital.api.service;

import com.example.hospital.api.common.PageUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DoctorPrescriptionService {
    PageUtils searchRegistrationByPage(Map param);

    HashMap searchByRegistrationId(int registrationId, int userId, List<String> permissions);

    void save(Map param, int userId, List<String> permissions);
}
