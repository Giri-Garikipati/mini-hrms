package com.minihrms.dto;

import com.minihrms.model.Employee;
import com.minihrms.model.LeaveRequest;
import com.minihrms.model.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class LeaveDtos {

    private LeaveDtos() { }

    public record ApplyLeaveRequest(
            @NotNull LocalDate startDate,      // JSON: "2026-11-03"
            @NotNull LocalDate endDate,
            @Size(max = 500) String reason) { }

    public record DecisionRequest(
            @NotNull Boolean approve,
            @Size(max = 500) String note) { }

    public record LeaveDto(Long id, Long employeeId, String employeeName,
                           LocalDate startDate, LocalDate endDate, int days, String reason,
                           LeaveStatus status, String decidedByName, Instant decidedAt, Instant createdAt) {

        public static LeaveDto from(LeaveRequest l) {
            Employee decider = l.getDecidedBy();
            return new LeaveDto(l.getId(), l.getEmployee().getId(), l.getEmployee().getName(),
                    l.getStartDate(), l.getEndDate(), l.getDays(), l.getReason(),
                    l.getStatus(), decider == null ? null : decider.getName(),
                    l.getDecidedAt(), l.getCreatedAt());
        }
    }
}
