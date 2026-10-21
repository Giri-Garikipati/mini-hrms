package com.minihrms.repository;

import com.minihrms.model.LeaveRequest;
import com.minihrms.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // An employee's own leave history, newest first
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    // All pending requests (for admins), oldest first so nobody waits too long
    List<LeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveStatus status);

    // Pending requests from a manager's team.
    // "Employee_Manager_Id" walks the relationship: leave -> employee -> manager -> id
    List<LeaveRequest> findByEmployee_Manager_IdAndStatusOrderByCreatedAtAsc(Long managerId, LeaveStatus status);

    /**
     * Does this employee already have leave that overlaps the given dates?
     * Two date ranges overlap when: existing.start <= new.end AND existing.end >= new.start.
     * This is JPQL: it uses Java class/field names (LeaveRequest, startDate), not table names.
     */
    @Query("""
            select case when count(l) > 0 then true else false end
            from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    boolean hasOverlap(@Param("employeeId") Long employeeId,
                       @Param("statuses") Collection<LeaveStatus> statuses,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);
}
