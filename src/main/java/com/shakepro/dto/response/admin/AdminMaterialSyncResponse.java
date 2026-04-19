package com.shakepro.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminMaterialSyncResponse {

    private int totalFetched;
    private int processed;
    private int matchedByDictionary;
    private int created;
    private int updated;
    private int skippedNoDictionary;
    private int skippedImageExists;
    private int failed;
    private boolean dryRun;
}
