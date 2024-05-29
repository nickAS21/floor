package org.nickas21.smart.security.controller;

import lombok.Data;

@Data
public class AuthResponse {
    private JwtToken token;
    private String message;


    public AuthResponse(JwtToken token, String message) {
        this.token = token;
        this.message = message;
    }


}