package com.dsangkun.ecommerceops.service;

import com.dsangkun.ecommerceops.dto.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkbenchService {

    private final JdbcTemplate jdbcTemplate;

    public WorkbenchService(@Nullable JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public WorkbenchOverviewDTO getOverview() {
        int newArrivalCount = getNewArrivals().size();
        int competitorChangeCount = getCompetitorChanges().size();
        int operationProductCount = getLatestOperationProductCount();
        return new WorkbenchOverviewDTO(
                newArrivalCount + competitorChangeCount + operationProductCount,
                newArrivalCount,
                competitorChangeCount,
                operationProductCount
        );
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

        try {
            List<ProductOperationItemDTO> databaseItems = getOperationDataFromDb(statDate);
            if (!databaseItems.isEmpty()) {
                return databaseItems;
            }
        } catch (Exception ignored) {
            // 当前阶段数据库不可用时，仍回退 mock，避免前端直接空白
        }

        return getOperationDataMock(statDate);
    }

    public List<String> getOperationDates() {
        if (jdbcTemplate == null) {
            return List.of("2026-03-19", "2026-03-18", "2026-03-17");
        }

        try {
            List<String> dates = jdbcTemplate.queryForList(
                    """
                    SELECT DISTINCT DATE_FORMAT(stat_date, '%Y-%m-%d') AS stat_date
                      FROM m3_product_daily_summary
                     ORDER BY stat_date DESC
                    """,
                    String.class
            );
            if (dates != null && !dates.isEmpty()) {
                return dates;
            }
        } catch (Exception ignored) {
            // 当前阶段数据库不可用时，仍回退默认日期，避免前端直接空白
        }

        return List.of("2026-03-19", "2026-03-18", "2026-03-17");
    }

    private int getLatestOperationProductCount() {
        if (jdbcTemplate == null) {
            return 2;
        }

        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(DISTINCT product_id)
                      FROM m3_product_daily_summary
                     WHERE stat_date = (
                           SELECT MAX(stat_date)
                             FROM m3_product_daily_summary
                     )
                    """,
                    Integer.class
            );
            if (count != null && count > 0) {
                return count;
            }
        } catch (Exception ignored) {
            // 当前阶段数据库不可用时，仍回退默认值
        }

        return 2;
    }

    private LocalDate resolveDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.of(2026, 3, 17);
        }
        return LocalDate.parse(date);
    }

    private List<ProductOperationItemDTO> getOperationDataFromDb(LocalDate statDate) {
        if (jdbcTemplate == null) {
            return List.of();
        }

        String summarySql = """
                SELECT p.id,
                       p.product_code,
                       p.product_name,
                       p.owner_name,
                       p.shop_name,
                       p.primary_asin,
                       s.stat_date,
                       s.rating_avg,
                       s.review_count_total,
                       s.review_count_new,
                       s.sales_amount,
                       s.units_ordered,
                       s.order_item_count,
                       s.sessions_total,
                       s.page_views_total,
                       s.listing_conversion_rate,
                       s.sp_impressions,
                       s.sp_clicks,
                       s.sp_spend,
                       s.sp_sales,
                       s.sp_orders,
                       s.sbv_impressions,
                       s.sbv_clicks,
                       s.sbv_spend,
                       s.sbv_sales,
                       s.sbv_orders,
                       s.ad_impressions,
                       s.ad_clicks,
                       s.ad_spend,
                       s.ad_sales,
                       s.ad_orders,
                       s.ad_acos,
                       s.ad_cvr
                  FROM m3_product_daily_summary s
                  JOIN m3_product p ON p.id = s.product_id
                 WHERE s.stat_date = ?
                 ORDER BY s.order_item_count DESC, s.sales_amount DESC, p.id ASC
                """;

        List<Map<String, Object>> summaryRows = jdbcTemplate.queryForList(summarySql, Date.valueOf(statDate));
        if (summaryRows.isEmpty()) {
            return List.of();
        }

        String spSql = """
                SELECT product_id,
                       campaign_name,
                       impressions,
                       clicks,
                       ctr,
                       cpc,
                       spend,
                       ad_sales_7d,
                       acos,
                       ad_orders_7d,
                       cvr_7d
                  FROM m3_sp_campaign_daily
                 WHERE stat_date = ?
                   AND product_id IS NOT NULL
                 ORDER BY product_id ASC, clicks DESC, spend DESC, id ASC
                """;
        List<Map<String, Object>> spRows = jdbcTemplate.queryForList(spSql, Date.valueOf(statDate));
        Map<Long, List<Map<String, Object>>> spGrouped = spRows.stream()
                .collect(Collectors.groupingBy(row -> ((Number) row.get("product_id")).longValue()));

        String sbvSql = """
                SELECT product_id,
                       campaign_name,
                       impressions,
                       clicks,
                       ctr,
                       cpc,
                       spend,
                       ad_sales,
                       acos,
                       acos_raw,
                       ad_orders,
                       cvr,
                       cvr_raw,
                       is_summary_row
                  FROM m3_sbv_campaign_daily
                 WHERE stat_date = ?
                   AND product_id IS NOT NULL
                 ORDER BY product_id ASC, is_summary_row ASC, clicks DESC, spend DESC, id ASC
                """;
        List<Map<String, Object>> sbvRows = jdbcTemplate.queryForList(sbvSql, Date.valueOf(statDate));
        Map<Long, List<Map<String, Object>>> sbvGrouped = sbvRows.stream()
                .collect(Collectors.groupingBy(row -> ((Number) row.get("product_id")).longValue()));

        Set<Long> productIds = summaryRows.stream()
                .map(row -> ((Number) row.get("id")).longValue())
                .collect(Collectors.toSet());
        Map<Long, List<Map<String, Object>>> historyGrouped = fetchSummaryHistory(productIds, statDate.minusDays(7), statDate.minusDays(1));

        List<ProductOperationItemDTO> result = new ArrayList<>();
        int index = 1;
        for (Map<String, Object> row : summaryRows) {
            Long productId = ((Number) row.get("id")).longValue();
            String productCode = asString(row.get("product_code"));
            String productName = asString(row.get("product_name"));
            String ownerName = asString(row.get("owner_name"));
            String childAsin = asString(row.get("primary_asin"));
            String shopName = defaultIfBlank(asString(row.get("shop_name")), "EXPERLAM");

            List<Map<String, Object>> spActivities = spGrouped.getOrDefault(productId, List.of());
            List<Map<String, Object>> sbvActivities = sbvGrouped.getOrDefault(productId, List.of())
                    .stream()
                    .filter(item -> asInt(item.get("is_summary_row")) == 0)
                    .toList();

            List<Map<String, Object>> historyRows = historyGrouped.getOrDefault(productId, List.of());

            ProductMetricBlockDTO sales = new ProductMetricBlockDTO(
                    "销售数据",
                    buildCompareListFromHistory(
                            asInt(row.get("order_item_count")),
                            historyRows,
                            statDate,
                            "order_item_count",
                            "单"
                    ),
                    List.of(
                            new MetricHighlightItemDTO("销量", formatInt(asInt(row.get("units_ordered"))), "当日销量", "good"),
                            new MetricHighlightItemDTO("销售额", formatCurrency(row.get("sales_amount")), "当日销售额", "good"),
                            new MetricHighlightItemDTO("订单数", formatInt(asInt(row.get("order_item_count"))), "当日订单数", "neutral"),
                            new MetricHighlightItemDTO("转化率", formatPercent(row.get("listing_conversion_rate")), "Listing 转化率", "neutral")
                    ),
                    formatInt(asInt(row.get("order_item_count"))),
                    "近7日订单对比"
            );

            ProductMetricBlockDTO traffic = new ProductMetricBlockDTO(
                    "流量数据",
                    buildCompareListFromHistory(
                            asInt(row.get("sessions_total")),
                            historyRows,
                            statDate,
                            "sessions_total",
                            "次"
                    ),
                    List.of(
                            new MetricHighlightItemDTO("总流量", formatInt(asInt(row.get("sessions_total"))), "当日会话数", "good"),
                            new MetricHighlightItemDTO("页面浏览", formatInt(asInt(row.get("page_views_total"))), "当日页面浏览量", "neutral"),
                            new MetricHighlightItemDTO("SP流量", formatInt(asInt(row.get("sp_clicks"))), "SP 点击", "neutral"),
                            new MetricHighlightItemDTO("SBV流量", formatInt(asInt(row.get("sbv_clicks"))), "SBV 点击", "neutral")
                    ),
                    formatInt(asInt(row.get("sessions_total"))),
                    "近7日流量对比"
            );

            ProductAdsBlockDTO spAds = new ProductAdsBlockDTO(
                    "SP广告",
                    List.of(
                            new MetricHighlightItemDTO("曝光量", formatInt(asInt(row.get("sp_impressions"))), "SP 汇总曝光", "neutral"),
                            new MetricHighlightItemDTO("点击量", formatInt(asInt(row.get("sp_clicks"))), "SP 汇总点击", "neutral"),
                            new MetricHighlightItemDTO("CTR", formatPercent(safeDivide(asBigDecimal(row.get("sp_clicks")), asBigDecimal(row.get("sp_impressions")))), "SP 点击率", "neutral"),
                            new MetricHighlightItemDTO("CPC", formatCurrency(safeDivide(asBigDecimal(row.get("sp_spend")), asBigDecimal(row.get("sp_clicks")))), "SP 平均点击花费", "neutral"),
                            new MetricHighlightItemDTO("花费", formatCurrency(row.get("sp_spend")), "SP 总花费", "neutral"),
                            new MetricHighlightItemDTO("总销售额", formatCurrency(row.get("sp_sales")), "SP 总销售额", "good"),
                            new MetricHighlightItemDTO("ACOS", formatPercent(safeDivide(asBigDecimal(row.get("sp_spend")), asBigDecimal(row.get("sp_sales")))), "SP 整体 ACOS", "neutral"),
                            new MetricHighlightItemDTO("总订单数", formatInt(asInt(row.get("sp_orders"))), "SP 总订单", "neutral"),
                            new MetricHighlightItemDTO("CVR", formatPercent(safeDivide(asBigDecimal(row.get("sp_orders")), asBigDecimal(row.get("sp_clicks")))), "SP 转化率", "neutral")
                    ),
                    buildSpActivityList(spActivities),
                    "SP广告：按广告ASIN筛选该产品的 SP 活动数据。"
            );

            ProductAdsBlockDTO sbvAds = new ProductAdsBlockDTO(
                    "SBV广告",
                    List.of(
                            new MetricHighlightItemDTO("曝光量", formatInt(asInt(row.get("sbv_impressions"))), "SBV 汇总曝光", "neutral"),
                            new MetricHighlightItemDTO("点击量", formatInt(asInt(row.get("sbv_clicks"))), "SBV 汇总点击", "neutral"),
                            new MetricHighlightItemDTO("CTR", formatPercent(safeDivide(asBigDecimal(row.get("sbv_clicks")), asBigDecimal(row.get("sbv_impressions")))), "SBV 点击率", "neutral"),
                            new MetricHighlightItemDTO("CPC", formatCurrency(safeDivide(asBigDecimal(row.get("sbv_spend")), asBigDecimal(row.get("sbv_clicks")))), "SBV 平均点击花费", "neutral"),
                            new MetricHighlightItemDTO("花费", formatCurrency(row.get("sbv_spend")), "SBV 总花费", "neutral"),
                            new MetricHighlightItemDTO("总销售额", formatCurrency(row.get("sbv_sales")), "SBV 总销售额", "good"),
                            new MetricHighlightItemDTO("ACOS", formatPercent(safeDivide(asBigDecimal(row.get("sbv_spend")), asBigDecimal(row.get("sbv_sales")))), "SBV 整体 ACOS", "neutral"),
                            new MetricHighlightItemDTO("总订单数", formatInt(asInt(row.get("sbv_orders"))), "SBV 总订单", "neutral"),
                            new MetricHighlightItemDTO("CVR", formatPercent(safeDivide(asBigDecimal(row.get("sbv_orders")), asBigDecimal(row.get("sbv_clicks")))), "SBV 转化率", "neutral")
                    ),
                    buildSbvActivityList(sbvActivities),
                    "SBV广告：读取该产品 SBV 单表，剔除首行汇总后展示活动明细。"
            );

            ProductReviewInfoDTO review = new ProductReviewInfoDTO(
                    formatDecimal(row.get("rating_avg"), 1, "4.6"),
                    formatInt(asInt(row.get("review_count_total"))),
                    "+" + formatInt(asInt(row.get("review_count_new"))),
                    "0",
                    "评论数据待补充",
                    "当前阶段已接入当日汇总数据，评论明细后续补齐。",
                    statDate.toString(),
                    shopName,
                    List.of(
                            new ProductReviewCommentDTO(shopName, "当前为数据库回查结果，后续补充最新评论明细。", statDate.toString())
                    )
            );

            result.add(new ProductOperationItemDTO(
                    "P" + String.format(Locale.ROOT, "%03d", index++),
                    productName,
                    productCode,
                    shopName,
                    "美国站",
                    "数据库数据",
                    defaultIfBlank(ownerName, "未分配"),
                    buildCoverText(productName),
                    pickCoverTone(productId),
                    productName,
                    null,
                    null,
                    childAsin,
                    productCode,
                    review,
                    sales,
                    traffic,
                    spAds,
                    sbvAds
            ));
        }

        return result;
    }

    private List<ProductAdActivityItemDTO> buildSpActivityList(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> new ProductAdActivityItemDTO(
                        "SP",
                        asString(row.get("campaign_name")),
                        null,
                        formatInt(asInt(row.get("impressions"))),
                        formatInt(asInt(row.get("clicks"))),
                        formatPercent(row.get("ctr")),
                        formatCurrency(row.get("cpc")),
                        formatCurrency(row.get("spend")),
                        formatCurrency(row.get("ad_sales_7d")),
                        formatPercent(row.get("acos")),
                        formatInt(asInt(row.get("ad_orders_7d"))),
                        formatPercent(row.get("cvr_7d"))
                ))
                .toList();
    }

    private List<ProductAdActivityItemDTO> buildSbvActivityList(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> deduped = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String campaignName = defaultIfBlank(asString(row.get("campaign_name")), "-");
            String adGroupName = defaultIfBlank(asString(row.get("ad_group_name")), "-");
            String promotedAsin = defaultIfBlank(asString(row.get("promoted_asin")), "-");
            String key = campaignName + "|" + adGroupName + "|" + promotedAsin;
            deduped.putIfAbsent(key, row);
        }

        return deduped.values().stream()
                .map(row -> new ProductAdActivityItemDTO(
                        "SBV",
                        asString(row.get("campaign_name")),
                        null,
                        formatInt(asInt(row.get("impressions"))),
                        formatInt(asInt(row.get("clicks"))),
                        formatPercent(row.get("ctr")),
                        formatCurrency(row.get("cpc")),
                        formatCurrency(row.get("spend")),
                        formatCurrency(row.get("ad_sales")),
                        row.get("acos") != null ? formatPercent(row.get("acos")) : defaultIfBlank(asString(row.get("acos_raw")), "-"),
                        formatInt(asInt(row.get("ad_orders"))),
                        row.get("cvr") != null ? formatPercent(row.get("cvr")) : defaultIfBlank(asString(row.get("cvr_raw")), "-")
                ))
                .toList();
    }

    private Map<Long, List<Map<String, Object>>> fetchSummaryHistory(Set<Long> productIds, LocalDate startDate, LocalDate endDate) {
        if (productIds == null || productIds.isEmpty() || startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return Map.of();
        }

        String placeholders = productIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
                SELECT product_id,
                       stat_date,
                       order_item_count,
                       sessions_total
                  FROM m3_product_daily_summary
                 WHERE stat_date BETWEEN ? AND ?
                   AND product_id IN (%s)
                """.formatted(placeholders);

        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(startDate));
        params.add(Date.valueOf(endDate));
        params.addAll(productIds);

        return jdbcTemplate.queryForList(sql, params.toArray()).stream()
                .collect(Collectors.groupingBy(row -> ((Number) row.get("product_id")).longValue()));
    }

    private List<MetricCompareItemDTO> buildCompareListFromHistory(int todayValue,
                                                                   List<Map<String, Object>> historyRows,
                                                                   LocalDate statDate,
                                                                   String metricKey,
                                                                   String unitSuffix) {
        int avgValue = 0;
        int lastWeekValue = 0;
        int peakValue = todayValue;

        if (historyRows != null && !historyRows.isEmpty()) {
            int sum = 0;
            int count = 0;
            for (Map<String, Object> historyRow : historyRows) {
                int value = asInt(historyRow.get(metricKey));
                sum += value;
                count += 1;
                if (value > peakValue) {
                    peakValue = value;
                }

                LocalDate rowDate = asLocalDate(historyRow.get("stat_date"));
                if (statDate.minusDays(7).equals(rowDate)) {
                    lastWeekValue = value;
                }
            }
            avgValue = count == 0 ? 0 : (int) Math.round((double) sum / count);
        }

        int maxValue = Math.max(Math.max(todayValue, avgValue), Math.max(lastWeekValue, peakValue));
        return List.of(
                new MetricCompareItemDTO("今", formatInt(todayValue) + unitSuffix, calcCompareHeight(todayValue, maxValue), "today"),
                new MetricCompareItemDTO("均", formatInt(avgValue) + unitSuffix, calcCompareHeight(avgValue, maxValue), "avg"),
                new MetricCompareItemDTO("周", formatInt(lastWeekValue) + unitSuffix, calcCompareHeight(lastWeekValue, maxValue), "lastweek"),
                new MetricCompareItemDTO("高", formatInt(peakValue) + unitSuffix, calcCompareHeight(peakValue, maxValue), "target")
        );
    }

    private int calcCompareHeight(int value, int maxValue) {
        if (maxValue <= 0) {
            return 36;
        }
        return Math.max(36, (int) Math.round(36 + (value * 102.0 / maxValue)));
    }

    private List<ProductOperationItemDTO> getOperationDataMock(LocalDate statDate) {
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
                        "Mock数据",
                        "未分配",
                        "K1",
                        "blue",
                        "智能咖啡机 K1",
                        "$39.99",
                        null,
                        "K1",
                        "K1",
                        new ProductReviewInfoDTO("4.6", "860", "+5", "1", "评价摘要待接真实数据", "当前为页面结构验证数据，后续由真实评论数据替换。", statDate.toString(), "Amazon User", List.of()),
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

    private String buildCoverText(String productName) {
        if (productName == null || productName.isBlank()) {
            return "产品";
        }
        return productName.length() <= 4 ? productName : productName.substring(0, 4);
    }

    private String pickCoverTone(Long productId) {
        String[] tones = {"blue", "green", "orange", "purple"};
        return tones[(int) (Math.abs(productId) % tones.length)];
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int asInt(Object value) {
        if (value == null) return 0;
        return ((Number) value).intValue();
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private LocalDate asLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private String formatInt(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    private String formatCurrency(Object value) {
        BigDecimal decimal = asBigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        return "$" + String.format(Locale.US, "%,.2f", decimal);
    }

    private String formatPercent(Object value) {
        if (value == null) {
            return "0.00%";
        }
        BigDecimal decimal = asBigDecimal(value).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return decimal.toPlainString() + "%";
    }

    private String formatDecimal(Object value, int scale, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        BigDecimal decimal = asBigDecimal(value).setScale(scale, RoundingMode.HALF_UP);
        return decimal.toPlainString();
    }
}
