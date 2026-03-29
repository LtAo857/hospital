package com.example.hospital.api.controller.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class UpdateDoctorAccountForm {
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id内容不正确")
    private Integer id;

    @NotBlank(message = "username不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{5,50}$", message = "username内容不正确")
    private String username;

    @Length(max = 20, message = "password内容不正确")
    @Pattern(regexp = "^$|^[a-zA-Z0-9]{6,20}$", message = "password内容不正确")
    private String password;
}
