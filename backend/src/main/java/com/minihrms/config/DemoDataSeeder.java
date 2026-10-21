package com.minihrms.config;

import com.minihrms.model.*;
import com.minihrms.repository.DepartmentRepository;
import com.minihrms.repository.EmployeeRepository;
import com.minihrms.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Runs once at startup. If the database has no employees, it creates demo users so an
 * interviewer can log in as each role. All demo passwords: password123
 * Turn it off with SEED_DEMO_DATA=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final LeaveRequestRepository leaves;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(EmployeeRepository employees, DepartmentRepository departments,
                          LeaveRequestRepository leaves, PasswordEncoder encoder) {
        this.employees = employees;
        this.departments = departments;
        this.leaves = leaves;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (employees.count() > 0) {
            return;                                   // already seeded, do nothing
        }
        Department engineering = department("Engineering");
        Department hr = department("Human Resources");

        Employee admin = person("Asha Rao", "admin@demo.com", Role.ADMIN, hr, null);
        Employee ravi = person("Ravi Kumar", "manager@demo.com", Role.MANAGER, engineering, admin);
        Employee priya = person("Priya Sharma", "priya@demo.com", Role.EMPLOYEE, engineering, ravi);
        Employee arjun = person("Arjun Reddy", "arjun@demo.com", Role.EMPLOYEE, engineering, ravi);

        LocalDate nextWeek = LocalDate.now().plusDays(7);
        leaves.save(new LeaveRequest(priya, nextWeek, nextWeek.plusDays(1), "Family function"));
        leaves.save(new LeaveRequest(arjun, nextWeek.plusDays(3), nextWeek.plusDays(3), "Doctor appointment"));

        log.info("Demo data created. Log in as admin@demo.com / manager@demo.com / priya@demo.com "
                + "with password '{}'", DEMO_PASSWORD);
    }

    private Department department(String name) {
        return departments.findByNameIgnoreCase(name).orElseGet(() -> departments.save(new Department(name)));
    }

    private Employee person(String name, String email, Role role, Department dept, Employee manager) {
        Employee e = new Employee(name, email, encoder.encode(DEMO_PASSWORD), role);
        e.setDepartment(dept);
        e.setManager(manager);
        return employees.save(e);
    }
}
