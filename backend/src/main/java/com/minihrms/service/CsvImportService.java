package com.minihrms.service;

import com.minihrms.dto.EmployeeDtos.ImportResult;
import com.minihrms.dto.EmployeeDtos.RowError;
import com.minihrms.exception.ApiException;
import com.minihrms.model.Department;
import com.minihrms.model.Employee;
import com.minihrms.model.Role;
import com.minihrms.repository.DepartmentRepository;
import com.minihrms.repository.EmployeeRepository;
import com.minihrms.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Bulk-creates employees from a CSV file.
 *
 * Expected header:  name,email,role,department,manager_email
 * Example row:      Kiran Das,kiran@demo.com,EMPLOYEE,Engineering,manager@demo.com
 *
 * Design decision: ALL-OR-NOTHING. We validate every row first. If even one row is bad,
 * nothing is imported and the admin gets a list of exactly which rows are wrong and why.
 * This avoids a half-imported file that's hard to fix.
 *
 * Known limitation: simple comma splitting, so values can't contain commas.
 */
@Service
public class CsvImportService {

    private static final List<String> EXPECTED_HEADER =
            List.of("name", "email", "role", "department", "manager_email");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final String defaultPassword;

    public CsvImportService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
                            PasswordEncoder passwordEncoder, AuditService auditService,
                            CurrentUserService currentUser,
                            @Value("${app.import.default-password}") String defaultPassword) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.defaultPassword = defaultPassword;
    }

    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        Employee admin = currentUser.get();
        List<String> lines = readLines(file);

        List<String> header = Arrays.stream(lines.get(0).replace("﻿", "").split(",", -1))
                .map(h -> h.trim().toLowerCase()).toList();
        if (!header.equals(EXPECTED_HEADER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "The first line must be exactly: " + String.join(",", EXPECTED_HEADER));
        }

        Map<String, Department> departmentsByName = new HashMap<>();
        departmentRepository.findAll().forEach(d -> departmentsByName.put(d.getName().toLowerCase(), d));

        Set<String> emailsInFile = new HashSet<>();
        List<RowError> errors = new ArrayList<>();
        List<Employee> toSave = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            int rowNumber = i + 1;                       // row number as the admin sees it in Excel
            String[] cols = line.split(",", -1);
            if (cols.length != EXPECTED_HEADER.size()) {
                errors.add(new RowError(rowNumber, "Expected 5 columns but found " + cols.length));
                continue;
            }

            String name = cols[0].trim();
            String email = cols[1].trim().toLowerCase();
            String roleText = cols[2].trim().toUpperCase();
            String deptName = cols[3].trim();
            String managerEmail = cols[4].trim();
            List<String> problems = new ArrayList<>();

            if (name.isEmpty()) problems.add("name is empty");

            if (!EMAIL.matcher(email).matches()) problems.add("invalid email '" + email + "'");
            else if (!emailsInFile.add(email)) problems.add("email appears more than once in the file");
            else if (employeeRepository.existsByEmailIgnoreCase(email)) problems.add("email already exists");

            Role role = null;
            try {
                role = Role.valueOf(roleText);
            } catch (IllegalArgumentException ex) {
                problems.add("role must be ADMIN, MANAGER or EMPLOYEE");
            }

            Department department = null;
            if (!deptName.isEmpty()) {
                department = departmentsByName.get(deptName.toLowerCase());
                if (department == null) problems.add("unknown department '" + deptName + "'");
            }

            Employee manager = null;
            if (!managerEmail.isEmpty()) {
                manager = employeeRepository.findByEmailIgnoreCase(managerEmail).orElse(null);
                if (manager == null) problems.add("manager '" + managerEmail + "' not found");
                else if (manager.getRole() == Role.EMPLOYEE) problems.add("'" + managerEmail + "' is not a manager");
            }

            if (!problems.isEmpty()) {
                errors.add(new RowError(rowNumber, String.join("; ", problems)));
                continue;
            }

            Employee e = new Employee(name, email, passwordEncoder.encode(defaultPassword), role);
            e.setDepartment(department);
            e.setManager(manager);
            toSave.add(e);
        }

        if (!errors.isEmpty()) {
            return new ImportResult(0, errors);          // nothing saved
        }
        if (toSave.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The file has no employee rows");
        }

        employeeRepository.saveAll(toSave);
        auditService.log(admin, "EMPLOYEES_IMPORTED", "Employee", null,
                "Imported " + toSave.size() + " employee(s) from " + file.getOriginalFilename());
        return new ImportResult(toSave.size(), List.of());
    }

    private List<String> readLines(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload a non-empty CSV file");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().toList();
            if (lines.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The file is empty");
            }
            return lines;
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
    }
}
