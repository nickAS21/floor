package org.nickas21.smart.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/auth/user")
public class AuthController {

    @Autowired
    private ReactiveAuthenticationManager authenticationManager;

    @Autowired
    private WebSessionServerSecurityContextRepository securityContextRepository;

    @Autowired
    private ReactiveUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public Mono<AuthResponse> login(@RequestBody AuthRequest authRequest, ServerWebExchange exchange) {
        return userDetailsService.findByUsername(authRequest.getUsername())
                .flatMap(userDetails -> {
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(), authRequest.getPassword(), userDetails.getAuthorities());

                    return authenticationManager.authenticate(token)
                            .flatMap(auth -> {
                                SecurityContext securityContext = new SecurityContextImpl(auth);
                                return securityContextRepository.save(exchange, securityContext)
                                        .then(Mono.just(new AuthResponse(jwtUtil.generateToken(userDetails), "Login successful"))); // Генерація і повернення JWT токену
                            });
                })
                .switchIfEmpty(Mono.just(new AuthResponse(null, "Invalid username or password")));
    }
}

