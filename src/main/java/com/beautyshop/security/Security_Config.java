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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.web.filter.RequestContextFilter;
import com.beautyshop.service.DanhTinhOAuth_Service;

@Configuration
@EnableMethodSecurity
public class Security_Config {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, LoginSuccess_Handler loginSuccess,
                                            NguoiDung_Repository nguoiDungRepository,
                                            ObjectProvider<ClientRegistrationRepository> oauthClients,
                                            ObjectProvider<OAuth2NguoiDung_Service> oauthUsers,
                                            ObjectProvider<DanhTinhOAuth_Service> identities,
                                            ObjectProvider<Momo_Settings> momoSettings) throws Exception {
        var momo = momoSettings.getIfAvailable();
        if (momo != null && momo.enabled()) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers(PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/payment/momo/ipn")));
        }
        var clients = oauthClients.getIfAvailable();
        if (clients != null) {
            var oauthService = oauthUsers.getObject();
            http.addFilterBefore(new RequestContextFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
            http.oauth2Login(oauth2 -> oauth2.loginPage("/login")
                .clientRegistrationRepository(clients)
                .authorizedClientRepository(new OAuth2ThaoTac.KhongLuuToken())
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(new OAuth2ThaoTac.YeuCauDangNhap(clients))
                    .authorizationRequestRepository(new OAuth2ThaoTac.YeuCau()))
                .userInfoEndpoint(userInfo -> userInfo.userService(oauthService).oidcUserService(oauthService::loadGoogle))
                .withObjectPostProcessor(new ObjectPostProcessor<OAuth2LoginAuthenticationFilter>() {
                    @Override
                    public <O extends OAuth2LoginAuthenticationFilter> O postProcess(O filter) {
                        filter.setAuthenticationResultConverter(token -> OAuth2ThaoTac.ketQua(token, identities.getObject()));
                        return filter;
                    }
                })
                .successHandler(loginSuccess)
                .failureHandler((request, response, failure) -> {
                    String code = failure instanceof OAuth2AuthenticationException oauthError
                        ? oauthError.getError().getErrorCode() : "";
                    String path = switch (code) {
                        case "beautyshop_operation_verified" -> "/account/security";
                        case "oauth_operation_invalid" -> "/account/security?oauthError";
                        default -> OAuth2ThaoTac.laThaoTac(request)
                            ? "/account/security?oauthError" : "/login?oauth2Error";
                    };
                    response.sendRedirect(request.getContextPath() + path);
                }));
        }
        return http
                .addFilterBefore(new TrangThaiTaiKhoan_Filter(nguoiDungRepository), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/products", "/products/**", "/login", "/register","/register/**","/oauth2/**",
                            "/login/oauth2/**", "/forgot-password", "/reset-password",
                            "/css/**", "/js/**", "/img/**", "/error", "/favicon.ico").permitAll()
                    .requestMatchers(HttpMethod.GET, "/cart", "/cart/**", "/uploads/sku/*").permitAll()
                    .requestMatchers(HttpMethod.HEAD, "/uploads/sku/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/chat").permitAll()
                    .requestMatchers(HttpMethod.POST, "/chat").permitAll()
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
