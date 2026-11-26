package com.beautyshop.integration;

import com.beautyshop.service.EmailVerification_Service;
import java.util.Locale;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

final class DangKyOtp_TestSupport {
    private DangKyOtp_TestSupport() {}

    static void dangKy(MockMvc mvc, EmailVerification_Service otp, String email, String matKhau) throws Exception {
        String ma = "123456";
        String emailChuan = email.toLowerCase(Locale.ROOT);
        when(otp.guiOTP(emailChuan, "register")).thenReturn(emailChuan);
        when(otp.xacThucOTP(emailChuan, ma)).thenReturn(true);
        var session = new MockHttpSession();
        mvc.perform(post("/register").session(session).with(csrf())
                .param("email", email).param("hoTen", "Khách thử").param("matKhau", matKhau).param("vaiTro", "ADMIN"))
            .andExpect(redirectedUrl("/register")).andExpect(unauthenticated());
        mvc.perform(post("/register/verify-otp").session(session).with(csrf()).param("otp", ma))
            .andExpect(redirectedUrl("/login?registered")).andExpect(unauthenticated());
    }
}
