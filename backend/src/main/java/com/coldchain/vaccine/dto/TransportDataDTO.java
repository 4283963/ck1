package com.coldchain.vaccine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransportDataDTO {

    @NotBlank(message = "车牌号不能为空")
    private String plateNumber;

    @NotNull(message = "温度不能为空")
    private BigDecimal temperature;

    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;

    private String locationAddress;
}
