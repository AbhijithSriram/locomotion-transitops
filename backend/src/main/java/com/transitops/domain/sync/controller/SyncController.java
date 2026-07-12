package com.transitops.domain.sync.controller;

import com.transitops.domain.sync.dto.SyncActionsRequest;
import com.transitops.domain.sync.dto.SyncActionsResponse;
import com.transitops.domain.sync.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/actions")
    public ResponseEntity<SyncActionsResponse> syncActions(@Valid @RequestBody SyncActionsRequest request) {
        return ResponseEntity.ok(syncService.process(request));
    }
}