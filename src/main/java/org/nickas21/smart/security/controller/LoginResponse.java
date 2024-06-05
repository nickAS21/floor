package org.nickas21.smart.security.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class LoginResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "JWT token",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZW5hbnRAdGhpbmdzYm9hcmQub3JnIi...")
    private JwtToken token;
    private String message;


    public LoginResponse(JwtToken token, String message) {
        this.token = token;
        this.message = message;
    }
}