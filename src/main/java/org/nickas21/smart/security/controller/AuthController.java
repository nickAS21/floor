package org.nickas21.smart.security.controller;

import org.nickas21.smart.data.service.UserService;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/auth")
public class AuthController {

    private final UserService userService;


    private final  ReactiveAuthenticationManager authenticationManager;


    private final  WebSessionServerSecurityContextRepository securityContextRepository;


    private final  ReactiveUserDetailsService userDetailsService;


    private final  JwtUtil jwtUtil;

    public AuthController(UserService userService, ReactiveAuthenticationManager authenticationManager, WebSessionServerSecurityContextRepository securityContextRepository, ReactiveUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@RequestBody AuthRequest authRequest, ServerWebExchange exchange) {
        return userDetailsService.findByUsername(authRequest.getUsername())
                .flatMap(userDetails -> {
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(), authRequest.getPassword(), userDetails.getAuthorities());
                    return authenticationManager.authenticate(token)
                            .flatMap(auth -> {
                                SecurityContext securityContext = new SecurityContextImpl(auth);
                                JwtToken jwtToken = jwtUtil.generateToken(userDetails); // create JWT token
                                this.userService.saveJwtToken(authRequest.getUsername(), jwtToken);
                                return securityContextRepository.save(exchange, securityContext)
                                        .then(Mono.just(new AuthResponse(jwtToken, "Login successful"))); // Response with JWT token
                            });
                })
                .switchIfEmpty(Mono.just(new AuthResponse(null, "Invalid username or password")));
    }

    @PostMapping("/refresh")
    public Mono<AuthResponse> refreshToken(@RequestHeader("Authorization") String token) {
        return userService.validateRefreshToken(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        String[] userNames = userService.getUserNameFromToken(token);
                        return userDetailsService.findByUsername(userNames[0])
                                .flatMap(userDetails -> {
                                    JwtToken newToken = jwtUtil.generateToken(userDetails);
                                    userService.replaceJwtToken(userNames[0], newToken);
                                    return Mono.just(new AuthResponse(newToken, "Refresh token successful"));
                                });
                    } else {
                        return Mono.just(new AuthResponse(null, "Invalid Refresh token"));
                    }
                })
                .onErrorResume(e -> Mono.just(new AuthResponse(null, "An error Refresh token: " + e.getMessage())));
    }
    @PostMapping("/logout")
    public Mono<AuthResponse> logout(@RequestHeader("Authorization") String token) {
        return userService.validateToken(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        String[] userNames = userService.getUserNameFromToken(token);
                        return userDetailsService.findByUsername(userNames[0])
                                .flatMap(userDetails -> {
                                    userService.removeJwtToken(userNames[0]);
                                    return Mono.just(new AuthResponse(null, "Remove token successful"));
                                });
                    } else {
                        return Mono.just(new AuthResponse(null, "Invalid Remove token"));
                    }
                })
                .onErrorResume(e -> Mono.just(new AuthResponse(null, "An error Remove token: " + e.getMessage())));
    }
}

