package com.example.hospital.patient.wx.api.db.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class PetEntity {
//    private Integer id;
    private String name;
    private Integer userId;
    private String sex;
    private String hobby;
    private String birthday;
    private String disease_history;
    private String breed;
    private String weight;
    private String image;
    private String vaccine;


}