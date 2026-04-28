package com.dsangkun.ecommerceops.dto;

import java.util.List;

public record ProductReviewInfoDTO(
        String score,
        String reviewCount,
        String newReviewCount,
        String badReviewCount,
        String latestTitle,
        String latestContent,
        String latestDate,
        String latestAuthor,
        List<ProductReviewCommentDTO> recentComments
) {
}
