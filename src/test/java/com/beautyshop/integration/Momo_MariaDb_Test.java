package com.beautyshop.integration;

import com.beautyshop.payment.MomoGateway;
import com.beautyshop.payment.MomoSignature;
import com.beautyshop.payment.MomoSignature.Ipn;
import com.beautyshop.payment.Momo_Settings;
import com.beautyshop.repository.ThanhToanMomo_Repository;
import com.beautyshop.service.GioHang_Service;
import com.beautyshop.service.PhieuGiamGia_Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.payment.momo.enabled=true", "app.payment.momo.mode=mock"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class Momo_MariaDb_Test {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder encoder;
    @Autowired GioHang_Service gioHang;
    @Autowired ThanhToanMomo_Repository attempts;
    @Autowired Momo_Settings settings;
    @Autowired PhieuGiamGia_Service phieuGiamGiaService;
    @Autowired PlatformTransactionManager transactionManager;
    private final String password = UUID.randomUUID().toString();

    @Test
    void ipnKySaiSaiSoTienVaReplayKhongTruTonHaiLan() throws Exception {
        var don = datDon();
        var request = attempts.moiNhat(don.id()).orElseThrow().request();
        mvc.perform(get("/payment/momo/mock/" + don.id()).session(don.session()))
            .andExpect(status().isOk()).andExpect(content().string(containsString("MÔ PHỎNG - KHÔNG THU TIỀN")));
        mvc.perform(post("/payment/momo/mock/" + don.id()).session(don.session())
                .param("requestId", request.requestId()).param("result", "0"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/payment/momo/return").param("resultCode", "0").param("orderId", request.orderId()))
            .andExpect(status().isOk());
        assertThat(trangThai(don)).isEqualTo("PENDING_PAYMENT");
        assertThat(soLuong(don, "so_luong_trong_kho")).isEqualTo(2);
        assertThat(soLuong(don, "so_luong_tam_giu")).isEqualTo(1);
        assertThat(luot(don, "so_luot_tam_giu")).isEqualTo(1);
        var admin = login(taiKhoan(true));
        mvc.perform(post("/admin/orders/" + don.id() + "/status").session(admin).with(csrf())
                .param("status", "DELIVERED"))
            .andExpect(flash().attributeExists("error"));
        assertThat(trangThai(don)).isEqualTo("PENDING_PAYMENT");
        String code = jdbc.sql("SELECT ma_ap_dung FROM phieu_giam_gia WHERE ma_phieu_giam_gia = ?")
            .param(don.voucher()).query(String.class).single();
        mvc.perform(suaVoucher(don, code, "0").session(admin))
            .andExpect(model().attributeHasFieldErrors("form", "gioiHanSuDung"));
        mvc.perform(suaVoucher(don, code, "1").session(admin)).andExpect(redirectedUrl("/admin/vouchers"));
        assertThat(jdbc.sql("SELECT dang_hoat_dong FROM phieu_giam_gia WHERE ma_phieu_giam_gia = ?")
            .param(don.voucher()).query(Boolean.class).single()).isFalse();
        assertThat(luot(don, "so_luot_tam_giu")).isEqualTo(1);

        Ipn dung = settings.moPhong(request, 0);
        var saiKy = mapper.valueToTree(dung);
        ((com.fasterxml.jackson.databind.node.ObjectNode) saiKy).put("signature", "0".repeat(64));
        mvc.perform(post("/payment/momo/ipn").contentType("application/json").content(mapper.writeValueAsBytes(saiKy)))
            .andExpect(status().isUnauthorized());
        Ipn saiTien = settings.moPhong(new MomoGateway.Request(request.maDonHang(), request.maHienThi(),
            request.requestId(), request.amount() + 1), 0);
        mvc.perform(ipn(saiTien)).andExpect(status().isBadRequest());
        Ipn saiDon = settings.moPhong(new MomoGateway.Request(request.maDonHang(), "wrong",
            request.requestId(), request.amount()), 0);
        mvc.perform(ipn(saiDon)).andExpect(status().isBadRequest());
        assertThat(trangThai(don)).isEqualTo("PENDING_PAYMENT");
        mvc.perform(ipn(dung)).andExpect(status().isNoContent());
        mvc.perform(ipn(dung)).andExpect(status().isNoContent());
        assertThat(trangThai(don)).isEqualTo("PENDING_CONFIRMATION");
        assertThat(attempts.moiNhat(don.id()).orElseThrow().trangThai()).isEqualTo("PAID");
        assertThat(soLuong(don, "so_luong_trong_kho")).isEqualTo(1);
        assertThat(soLuong(don, "so_luong_tam_giu")).isZero();
        assertThat(luot(don, "so_luot_da_dung")).isEqualTo(1);
        assertThat(luot(don, "so_luot_tam_giu")).isZero();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            phieuGiamGiaService.ketThucTamGiu(don.id(), don.voucher(), true));
        assertThat(luot(don, "so_luot_da_dung")).isEqualTo(1);
    }

    @Test
    void retryTransportGiuMaCuRetrySauTuChoiCoMaMoiVaDoiSoat() throws Exception {
        var don = datDon();
        var lanDau = attempts.moiNhat(don.id()).orElseThrow().request();
        String url = "/orders/" + don.id() + "/momo/retry";
        mvc.perform(post(url).session(don.session()).with(csrf()))
            .andExpect(redirectedUrl("/payment/momo/mock/" + don.id()));
        assertThat(attempts.moiNhat(don.id()).orElseThrow().request()).isEqualTo(lanDau);
        mvc.perform(ipn(settings.moPhong(lanDau, 1006))).andExpect(status().isNoContent());
        assertThat(attempts.moiNhat(don.id()).orElseThrow().trangThai()).isEqualTo("FAILED");
        assertThat(soLuong(don, "so_luong_tam_giu")).isEqualTo(1);

        mvc.perform(post(url).session(don.session()).with(csrf())).andExpect(redirectedUrl("/payment/momo/mock/" + don.id()));
        var lanHai = attempts.moiNhat(don.id()).orElseThrow().request();
        assertThat(lanHai.requestId()).isNotEqualTo(lanDau.requestId());
        assertThat(jdbc.sql("SELECT count(*) FROM lan_thanh_toan WHERE ma_don_hang = ?").param(don.id())
            .query(Integer.class).single()).isEqualTo(2);
        settings.moPhong(lanHai, 0); // Gateway biết kết quả dù callback chưa đến.
        var admin = login(taiKhoan(true));
        String dich = mvc.perform(post("/admin/orders/" + don.id() + "/momo/reconcile").session(admin).with(csrf())
                .param("listStatus", "PENDING_PAYMENT", "PENDING_CONFIRMATION").param("listMethod", "MOMO")
                .param("listPage", "0").param("listSize", "10"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        mvc.perform(get(java.net.URI.create(dich)).session(admin)).andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/admin/orders?method=MOMO&amp;page=0&amp;size=10"
                + "&amp;status=PENDING_PAYMENT&amp;status=PENDING_CONFIRMATION#admin-order-" + don.id() + "\"")));
        assertThat(trangThai(don)).isEqualTo("PENDING_CONFIRMATION");
        assertThat(soLuong(don, "so_luong_trong_kho")).isEqualTo(1);
    }

    @Test
    void chiGiaiPhongKhiCongXacNhanHetHanVaDanhDauThanhCongMuon() throws Exception {
        var don = datDon();
        var request = attempts.moiNhat(don.id()).orElseThrow().request();
        var admin = login(taiKhoan(true));
        jdbc.sql("UPDATE tam_giu_ton_kho SET het_han_luc = UTC_TIMESTAMP() - INTERVAL 1 MINUTE WHERE ma_don_hang = ?")
            .param(don.id()).update();
        assertThat(attempts.donQuaHan(MomoGateway.MA_MOCK, don.id() - 1)).contains(don.id());
        mvc.perform(post("/admin/orders/" + don.id() + "/momo/reconcile").session(admin).with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertThat(trangThai(don)).isEqualTo("PENDING_PAYMENT");
        assertThat(soLuong(don, "so_luong_tam_giu")).isEqualTo(1);
        Ipn hetHan = settings.moPhong(request, 1005);
        mvc.perform(ipn(hetHan)).andExpect(status().isNoContent());
        mvc.perform(ipn(hetHan)).andExpect(status().isNoContent());
        assertThat(trangThai(don)).isEqualTo("EXPIRED");
        assertThat(soLuong(don, "so_luong_tam_giu")).isZero();
        assertThat(luot(don, "so_luot_tam_giu")).isZero();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            phieuGiamGiaService.ketThucTamGiu(don.id(), don.voucher(), false));
        assertThat(luot(don, "so_luot_tam_giu")).isZero();
        mvc.perform(post("/admin/vouchers/" + don.voucher() + "/delete").session(admin).with(csrf()))
            .andExpect(redirectedUrl("/admin/vouchers")).andExpect(flash().attributeExists("error"));
        assertThat(luot(don, "so_luot_da_dung")).isZero();

        // Mô phỏng thông báo thành công ký hợp lệ nhưng trái với kết quả cổng đã báo trước đó.
        String accessKey = (String) ReflectionTestUtils.getField(settings, "accessKey");
        String secretKey = (String) ReflectionTestUtils.getField(settings, "secretKey");
        var muon = new Ipn(hetHan.partnerCode(), request.orderId(), request.requestId(), request.amount(),
            request.orderInfo(), hetHan.orderType(), hetHan.transId(), 0, "Late", hetHan.payType(),
            System.currentTimeMillis(), "", null);
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(muon);
        body.put("signature", MomoSignature.sign(secretKey, muon.raw(accessKey)));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/payment/momo/ipn").contentType("application/json").content(mapper.writeValueAsBytes(body)))
                .andExpect(status().isNoContent());
        }
        assertThat(trangThai(don)).isEqualTo("EXPIRED");
        assertThat(attempts.soLanCanHoan(don.id())).isEqualTo(1);
        assertThat(soLuong(don, "so_luong_trong_kho")).isEqualTo(2);
        assertThat(luot(don, "so_luot_da_dung")).isZero();
    }

    private MockHttpServletRequestBuilder ipn(Ipn ipn) throws Exception {
        return post("/payment/momo/ipn").contentType("application/json").content(mapper.writeValueAsBytes(ipn));
    }

    private MockHttpServletRequestBuilder suaVoucher(Don don, String code, String gioiHan) {
        return post("/admin/vouchers/" + don.voucher() + "/edit").with(csrf())
            .param("maApDung", code).param("loaiGiamGia", "FIXED").param("giaTriGiam", "10000")
            .param("donToiThieu", "0").param("gioiHanSuDung", gioiHan)
            .param("batDauLuc", "2020-01-01T00:00").param("ketThucLuc", "2099-01-01T00:00")
            .param("_dangHoatDong", "on");
    }

    private Don datDon() throws Exception {
        var user = taiKhoan(false);
        var session = login(user);
        long diaChi = jdbc.sql("""
            INSERT INTO dia_chi_nguoi_dung (ma_nguoi_dung, ten_nguoi_nhan, so_dien_thoai, dia_chi_chi_tiet)
            VALUES (?, 'Test', '0901234567', 'Địa chỉ thử') RETURNING ma_dia_chi
            """).param(user.id()).query(Long.class).single();
        String ma = UUID.randomUUID().toString();
        long danhMuc = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Mock', ?) RETURNING ma_danh_muc")
            .param(ma).query(Long.class).single();
        long thuongHieu = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Mock', ?) RETURNING ma_thuong_hieu")
            .param(ma).query(Long.class).single();
        long sanPham = jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
            VALUES (?, ?, 'Mock', ?) RETURNING ma_san_pham
            """).params(danhMuc, thuongHieu, ma).query(Long.class).single();
        long sku = jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, 'Mock', 150000, 2) RETURNING ma_bien_the
            """).params(sanPham, ma).query(Long.class).single();
        long voucher = jdbc.sql("""
            INSERT INTO phieu_giam_gia (ma_ap_dung, loai_giam_gia, gia_tri_giam, gioi_han_su_dung, bat_dau_luc, ket_thuc_luc)
            VALUES (?, 'FIXED', 10000, 1, UTC_TIMESTAMP() - INTERVAL 1 DAY, UTC_TIMESTAMP() + INTERVAL 1 DAY)
            RETURNING ma_phieu_giam_gia
            """).param(ma).query(Long.class).single();
        gioHang.themGioCustomer(user.id(), sku);
        mvc.perform(get("/checkout").session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("MÔ PHỎNG - KHÔNG THU TIỀN")));
        String redirect = mvc.perform(post("/checkout").session(session).with(csrf())
                .param("payment", "MOMO").param("addressId", Long.toString(diaChi)).param("voucher", ma))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(redirect).startsWith("/payment/momo/mock/");
        return new Don(Long.parseLong(redirect.substring(redirect.lastIndexOf('/') + 1)), sku, voucher, session);
    }

    private User taiKhoan(boolean admin) {
        String email = UUID.randomUUID() + "@example.invalid";
        long id = jdbc.sql("""
            INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro)
            VALUES (?, ?, 'Test', ?) RETURNING ma_nguoi_dung
            """).params(email, encoder.encode(password), admin ? "ADMIN" : "CUSTOMER").query(Long.class).single();
        return new User(id, email);
    }

    private MockHttpSession login(User user) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", user.email()).param("password", password))
            .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }

    private String trangThai(Don don) {
        return jdbc.sql("SELECT trang_thai FROM don_hang WHERE ma_don_hang = ?").param(don.id()).query(String.class).single();
    }

    private int soLuong(Don don, String cot) {
        return jdbc.sql("SELECT " + cot + " FROM bien_the_san_pham WHERE ma_bien_the = ?")
            .param(don.sku()).query(Integer.class).single();
    }

    private int luot(Don don, String cot) {
        return jdbc.sql("SELECT " + cot + " FROM phieu_giam_gia WHERE ma_phieu_giam_gia = ?")
            .param(don.voucher()).query(Integer.class).single();
    }

    private record User(long id, String email) {}
    private record Don(long id, long sku, long voucher, MockHttpSession session) {}
}
