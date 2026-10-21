package com.minihrms.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One row in "leave_requests". */
@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private Employee decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Optimistic locking: Hibernate adds "WHERE version = ?" to every UPDATE (see V3 migration)
    @Version
    private Long version;

    protected LeaveRequest() { }

    public LeaveRequest(Employee employee, LocalDate startDate, LocalDate endDate, String reason) {
        this.employee = employee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    /** Number of days requested, counting both start and end day (Mon to Wed = 3). */
    public int getDays() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }
    public Employee getDecidedBy() { return decidedBy; }
    public void setDecidedBy(Employee decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
