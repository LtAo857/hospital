package com.example.hospital.api.controller.form;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class UpdatePriceForm {
//    @NotBlank(message = "symptom不能为空")
//    private Integer id;

    @NotNull(message = "doctorID不能为空")
    private Integer doctorId;


//    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @NotNull(message = "price_1不能为空")
    private Integer priceMenZhen;


//    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @NotNull(message = "price_2不能为空")
    private Integer priceShiPing;
}
