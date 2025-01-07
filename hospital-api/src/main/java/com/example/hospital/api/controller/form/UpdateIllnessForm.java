package com.example.hospital.api.controller.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.*;

@Data
public class UpdateIllnessForm {
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id不能小于1")
    private Integer id;

    @NotBlank(message = "name不能为空")
    private String name;

    @NotBlank(message = "cause不能为空")
    private String cause;

    @NotBlank(message = "symptom不能为空")
    private String symptom;

    @NotBlank(message = "method不能为空")
    private String method;

    @NotBlank(message = "description不能为空")
    private String description;
}
