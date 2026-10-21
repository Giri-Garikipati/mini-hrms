package com.minihrms.security;

import com.minihrms.model.Employee;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Creates the login token (JWT) after a successful login. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long ttlHours;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.ttl-hours:8}") long ttlHours) {
        this.encoder = encoder;
        this.ttlHours = ttlHours;
    }

    public String createToken(Employee employee) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mini-hrms")
                .issuedAt(now)
                .expiresAt(now.plus(ttlHours, ChronoUnit.HOURS))
                .subject(String.valueOf(employee.getId()))   // WHO: employee id
                .claim("role", employee.getRole().name())    // WHAT ROLE: used by hasRole(...)
                .claim("name", employee.getName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
