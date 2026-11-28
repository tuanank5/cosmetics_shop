package com.beautyshop.integration;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.model.DanhTinhOAuth;
import com.beautyshop.model.NguoiDung;
import com.beautyshop.repository.DanhTinhOAuth_Repository;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.OAuth2NguoiDung_Service;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.DanhTinhOAuth_Service.BangChung;
import com.beautyshop.service.EmailVerification_Service;
import com.beautyshop.service.GioHang_Service;
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
import java.time.LocalDateTime;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.address=127.0.0.1",
    classes = {com.beautyshop.BeautyShopApplication.class, OAuth2Callback_MariaDb_Test.Clients.class})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class OAuth2Callback_MariaDb_Test {
    private static final String PASSWORD = "fixture-callback-password";
    @Autowired MockMvc mvc;
    @Autowired NguoiDung_Repository users;
    @Autowired PasswordEncoder encoder;
    @Autowired DanhTinhOAuth_Repository identities;
    @Autowired GioHang_Service gioHang;
    @Autowired JdbcClient jdbc;
    @MockitoSpyBean OAuth2NguoiDung_Service providerUser;
    @MockitoBean StringRedisTemplate redis;
    @MockitoBean EmailVerification_Service otp;

    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired com.beautyshop.repository.YeuCauDatLaiMatKhau_Repository resets;

    @Test
    @EnabledIfEnvironmentVariable(named = "AUTH_BROWSER_SCRIPT", matches = ".+")
    void webkitDatLaiMatKhauVaXacThucLaiGoogleTrenHaiTab() throws Exception {
        String base = "http://127.0.0.1:" + port;
        String callback = base + "/login/oauth2/code/google";
        var lanGoogle = new java.util.concurrent.atomic.AtomicInteger();
        var googlePrompts = new java.util.concurrent.CopyOnWriteArrayList<String>();
        Clients.googleEmail = UUID.randomUUID() + "@example.invalid";
        Clients.googleSubject = UUID.randomUUID().toString();
        var customer = new NguoiDung(UUID.randomUUID() + "@example.invalid", encoder.encode(PASSWORD), "Fixture");
        customer.xacNhanEmail(LocalDateTime.now(java.time.ZoneOffset.UTC));
        users.saveAndFlush(customer);
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        resets.saveAndFlush(new com.beautyshop.model.YeuCauDatLaiMatKhau(customer,
            java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)),
            LocalDateTime.now(java.time.ZoneOffset.UTC), "SELF", UUID.randomUUID().toString()));
        when(otp.guiOTP(Clients.googleEmail, "security")).thenReturn("fixture-challenge");
        when(otp.xacThucOTP("fixture-challenge", "123456")).thenReturn(true);
        Clients.server.createContext("/authorize", exchange -> {
            var params = UriComponentsBuilder.fromUri(exchange.getRequestURI()).build().getQueryParams();
            if (callback.equals(org.springframework.web.util.UriUtils.decode(params.getFirst("redirect_uri"), StandardCharsets.UTF_8))) {
                Clients.nonce = params.getFirst("nonce");
                googlePrompts.add(params.getFirst("prompt"));
                lanGoogle.incrementAndGet();
                exchange.getResponseHeaders().set("Location", callback + "?code=fixture&state=" + params.getFirst("state"));
                exchange.sendResponseHeaders(302, -1);
            } else {
                exchange.sendResponseHeaders(400, -1);
            }
            exchange.close();
        });
        var runner = new ProcessBuilder("node", System.getenv("AUTH_BROWSER_SCRIPT"), base);
        runner.environment().put("AUTH_RESET_TOKEN", token);
        runner.environment().put("AUTH_OLD_PASSWORD", PASSWORD);
        runner.environment().put("AUTH_NEW_PASSWORD", UUID.randomUUID().toString());
        var process = runner.redirectErrorStream(true).start();
        try {
            assertThat(process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            try (var lines = process.inputReader(StandardCharsets.UTF_8)) {
                lines.lines().filter(line -> line.startsWith("[AUTH WebKit]") || line.startsWith("Browser check failed at "))
                    .forEach(System.out::println);
            }
            assertThat(process.exitValue()).isZero();
            assertThat(lanGoogle).hasValue(3);
            assertThat(googlePrompts).containsExactly("select_account", "select_account", null);
            var owner = users.findByEmail(Clients.googleEmail).orElseThrow();
            assertThat(encoder.matches(runner.environment().get("AUTH_NEW_PASSWORD"), owner.getMatKhauBam())).isTrue();
            assertThat(identities.findByNguoiDung_MaNguoiDung(owner.getMaNguoiDung())).hasSize(1);
            assertThat(users.findById(customer.getMaNguoiDung()).orElseThrow().getPhienBanXacThuc()).isOne();
            System.out.println("[AUTH WebKit] PASS reset/retry/replay, two-tab logout, fresh Google callback, reauth/OTP, last method and first password; fixture authorizations=3");
        } finally {
            process.destroyForcibly();
            Clients.server.removeContext("/authorize");
        }
    }

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
        String stateDaDung = batDau(nextSession, "/oauth2/authorization/github");
        callback(nextSession, stateDaDung, "/");
        assertThat(users.findByEmail(email).orElseThrow().getMaNguoiDung()).isEqualTo(id);
        assertThat(users.findByEmail(changed)).isEmpty();
        callback(nextSession, stateDaDung, "/login?oauth2Error");
    }

    @Test
    void googleOidcKiemChuKyNonceVaEmailDaXacMinh() throws Exception {
        Clients.googleEmail = UUID.randomUUID() + "@example.invalid";
        Clients.googleSubject = "fixture-google-id";
        var session = new MockHttpSession();
        String location = mvc.perform(get("/oauth2/authorization/google").session(session))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        var parameters = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        assertThat(parameters.get("prompt")).containsExactly("select_account");
        assertThat(parameters.getFirst("state")).isNotBlank();
        assertThat(parameters.getFirst("nonce")).isNotBlank();
        Clients.nonce = parameters.getFirst("nonce");
        mvc.perform(get("/login/oauth2/code/google").session(session).param("code", "fixture")
                .param("state", state(location)))
            .andExpect(redirectedUrl("/"));
        assertThat(users.findByEmail(Clients.googleEmail)).isPresent();
        mvc.perform(get("/account").session(session)).andExpect(authenticated()).andExpect(status().isOk());
    }

    @Test
    void baCachLoginCungIdVaMoiPhienGopGioDungMotLan() throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        var owner = users.saveAndFlush(new NguoiDung(email, encoder.encode(PASSWORD), "Owner"));
        long id = owner.getMaNguoiDung();
        Clients.googleEmail = email;
        Clients.googleSubject = "fixture-google-merge";
        identities.saveAllAndFlush(List.of(
            new DanhTinhOAuth(owner, "google", Clients.googleSubject, LocalDateTime.now()),
            new DanhTinhOAuth(owner, "github", "1204", LocalDateTime.now())));
        chungMinh(new BangChung("github", "1204", email, "OAuth fixture"));
        String slug = "cart-" + UUID.randomUUID();
        long danhMuc = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Fixture', ?) RETURNING ma_danh_muc")
            .param(slug).query(Long.class).single();
        long thuongHieu = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Fixture', ?) RETURNING ma_thuong_hieu")
            .param(slug).query(Long.class).single();
        long sanPham = jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
            VALUES (?, ?, 'Fixture', ?) RETURNING ma_san_pham
            """).params(danhMuc, thuongHieu, slug).query(Long.class).single();
        long sku = jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, '50 ml', 100000, 10) RETURNING ma_bien_the
            """).params(sanPham, slug).query(Long.class).single();
        gioHang.themGioCustomer(id, sku);
        long nguoiKhac = users.saveAndFlush(new NguoiDung(UUID.randomUUID() + "@example.invalid", null, "Fixture")).getMaNguoiDung();
        long donSoHuu = taoDonFixture(id);
        long donNguoiKhac = taoDonFixture(nguoiKhac);
        var phienDangNhap = new java.util.ArrayList<MockHttpSession>();
        for (String phuongThuc : List.of("password", "google", "github")) {
            var session = new MockHttpSession();
            phienDangNhap.add(session);
            var snapshot = GioKhach.doc(session).them(sku, 5);
            snapshot.luu(session);
            gioHang.datSoLuongCustomer(id, sku, 7);
            var login = post("/login").session(session).with(csrf()).param("username", email).param("password", PASSWORD);
            if (!phuongThuc.equals("password")) {
                String url = mvc.perform(get("/oauth2/authorization/" + phuongThuc).session(session))
                    .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
                if (phuongThuc.equals("google")) {
                    Clients.nonce = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("nonce");
                }
                login = get("/login/oauth2/code/" + phuongThuc).session(session).param("code", "fixture").param("state", state(url));
            }
            String dich = mvc.perform(login).andExpect(authenticated()).andReturn().getResponse().getRedirectedUrl();
            var security = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertThat(((PhienNguoiDung) security.getAuthentication().getPrincipal()).getMaNguoiDung()).isEqualTo(id);
            if (phuongThuc.equals("password")) {
                mvc.perform(post("/account/wishlist/" + sanPham).session(session).with(csrf()).header("X-Wishlist-Partial", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
            }
            mvc.perform(get("/account/wishlist/state").session(session).param("ids", Long.toString(sanPham)))
                .andExpect(content().json("[\"" + sanPham + "\"]"));
            mvc.perform(get("/orders/" + donSoHuu).session(session)).andExpect(status().isOk());
            mvc.perform(get("/orders/" + donNguoiKhac).session(session)).andExpect(status().isNotFound());
            mvc.perform(post("/admin/reviews/65f000000000000000000001/visibility").session(session).with(csrf())
                    .param("visible", "true").param("revision", "0"))
                .andExpect(status().isForbidden());
            assertThat(dich).startsWith("/?cartMerge=");
            mvc.perform(get(URI.create(dich)).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Dự kiến 12, thực nhận 10, giảm 2")));
            assertThat(GioKhach.doc(session).soLuong()).isEmpty();
            assertThat(gioHang.gopSauDangNhap(id, snapshot).daGioiHan()).isFalse();
            assertThat(gioHang.docGioCustomer(id).getFirst().soLuong()).isEqualTo(10);
        }
        jdbc.sql("UPDATE nguoi_dung SET phien_ban_xac_thuc = phien_ban_xac_thuc + 1 WHERE ma_nguoi_dung = ?")
            .param(id).update();
        for (var session : phienDangNhap) {
            mvc.perform(get("/orders/" + donSoHuu).session(session)).andExpect(redirectedUrl("/login?inactive"));
        }
        var phienMoi = new MockHttpSession();
        mvc.perform(post("/login").session(phienMoi).with(csrf()).param("username", email).param("password", PASSWORD))
            .andExpect(authenticated());
        mvc.perform(get("/orders/" + donSoHuu).session(phienMoi)).andExpect(status().isOk());
        jdbc.sql("UPDATE nguoi_dung SET kich_hoat = FALSE WHERE ma_nguoi_dung = ?").param(id).update();
        mvc.perform(get("/orders/" + donSoHuu).session(phienMoi)).andExpect(redirectedUrl("/login?inactive"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ACCOUNT_BROWSER_SCRIPT", matches = ".+")
    void webKitKhoaMoBaPhuongThuc() throws Exception {
        String base = "http://127.0.0.1:" + port;
        var admin = users.saveAndFlush(new NguoiDung(UUID.randomUUID() + "@example.invalid", encoder.encode(PASSWORD), "Admin"));
        jdbc.sql("UPDATE nguoi_dung SET vai_tro='ADMIN' WHERE ma_nguoi_dung=?").param(admin.getMaNguoiDung()).update();
        Clients.googleEmail = UUID.randomUUID() + "@example.invalid";
        Clients.googleSubject = UUID.randomUUID().toString();
        var customer = new NguoiDung(Clients.googleEmail, encoder.encode(PASSWORD), "Customer");
        customer.xacNhanEmail(LocalDateTime.now(java.time.ZoneOffset.UTC));
        users.saveAndFlush(customer);
        String github = UUID.randomUUID().toString();
        identities.saveAllAndFlush(List.of(new DanhTinhOAuth(customer, "google", Clients.googleSubject, LocalDateTime.now()),
            new DanhTinhOAuth(customer, "github", github, LocalDateTime.now())));
        chungMinh(new BangChung("github", github, customer.getEmail(), "Customer"));
        Clients.server.createContext("/authorize", exchange -> {
            var q = UriComponentsBuilder.fromUri(exchange.getRequestURI()).build().getQueryParams();
            String callback = org.springframework.web.util.UriUtils.decode(q.getFirst("redirect_uri"), StandardCharsets.UTF_8);
            if (List.of(base + "/login/oauth2/code/google", base + "/login/oauth2/code/github").contains(callback)) {
                Clients.nonce = q.getFirst("nonce");
                exchange.getResponseHeaders().set("Location", callback + "?code=fixture&state=" + q.getFirst("state"));
                exchange.sendResponseHeaders(302, -1);
            } else exchange.sendResponseHeaders(400, -1);
            exchange.close();
        });
        var runner = new ProcessBuilder("node", System.getenv("ACCOUNT_BROWSER_SCRIPT"), base);
        runner.environment().put("ACCOUNT_FIXTURE_PASSWORD", PASSWORD);
        runner.environment().put("ACCOUNT_FIXTURE_ADMIN", admin.getEmail());
        runner.environment().put("ACCOUNT_FIXTURE_CUSTOMER", customer.getEmail());
        var process = runner.redirectErrorStream(true).start();
        try {
            assertThat(process.waitFor(150, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            try (var lines = process.inputReader(StandardCharsets.UTF_8)) {
                lines.lines().filter(line -> line.startsWith("[ACCOUNT WebKit]") || line.startsWith("ACCOUNT WebKit failed at"))
                    .forEach(System.out::println);
            }
            assertThat(process.exitValue()).isZero();
        } finally {
            process.destroyForcibly();
            Clients.server.removeContext("/authorize");
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"google", "github"})
    void callbackDenMuonVaThuHoiPhien(String provider) throws Exception {
        var owner = users.saveAndFlush(new NguoiDung(UUID.randomUUID() + "@example.invalid", encoder.encode(PASSWORD), "Fixture"));
        String subject = UUID.randomUUID().toString();
        identities.saveAndFlush(new DanhTinhOAuth(owner, provider, subject, LocalDateTime.now()));
        if (provider.equals("google")) {
            Clients.googleEmail = owner.getEmail();
            Clients.googleSubject = subject;
        } else chungMinh(new BangChung(provider, subject, owner.getEmail(), "Fixture"));
        var actor = users.saveAndFlush(new NguoiDung(UUID.randomUUID()+"@example.invalid",encoder.encode(PASSWORD),"Fixture"));
        jdbc.sql("UPDATE nguoi_dung SET vai_tro='ADMIN' WHERE ma_nguoi_dung=?").param(actor.getMaNguoiDung()).update();
        var admin = (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username",actor.getEmail())
            .param("password",PASSWORD)).andExpect(authenticated()).andReturn().getRequest().getSession(false);
        var sessionsSecurite = new java.util.ArrayList<MockHttpSession>();
        for (int i=0; i<2; i++) {
            sessionsSecurite.add((MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username",owner.getEmail())
                .param("password",PASSWORD)).andExpect(authenticated()).andReturn().getRequest().getSession(false));
        }
        for (boolean mo : List.of(false,true)) {
            var session = new MockHttpSession();
            var url = mvc.perform(get("/oauth2/authorization/"+provider).session(session))
                .andReturn().getResponse().getRedirectedUrl();
            if (provider.equals("google")) Clients.nonce = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("nonce");
            var view = mvc.perform(get("/admin/users").session(admin).param("q",owner.getEmail())).andReturn().getModelAndView();
            var form = ((Map<?,?>) view.getModel().get("lenh")).get(owner.getMaNguoiDung());
            mvc.perform(post("/admin/users/"+owner.getMaNguoiDung()+"/status").session(admin).with(csrf())
                .param("operation",form.toString()).param("reason",mo ? "DA_XU_LY_RUI_RO" : "BAO_VE_TAI_KHOAN"))
                .andExpect(flash().attributeExists("message"));
            var result = mvc.perform(get("/login/oauth2/code/"+provider).session(session).param("code","fixture")
                .param("state",state(url)));
            if (mo) result.andExpect(authenticated());
            else {
                result.andExpect(unauthenticated());
                for (String but : List.of("LIEN_KET","XAC_THUC_LAI")) {
                    mvc.perform(post("/account/security/oauth/"+provider).session(sessionsSecurite.removeFirst())
                        .with(csrf()).param("purpose",but)).andExpect(redirectedUrl("/login?inactive"));
                }
            }
        }
        assertThat(jdbc.sql("SELECT trang_thai FROM thu_trang_thai_tai_khoan WHERE ma_nguoi_dung=? ORDER BY ma_thu")
            .param(owner.getMaNguoiDung()).query(String.class).list()).containsExactly("UNVERIFIED","UNVERIFIED");
    }

    private long taoDonFixture(long owner) {
        return jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan,
                dia_chi_khi_dat, tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan)
            VALUES (?, ?, 'Fixture', '000', 'Fixture', 1000, 1000, 'PENDING_CONFIRMATION', 'COD')
            RETURNING ma_don_hang
            """).params("ORDER-" + UUID.randomUUID(), owner).query(Long.class).single();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"github", "google"})
    void callbackLienKetGiuPhienVaChiGhiSauXacNhanPost(String provider) throws Exception {
        String ownerEmail = UUID.randomUUID() + "@example.invalid";
        var owner = users.saveAndFlush(new NguoiDung(ownerEmail, encoder.encode(PASSWORD), "Owner"));
        String providerEmail = UUID.randomUUID() + "@example.invalid";
        String subject = UUID.randomUUID().toString();
        if (provider.equals("google")) {
            Clients.googleEmail = providerEmail;
            Clients.googleSubject = subject;
        } else {
            chungMinh(new BangChung(provider, subject, providerEmail, "Khác email"));
        }
        var session = (MockHttpSession) mvc.perform(post("/login").with(csrf())
                .param("username", ownerEmail).param("password", PASSWORD))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList())).thenReturn(1L);
        String operation = "/account/security/oauth/" + provider;
        mvc.perform(post(operation).with(csrf()).session(session).param("purpose", "LIEN_KET"))
            .andExpect(redirectedUrl("/account/security")).andExpect(flash().attributeExists("error"));
        mvc.perform(post(operation + "/unlink").with(csrf()).session(session))
            .andExpect(redirectedUrl("/account/security")).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/account/security/reauth/password").with(csrf()).session(session)
                .param("matKhau", PASSWORD)).andExpect(redirectedUrl("/account/security"));
        String path = mvc.perform(post(operation).with(csrf()).session(session)
                .param("purpose", "LIEN_KET")).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(path).contains("operation=");
        var snapshot = GioKhach.doc(session).them(Long.MAX_VALUE, 1);
        snapshot.luu(session);
        String location = mvc.perform(get(URI.create(path)).session(session)).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        var params = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        assertThat(params).doesNotContainKey("prompt");
        if (provider.equals("google")) {
            Clients.nonce = params.getFirst("nonce");
        }
        mvc.perform(get("/login/oauth2/code/" + provider).session(session)
            .param("code", "fixture").param("state", state(location))).andExpect(redirectedUrl("/account/security"));
        assertThat(GioKhach.doc(session)).isEqualTo(snapshot);
        assertThat(users.findByEmail(providerEmail)).isEmpty();
        assertThat(users.findByEmail(ownerEmail).orElseThrow().getPhienBanXacThuc()).isZero();
        var hienThi = mvc.perform(get("/account/security").session(session)).andExpect(status().isOk()).andExpect(authenticated())
            .andExpect(model().attribute("lienKetCho", providerEmail)).andReturn();
        assertThat(hienThi.getResponse().getContentAsString()).contains(providerEmail, "/account/security/oauth/confirm")
            .doesNotContain(owner.getMatKhauBam(), "Khác email", "name=\"subject\"", "name=\"operation\"");
        assertThat(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
            hienThi.getModelAndView().getModel().get("lienKetCho"))).isEqualTo("\"" + providerEmail + "\"");
        mvc.perform(post("/account/security/oauth/confirm").with(csrf()).session(session))
            .andExpect(redirectedUrl("/login?securityUpdated"));
        assertThat(users.findByEmail(ownerEmail).orElseThrow().getPhienBanXacThuc()).isEqualTo(1);
        assertThat(users.findByEmail(ownerEmail).orElseThrow().getMaNguoiDung()).isEqualTo(owner.getMaNguoiDung());
        assertThat(identities.findByNhaCungCapAndMaDinhDanhNhaCungCap(provider, subject).orElseThrow()
            .getNguoiDung().getMaNguoiDung()).isEqualTo(owner.getMaNguoiDung());
    }

    @Test
    void oauthChuaCoMatKhauCanCallbackDungDanhTinhVaOtpDeThietLap() throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        chungMinh(new BangChung("github", "1203", email, "OAuth only"));
        var session = new MockHttpSession();
        callback(session, batDau(session, "/oauth2/authorization/github"), "/");
        mvc.perform(post("/account/security/oauth/github/unlink").with(csrf()).session(session))
            .andExpect(redirectedUrl("/account/security")).andExpect(flash().attributeExists("error"));
        mvc.perform(post("/account/security/password").with(csrf()).session(session)
                .param("matKhauMoi", PASSWORD).param("xacNhan", PASSWORD))
            .andExpect(redirectedUrl("/account/security")).andExpect(flash().attributeExists("error"));
        String path = mvc.perform(post("/account/security/oauth/github").with(csrf()).session(session)
                .param("purpose", "XAC_THUC_LAI")).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        var snapshot = GioKhach.doc(session).them(Long.MAX_VALUE, 1);
        snapshot.luu(session);
        callback(session, batDau(session, path), "/account/security");
        assertThat(GioKhach.doc(session)).isEqualTo(snapshot);
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

    @Test
    void haiPhienGoDongThoiVanConMotDuongDangNhap() throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        var owner = users.saveAndFlush(new NguoiDung(email, null, "Fixture"));
        Clients.googleEmail = email;
        Clients.googleSubject = UUID.randomUUID().toString();
        identities.saveAllAndFlush(List.of(
            new DanhTinhOAuth(owner, "google", Clients.googleSubject, LocalDateTime.now()),
            new DanhTinhOAuth(owner, "github", "1221", LocalDateTime.now())));
        chungMinh(new BangChung("github", "1221", email, "Fixture"));
        when(otp.guiOTP(email, "security")).thenReturn("fixture-challenge");
        when(otp.xacThucOTP("fixture-challenge", "123456")).thenReturn(true);

        var sessions = new java.util.HashMap<String, MockHttpSession>();
        for (String method : List.of("google", "github")) {
            var session = new MockHttpSession();
            callback(session, batDau(session, "/oauth2/authorization/github"), "/");
            String path = mvc.perform(post("/account/security/oauth/" + method).with(csrf()).session(session)
                    .param("purpose", "XAC_THUC_LAI")).andReturn().getResponse().getRedirectedUrl();
            String authorization = mvc.perform(get(URI.create(path)).session(session))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
            Clients.nonce = UriComponentsBuilder.fromUriString(authorization).build().getQueryParams().getFirst("nonce");
            mvc.perform(get("/login/oauth2/code/" + method).session(session)
                .param("state", state(authorization)).param("code", "fixture")).andExpect(redirectedUrl("/account/security"));
            mvc.perform(post("/account/security/email/send").with(csrf()).session(session))
                .andExpect(flash().attributeExists("message"));
            mvc.perform(post("/account/security/email/verify").with(csrf()).session(session).param("ma", "123456"))
                .andExpect(flash().attributeExists("message"));
            sessions.put(method, session);
        }

        var start = new java.util.concurrent.CyclicBarrier(2);
        var results = new java.util.ArrayList<java.util.concurrent.Future<String>>();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            for (String remove : List.of("google", "github")) {
                var session = sessions.get(remove.equals("google") ? "github" : "google");
                results.add(pool.submit(() -> {
                    start.await(10, java.util.concurrent.TimeUnit.SECONDS);
                    return mvc.perform(post("/account/security/oauth/" + remove + "/unlink").with(csrf()).session(session))
                        .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
                }));
            }
            String first = results.get(0).get(15, java.util.concurrent.TimeUnit.SECONDS);
            String second = results.get(1).get(15, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(List.of(first, second)).containsOnlyOnce("/login?securityUpdated");
            assertThat(List.of(first, second)).allMatch(path -> List.of("/account/security", "/login?inactive",
                "/login?securityUpdated").contains(path));
            String remaining = "/login?securityUpdated".equals(first) ? "github" : "google";
            assertThat(identities.docNhaCungCap(owner.getMaNguoiDung())).containsExactly(remaining);
        }
        assertThat(users.findById(owner.getMaNguoiDung()).orElseThrow().getPhienBanXacThuc()).isOne();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE ma_doi_tuong=? AND hanh_dong='OAUTH_UNLINKED'")
            .param(owner.getMaNguoiDung()).query(Integer.class).single()).isOne();
    }

    private void chungMinh(BangChung proof) {
        doReturn(new DefaultOAuth2User(List.of(), Map.of("id", proof.subject(), "beautyshopProof", proof), "id"))
            .when(providerUser).loadUser(any());
    }

    private String batDau(MockHttpSession session, String path) throws Exception {
        String location = mvc.perform(get(URI.create(path)).session(session)).andExpect(status().is3xxRedirection())
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(UriComponentsBuilder.fromUriString(location).build().getQueryParams()).doesNotContainKey("prompt");
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
        private static volatile String googleSubject;

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
                    .subject(googleSubject).issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(300)))
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
