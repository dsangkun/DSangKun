package com.dsangkun.ecommerceops.dto;

public record WorkbenchOverviewDTO(
        int totalTodoCount,
        int newArrivalCount,
        int competitorChangeCount,
        int operationProductCount
) {
}
