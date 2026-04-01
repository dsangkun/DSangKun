package com.dsangkun.ecommerceops.dto;

import java.util.List;

public record ProductMetricBlockDTO(
        String title,
        List<MetricCompareItemDTO> compareList
) {
}
