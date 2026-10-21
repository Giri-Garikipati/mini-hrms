package com.minihrms.security;

import com.minihrms.exception.ApiException;
import com.minihrms.model.Employee;
import com.minihrms.repository.EmployeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * "Who is making this request?"
 * Spring has already verified the token. We take the employee id from it and load the
 * employee FRESH from the database. This matters: if an admin deactivated this person
 * 5 minutes ago, their old token still has a valid signature, but this check blocks them.
 */
@Service
public class CurrentUserService {

    private final EmployeeRepository employeeRepository;

    public CurrentUserService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "You are not logged in");
        }
        Long id = Long.valueOf(jwt.getSubject());
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Your account no longer exists"));
        if (!employee.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your account has been deactivated");
        }
        return employee;
    }
}
