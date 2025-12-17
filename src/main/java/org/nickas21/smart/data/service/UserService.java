package org.nickas21.smart.data.service;

import org.nickas21.smart.security.controller.JwtToken;
import org.nickas21.smart.security.controller.JwtUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    private final Map<String, JwtToken> tokenUsers;

    public UserService(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenUsers = new ConcurrentHashMap<>();
    }

    public boolean validateToken(String token){
        return validateToken(token, false);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, true);
    }

    public boolean validateToken(String token, boolean isRefresh) {
        if (token == null || token.isBlank()) return false;

        try {
            String[] parts = getUserNameFromToken(token);
            if (parts.length != 2) return false;
            String username = parts[0];
            String jwtToken = parts[1];

            var session = tokenUsers.get(username);
            if (session == null) return false;

            if (isRefresh) {
                if (!jwtToken.equals(session.getRefreshToken())) return false;
            } else {
                if (!jwtToken.equals(session.getAccessToken())) return false;
            }

            String expectedToken = isRefresh ? session.getRefreshToken() : session.getAccessToken();
            if (jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }
            jwtToken = jwtToken.trim();
            expectedToken = expectedToken.trim();

            if (!jwtToken.equals(expectedToken)) return false;

            UserDetails user = userDetailsService.loadUserByUsername(username); // синхронно

            return jwtUtil.validateToken(jwtToken, user);


        } catch (Exception e) {
            return false;
        }
    }

    public String[] getUserNameFromToken(String token) {
        String jwtToken = token.substring(7); // Remove "Bearer " prefix
        String userName = jwtUtil.extractUsername(jwtToken);
       return new String[]{userName, jwtToken};
    }

    public void saveJwtToken(String userName, JwtToken jwtToken){
        this.tokenUsers.put(userName, jwtToken);
    }
    public void replaceJwtToken(String userName, JwtToken jwtToken){
        this.tokenUsers.replace(userName, jwtToken);
    }
    public void removeJwtToken(String userName){
        this.tokenUsers.remove(userName);
    }
}
