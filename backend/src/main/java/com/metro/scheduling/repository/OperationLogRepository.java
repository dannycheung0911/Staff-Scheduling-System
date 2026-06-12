package com.metro.scheduling.repository;

import com.metro.scheduling.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    Page<OperationLog> findByUsernameContaining(String username, Pageable pageable);
}
