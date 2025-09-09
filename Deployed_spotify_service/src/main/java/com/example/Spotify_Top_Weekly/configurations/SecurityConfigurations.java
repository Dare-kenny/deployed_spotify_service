package com.example.Spotify_Top_Weekly.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/", "/index", "/css/**", "/images/**", "/js/**").permitAll()

                        .anyRequest().authenticated()
                )
                .oauth2Login(o -> o

                        .defaultSuccessUrl("/recent_24h", true)
                )
                .logout(l -> l

                        .logoutSuccessUrl("/")
                );

        return http.build();
    }
}

