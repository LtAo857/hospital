package com.example.hospital.api.db.pojo;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class PatientInfoCardEntity {
    private Integer id;
    private Integer userId;
    private String uuid;
    private String name;
    private String sex;
    private String tel;
    private LocalDate birthday;
    private String medicalHistory;
    private String insurance_type;
    private Byte exist_face_model;

}
