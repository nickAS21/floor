package org.nickas21.smart.security.controller;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class LoginRequest {

    private final String username;

    private final String password;

    @JsonCreator
    public LoginRequest(@JsonProperty("username") String username, @JsonProperty("password") String password) {
        this.username = username;
        this.password = password;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "User name", example = "user")
    public String getUsername() {
        return username;
    }

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "User password", example = "user")
    public String getPassword() {
        return password;
    }
}
