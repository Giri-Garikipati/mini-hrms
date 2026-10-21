package com.minihrms.service;

import com.minihrms.dto.AuthDtos.ChangePasswordRequest;
import com.minihrms.dto.AuthDtos.LoginRequest;
import com.minihrms.dto.AuthDtos.LoginResponse;
import com.minihrms.dto.EmployeeDtos.EmployeeDto;
import com.minihrms.exception.ApiException;
import com.minihrms.model.Employee;
import com.minihrms.repository.EmployeeRepository;
import com.minihrms.security.CurrentUserService;
import com.minihrms.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final CurrentUserService currentUser;
    private final AuditService auditService;

    public AuthService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService, CurrentUserService currentUser, AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        Employee e = employeeRepository.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        // Same message for "no such email" and "wrong password", so attackers
        // can't use the login form to discover which emails exist.
        if (e == null || !e.isActive() || !passwordEncoder.matches(req.password(), e.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return new LoginResponse(tokenService.createToken(e), EmployeeDto.from(e));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        Employee me = currentUser.get();
        if (!passwordEncoder.matches(req.currentPassword(), me.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        me.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        // No save() needed: "me" is managed by JPA inside this transaction,
        // so the change is written automatically when the transaction commits ("dirty checking").
        auditService.log(me, "PASSWORD_CHANGED", "Employee", me.getId(), "Changed own password");
    }
}
