package com.beautyshop.controller;

import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.security.Security_Config;
import com.beautyshop.service.TuVan_Service;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TuVan_Controller.class)
@Import(Security_Config.class)
class TuVan_ControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDungRepository;
    @MockitoBean TuVan_Service tuVanService;

    @Test
    void trangChatRenderDungFormFaqChoGuestVaTaiKhoanDangNhap() throws Exception {
        mvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("TuVanDa"))
                .andExpect(content().string(containsString("action=\"/chat\"")))
                .andExpect(content().string(containsString("name=\"cauHoi\"")))
                .andExpect(content().string(containsString("Gửi câu hỏi")));

        mvc.perform(get("/chat").with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isOk());
        mvc.perform(get("/chat").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void guiCauHoiHienCauTraLoiVaNguonDaKiemTra() throws Exception {
        when(tuVanService.hoi("Đổi trả trong bao lâu?")).thenReturn(
                new TuVan_Service.TraLoi(
                        "Trả lời từ câu hỏi thường gặp",
                        "Bạn có thể gửi yêu cầu đổi trả trong 7 ngày.",
                        "Chính sách đổi trả",
                        "/account/returns",
                        new BigDecimal("125000"),
                        3));

        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "Đổi trả trong bao lâu?"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn có thể gửi yêu cầu đổi trả trong 7 ngày.")))
                .andExpect(content().string(containsString("Nguồn: Chính sách đổi trả")))
                .andExpect(content().string(containsString("href=\"/account/returns\"")))
                .andExpect(content().string(containsString("125.000 ₫")))
                .andExpect(content().string(containsString("Đổi trả trong bao lâu?</textarea>")))
                .andExpect(content().string(containsString("Số lượng còn mua được:")));
        verify(tuVanService).hoi("Đổi trả trong bao lâu?");

        mvc.perform(post("/chat").param("cauHoi", "COD"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatEscapesContentAndShowsVerifiedFaqWhenAiIsUnavailable() throws Exception {
        when(tuVanService.hoi("Có đổi trả không?")).thenReturn(new TuVan_Service.TraLoi(
            "Trả lời từ câu hỏi thường gặp", "<em>Thông tin đã kiểm tra</em>", "Đổi trả",
            "/account/returns", null, null, false, true));

        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "Có đổi trả không?"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("AI hiện chưa sẵn sàng")))
            .andExpect(content().string(containsString("&lt;em&gt;Thông tin đã kiểm tra&lt;/em&gt;")))
            .andExpect(content().string(not(containsString("<em>Thông tin đã kiểm tra</em>"))));
    }

    @Test
    void cauHoiQuaDaiKhongGoiService() throws Exception {
        String email = "qa@example.invalid";
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", email + "a".repeat(501)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "cauHoi"))
                .andExpect(content().string(containsString("Câu hỏi tối đa 500 ký tự.")))
                .andExpect(content().string(not(containsString(email))));

        verifyNoInteractions(tuVanService);
    }

    @Test
    void cauHoiVuotGioiHanKhongHienLaiHoacGoiService() throws Exception {
        var session = new MockHttpSession();
        String cauHoi = "Có hỗ trợ đổi trả?";
        when(tuVanService.hoi(cauHoi)).thenReturn(
                new TuVan_Service.TraLoi("FAQ", "Xem chính sách đổi trả.", null, null, null, null));
        for (int i = 0; i < 6; i++) {
            mvc.perform(post("/chat").with(csrf()).session(session).param("cauHoi", cauHoi))
                    .andExpect(status().isOk());
        }

        String email = "qa@example.invalid";
        mvc.perform(post("/chat").with(csrf()).session(session).param("cauHoi", email))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(content().string(containsString("Bạn vừa gửi nhiều câu hỏi.")))
                .andExpect(content().string(not(containsString(email))));

        verify(tuVanService, times(6)).hoi(cauHoi);
        verifyNoMoreInteractions(tuVanService);
    }
}
