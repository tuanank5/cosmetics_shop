package com.beautyshop.controller;

import com.beautyshop.ai.dto.TuVanDaRequest;
import com.beautyshop.ai.service.TuVanDaService;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.security.Security_Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TuVanDa_Controller.class)
@Import(Security_Config.class)
class TuVanDaControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDungRepository;
    @MockitoBean TuVanDaService tuVanDaService;

    @Test
    void trangTuVanMoChoGuestCustomerVaAdmin() throws Exception {
        mvc.perform(get("/tu-van-da"))
            .andExpect(status().isOk())
            .andExpect(view().name("chat/TuVanDa"))
            .andExpect(content().string(containsString("id=\"main\"")))
            .andExpect(content().string(containsString("maxlength=\"1000\"")));
        mvc.perform(get("/tu-van-da").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk());
        mvc.perform(get("/tu-van-da").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());
    }

    @Test
    void postCanTuVanCanCsrfVaHienKetQuaDaXacMinh() throws Exception {
        when(tuVanDaService.tuVan(any(TuVanDaRequest.class)))
            .thenReturn("Serum hiện có trong catalog.");

        mvc.perform(post("/tu-van-da").with(csrf())
                .param("loaiDa", "Da dầu")
                .param("vanDeDa", "Tôi cần serum.")
                .param("daNhayCam", "Ít nhạy cảm")
                .param("nhuCau", "Dưỡng ẩm"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Serum hiện có trong catalog.")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tôi cần serum.")));

        mvc.perform(post("/tu-van-da")
                .param("loaiDa", "Da dầu")
                .param("vanDeDa", "Tôi cần serum.")
                .param("daNhayCam", "Ít nhạy cảm")
                .param("nhuCau", "Dưỡng ẩm"))
            .andExpect(status().isForbidden());
    }

    @Test
    void loiKetNoiHienThongBaoVaGiuNoiDungNguoiDung() throws Exception {
        when(tuVanDaService.tuVan(any(TuVanDaRequest.class)))
            .thenThrow(new ResourceAccessException("Ollama unavailable"));

        mvc.perform(post("/tu-van-da").with(csrf())
                .param("loaiDa", "Da dầu")
                .param("vanDeDa", "Tôi cần serum.")
                .param("daNhayCam", "Ít nhạy cảm")
                .param("nhuCau", "Dưỡng ẩm"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("AI tư vấn hiện chưa sẵn sàng.")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tôi cần serum.")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Ollama unavailable"))));
    }

    @Test
    void duLieuKhongHopLeKhongGoiService() throws Exception {
        mvc.perform(post("/tu-van-da").with(csrf())
                .param("loaiDa", "unknown")
                .param("vanDeDa", "Tôi cần serum.")
                .param("daNhayCam", "Ít nhạy cảm")
                .param("nhuCau", "Dưỡng ẩm"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "loaiDa"))
            .andExpect(content().string(containsString("aria-describedby=\"loai-da-error\"")))
            .andExpect(content().string(containsString("id=\"loai-da-error\"")));

        verifyNoInteractions(tuVanDaService);
    }
}
