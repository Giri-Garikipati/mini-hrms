package com.minihrms.service;

import com.minihrms.dto.LeaveDtos.ApplyLeaveRequest;
import com.minihrms.dto.LeaveDtos.DecisionRequest;
import com.minihrms.dto.LeaveDtos.LeaveDto;
import com.minihrms.exception.ApiException;
import com.minihrms.model.Employee;
import com.minihrms.model.LeaveRequest;
import com.minihrms.model.LeaveStatus;
import com.minihrms.model.Role;
import com.minihrms.repository.LeaveRequestRepository;
import com.minihrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The leave workflow:  apply -> PENDING -> (manager/admin decides) -> APPROVED or REJECTED.
 * Every rule is enforced HERE on the server. The frontend hiding a button is only for
 * convenience; anyone can send requests directly with tools like Postman or curl.
 */
@Service
public class LeaveService {

    static final int MAX_DAYS_PER_REQUEST = 30;

    private final LeaveRequestRepository leaveRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public LeaveService(LeaveRequestRepository leaveRepository, AuditService auditService,
                        CurrentUserService currentUser) {
        this.leaveRepository = leaveRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional
    public LeaveDto apply(ApplyLeaveRequest req) {
        Employee me = currentUser.get();

        if (req.endDate().isBefore(req.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
        if (req.startDate().isBefore(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Leave cannot start in the past");
        }

        String reason = req.reason() == null || req.reason().isBlank() ? null : req.reason().trim();
        LeaveRequest leave = new LeaveRequest(me, req.startDate(), req.endDate(), reason);
        int days = leave.getDays();

        if (days > MAX_DAYS_PER_REQUEST) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "A single request cannot be longer than " + MAX_DAYS_PER_REQUEST + " days");
        }
        if (leaveRepository.hasOverlap(me.getId(), List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                req.startDate(), req.endDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have leave booked on some of these dates");
        }

        // Count days already waiting for approval, so nobody can request more than they have
        int pendingDays = leaveRepository.findByEmployeeIdAndStatus(me.getId(), LeaveStatus.PENDING)
                .stream().mapToInt(LeaveRequest::getDays).sum();
        if (days + pendingDays > me.getLeaveBalance()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Not enough leave balance: you have " + me.getLeaveBalance() + " day(s), with "
                            + pendingDays + " day(s) already pending, and asked for " + days);
        }

        leaveRepository.save(leave);
        auditService.log(me, "LEAVE_APPLIED", "LeaveRequest", leave.getId(),
                days + " day(s): " + req.startDate() + " to " + req.endDate());
        return LeaveDto.from(leave);
    }

    @Transactional(readOnly = true)
    public List<LeaveDto> myLeaves() {
        Employee me = currentUser.get();
        return leaveRepository.findByEmployeeIdOrderByCreatedAtDesc(me.getId())
                .stream().map(LeaveDto::from).toList();
    }

    /** Requests waiting for MY decision. */
    @Transactional(readOnly = true)
    public List<LeaveDto> pendingApprovals() {
        Employee me = currentUser.get();
        List<LeaveRequest> pending = switch (me.getRole()) {
            case ADMIN -> leaveRepository.findByStatusOrderByCreatedAtAsc(LeaveStatus.PENDING);
            case MANAGER -> leaveRepository.findByEmployee_Manager_IdAndStatusOrderByCreatedAtAsc(
                    me.getId(), LeaveStatus.PENDING);
            case EMPLOYEE -> throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to do this");
        };
        return pending.stream()
                .filter(l -> !l.getEmployee().getId().equals(me.getId()))   // never show my own
                .map(LeaveDto::from).toList();
    }

    @Transactional
    public LeaveDto decide(Long leaveId, DecisionRequest req) {
        Employee me = currentUser.get();
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Leave request not found"));
        Employee owner = leave.getEmployee();

        // Rule 1: nobody approves their own leave
        if (owner.getId().equals(me.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot decide on your own leave request");
        }
        // Rule 2: admins can decide any request; managers only their own team's
        boolean isTheirManager = owner.getManager() != null && owner.getManager().getId().equals(me.getId());
        boolean allowed = me.getRole() == Role.ADMIN || (me.getRole() == Role.MANAGER && isTheirManager);
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only decide on leave requests from your own team");
        }
        // Rule 3: a request is decided exactly once
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This request was already " + leave.getStatus().name().toLowerCase());
        }

        int days = leave.getDays();
        boolean approve = Boolean.TRUE.equals(req.approve());
        if (approve) {
            // Rule 4: re-check the balance at approval time (it may have changed since applying)
            if (owner.getLeaveBalance() < days) {
                throw new ApiException(HttpStatus.CONFLICT,
                        owner.getName() + " no longer has enough leave balance for this request");
            }
            owner.setLeaveBalance(owner.getLeaveBalance() - days);
            leave.setStatus(LeaveStatus.APPROVED);
        } else {
            leave.setStatus(LeaveStatus.REJECTED);
        }
        leave.setDecidedBy(me);
        leave.setDecidedAt(Instant.now());

        String note = req.note() == null || req.note().isBlank() ? "" : " | note: " + req.note().trim();
        auditService.log(me, approve ? "LEAVE_APPROVED" : "LEAVE_REJECTED", "LeaveRequest", leave.getId(),
                owner.getName() + ", " + days + " day(s)" + note);
        return LeaveDto.from(leave);
    }
}
