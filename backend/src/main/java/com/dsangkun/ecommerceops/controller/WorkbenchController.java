package com.dsangkun.ecommerceops.controller;

import com.dsangkun.ecommerceops.common.ApiResponse;
import com.dsangkun.ecommerceops.dto.*;
import com.dsangkun.ecommerceops.service.WorkbenchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workbench")
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    public WorkbenchController(WorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
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
    public ApiResponse<List<ProductOperationItemDTO>> operationData() {
        return ApiResponse.ok(workbenchService.getOperationData());
    }
}
