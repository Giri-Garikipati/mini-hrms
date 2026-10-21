package com.minihrms.dto;

import com.minihrms.model.Department;
import com.minihrms.model.Employee;
import com.minihrms.model.Role;
import jakarta.validation.constraints.*;

import java.util.List;

public final class EmployeeDtos {

    private EmployeeDtos() { }

    /** What the API sends back about an employee. Note: no password_hash. */
    public record EmployeeDto(Long id, String name, String email, Role role,
                              Long departmentId, String departmentName,
                              Long managerId, String managerName,
                              int leaveBalance, boolean active) {

        public static EmployeeDto from(Employee e) {
            Department d = e.getDepartment();
            Employee m = e.getManager();
            return new EmployeeDto(e.getId(), e.getName(), e.getEmail(), e.getRole(),
                    d == null ? null : d.getId(), d == null ? null : d.getName(),
                    m == null ? null : m.getId(), m == null ? null : m.getName(),
                    e.getLeaveBalance(), e.isActive());
        }
    }

    /** Admin creates an employee. Validation annotations reject bad input before our code runs. */
    public record CreateEmployeeRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password,
            @NotNull Role role,
            Long departmentId,
            Long managerId,
            @Min(0) @Max(365) Integer leaveBalance) { }

    /** Admin edits an employee (password is changed separately by the employee). */
    public record UpdateEmployeeRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull Role role,
            Long departmentId,
            Long managerId,
            @NotNull @Min(0) @Max(365) Integer leaveBalance) { }

    public record SetActiveRequest(@NotNull Boolean active) { }

    public record DepartmentDto(Long id, String name) {
        public static DepartmentDto from(Department d) {
            return new DepartmentDto(d.getId(), d.getName());
        }
    }

    /** Result of a CSV import: how many were imported, and what was wrong with each bad row. */
    public record ImportResult(int imported, List<RowError> errors) { }

    public record RowError(int row, String message) { }
}
