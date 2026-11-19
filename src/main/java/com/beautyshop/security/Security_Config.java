package com.beautyshop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class Security_Config {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // IPN server-to-server khong co CSRF browser; P07 phai verify HMAC ngay tai endpoint
            .csrf(csrf -> csrf.ignoringRequestMatchers("/payment/momo/ipn"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/products", "/products/**", "/login", "/register",
                        "/cart/**", "/css/**", "/js/**", "/img/**", "/uploads/**", "/error", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/payment/momo/ipn").permitAll()
                .requestMatchers(HttpMethod.GET, "/payment/momo/return").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/checkout/**", "/orders/**", "/account/**").hasRole("CUSTOMER")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", false).permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/?logout").invalidateHttpSession(true))
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
