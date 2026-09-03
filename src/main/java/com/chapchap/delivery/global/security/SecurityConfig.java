package com.chapchap.delivery.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public GatewayAuthenticationFilter gatewayAuthenticationFilter() {
        return new GatewayAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http
        , GatewayAuthenticationFilter gatewayAuthenticationFilter
        , CustomAuthenticationEntryPoint authenticationEntryPoint
        , CustomAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http
            .csrf(
                AbstractHttpConfigurer::disable
            )
            .httpBasic(
                AbstractHttpConfigurer::disable
            )
            .formLogin(
                AbstractHttpConfigurer::disable
            )
            .logout(
                AbstractHttpConfigurer::disable
            )
            .sessionManagement(
                session -> session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .exceptionHandling(
                exception -> exception
                    .authenticationEntryPoint(
                        authenticationEntryPoint
                    )
                    .accessDeniedHandler(
                        accessDeniedHandler
                    )
            )
            .authorizeHttpRequests(
                authorize -> authorize
                    .requestMatchers(
                        "/actuator/health"
                        , "/api-docs/**"
                        , "/swagger-ui/**"
                        , "/swagger-ui.html"
                    )
                    .permitAll()

                    .requestMatchers(
                        "/api/admin/**"
                    )
                    .hasRole("ADMIN")

                    .requestMatchers(
                        "/api/rider/**"
                    )
                    .hasRole("RIDER")

                    .requestMatchers(
                        "/api/customer/**"
                    )
                    .hasRole("CUSTOMER")

                    .anyRequest()
                    .authenticated()
            )
            .addFilterBefore(
                gatewayAuthenticationFilter
                , UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}