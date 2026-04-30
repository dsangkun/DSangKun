package com.dsangkun.ecommerceops.integration.dingtalk;

import com.dsangkun.ecommerceops.exception.ExternalApiException;
import com.dsangkun.ecommerceops.integration.dingtalk.dto.DingTalkTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DingTalkAuthService {

    private final DingTalkProperties properties;

    @Qualifier("dingTalkWebClient")
    private final WebClient webClient;

    private volatile String cachedToken;
    private volatile Instant expireAt;

    public synchronized String getAccessToken() {
        return getAccessToken(false);
    }

    public synchronized String forceRefreshToken() {
        cachedToken = null;
        expireAt = null;
        return getAccessToken(true);
    }

    private String getAccessToken(boolean forceRefresh) {
        if (!StringUtils.hasText(properties.getAppKey()) || !StringUtils.hasText(properties.getAppSecret())) {
            throw new ExternalApiException("DINGTALK_CONFIG_MISSING", "未配置钉钉 appKey/appSecret");
        }

        if (!forceRefresh && cachedToken != null && expireAt != null && Instant.now().isBefore(expireAt.minusSeconds(60))) {
            return cachedToken;
        }

        DingTalkTokenResponse response = webClient.post()
                .uri("/v1.0/oauth2/accessToken")
                .bodyValue(Map.of(
                        "appKey", properties.getAppKey(),
                        "appSecret", properties.getAppSecret()
                ))
                .retrieve()
                .bodyToMono(DingTalkTokenResponse.class)
                .block();

        if (response == null || !StringUtils.hasText(response.getAccessToken())) {
            throw new ExternalApiException("DINGTALK_TOKEN_ERROR", "获取钉钉 access_token 失败");
        }

        cachedToken = response.getAccessToken();
        expireAt = Instant.now().plusSeconds(response.getExpireIn() == null ? 7200 : response.getExpireIn());
        return cachedToken;
    }
}
