package com.transitops.domain.sync.service;

import com.transitops.domain.sync.dto.SyncActionItem;
import com.transitops.domain.sync.dto.SyncActionResult;
import com.transitops.domain.sync.dto.SyncActionsRequest;
import com.transitops.domain.sync.dto.SyncActionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncService {

    private final SyncActionProcessor processor;

    public SyncActionsResponse process(SyncActionsRequest request) {
        List<SyncActionResult> results = request.actions().stream()
                .map(processor::processOne)
                .toList();
        return new SyncActionsResponse(results);
    }
}