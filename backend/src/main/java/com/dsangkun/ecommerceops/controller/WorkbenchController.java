package com.dsangkun.ecommerceops.controller;

import com.dsangkun.ecommerceops.common.ApiResponse;
import com.dsangkun.ecommerceops.dto.*;
import com.dsangkun.ecommerceops.service.DailyReportService;
import com.dsangkun.ecommerceops.service.WorkbenchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workbench")
public class WorkbenchController {

    private final WorkbenchService workbenchService;
    private final DailyReportService dailyReportService;

    public WorkbenchController(WorkbenchService workbenchService, DailyReportService dailyReportService) {
        this.workbenchService = workbenchService;
        this.dailyReportService = dailyReportService;
    }

    @GetMapping("/overview")
    public ApiResponse<WorkbenchOverviewDTO> overview() {
        return ApiResponse.ok(workbenchService.getOverview());
    }

    @GetMapping("/new-arrivals")
    public ApiResponse<List<NewArrivalItemDTO>> newArrivals() {
        return ApiResponse.ok(workbenchService.getNewArrivals());
    }

    @PostMapping("/new-arrivals/{id}/action")
    public ApiResponse<NewArrivalActionResponse> handleNewArrival(
            @PathVariable String id,
            @Valid @RequestBody NewArrivalActionRequest request
    ) {
        return ApiResponse.ok("处理成功", workbenchService.handleNewArrivalAction(id, request.action()));
    }

    @GetMapping("/competitor-changes")
    public ApiResponse<List<CompetitorChangeItemDTO>> competitorChanges() {
        return ApiResponse.ok(workbenchService.getCompetitorChanges());
    }

    @GetMapping("/operation-data")
    public ApiResponse<List<ProductOperationItemDTO>> operationData(
            @RequestParam(required = false) String date
    ) {
        return ApiResponse.ok(workbenchService.getOperationData(date));
    }

    @GetMapping("/operation-dates")
    public ApiResponse<List<String>> operationDates() {
        return ApiResponse.ok(workbenchService.getOperationDates());
    }

    @GetMapping("/daily-report/latest-sheet")
    public ApiResponse<DailyReportProductSheetResponse> latestDailyReportSheet(
            @RequestParam String unionId,
            @RequestParam String parentAsin,
            @RequestParam String parentProductName,
            @RequestParam(required = false) List<String> childProductNames
    ) {
        return ApiResponse.ok(dailyReportService.getLatestProductSheet(unionId, parentAsin, parentProductName, childProductNames));
    }

    @GetMapping("/daily-report/latest-sheet-debug")
    public ApiResponse<java.util.Map<String, Object>> latestDailyReportSheetDebug(
            @RequestParam String unionId,
            @RequestParam String parentAsin,
            @RequestParam String parentProductName,
            @RequestParam(required = false) List<String> childProductNames,
            @RequestParam(required = false) String latestReportFileId
    ) {
        return ApiResponse.ok(dailyReportService.getLatestProductSheetDebug(
                unionId,
                parentAsin,
                parentProductName,
                childProductNames,
                latestReportFileId
        ));
    }
}
