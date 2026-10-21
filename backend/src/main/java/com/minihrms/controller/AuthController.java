package com.minihrms.controller;

import com.minihrms.dto.AuthDtos.ChangePasswordRequest;
import com.minihrms.dto.AuthDtos.LoginRequest;
import com.minihrms.dto.AuthDtos.LoginResponse;
import com.minihrms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controllers are thin: read the HTTP request, call a service, return the result.
 * @Valid runs the validation annotations on the DTO before our code runs.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/login   body: {"email": "...", "password": "..."}
    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    // POST /api/me/password  (any logged-in user)
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req);
        return ResponseEntity.noContent().build();        // 204: success, nothing to return
    }
}
