package com.dsangkun.ecommerceops.controller;

import com.dsangkun.ecommerceops.common.ApiResponse;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkAuthService;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkDriveClient;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkProperties;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkSdkService;
import com.dsangkun.ecommerceops.integration.dingtalk.dto.DingTalkConnectionTestResponse;
import com.dsangkun.ecommerceops.integration.dingtalk.dto.DingTalkDriveItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations/dingtalk")
@RequiredArgsConstructor
public class IntegrationDebugController {

    private final DingTalkProperties dingTalkProperties;
    private final DingTalkAuthService dingTalkAuthService;
    private final DingTalkDriveClient dingTalkDriveClient;
    private final DingTalkSdkService dingTalkSdkService;

    @GetMapping("/test")
    public ApiResponse<DingTalkConnectionTestResponse> test(
            @RequestParam(required = false) String unionId
    ) {
        boolean configured = StringUtils.hasText(dingTalkProperties.getAppKey())
                && StringUtils.hasText(dingTalkProperties.getAppSecret());
        boolean tokenAcquired = false;
        boolean spaceListProbed = false;
        boolean spaceListAccessible = false;
        Integer spaceCount = null;
        String lastError = null;
        String message = "钉钉集成未启用";

        if (dingTalkProperties.isEnabled()) {
            message = "钉钉集成已启用，但未获取 token";
            if (configured) {
                dingTalkAuthService.getAccessToken();
                tokenAcquired = true;
                try {
                    Map<String, Object> response = dingTalkDriveClient.listSpaces(unionId);
                    spaceListProbed = true;
                    spaceListAccessible = true;
                    Object spaces = response == null ? null : response.get("spaces");
                    if (spaces instanceof List<?> list) {
                        spaceCount = list.size();
                    }
                    message = "钉钉 access_token 获取成功，空间列表接口可访问";
                } catch (Exception ex) {
                    spaceListProbed = true;
                    lastError = ex.getMessage();
                    message = "钉钉 access_token 获取成功，但空间列表接口不可访问：" + ex.getMessage();
                }
            }
        }

        return ApiResponse.ok(DingTalkConnectionTestResponse.builder()
                .enabled(dingTalkProperties.isEnabled())
                .tokenConfigured(configured)
                .tokenAcquired(tokenAcquired)
                .baseUrl(dingTalkProperties.getBaseUrl())
                .corpSpaceId(dingTalkProperties.getCorpSpaceId())
                .rootFolderId(dingTalkProperties.getRootFolderId())
                .unionId(unionId)
                .spaceListProbed(spaceListProbed)
                .spaceListAccessible(spaceListAccessible)
                .spaceCount(spaceCount)
                .lastError(lastError)
                .message(message)
                .build());
    }

    @GetMapping("/token/refresh")
    public ApiResponse<Map<String, Object>> refreshToken() {
        String token = dingTalkAuthService.forceRefreshToken();
        return ApiResponse.ok(Map.of(
                "refreshed", true,
                "tokenPrefix", token.substring(0, Math.min(12, token.length()))
        ));
    }

    @GetMapping("/spaces")
    public ApiResponse<Map<String, Object>> spaces(
            @RequestParam(required = false) String unionId
    ) {
        return ApiResponse.ok(dingTalkDriveClient.listSpaces(unionId));
    }

    @GetMapping("/files")
    public ApiResponse<List<DingTalkDriveItemDTO>> files(
            @RequestParam(required = false) String spaceId,
            @RequestParam(required = false) String folderId,
            @RequestParam(required = false) String unionId
    ) {
        String resolvedSpaceId = StringUtils.hasText(spaceId) ? spaceId : dingTalkProperties.getCorpSpaceId();
        String resolvedFolderId = StringUtils.hasText(folderId) ? folderId : dingTalkProperties.getRootFolderId();
        return ApiResponse.ok(dingTalkDriveClient.listFiles(resolvedSpaceId, resolvedFolderId, unionId));
    }

    @GetMapping("/userid-to-unionid")
    public ApiResponse<Map<String, Object>> userIdToUnionId(
            @RequestParam String userId
    ) {
        return ApiResponse.ok(dingTalkSdkService.getUnionIdByUserId(userId));
    }

    @GetMapping("/download-info")
    public ApiResponse<Map<String, Object>> downloadInfo(
            @RequestParam String spaceId,
            @RequestParam String fileId,
            @RequestParam(defaultValue = "true") boolean refreshToken
    ) {
        return ApiResponse.ok(dingTalkDriveClient.getDownloadInfoProbe(spaceId, fileId, refreshToken));
    }

    @GetMapping("/download-info-sdk")
    public ApiResponse<Map<String, Object>> downloadInfoSdk(
            @RequestParam String unionId,
            @RequestParam String spaceId,
            @RequestParam String fileId
    ) {
        return ApiResponse.ok(dingTalkSdkService.getDownloadInfoBySdk(unionId, spaceId, fileId));
    }

    @GetMapping("/download-file-sdk")
    public ResponseEntity<ByteArrayResource> downloadFileSdk(
            @RequestParam String unionId,
            @RequestParam String spaceId,
            @RequestParam String fileId,
            @RequestParam(required = false) String fileName
    ) {
        return dingTalkSdkService.downloadFileBySdk(unionId, spaceId, fileId, fileName);
    }

    @GetMapping("/raw-get")
    public ApiResponse<Map<String, Object>> rawGet(
            @RequestParam String path,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "false") boolean refreshToken
    ) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        MultiValueMap<String, String> queryParams = parseQuery(query);
        return ApiResponse.ok(dingTalkDriveClient.getRaw(path, queryParams, refreshToken));
    }

    @GetMapping("/raw-post")
    public ApiResponse<Map<String, Object>> rawPost(
            @RequestParam String path,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String body,
            @RequestParam(defaultValue = "false") boolean refreshToken
    ) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        MultiValueMap<String, String> queryParams = parseQuery(query);
        return ApiResponse.ok(dingTalkDriveClient.postRaw(path, queryParams, parseBody(body), refreshToken));
    }

    private MultiValueMap<String, String> parseQuery(String query) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        if (!StringUtils.hasText(query)) {
            return queryParams;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                queryParams.add(pair.trim(), "");
                continue;
            }
            String key = pair.substring(0, idx).trim();
            String value = pair.substring(idx + 1).trim();
            if (StringUtils.hasText(key)) {
                queryParams.add(key, value);
            }
        }
        return queryParams;
    }

    private Map<String, Object> parseBody(String body) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(body)) {
            return result;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                result.put(pair.trim(), "");
                continue;
            }
            String key = pair.substring(0, idx).trim();
            String value = pair.substring(idx + 1).trim();
            if (StringUtils.hasText(key)) {
                result.put(key, value);
            }
        }
        return result;
    }
}
