package com.beautyshop.integration;

import com.beautyshop.entity.NguoiDung;
import com.beautyshop.repository.NguoiDung_Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class OAuth2_MariaDb_Test {
    private static NhaCungCapGia nhaCungCapGia;

    @DynamicPropertySource
    static void cauHinh(DynamicPropertyRegistry properties) throws Exception {
        nhaCungCapGia = new NhaCungCapGia();
        for (String provider : List.of("google", "github")) {
            String registration = "spring.security.oauth2.client.registration." + provider;
            String settings = "spring.security.oauth2.client.provider." + provider;
            properties.add(registration + ".client-id", () -> "qa-" + provider);
            properties.add(registration + ".client-secret", () -> "fixture-" + provider);
            properties.add(registration + ".scope", () -> provider.equals("google") ? "openid,profile,email" : "read:user,user:email");
            properties.add(settings + ".authorization-uri", () -> nhaCungCapGia.base + "/" + provider + "/authorize");
            properties.add(settings + ".token-uri", () -> nhaCungCapGia.base + "/" + provider + "/token");
            properties.add(settings + ".user-info-uri", () -> nhaCungCapGia.base + "/" + provider + "/user");
        }
        properties.add("spring.security.oauth2.client.provider.google.issuer-uri", () -> nhaCungCapGia.base);
        properties.add("spring.security.oauth2.client.provider.google.jwk-set-uri", () -> nhaCungCapGia.base + "/jwks");
    }

    @AfterAll
    static void dungNhaCungCap() {
        if (nhaCungCapGia != null) {
            nhaCungCapGia.server.stop(0);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired NguoiDung_Repository repository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcClient jdbc;

    @Test
    void googleTaoCustomerTheoSubVaDungPhienBeautyShop() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(content().string(containsString("/oauth2/authorization/google")))
            .andExpect(content().string(containsString("/oauth2/authorization/github")));

        String email = email();
        String sub = UUID.randomUUID().toString();
        var session = new MockHttpSession();
        dangNhap("google", sub, email, true, session)
            .andExpect(redirectedUrl("/")).andExpect(authenticated().withRoles("CUSTOMER"));
        var daLuu = repository.findByNhaCungCapOauthAndMaDanhTinhOauth("google", sub).orElseThrow();
        assertThat(daLuu.getEmail()).isEqualTo(email);
        mvc.perform(get("/account").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin/dashboard").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(unauthenticated());

        var moi = new MockHttpSession();
        dangNhap("google", sub, email(), true, moi).andExpect(authenticated().withRoles("CUSTOMER"));
        assertThat(repository.findByNhaCungCapOauthAndMaDanhTinhOauth("google", sub).orElseThrow().getMaNguoiDung())
            .isEqualTo(daLuu.getMaNguoiDung());
        jdbc.sql("UPDATE nguoi_dung SET kich_hoat = FALSE, phien_ban_xac_thuc = phien_ban_xac_thuc + 1 WHERE ma_nguoi_dung = ?")
            .param(daLuu.getMaNguoiDung()).update();
        mvc.perform(get("/account").session(moi)).andExpect(redirectedUrl("/login?inactive")).andExpect(unauthenticated());
        dangNhap("google", sub, email, true, new MockHttpSession()).andExpect(redirectedUrl("/login?oauth2Error"));
    }

    @Test
    void githubChonEmailChinhDaXacMinhVaTuChoiEmailChuaXacMinh() throws Exception {
        String email = email();
        String id = Long.toString(1_000 + (UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE) % 1_000_000_000);
        var session = new MockHttpSession();
        dangNhap("github", id, email, false, session)
            .andExpect(redirectedUrl("/login?oauth2Error")).andExpect(unauthenticated());
        assertThat(repository.findByEmail(email)).isEmpty();

        dangNhap("github", id, email, true, new MockHttpSession())
            .andExpect(redirectedUrl("/")).andExpect(authenticated().withRoles("CUSTOMER"));
        assertThat(repository.findByNhaCungCapOauthAndMaDanhTinhOauth("github", id).orElseThrow().getEmail())
            .isEqualTo(email);
    }

    @Test
    void oauthTuChoiEmailTrungVaGoogleChuaXacMinh() throws Exception {
        String email = email();
        String sub = UUID.randomUUID().toString();
        dangNhap("google", sub, email, false, new MockHttpSession())
            .andExpect(redirectedUrl("/login?oauth2Error")).andExpect(unauthenticated());
        assertThat(repository.findByEmail(email)).isEmpty();

        var coSan = repository.saveAndFlush(new NguoiDung(email, passwordEncoder.encode(UUID.randomUUID().toString()), "QA"));
        jdbc.sql("UPDATE nguoi_dung SET vai_tro = 'ADMIN' WHERE ma_nguoi_dung = ?").param(coSan.getMaNguoiDung()).update();
        dangNhap("google", sub, email, true, new MockHttpSession())
            .andExpect(redirectedUrl("/login?oauth2Error")).andExpect(unauthenticated());
        assertThat(repository.findByNhaCungCapOauthAndMaDanhTinhOauth("google", sub)).isEmpty();
        assertThat(repository.findByEmail(email).orElseThrow().getVaiTro()).isEqualTo(NguoiDung.VaiTro.ADMIN);
    }

    @Test
    void callbackSaiStateBiTuChoi() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get("/oauth2/authorization/google").session(session)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login/oauth2/code/google").session(session).param("state", "invalid").param("code", "fixture"))
            .andExpect(redirectedUrl("/login?oauth2Error")).andExpect(unauthenticated());
    }

    private ResultActions dangNhap(String provider, String id, String email, boolean verified, MockHttpSession session)
            throws Exception {
        String redirect = mvc.perform(get("/oauth2/authorization/" + provider).session(session))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        var params = thamSo(URI.create(redirect).getRawQuery());
        assertThat(params.get("state")).isNotBlank();
        if (provider.equals("google")) {
            assertThat(params.get("nonce")).isNotBlank();
        }
        var code = UUID.randomUUID().toString();
        nhaCungCapGia.danhTinh.put(code, new DanhTinh(id, email, verified, params.get("nonce")));
        return mvc.perform(get("/login/oauth2/code/" + provider).session(session)
            .param("state", params.get("state")).param("code", code));
    }

    private static Map<String, String> thamSo(String query) {
        var params = new HashMap<String, String>();
        for (String thamSo : query.split("&")) {
            String[] cap = thamSo.split("=", 2);
            params.put(cap[0], URLDecoder.decode(cap[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    private static String email() {
        return UUID.randomUUID() + "@example.invalid";
    }

    private record DanhTinh(String id, String email, boolean verified, String nonce) {}

    private static final class NhaCungCapGia {
        private final HttpServer server;
        private final String base;
        private final Map<String, DanhTinh> danhTinh = new ConcurrentHashMap<>();
        private final ObjectMapper json = new ObjectMapper();

        private NhaCungCapGia() throws Exception {
            var key = new RSAKeyGenerator(2048).keyID("qa-key").generate();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            base = "http://127.0.0.1:" + server.getAddress().getPort();
            server.createContext("/.well-known/openid-configuration", request -> send(request, Map.of(
                "issuer", base, "authorization_endpoint", base + "/google/authorize",
                "token_endpoint", base + "/google/token", "jwks_uri", base + "/jwks",
                "userinfo_endpoint", base + "/google/user", "response_types_supported", List.of("code"),
                "subject_types_supported", List.of("public"), "id_token_signing_alg_values_supported", List.of("RS256"))));
            server.createContext("/jwks", request -> send(request, Map.of("keys", List.of(key.toPublicJWK().toJSONObject()))));
            server.createContext("/google/token", request -> {
                String code = code(request);
                DanhTinh id = danhTinh.get(code);
                Instant now = Instant.now();
                var claims = new JWTClaimsSet.Builder().issuer(base).audience("qa-google").subject(id.id())
                    .issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300)))
                    .claim("nonce", id.nonce()).claim("email", id.email()).claim("email_verified", id.verified())
                    .claim("name", "Khách Google").build();
                var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
                try {
                    jwt.sign(new RSASSASigner(key));
                    send(request, Map.of("access_token", code, "token_type", "Bearer", "expires_in", 300,
                        "scope", "openid profile email", "id_token", jwt.serialize()));
                } catch (com.nimbusds.jose.JOSEException exception) {
                    request.sendResponseHeaders(500, -1);
                    request.close();
                }
            });
            server.createContext("/google/user", request -> {
                DanhTinh id = tuToken(request);
                send(request, Map.of("sub", id.id(), "email", id.email(), "email_verified", id.verified(), "name", "Khách Google"));
            });
            server.createContext("/github/token", request -> send(request, Map.of("access_token", code(request),
                "token_type", "Bearer", "expires_in", 300, "scope", "read:user,user:email")));
            server.createContext("/github/user", request -> {
                DanhTinh id = tuToken(request);
                send(request, Map.of("id", Long.parseLong(id.id()), "login", "qa"));
            });
            server.createContext("/github/user/emails", request -> {
                DanhTinh id = tuToken(request);
                send(request, List.of(Map.of("email", "secondary@example.invalid", "verified", true, "primary", false),
                    Map.of("email", id.email(), "verified", id.verified(), "primary", true)));
            });
            server.start();
        }

        private static String code(HttpExchange request) throws java.io.IOException {
            return thamSo(new String(request.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).get("code");
        }

        private DanhTinh tuToken(HttpExchange request) {
            return danhTinh.get(request.getRequestHeaders().getFirst("Authorization").substring("Bearer ".length()));
        }

        private void send(HttpExchange request, Object body) throws java.io.IOException {
            byte[] response = json.writeValueAsBytes(body);
            request.getResponseHeaders().set("Content-Type", "application/json");
            request.sendResponseHeaders(200, response.length);
            try (var output = request.getResponseBody()) {
                output.write(response);
            }
        }
    }
}
