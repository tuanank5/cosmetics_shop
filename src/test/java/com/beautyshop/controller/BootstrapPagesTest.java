package com.beautyshop.controller;

import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.Security_Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Bootstrap_Controller.class)
@Import(Security_Config.class)
class BootstrapPagesTest {
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDungRepository;
    @MockitoBean UserDetailsService userDetailsService;
    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @CsvSource({
        "/, Vẻ đẹp.",
        "/login, Đăng nhập"
    })
    void publicPagesRenderInVietnamese(String path, String heading) throws Exception {
        mvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(content().encoding("UTF-8"))
            .andExpect(content().string(containsString(heading)))
            .andExpect(content().string(containsString("class=\"nav-shell\"")))
            .andExpect(content().string(containsString("class=\"site-footer\"")))
            .andExpect(content().string(containsString("href=\"/css/tokens.css\"")));
    }

    @Test
    void homeLinksToTheExistingCatalogDiscoveryControls() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/\" aria-current=\"page\"")))
            .andExpect(content().string(containsString("href=\"/products#categories\"")))
            .andExpect(content().string(containsString("href=\"/products#brands\"")))
            .andExpect(content().string(containsString("href=\"/products#skin-type\"")))
            .andExpect(content().string(containsString("href=\"/products?sort=newest\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GUEST", "CUSTOMER", "ADMIN"})
    void heroMotionLoadsLocallyInOrderOnlyOnHome(String role) throws Exception {
        var home = get("/");
        var login = get("/login");
        if (!role.equals("GUEST")) {
            home.with(user("fixture").roles(role));
            login.with(user("fixture").roles(role));
        }
        String html = mvc.perform(home).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(html).containsSubsequence(
            "<script defer src=\"/js/vendor/gsap-3.15.1.min.js\"></script>",
            "<script defer src=\"/js/home-motion.js\"></script>");
        assertThat(html).containsOnlyOnce("/js/vendor/gsap-3.15.1.min.js")
            .containsOnlyOnce("/js/home-motion.js")
            .contains("class=\"shop-hero-art\" aria-hidden=\"true\"", "id=\"home-title\"");
        String otherPage = mvc.perform(login).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(otherPage).doesNotContain("gsap-", "home-motion.js");
    }

    @Test
    void editorialMediaIsOptionalAndDeferredWithoutChangingDiscoveryLinks() throws Exception {
        String plain = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(plain).contains("data-story", "/img/editorial/nocturne.svg", "data-home-carousel", "home-carousel-pagination")
            .doesNotContain("data-story-video", "ScrollTrigger");
        assertThat(plain).containsOnlyOnce("/js/vendor/swiper-14.4.0.min.js")
            .containsOnlyOnce("/css/vendor/swiper-14.4.0.min.css").contains("/js/home-carousel.js", "id=\"home-footer\"");
        String media = mvc.perform(get("/").flashAttr("homeEditorialMedia", java.util.Map.of(
                "opening", java.util.Map.of("previewVideoUrl", "/img/editorial/approved.webm"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(media).contains("data-story-video", "data-video-src=\"/img/editorial/approved.webm\"", "preload=\"none\"")
            .doesNotContain("<video src=", "autoplay=");
        String login = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString();
        assertThat(login).doesNotContain("ScrollTrigger", "home-story.js", "swiper-14.4.0", "home-carousel.js");
    }

    @Test
    void loginRequiresCsrfAndRejectsUnknownUser() throws Exception {
        org.mockito.Mockito.when(userDetailsService.loadUserByUsername("placeholder"))
            .thenThrow(new UsernameNotFoundException("Tài khoản không tồn tại."));
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
