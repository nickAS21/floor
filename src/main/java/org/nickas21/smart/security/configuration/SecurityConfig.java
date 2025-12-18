package org.nickas21.smart.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SmartConnectionService smartConnectionService;

    public SecurityConfig(SmartConnectionService smartConnectionService) {
        this.smartConnectionService = smartConnectionService;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username(smartConnectionService.getUserLogin())
                .password(passwordEncoder().encode(smartConnectionService.getUserPassword()))
                .roles("USER")
                .build();

        UserDetails admin = User.builder()
                .username(smartConnectionService.getAdminLogin())
                .password(passwordEncoder().encode(smartConnectionService.getAdminPassword()))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/error").permitAll() // Відкрито для всіх
                        .requestMatchers("/api/**").authenticated()      // ВСІ ендпоінти, що починаються з /api/, потребують токена
                        .anyRequest().denyAll()                          // Все інше закриваємо наглухо
                );

        // ВАЖЛИВО: Додаємо ваш JWT фільтр ПЕРЕД стандартним фільтром аутентифікації
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        // Дозволяємо ваш локальний домен фронтенда (або "*" для розробки)
        configuration.setAllowedOriginPatterns(java.util.List.of("*"));

        // Дозволяємо всі необхідні методи
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Дозволяємо всі заголовки (включаючи Authorization)
        configuration.setAllowedHeaders(java.util.List.of("*"));

        // Дозволяємо передачу кукі та аутентифікації
        configuration.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
