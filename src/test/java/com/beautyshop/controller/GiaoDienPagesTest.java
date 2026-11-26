package com.beautyshop.controller;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.security.Security_Config;
import com.beautyshop.service.NguoiDung_Service;
import com.beautyshop.service.CuaHang_Service;
import com.beautyshop.service.SauMua_Service;
import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.model.BienTheSanPham;
import com.beautyshop.model.SanPham;
import com.beautyshop.model.ThuongHieu;
import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.repository.DanhGia_Store.DanhGiaCongKhai;
import com.beautyshop.repository.DanhGia_Store.TomTatDanhGia;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import jakarta.servlet.RequestDispatcher;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.DefaultErrorViewResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({Bootstrap_Controller.class, TaiKhoan_Controller.class, CuaHang_Controller.class})
@Import({Security_Config.class, GiaoDienPagesTest.ErrorViews.class})
class GiaoDienPagesTest {
    // The MVC slice omits Boot's default status-view resolver; use the production resolver here.
    @TestConfiguration
    static class ErrorViews {
        @Bean DefaultErrorViewResolver defaultErrorViewResolver(ApplicationContext context) {
            return new DefaultErrorViewResolver(context, new WebProperties.Resources());
        }
    }
    @Autowired MockMvc mvc;
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDungRepository;
    @MockitoBean NguoiDung_Service nguoiDungService;
    @MockitoBean com.beautyshop.service.EmailVerification_Service emailVerificationService;
    @MockitoBean CuaHang_Service cuaHangService;
    @MockitoBean SauMua_Service sauMuaService;

    @BeforeEach
    void danhGiaMacDinh() {
        // P23: mặc định trang chi tiết chưa có đánh giá được duyệt (kho đánh giá phản hồi bình thường).
        when(sauMuaService.danhGiaSanPham(anyLong(), anyInt())).thenReturn(new SauMua_Service.TrangDanhGiaSanPham(
            TomTatDanhGia.RONG, Page.empty(PageRequest.of(0, 10)), false));
    }

