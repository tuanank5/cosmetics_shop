package com.beautyshop.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class TuVanFaq_MariaDb_Test {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;

    @Test
    void faqTimTiengVietLocNguonVaEscapeHtml() throws Exception {
        jdbc.sql("""
            INSERT INTO bai_viet_faq (cau_hoi, cau_tra_loi, url_nguon)
            VALUES ('Xylophone mô phỏng', '<script>demoUnsafe()</script>', '/products'),
                   ('Quartz mô phỏng', 'Nguồn không hợp lệ', 'javascript:alert(1)')
            """).update();
        mvc.perform(get("/chat")).andExpect(status().isOk());
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "Doi tra trong bao lau?"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("không phải AI")))
            .andExpect(content().string(containsString("7 ngày")))
            .andExpect(content().string(containsString("href=\"/account/returns\"")));
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "Xylophone"))
            .andExpect(content().string(containsString("&lt;script&gt;demoUnsafe()&lt;/script&gt;")))
            .andExpect(content().string(not(containsString("<script>demoUnsafe()"))));
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "Quartz"))
            .andExpect(content().string(containsString("Chưa đủ thông tin")))
            .andExpect(content().string(not(containsString("javascript:alert(1)"))));
    }

    @Test
    void csrfGioiHanKyTuVaTanSuat() throws Exception {
        mvc.perform(post("/chat").param("cauHoi", "COD")).andExpect(status().isForbidden());
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "a".repeat(501)))
            .andExpect(model().attributeHasFieldErrors("form", "cauHoi"));
        mvc.perform(get("/admin/ai")).andExpect(redirectedUrl("http://localhost/login"));
        var session = new MockHttpSession();
        for (int i = 0; i < 6; i++) {
            mvc.perform(post("/chat").session(session).with(csrf()).param("cauHoi", "COD")).andExpect(status().isOk());
        }
        mvc.perform(post("/chat").session(session).with(csrf()).param("cauHoi", "COD"))
            .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
    }
}
