package com.beautyshop.integration;

import com.beautyshop.model.NguoiDung;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.service.DanhTinhOAuth_Service;
import com.beautyshop.service.DanhTinhOAuth_Service.BangChung;
import com.beautyshop.service.Email_Service;
import com.beautyshop.service.MatKhau_Service;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "beautyshop.auth.public-url=http://localhost:8080")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class XacThucDaPhuongThuc_MariaDb_Test {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired NguoiDung_Repository users;
    @Autowired PasswordEncoder encoder;
    @Autowired DanhTinhOAuth_Service identities;
    @Autowired MatKhau_Service passwords;
    @MockitoBean Email_Service mail;
    @MockitoBean StringRedisTemplate redis;

    private static final String OLD = "password-fixture-01";
    private static final String NEW = "password-fixture-02";

    @Test
    void oauthGiuaCacLanDangNhapNhanDungIdVaKhongGhiDeEmail() throws Exception {
        String googleId = UUID.randomUUID().toString();
        String email = email();
        var created = identities.dangNhap(new BangChung("google", googleId, email, "Khách OAuth"));
        assertThat(created.getMatKhauBam()).isNull();
        assertThat(created.getNgayXacMinhEmail()).isNull();
        var same = identities.dangNhap(new BangChung("google", googleId, email(), "Tên khác"));
        assertThat(same.getMaNguoiDung()).isEqualTo(created.getMaNguoiDung());
        assertThat(same.getEmail()).isEqualTo(email);

        mvc.perform(post("/login").with(csrf()).param("username", email).param("password", OLD))
            .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());

        assertThatThrownBy(() -> identities.dangNhap(new BangChung("github", "801", email, "Khác")))
            .isInstanceOf(AccessDeniedException.class);
        String adminEmail = taoUser(true, true);
        assertThatThrownBy(() -> identities.dangNhap(new BangChung("google", "admin", adminEmail, "Admin")))
            .isInstanceOf(AccessDeniedException.class);
        jdbc.sql("UPDATE nguoi_dung SET kich_hoat = FALSE WHERE email = ?").param(email).update();
        assertThatThrownBy(() -> identities.dangNhap(new BangChung("google", googleId, email, "Khách OAuth")))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void goCanPhuongThucConLaiVaHaiProviderDungCungNguoiDung() {
        String google = UUID.randomUUID().toString();
        var user = identities.dangNhap(new BangChung("google", google, email(), "Khách OAuth"));
        long id = user.getMaNguoiDung();
        assertThatThrownBy(() -> identities.goLienKet(id, 0, "google", "google", Set.of("google")))
            .isInstanceOf(AccessDeniedException.class);
        String github = UUID.randomUUID().toString();
        var proof = new BangChung("github", github, email(), "Khác email");
        identities.lienKet(id, 0, proof);
        assertThat(identities.danhTinhDaGan(proof).getMaNguoiDung()).isEqualTo(id);
        assertThatThrownBy(() -> identities.goLienKet(id, 0, "github", "google", Set.of("google", "github")))
            .isInstanceOf(AccessDeniedException.class);
        identities.goLienKet(id, 1, "github", "google", Set.of("google"));
        assertThatThrownBy(() -> identities.danhTinhDaGan(proof)).isInstanceOf(AccessDeniedException.class);
        assertThat(audit("OAUTH_UNLINKED", id)).isEqualTo(1);
    }

    @Test
    void adminGuiLinkKhachTuDatTokenChiMotLanVaPhienCuBiThuHoi() throws Exception {
        String customer = taoUser(false, true);
        String admin = taoUser(true, true);
        long customerId = users.findByEmail(customer).orElseThrow().getMaNguoiDung();
        long adminId = users.findByEmail(admin).orElseThrow().getMaNguoiDung();
        var customerSession = login(customer);
        var adminSession = login(admin);
        String route = "/admin/users/" + customerId + "/password-reset";
        mvc.perform(get("/account/security").session(customerSession)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Bảo mật và phương thức đăng nhập")));
        mvc.perform(post(route).session(customerSession).with(csrf()).param("reason", "KHACH_YEU_CAU"))
            .andExpect(status().isForbidden());
        mvc.perform(post(route).session(adminSession).param("reason", "KHACH_YEU_CAU"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/admin/users/" + customerId).session(adminSession).param("q", "Fixture").param("page", "2"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Lý do hỗ trợ")))
            .andExpect(content().string(containsString("href=\"/admin/users?q=Fixture&amp;page=2\"")));
        mvc.perform(post(route).session(adminSession).with(csrf()).param("reason", "KHACH_YEU_CAU"))
            .andExpect(status().is3xxRedirection());
        assertThat(audit("ADMIN_PASSWORD_RESET_REQUESTED", customerId)).isEqualTo(1);
        assertThat(jdbc.sql("""
            SELECT ma_nguoi_thuc_hien FROM nhat_ky_quan_tri
            WHERE ma_doi_tuong=? AND hanh_dong='ADMIN_PASSWORD_RESET_REQUESTED'
            """).param(customerId).query(Long.class).single()).isEqualTo(adminId);
        assertThat(audit("PASSWORD_RESET_COMPLETED", customerId)).isZero();
        assertThat(encoder.matches(OLD, users.findByEmail(customer).orElseThrow().getMatKhauBam())).isTrue();
        String token = token(customer);

        var scanner = new MockHttpSession();
        mvc.perform(get("/reset-password").param("token", token).session(scanner))
            .andExpect(redirectedUrl("/reset-password")).andExpect(header().string("Referrer-Policy", "no-referrer"));
        mvc.perform(get("/reset-password").session(scanner)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Mật khẩu mới")));
        assertThat(audit("PASSWORD_RESET_COMPLETED", customerId)).isZero();

        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> passwords.datLai(token, NEW));
            var b = pool.submit(() -> passwords.datLai(token, NEW));
            assertThat(java.util.List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        }
        assertThat(audit("PASSWORD_RESET_COMPLETED", customerId)).isEqualTo(1);
        assertThat(audit("SESSION_REVOKED", customerId)).isEqualTo(1);
        assertThat(encoder.matches(NEW, users.findByEmail(customer).orElseThrow().getMatKhauBam())).isTrue();
        mvc.perform(get("/account").session(customerSession)).andExpect(redirectedUrl("/login?inactive"));
        verify(mail).baoDaDoiMatKhau(customer);
    }

    @Test
    void smtpThatBaiKhongDoiHashVaHetHanLink() throws Exception {
        String customer = taoUser(false, true);
        String admin = taoUser(true, true);
        long id = users.findByEmail(customer).orElseThrow().getMaNguoiDung();
        doThrow(new IllegalStateException()).when(mail).guiLinkDatLai(eq(customer), anyString());
        mvc.perform(post("/admin/users/" + id + "/password-reset").session(login(admin)).with(csrf())
                .param("reason", "HO_TRO_BAO_MAT"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("error"));
        assertThat(passwords.datLai(token(customer), NEW)).isFalse();
        assertThat(audit("PASSWORD_RESET_COMPLETED", id)).isZero();
        assertThat(encoder.matches(OLD, users.findByEmail(customer).orElseThrow().getMatKhauBam())).isTrue();
    }

    @Test
    void guestThayCungPhanHoiVaPostCanCsrf() throws Exception {
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisScript<Long>>any(), anyList())).thenReturn(1L);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), any(java.time.Duration.class))).thenReturn(true);
        mvc.perform(get("/forgot-password")).andExpect(status().isOk());
        mvc.perform(post("/forgot-password").param("email", email())).andExpect(status().isForbidden());
        String customer = taoUser(false, true);
        for (String email : Set.of(customer, email())) {
            mvc.perform(post("/forgot-password").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attribute("message", "Nếu tài khoản đủ điều kiện, bạn sẽ nhận được email hướng dẫn."));
        }
        var link = ArgumentCaptor.forClass(String.class);
        verify(mail, timeout(5000)).guiLinkDatLai(eq(customer), link.capture());
        assertThat(audit("PASSWORD_RESET_REQUESTED", users.findByEmail(customer).orElseThrow().getMaNguoiDung()))
            .isEqualTo(1);
        var session = new MockHttpSession();
        mvc.perform(get("/reset-password").session(session).param("token", link.getValue().split("token=", 2)[1]))
            .andExpect(redirectedUrl("/reset-password"));
        mvc.perform(post("/reset-password").session(session).param("matKhauMoi", NEW).param("xacNhan", NEW))
            .andExpect(status().isForbidden());
        mvc.perform(post("/reset-password").session(session).with(csrf())
                .param("matKhauMoi", NEW).param("xacNhan", NEW))
            .andExpect(redirectedUrl("/login?passwordChanged"));
    }

    private String token(String customer) {
        var link = ArgumentCaptor.forClass(String.class);
        verify(mail).guiLinkDatLai(eq(customer), link.capture());
        return link.getValue().split("token=", 2)[1];
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", email).param("password", OLD))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }

    private long audit(String action, long id) {
        return jdbc.sql("SELECT count(*) FROM nhat_ky_quan_tri WHERE hanh_dong=? AND ma_doi_tuong=?")
            .params(action, id).query(Long.class).single();
    }

    private String taoUser(boolean admin, boolean verified) {
        String email = email();
        var user = new NguoiDung(email, encoder.encode(OLD), "Account test");
        if (verified) {
            user.xacNhanEmail(LocalDateTime.now(ZoneOffset.UTC));
        }
        users.saveAndFlush(user);
        if (admin) {
            jdbc.sql("UPDATE nguoi_dung SET vai_tro = 'ADMIN' WHERE email = ?").param(email).update();
        }
        return email;
    }

    private static String email() { return UUID.randomUUID() + "@example.invalid"; }
}