    @Test
    void footerGuestChiCoDichCongKhai() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andDo(result -> assertThat(footer(result.getResponse().getContentAsString()))
                .contains("href=\"/login\"", "href=\"/register\"", "href=\"/cart\"", "href=\"/products?filterPanel=open#brands\"")
                .doesNotContain("href=\"/account\"", "href=\"/orders\"", "href=\"/account/wishlist\"", "/admin/"));
    }

    @Test
    void footerCustomerCoDanhSachCaNhan() throws Exception {
        mvc.perform(get("/").with(user("fixture").roles("CUSTOMER")))
            .andExpect(status().isOk())
            .andDo(result -> assertThat(footer(result.getResponse().getContentAsString()))
                .contains("href=\"/account\"", "href=\"/orders\"", "href=\"/account/wishlist\"", "href=\"/account/returns\"")
                .doesNotContain("href=\"/login\"", "href=\"/register\"", "/admin/"));
    }

    @Test
    void footerAdminCoQuanTriVaKhongCoDichCustomer() throws Exception {
        mvc.perform(get("/").with(user("fixture").roles("ADMIN")))
            .andExpect(status().isOk())
            .andDo(result -> assertThat(footer(result.getResponse().getContentAsString()))
                .contains("href=\"/admin/catalog/categories\"", "href=\"/admin/catalog/brands\"", "href=\"/admin/catalog/products\"")
                .doesNotContain("href=\"/account", "href=\"/orders", "href=\"/cart\"", "href=\"/login\""));
    }

    private String footer(String html) {
        return html.substring(html.indexOf("<footer"), html.indexOf("</footer>") + "</footer>".length());
    }

    @Test
    void wishlistTrongHeaderChiDanhChoCustomer() throws Exception {
        mvc.perform(get("/").with(user("fixture").roles("CUSTOMER")))
            .andExpect(status().isOk())
            .andDo(result -> assertThat(header(result.getResponse().getContentAsString()))
                .containsPattern("(?s)<a[^>]*href=\"/account/wishlist\"[^>]*aria-label=\"Sản phẩm yêu thích\"[^>]*>\\s*<svg"));
        mvc.perform(get("/"))
            .andDo(result -> assertThat(header(result.getResponse().getContentAsString()))
                .doesNotContain("href=\"/account/wishlist\""));
        mvc.perform(get("/").with(user("fixture").roles("ADMIN")))
            .andDo(result -> assertThat(header(result.getResponse().getContentAsString()))
                .doesNotContain("href=\"/account/wishlist\"", "href=\"/cart\"", "href=\"/account\""));
    }

    private String header(String html) {
        return html.substring(html.indexOf("<header"), html.indexOf("</header>") + "</header>".length());
    }

    @ParameterizedTest
    @CsvSource({"0,0", "1,0", "2,3", "8,4"})
    void badgeDemDonViVaCoTenTruyCap(int dau, int sau) throws Exception {
        var session = new MockHttpSession();
        var gio = GioKhach.doc(session);
        if (dau > 0) gio = gio.them(11L, dau);
        if (sau > 0) gio = gio.them(22L, sau);
        gio.luu(session);
        mvc.perform(get("/login").session(session))
            .andExpect(status().isOk())
            .andDo(result -> assertThat(header(result.getResponse().getContentAsString()))
                .contains("aria-label=\"Giỏ hàng · " + (dau + sau) + " sản phẩm\"")
                .contains("aria-label=\"" + (dau + sau) + " sản phẩm trong giỏ\">" + (dau + sau) + "</span>")
                .contains("class=\"header-icon\""));
    }

    @ParameterizedTest
    @CsvSource({"/login,/login", "/register,NONE", "/products,/products", "/products/kiem-thu,NONE"})
    void currentChiDanhDauDungTrang(String path, String current) throws Exception {
        sanPhamHaiBienThe();
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 12)));
        var response = mvc.perform(get(path)).andExpect(status().isOk());
        if ("NONE".equals(current)) {
            response.andDo(result -> assertThat(header(result.getResponse().getContentAsString()))
                .doesNotContain("aria-current=\"page\""));
        } else {
            response.andDo(result -> assertThat(header(result.getResponse().getContentAsString()))
                .contains("href=\"" + current + "\" aria-current=\"page\""));
        }
    }

    @Test
    void menuKhamPhaDungDichBoLocVaGiuFormTimKiem() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("class=\"nav-discovery\"")))
            .andExpect(content().string(containsString("href=\"/products?filterPanel=open#categories\"")))
            .andExpect(content().string(containsString("href=\"/products?filterPanel=open#brands\"")))
            .andExpect(content().string(containsString("href=\"/products?filterPanel=open#skin-type\"")))
            .andExpect(content().string(containsString("<summary>Loại da</summary>")))
            .andExpect(content().string(containsString("method=\"get\" action=\"/products\"")))
            .andExpect(content().string(containsString("name=\"q\"")));
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER,true", "ADMIN,false"})
    void menuKhamPhaGiuDungVaiTro(String role, boolean hienMenu) throws Exception {
        mvc.perform(get("/").with(user("fixture").roles(role)))
            .andExpect(status().isOk())
            .andExpect(content().string(hienMenu
                ? containsString("class=\"nav-discovery\"")
                : not(containsString("class=\"nav-discovery\""))));
    }

    @Test
    void catalogFragmentsFormatMoneyEscapeNamesAndKeepPaginationFilters() throws Exception {
        var card = mock(TheSanPham.class);
        when(card.getDuongDan()).thenReturn("serum");
        when(card.getTenSanPham()).thenReturn("Serum <script>");
        when(card.getTenThuongHieu()).thenReturn("BeautyShop");
        when(card.getGiaTu()).thenReturn(new BigDecimal("1234567"));
        when(cuaHangService.thuongHieu()).thenReturn(List.of(new CuaHang_Service.TuyChon("demo", "Thương hiệu kiểm thử")));
        when(cuaHangService.danhMuc()).thenReturn(List.of(new CuaHang_Service.TuyChon("duong-da", "Dưỡng da")));
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(1, 12), 27));
        mvc.perform(get("/products").param("q", "serum").param("brand", "demo").param("category", "duong-da")
                .param("skinType", "dau").param("sort", "priceAsc").param("page", "1").param("size", "12"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/products\" aria-current=\"page\"")))
            .andExpect(content().string(containsString("27 sản phẩm")))
            .andExpect(content().string(containsString("1.234.567 ₫")))
            .andExpect(content().string(containsString("Serum &lt;script&gt;")))
            .andExpect(content().string(containsString("Hết hàng")))
            .andExpect(content().string(containsString("Thương hiệu: Thương hiệu kiểm thử")))
            .andExpect(content().string(containsString("Danh mục: Dưỡng da")))
            .andExpect(content().string(containsString("/products?q=serum&amp;category=duong-da&amp;skinType=dau&amp;sort=priceAsc&amp;size=12")))
            .andExpect(content().string(containsString("/products?q=serum&amp;brand=demo&amp;category=duong-da&amp;skinType=dau&amp;sort=priceAsc&amp;size=12&amp;page=0")))
            .andExpect(content().string(containsString("/products?q=serum&amp;brand=demo&amp;category=duong-da&amp;skinType=dau&amp;sort=priceAsc&amp;size=12&amp;page=2")))
            .andExpect(content().string(containsString("Trang sau")));
    }

    @Test
    void catalogExplainsEmptyStatesAndEscapesTheSearchValueInBothFieldAndChip() throws Exception {
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 12)));
        mvc.perform(get("/products"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Chưa có sản phẩm để hiển thị")))
            .andExpect(content().string(containsString("0 sản phẩm")))
            .andExpect(content().string(not(containsString("class=\"store-card\""))));
        mvc.perform(get("/products").param("q", "<script>alert(1)</script>"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Không tìm thấy sản phẩm phù hợp")))
            .andExpect(content().string(containsString("value=\"&lt;script&gt;alert(1)&lt;/script&gt;\"")))
            .andExpect(content().string(containsString("Từ khóa: &lt;script&gt;alert(1)&lt;/script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(containsString("Xem tất cả sản phẩm")));
    }

    @Test
    void homeHienSanPhamNoiBatTruocFooterVaDungLaiTheSanPham() throws Exception {
        var coAnh = mock(TheSanPham.class);
        when(coAnh.getDuongDan()).thenReturn("serum-noi-bat");
        when(coAnh.getTenSanPham()).thenReturn("Serum <b>nổi bật</b>");
        when(coAnh.getTenThuongHieu()).thenReturn("Thương hiệu kiểm thử");
        when(coAnh.getGiaTu()).thenReturn(new BigDecimal("450000"));
        when(coAnh.getUrlHinhAnh()).thenReturn("/uploads/sku/fixture.png");
        when(coAnh.getSoLuongKhaDungLonNhat()).thenReturn(3);
        var chuaAnh = mock(TheSanPham.class);
        when(chuaAnh.getDuongDan()).thenReturn("kem-chua-anh");
        when(chuaAnh.getTenSanPham()).thenReturn("Kem chưa có ảnh");
        when(chuaAnh.getGiaTu()).thenReturn(new BigDecimal("125000"));
        when(chuaAnh.getSoLuongKhaDungLonNhat()).thenReturn(1);
        when(cuaHangService.sanPhamNoiBat(4)).thenReturn(List.of(coAnh, chuaAnh));

        String html = mvc.perform(get("/")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // P18: the featured strip is chapter 5 of the carousel, inside <main>, before the footer.
        assertThat(html).containsSubsequence("data-home-carousel", "id=\"home-featured\"",
                "Sản phẩm nổi bật", "</main>", "id=\"home-footer\"", "class=\"site-footer\"")
            .contains("href=\"/products/serum-noi-bat?from=home-featured", "href=\"/products/kem-chua-anh?from=home-featured",
                "Serum &lt;b&gt;nổi bật&lt;/b&gt;", "450.000 ₫", "Còn hàng",
                "src=\"/uploads/sku/fixture.png\"", "data-catalog-placeholder", "loading=\"lazy\"")
            .doesNotContain("loading=\"eager\"", "src=\"\"", "th:utext", "style=")
            .containsOnlyOnce("<h1");
        assertThat(html.substring(html.indexOf("id=\"home-featured\"")))
            .contains("class=\"btn btn--secondary home-featured-all\" href=\"/products\"");
        assertThat(html.indexOf("id=\"home-featured\"")).isLessThan(html.indexOf("</main>"));
    }

    @Test
    void homeChuong5VanHienLoiMoiKhiRongHoacCatalogLoi() throws Exception {
        when(cuaHangService.sanPhamNoiBat(anyInt())).thenReturn(List.of());
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"home-featured\"")))
            .andExpect(content().string(containsString("Khám phá tất cả sản phẩm")))
            .andExpect(content().string(not(containsString("home-featured-grid"))))
            .andExpect(content().string(containsString("class=\"site-footer\"")));
        when(cuaHangService.sanPhamNoiBat(anyInt()))
            .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("fixture"));
        mvc.perform(get("/")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Khám phá tất cả sản phẩm")))
            .andExpect(content().string(not(containsString("home-featured-grid"))))
            .andExpect(content().string(containsString("Vẻ đẹp.")));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullAndEmptySource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"   ", "/uploads/sku/fixture.png", "/img/products/r001.webp"})
    void catalogAnhCoFallbackSsrVaKhongGuiUrlRong(String url) throws Exception {
        var card = mock(TheSanPham.class);
        when(card.getDuongDan()).thenReturn("kiem-thu");
        when(card.getTenSanPham()).thenReturn("Sản phẩm kiểm thử");
        when(card.getGiaTu()).thenReturn(new BigDecimal("125000"));
        when(card.getUrlHinhAnh()).thenReturn(url);
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(new PageImpl<>(List.of(card, card), PageRequest.of(0, 12), 2));
        String html = mvc.perform(get("/products")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("data-catalog-placeholder", "Chưa có ảnh sản phẩm")
            .doesNotContain("onerror=", "src=\"\"");
        if (url == null || url.isBlank()) {
            assertThat(html).doesNotContain("data-catalog-image");
        } else {
            assertThat(html).contains("data-catalog-image", "width=\"400\" height=\"500\"",
                "loading=\"eager\"", "loading=\"lazy\"", "alt=\"\"");
        }
    }

    @Test
    void catalogOptionalPreviewDoesNotInventWishlistPostOrChangeTheProductLink() throws Exception {
        var card = mock(TheSanPham.class);
        when(card.getMaSanPham()).thenReturn(21L);
        when(card.getDuongDan()).thenReturn("preview-fixture");
        when(card.getTenSanPham()).thenReturn("Kiểm thử media");
        when(card.getGiaTu()).thenReturn(new BigDecimal("125000"));
        when(card.getUrlHinhAnh()).thenReturn("/uploads/sku/original.png");
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0,12),1));
        String plain = mvc.perform(get("/products")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(plain).contains("/uploads/sku/original.png").doesNotContain("data-product-preview", "ScrollTrigger", "gsap-");
        String media = mvc.perform(get("/products").flashAttr("catalogPreviewMedia", java.util.Map.of(
                "preview-fixture", java.util.Map.of("thumbnailUrl", "/img/test-poster.png",
                    "previewVideoUrl", "/img/test-video.webm", "videoPosterUrl", "/img/test-poster.png"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(media).contains("href=\"/products/preview-fixture?from=catalog", "action=\"/account/wishlist/21/source\"",
                "src=\"/img/test-poster.png\"", "data-video-src=\"/img/test-video.webm\"", "preload=\"none\"")
            .doesNotContain("<video src=", "data-wishlist-id");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void catalogDemBaBoLocKhongDemTuKhoaSapXepHayPhanTrang(int mask) throws Exception {
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 7)));
        String html = mvc.perform(get("/products").param("q", "serum").param("sort", "priceAsc")
                .param("brand", (mask & 1) == 0 ? "   " : "demo")
                .param("category", (mask & 2) == 0 ? "" : "duong-da")
                .param("skinType", (mask & 4) == 0 ? "   " : "da khô")
                .param("page", "2").param("size", "7"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("id=\"catalog-filter-count\">" + Integer.bitCount(mask) + "</span>",
            "Từ khóa: serum", "Giá tăng dần", "Không tìm thấy sản phẩm phù hợp", "href=\"/products\">Xóa tất cả");
        if (mask == 7) {
            assertThat(html).contains(
                "/products?q=serum&amp;brand=demo&amp;skinType=da%20kh%C3%B4&amp;sort=priceAsc&amp;size=7",
                "/products?q=serum&amp;category=duong-da&amp;skinType=da%20kh%C3%B4&amp;sort=priceAsc&amp;size=7",
                "/products?q=serum&amp;brand=demo&amp;category=duong-da&amp;sort=priceAsc&amp;size=7");
        }
    }

    @ParameterizedTest
    @CsvSource({"'', '', false", "open, '', true", "closed, fixture, false", "unknown, fixture, true"})
    void catalogChiChapNhanHaiTrangThaiHienThiBoLoc(String panel, String brand, boolean open) throws Exception {
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(Page.empty(PageRequest.of(0, 7)));
        String html = mvc.perform(get("/products").param("filterPanel", panel).param("brand", brand).param("size", "7"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("name=\"filterPanel\" value=\"open\"", "name=\"size\" value=\"7\"");
        String details = html.substring(html.indexOf("<details id=\"catalog-filters\""));
        assertThat(details.substring(0, details.indexOf('>')).contains(" open=")).isEqualTo(open);
    }

    @ParameterizedTest
    @CsvSource({"0,0,0", "1,0,1", "12,0,12", "27,0,12", "27,1,12", "27,2,3", "27,99,3"})
    void catalogTienDoLayDungPageVaTuDocLaiTrangCuoi(int total, int requested, int expectedElements) throws Exception {
        var card = mock(TheSanPham.class);
        when(card.getDuongDan()).thenReturn("kiem-thu");
        when(card.getTenSanPham()).thenReturn("Sản phẩm kiểm thử");
        when(card.getGiaTu()).thenReturn(new BigDecimal("125000"));
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenAnswer(call -> {
                org.springframework.data.domain.Pageable p = call.getArgument(5);
                int count = (int) Math.max(0, Math.min(p.getPageSize(), total - p.getOffset()));
                return new PageImpl<>(java.util.Collections.nCopies(count, card), p, total);
            });
        String html = mvc.perform(get("/products").param("page", String.valueOf(requested)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int page = Math.min(requested, Math.max(0, (total - 1) / 12));
        if (total == 0) assertThat(html).contains("0 sản phẩm").doesNotContain("1–0");
        else assertThat(html).contains("Đang hiển thị " + (page * 12 + 1) + "–" + (page * 12 + expectedElements)
            + " trong tổng " + total + " sản phẩm");
        if (requested == 99) verify(cuaHangService).timSanPham("", "", "", "", "newest", PageRequest.of(2, 12));
        if (total <= 12) {
            assertThat(html).doesNotContain("Phân trang sản phẩm");
        } else {
            assertThat(html).contains("Phân trang sản phẩm", "class=\"pagination-current\" aria-current=\"page\"");
            if (page == 0) assertThat(html).doesNotContain("Trang đầu", "Trang trước").contains("Trang sau", "Trang cuối");
            if (page == (total - 1) / 12) assertThat(html).contains("Trang đầu", "Trang trước").doesNotContain("Trang sau", "Trang cuối");
        }
    }

    @Test
    void catalogKhongBiaKhoangKhiPageRongNhungTongConSanPham() throws Exception {
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 12), 27));
        mvc.perform(get("/products").param("page", "2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Không có sản phẩm ở trang này trong tổng 27 sản phẩm")))
            .andExpect(content().string(not(containsString("Đang hiển thị"))));
    }

    private void sanPhamHaiBienThe() {
        var sanPham = mock(SanPham.class);
        var thuongHieu = mock(ThuongHieu.class);
        when(sanPham.getMa()).thenReturn(3L);
        when(sanPham.getDuongDan()).thenReturn("kiem-thu");
        when(sanPham.getTenSanPham()).thenReturn("Tinh chất kiểm thử");
        when(sanPham.getThuongHieu()).thenReturn(thuongHieu);
        when(thuongHieu.getTenThuongHieu()).thenReturn("Thương hiệu kiểm thử");
        var conHang = spy(new BienTheSanPham(sanPham));
        doReturn(11L).when(conHang).getMaBienThe();
        conHang.capNhat("QA-11", "30 ml", new BigDecimal("125000"), 4, null, null, true);
        var hetHang = spy(new BienTheSanPham(sanPham));
        doReturn(22L).when(hetHang).getMaBienThe();
        hetHang.capNhat("QA-22", "50 ml", new BigDecimal("185000"), 0, "QA-LOT", null, true);
        hetHang.doiAnh("/uploads/sku/fixture.png");
        var cacBienThe = List.of(conHang, hetHang);
        when(cuaHangService.chiTiet("kiem-thu", null)).thenReturn(new CuaHang_Service.ChiTiet(sanPham, cacBienThe, conHang));
        when(cuaHangService.chiTiet("kiem-thu", 22L)).thenReturn(new CuaHang_Service.ChiTiet(sanPham, cacBienThe, hetHang));
    }

    @Test
    void productReturnKeepsOnlyTypedSourceAndRespectsContextPath() throws Exception {
        sanPhamHaiBienThe();
        mvc.perform(get("/shop/products/kiem-thu").contextPath("/shop").param("from", "home-featured"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/shop/#home-featured\"")))
            .andExpect(content().string(not(containsString("#home-featured-product-3"))));
        mvc.perform(get("/shop/products/kiem-thu").contextPath("/shop").param("from", "home-popular")
                .param("token", "fixture").param("state", "fixture"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/shop/#home-popular-product-3\"")))
            .andExpect(content().string(not(containsString("name=\"token\""))))
            .andExpect(content().string(not(containsString("name=\"state\""))));
        mvc.perform(get("/products/kiem-thu").param("from", "catalog").param("q", "kem + serum")
                .param("sort", "priceAsc").param("page", "2").param("size", "7"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/products?q=kem%20%2B%20serum&amp;sort=priceAsc&amp;page=2&amp;size=7#catalog-product-3\"")));
        mvc.perform(get("/products/kiem-thu").param("from", "//other.invalid"))
            .andExpect(content().string(containsString("class=\"pdp-back text-link\" href=\"/products\"")));
        mvc.perform(get("/products/kiem-thu").param("from", "wishlist"))
            .andExpect(content().string(containsString("class=\"pdp-back text-link\" href=\"/products\"")));
        mvc.perform(get("/products/kiem-thu").param("from", "wishlist").with(user("fixture").roles("CUSTOMER")))
            .andExpect(content().string(containsString("class=\"pdp-back text-link\" href=\"/account/wishlist\"")));
    }

    @Test
    void guestCanCompareSkusAndOnlySubmitTheCurrentSku() throws Exception {
        sanPhamHaiBienThe();
        mvc.perform(get("/products/kiem-thu"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"variant\" value=\"11\" aria-current=\"true\"")))
            .andExpect(content().string(containsString("name=\"variant\" value=\"22\"")))
            .andExpect(content().string(containsString("125.000 ₫")))
            .andExpect(content().string(containsString("185.000 ₫")))
            .andExpect(content().string(containsString("Còn 4 sản phẩm")))
            .andExpect(content().string(containsString("type=\"hidden\" name=\"variant\" value=\"11\"")))
            .andExpect(content().string(containsString("action=\"/cart/items\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("Chưa có ảnh cho biến thể này")))
            .andExpect(content().string(containsString("Chưa có đánh giá được duyệt.")))
            .andExpect(content().string(not(containsString("<summary>Thành phần"))))
            .andExpect(content().string(not(containsString("name=\"quantity\""))));
    }

    @Test
    void soldOutSelectionHasItsOwnImageAndPublicReviewsButNoPurchaseForm() throws Exception {
        sanPhamHaiBienThe();
        when(sauMuaService.danhGiaSanPham(3L, 0)).thenReturn(new SauMua_Service.TrangDanhGiaSanPham(
            new TomTatDanhGia(1, 4, new long[] {0, 0, 0, 1, 0}),
            new PageImpl<>(List.of(new DanhGiaCongKhai("Thịnh P.", 4, "Ghi chú <b>", Instant.parse("2026-11-20T18:30:00Z"))),
                PageRequest.of(0, 10), 1), false));
        mvc.perform(get("/products/kiem-thu").param("variant", "22"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"variant\" value=\"22\" aria-current=\"true\"")))
            .andExpect(content().string(containsString("50 ml · Mã biến thể sản phẩm: QA-22")))
            .andExpect(content().string(containsString("alt=\"Tinh chất kiểm thử · 50 ml\"")))
            .andExpect(content().string(containsString("/uploads/sku/fixture.png")))
            .andExpect(content().string(containsString("QA-LOT")))
            .andExpect(content().string(containsString("Biến thể này đã hết hàng")))
            .andExpect(content().string(not(containsString("action=\"/cart/items\""))))
            .andExpect(content().string(containsString("Ghi chú &lt;b&gt;")))
            .andExpect(content().string(containsString("Thịnh P.")))
            .andExpect(content().string(containsString("Đã mua hàng")))
            // 18:30 UTC ngày 20 là 01:30 ngày 21 giờ Việt Nam.
            .andExpect(content().string(containsString("21/11/2026")))
            .andExpect(content().string(containsString("aria-label=\"4,0 trên 5 sao\"")))
            .andExpect(content().string(containsString("1 đánh giá")))
            .andExpect(content().string(not(containsString("Tạm thời chưa tải được đánh giá"))));
    }

    @Test
    void khoDanhGiaLoiThiTrangChiTietVanHienVaBaoTamThoi() throws Exception {
        sanPhamHaiBienThe();
        when(sauMuaService.danhGiaSanPham(anyLong(), anyInt())).thenReturn(new SauMua_Service.TrangDanhGiaSanPham(
            TomTatDanhGia.RONG, Page.empty(PageRequest.of(0, 10)), true));
        mvc.perform(get("/products/kiem-thu"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tạm thời chưa tải được đánh giá")))
            .andExpect(content().string(containsString("action=\"/cart/items\"")))
            .andExpect(content().string(not(containsString("Chưa có đánh giá được duyệt."))));
    }

    @Test
    void danhGiaPhanTrangGiuBienTheVaNeoVeKhuDanhGia() throws Exception {
        sanPhamHaiBienThe();
        var mot = new DanhGiaCongKhai("An N.", 5, "", Instant.parse("2026-11-01T00:00:00Z"));
        when(sauMuaService.danhGiaSanPham(3L, 1)).thenReturn(new SauMua_Service.TrangDanhGiaSanPham(
            new TomTatDanhGia(25, 100, new long[] {1, 2, 3, 4, 15}), new PageImpl<>(List.of(mot), PageRequest.of(1, 10), 25), false));
        mvc.perform(get("/products/kiem-thu").param("variant", "22").param("reviewPage", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Trang 2 / 3")))
            .andExpect(content().string(containsString("/products/kiem-thu?variant=22&amp;reviewPage=0#danh-gia")))
            .andExpect(content().string(containsString("/products/kiem-thu?variant=22&amp;reviewPage=2#danh-gia")))
            .andExpect(content().string(containsString("4,0 trên 5 sao")))
            .andExpect(content().string(containsString("value=\"60\"")));
    }

    @Test
    void theSanPhamHienSaoTrungBinhTuMotTruyVanTomTat() throws Exception {
        var card = mock(TheSanPham.class);
        when(card.getMaSanPham()).thenReturn(7L);
        when(card.getDuongDan()).thenReturn("serum-sao");
        when(card.getTenSanPham()).thenReturn("Serum sao");
        when(card.getTenThuongHieu()).thenReturn("BeautyShop");
        when(card.getGiaTu()).thenReturn(new BigDecimal("150000"));
        var khongDanhGia = mock(TheSanPham.class);
        when(khongDanhGia.getMaSanPham()).thenReturn(8L);
        when(khongDanhGia.getDuongDan()).thenReturn("serum-moi");
        when(khongDanhGia.getTenSanPham()).thenReturn("Serum mới");
        when(khongDanhGia.getGiaTu()).thenReturn(new BigDecimal("90000"));
        when(cuaHangService.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(new PageImpl<>(List.of(card, khongDanhGia), PageRequest.of(0, 12), 2));
        when(sauMuaService.tomTatDanhGia(List.of(7L, 8L))).thenReturn(new SauMua_Service.TomTatTheSanPham(
            Map.of(7L, new TomTatDanhGia(2, 9, new long[] {0, 0, 0, 1, 1})), false));
        mvc.perform(get("/products"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("aria-label=\"4,5 trên 5 sao\"")))
            .andExpect(content().string(containsString("4,5 · 2 đánh giá")));
        verify(sauMuaService, times(1)).tomTatDanhGia(any());
    }

    @ParameterizedTest
    @CsvSource({"CUSTOMER,true", "ADMIN,false"})
    void productActionsFollowTheAuthenticatedRole(String role, boolean duocMua) throws Exception {
        sanPhamHaiBienThe();
        var themGio = containsString("action=\"/cart/items\"");
        var yeuThich = containsString("action=\"/account/wishlist/3\"");
        mvc.perform(get("/products/kiem-thu").with(user("fixture").roles(role)))
            .andExpect(status().isOk())
            .andExpect(content().string(duocMua ? themGio : not(themGio)))
            .andExpect(content().string(duocMua ? yeuThich : not(yeuThich)));
    }

    @Test
    void registerValidationKeepsSafeFieldsAndLinksErrorsWithoutEchoingPassword() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("hoTen", "<script>alert(1)</script>")
                .param("email", "invalid-email").param("matKhau", "short"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;script&gt;")))
            .andExpect(content().string(containsString("value=\"invalid-email\"")))
            .andExpect(content().string(containsString("href=\"#email\"")))
            .andExpect(content().string(containsString("id=\"email-error\"")))
            .andExpect(content().string(containsString("aria-invalid=\"true\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(not(containsString("value=\"short\""))));
    }

    @Test
    void loginKeepsPasswordFormWhenNoOAuthProviderIsConfigured() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"password\"")))
            .andExpect(content().string(not(containsString("/oauth2/authorization/"))));
    }

    @Test
    void guestBadgeCountsUnitsAndNavigationMarksCurrentPage() throws Exception {
        var session = new MockHttpSession();
        GioKhach.doc(session).them(1L, 2).them(2L, 3).luu(session);
        mvc.perform(get("/login").session(session))
            .andExpect(model().attribute("uiCartCount", 5L))
            .andExpect(content().string(containsString("5 sản phẩm trong giỏ")))
            .andExpect(content().string(containsString("aria-current=\"page\"")))
            .andExpect(content().string(containsString("href=\"#main\"")))
            .andExpect(content().string(containsString("id=\"main\"")));
    }

    @ParameterizedTest
    @CsvSource({"400,Thông tin bạn gửi chưa hợp lệ", "403,Bạn chưa có quyền truy cập", "404,Không tìm thấy trang",
        "409,Thông tin vừa thay đổi", "413,Tệp tải lên quá lớn", "429,Bạn thao tác quá nhanh", "500,Chưa thể hoàn tất yêu cầu", "503,Chưa thể mở nội dung"})
    void bootErrorsUseVietnameseViewsAndDoNotExposeDetails(int code, String heading) throws Exception {
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, code)
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, "sensitive-internal-detail")
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException("sensitive-internal-detail")))
            .andExpect(status().is(code))
            .andExpect(content().string(containsString(heading)))
            .andExpect(content().string(containsString("Xem sản phẩm")))
            .andExpect(content().string(not(containsString("sensitive-internal-detail"))))
            .andExpect(content().string(not(containsString("Apache Tomcat"))));
    }

    @Test
    void errorPageOffersAdminDashboardOnlyToAdmin() throws Exception {
        mvc.perform(get("/error").with(user("fixture").roles("ADMIN")).accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Về tổng quan quản trị")));
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
            .andExpect(status().isNotFound())
            .andExpect(content().string(not(containsString("Về tổng quan quản trị"))));
    }
}
