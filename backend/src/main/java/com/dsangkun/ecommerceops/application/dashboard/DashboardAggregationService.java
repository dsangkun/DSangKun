package com.dsangkun.ecommerceops.application.dashboard;

import com.dsangkun.ecommerceops.dto.WorkbenchOverviewDTO;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkDriveClient;
import com.dsangkun.ecommerceops.integration.dingtalk.DingTalkProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardAggregationService {

    private final DingTalkDriveClient dingTalkDriveClient;
    private final DingTalkProperties dingTalkProperties;

    @Cacheable(value = "dashboardOverview", key = "'overview'")
    public WorkbenchOverviewDTO getOverview() {
        int dingtalkDebugCount = 0;
        if (dingTalkProperties.isEnabled()) {
            try {
                dingtalkDebugCount = dingTalkDriveClient
                        .listFiles(dingTalkProperties.getCorpSpaceId(), dingTalkProperties.getRootFolderId())
                        .size();
            } catch (Exception ignored) {
                dingtalkDebugCount = 0;
            }
        }

        return new WorkbenchOverviewDTO(
                dingtalkDebugCount,
                dingtalkDebugCount,
                0,
                0
        );
    }
}
