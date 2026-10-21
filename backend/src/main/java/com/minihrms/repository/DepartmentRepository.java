package com.minihrms.repository;

import com.minihrms.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * A repository is how we read/write a table.
 * We only write the INTERFACE. Spring Data generates the real code at startup,
 * giving us save(), findById(), findAll(), count(), deleteById() for free.
 * JpaRepository<Department, Long> = "for the Department entity, whose id is a Long".
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByNameIgnoreCase(String name);

    List<Department> findAllByOrderByNameAsc();
}
