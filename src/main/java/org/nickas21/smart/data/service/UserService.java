package org.nickas21.smart.data.service;

import org.nickas21.smart.security.configuration.SmartConnectionService;
import org.nickas21.smart.security.controller.JwtUtil;
import org.nickas21.smart.util.StringUtils;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    private final JwtUtil jwtUtil;
    private final ReactiveUserDetailsService userDetailsService;

    public UserService(JwtUtil jwtUtil, ReactiveUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    public Mono<Boolean> validateToken(String token) {
        try {
            String jwtToken = token.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(jwtToken);

            return userDetailsService.findByUsername(username)
                    .map(userDetails -> jwtUtil.validateToken(jwtToken, userDetails));
        } catch (Exception e) {
            return Mono.just(false);
        }
    }
}
