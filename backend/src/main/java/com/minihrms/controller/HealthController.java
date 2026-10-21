package com.minihrms.controller;

import com.minihrms.repository.DepartmentRepository;
import com.minihrms.repository.EmployeeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A @RestController handles HTTP requests and returns JSON.
 * This one is just to prove the full chain works:
 *   browser -> controller -> repository -> database -> back as JSON
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    // Constructor injection: Spring sees this constructor and passes in the repositories.
    // We never write "new DepartmentRepository()" ourselves.
    public HealthController(DepartmentRepository departmentRepository,
                            EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    // GET http://localhost:8080/api/health
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "departments", departmentRepository.count(),
                "employees", employeeRepository.count()
        );
    }
}
