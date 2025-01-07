package com.example.hospital.patient.wx.api.db.pojo;

import lombok.Data;

@Data
public class FaceAuthEntity {
    private Integer id;
    private Integer patientCardId;
    private String date;
}