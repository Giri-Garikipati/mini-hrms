package com.minihrms.service;

import com.minihrms.dto.LeaveDtos.ApplyLeaveRequest;
import com.minihrms.dto.LeaveDtos.DecisionRequest;
import com.minihrms.exception.ApiException;
import com.minihrms.model.Employee;
import com.minihrms.model.LeaveRequest;
import com.minihrms.model.LeaveStatus;
import com.minihrms.model.Role;
import com.minihrms.repository.LeaveRequestRepository;
import com.minihrms.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the leave rules. Mocks replace the database, so these run in milliseconds
 * and test ONLY our business logic. This is how you prove AI-generated code actually works.
 */
@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock LeaveRequestRepository leaveRepository;
    @Mock AuditService auditService;
    @Mock CurrentUserService currentUser;

    LeaveService service;
    Employee ravi;    // manager
    Employee priya;   // reports to ravi

    @BeforeEach
    void setUp() {
        service = new LeaveService(leaveRepository, auditService, currentUser);
        ravi = employee(2L, "Ravi", Role.MANAGER, null);
        priya = employee(3L, "Priya", Role.EMPLOYEE, ravi);
    }

    @Test
    void managerApprovesTeamLeaveAndBalanceIsDeducted() {
        LeaveRequest leave = leave(10L, priya, 3);
        when(currentUser.get()).thenReturn(ravi);
        when(leaveRepository.findById(10L)).thenReturn(Optional.of(leave));

        service.decide(10L, new DecisionRequest(true, null));

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(leave.getDecidedBy()).isEqualTo(ravi);
        assertThat(priya.getLeaveBalance()).isEqualTo(17);
        verify(auditService).log(eq(ravi), eq("LEAVE_APPROVED"), eq("LeaveRequest"), eq(10L), anyString());
    }

    @Test
    void rejectingDoesNotChangeBalance() {
        LeaveRequest leave = leave(10L, priya, 3);
        when(currentUser.get()).thenReturn(ravi);
        when(leaveRepository.findById(10L)).thenReturn(Optional.of(leave));

        service.decide(10L, new DecisionRequest(false, "Busy week"));

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(priya.getLeaveBalance()).isEqualTo(20);
    }

    @Test
    void cannotApproveOwnLeave() {
        LeaveRequest leave = leave(10L, ravi, 1);
        when(currentUser.get()).thenReturn(ravi);
        when(leaveRepository.findById(10L)).thenReturn(Optional.of(leave));

        ApiException ex = assertThrows(ApiException.class, () -> service.decide(10L, new DecisionRequest(true, null)));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void managerCannotApproveAnotherTeamsLeave() {
        Employee otherManager = employee(9L, "Meena", Role.MANAGER, null);
        Employee kiran = employee(4L, "Kiran", Role.EMPLOYEE, otherManager);
        when(currentUser.get()).thenReturn(ravi);
        when(leaveRepository.findById(11L)).thenReturn(Optional.of(leave(11L, kiran, 1)));

        ApiException ex = assertThrows(ApiException.class, () -> service.decide(11L, new DecisionRequest(true, null)));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void cannotDecideTwice() {
        LeaveRequest leave = leave(10L, priya, 2);
        leave.setStatus(LeaveStatus.APPROVED);
        when(currentUser.get()).thenReturn(ravi);
        when(leaveRepository.findById(10L)).thenReturn(Optional.of(leave));

        ApiException ex = assertThrows(ApiException.class, () -> service.decide(10L, new DecisionRequest(false, null)));
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void cannotApplyForMoreDaysThanBalance() {
        priya.setLeaveBalance(2);
        when(currentUser.get()).thenReturn(priya);
        LocalDate start = LocalDate.now().plusDays(5);

        assertThatThrownBy(() -> service.apply(new ApplyLeaveRequest(start, start.plusDays(2), "Trip")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Not enough leave balance");
    }

    @Test
    void cannotApplyWithEndBeforeStart() {
        when(currentUser.get()).thenReturn(priya);
        LocalDate start = LocalDate.now().plusDays(5);

        assertThatThrownBy(() -> service.apply(new ApplyLeaveRequest(start, start.minusDays(1), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("End date cannot be before start date");
    }

    // ---------- helpers ----------

    private static Employee employee(Long id, String name, Role role, Employee manager) {
        Employee e = new Employee(name, name.toLowerCase() + "@demo.com", "hash", role);
        ReflectionTestUtils.setField(e, "id", id);   // normally the database sets the id
        e.setManager(manager);
        return e;
    }

    private static LeaveRequest leave(Long id, Employee owner, int days) {
        LocalDate start = LocalDate.now().plusDays(7);
        LeaveRequest l = new LeaveRequest(owner, start, start.plusDays(days - 1), "test");
        ReflectionTestUtils.setField(l, "id", id);
        return l;
    }
}
