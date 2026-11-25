package com.beautyshop.integration;

import com.beautyshop.dto.DoiTra_Form;
import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.repository.DanhGia_Store.BoLocDanhGia;
import com.beautyshop.repository.DanhGia_Store.TrangThaiLoc;
import com.beautyshop.service.SauMua_Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
@TestPropertySource(properties = "app.reviews.store=mariadb")
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class SauMua_MariaDb_Test {
    @Autowired JdbcClient jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired DanhGia_Store kho;
    @Autowired SauMua_Service sauMua;
    private final String password = UUID.randomUUID().toString();

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
        mvc.perform(post(duyet).session(admin.session()).with(csrf()).param("visible", "true").param("revision", phienBanCu))
            .andExpect(flash().attributeExists("error"));
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
    void doiTraNhieuMucCoHanVaTrangThaiThuCongKhongDoiTonHayThanhToan() throws Exception {
        var chu = taiKhoan("CUSTOMER");
        var khac = taiKhoan("CUSTOMER");
        var admin = taiKhoan("ADMIN");
        var sku = taoSku();
        long maDon = taoDon(chu.id(), "DELIVERED", 2);
        long mucA = themMuc(maDon, sku.id());
        long mucB = themMuc(maDon, taoSku().id());
        long maDonHetHan = taoDon(chu.id(), "DELIVERED", 8);
        long mucHetHan = themMuc(maDonHetHan, sku.id());
        String url = "/orders/" + maDon + "/returns/new";
        mvc.perform(get(url).session(chu.session())).andExpect(status().isOk())
            .andExpect(content().string(containsString("soLuong[" + mucA + "]")));
        mvc.perform(tra(url, khac).param("soLuong[" + mucA + "]", "1")).andExpect(status().isNotFound());
        mvc.perform(tra("/orders/" + maDonHetHan + "/returns/new", chu).param("soLuong[" + mucHetHan + "]", "1"))
            .andExpect(status().isConflict());
        mvc.perform(tra(url, chu).param("soLuong[" + mucHetHan + "]", "1")).andExpect(model().attributeHasErrors("doiTra"));
        String redirect = mvc.perform(tra(url, chu).param("soLuong[" + mucA + "]", "2").param("soLuong[" + mucB + "]", "1"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(redirect).startsWith("/account/returns/");
        long ma = Long.parseLong(redirect.substring(redirect.lastIndexOf('/') + 1));
        assertThat(sauMua.chiTiet(ma, chu.id()).muc()).hasSize(2);
        mvc.perform(get(redirect).session(chu.session())).andExpect(status().isOk());
        mvc.perform(get(redirect).session(khac.session())).andExpect(status().isNotFound());
        mvc.perform(tra(url, chu).param("soLuong[" + mucA + "]", "1")).andExpect(model().attributeHasErrors("doiTra"));
        String adminUrl = "/admin/returns/" + ma;
        mvc.perform(get(adminUrl).session(admin.session())).andExpect(status().isOk());
        mvc.perform(post(adminUrl + "/status").session(chu.session()).with(csrf()).param("status", "APPROVED"))
            .andExpect(status().isForbidden());
        mvc.perform(post(adminUrl + "/status").session(admin.session()).with(csrf()).param("status", "COMPLETED"))
            .andExpect(flash().attributeExists("error"));
        for (String status : new String[]{"APPROVED", "RECEIVED", "COMPLETED"}) {
            mvc.perform(post(adminUrl + "/status").session(admin.session()).with(csrf()).param("status", status))
                .andExpect(flash().attributeExists("message"));
            assertThat(sauMua.chiTiet(ma, chu.id()).chiTiet().yeuCau().trangThai().name()).isEqualTo(status);
        }
        mvc.perform(tra(url, chu).param("soLuong[" + mucA + "]", "1")).andExpect(model().attributeHasErrors("doiTra"));
        long maTuChoi = sauMua.yeuCau(chu.id(), maDon, doiTra(mucB, 1));
        mvc.perform(post("/admin/returns/" + maTuChoi + "/status").session(admin.session()).with(csrf()).param("status", "REJECTED"))
            .andExpect(flash().attributeExists("message"));
        assertThat(sauMua.yeuCau(chu.id(), maDon, doiTra(mucB, 1))).isPositive();

        assertThat(jdbc.sql("SELECT trang_thai FROM don_hang WHERE ma_don_hang = ?").param(maDon)
            .query(String.class).single()).isEqualTo("DELIVERED");
        assertThat(jdbc.sql("SELECT trang_thai FROM lan_thanh_toan WHERE ma_don_hang = ?").param(maDon)
            .query(String.class).single()).isEqualTo("PAID");
        assertThat(jdbc.sql("SELECT so_luong_trong_kho FROM bien_the_san_pham WHERE ma_bien_the = ?").param(sku.id())
            .query(Integer.class).single()).isEqualTo(10);
    }

    @Test
    void haiYeuCauDongThoiKhongVuotSoLuongDaMua() throws Exception {
        var chu = taiKhoan("CUSTOMER");
        long don = taoDon(chu.id(), "DELIVERED", 1);
        long muc = themMuc(don, taoSku().id());
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> thuYeuCau(chu.id(), don, muc));
            var b = executor.submit(() -> thuYeuCau(chu.id(), don, muc));
            assertThat(a.get(10, TimeUnit.SECONDS) ^ b.get(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }
        assertThat(sauMua.danhSachDoiTra(chu.id())).hasSize(1);
    }

    private boolean thuYeuCau(long nguoiDung, long don, long muc) {
        try {
            sauMua.yeuCau(nguoiDung, don, doiTra(muc, 2));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private DoiTra_Form doiTra(long muc, int soLuong) {
        var form = new DoiTra_Form();
        form.setLyDo("Lý do kiểm thử");
        form.setSoLuong(Map.of(muc, soLuong));
        return form;
    }

    private MockHttpServletRequestBuilder tra(String url, TaiKhoan user) {
        return post(url).session(user.session()).with(csrf()).param("lyDo", "Lý do kiểm thử");
    }

    private String phienBan(String ma, long maSanPham) {
        return kho.danhSachAdmin(new BoLocDanhGia(TrangThaiLoc.TAT_CA, null, maSanPham), PageRequest.of(0, 50)).stream()
            .filter(dg -> dg.ma().equals(ma)).findFirst().orElseThrow().phienBan();
    }

    private TaiKhoan taiKhoan(String vaiTro) throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        long id = jdbc.sql("""
            INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro)
            VALUES (?, ?, 'Test', ?) RETURNING ma_nguoi_dung
            """).params(email, encoder.encode(password), vaiTro).query(Long.class).single();
        var session = (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", email).param("password", password))
            .andExpect(authenticated().withRoles(vaiTro)).andReturn().getRequest().getSession(false);
        return new TaiKhoan(id, session);
    }

    private Sku taoSku() {
        String slug = UUID.randomUUID().toString();
        long dm = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Test', ?) RETURNING ma_danh_muc")
            .param(slug).query(Long.class).single();
        long th = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Test', ?) RETURNING ma_thuong_hieu")
            .param(slug).query(Long.class).single();
        long sp = jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
            VALUES (?, ?, 'Sản phẩm test', ?) RETURNING ma_san_pham
            """).params(dm, th, slug).query(Long.class).single();
        long sku = jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, 'Test', 100000, 10) RETURNING ma_bien_the
            """).params(sp, slug).query(Long.class).single();
        return new Sku(sku, sp, slug);
    }

    private long taoDon(long user, String status, int cachNgay) {
        long ma = jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan,
               dia_chi_khi_dat, tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_giao_thanh_cong)
            VALUES (?, ?, 'Test', '0900000000', 'Test', 100000, 100000, ?, 'COD',
               TIMESTAMPADD(DAY, ?, UTC_TIMESTAMP(6))) RETURNING ma_don_hang
            """).params(UUID.randomUUID().toString(), user, status, -cachNgay).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai)
            VALUES (?, ?, 'COD', 100000, 'PAID')
            """).params(ma, "COD-" + ma).update();
        return ma;
    }

    private long themMuc(long don, long sku) {
        return jdbc.sql("""
            INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat,
                ten_bien_the_khi_dat, don_gia, so_luong) VALUES (?, ?, 'test', 'Sản phẩm test', 'Test', 100000, 2)
            RETURNING ma_chi_tiet_don_hang
            """).params(don, sku).query(Long.class).single();
    }

    private record TaiKhoan(long id, MockHttpSession session) {}
    private record Sku(long id, long sanPham, String slug) {}
}
