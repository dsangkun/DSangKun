package com.dsangkun.ecommerceops.service;

import com.dsangkun.ecommerceops.dto.DailyReportProductSheetResponse;
import com.dsangkun.ecommerceops.dto.DailyReportSheetCandidateDTO;
import com.dsangkun.ecommerceops.dto.DailyReportSheetDTO;
import com.dsangkun.ecommerceops.exception.BizException;
import com.dsangkun.ecommerceops.exception.ExternalApiException;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkDriveClient;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkProperties;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkSdkService;
import com.dsangkun.ecommerceops.integration.dingtalk.dto.DingTalkDriveItemDTO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DailyReportService {

    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{6})");
    private static final DateTimeFormatter DINGTALK_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    private final DingTalkDriveClient dingTalkDriveClient;
    private final DingTalkSdkService dingTalkSdkService;
    private final DingTalkProperties dingTalkProperties;

    public DailyReportProductSheetResponse getLatestProductSheet(String unionId,
                                                                 String parentAsin,
                                                                 String parentProductName,
                                                                 List<String> childProductNames) {
        return buildLatestProductSheet(unionId, parentAsin, parentProductName, childProductNames, null).response();
    }

    public Map<String, Object> getLatestProductSheetDebug(String unionId,
                                                          String parentAsin,
                                                          String parentProductName,
                                                          List<String> childProductNames,
                                                          String latestReportFileId) {
        LinkedHashMap<String, Object> debug = new LinkedHashMap<>();
        debug.put("unionId", unionId);
        debug.put("parentAsin", parentAsin);
        debug.put("parentProductName", parentProductName);
        debug.put("childProductNames", childProductNames == null ? List.of() : childProductNames);
        debug.put("manualLatestReportFileId", latestReportFileId);

        try {
            BuildLatestSheetResult result = buildLatestProductSheet(unionId, parentAsin, parentProductName, childProductNames, latestReportFileId);
            debug.putAll(result.debug());
            debug.put("success", true);
            debug.put("result", result.response());
            return debug;
        } catch (Exception ex) {
            debug.put("success", false);
            debug.put("errorType", ex.getClass().getSimpleName());
            debug.put("errorMessage", ex.getMessage());
            if (ex instanceof BizException bizException) {
                debug.put("errorCode", bizException.getCode());
            }
            return debug;
        }
    }

    private BuildLatestSheetResult buildLatestProductSheet(String unionId,
                                                           String parentAsin,
                                                           String parentProductName,
                                                           List<String> childProductNames,
                                                           String latestReportFileId) {
        LinkedHashMap<String, Object> debug = new LinkedHashMap<>();

        String folderId = resolveDailyReportFolderId(unionId);
        debug.put("resolvedFolderId", folderId);

        List<DingTalkDriveItemDTO> files = dingTalkDriveClient.listStorageDentries(dingTalkProperties.getCorpSpaceId(), folderId, unionId);
        debug.put("listedFileCount", files.size());

        List<DingTalkDriveItemDTO> excelFiles = files.stream()
                .filter(this::isExcelFile)
                .toList();
        debug.put("excelFileCount", excelFiles.size());
        debug.put("excelFiles", excelFiles.stream()
                .map(this::toDebugFile)
                .toList());

        DingTalkDriveItemDTO latestFile = resolveLatestFile(excelFiles, latestReportFileId);
        debug.put("selectedFile", toDebugFile(latestFile));
        debug.put("selectedBy", StringUtils.hasText(latestReportFileId) ? "manualLatestReportFileId" : "modifiedTime");

        String reportDate = extractReportDate(latestFile.getName());
        if (!StringUtils.hasText(reportDate)) {
            reportDate = LocalDate.now().toString().replace("-", "");
        }
        debug.put("resolvedReportDate", reportDate);

        byte[] content = dingTalkSdkService.downloadFileBytesBySdk(unionId, dingTalkProperties.getCorpSpaceId(), latestFile.getFileId());
        debug.put("downloadedBytes", content.length);

        ParsedWorkbookResult parsedWorkbook = parseWorkbook(latestFile.getName(), reportDate, content);
        debug.put("workbookSheetCount", parsedWorkbook.sheetCount());
        debug.put("workbookSheetNames", parsedWorkbook.sheetNames());

        DailyReportProductSheetResponse response = matchSheet(parentAsin, parentProductName, childProductNames, reportDate, parsedWorkbook.sheetMap());
        debug.put("matchedSheetName", response.sheet() == null ? null : response.sheet().sheetName());
        debug.put("matchedBy", response.matchedBy());
        debug.put("confidence", response.confidence());
        debug.put("candidateCount", response.candidates() == null ? 0 : response.candidates().size());
        debug.put("candidates", response.candidates());

        return new BuildLatestSheetResult(response, debug);
    }

    private String resolveDailyReportFolderId(String unionId) {
        if (StringUtils.hasText(dingTalkProperties.getDailyReportFolderId())) {
            return dingTalkProperties.getDailyReportFolderId();
        }
        List<DingTalkDriveItemDTO> rootFiles = dingTalkDriveClient.listStorageDentries(dingTalkProperties.getCorpSpaceId(), dingTalkProperties.getRootFolderId(), unionId);
        return rootFiles.stream()
                .filter(item -> "folder".equalsIgnoreCase(String.valueOf(item.getType())))
                .filter(item -> dingTalkProperties.getDailyReportFolderName().equalsIgnoreCase(String.valueOf(item.getName())))
                .map(DingTalkDriveItemDTO::getFileId)
                .findFirst()
                .orElse(dingTalkProperties.getRootFolderId());
    }

    private boolean isExcelFile(DingTalkDriveItemDTO item) {
        String name = String.valueOf(item.getName()).toLowerCase(Locale.ROOT);
        return name.endsWith(".xlsx");
    }

    private String extractReportDate(String fileName) {
        Matcher matcher = DATE_PATTERN.matcher(String.valueOf(fileName));
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

    private ParsedWorkbookResult parseWorkbook(String sourceFile, String reportDate, byte[] content) {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Map<String, DailyReportSheetDTO> result = new LinkedHashMap<>();
            List<String> sheetNames = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sheetNames.add(sheet.getSheetName());
                List<List<String>> rows = new ArrayList<>();
                int maxCellNum = 0;
                for (Row row : sheet) {
                    maxCellNum = Math.max(maxCellNum, Math.max(row.getLastCellNum(), 0));
                }
                for (Row row : sheet) {
                    List<String> cols = new ArrayList<>();
                    for (int cellIndex = 0; cellIndex < maxCellNum; cellIndex++) {
                        var cell = row.getCell(cellIndex);
                        cols.add(cell == null ? "" : formatter.formatCellValue(cell));
                    }
                    rows.add(cols);
                }
                result.put(sheet.getSheetName(), new DailyReportSheetDTO(sheet.getSheetName(), sourceFile, reportDate, rows));
            }
            return new ParsedWorkbookResult(result, workbook.getNumberOfSheets(), sheetNames);
        } catch (Exception ex) {
            throw new ExternalApiException("DAILY_REPORT_PARSE_ERROR", ex.getMessage());
        }
    }

    private DailyReportProductSheetResponse matchSheet(String parentAsin,
                                                       String parentProductName,
                                                       List<String> childProductNames,
                                                       String reportDate,
                                                       Map<String, DailyReportSheetDTO> sheetMap) {
        List<String> aliases = dedupeAliases(parentProductName, childProductNames);
        List<DailyReportSheetCandidateDTO> candidates = sheetMap.values().stream()
                .map(sheet -> scoreSheet(sheet, aliases))
                .filter(item -> item.score() > 0)
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .limit(5)
                .toList();

        DailyReportSheetCandidateDTO best = candidates.isEmpty() ? null : candidates.get(0);
        DailyReportSheetDTO matchedSheet = best == null ? null : sheetMap.get(best.sheetName());
        String confidence = resolveConfidence(best == null ? 0 : best.score());
        String matchedBy = best == null || best.reasons().isEmpty() ? "未匹配到可用 Sheet" : best.reasons().get(0);

        return new DailyReportProductSheetResponse(
                parentAsin,
                parentProductName,
                reportDate,
                matchedBy,
                matchedSheet == null ? "none" : confidence,
                aliases,
                matchedSheet,
                candidates
        );
    }

    private DailyReportSheetCandidateDTO scoreSheet(DailyReportSheetDTO sheet, List<String> aliases) {
        String normalizedSheetName = normalize(sheet.sheetName());
        List<String> normalizedSubjects = new ArrayList<>();
        for (List<String> row : sheet.rows()) {
            String subject = row.isEmpty() ? "" : row.get(0);
            String normalized = normalize(subject);
            if (StringUtils.hasText(normalized)) {
                normalizedSubjects.add(normalized);
            }
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();
        for (String alias : aliases) {
            String normalizedAlias = normalize(alias);
            if (!StringUtils.hasText(normalizedAlias)) {
                continue;
            }
            if (sheet.sheetName().equals(alias)) {
                score += 220;
                reasons.add("Sheet 名完全匹配：" + alias);
            }
            if (normalizedAlias.equals(normalizedSheetName)) {
                score += 180;
                reasons.add("Sheet 名归一化匹配：" + alias);
            }
            if (normalizedSubjects.contains(normalizedAlias)) {
                score += 140;
                reasons.add("Sheet 首列归一化命中：" + alias);
            }
            if (normalizedSheetName.contains(normalizedAlias) || normalizedAlias.contains(normalizedSheetName)) {
                score += 90;
                reasons.add("Sheet 名包含关系：" + alias);
            }
            boolean subjectContains = normalizedSubjects.stream().anyMatch(item -> item.contains(normalizedAlias) || normalizedAlias.contains(item));
            if (subjectContains) {
                score += 80;
                reasons.add("Sheet 首列包含关系：" + alias);
            }
        }
        return new DailyReportSheetCandidateDTO(sheet.sheetName(), score, reasons.stream().distinct().toList());
    }

    private List<String> dedupeAliases(String parentProductName, List<String> childProductNames) {
        Map<String, String> seen = new LinkedHashMap<>();
        List<String> source = new ArrayList<>();
        source.add(parentProductName);
        if (childProductNames != null) {
            source.addAll(childProductNames);
        }
        for (String item : source) {
            String normalized = normalize(item);
            if (StringUtils.hasText(normalized) && !seen.containsKey(normalized)) {
                seen.put(normalized, item);
            }
        }
        return new ArrayList<>(seen.values());
    }

    private String normalize(String value) {
        return String.valueOf(value == null ? "" : value)
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "");
    }

    private Instant resolveSortInstant(DingTalkDriveItemDTO item) {
        String modifiedTime = item.getModifiedTime();
        if (StringUtils.hasText(modifiedTime)) {
            try {
                return ZonedDateTime.parse(modifiedTime, DINGTALK_TIME_FORMATTER).toInstant();
            } catch (Exception ignored) {
            }
        }
        String reportDate = extractReportDate(item.getName());
        if (StringUtils.hasText(reportDate)) {
            try {
                return LocalDate.parse(reportDate, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {
            }
        }
        return Instant.EPOCH;
    }

    private DingTalkDriveItemDTO resolveLatestFile(List<DingTalkDriveItemDTO> excelFiles, String latestReportFileId) {
        if (StringUtils.hasText(latestReportFileId)) {
            return excelFiles.stream()
                    .filter(item -> latestReportFileId.equals(item.getFileId()))
                    .findFirst()
                    .orElseGet(() -> new DingTalkDriveItemDTO(latestReportFileId, latestReportFileId, "file", null, null));
        }
        return excelFiles.stream()
                .max(Comparator.comparing(this::resolveSortInstant))
                .orElseThrow(() -> new ExternalApiException("DAILY_REPORT_FILE_NOT_FOUND", "未找到可用的最新日报文件"));
    }

    private Map<String, Object> toDebugFile(DingTalkDriveItemDTO item) {
        if (item == null) {
            return Map.of();
        }
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("fileId", item.getFileId());
        map.put("name", item.getName());
        map.put("type", item.getType());
        map.put("size", item.getSize());
        map.put("modifiedTime", item.getModifiedTime());
        map.put("sortInstant", resolveSortInstant(item).toString());
        map.put("reportDate", extractReportDate(item.getName()));
        return map;
    }

    private String resolveConfidence(int score) {
        if (score >= 180) return "high";
        if (score >= 140) return "medium";
        if (score >= 90) return "low";
        return "none";
    }

    private record ParsedWorkbookResult(
            Map<String, DailyReportSheetDTO> sheetMap,
            int sheetCount,
            List<String> sheetNames
    ) {
    }

    private record BuildLatestSheetResult(
            DailyReportProductSheetResponse response,
            Map<String, Object> debug
    ) {
    }
}
