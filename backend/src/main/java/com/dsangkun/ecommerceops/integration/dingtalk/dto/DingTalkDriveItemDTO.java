package com.dsangkun.ecommerceops.integration.dingtalk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DingTalkDriveItemDTO {
    private String fileId;
    private String name;
    private String type;
    private Long size;
    private String modifiedTime;
}
