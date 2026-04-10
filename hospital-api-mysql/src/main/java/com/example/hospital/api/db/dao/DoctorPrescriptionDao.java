package com.example.hospital.api.db.dao;

import com.example.hospital.api.db.pojo.DoctorPrescriptionEntity;

import java.util.HashMap;

public interface DoctorPrescriptionDao {
    HashMap searchByRegistrationId(int registrationId);

    void upsert(DoctorPrescriptionEntity entity);
}
