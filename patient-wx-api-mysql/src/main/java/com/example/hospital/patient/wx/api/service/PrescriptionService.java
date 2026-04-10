package com.example.hospital.patient.wx.api.service;

import java.util.HashMap;
import java.util.Map;

public interface PrescriptionService {
    HashMap searchPrescriptionByRegistrationId(Map param);
}
