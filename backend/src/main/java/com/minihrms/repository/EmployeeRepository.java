package com.minihrms.repository;

import com.minihrms.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Spring reads the METHOD NAME and writes the SQL for us:
    // findByEmailIgnoreCase -> SELECT * FROM employees WHERE lower(email) = lower(?)
    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // -> SELECT * FROM employees WHERE manager_id = ? ORDER BY name   (a manager's team)
    List<Employee> findByManagerIdOrderByNameAsc(Long managerId);

    List<Employee> findAllByOrderByNameAsc();
}
