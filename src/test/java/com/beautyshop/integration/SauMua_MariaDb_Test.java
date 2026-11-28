package com.beautyshop.integration;

import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.repository.DanhGia_Store.BoLocDanhGia;
import com.beautyshop.repository.DanhGia_Store.TrangThaiLoc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Luồng sau mua trên MariaDB thật. Từ P23 kịch bản đánh giá đi qua {@link DanhGia_Store}: lớp này chạy store dự phòng
 * {@code mariadb}; {@link SauMua_MongoDanhGia_Test} chạy lại đúng kịch bản với store {@code mongo}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
abstract class SauMua_MariaDb_Test extends DoiTraFixture {
    @Autowired DanhGia_Store kho;

    @Test
    void danhGiaDaGiaoChiChuMucVaChiCongKhaiSauDuyet() throws Exception {
        var chu = taiKhoan("CUSTOMER");
        var khac = taiKhoan("CUSTOMER");
        var admin = taiKhoan("ADMIN");
        var sku = taoSku();
        long don = taoDon(chu.id(), "DELIVERED", 2);
        long muc = themMuc(don, sku.id());
        long chuaGiao = themMuc(taoDon(chu.id(), "SHIPPING", 0), sku.id());
        String url = "/orders/items/" + muc + "/review";
        mvc.perform(get(url).session(khac.session())).andExpect(status().isNotFound());
        mvc.perform(post(url).session(khac.session()).with(csrf()).param("soSao", "5")).andExpect(status().isNotFound());
        mvc.perform(post("/orders/items/" + chuaGiao + "/review").session(chu.session()).with(csrf()).param("soSao", "5"))
            .andExpect(status().isConflict());
        mvc.perform(get(url).session(chu.session())).andExpect(status().isOk());
        mvc.perform(post(url).session(chu.session()).with(csrf()).param("soSao", "6"))
            .andExpect(model().attributeHasFieldErrors("form", "soSao"));
        mvc.perform(post(url).session(chu.session()).with(csrf()).param("soSao", "5").param("noiDung", "Đánh giá ban đầu"))
            .andExpect(redirectedUrl("/orders/" + don));
        mvc.perform(post(url).session(chu.session()).with(csrf()).param("soSao", "4"))
            .andExpect(status().isConflict());
        mvc.perform(get("/products/" + sku.slug()))
            .andExpect(content().string(not(containsString("Đánh giá ban đầu"))));
        String ma = kho.cuaChuMuc(muc, chu.id()).orElseThrow().ma();
        String duyet = "/admin/reviews/" + ma + "/visibility";
        String phienBanCu = phienBan(ma, sku.sanPham());
        mvc.perform(get("/admin/reviews").session(admin.session()).param("status", "CHO_DUYET")
                .param("product", String.valueOf(sku.sanPham())))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(duyet)))
            .andExpect(content().string(containsString("value=\"" + phienBanCu + "\"")));
        mvc.perform(post(duyet).session(khac.session()).with(csrf()).param("visible", "true").param("revision", phienBanCu))
            .andExpect(status().isForbidden());
        mvc.perform(post(duyet).session(admin.session()).with(csrf()).param("visible", "true").param("revision", phienBanCu))
            .andExpect(redirectedUrl("/admin/reviews")).andExpect(flash().attributeExists("message"));
        mvc.perform(get("/products/" + sku.slug())).andExpect(status().isOk())
            .andExpect(content().string(containsString("Đã mua hàng")))
            .andExpect(content().string(containsString("5,0 trên 5 sao")))
            .andExpect(content().string(containsString("Đánh giá ban đầu")));
        assertThat(kho.tomTat(sku.sanPham()).soLuong()).isEqualTo(1);
        mvc.perform(post(url + "/edit").session(chu.session()).with(csrf()).param("soSao", "3")
                .param("noiDung", "Đánh giá đã sửa"))
            .andExpect(redirectedUrl("/orders/" + don));
        var loiDuyet = mvc.perform(post(duyet).session(admin.session()).with(csrf()).param("visible", "true")
                .param("revision", phienBanCu).param("status", "CHO_DUYET").param("product", String.valueOf(sku.sanPham())))
            .andExpect(flash().attributeExists("error")).andReturn();
        mvc.perform(get(java.net.URI.create(loiDuyet.getResponse().getRedirectedUrl())).session(admin.session())
                .flashAttrs(loiDuyet.getFlashMap())).andExpect(status().isOk())
            .andExpect(model().attribute("focusReviewId", ma))
            .andExpect(content().string(containsString("Đánh giá đã thay đổi")))
            .andExpect(content().string(containsString("Đánh giá đã sửa")))
            .andExpect(content().string(containsString("name=\"revision\" value=\"" + phienBan(ma, sku.sanPham()) + "\"")));
        mvc.perform(get("/products/" + sku.slug()))
            .andExpect(content().string(not(containsString("Đánh giá đã sửa"))));
        assertThat(kho.tomTat(sku.sanPham()).soLuong()).isZero();
        mvc.perform(post(duyet).session(admin.session()).with(csrf()).param("visible", "true")
                .param("revision", phienBan(ma, sku.sanPham())))
            .andExpect(flash().attributeExists("message"));
        assertThat(kho.tomTat(sku.sanPham()).soSao(3)).isEqualTo(1);
        mvc.perform(post(duyet).session(admin.session()).with(csrf()).param("visible", "false"))
            .andExpect(flash().attributeExists("message"));
        assertThat(kho.congKhai(sku.sanPham(), PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    void yeuThichYeuCauDangNhapIdempotentVaChiThayGioCuaMinh() throws Exception {
        var chu = taiKhoan("CUSTOMER");
        var khac = taiKhoan("CUSTOMER");
        var sku = taoSku();
        String url = "/account/wishlist/" + sku.sanPham();
        mvc.perform(get("/account/wishlist")).andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(post(url).with(csrf())).andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(post(url).session(chu.session())).andExpect(status().isForbidden());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post(url).session(chu.session()).with(csrf())).andExpect(redirectedUrl("/account/wishlist"));
        }
        assertThat(sauMua.danhSachYeuThich(chu.id())).hasSize(1);
        mvc.perform(get("/account/wishlist").session(chu.session())).andExpect(status().isOk())
            .andExpect(content().string(containsString(sku.slug())));
        mvc.perform(get("/account/wishlist").session(khac.session()))
            .andExpect(content().string(not(containsString(sku.slug()))));
        mvc.perform(post(url + "/delete").session(khac.session()).with(csrf()));
        assertThat(sauMua.danhSachYeuThich(chu.id())).hasSize(1);
        mvc.perform(post(url + "/delete").session(chu.session()).with(csrf())).andExpect(redirectedUrl("/account/wishlist"));
        assertThat(sauMua.danhSachYeuThich(chu.id())).isEmpty();
    }

    @Test
    void timTaiChoTheoChuVaChongThanhCongGiaKhiSkuDaTat() throws Exception {
        var chu = taiKhoan("CUSTOMER");
        var khac = taiKhoan("CUSTOMER");
        var admin = taiKhoan("ADMIN");
        var sku = taoSku();
        String url = "/account/wishlist/" + sku.sanPham();
        String state = "/account/wishlist/state";
        var guest = new MockHttpSession();
        mvc.perform(get(url + "/source").session(guest).param("origin", "pdp")
                .param("variant", Long.toString(sku.id())).param("next", "https://example.invalid"))
            .andExpect(redirectedUrl("http://localhost/login"));
        String emailKhac = jdbc.sql("SELECT email FROM nguoi_dung WHERE ma_nguoi_dung = ?")
            .param(khac.id()).query(String.class).single();
        String veNguon = mvc.perform(post("/login").session(guest).with(csrf())
                .param("username", emailKhac).param("password", password))
            .andExpect(authenticated()).andReturn().getResponse().getRedirectedUrl();
        assertThat(veNguon).startsWith("http://localhost" + url + "/source?");
        mvc.perform(get(java.net.URI.create(veNguon)).session(guest))
            .andExpect(redirectedUrl("/products/" + sku.slug() + "?variant=" + sku.id() + "#pdp-actions"));
        assertThat(sauMua.danhSachYeuThich(khac.id())).isEmpty();
        mvc.perform(post(url).session(admin.session()).with(csrf()).header("X-Wishlist-Partial", "1"))
            .andExpect(status().isForbidden());
        mvc.perform(post(url).session(chu.session()).header("X-Wishlist-Partial", "1")).andExpect(status().isForbidden());
        mvc.perform(post(url).session(chu.session()).with(csrf()).header("X-Wishlist-Partial", "1")
                .param("maNguoiDung", Long.toString(khac.id())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        mvc.perform(get(state).session(khac.session()).param("ids", Long.toString(sku.sanPham())))
            .andExpect(content().json("[]"));
        mvc.perform(get(state).session(chu.session()).param("ids", sku.sanPham() + "," + Long.MAX_VALUE))
            .andExpect(content().json("[\"" + sku.sanPham() + "\"]"));
        mvc.perform(get("/products/" + sku.slug()).session(chu.session()))
            .andExpect(content().string(containsString("aria-pressed=\"true\"")));
        mvc.perform(post(url).session(chu.session()).with(csrf()).param("origin", "pdp")
                .param("variant", Long.toString(sku.id())).param("reviewPage", "1").param("next", "https://example.invalid/"))
            .andExpect(redirectedUrl("/products/" + sku.slug() + "?variant=" + sku.id() + "&reviewPage=1#pdp-actions"));
        assertThat(sauMua.danhSachYeuThich(chu.id())).hasSize(1);

        jdbc.sql("UPDATE san_pham SET dang_hoat_dong = FALSE WHERE ma_san_pham = ?").param(sku.sanPham()).update();
        mvc.perform(post(url).session(khac.session()).with(csrf()).header("X-Wishlist-Partial", "1"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.saved").value(false));
        jdbc.sql("UPDATE san_pham SET dang_hoat_dong = TRUE WHERE ma_san_pham = ?").param(sku.sanPham()).update();
        jdbc.sql("UPDATE bien_the_san_pham SET dang_hoat_dong = FALSE WHERE ma_bien_the = ?").param(sku.id()).update();
        mvc.perform(post(url).session(khac.session()).with(csrf()).header("X-Wishlist-Partial", "1"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.saved").value(false));
        mvc.perform(post(url).session(chu.session()).with(csrf()).header("X-Wishlist-Partial", "1"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.saved").value(true));
        mvc.perform(get("/account/wishlist").session(chu.session()))
            .andExpect(content().string(containsString("Tạm ngừng bán")));
        String email = jdbc.sql("SELECT email FROM nguoi_dung WHERE ma_nguoi_dung = ?").param(chu.id()).query(String.class).single();
        var tabKhac = (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", email).param("password", password))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
        jdbc.sql("UPDATE nguoi_dung SET phien_ban_xac_thuc = phien_ban_xac_thuc + 1 WHERE ma_nguoi_dung = ?")
            .param(chu.id()).update();
        mvc.perform(post(url + "/delete").session(chu.session()).with(csrf()).header("X-Wishlist-Partial", "1"))
            .andExpect(redirectedUrl("/login?inactive"));
        mvc.perform(get(state).session(tabKhac)).andExpect(redirectedUrl("/login?inactive"));
        assertThat(sauMua.danhSachYeuThich(chu.id())).hasSize(1);
        var moi = (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", email).param("password", password))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
        mvc.perform(post(url + "/delete").session(moi).with(csrf()).header("X-Wishlist-Partial", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false));
        mvc.perform(get("/account/wishlist").session(moi))
            .andExpect(content().string(containsString("Chưa có sản phẩm yêu thích.")));
    }

    private String phienBan(String ma, long maSanPham) {
        return kho.danhSachAdmin(new BoLocDanhGia(TrangThaiLoc.TAT_CA, null, maSanPham), PageRequest.of(0, 50)).stream()
            .filter(dg -> dg.ma().equals(ma)).findFirst().orElseThrow().phienBan();
    }

}
