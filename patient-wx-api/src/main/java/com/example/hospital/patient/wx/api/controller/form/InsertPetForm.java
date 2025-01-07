package com.example.hospital.patient.wx.api.controller.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class InsertPetForm {
//    @NotBlank(message = "name不能为空")
    private String name;
    private Integer userId;

    @NotBlank(message = "sex不能为空")
    private String sex;

    @NotBlank(message = "hobby不能为空")
    private String hobby;

    @NotNull(message = "birthday不能为空")
    private String birthday;

    @NotBlank(message = "disease_history不能为空")
    private String disease_history;

    @NotBlank(message = "breed不能为空")
    private String breed;

    @NotBlank(message = "weight不能为空")
    private String weight;

    @NotBlank(message = "image不能为空")
    private String image;

    @NotBlank(message = "vaccine不能为空")
    private String vaccine;



}
