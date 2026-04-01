package com.dsangkun.ecommerceops.dto;

public record NewArrivalItemDTO(
        String id,
        String title,
        String time,
        String category,
        String shop,
        String snapshotUrl
) {
}
