package com.example.hospital.patient.wx.api.db.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class PatientDoctorFavoriteEntity implements Serializable {
    private Integer id;
    private Integer patientCardId;
    private Integer doctorId;
    private String createTime;
}
