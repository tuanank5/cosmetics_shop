package com.beautyshop.controller;

import com.beautyshop.security.BootstrapUserDetails_Service;
import com.beautyshop.security.Security_Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Bootstrap_Controller.class)
@Import({Security_Config.class, BootstrapUserDetails_Service.class})
class BootstrapPagesTest {
    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @CsvSource({
        "/, Vẻ đẹp bắt đầu từ",
        "/products, Danh mục đang được chuẩn bị",
        "/login, Đăng nhập chưa mở"
    })
    void publicPagesRenderInVietnamese(String path, String heading) throws Exception {
        mvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(content().encoding("UTF-8"))
            .andExpect(content().string(containsString(heading)))
            .andExpect(content().string(containsString("class=\"nav-shell\"")))
            .andExpect(content().string(containsString("class=\"site-footer\"")))
            .andExpect(content().string(containsString("href=\"/css/theme.css\"")));
    }

    @Test
    void loginRequiresCsrfAndRejectsCredentialsInScaffold() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(post("/login"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/login").with(csrf())
                .param("username", "placeholder")
                .param("password", "placeholder"))
            .andExpect(redirectedUrl("/login?error"));
    }
}
