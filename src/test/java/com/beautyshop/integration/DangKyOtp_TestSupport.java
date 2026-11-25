package com.beautyshop.integration;

import com.beautyshop.service.XacThucEmail_Service;
import java.util.Locale;
import java.util.UUID;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

final class DangKyOtp_TestSupport {
    private DangKyOtp_TestSupport() {}

    static void dangKy(MockMvc mvc, XacThucEmail_Service otp, String email, String matKhau) throws Exception {
        String ma = String.format(Locale.ROOT, "%06d", new java.security.SecureRandom().nextInt(1_000_000));
        when(otp.guiMa(any(UUID.class), eq(email.toLowerCase(Locale.ROOT)))).thenReturn(true);
        when(otp.xacThuc(any(UUID.class), eq(ma))).thenReturn(true);

        var session = new MockHttpSession();
        mvc.perform(post("/register").session(session).with(csrf()).param("email", email).param("hoTen", "Khách thử")
                .param("matKhau", matKhau).param("vaiTro", "ADMIN"))
            .andExpect(redirectedUrl("/register")).andExpect(unauthenticated());
        mvc.perform(post("/register/verify-otp").session(session).with(csrf()).param("otp", ma))
            .andExpect(redirectedUrl("/login?registered")).andExpect(unauthenticated());
    }
}
