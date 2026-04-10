package com.example.hospital.api.controller.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class SaveDoctorPrescriptionForm {
    @NotNull(message = "registrationId不能为空")
    @Min(value = 1, message = "registrationId不能小于1")
    private Integer registrationId;

    @NotBlank(message = "diagnosis不能为空")
    @Length(max = 2000, message = "diagnosis内容过长")
    private String diagnosis;

    @NotBlank(message = "rp不能为空")
    @Length(max = 20000, message = "rp内容过长")
    private String rp;
}
