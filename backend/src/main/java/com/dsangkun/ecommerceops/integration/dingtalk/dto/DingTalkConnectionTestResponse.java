package com.dsangkun.ecommerceops.integration.dingtalk.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DingTalkConnectionTestResponse {
    private boolean enabled;
    private boolean tokenConfigured;
    private boolean tokenAcquired;
    private String baseUrl;
    private String corpSpaceId;
    private String rootFolderId;
    private String unionId;
    private boolean spaceListProbed;
    private boolean spaceListAccessible;
    private Integer spaceCount;
    private String lastError;
    private String message;
}
