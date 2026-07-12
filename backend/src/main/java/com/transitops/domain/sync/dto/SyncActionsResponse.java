package com.transitops.domain.sync.dto;

import java.util.List;

public record SyncActionsResponse(
        List<SyncActionResult> results
) {}