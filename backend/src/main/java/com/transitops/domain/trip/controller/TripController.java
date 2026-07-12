package com.transitops.domain.trip.controller;

import com.transitops.domain.trip.dto.TripCompleteRequest;
import com.transitops.domain.trip.dto.TripCreateRequest;
import com.transitops.domain.trip.dto.TripResponse;
import com.transitops.domain.trip.entity.Trip;
import com.transitops.domain.trip.service.DispatchService;
import com.transitops.domain.trip.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final DispatchService dispatchService;

    @PostMapping
    public ResponseEntity<TripResponse> create(@Valid @RequestBody TripCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<TripResponse>> findAll() {
        return ResponseEntity.ok(tripService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(tripService.findById(id));
    }

    @PatchMapping("/{id}/dispatch")
    public ResponseEntity<TripResponse> dispatch(@PathVariable String id) {
        Trip trip = dispatchService.dispatch(id);
        return ResponseEntity.ok(tripService.toResponse(trip));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TripResponse> complete(@PathVariable String id, @Valid @RequestBody TripCompleteRequest request) {
        Trip trip = dispatchService.complete(id, request.finalOdometer(), request.actualDistanceKm());
        return ResponseEntity.ok(tripService.toResponse(trip));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TripResponse> cancel(@PathVariable String id) {
        Trip trip = dispatchService.cancel(id);
        return ResponseEntity.ok(tripService.toResponse(trip));
    }
}