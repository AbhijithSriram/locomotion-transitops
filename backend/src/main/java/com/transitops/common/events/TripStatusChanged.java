package com.transitops.common.events;

import com.transitops.domain.trip.entity.Trip;

public record TripStatusChanged(Trip trip) {
}