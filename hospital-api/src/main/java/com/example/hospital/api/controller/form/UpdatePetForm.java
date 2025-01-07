package com.example.hospital.api.controller.form;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UpdatePetForm {
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id不能小于1")
    private Integer id;


    @NotBlank(message = "name不能为空")
    private String name;
    @NotNull(message = "master_id不能为空")
    private Integer master_id;

    @NotBlank(message = "sex不能为空")
    private String sex;
    @NotBlank(message = "hobby不能为空")
    private String hobby;
    @NotBlank(message = "birthday不能为空")
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
