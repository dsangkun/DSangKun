package com.dsangkun.ecommerceops.dto;

import java.util.List;

public record ProductAdsBlockDTO(
        String title,
        List<MetricHighlightItemDTO> highlights,
        List<ProductAdActivityItemDTO> activityList,
        String sourceNote
) {
}
