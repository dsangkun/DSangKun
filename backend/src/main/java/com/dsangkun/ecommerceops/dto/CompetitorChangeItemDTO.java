package com.dsangkun.ecommerceops.dto;

import java.util.List;

public record CompetitorChangeItemDTO(
        String id,
        String name,
        String shop,
        String rank,
        List<String> changes
) {
}
