package com.chapchap.delivery.global.security;

import com.chapchap.delivery.global.config.InternalApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(InternalApiProperties.class)
public class SecurityConfig {

    @Bean
    public GatewayAuthenticationFilter gatewayAuthenticationFilter() {
        return new GatewayAuthenticationFilter();
    }

    @Bean
    public InternalServiceAuthenticationFilter internalServiceAuthenticationFilter(
        InternalApiProperties properties, ObjectMapper objectMapper
    ) {
        return new InternalServiceAuthenticationFilter(properties, objectMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http
        , GatewayAuthenticationFilter gatewayAuthenticationFilter
        , InternalServiceAuthenticationFilter internalServiceAuthenticationFilter
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
                        "/api/delivery/admin/**"
                    )
                    .hasRole("ADMIN")

                    .requestMatchers(
                        "/api/delivery/rider/**"
                    )
                    .hasRole("RIDER")

                    .requestMatchers(
                        "/api/delivery/customer/**"
                    )
                    .hasRole("CUSTOMER")

                    .requestMatchers("/internal/**")
                    .hasRole("CUSTOMER")

                    .anyRequest()
                    .authenticated()
            )
            .addFilterBefore(
                gatewayAuthenticationFilter
                , UsernamePasswordAuthenticationFilter.class
            )
            .addFilterBefore(
                internalServiceAuthenticationFilter
                , GatewayAuthenticationFilter.class
            );

        return http.build();
    }
}
