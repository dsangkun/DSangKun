package com.dsangkun.ecommerceops.dto;

import java.util.List;

public record DailyReportProductSheetResponse(
        String parentAsin,
        String parentProductName,
        String reportDate,
        String matchedBy,
        String confidence,
        List<String> aliases,
        DailyReportSheetDTO sheet,
        List<DailyReportSheetCandidateDTO> candidates
) {
}
