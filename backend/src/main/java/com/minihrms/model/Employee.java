package com.minihrms.model;

import jakarta.persistence.*;
import java.time.Instant;

/** One row in the "employees" table. Employees are also the app's users (they log in). */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    // Store the enum as text ("MANAGER"), not as a number (1).
    // Numbers would silently break if someone reorders the enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Many employees belong to one department.
    // LAZY = don't load the department from the DB until we actually ask for it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Self-reference: an employee's manager is another Employee.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "leave_balance", nullable = false)
    private int leaveBalance = 20;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Optimistic locking: Hibernate adds "WHERE version = ?" to every UPDATE (see V3 migration)
    @Version
    private Long version;

    protected Employee() { }

    public Employee(String name, String email, String passwordHash, Role role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }
    public int getLeaveBalance() { return leaveBalance; }
    public void setLeaveBalance(int leaveBalance) { this.leaveBalance = leaveBalance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
