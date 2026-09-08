package com.runmate.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/phone-verifications/**",
                                "/api/auth/email-verifications/**",
                                "/api/auth/kakao/signup"
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/csrf",
                                "/api/auth/login-id/availability",
                                "/api/auth/phone-verifications/**",
                                "/api/auth/email-verifications/**",
                                "/api/auth/kakao",
                                "/api/auth/kakao/callback",
                                "/api/auth/kakao/pending",
                                "/api/auth/kakao/signup"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
