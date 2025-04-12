package com.example.hospital.patient.wx.api.db.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class DoctorWorkPlanEntity {
    private Integer id;
    private Integer doctorId;
    private Integer deptSubId;
    private Date date;
    private Short maximum;
    private Short num;

}