package com.dsangkun.ecommerceops.dto;

import jakarta.validation.constraints.NotBlank;

public record NewArrivalActionRequest(
        @NotBlank(message = "action不能为空")
        String action
) {
}
