package com.example.hospital.patient.wx.api.controller.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ReadMessageForm {
    @NotNull(message = "id不能为空")
    private Integer id;
}
