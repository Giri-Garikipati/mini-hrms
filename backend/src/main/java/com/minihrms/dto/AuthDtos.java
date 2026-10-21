package com.minihrms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO = Data Transfer Object: the exact shape of JSON going in or out of the API.
 * We never send entities directly. That way we control what leaves the server
 * (e.g. password_hash is never in any DTO, so it can never leak).
 * A Java "record" is a short way to write an immutable class with fields + getters.
 */
public final class AuthDtos {

    private AuthDtos() { }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) { }

    public record LoginResponse(String token, EmployeeDtos.EmployeeDto user) { }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String newPassword) { }
}
