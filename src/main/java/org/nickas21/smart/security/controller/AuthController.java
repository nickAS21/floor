package org.nickas21.smart.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.pulsar.shade.io.swagger.annotations.ApiOperation;
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
    public Mono<LoginResponse> login(@RequestBody LoginRequest loginRequest, ServerWebExchange exchange) {
        return userDetailsService.findByUsername(loginRequest.getUsername())
                .flatMap(userDetails -> {
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword(), userDetails.getAuthorities());
                    return authenticationManager.authenticate(token)
                            .flatMap(auth -> {
                                SecurityContext securityContext = new SecurityContextImpl(auth);
                                JwtToken jwtToken = jwtUtil.generateToken(userDetails); // create JWT token
                                this.userService.saveJwtToken(loginRequest.getUsername(), jwtToken);
                                return securityContextRepository.save(exchange, securityContext)
                                        .then(Mono.just(new LoginResponse(jwtToken, "Login successful"))); // Response with JWT token
                            });
                })
                .switchIfEmpty(Mono.just(new LoginResponse(null, "Invalid username or password")));
    }

    @ApiOperation(value = "Refresh Token (refresh)",
            notes = "Special API call to record the 'refresh' for the user which \"refresh token\" is using to make this REST API call. Note that previously generated [JWT](https://jwt.io/) tokens will be valid until they expire.")
    @Operation(security = { @SecurityRequirement(name = "bearerAuth") })
    @PostMapping("/refresh")
    public Mono<LoginResponse> refreshToken(@RequestHeader(required = false, value = "Authorization") String token) {
        return userService.validateRefreshToken(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        String[] userNames = userService.getUserNameFromToken(token);
                        return userDetailsService.findByUsername(userNames[0])
                                .flatMap(userDetails -> {
                                    JwtToken newToken = jwtUtil.generateToken(userDetails);
                                    userService.replaceJwtToken(userNames[0], newToken);
                                    return Mono.just(new LoginResponse(newToken, "Refresh token successful"));
                                });
                    } else {
                        return Mono.just(new SmartErrorResponse("Invalid Refresh token",
                                SmartErrorCode.AUTHENTICATION));
                    }
                })
                .onErrorResume(e -> Mono.just(new SmartErrorResponse("An error Refresh token: " +  e.getMessage(),
                        SmartErrorCode.GENERAL)));
    }

    @ApiOperation(value = "Authorization",
            notes = "Special API call to record the 'logout' of the user to the Audit Logs. Since platform uses [JWT](https://jwt.io/), the actual logout is the procedure of clearing the [JWT](https://jwt.io/) token on the client side. ")
    @Operation(security = { @SecurityRequirement(name = "bearerAuth") })
    @PostMapping("/logout")
    public Mono<LoginResponse> logout(@RequestHeader(required = false, value = "Authorization") String token) {
        return userService.validateToken(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        String[] userNames = userService.getUserNameFromToken(token);
                        return userDetailsService.findByUsername(userNames[0])
                                .flatMap(userDetails -> {
                                    userService.removeJwtToken(userNames[0]);
                                    return Mono.just(new LoginResponse(null, "Remove token successful"));
                                });
                    } else {
                        return Mono.just(new SmartErrorResponse("Invalid Remove token",
                                SmartErrorCode.AUTHENTICATION));
                    }
                })
                .onErrorResume(e -> Mono.just(new SmartErrorResponse("An error Remove token: " + e.getMessage(),
                        SmartErrorCode.GENERAL)));
    }
}

