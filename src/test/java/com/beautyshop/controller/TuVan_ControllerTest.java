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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
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
                        "Tìm FAQ · không phải AI",
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
                .andExpect(content().string(containsString("Tồn khả dụng:")));
        verify(tuVanService).hoi("Đổi trả trong bao lâu?");

        mvc.perform(post("/chat").param("cauHoi", "COD"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cauHoiQuaDaiKhongGoiService() throws Exception {
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "a".repeat(501)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "cauHoi"));

        verifyNoInteractions(tuVanService);
    }
}
