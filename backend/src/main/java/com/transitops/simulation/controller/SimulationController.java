package com.transitops.simulation.controller;

import com.transitops.simulation.service.SimulationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sim")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @PostMapping("/spawn-trip")
    public ResponseEntity<Void> spawnTrip() {
        simulationService.spawnTrip();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/trigger-breakdown/{vehicleId}")
    public ResponseEntity<Void> triggerBreakdown(@PathVariable String vehicleId) {
        simulationService.triggerBreakdown(vehicleId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/speed")
    public ResponseEntity<SpeedResponse> setSpeed(@RequestBody SpeedRequest request) {
        simulationService.setSpeedMultiplier(request.getMultiplier());
        SpeedResponse res = new SpeedResponse();
        res.setMultiplier(simulationService.getSpeedMultiplier());
        return ResponseEntity.ok(res);
    }

    @Data
    public static class SpeedRequest {
        private int multiplier;
    }

    @Data
    public static class SpeedResponse {
        private int multiplier;
    }
}
