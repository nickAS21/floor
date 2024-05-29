package org.nickas21.smart.security.controller;

import lombok.Data;

@Data
public class JwtToken {
    String accessToken;
    String refreshToken;
    Long expiresTimeInSec;

    final String tokenType = "Bearer";
}
