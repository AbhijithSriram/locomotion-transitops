package com.transitops.domain.sync.repository;

import com.transitops.domain.sync.entity.SyncedAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncedActionRepository extends JpaRepository<SyncedAction, String> {
}