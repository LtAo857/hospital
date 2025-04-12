package com.example.hospital.api.db.pojo;

import lombok.Data;

@Data
public class IllnessEntity {
    private Integer id;
    private String name;
    private String cause;
    private String symptom;
    private String method;
    private String description;


}