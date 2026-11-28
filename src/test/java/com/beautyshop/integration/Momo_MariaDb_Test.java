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
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean Momo_Settings settings;
    @Autowired com.beautyshop.service.ThanhToanMomo_Service thanhToan;
    @Autowired PhieuGiamGia_Service phieuGiamGiaService;
    @Autowired PlatformTransactionManager transactionManager;
    private final String password = UUID.randomUUID().toString();

    @Test
    void doiSoatHaiSlotDenGatewayVaRangBuocSql() throws Exception {
        var gateway = org.mockito.Mockito.spy(settings.gateway().orElseThrow());
        org.mockito.Mockito.doReturn(java.util.Optional.of(gateway)).when(settings).gateway();
        var don = datDon(520000,30000,10000);
        var request = attempts.moiNhat(don.id()).orElseThrow().request();
        org.mockito.Mockito.verify(gateway).tao(request);
        assertThat(request.amount()).isEqualTo(505000);
        assertThat(jdbc.sql("""
            SELECT CONCAT_WS(',', SUM(c.don_gia*c.so_luong),SUM(c.giam_hang),d.phi_chuan,
                d.giam_van_chuyen,d.tong_thanh_toan) FROM don_hang d JOIN chi_tiet_don_hang c USING(ma_don_hang)
            WHERE d.ma_don_hang=? GROUP BY d.ma_don_hang,d.phi_chuan,d.giam_van_chuyen,d.tong_thanh_toan
            """).param(don.id()).query(String.class).single()).isEqualTo("520000,30000,25000,10000,505000");
        var keys = jdbc.sql("""
            SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics
            WHERE table_schema=DATABASE() AND table_name='tam_giu_phieu_giam_gia' AND non_unique=0 GROUP BY index_name
            """).query(String.class).list();
        assertThat(keys).contains("ma_don_hang,slot","ma_don_hang,ma_phieu_giam_gia");
        assertThat(jdbc.sql("""
            SELECT COUNT(*) FROM information_schema.key_column_usage
            WHERE table_schema=DATABASE() AND table_name='tam_giu_phieu_giam_gia'
            AND referenced_table_name IN ('don_hang','phieu_giam_gia')
            """).query(Integer.class).single()).isEqualTo(2);
        jdbc.sql("UPDATE phieu_giam_gia SET gioi_han_su_dung=3,gioi_han_khach=1 WHERE ma_phieu_giam_gia=?")
            .param(don.voucher()).update();
        // Bypass the quote and exercise the SQL write guard in its own transaction.
        var gia = new PhieuGiamGia_Service.BaoGia(new PhieuGiamGia_Service.GiamGia(don.voucher(),java.math.BigDecimal.ONE),
            new PhieuGiamGia_Service.GiamGia(null,java.math.BigDecimal.ZERO),java.math.BigDecimal.ZERO,
            java.math.BigDecimal.ONE,java.util.Map.of());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new TransactionTemplate(transactionManager)
            .executeWithoutResult(tx -> phieuGiamGiaService.ghiLuot(don.id(),gia,true)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hết lượt");
        assertThat(luot(don,"so_luot_tam_giu")).isOne();
    }

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
        assertThat(attempts.lichSu(don.id(), true)).hasSize(1);
        assertThat(soLuong(don, "so_luong_trong_kho")).isEqualTo(2);
        assertThat(luot(don, "so_luot_da_dung")).isZero();
    }

    @Test
    void haiRetrySauFailedChiTaoMotLanMoi() throws Exception {
        var don = datDon();
        var request = attempts.moiNhat(don.id()).orElseThrow().request();
        var tuChoi = settings.moPhong(request, 1006);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> thanhToan.nhanIPN(tuChoi, "MOMO"))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(attempts.trangThaiHienThi(don.id())).contains("PENDING");
        mvc.perform(ipn(tuChoi)).andExpect(status().isNoContent());
        var batDau = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.Callable<MomoGateway.Request> retry = () -> {
            batDau.await(10, java.util.concurrent.TimeUnit.SECONDS);
            return thanhToan.chuanBi(don.id(), MomoGateway.MA_MOCK);
        };
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a = pool.submit(retry);
            var b = pool.submit(retry);
            assertThat(a.get(15, java.util.concurrent.TimeUnit.SECONDS))
                .isEqualTo(b.get(15, java.util.concurrent.TimeUnit.SECONDS));
        }
        var lichSu = attempts.lichSu(don.id(), true);
        assertThat(lichSu).extracting("trangThai").containsExactly("FAILED", "PENDING");
        assertThat(lichSu.getFirst().maYeuCau()).isEqualTo(request.requestId());
        assertThat(attempts.lichSu(don.id(), false)).allMatch(lan ->
            lan.maLan() == null && lan.maYeuCau() == null && lan.maGiaoDich() == null);
        assertThat(attempts.trangThaiHienThi(don.id())).contains("PENDING");
        assertThat(trangThai(don)).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void giuQuotaKhachVaChoDoiSoatKhoanDaThu() throws Exception {
        var don = datDon();
        long khach = jdbc.sql("SELECT ma_nguoi_dung FROM don_hang WHERE ma_don_hang=?").param(don.id()).query(Long.class).single();
        jdbc.sql("UPDATE phieu_giam_gia SET gioi_han_su_dung=3,gioi_han_khach=1 WHERE ma_phieu_giam_gia=?")
            .param(don.ship()).update();
        var code = jdbc.sql("SELECT ma_ap_dung FROM phieu_giam_gia WHERE ma_phieu_giam_gia=?").param(don.ship()).query(String.class).single();
        gioHang.themGioCustomer(khach,don.sku());
        var muc = gioHang.docGioCustomer(khach);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> phieuGiamGiaService.xemGio(null,code,khach,muc))
            .hasMessageContaining("hết lượt");
        var dau = attempts.moiNhat(don.id()).orElseThrow().request();
        assertThat(dau.amount()).isEqualTo(164000);
        thanhToan.nhanDoiSoat(dau,new MomoGateway.KetQua(1006,0),MomoGateway.MA_MOCK);
        var sau = thanhToan.chuanBi(don.id(),MomoGateway.MA_MOCK);
        var ketQuaCu = settings.moPhong(dau,0);
        thanhToan.nhanIPN(ketQuaCu,MomoGateway.MA_MOCK);
        thanhToan.nhanDoiSoat(sau,new MomoGateway.KetQua(1005,0),MomoGateway.MA_MOCK);
        assertThat(attempts.soLanCanHoan(don.id())).isOne();
        assertThat(luot(don,"so_luot_tam_giu")).isOne();
        assertThat(luot(don,"so_luot_da_dung")).isZero();
        assertThat(soLuong(don,"so_luong_tam_giu")).isOne();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> thanhToan.chuanBi(don.id(),MomoGateway.MA_MOCK))
            .hasMessageContaining("đối soát");
        assertThat(attempts.lichSu(don.id(),true)).hasSize(2);
        assertThat(trangThai(don)).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void rollbackSauKhiGiuSlotDau() throws Exception {
        var don = datDon();
        // A new HOLD inside this transaction must roll back if the second voucher violates its FK.
        var hang = jdbc.sql("""
            INSERT INTO phieu_giam_gia(ma_ap_dung,loai_giam_gia,gia_tri_giam,gioi_han_su_dung,bat_dau_luc,ket_thuc_luc)
            VALUES (UUID(),'FIXED',1,1,'2020-01-01','2099-01-01') RETURNING ma_phieu_giam_gia
            """).query(Long.class).single();
        var donKhac = jdbc.sql("""
            INSERT INTO don_hang(ma_don_hang_hien_thi,ma_nguoi_dung,ten_nguoi_nhan,sdt_nguoi_nhan,dia_chi_khi_dat,
                tam_tinh,so_tien_giam,phi_van_chuyen,tong_thanh_toan,trang_thai,phuong_thuc_thanh_toan)
            SELECT UUID(),ma_nguoi_dung,'Fixture','0','Fixture',tam_tinh,so_tien_giam,phi_van_chuyen,tong_thanh_toan,
                'PENDING_PAYMENT','MOMO' FROM don_hang WHERE ma_don_hang=? RETURNING ma_don_hang
            """).param(don.id()).query(Long.class).single();
        var gia = new PhieuGiamGia_Service.BaoGia(
            new PhieuGiamGia_Service.GiamGia(hang,java.math.BigDecimal.ONE),
            new PhieuGiamGia_Service.GiamGia(-1L,java.math.BigDecimal.ONE),
            java.math.BigDecimal.TEN,java.math.BigDecimal.TEN,java.util.Map.of());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new TransactionTemplate(transactionManager)
            .executeWithoutResult(status -> phieuGiamGiaService.ghiLuot(donKhac,gia,true)))
            .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(jdbc.sql("SELECT so_luot_tam_giu FROM phieu_giam_gia WHERE ma_phieu_giam_gia=?")
            .param(hang).query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM tam_giu_phieu_giam_gia WHERE ma_don_hang=?")
            .param(donKhac).query(Integer.class).single()).isZero();
    }

    @Test
    void callbackDongThoiVaAttemptCuSauKhiDaChot() throws Exception {
        var don = datDon();
        var dau = attempts.moiNhat(don.id()).orElseThrow().request();
        thanhToan.nhanDoiSoat(dau,new MomoGateway.KetQua(1006,0),MomoGateway.MA_MOCK);
        var sau = thanhToan.chuanBi(don.id(),MomoGateway.MA_MOCK);
        var ok = settings.moPhong(sau,0);
        var start = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.Callable<Void> callback = () -> {
            start.await(10,java.util.concurrent.TimeUnit.SECONDS);
            thanhToan.nhanIPN(ok,MomoGateway.MA_MOCK);
            return null;
        };
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(callback);
            var b=pool.submit(callback);
            a.get(15,java.util.concurrent.TimeUnit.SECONDS);
            b.get(15,java.util.concurrent.TimeUnit.SECONDS);
        }
        thanhToan.nhanDoiSoat(dau,new MomoGateway.KetQua(0,ok.transId()+100),MomoGateway.MA_MOCK);
        assertThat(attempts.soLanCanHoan(don.id())).isOne();
        assertThat(luot(don,"so_luot_da_dung")).isOne();
        assertThat(luot(don,"so_luot_tam_giu")).isZero();
        assertThat(soLuong(don,"so_luong_trong_kho")).isOne();
    }

    @Test
    void huyVaCallbackChayDuaKhongTraQuaLuot() throws Exception {
        var don=datDon();
        var req=attempts.moiNhat(don.id()).orElseThrow().request();
        var ok=settings.moPhong(req,0);
        var start=new java.util.concurrent.CyclicBarrier(2);
        try (var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=pool.submit(() -> {
                start.await();
                thanhToan.nhanIPN(ok,MomoGateway.MA_MOCK);
                return null;
            });
            var b=pool.submit(() -> {
                start.await();
                thanhToan.nhanDoiSoat(req,new MomoGateway.KetQua(1005,0),MomoGateway.MA_MOCK);
                return null;
            });
            a.get(15,java.util.concurrent.TimeUnit.SECONDS);
            b.get(15,java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(luot(don,"so_luot_tam_giu")).isZero();
        if ("EXPIRED".equals(trangThai(don))) {
            assertThat(attempts.soLanCanHoan(don.id())).isOne();
            assertThat(luot(don,"so_luot_da_dung")).isZero();
            assertThat(soLuong(don,"so_luong_trong_kho")).isEqualTo(2);
        } else {
            assertThat(trangThai(don)).isEqualTo("PENDING_CONFIRMATION");
            assertThat(luot(don,"so_luot_da_dung")).isOne();
            assertThat(soLuong(don,"so_luong_trong_kho")).isOne();
        }
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

    private Don datDon() throws Exception { return datDon(150000,10000,1000); }

    private Don datDon(int gia, int hang, int phi) throws Exception {
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
            VALUES (?, ?, 'Mock', ?, 2) RETURNING ma_bien_the
            """).params(sanPham, ma, gia).query(Long.class).single();
        long voucher = jdbc.sql("""
            INSERT INTO phieu_giam_gia (ma_ap_dung, loai_giam_gia, gia_tri_giam, gioi_han_su_dung, bat_dau_luc, ket_thuc_luc)
            VALUES (?, 'FIXED', ?, 1, UTC_TIMESTAMP() - INTERVAL 1 DAY, UTC_TIMESTAMP() + INTERVAL 1 DAY)
            RETURNING ma_phieu_giam_gia
            """).params(ma,hang).query(Long.class).single();
        String ship = "S-" + ma;
        long maShip = jdbc.sql("""
            INSERT INTO phieu_giam_gia(ma_ap_dung,slot,loai_giam_gia,gia_tri_giam,gioi_han_su_dung,bat_dau_luc,ket_thuc_luc)
            VALUES (?, 'VAN_CHUYEN', 'FIXED', ?, 1, UTC_TIMESTAMP()-INTERVAL 1 DAY,UTC_TIMESTAMP()+INTERVAL 1 DAY)
            RETURNING ma_phieu_giam_gia
            """).params(ship,phi).query(Long.class).single();
        gioHang.themGioCustomer(user.id(), sku);
        mvc.perform(get("/checkout").session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("MÔ PHỎNG - KHÔNG THU TIỀN")));
        String redirect = mvc.perform(post("/checkout").session(session).with(csrf())
                .param("payment", "MOMO").param("addressId", Long.toString(diaChi)).param("voucher", ma).param("ship",ship))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(redirect).startsWith("/payment/momo/mock/");
        return new Don(Long.parseLong(redirect.substring(redirect.lastIndexOf('/') + 1)), sku, voucher, maShip, session);
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
        int ship = jdbc.sql("SELECT " + cot + " FROM phieu_giam_gia WHERE ma_phieu_giam_gia=?").param(don.ship()).query(Integer.class).single();
        int hang = jdbc.sql("SELECT " + cot + " FROM phieu_giam_gia WHERE ma_phieu_giam_gia = ?")
            .param(don.voucher()).query(Integer.class).single();
        assertThat(ship).isEqualTo(hang);
        return hang;
    }

    private record User(long id, String email) {}
    private record Don(long id, long sku, long voucher, long ship, MockHttpSession session) {}
}
