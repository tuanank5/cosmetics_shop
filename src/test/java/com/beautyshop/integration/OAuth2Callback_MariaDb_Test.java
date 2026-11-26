package com.beautyshop.integration;

import com.beautyshop.entity.NguoiDung;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.OAuth2NguoiDung_Service;
import com.beautyshop.service.DanhTinhOAuth_Service.BangChung;
import com.beautyshop.service.EmailVerification_Service;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {com.beautyshop.BeautyShopApplication.class, OAuth2Callback_MariaDb_Test.Clients.class})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class OAuth2Callback_MariaDb_Test {
    private static final String PASSWORD = "fixture-callback-password";
    @Autowired MockMvc mvc;
    @Autowired NguoiDung_Repository users;
    @Autowired PasswordEncoder encoder;
    @MockitoSpyBean OAuth2NguoiDung_Service providerUser;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean EmailVerification_Service otp;

    @Test
    void callbackDoiCodeKiemStateVaDangNhapBangUserId() throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        chungMinh(new BangChung("github", "1201", email, "OAuth fixture"));
        var session = new MockHttpSession();
        String state = batDau(session, "/oauth2/authorization/github");
        mvc.perform(get("/login/oauth2/code/github").session(session).param("code", "fixture").param("state", "wrong"))
            .andExpect(redirectedUrl("/login?oauth2Error")).andExpect(unauthenticated());
        assertThat(users.findByEmail(email)).isEmpty();

        callback(session, state, "/");
        long id = users.findByEmail(email).orElseThrow().getMaNguoiDung();
        mvc.perform(get("/account").session(session)).andExpect(status().isOk()).andExpect(authenticated());
        String changed = UUID.randomUUID() + "@example.invalid";
        chungMinh(new BangChung("github", "1201", changed, "OAuth fixture"));
        var nextSession = new MockHttpSession();
        callback(nextSession, batDau(nextSession, "/oauth2/authorization/github"), "/");
        assertThat(users.findByEmail(email).orElseThrow().getMaNguoiDung()).isEqualTo(id);
        assertThat(users.findByEmail(changed)).isEmpty();
    }

    @Test
    void googleOidcKiemChuKyNonceVaEmailDaXacMinh() throws Exception {
        Clients.googleEmail = UUID.randomUUID() + "@example.invalid";
        var session = new MockHttpSession();
        String location = mvc.perform(get("/oauth2/authorization/google").session(session))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        var parameters = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        Clients.nonce = parameters.getFirst("nonce");
        mvc.perform(get("/login/oauth2/code/google").session(session).param("code", "fixture")
                .param("state", state(location)))
            .andExpect(redirectedUrl("/"));
        assertThat(users.findByEmail(Clients.googleEmail)).isPresent();
        mvc.perform(get("/account").session(session)).andExpect(authenticated()).andExpect(status().isOk());
    }

    @Test
    void callbackLienKetGiuPhienVaChiGhiSauXacNhanPost() throws Exception {
        String ownerEmail = UUID.randomUUID() + "@example.invalid";
        var owner = users.saveAndFlush(new NguoiDung(ownerEmail, encoder.encode(PASSWORD), "Owner"));
        String providerEmail = UUID.randomUUID() + "@example.invalid";
        chungMinh(new BangChung("github", "1202", providerEmail, "Khác email"));
        var session = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("username", ownerEmail).param("password", PASSWORD))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList())).thenReturn(1L);
        mvc.perform(post("/account/security/reauth/password").with(csrf()).session(session)
                .param("matKhau", PASSWORD)).andExpect(redirectedUrl("/account/security"));
        String path = mvc.perform(post("/account/security/oauth/github").with(csrf()).session(session)
                .param("purpose", "LIEN_KET")).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(path).contains("operation=");
        callback(session, batDau(session, path), "/account/security");
        assertThat(users.findByEmail(providerEmail)).isEmpty();
        assertThat(users.findByEmail(ownerEmail).orElseThrow().getPhienBanXacThuc()).isZero();
        mvc.perform(get("/account/security").session(session)).andExpect(status().isOk()).andExpect(authenticated());
        mvc.perform(post("/account/security/oauth/confirm").with(csrf()).session(session))
            .andExpect(redirectedUrl("/login?securityUpdated"));
        assertThat(users.findByEmail(ownerEmail).orElseThrow().getPhienBanXacThuc()).isEqualTo(1);
        assertThat(users.findByEmail(ownerEmail).orElseThrow().getMaNguoiDung()).isEqualTo(owner.getMaNguoiDung());
    }

    @Test
    void oauthChuaCoMatKhauCanCallbackDungDanhTinhVaOtpDeThietLap() throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        chungMinh(new BangChung("github", "1203", email, "OAuth only"));
        var session = new MockHttpSession();
        callback(session, batDau(session, "/oauth2/authorization/github"), "/");
        mvc.perform(post("/account/security/password").with(csrf()).session(session)
                .param("matKhauMoi", PASSWORD).param("xacNhan", PASSWORD))
            .andExpect(redirectedUrl("/account/security")).andExpect(flash().attributeExists("error"));
        String path = mvc.perform(post("/account/security/oauth/github").with(csrf()).session(session)
                .param("purpose", "XAC_THUC_LAI")).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        callback(session, batDau(session, path), "/account/security");
        when(otp.guiOTP(email, "security")).thenReturn("fixture-challenge");
        when(otp.xacThucOTP("fixture-challenge", "123456")).thenReturn(true);
        mvc.perform(post("/account/security/email/send").with(csrf()).session(session))
            .andExpect(redirectedUrl("/account/security"));
        mvc.perform(post("/account/security/email/verify").with(csrf()).session(session).param("ma", "123456"))
            .andExpect(redirectedUrl("/account/security"));
        mvc.perform(post("/account/security/password").with(csrf()).session(session)
                .param("matKhauMoi", PASSWORD).param("xacNhan", PASSWORD))
            .andExpect(redirectedUrl("/login?securityUpdated"));
        var user = users.findByEmail(email).orElseThrow();
        assertThat(user.getNgayXacMinhEmail()).isNotNull();
        assertThat(encoder.matches(PASSWORD, user.getMatKhauBam())).isTrue();
    }

    private void chungMinh(BangChung proof) {
        doReturn(new DefaultOAuth2User(List.of(), Map.of("id", proof.subject(), "beautyshopProof", proof), "id"))
            .when(providerUser).loadUser(any());
    }

    private String batDau(MockHttpSession session, String path) throws Exception {
        String location = mvc.perform(get(URI.create(path)).session(session)).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        return state(location);
    }

    private static String state(String location) {
        return org.springframework.web.util.UriUtils.decode(
            UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("state"),
            StandardCharsets.UTF_8);
    }

    private void callback(MockHttpSession session, String state, String expected) throws Exception {
        mvc.perform(get("/login/oauth2/code/github").session(session).param("code", "fixture").param("state", state))
            .andExpect(redirectedUrl(expected));
    }

    @AfterAll static void close() { if (Clients.server != null) { Clients.server.stop(0); } }

    @TestConfiguration(proxyBeanMethods = false)
    static class Clients {
        private static HttpServer server;
        private static volatile String nonce;
        private static volatile String googleEmail;

        @Bean
        ClientRegistrationRepository oauthFixture() throws IOException, com.nimbusds.jose.JOSEException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            var key = new RSAKeyGenerator(2048).keyID("fixture-key").generate();
            server.createContext("/token", exchange -> json(exchange,
                "{\"access_token\":\"dummy-fixture\",\"token_type\":\"Bearer\"}"));
            server.createContext("/keys", exchange -> json(exchange, new JWKSet(key.toPublicJWK()).toString()));
            server.createContext("/google-token", exchange -> {
                Instant now = Instant.now();
                var claims = new JWTClaimsSet.Builder().issuer("https://accounts.google.com").audience("fixture-google")
                    .subject("fixture-google-id").issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300)))
                    .claim("email", googleEmail).claim("email_verified", true).claim("nonce", nonce).build();
                var token = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
                try {
                    token.sign(new RSASSASigner(key));
                    json(exchange, "{\"access_token\":\"dummy-fixture\",\"token_type\":\"Bearer\",\"id_token\":\""
                        + token.serialize() + "\"}");
                } catch (com.nimbusds.jose.JOSEException failed) {
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                }
            });
            server.start();
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var registration = ClientRegistration.withRegistrationId("github")
                .clientId("fixture").clientSecret("fixture").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}").authorizationUri(base + "/authorize")
                .tokenUri(base + "/token").userInfoUri(base + "/user").userNameAttributeName("id").build();
            var google = ClientRegistration.withRegistrationId("google").clientId("fixture-google")
                .clientSecret("fixture").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .scope("openid", "email").redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri(base + "/authorize").tokenUri(base + "/google-token")
                .issuerUri("https://accounts.google.com").jwkSetUri(base + "/keys").userNameAttributeName("sub").build();
            return new InMemoryClientRegistrationRepository(registration, google);
        }

        private static void json(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var stream = exchange.getResponseBody()) { stream.write(response); }
        }
    }
}
