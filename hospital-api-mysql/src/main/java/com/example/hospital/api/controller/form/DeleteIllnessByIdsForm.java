package com.example.hospital.api.controller.form;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class DeleteIllnessByIdsForm {
    @NotEmpty(message = "ids不能为空")
    private Integer[] ids;
}
