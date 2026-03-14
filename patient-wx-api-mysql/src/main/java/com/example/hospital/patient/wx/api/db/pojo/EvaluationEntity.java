package com.example.hospital.patient.wx.api.db.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class EvaluationEntity {
    private Integer id;
    private Integer doctorId;
    private Integer patientCardId;
    private Integer registrationId;
    private Integer videoDiagnoseId;
    private Byte score;
    private String comment;
    private Date createTime;
}
