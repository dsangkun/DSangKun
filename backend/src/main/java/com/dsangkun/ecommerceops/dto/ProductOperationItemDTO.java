package com.dsangkun.ecommerceops.dto;

public record ProductOperationItemDTO(
        String id,
        String productName,
        ProductMetricBlockDTO sales,
        ProductMetricBlockDTO traffic,
        ProductMetricBlockDTO ads
) {
}
