package com.dsangkun.ecommerceops.dto;

import java.util.List;

public record DailyReportSheetCandidateDTO(
        String sheetName,
        int score,
        List<String> reasons
) {
}
