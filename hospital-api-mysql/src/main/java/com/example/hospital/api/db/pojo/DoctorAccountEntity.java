package com.example.hospital.api.db.pojo;

import lombok.Data;

@Data
public class DoctorAccountEntity {
    private Integer id;
    private String name;
    private String username;
    private String password;
    private Integer deptId;
    private Integer refId;

    private String sex;

    private String tel;

    private String email;
    private String job;

    private Byte status;
    private String createTime;

}
