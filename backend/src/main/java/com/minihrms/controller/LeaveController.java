package com.minihrms.controller;

import com.minihrms.dto.LeaveDtos.ApplyLeaveRequest;
import com.minihrms.dto.LeaveDtos.DecisionRequest;
import com.minihrms.dto.LeaveDtos.LeaveDto;
import com.minihrms.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/mine")
    public List<LeaveDto> mine() {
        return leaveService.myLeaves();
    }

    @PostMapping
    public ResponseEntity<LeaveDto> apply(@Valid @RequestBody ApplyLeaveRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.apply(req));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<LeaveDto> pending() {
        return leaveService.pendingApprovals();
    }

    // POST /api/leaves/5/decision   body: {"approve": true, "note": "Enjoy!"}
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public LeaveDto decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return leaveService.decide(id, req);
    }
}
