package com.transitops.domain.sync.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "synced_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncedAction {

    @Id
    private String idempotencyKey;

    private String driverId;
    private String type;

    @jakarta.persistence.Lob
    private String payloadJson;

    private Instant processedAt;
}