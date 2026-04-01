package com.example.hospital.patient.wx.api.db.dao;

import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public interface PatientDoctorFavoriteDao {
    Integer searchIdByPatientCardIdAndDoctorId(@Param("patientCardId") int patientCardId, @Param("doctorId") int doctorId);

    int insert(@Param("patientCardId") int patientCardId, @Param("doctorId") int doctorId);

    int delete(@Param("patientCardId") int patientCardId, @Param("doctorId") int doctorId);

    long searchFavoriteDoctorCount(@Param("patientCardId") int patientCardId);

    ArrayList<HashMap> searchFavoriteDoctorByPage(Map param);
}
