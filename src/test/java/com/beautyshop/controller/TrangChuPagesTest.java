package com.beautyshop.controller;

import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.security.Security_Config;
import com.beautyshop.service.CuaHang_Service;
import com.beautyshop.service.GoiYSanPham_Service;
import com.beautyshop.service.TrangChu_Service;
import com.beautyshop.service.TrangChu_Service.BuocNghiThuc;
import com.beautyshop.service.TrangChu_Service.HoatChat;
import com.beautyshop.service.TrangChu_Service.LoaiDa;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P18: trang chủ sáu chương, dữ liệu chỉ đọc, chương lỗi rơi về bản tĩnh. */
@WebMvcTest(Bootstrap_Controller.class)
@Import(Security_Config.class)
class TrangChuPagesTest {
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDungRepository;
    @MockitoBean CuaHang_Service cuaHangService;
    @MockitoBean TrangChu_Service trangChuService;
    @MockitoBean GoiYSanPham_Service goiYService;
    @Autowired MockMvc mvc;

    private String trangChu() throws Exception {
        return mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void sauChuongTheoThuTuVoiNeoVaTieuDeRieng() throws Exception {
        when(trangChuService.loaiDa()).thenReturn(List.of(
            new LoaiDa("Da khô", "Da khô", 9, "Thường cảm thấy căng, cần thêm độ ẩm."),
            new LoaiDa("Da nhạy cảm", "Da nhạy cảm", 7, null)));
        when(trangChuService.nghiThuc()).thenReturn(List.of(
            new BuocNghiThuc(1, "Làm sạch", "Loại bỏ bụi bẩn và dầu thừa.", "lam-sach"),
            new BuocNghiThuc(2, "Cân bằng", "Chuẩn bị da cho các bước tiếp theo.", null)));
        when(trangChuService.hoatChat()).thenReturn(List.of(
            new HoatChat("Hyaluronic Acid", "hyaluronic", "Hoạt chất giữ nước.", 5)));
        var the = mock(TheSanPham.class);
        when(the.getDuongDan()).thenReturn("demo-r001");
        when(the.getTenSanPham()).thenReturn("Moisturizing Cream");
        when(the.getGiaTu()).thenReturn(new BigDecimal("355000"));
        when(the.getSoLuongKhaDungLonNhat()).thenReturn(12);
        when(cuaHangService.sanPhamNoiBat(4)).thenReturn(List.of(the));

        String html = trangChu();

        assertThat(html).containsSubsequence(
                "href=\"#home-opening\"", "href=\"#home-skin\"", "href=\"#home-ritual\"",
                "href=\"#home-ingredients\"", "href=\"#home-featured\"", "href=\"#home-advisor\"",
                "id=\"home-opening\"", "id=\"home-skin\"", "id=\"home-ritual\"", "id=\"home-ingredients\"",
                "id=\"home-featured\"", "id=\"home-advisor\"", "</main>", "id=\"home-footer\"")
            .contains("aria-labelledby=\"home-title\"", "aria-labelledby=\"skin-title\"", "aria-labelledby=\"ritual-title\"",
                "aria-labelledby=\"ingredients-title\"", "aria-labelledby=\"featured-title\"", "aria-labelledby=\"advisor-title\"",
                "id=\"skin-title\"", "id=\"ritual-title\"", "id=\"ingredients-title\"", "id=\"featured-title\"", "id=\"advisor-title\"")
            .contains("href=\"/products?skinType=Da%20kh%C3%B4\"", "9 sản phẩm", "Thường cảm thấy căng",
                "href=\"/products?category=lam-sach\"", "href=\"/products?q=hyaluronic\"", "Xem 5 sản phẩm",
                "href=\"/products/demo-r001\"", "Xem tất cả sản phẩm", "href=\"/chat\"", "Hỏi BeautyShop",
                "không thay thế tư vấn của bác sĩ da liễu", "href=\"#home-products\"")
            .doesNotContain("th:utext", "style=", "onclick=", "home-slide-1", "href=\"#ritual\"")
            .containsOnlyOnce("<h1");
        assertThat(Pattern.compile("class=\"[^\"]*\\bhome-slide[\\s\"]").matcher(html).results().count()).isEqualTo(6);
        // A ritual step without a selling category keeps its text but has no link.
        String ritual = html.substring(html.indexOf("id=\"home-ritual\""), html.indexOf("id=\"home-ingredients\""));
        assertThat(ritual).contains("<span>Cân bằng</span>").doesNotContain("category=null");
        assertThat(Pattern.compile("<li>\\s*<span class=\"home-ritual-number\"").matcher(ritual).results().count()).isEqualTo(2);
        // The large wordmark is only rendered on chapter 1.
        assertThat(html.indexOf("home-slide-wordmark")).isEqualTo(html.lastIndexOf("home-slide-wordmark"));
    }

    @Test
    void chuongLoiHienBanTinhToiGianVaTrangVanRender() throws Exception {
        var loi = new DataAccessResourceFailureException("fixture");
        when(trangChuService.loaiDa()).thenThrow(loi);
        when(trangChuService.nghiThuc()).thenThrow(loi);
        when(trangChuService.hoatChat()).thenThrow(loi);
        when(cuaHangService.sanPhamNoiBat(anyInt())).thenThrow(loi);

        String html = trangChu();

        assertThat(html).contains("Vẻ đẹp.", "id=\"home-skin\"", "href=\"/products#skin-type\"",
                "id=\"home-ritual\"", "Làm sạch", "Cân bằng", "Tinh chất", "Dưỡng ẩm", "Chống nắng",
                "id=\"home-ingredients\"", "id=\"home-featured\"", "Khám phá tất cả sản phẩm", "id=\"home-advisor\"",
                "class=\"site-footer\"")
            .doesNotContain("?category=", "?skinType=", "?q=", "home-featured-grid", "home-skin-list", "home-ingredient-list");
        String ritual = html.substring(html.indexOf("id=\"home-ritual\""), html.indexOf("id=\"home-ingredients\""));
        assertThat(Pattern.compile("<li>\\s*<span class=\"home-ritual-number\"").matcher(ritual).results().count()).isEqualTo(5);
    }

    @Test
    void logoThuongHieuTrenHeaderHeroVaFooter() throws Exception {
        String html = trangChu();

        // Header: lockup image inside a labelled home link, monogram below 360px.
        assertThat(html).contains("aria-label=\"BeautyShop – Trang chủ\"", "src=\"/img/brand/beautyshop-lockup.svg\"",
                "alt=\"BeautyShop\" width=\"402\" height=\"54\"",
                "media=\"(max-width: 359px)\" srcset=\"/img/brand/beautyshop-mark.svg\"",
                "src=\"/img/brand/beautyshop-wordmark.svg\"", "href=\"/css/brand-hero.css\"",
                "type=\"image/svg+xml\" href=\"/img/favicon.svg\"")
            .doesNotContain("BEAUTY<span>SHOP</span>", "beautyshop-lockup-light.svg", "<script>", "style=", "th:utext")
            .containsOnlyOnce("<h1");
        // Chapter 1 hero is inline, decorative and not announced twice.
        String hero = html.substring(html.indexOf("<svg class=\"bs-hero"), html.indexOf("</svg>", html.indexOf("<svg class=\"bs-hero")));
        assertThat(hero).contains("aria-hidden=\"true\"", "focusable=\"false\"", "class=\"bs-letter\"", "bs-sheen")
            .doesNotContain("role=\"img\"", "<title", "aria-labelledby");
        assertThat(html.indexOf("<svg class=\"bs-hero")).isLessThan(html.indexOf("id=\"home-title\""));
    }

    @Test
    void cssHeroChiNapOTrangChu() throws Exception {
        String login = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(login).contains("src=\"/img/brand/beautyshop-lockup.svg\"").doesNotContain("brand-hero.css", "bs-hero");
    }

    @Test
    void danhSachRongCungDungBanTinh() throws Exception {
        when(trangChuService.loaiDa()).thenReturn(List.of());
        when(trangChuService.nghiThuc()).thenReturn(List.of());
        when(trangChuService.hoatChat()).thenReturn(List.of());
        when(cuaHangService.sanPhamNoiBat(anyInt())).thenReturn(List.of());

        String html = trangChu();

        assertThat(html).contains("href=\"/products#skin-type\"", "Chống nắng", "Khám phá tất cả sản phẩm")
            .doesNotContain("home-skin-list", "home-ingredient-list", "home-featured-grid");
    }

    private static TheSanPham the(String slug) {
        var the = mock(TheSanPham.class);
        when(the.getDuongDan()).thenReturn(slug);
        when(the.getTenSanPham()).thenReturn("Sản phẩm " + slug);
        when(the.getTenThuongHieu()).thenReturn("Thương hiệu");
        when(the.getGiaTu()).thenReturn(new BigDecimal("199000"));
        when(the.getSoLuongKhaDungLonNhat()).thenReturn(3);
        return the;
    }

    private static long dem(String html, String tu, String den) {
        String khu = html.substring(html.indexOf(tu), den == null ? html.length() : html.indexOf(den));
        return Pattern.compile("<a class=\"store-card\"").matcher(khu).results().count();
    }

    @Test
    void haiKhuSanPhamSauChuong6TruocFooterDungThuTuVaSoThe() throws Exception {
        var phoBien = IntStream.rangeClosed(1, 5).mapToObj(i -> the("pho-bien-" + i)).toList();
        var tatCa = IntStream.rangeClosed(1, 12).mapToObj(i -> the("tat-ca-" + i)).toList();
        when(goiYService.goiY(isNull(), eq(5))).thenReturn(phoBien);
        when(cuaHangService.timSanPham(eq(""), eq(""), eq(""), eq(""), eq("newest"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(tatCa));

        String html = trangChu();

        assertThat(html).containsSubsequence("id=\"home-advisor\"", "href=\"#home-products\"", "</main>",
                "id=\"home-products\"", "id=\"home-popular\"", "Được yêu thích nhất",
                "href=\"/products/pho-bien-1\"", "href=\"/products/pho-bien-5\"",
                "id=\"home-all-products\"", "Tất cả sản phẩm", "href=\"/?sort=newest#home-all-products\" aria-current=\"true\"",
                "href=\"/?sort=priceAsc#home-all-products\"", "href=\"/?sort=priceDesc#home-all-products\"",
                "href=\"/products/tat-ca-1\"", "href=\"/products/tat-ca-12\"", "href=\"/products\"", "Xem tất cả sản phẩm",
                "id=\"home-footer\"", "class=\"site-footer\"")
            .doesNotContain("style=", "th:utext", "onclick=");
        assertThat(dem(html, "id=\"home-popular\"", "id=\"home-all-products\"")).isEqualTo(5);
        assertThat(dem(html, "id=\"home-all-products\"", "id=\"home-footer\"")).isEqualTo(12);
        // Product sections are outside the carousel: never part of a Swiper slide.
        assertThat(html.indexOf("id=\"home-products\"")).isGreaterThan(html.indexOf("</main>"));
    }

    @Test
    void sapXepGiaVaThamSoLaRoiVeMoiNhat() throws Exception {
        var a = the("a");
        var datNhat = the("dat-nhat");
        var moiNhat = the("moi-nhat");
        when(goiYService.goiY(any(), anyInt())).thenReturn(List.of(a));
        when(cuaHangService.timSanPham(eq(""), eq(""), eq(""), eq(""), eq("priceDesc"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(datNhat)));
        String html = mvc.perform(get("/").param("sort", "priceDesc")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("href=\"/?sort=priceDesc#home-all-products\" aria-current=\"true\"", "href=\"/products/dat-nhat\"",
            "href=\"/products?sort=priceDesc\"");

        when(cuaHangService.timSanPham(eq(""), eq(""), eq(""), eq(""), eq("newest"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(moiNhat)));
        String la = mvc.perform(get("/").param("sort", "<script>")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(la).contains("href=\"/products/moi-nhat\"", "href=\"/?sort=newest#home-all-products\" aria-current=\"true\"")
            .doesNotContain("<script>\"");
    }

    @Test
    void danhSachRongHoacLoiThiAnKhuVaTrangVanRender() throws Exception {
        when(goiYService.goiY(any(), anyInt())).thenThrow(new DataAccessResourceFailureException("fixture"));
        when(cuaHangService.timSanPham(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        String html = trangChu();

        assertThat(html).contains("id=\"home-products\"", "id=\"home-footer\"", "class=\"site-footer\"", "id=\"home-advisor\"")
            .doesNotContain("id=\"home-popular\"", "id=\"home-all-products\"", "Được yêu thích nhất", "home-all-grid");
    }
}
