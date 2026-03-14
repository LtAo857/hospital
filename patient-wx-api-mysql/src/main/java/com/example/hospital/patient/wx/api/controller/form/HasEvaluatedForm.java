package com.example.hospital.patient.wx.api.controller.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class HasEvaluatedForm {
    private int userId;

    private Integer registrationId;

    private Integer videoDiagnoseId;
}
