package com.dsangkun.ecommerceops.service;

import com.dsangkun.ecommerceops.application.dashboard.DashboardAggregationService;
import com.dsangkun.ecommerceops.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkbenchService {

    private final DashboardAggregationService dashboardAggregationService;

    public WorkbenchOverviewDTO getOverview() {
        return dashboardAggregationService.getOverview();
    }

    public List<NewArrivalItemDTO> getNewArrivals() {
        return List.of(
                new NewArrivalItemDTO("A001", "竞品 A 新上架：便携咖啡机 Pro", "09:12", "厨房小家电", "竞品旗舰店A", "https://snapshot.example.com/product/A001"),
                new NewArrivalItemDTO("B014", "竞品 B 新上架：无叶挂脖风扇 Lite", "10:03", "季节电器", "竞品旗舰店B", "https://snapshot.example.com/product/B014"),
                new NewArrivalItemDTO("C102", "竞品 C 新上架：桌面空气循环扇 Mini", "10:48", "家居电器", "竞品旗舰店C", "https://snapshot.example.com/product/C102")
        );
    }

    public NewArrivalActionResponse handleNewArrivalAction(String id, String action) {
        return new NewArrivalActionResponse(id, action, "PROCESSED");
    }

    public List<CompetitorChangeItemDTO> getCompetitorChanges() {
        return List.of(
                new CompetitorChangeItemDTO("CX9", "咖啡机旗舰款 X9", "竞品旗舰店A", "12", List.of("价格：¥399 → ¥369", "活动：新增满300减30", "销量：日增 +126")),
                new CompetitorChangeItemDTO("ACS", "挂脖风扇 AirCool S", "竞品旗舰店B", "28", List.of("主图：已更新", "文案：卖点描述调整", "销量：日增 +84")),
                new CompetitorChangeItemDTO("PMX", "空气循环扇 Pro Max", "竞品旗舰店C", "7", List.of("价格：¥259 → ¥279", "活动：取消店铺券", "排名：11 → 7"))
        );
    }

    public List<ProductOperationItemDTO> getOperationData(String date) {
        LocalDate statDate = resolveDate(date);
        if (!LocalDate.of(2026, 3, 17).equals(statDate)) {
            return List.of();
        }

        return List.of(
                new ProductOperationItemDTO(
                        "K1",
                        "智能咖啡机 K1",
                        "K1",
                        "EXPERLAM",
                        "美国站",
                        "聚合调试数据",
                        "未分配",
                        "K1",
                        "blue",
                        "智能咖啡机 K1",
                        "$39.99",
                        null,
                        "K1",
                        "K1",
                        new ProductReviewInfoDTO("4.6", "860", "+5", "1", "评价摘要待接真实数据", "当前为页面结构验证数据，后续由真实接口数据替换。", statDate.toString(), "Amazon User", List.of()),
                        new ProductMetricBlockDTO("销售数据", List.of(
                                new MetricCompareItemDTO("当日数据", "860", 138, "today"),
                                new MetricCompareItemDTO("七日平均", "695", 112, "avg"),
                                new MetricCompareItemDTO("上周同日", "780", 124, "lastweek")
                        ), List.of(), "--", "Mock"),
                        new ProductMetricBlockDTO("流量数据", List.of(
                                new MetricCompareItemDTO("当日数据", "12k", 146, "today"),
                                new MetricCompareItemDTO("七日平均", "9.7k", 118, "avg"),
                                new MetricCompareItemDTO("上周同日", "10.8k", 130, "lastweek")
                        ), List.of(), "--", "Mock"),
                        new ProductAdsBlockDTO("SP广告", List.of(), List.of(), "Mock"),
                        new ProductAdsBlockDTO("SBV广告", List.of(), List.of(), "Mock")
                )
        );
    }

    public List<String> getOperationDates() {
        return List.of("2026-03-19", "2026-03-18", "2026-03-17");
    }

    private LocalDate resolveDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.of(2026, 3, 17);
        }
        return LocalDate.parse(date);
    }
}
