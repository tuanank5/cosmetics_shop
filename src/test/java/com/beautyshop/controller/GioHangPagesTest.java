package com.beautyshop.controller;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.model.NguoiDung;
import com.beautyshop.repository.GioHang_Repository.MucGio;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.security.Security_Config;
import com.beautyshop.service.GioHang_Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GioHang_Controller.class)
@Import(Security_Config.class)
class GioHangPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean GioHang_Service gioHang;
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDung;

    @Test
    void themGuestDangDoiMutexSeTheoPhienKhachVuaDangNhap() throws Exception {
        var user = new NguoiDung("fixture@example.invalid", "", "Fixture");
        ReflectionTestUtils.setField(user, "maNguoiDung", 42L);
        var principal = BeanUtils.instantiateClass(PhienNguoiDung.class.getDeclaredConstructor(NguoiDung.class), user);
        var session = new MockHttpSession();
        GioKhach.doc(session).them(11L, 5).luu(session);
        var started = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            java.util.concurrent.Future<String> pending;
            synchronized (WebUtils.getSessionMutex(session)) {
                pending = executor.submit(() -> {
                    started.countDown();
                    return new GioHang_Controller(gioHang).them(11L, null, session, new RedirectAttributesModelMap());
                });
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities())));
                GioKhach.xoa(session);
            }
            assertThat(pending.get(5, TimeUnit.SECONDS)).isEqualTo("redirect:/cart");
        }
        verify(gioHang).themGioCustomer(42L, 11L);
        assertThat(GioKhach.doc(session).soLuong()).isEmpty();
    }

    @Test
    void gioTrongCoLoiMuaSamVaKhongCoNutThanhToan() throws Exception {
        mvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Giỏ hàng đang trống.")))
            .andExpect(content().string(containsString("Khám phá sản phẩm")))
            .andExpect(content().string(not(containsString("href=\"/checkout\""))));
    }

    @Test
    void tamTinhChinhXacTheoSoLuongTrongGioKeCaKhiTonGiam() throws Exception {
        var lon = new MucGio(11, "Tinh chất <mẫu>", "30 ml", new BigDecimal("999999999999999"), 3, 10, "QA-11",
            null, null, null);
        var nho = new MucGio(22, "Kem dưỡng", "50 g", new BigDecimal("7"), 0, 1, "QA-22",
            null, null, null);
        when(gioHang.docGioKhach(any())).thenReturn(List.of(lon, nho));
        mvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("9.999.999.999.999.990 ₫")))
            .andExpect(content().string(containsString("9.999.999.999.999.997 ₫")))
            .andExpect(content().string(containsString("Tinh chất &lt;mẫu&gt;")))
            .andExpect(content().string(containsString("Hãy xóa biến thể này khỏi giỏ.")))
            .andExpect(content().string(containsString("Biến thể hiện không còn hàng.")))
            .andExpect(content().string(containsString("action=\"/cart/items/11/quantity\"")))
            .andExpect(content().string(containsString("name=\"quantity\" value=\"0\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("href=\"/checkout\"")));
    }

    @Test
    void tuCapNhatChiLaTangTangCuongVaGiuNguyenFormKhongJs() throws Exception {
        var muc = new MucGio(11, "Kem dưỡng", "50 g", new BigDecimal("245000"), 5, 2, "QA-11", null, null, null);
        when(gioHang.docGioKhach(any())).thenReturn(List.of(muc));
        String html = mvc.perform(get("/cart")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString().replaceAll("\\s+", " ");
        org.assertj.core.api.Assertions.assertThat(html)
            .contains("data-cart-line=\"11\"", "data-cart-live", "aria-live=\"polite\"", "class=\"cart-progress\"",
                "<script defer src=\"/js/cart.js\"></script>",
                "action=\"/cart/items/11/quantity\"", "name=\"quantity\"", "name=\"_csrf\"",
                "type=\"submit\" class=\"btn btn--secondary\" aria-label=\"Cập nhật số lượng cho mã biến thể sản phẩm QA-11\">Cập nhật</button>",
                "data-zero-confirm=", "id=\"error-11\"", "id=\"stock-11\"", "490.000 ₫")
            .doesNotContain("style=", "onclick=", "th:utext")
            .containsOnlyOnce("<h1");
    }

    @Test
    void postSoLuongVanPrgVeGioHang() throws Exception {
        when(gioHang.datSoLuongKhach(any(), eq(11L), eq(3))).thenReturn(new GioKhach(null, java.util.Map.of(11L, 3)));
        mvc.perform(post("/cart/items/11/quantity").param("quantity", "3").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cart"))
            .andExpect(flash().attribute("message", "Đã cập nhật số lượng."));
        mvc.perform(post("/cart/items/11/quantity").param("quantity", "3"))
            .andExpect(status().isForbidden());
    }

    @Test
    void anhDemoTrongJarHienVoiKieuMimeDung() throws Exception {
        var webp = new MucGio(11, "Moisturizing Cream", "Hũ demo", new BigDecimal("355000"), 6, 1,
            "DEMO-001", "/img/products/r001.webp", "CeraVe", "demo-r001");
        var jpg = new MucGio(22, "Water Gel", "Gel demo", new BigDecimal("395000"), 6, 1,
            "DEMO-002", "/img/products/r002.jpg", "CeraVe", "demo-r002");
        when(gioHang.docGioKhach(any())).thenReturn(List.of(webp, jpg));
        String html = mvc.perform(get("/cart")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString().replaceAll("\\s+", " ");
        org.assertj.core.api.Assertions.assertThat(html)
            .contains("data=\"/img/products/r001.webp\" type=\"image/webp\"",
                "data=\"/img/products/r002.jpg\" type=\"image/jpeg\"");
    }

    @Test
    void haiSkuCungSanPhamGiuDungAnhThuongHieuVaLienKet() throws Exception {
        String url = "/uploads/sku/00000000-0000-4000-8000-000000000026.png";
        var lon = new MucGio(11, "Moisturizing Cream", "16 oz", new BigDecimal("410000"), 6, 2,
            "DEMO-026", url, "Vanicream", "demo-r026");
        var nho = new MucGio(22, "Moisturizing Cream", "4 oz", new BigDecimal("245000"), 0, 1,
            "DEMO-026-B", null, "Vanicream", "demo-r026");
        when(gioHang.docGioKhach(any())).thenReturn(List.of(lon, nho));
        mvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Vanicream")))
            .andExpect(content().string(containsString("data=\"" + url + "\"")))
            .andExpect(content().string(containsString("href=\"/products/demo-r026?variant=11&amp;from=cart&amp;cartVariant=11\"")))
            .andExpect(content().string(containsString("href=\"/products/demo-r026?variant=22&amp;from=cart&amp;cartVariant=22\"")))
            .andExpect(content().string(containsString("aria-label=\"Moisturizing Cream, 4 oz (mã biến thể sản phẩm DEMO-026-B)\"")))
            .andExpect(content().string(containsString("1.065.000 ₫")));
    }

    @Test
    void thieuMetadataVanHienDongGioVaKhongNapAnhNgoaiRoute() throws Exception {
        var muc = new MucGio(11, "Moisturizing Cream", "16 oz", new BigDecimal("410000"), 0, 1,
            "DEMO-026", "https://example.invalid/photo.png", null, null);
        when(gioHang.docGioKhach(any())).thenReturn(List.of(muc));
        mvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<span>Moisturizing Cream</span>")))
            .andExpect(content().string(containsString("class=\"cart-monogram\"")))
            .andExpect(content().string(not(containsString("<object"))))
            .andExpect(content().string(not(containsString("?variant="))))
            .andExpect(content().string(containsString("action=\"/cart/items/11/quantity\"")))
            .andExpect(content().string(containsString("Biến thể hiện không còn hàng.")));
    }

    @Test
    void xoaVanGuiQuantityBangKhongVaCanCsrf() throws Exception {
        mvc.perform(post("/cart/items/11/quantity").param("quantity", "0"))
            .andExpect(status().isForbidden());
        when(gioHang.datSoLuongKhach(any(), eq(11L), eq(0))).thenReturn(new GioKhach(null, java.util.Map.of()));
        mvc.perform(post("/cart/items/11/quantity").param("quantity", "0").with(csrf()))
            .andExpect(redirectedUrl("/cart"))
            .andExpect(flash().attribute("message", "Đã xóa biến thể sản phẩm khỏi giỏ hàng."));
        verify(gioHang).datSoLuongKhach(any(), eq(11L), eq(0));
    }
}
