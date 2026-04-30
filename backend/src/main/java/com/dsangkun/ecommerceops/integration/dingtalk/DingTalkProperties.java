package com.dsangkun.ecommerceops.integration.dingtalk;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "integration.dingtalk")
public class DingTalkProperties {

    private boolean enabled;
    private String baseUrl;
    private String appKey;
    private String appSecret;
    private String corpSpaceId;
    private String rootFolderId;
    private String dailyReportFolderId;
    private String dailyReportFolderName = "Amazon广告数据";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;
}
