package com.beautyshop.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties="app.ai.enabled=false")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class TuVanFaq_MariaDb_Test {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired com.beautyshop.ai.AiMetrics metrics;

    @Test
    @EnabledIfEnvironmentVariable(named="AI_OPS_BROWSER", matches=".+")
    void webKitFaqVaTrangThaiChuaKiem() throws Exception {
        AiOpsBrowser.run(port, jdbc, "disabled");
        assertThat(metrics.snapshot().lastAttempt()).isNull();
    }

    @Test
    void faqTimTiengVietLocNguonVaEscapeHtml() throws Exception {
        jdbc.sql("""
            INSERT INTO bai_viet_faq (cau_hoi, cau_tra_loi, url_nguon)
            VALUES ('Xylophone mô phỏng', 'Số lượng < 5 & còn hàng', '/products'),
                   ('Quartz mô phỏng', 'Nguồn không hợp lệ', 'javascript:alert(1)')
            """).update();
        mvc.perform(get("/chat")).andExpect(status().isOk());
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "Doi tra trong bao lau?"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Trả lời từ câu hỏi thường gặp")))
            .andExpect(content().string(containsString("7 ngày")))
            .andExpect(content().string(containsString("href=\"/account/returns\"")));
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "Xylophone"))
            .andExpect(content().string(containsString("&lt; 5 &amp; còn hàng")))
            .andExpect(content().string(not(containsString("<script>demoUnsafe()"))));
        mvc.perform(post("/chat").with(csrf()).param("cauHoi", "Quartz"))
            .andExpect(content().string(containsString("Chưa đủ thông tin")))
            .andExpect(content().string(not(containsString("javascript:alert(1)"))));
    }

    @Test
    void faqMauVeBienTheDungCungNoiDungTrongMariaDbVaTrangTuVan() throws Exception {
        String cauHoi = "Xem giá và lượng còn hàng của sản phẩm ở đâu?";
        String cauTraLoi = jdbc.sql("SELECT cau_tra_loi FROM bai_viet_faq WHERE cau_hoi = ?")
            .param(cauHoi).query(String.class).single();
        assertThat(cauTraLoi).contains("chọn biến thể sản phẩm", "số lượng còn mua được").doesNotContain("SKU");

        mvc.perform(post("/chat").with(csrf()).param("cauHoi", cauHoi))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(cauTraLoi)));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles="CUSTOMER")
    void duongDanTuVanCuDaGo() throws Exception {
        mvc.perform(get("/tu-van-da")).andExpect(status().isNotFound());
        mvc.perform(post("/tu-van-da").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void csrfGioiHanKyTuVaTanSuat() throws Exception {
        var before = metrics.snapshot().rows().getFirst();
        var reasons = metrics.snapshot();
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
        var after = metrics.snapshot().rows().getFirst();
        assertThat(after.total() - before.total()).isEqualTo(9);
        assertThat(after.rejected() - before.rejected()).isEqualTo(2);
        assertThat(after.rateLimited() - before.rateLimited()).isOne();
        assertThat(metrics.snapshot().denied() - reasons.denied()).isOne();
        assertThat(metrics.snapshot().invalid() - reasons.invalid()).isOne();
        assertThat(metrics.snapshot().lastResponse()).isNull();
    }
}
