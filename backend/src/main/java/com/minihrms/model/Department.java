package com.minihrms.model;

import jakarta.persistence.*;

/**
 * An @Entity is a Java class that maps to a database table.
 * One object of this class = one row in the "departments" table.
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id                                                   // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // the database generates it (BIGSERIAL)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    protected Department() { }                            // JPA needs an empty constructor

    public Department(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
