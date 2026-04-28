package com.dsangkun.ecommerceops.dto;

public record ProductAdActivityItemDTO(
        String source,
        String campaignName,
        String campaignUrl,
        String impressions,
        String clicks,
        String ctr,
        String cpc,
        String cost,
        String sales,
        String acos,
        String orders,
        String cvr
) {
}
