package com.dsangkun.ecommerceops.dto;

import java.util.List;

public record DailyReportSheetDTO(
        String sheetName,
        String sourceFile,
        String reportDate,
        List<List<String>> rows
) {
}
