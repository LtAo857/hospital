package com.example.hospital.api.controller.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class InsertPriceForm {
    @NotNull(message = "doctorId不能为空")
    private Integer doctorId;

//    @NotBlank(message = "level不能为空")
    private String level;

    @NotNull(message = "price_1不能为空")
    private BigDecimal price_1;

    @NotNull(message = "price_2不能为空")
    private BigDecimal price_2;




}
