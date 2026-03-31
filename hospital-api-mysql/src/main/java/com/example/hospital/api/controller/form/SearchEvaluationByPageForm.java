package com.example.hospital.api.controller.form;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class SearchEvaluationByPageForm {
    @Min(value = 1, message = "doctorId内容不正确")
    private Integer doctorId;

    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9]{1,20}$", message = "doctorName内容不正确")
    private String doctorName;

    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z0-9]{1,20}$", message = "patientName内容不正确")
    private String patientName;

    @Range(min = 1, max = 5, message = "score内容不正确")
    private Integer score;

    @Pattern(regexp = "^门诊$|^视频问诊$", message = "source内容不正确")
    private String source;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "startDate内容不正确")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "endDate内容不正确")
    private String endDate;

    @NotNull(message = "page不能为空")
    @Min(value = 1, message = "page不能小于1")
    private Integer page;

    @NotNull(message = "length不能为空")
    @Range(min = 10, max = 50, message = "length内容不正确")
    private Integer length;
}
