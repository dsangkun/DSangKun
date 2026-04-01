package com.dsangkun.ecommerceops.service;

import com.dsangkun.ecommerceops.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkbenchService {

    public WorkbenchOverviewDTO getOverview() {
        return new WorkbenchOverviewDTO(8, 3, 3, 2);
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

    public List<ProductOperationItemDTO> getOperationData() {
        return List.of(
                new ProductOperationItemDTO(
                        "K1",
                        "智能咖啡机 K1",
                        new ProductMetricBlockDTO("销售数据", List.of(
                                new MetricCompareItemDTO("当日数据", "860", 138, "today"),
                                new MetricCompareItemDTO("七日平均", "695", 112, "avg"),
                                new MetricCompareItemDTO("上周同日", "780", 124, "lastweek")
                        )),
                        new ProductMetricBlockDTO("流量数据", List.of(
                                new MetricCompareItemDTO("当日数据", "12k", 146, "today"),
                                new MetricCompareItemDTO("七日平均", "9.7k", 118, "avg"),
                                new MetricCompareItemDTO("上周同日", "10.8k", 130, "lastweek")
                        )),
                        new ProductMetricBlockDTO("广告数据", List.of(
                                new MetricCompareItemDTO("当日数据", "3.4k", 120, "today"),
                                new MetricCompareItemDTO("七日平均", "2.8k", 96, "avg"),
                                new MetricCompareItemDTO("上周同日", "3.0k", 106, "lastweek")
                        ))
                ),
                new ProductOperationItemDTO(
                        "F2",
                        "便携风扇 F2",
                        new ProductMetricBlockDTO("销售数据", List.of(
                                new MetricCompareItemDTO("当日数据", "430", 104, "today"),
                                new MetricCompareItemDTO("七日平均", "388", 94, "avg"),
                                new MetricCompareItemDTO("上周同日", "462", 112, "lastweek")
                        )),
                        new ProductMetricBlockDTO("流量数据", List.of(
                                new MetricCompareItemDTO("当日数据", "8.6k", 124, "today"),
                                new MetricCompareItemDTO("七日平均", "7.8k", 112, "avg"),
                                new MetricCompareItemDTO("上周同日", "9.2k", 132, "lastweek")
                        )),
                        new ProductMetricBlockDTO("广告数据", List.of(
                                new MetricCompareItemDTO("当日数据", "2.1k", 90, "today"),
                                new MetricCompareItemDTO("七日平均", "2.4k", 102, "avg"),
                                new MetricCompareItemDTO("上周同日", "1.9k", 82, "lastweek")
                        ))
                )
        );
    }
}
