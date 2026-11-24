package com.beautyshop.security;

import com.beautyshop.entity.NguoiDung;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.service.GioHang_Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/** P20: điều hướng sau đăng nhập qua form login thật (CSRF, session, saved request). */
@WebMvcTest(controllers = com.beautyshop.controller.Bootstrap_Controller.class)
@Import({Security_Config.class, LoginSuccess_Handler.class, LoginRedirectTest.Users.class})
class LoginRedirectTest {
    private static final String MAT_KHAU = "mat-khau-thu-nghiem";

    @Autowired MockMvc mvc;
    @MockitoBean GioHang_Service gioHangService;
    @MockitoBean NguoiDung_Repository nguoiDungRepository;

    @TestConfiguration
    static class Users {
        @Bean
        UserDetailsService users(PasswordEncoder encoder) {
            String hash = encoder.encode(MAT_KHAU);
            return new InMemoryUserDetailsManager(
                User.withUsername("admin@example.test").password(hash).roles("ADMIN").build(),
                User.withUsername("khach@example.test").password(hash).roles("CUSTOMER").build());
        }
    }

    private MockHttpSession dangNhap(String email, MockHttpSession session, String duKien) throws Exception {
        mvc.perform(post("/login").session(session).with(csrf()).param("username", email).param("password", MAT_KHAU))
            .andExpect(redirectedUrl(duKien));
        return session;
    }

    @Test
    void adminDangNhapTrucTiepVaoDashboard() throws Exception {
        dangNhap("admin@example.test", new MockHttpSession(), "/admin/dashboard");
    }

    @Test
    void adminBiChanTaiUrlAdminQuayLaiDungUrlDo() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get("/admin/orders/5").session(session)).andExpect(redirectedUrl("http://localhost/login"));
        dangNhap("admin@example.test", session, "/admin/orders/5");
    }

    @Test
    void adminBoQuaUrlNgoaiAdminHoacKhacHost() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(get("/orders/7").session(session)).andExpect(redirectedUrl("http://localhost/login"));
        dangNhap("admin@example.test", session, "/admin/dashboard");

        var khacHost = new MockHttpSession();
        mvc.perform(get("http://evil.example/admin/orders/5").session(khacHost));
        dangNhap("admin@example.test", khacHost, "/admin/dashboard");
    }

    @Test
    void customerGiuNguyenLuongCu() throws Exception {
        dangNhap("khach@example.test", new MockHttpSession(), "/");
        var session = new MockHttpSession();
        mvc.perform(get("/orders/7").session(session)).andExpect(redirectedUrl("http://localhost/login"));
        // Hành vi mặc định của Spring Security 6 (SavedRequestAware + tham số đánh dấu "continue"), không đổi.
        dangNhap("khach@example.test", session, "http://localhost/orders/7?continue");
    }

    @Test
    void customerVanGopGioKhachSauDangNhap() throws Exception {
        var gioHang = mock(GioHang_Service.class);
        when(gioHang.gopSauDangNhap(anyLong(), any())).thenReturn(true);
        var handler = new LoginSuccess_Handler(gioHang);
        var nguoiDung = new NguoiDung("khach@example.test", "x", "Khách");
        ReflectionTestUtils.setField(nguoiDung, "maNguoiDung", 42L);
        ReflectionTestUtils.setField(nguoiDung, "kichHoat", true);
        ReflectionTestUtils.setField(nguoiDung, "vaiTro", NguoiDung.VaiTro.CUSTOMER);
        var phien = new PhienNguoiDung(nguoiDung);
        var request = new MockHttpServletRequest("POST", "/login");
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response,
            UsernamePasswordAuthenticationToken.authenticated(phien, null, phien.getAuthorities()));
        verify(gioHang).gopSauDangNhap(org.mockito.ArgumentMatchers.eq(42L), any());
        assertThat(response.getRedirectedUrl()).isEqualTo("/");
        assertThat(request.getSession().getAttribute(LoginSuccess_Handler.CART_NOTICE)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void kiemTraUrlAdminDaLuu() {
        var request = new MockHttpServletRequest("POST", "/login");
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("http://localhost/admin/orders/5?tab=x", request)).isEqualTo("/admin/orders/5?tab=x");
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("/admin/vouchers", request)).isEqualTo("/admin/vouchers");
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("http://localhost/admin/orders?status=PACKING&continue", request))
            .isEqualTo("/admin/orders?status=PACKING");
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("https://localhost/admin/orders/5", request)).isNull();
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("http://localhost:8443/admin/orders/5", request)).isNull();
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("http://evil.example/admin/orders/5", request)).isNull();
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("//evil.example/admin/orders/5", request)).isNull();
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("http://localhost/admin/../orders/5", request)).isNull();
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("http://localhost/admin/%2e%2e/orders", request)).isNull();
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("http://localhost/administrator", request)).isNull();
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("http://localhost/orders/5", request)).isNull();
        assertThat(LoginSuccess_Handler.duongDanAdminHopLe("javascript:alert(1)", request)).isNull();
    }
}
