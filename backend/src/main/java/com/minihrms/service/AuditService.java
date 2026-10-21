package com.minihrms.service;

import com.minihrms.dto.CommonDtos.AuditLogDto;
import com.minihrms.dto.CommonDtos.PageDto;
import com.minihrms.model.AuditLog;
import com.minihrms.model.Employee;
import com.minihrms.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the audit trail.
 * log() is called from INSIDE other services' transactions. So the change and its audit
 * entry are saved together: if the change fails and rolls back, the log entry rolls back too.
 * You can never have "approved" without a log, or a log for something that didn't happen.
 */
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(Employee actor, String action, String entity, Long entityId, String details) {
        auditLogRepository.save(new AuditLog(actor, action, entity, entityId, details));
    }

    @Transactional(readOnly = true)
    public PageDto<AuditLogDto> list(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));      // never return more than 100 rows
        Page<AuditLog> result = auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(page, 0), safeSize));
        return new PageDto<>(result.getContent().stream().map(AuditLogDto::from).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }
}
