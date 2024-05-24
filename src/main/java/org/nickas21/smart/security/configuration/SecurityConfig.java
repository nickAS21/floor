package org.nickas21.smart.security.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.nickas21.smart.util.JwtUtil.generateToken;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        UserDetails user = User.withUsername("user")
                .password(encoder.encode("user"))
                .roles("USER")
                .build();
        return new MapReactiveUserDetailsService(user);
    }


    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                .and()
                // Add other security configurations as needed
                .authorizeExchange()
                .pathMatchers("/login").permitAll()
                .anyExchange().authenticated()
                .and()
                .httpBasic().and()
                .formLogin().and()
                .build();
    }

//    @Bean
//    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
//        return http
//                .csrf().csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
//                .and()
//                // Add other security configurations as needed
//                .authorizeExchange()
//                .pathMatchers("/login").permitAll()
//                .anyExchange().authenticated()
//                .and()
//                .httpBasic().and()
//                .formLogin().and()
//                .formLogin(formLoginSpec -> formLoginSpec
//                        .loginPage("/login")
//                        .authenticationSuccessHandler(authenticationSuccessHandler())
//                )
//                .build();
//    }


//    private ServerAuthenticationSuccessHandler authenticationSuccessHandler() {
//        return (webFilterExchange, authentication) -> {
//            String username = authentication.getName();
//            String token = generateToken(username);
//            webFilterExchange.getExchange().getResponse().getHeaders().add("Authorization", "Bearer " + token);
//            webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.FOUND);
//            webFilterExchange.getExchange().getResponse().getHeaders().setLocation(URI.create("/main"));
//            return Mono.empty();
//        };
//    }
}
