package com.transitops.domain.reports.controller;

import com.transitops.domain.reports.dto.FleetSummary;
import com.transitops.domain.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<FleetSummary> summary() {
        return ResponseEntity.ok(reportService.getSummary());
    }

    @GetMapping("/export.csv")
    public ResponseEntity<String> exportCsv() {
        String csv = reportService.exportCsv();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transitops-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}