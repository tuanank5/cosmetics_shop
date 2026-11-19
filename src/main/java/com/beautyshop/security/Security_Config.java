package com.beautyshop.security;

import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.payment.Momo_Settings;
import org.springframework.beans.factory.ObjectProvider;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.util.HttpSessionMutexListener;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class Security_Config {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, LoginSuccess_Handler loginSuccess,
                                            NguoiDung_Repository nguoiDungRepository,
                                            ObjectProvider<Momo_Settings> momoSettings) throws Exception {
        var momo = momoSettings.getIfAvailable();
        if (momo != null && momo.enabled()) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers(PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/payment/momo/ipn")));
        }
        return http
            .addFilterBefore(new TrangThaiTaiKhoan_Filter(nguoiDungRepository), AuthorizationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/products", "/products/**", "/login", "/register",
                        "/css/**", "/js/**", "/img/**", "/error", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET, "/cart", "/cart/**", "/uploads/sku/*").permitAll()
                .requestMatchers(HttpMethod.HEAD, "/uploads/sku/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/cart", "/cart/**")
                    .access(new WebExpressionAuthorizationManager("isAnonymous() or hasRole('CUSTOMER')"))
                .requestMatchers(HttpMethod.POST, "/payment/momo/ipn").permitAll()
                .requestMatchers(HttpMethod.GET, "/payment/momo/return").permitAll()
                .requestMatchers("/payment/momo/mock/**").hasRole("CUSTOMER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/checkout/**", "/orders/**", "/account/**").hasRole("CUSTOMER")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").successHandler(loginSuccess).permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/?logout").invalidateHttpSession(true))
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder() {
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword != null && rawPassword.toString().getBytes(StandardCharsets.UTF_8).length <= 72
                    && super.matches(rawPassword, encodedPassword);
            }
        };
    }

    @Bean
    HttpSessionMutexListener sessionMutexListener() {
        return new HttpSessionMutexListener();
    }
}
