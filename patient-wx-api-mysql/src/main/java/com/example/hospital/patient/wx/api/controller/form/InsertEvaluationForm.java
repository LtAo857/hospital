package com.example.hospital.patient.wx.api.controller.form;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class InsertEvaluationForm {
    private int userId;

    @NotNull(message = "doctorId不能为空")
    private Integer doctorId;

    @NotNull(message = "score不能为空")
    @Min(value = 1, message = "score不能小于1")
    @Max(value = 5, message = "score不能大于5")
    private Byte score;

    private String comment;

    private Integer registrationId;

    private Integer videoDiagnoseId;
}
