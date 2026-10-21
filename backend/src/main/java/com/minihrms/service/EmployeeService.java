package com.minihrms.service;

import com.minihrms.dto.EmployeeDtos.CreateEmployeeRequest;
import com.minihrms.dto.EmployeeDtos.DepartmentDto;
import com.minihrms.dto.EmployeeDtos.EmployeeDto;
import com.minihrms.dto.EmployeeDtos.UpdateEmployeeRequest;
import com.minihrms.exception.ApiException;
import com.minihrms.model.Department;
import com.minihrms.model.Employee;
import com.minihrms.model.Role;
import com.minihrms.repository.DepartmentRepository;
import com.minihrms.repository.EmployeeRepository;
import com.minihrms.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Business rules for employees. Controllers stay thin; the real decisions live here.
 * @Transactional = everything in the method succeeds together or is undone together.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
                           PasswordEncoder passwordEncoder, AuditService auditService,
                           CurrentUserService currentUser) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public EmployeeDto me() {
        return EmployeeDto.from(currentUser.get());
    }

    /** Admin sees everyone; a manager sees only their own team. */
    @Transactional(readOnly = true)
    public List<EmployeeDto> list() {
        Employee me = currentUser.get();
        List<Employee> employees = switch (me.getRole()) {
            case ADMIN -> employeeRepository.findAllByOrderByNameAsc();
            case MANAGER -> employeeRepository.findByManagerIdOrderByNameAsc(me.getId());
            case EMPLOYEE -> throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to do this");
        };
        return employees.stream().map(EmployeeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> departments() {
        return departmentRepository.findAllByOrderByNameAsc().stream().map(DepartmentDto::from).toList();
    }

    @Transactional
    public EmployeeDto create(CreateEmployeeRequest req) {
        Employee admin = currentUser.get();
        String email = req.email().trim().toLowerCase();
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An employee with this email already exists");
        }
        Employee e = new Employee(req.name().trim(), email, passwordEncoder.encode(req.password()), req.role());
        e.setDepartment(resolveDepartment(req.departmentId()));
        e.setManager(resolveManager(req.managerId(), null));
        if (req.leaveBalance() != null) {
            e.setLeaveBalance(req.leaveBalance());
        }
        employeeRepository.save(e);
        auditService.log(admin, "EMPLOYEE_CREATED", "Employee", e.getId(),
                "Created " + e.getEmail() + " as " + e.getRole());
        return EmployeeDto.from(e);
    }

    @Transactional
    public EmployeeDto update(Long id, UpdateEmployeeRequest req) {
        Employee admin = currentUser.get();
        Employee e = findOrThrow(id);

        // Safety rule: an admin can't remove their own admin role and lock themselves out.
        if (e.getId().equals(admin.getId()) && req.role() != Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot remove your own admin role");
        }

        Department newDept = resolveDepartment(req.departmentId());
        Employee newManager = resolveManager(req.managerId(), e.getId());

        // Record exactly what changed, for the audit log
        List<String> changes = new ArrayList<>();
        if (!e.getName().equals(req.name().trim())) changes.add("name: " + e.getName() + " -> " + req.name().trim());
        if (e.getRole() != req.role()) changes.add("role: " + e.getRole() + " -> " + req.role());
        if (!Objects.equals(idOf(e.getDepartment()), req.departmentId())) changes.add("department changed");
        if (!Objects.equals(idOf(e.getManager()), req.managerId())) changes.add("manager changed");
        if (e.getLeaveBalance() != req.leaveBalance()) changes.add("leave balance: " + e.getLeaveBalance() + " -> " + req.leaveBalance());

        e.setName(req.name().trim());
        e.setRole(req.role());
        e.setDepartment(newDept);
        e.setManager(newManager);
        e.setLeaveBalance(req.leaveBalance());

        if (!changes.isEmpty()) {
            auditService.log(admin, "EMPLOYEE_UPDATED", "Employee", e.getId(), String.join("; ", changes));
        }
        return EmployeeDto.from(e);
    }

    /** "Soft delete": we deactivate instead of deleting, so history (leaves, audit logs) stays intact. */
    @Transactional
    public EmployeeDto setActive(Long id, boolean active) {
        Employee admin = currentUser.get();
        Employee e = findOrThrow(id);
        if (e.getId().equals(admin.getId()) && !active) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        }
        if (e.isActive() != active) {
            e.setActive(active);
            auditService.log(admin, active ? "EMPLOYEE_ACTIVATED" : "EMPLOYEE_DEACTIVATED",
                    "Employee", e.getId(), e.getEmail());
        }
        return EmployeeDto.from(e);
    }

    // ---------- helpers ----------

    private Employee findOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private Department resolveDepartment(Long departmentId) {
        if (departmentId == null) return null;
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Department not found"));
    }

    private Employee resolveManager(Long managerId, Long selfId) {
        if (managerId == null) return null;
        if (managerId.equals(selfId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An employee cannot be their own manager");
        }
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Manager not found"));
        if (manager.getRole() == Role.EMPLOYEE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The selected manager must have the MANAGER or ADMIN role");
        }
        if (!manager.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The selected manager is deactivated");
        }
        return manager;
    }

    private static Long idOf(Department d) { return d == null ? null : d.getId(); }
    private static Long idOf(Employee e) { return e == null ? null : e.getId(); }
}
