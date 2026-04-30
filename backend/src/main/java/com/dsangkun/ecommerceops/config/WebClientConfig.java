package com.dsangkun.ecommerceops.config;

import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("dingTalkWebClient")
    public WebClient dingTalkWebClient(DingTalkProperties dingTalkProperties) {
        WebClient.Builder builder = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (StringUtils.hasText(dingTalkProperties.getBaseUrl())) {
            builder.baseUrl(dingTalkProperties.getBaseUrl());
        }

        return builder.build();
    }
}
