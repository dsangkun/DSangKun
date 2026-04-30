package com.dsangkun.ecommerceops.integration.dingtalk;

import com.dsangkun.ecommerceops.exception.ExternalApiException;
import com.dsangkun.ecommerceops.integration.dingtalk.dto.DingTalkDriveItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DingTalkDriveClient {

    private final DingTalkAuthService authService;

    @Qualifier("dingTalkWebClient")
    private final WebClient webClient;

    @Cacheable(value = "dingtalkFiles", key = "#spaceId + ':' + #folderId + ':' + (#unionId == null ? '' : #unionId)")
    public List<DingTalkDriveItemDTO> listFiles(String spaceId, String folderId, String unionId) {
        if (!StringUtils.hasText(spaceId)) {
            throw new ExternalApiException("DINGTALK_SPACE_ID_MISSING", "未配置 corpSpaceId / spaceId");
        }

        String token = authService.getAccessToken();
        String parentId = StringUtils.hasText(folderId) ? folderId : "0";

        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/v1.0/drive/spaces/{spaceId}/files")
                                .queryParam("parentId", parentId)
                                .queryParam("maxResults", 100);
                        if (StringUtils.hasText(unionId)) {
                            builder.queryParam("unionId", unionId);
                        }
                        return builder.build(spaceId);
                    })
                    .header("x-acs-dingtalk-access-token", token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            return mapFileItems(response);
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException("DINGTALK_DRIVE_FILES_ERROR", buildRemoteMessage(ex));
        }
    }

    public List<DingTalkDriveItemDTO> listFiles(String spaceId, String folderId) {
        return listFiles(spaceId, folderId, null);
    }

    public Map<String, Object> listSpaces(String unionId) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("spaceType", "org");
        queryParams.add("maxResults", "100");
        if (StringUtils.hasText(unionId)) {
            queryParams.add("unionId", unionId);
        }
        return getRaw("/v1.0/drive/spaces", queryParams, false);
    }

    public Map<String, Object> getRaw(String path, MultiValueMap<String, String> queryParams, boolean forceRefreshToken) {
        return getRaw(path, queryParams, forceRefreshToken, "DINGTALK_DRIVE_RAW_GET_ERROR");
    }

    public Map<String, Object> postRaw(String path,
                                       MultiValueMap<String, String> queryParams,
                                       Map<String, Object> body,
                                       boolean forceRefreshToken) {
        String token = forceRefreshToken ? authService.forceRefreshToken() : authService.getAccessToken();
        try {
            return webClient.post()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(path);
                        if (queryParams != null) {
                            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                                String key = entry.getKey();
                                List<String> values = entry.getValue();
                                if (!StringUtils.hasText(key) || values == null || values.isEmpty()) {
                                    continue;
                                }
                                for (String value : values) {
                                    builder.queryParam(key, value);
                                }
                            }
                        }
                        return builder.build();
                    })
                    .header("x-acs-dingtalk-access-token", token)
                    .bodyValue(body == null ? Map.of() : body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException("DINGTALK_DRIVE_RAW_POST_ERROR", buildRemoteMessage(ex));
        }
    }

    public List<DingTalkDriveItemDTO> listStorageDentries(String spaceId, String parentId, String unionId) {
        if (!StringUtils.hasText(spaceId)) {
            throw new ExternalApiException("DINGTALK_SPACE_ID_MISSING", "未配置 corpSpaceId / spaceId");
        }
        if (!StringUtils.hasText(parentId)) {
            throw new ExternalApiException("DINGTALK_PARENT_ID_MISSING", "未配置 parentId");
        }

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("parentId", parentId);
        queryParams.add("maxResults", "50");
        if (StringUtils.hasText(unionId)) {
            queryParams.add("unionId", unionId);
        }

        Map<String, Object> response = requestRaw(
                "/v1.0/storage/spaces/{spaceId}/dentries",
                Map.of("spaceId", spaceId),
                queryParams,
                true,
                "DINGTALK_STORAGE_DENTRIES_ERROR"
        );
        return mapDentryItems(response);
    }

    public Map<String, Object> getDownloadInfoProbe(String spaceId, String fileId, boolean forceRefreshToken) {
        if (!StringUtils.hasText(spaceId)) {
            throw new ExternalApiException("DINGTALK_SPACE_ID_MISSING", "下载测试缺少 spaceId");
        }
        if (!StringUtils.hasText(fileId)) {
            throw new ExternalApiException("DINGTALK_FILE_ID_MISSING", "下载测试缺少 fileId");
        }

        List<ProbeDefinition> probes = List.of(
                new ProbeDefinition(
                        "/v1.0/storage/spaces/{spaceId}/files/{fileId}/downloadInfos",
                        Map.of("spaceId", spaceId, "fileId", fileId),
                        null
                ),
                new ProbeDefinition(
                        "/v1.0/storage/spaces/{spaceId}/files/{fileId}/download-info",
                        Map.of("spaceId", spaceId, "fileId", fileId),
                        null
                ),
                new ProbeDefinition(
                        "/v1.0/drive/spaces/{spaceId}/files/{fileId}/downloadInfos",
                        Map.of("spaceId", spaceId, "fileId", fileId),
                        null
                ),
                new ProbeDefinition(
                        "/v1.0/drive/spaces/{spaceId}/files/{fileId}/download-info",
                        Map.of("spaceId", spaceId, "fileId", fileId),
                        null
                ),
                new ProbeDefinition(
                        "/v1.0/storage/downloadInfos",
                        Map.of(),
                        queryOf(Map.of("spaceId", spaceId, "fileId", fileId))
                ),
                new ProbeDefinition(
                        "/v1.0/drive/downloadInfos",
                        Map.of(),
                        queryOf(Map.of("spaceId", spaceId, "fileId", fileId))
                )
        );

        List<Map<String, Object>> attempts = new ArrayList<>();
        String firstError = null;
        for (ProbeDefinition probe : probes) {
            try {
                Map<String, Object> data = requestRaw(probe.pathTemplate(), probe.pathVariables(), probe.queryParams(), forceRefreshToken);
                attempts.add(Map.of(
                        "path", probe.pathTemplate(),
                        "query", probe.queryParams() == null ? Map.of() : probe.queryParams(),
                        "success", true,
                        "data", data
                ));
                return Map.of(
                        "matched", true,
                        "path", probe.pathTemplate(),
                        "query", probe.queryParams() == null ? Map.of() : probe.queryParams(),
                        "data", data,
                        "attempts", attempts
                );
            } catch (ExternalApiException ex) {
                if (firstError == null) {
                    firstError = ex.getMessage();
                }
                attempts.add(Map.of(
                        "path", probe.pathTemplate(),
                        "query", probe.queryParams() == null ? Map.of() : probe.queryParams(),
                        "success", false,
                        "error", ex.getMessage()
                ));
            }
        }

        return Map.of(
                "matched", false,
                "spaceId", spaceId,
                "fileId", fileId,
                "message", "所有下载信息候选接口均未命中",
                "firstError", firstError == null ? "未知错误" : firstError,
                "attempts", attempts
        );
    }

    private Map<String, Object> getRaw(String path, MultiValueMap<String, String> queryParams, boolean forceRefreshToken, String errorCode) {
        return requestRaw(path, Map.of(), queryParams, forceRefreshToken, errorCode);
    }

    private Map<String, Object> requestRaw(String pathTemplate,
                                           Map<String, String> pathVariables,
                                           MultiValueMap<String, String> queryParams,
                                           boolean forceRefreshToken) {
        return requestRaw(pathTemplate, pathVariables, queryParams, forceRefreshToken, "DINGTALK_DRIVE_RAW_GET_ERROR");
    }

    private Map<String, Object> requestRaw(String pathTemplate,
                                           Map<String, String> pathVariables,
                                           MultiValueMap<String, String> queryParams,
                                           boolean forceRefreshToken,
                                           String errorCode) {
        String token = forceRefreshToken ? authService.forceRefreshToken() : authService.getAccessToken();
        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(pathTemplate);
                        if (queryParams != null) {
                            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                                String key = entry.getKey();
                                List<String> values = entry.getValue();
                                if (!StringUtils.hasText(key) || values == null || values.isEmpty()) {
                                    continue;
                                }
                                for (String value : values) {
                                    builder.queryParam(key, value);
                                }
                            }
                        }
                        return pathVariables == null || pathVariables.isEmpty()
                                ? builder.build()
                                : builder.build(pathVariables);
                    })
                    .header("x-acs-dingtalk-access-token", token)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException(errorCode, buildRemoteMessage(ex));
        }
    }

    private MultiValueMap<String, String> queryOf(Map<String, String> source) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        if (source == null || source.isEmpty()) {
            return queryParams;
        }
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (StringUtils.hasText(entry.getKey())) {
                queryParams.add(entry.getKey(), entry.getValue());
            }
        }
        return queryParams;
    }

    private record ProbeDefinition(
            String pathTemplate,
            Map<String, String> pathVariables,
            MultiValueMap<String, String> queryParams
    ) {
    }

    private List<DingTalkDriveItemDTO> mapFileItems(Map<String, Object> response) {
        Object filesObj = response == null ? null : response.get("files");
        if (!(filesObj instanceof List<?> fileList)) {
            return List.of();
        }

        List<DingTalkDriveItemDTO> result = new ArrayList<>();
        for (Object item : fileList) {
            if (!(item instanceof Map<?, ?> raw)) {
                continue;
            }
            String fileId = stringValue(raw.get("fileId"));
            String name = stringValue(raw.get("name"));
            String type = stringValue(raw.get("type"));
            Long size = longValue(raw.get("size"));
            String modifiedTime = stringValue(raw.get("modifiedTime"));
            if (!StringUtils.hasText(modifiedTime)) {
                modifiedTime = stringValue(raw.get("gmtModified"));
            }
            result.add(new DingTalkDriveItemDTO(fileId, name, type, size, modifiedTime));
        }
        return result;
    }

    private List<DingTalkDriveItemDTO> mapDentryItems(Map<String, Object> response) {
        Object dentriesObj = response == null ? null : response.get("dentries");
        if (!(dentriesObj instanceof List<?> dentryList)) {
            return List.of();
        }

        List<DingTalkDriveItemDTO> result = new ArrayList<>();
        for (Object item : dentryList) {
            if (!(item instanceof Map<?, ?> raw)) {
                continue;
            }
            String fileId = stringValue(raw.get("id"));
            String name = stringValue(raw.get("name"));
            String type = stringValue(raw.get("type"));
            Long size = longValue(raw.get("size"));
            String modifiedTime = stringValue(raw.get("modifiedTime"));
            if (!StringUtils.hasText(modifiedTime)) {
                modifiedTime = stringValue(raw.get("gmtModified"));
            }
            result.add(new DingTalkDriveItemDTO(fileId, name, type, size, modifiedTime));
        }
        return result;
    }

    private String buildRemoteMessage(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (StringUtils.hasText(body)) {
            return body;
        }
        return "HTTP " + ex.getStatusCode().value() + " " + ex.getStatusText();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
