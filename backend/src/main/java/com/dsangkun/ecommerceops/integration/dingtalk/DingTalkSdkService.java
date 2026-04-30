package com.dsangkun.ecommerceops.integration.dingtalk;

import com.aliyun.dingtalkstorage_1_0.models.GetFileDownloadInfoHeaders;
import com.aliyun.dingtalkstorage_1_0.models.GetFileDownloadInfoRequest;
import com.aliyun.dingtalkstorage_1_0.models.GetFileDownloadInfoResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiV2UserGetRequest;
import com.dingtalk.api.response.OapiV2UserGetResponse;
import com.dsangkun.ecommerceops.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DingTalkSdkService {

    private final DingTalkAuthService authService;
    private final WebClient.Builder webClientBuilder = WebClient.builder();

    public Map<String, Object> getUnionIdByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ExternalApiException("DINGTALK_USER_ID_MISSING", "缺少 userid");
        }

        try {
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/topapi/v2/user/get");
            OapiV2UserGetRequest request = new OapiV2UserGetRequest();
            request.setUserid(userId);
            request.setLanguage("zh_CN");
            OapiV2UserGetResponse response = client.execute(request, authService.forceRefreshToken());

            if (response == null) {
                throw new ExternalApiException("DINGTALK_USER_GET_EMPTY", "获取用户信息失败：响应为空");
            }
            if (!response.isSuccess()) {
                throw new ExternalApiException(
                        "DINGTALK_USER_GET_ERROR",
                        "获取用户信息失败: errcode=" + response.getErrcode() + ", errmsg=" + response.getErrmsg()
                );
            }

            OapiV2UserGetResponse.UserGetResponse result = response.getResult();
            if (result == null) {
                throw new ExternalApiException("DINGTALK_USER_GET_EMPTY_RESULT", "获取用户信息失败：result 为空");
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userid", userId);
            data.put("unionId", result.getUnionid());
            data.put("name", result.getName());
            data.put("mobile", result.getMobile());
            data.put("active", result.getActive());
            data.put("stateCode", result.getStateCode());
            data.put("raw", result);
            return data;
        } catch (ExternalApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExternalApiException("DINGTALK_USER_GET_EXCEPTION", ex.getMessage());
        }
    }

    public Map<String, Object> getDownloadInfoBySdk(String unionId, String spaceId, String fileId) {
        if (!StringUtils.hasText(unionId)) {
            throw new ExternalApiException("DINGTALK_UNION_ID_MISSING", "SDK 下载测试缺少 unionId");
        }
        if (!StringUtils.hasText(spaceId)) {
            throw new ExternalApiException("DINGTALK_SPACE_ID_MISSING", "SDK 下载测试缺少 spaceId");
        }
        if (!StringUtils.hasText(fileId)) {
            throw new ExternalApiException("DINGTALK_FILE_ID_MISSING", "SDK 下载测试缺少 fileId");
        }

        try {
            com.aliyun.dingtalkstorage_1_0.Client client = new com.aliyun.dingtalkstorage_1_0.Client(buildConfig());
            GetFileDownloadInfoHeaders headers = new GetFileDownloadInfoHeaders();
            headers.xAcsDingtalkAccessToken = authService.forceRefreshToken();
            GetFileDownloadInfoRequest request = new GetFileDownloadInfoRequest()
                    .setUnionId(unionId);
            GetFileDownloadInfoResponse response = client.getFileDownloadInfoWithOptions(
                    spaceId,
                    fileId,
                    request,
                    headers,
                    new RuntimeOptions()
            );

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("unionId", unionId);
            data.put("spaceId", spaceId);
            data.put("fileId", fileId);
            data.put("request", Map.of("unionId", unionId));
            data.put("headersUsed", Map.of("x-acs-dingtalk-access-token", "***"));
            if (response != null && response.getBody() != null) {
                var body = response.getBody();
                var info = body.getHeaderSignatureInfo();
                data.put("resourceUrls", info == null ? List.of() : info.getResourceUrls());
                data.put("headers", info == null || info.getHeaders() == null ? Map.of() : info.getHeaders());
                data.put("body", body);
            } else {
                data.put("body", null);
            }
            return data;
        } catch (Exception ex) {
            throw new ExternalApiException("DINGTALK_DOWNLOAD_INFO_SDK_ERROR", ex.getMessage());
        }
    }

    public ResponseEntity<ByteArrayResource> downloadFileBySdk(String unionId,
                                                               String spaceId,
                                                               String fileId,
                                                               String fileName) {
        byte[] bytes = downloadFileBytesBySdk(unionId, spaceId, fileId);
        String resolvedFileName = StringUtils.hasText(fileName) ? fileName : fileId;
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(resolvedFileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(resource);
    }

    public byte[] downloadFileBytesBySdk(String unionId, String spaceId, String fileId) {
        Map<String, Object> info = getDownloadInfoBySdk(unionId, spaceId, fileId);
        Object urlsObj = info.get("resourceUrls");
        if (!(urlsObj instanceof List<?> urls) || urls.isEmpty() || urls.get(0) == null) {
            throw new ExternalApiException("DINGTALK_DOWNLOAD_URL_MISSING", "未获取到可用的下载地址");
        }
        String resourceUrl = String.valueOf(urls.get(0));

        HttpHeaders proxyHeaders = new HttpHeaders();
        Object headersObj = info.get("headers");
        if (headersObj instanceof Map<?, ?> headerMap) {
            for (Map.Entry<?, ?> entry : headerMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    proxyHeaders.add(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }

        byte[] bytes;
        try {
            bytes = webClientBuilder.build()
                    .get()
                    .uri(resourceUrl)
                    .headers(httpHeaders -> httpHeaders.addAll(proxyHeaders))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception ex) {
            throw new ExternalApiException("DINGTALK_DOWNLOAD_PROXY_ERROR", ex.getMessage());
        }

        if (bytes == null) {
            throw new ExternalApiException("DINGTALK_DOWNLOAD_EMPTY", "下载结果为空");
        }
        return bytes;
    }

    private Config buildConfig() {
        Config config = new Config();
        config.protocol = "https";
        config.regionId = "central";
        return config;
    }
}
