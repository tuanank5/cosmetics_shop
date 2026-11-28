package com.beautyshop.integration;

import com.beautyshop.dto.DoiTra_Form;
import com.beautyshop.service.SauMua_Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

abstract class DoiTraFixture {
    @Autowired JdbcClient jdbc;
    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired SauMua_Service sauMua;
    final String password = UUID.randomUUID().toString();
    int ton(long sku) {
        return jdbc.sql("SELECT so_luong_trong_kho FROM bien_the_san_pham WHERE ma_bien_the=?").param(sku).query(Integer.class).single();
    }

    MockHttpServletRequestBuilder lenh(String url, TaiKhoan admin, long version) {
        return post(url).session(admin.session()).with(csrf()).param("phienBan", Long.toString(version))
            .param("thaoTac", UUID.randomUUID().toString());
    }

    MockHttpServletRequestBuilder kiem(String url, TaiKhoan admin, long version, long dong, int duBan, int hong) {
        return lenh(url+"/inspect",admin,version).param("dong",Long.toString(dong))
            .param("duBan",Integer.toString(duBan)).param("daMo","0").param("hong",Integer.toString(hong))
            .param("khac","0").param("datChecklist","true");
    }
    @Test
    void doiTraNhieuMucChiNhapPhanDuBanVaGiuNguyenThanhToan() throws Exception {
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
        mvc.perform(get("/orders/" + maDon).session(chu.session())).andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"" + url + "\"")));
        mvc.perform(get("/orders/" + maDonHetHan).session(chu.session())).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("/returns/new"))));
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
        mvc.perform(lenh(adminUrl + "/status", admin, 0).param("status", "COMPLETED"))
            .andExpect(flash().attributeExists("error"));
        mvc.perform(lenh(adminUrl + "/status", admin, 0).param("status","APPROVED")).andExpect(flash().attributeExists("message"));
        var dong = sauMua.chiTiet(ma, chu.id()).muc();
        mvc.perform(lenh(adminUrl + "/receive", admin, 1).param("thamChieu", UUID.randomUUID().toString())
            .param("soLuong["+dong.getFirst().ma()+"]","2").param("soLuong["+dong.getLast().ma()+"]","1"))
            .andExpect(flash().attributeExists("message"));
        assertThat(ton(sku.id())).isEqualTo(10);
        var nhan = jdbc.sql("""
            SELECT n.ma_dong_nhan FROM dong_nhan_doi_tra n JOIN chi_tiet_doi_tra d ON n.ma_dong_tra=d.ma_chi_tiet_doi_tra
            WHERE d.ma_yeu_cau_doi_tra=? ORDER BY n.ma_dong_nhan
            """).param(ma).query(Long.class).list();
        long kien = jdbc.sql("SELECT ma_kien FROM dong_nhan_doi_tra WHERE ma_dong_nhan=?")
            .param(nhan.getFirst()).query(Long.class).single();
        mvc.perform(kiem(adminUrl, admin, 2, nhan.getFirst(), 1, 1)).andExpect(flash().attributeExists("message"));
        mvc.perform(lenh(adminUrl + "/confirm", admin, 3).param("kien",Long.toString(kien)))
            .andExpect(flash().attributeExists("error"));
        mvc.perform(kiem(adminUrl, admin, 3, nhan.getLast(), 0, 1)).andExpect(flash().attributeExists("message"));
        mvc.perform(lenh(adminUrl + "/confirm", admin, 4).param("kien",Long.toString(kien)))
            .andExpect(flash().attributeExists("message"));
        mvc.perform(lenh(adminUrl + "/close", admin, 5).param("nghiaVu","QA-CLOSED"))
            .andExpect(flash().attributeExists("message"));
        mvc.perform(get("/orders/" + maDon).session(chu.session())).andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"" + redirect + "\"")))
            .andExpect(content().string(containsString("Đã hoàn tất")));
        mvc.perform(tra(url, chu).param("soLuong[" + mucA + "]", "1")).andExpect(model().attributeHasErrors("doiTra"));
        long maTuChoi = sauMua.yeuCau(chu.id(), maDon, doiTra(mucB, 1));
        mvc.perform(lenh("/admin/returns/" + maTuChoi + "/status", admin, 0).param("status", "REJECTED"))
            .andExpect(flash().attributeExists("message"));
        assertThat(sauMua.yeuCau(chu.id(), maDon, doiTra(mucB, 1))).isPositive();

        assertThat(jdbc.sql("SELECT trang_thai FROM don_hang WHERE ma_don_hang = ?").param(maDon)
            .query(String.class).single()).isEqualTo("DELIVERED");
        assertThat(jdbc.sql("SELECT trang_thai FROM lan_thanh_toan WHERE ma_don_hang = ?").param(maDon)
            .query(String.class).single()).isEqualTo("PAID");
        assertThat(jdbc.sql("SELECT so_luong_trong_kho FROM bien_the_san_pham WHERE ma_bien_the = ?").param(sku.id())
            .query(Integer.class).single()).isEqualTo(11);
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

    DoiTra_Form doiTra(long muc, int soLuong) {
        var form = new DoiTra_Form();
        form.setLyDo("Lý do kiểm thử");
        form.setSoLuong(new java.util.LinkedHashMap<>(Map.of(muc, soLuong)));
        return form;
    }

    private MockHttpServletRequestBuilder tra(String url, TaiKhoan user) {
        return post(url).session(user.session()).with(csrf()).param("lyDo", "Lý do kiểm thử");
    }

    TaiKhoan taiKhoan(String vaiTro) throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        long id = jdbc.sql("""
            INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro)
            VALUES (?, ?, 'Test', ?) RETURNING ma_nguoi_dung
            """).params(email, encoder.encode(password), vaiTro).query(Long.class).single();
        var session = (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", email).param("password", password))
            .andExpect(authenticated().withRoles(vaiTro)).andReturn().getRequest().getSession(false);
        return new TaiKhoan(id, session);
    }

    Sku taoSku() {
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

    long taoDon(long user, String status, int cachNgay) {
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

    long themMuc(long don, long sku) {
        return jdbc.sql("""
            INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat,
                ten_bien_the_khi_dat, don_gia, so_luong) VALUES (?, ?, 'test', 'Sản phẩm test', 'Test', 100000, 2)
            RETURNING ma_chi_tiet_don_hang
            """).params(don, sku).query(Long.class).single();
    }

    record TaiKhoan(long id, MockHttpSession session) {}
    record Sku(long id, long sanPham, String slug) {}
}
