package org.nickas21.smart.data.service;

import org.nickas21.smart.security.controller.JwtToken;
import org.nickas21.smart.security.controller.JwtUtil;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final JwtUtil jwtUtil;
    private final ReactiveUserDetailsService userDetailsService;

    private final Map<String, JwtToken> tokenUsers;

    public UserService(JwtUtil jwtUtil, ReactiveUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenUsers = new ConcurrentHashMap<>();
    }

    public Mono<Boolean> validateToken(String token) {
        try {
            String[] userNames =  getUserNameFromToken(token);
            String username = userNames[0];
            String jwtToken = userNames[1];
            if (this.tokenUsers.containsKey(username) && (this.tokenUsers.get(username).getAccessToken().equals(jwtToken)
            || this.tokenUsers.get(username).getRefreshToken().equals(jwtToken))) {
                return userDetailsService.findByUsername(username)
                        .map(userDetails -> jwtUtil.validateToken(jwtToken, userDetails));
            } else {
                return Mono.just(false);
            }
        } catch (Exception e) {
            return Mono.just(false);
        }
    }

    public String[] getUserNameFromToken(String token) {
        String jwtToken = token.substring(7); // Remove "Bearer " prefix
        String userName = jwtUtil.extractUsername(jwtToken);
       return new String[]{userName, jwtToken};
    }

    public void setJwtToken(String userName, JwtToken jwtToken){
        this.tokenUsers.put(userName, jwtToken);
    }
}
