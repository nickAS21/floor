package org.nickas21.smart.security.controller;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String message;


    public AuthResponse(String token, String message) {
        this.token = token;
        this.message = message;
    }


}