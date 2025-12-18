package org.nickas21.smart.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.pulsar.shade.io.swagger.annotations.ApiOperation;
import org.nickas21.smart.data.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping(value = "/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private final UserService userService;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService,
                          UserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            JwtToken jwtToken = jwtUtil.generateToken(userDetails);
            this.userService.saveJwtToken(loginRequest.getUsername(), jwtToken);
            return ResponseEntity.ok(new LoginResponse(jwtToken, "Login successful"));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, "Invalid password or username"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse(null, "An error occurred: " + e.getMessage()));
        }
    }
    @ApiOperation(value = "Refresh Token (refresh)",
            notes = "Special API call to record the 'refresh' for the user which \"refresh token\" is using to make this REST API call. Note that previously generated [JWT](https://jwt.io/) tokens will be valid until they expire.")
    @Operation(security = { @SecurityRequirement(name = "bearerAuth") })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse>  refreshToken(
            @RequestHeader(required = false, value = "Authorization") String token) {
        if (!userService.validateRefreshToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String[] userNames = userService.getUserNameFromToken(token);
        String username = userNames[0];
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        JwtToken newToken = jwtUtil.generateToken(Objects.requireNonNull(userDetails));
        userService.replaceJwtToken(username, newToken);
        return ResponseEntity.ok(new LoginResponse(newToken, "Refresh token successful"));
    }

    @ApiOperation(value = "Authorization",
            notes = "Special API call to record the 'logout' of the user to the Audit Logs. Since platform uses [JWT](https://jwt.io/), the actual logout is the procedure of clearing the [JWT](https://jwt.io/) token on the client side. ")
    @Operation(security = { @SecurityRequirement(name = "bearerAuth") })
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(
            @RequestHeader(required = false, value = "Authorization") String token) {

        if (!userService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String[] userNames = userService.getUserNameFromToken(token);
        String username = userNames[0];
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        JwtToken newToken = jwtUtil.generateToken(Objects.requireNonNull(userDetails));
        userService.removeJwtToken(username);
        return ResponseEntity.ok(new LoginResponse(newToken, "Remove token successful"));
    }
}

