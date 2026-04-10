package com.example.hospital.api.db.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface MedicalRegistrationDao {
    long searchPrescriptionRegistrationCount(Map param);

    ArrayList<HashMap> searchPrescriptionRegistrationByPage(Map param);

    HashMap searchPrescriptionRegistrationBaseInfo(int registrationId);
}
