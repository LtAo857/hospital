package com.example.hospital.api.controller.form;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class SearchDoctorPrescriptionByRegistrationIdForm {
    @NotNull(message = "registrationId不能为空")
    @Min(value = 1, message = "registrationId不能小于1")
    private Integer registrationId;
}
