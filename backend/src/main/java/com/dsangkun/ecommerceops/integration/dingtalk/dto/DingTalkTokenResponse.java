package com.dsangkun.ecommerceops.integration.dingtalk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DingTalkTokenResponse {

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("expireIn")
    private Long expireIn;
}
