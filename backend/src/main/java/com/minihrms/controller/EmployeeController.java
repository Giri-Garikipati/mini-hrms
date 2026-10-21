package com.minihrms.controller;

import com.minihrms.dto.EmployeeDtos.*;
import com.minihrms.service.CsvImportService;
import com.minihrms.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @PreAuthorize = method-level authorization. The role is read from the login token
 * BEFORE the method runs; if it doesn't match, the method never runs (403).
 */
@RestController
@RequestMapping("/api")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final CsvImportService csvImportService;

    public EmployeeController(EmployeeService employeeService, CsvImportService csvImportService) {
        this.employeeService = employeeService;
        this.csvImportService = csvImportService;
    }

    @GetMapping("/me")
    public EmployeeDto me() {
        return employeeService.me();
    }

    @GetMapping("/departments")
    public List<DepartmentDto> departments() {
        return employeeService.departments();
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<EmployeeDto> list() {
        return employeeService.list();
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeDto> create(@Valid @RequestBody CreateEmployeeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(req));   // 201
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeDto update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest req) {
        return employeeService.update(id, req);
    }

    @PatchMapping("/employees/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeDto setActive(@PathVariable Long id, @Valid @RequestBody SetActiveRequest req) {
        return employeeService.setActive(id, req.active());
    }

    // Multipart upload: the frontend sends the file in a form field called "file"
    @PostMapping("/employees/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) {
        return csvImportService.importCsv(file);
    }
}
