package com.minihrms.repository;

import com.minihrms.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Page = one "page" of results (e.g. 20 at a time) so we never load thousands of rows at once
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
