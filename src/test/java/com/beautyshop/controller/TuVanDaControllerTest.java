package com.beautyshop.controller;

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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TuVanDa_Controller.class)
@Import(Security_Config.class)
class TuVanDaControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean LoginSuccess_Handler login;
    @MockitoBean NguoiDung_Repository users;
    @MockitoBean TuVanDaService formerAdvisor;

    @Test
    void allRolesReachTheSameChatFromTheFormerAdvisor() throws Exception {
        mvc.perform(get("/tu-van-da"))
            .andExpect(redirectedUrl("/chat"));
        mvc.perform(get("/tu-van-da").with(user("customer").roles("CUSTOMER")))
            .andExpect(redirectedUrl("/chat"));
        mvc.perform(get("/tu-van-da").with(user("admin").roles("ADMIN")))
            .andExpect(redirectedUrl("/chat"));
        verifyNoInteractions(formerAdvisor);
    }

    @Test
    void legacySubmitRequiresCsrfAndDoesNotForwardAnythingToChat() throws Exception {
        mvc.perform(post("/tu-van-da").param("vanDeDa", "qa@example.invalid"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/tu-van-da").with(csrf()).param("vanDeDa", "qa@example.invalid"))
            .andExpect(status().isSeeOther())
            .andExpect(redirectedUrl("/chat"));
        verifyNoInteractions(formerAdvisor);
    }
}
