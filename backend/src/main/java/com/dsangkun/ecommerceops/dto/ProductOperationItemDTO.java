package com.dsangkun.ecommerceops.dto;

public record ProductOperationItemDTO(
        String id,
        String productName,
        String productCode,
        String shopName,
        String siteName,
        String productTag,
        String ownerName,
        String coverText,
        String coverTone,
        String listingTitle,
        String listingPrice,
        String productImageUrl,
        String childAsin,
        String childSku,
        ProductReviewInfoDTO review,
        ProductMetricBlockDTO sales,
        ProductMetricBlockDTO traffic,
        ProductAdsBlockDTO spAds,
        ProductAdsBlockDTO sbvAds
) {
}
