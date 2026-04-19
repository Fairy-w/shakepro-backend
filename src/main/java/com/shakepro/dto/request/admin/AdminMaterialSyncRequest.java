package com.shakepro.dto.request.admin;

import lombok.Data;

@Data
public class AdminMaterialSyncRequest {

    private Integer maxItems;
    private Boolean dryRun;
    private Boolean overwriteImage;
}
