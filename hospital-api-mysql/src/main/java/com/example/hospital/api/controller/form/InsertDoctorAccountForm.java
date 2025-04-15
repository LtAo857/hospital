package com.example.hospital.api.controller.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.*;

@Data
public class InsertDoctorAccountForm {
    @NotBlank(message = "username不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9f5a]{2,20}$", message = "username内容不正确")
    private String name;

    @NotBlank(message = "username不能为空")
    private String username;
    @NotBlank(message = "password不能为空")
    private String password;



    @NotBlank(message = "sex不能为空")
    @Pattern(regexp = "^男$|^女$", message = "sex内容不正确")
    private String sex;



    @NotBlank(message = "tel不能为空")
    @Pattern(regexp = "^1[1-9][0-9]{9}$", message = "tel内容不正确")
    private String tel;


    @NotBlank(message = "email不能为空")
    @Email
    @Length(max = 200, message = "email内容不正确")
    private String email;

    private String job="医生";
    private Integer dept_id;
    private Integer ref_id=1;



    @NotNull(message = "status不能为空")
    @Range(min = 1, max = 3, message = "status不能为空")
    private Byte status;



}
