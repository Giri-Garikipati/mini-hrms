package com.minihrms.controller;

import com.minihrms.dto.CommonDtos.AuditLogDto;
import com.minihrms.dto.CommonDtos.PageDto;
import com.minihrms.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Everything under /api/admin is ADMIN-only via the URL rule in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuditService auditService;

    public AdminController(AuditService auditService) {
        this.auditService = auditService;
    }

    // GET /api/admin/audit-logs?page=0&size=20
    @GetMapping("/audit-logs")
    public PageDto<AuditLogDto> auditLogs(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return auditService.list(page, size);
    }
}
