package com.transitops.domain.sync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SyncActionsRequest(
        @NotEmpty @Valid List<SyncActionItem> actions
) {}