package com.beautyshop.integration;

import com.beautyshop.repository.GioHang_Repository;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.service.DonHang_Service;
import com.beautyshop.service.GioHang_Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class DonHangCOD_MariaDb_Test {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.beautyshop.service.EmailVerification_Service otp;
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired NguoiDung_Repository nguoiDungRepository;
    @Autowired DonHang_Service donHangService;
    @Autowired GioHang_Service gioHangService;
    @Autowired GioHang_Repository gioHangRepository;
    @Autowired PlatformTransactionManager transactionManager;

    private final String matKhau = UUID.randomUUID().toString();

    @Test
    void checkoutHuyVaGiaoCodEndToEnd() throws Exception {
        var chu = taiKhoan(false);
        var khac = taiKhoan(false);
        var admin = taiKhoan(true);
        var variant = taoSku(3, 550000);
        String voucher = taoVoucher("FIXED", 70000, null, 1, false);
        var guest = new MockHttpSession();
        mvc.perform(get("/checkout").session(guest)).andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(post("/cart/items").session(guest).with(csrf()).param("variant", Long.toString(variant)))
            .andExpect(redirectedUrl("/cart"));
        mvc.perform(post("/cart/items/" + variant + "/quantity").session(guest).with(csrf()).param("quantity", "2"))
            .andExpect(redirectedUrl("/cart"));
        assertThat(ton(variant)).isEqualTo(3);
        var session = login(chu.email(), guest);
        var phienKhac = login(khac.email(), new MockHttpSession());
        var phienAdmin = login(admin.email(), new MockHttpSession());
        mvc.perform(post("/cart/items/" + variant + "/quantity").session(session).with(csrf()).param("quantity", "1"))
            .andExpect(redirectedUrl("/cart"));
        mvc.perform(get("/checkout").session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("550.000")))
            .andExpect(content().string(containsString("href=\"/account/addresses?from=checkout\"")));
        mvc.perform(get("/account/addresses").param("from", "checkout").session(session))
            .andExpect(content().string(containsString("href=\"/checkout\"")))
            .andExpect(content().string(containsString("href=\"/account/addresses/" + chu.maDiaChi() + "/edit?from=checkout\"")));
        mvc.perform(post("/account/addresses/" + chu.maDiaChi() + "/edit").session(session).with(csrf())
                .param("from", "checkout").param("tenNguoiNhan", "Người nhận")
                .param("soDienThoai", "0901234567").param("diaChiChiTiet", "Địa chỉ snapshot").param("laMacDinh", "true"))
            .andExpect(redirectedUrl("/checkout"));
        mvc.perform(get("/checkout").session(session))
            .andExpect(model().attribute("baoGiaHopLe", true))
            .andExpect(content().string(containsString("550.000")));
        assertThat(donHangService.danhSachCustomer(chu.id())).isEmpty();
        mvc.perform(post("/checkout/quote").session(session).with(csrf())
                .param("addressId", Long.toString(chu.maDiaChi())).param("voucher", voucher))
            .andExpect(status().isOk()).andExpect(content().string(containsString("505.000")));
        assertThat(luot(voucher)).isZero();
        assertThat(ton(variant)).isEqualTo(3);
        mvc.perform(post("/checkout").session(session).with(csrf()).param("addressId", Long.toString(khac.maDiaChi())))
            .andExpect(model().attributeHasErrors("form"));
        mvc.perform(post("/checkout").session(session).param("addressId", Long.toString(chu.maDiaChi())))
            .andExpect(status().isForbidden());

        long maDon = datDon(session, chu.maDiaChi(), voucher);
        assertThat(ton(variant)).isEqualTo(2);
        assertThat(luot(voucher)).isEqualTo(1);
        assertThat(gioHangRepository.docGio(chu.id())).isEmpty();
        mvc.perform(get("/orders/" + maDon).session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("505.000")))
            .andExpect(content().string(containsString("Địa chỉ snapshot")));
        mvc.perform(get("/orders/" + maDon).session(phienKhac)).andExpect(status().isNotFound());
        mvc.perform(post("/orders/" + maDon + "/cancel").session(phienKhac).with(csrf()))
            .andExpect(status().isNotFound());
        mvc.perform(get("/orders").session(phienKhac)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Chưa có đơn hàng.")));
        jdbc.sql("UPDATE bien_the_san_pham SET gia_ban = 600000 WHERE ma_bien_the = ?").param(variant).update();
        jdbc.sql("UPDATE dia_chi_nguoi_dung SET dia_chi_chi_tiet = 'Địa chỉ mới' WHERE ma_dia_chi = ?")
            .param(chu.maDiaChi()).update();
        mvc.perform(get("/orders/" + maDon).session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("550.000")))
            .andExpect(content().string(not(containsString("Địa chỉ mới"))));
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/orders/" + maDon + "/cancel").session(session).with(csrf()))
                .andExpect(flash().attributeExists("message"));
        }
        assertThat(ton(variant)).isEqualTo(3);
        assertThat(luot(voucher)).isZero();
        assertThat(trangThai(maDon)).isEqualTo("CANCELLED");
        mvc.perform(get("/orders/" + maDon).session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Đơn đã hủy.")))
            .andExpect(content().string(not(containsString("Xác nhận hủy đơn"))))
            .andExpect(content().string(not(containsString("order-timeline"))));

        gioHangService.themGioCustomer(chu.id(), variant);
        var tongQuanTruoc = tongQuanAdmin();
        long maDonGiao = datDon(session, chu.maDiaChi(), "");
        var tongQuanChuaThu = tongQuanAdmin();
        assertThat(tongQuanChuaThu.tongSoDon()).isEqualTo(tongQuanTruoc.tongSoDon() + 1);
        assertThat(tongQuanChuaThu.soDonDaThu()).isEqualTo(tongQuanTruoc.soDonDaThu());
        assertThat(tongQuanChuaThu.doanhThuDaThu()).isEqualByComparingTo(tongQuanTruoc.doanhThuDaThu());
        mvc.perform(get("/admin/orders").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/orders").session(phienAdmin)).andExpect(status().isOk())
            .andExpect(model().attribute("tongQuan", tongQuanChuaThu));
        mvc.perform(get("/admin/orders/" + maDonGiao).session(phienAdmin)).andExpect(status().isOk());
        mvc.perform(get("/admin/orders/" + maDonGiao).session(phienAdmin).param("listStatus", "PACKING", "SHIPPING"))
            .andExpect(content().string(containsString("href=\"/admin/orders?status=PACKING&amp;status=SHIPPING\"")));
        mvc.perform(get("/admin/orders/" + maDonGiao).session(phienAdmin).param("nav", "dashboard").param("range", "7d"))
            .andExpect(content().string(containsString("href=\"/admin/dashboard?range=7d\"")));
        mvc.perform(post("/admin/orders/" + maDonGiao + "/status").session(session).with(csrf()).param("status", "DELIVERED"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/orders/" + maDonGiao + "/status").session(phienAdmin).with(csrf()).param("status", "SHIPPING"))
            .andExpect(flash().attributeExists("error"));
        assertThat(trangThai(maDonGiao)).isEqualTo("PENDING_CONFIRMATION");
        for (String status : List.of("CONFIRMED", "PACKING", "SHIPPING", "DELIVERED")) {
            mvc.perform(post("/admin/orders/" + maDonGiao + "/status").session(phienAdmin).with(csrf()).param("status", status))
                .andExpect(flash().attributeExists("message"));
            assertThat(trangThai(maDonGiao)).isEqualTo(status);
            if ("PACKING".equals(status)) {
                mvc.perform(post("/orders/" + maDonGiao + "/cancel").session(session).with(csrf()))
                    .andExpect(flash().attributeExists("error"));
            }
        }
        assertThat(jdbc.sql("SELECT trang_thai FROM lan_thanh_toan WHERE ma_don_hang = ?").param(maDonGiao)
            .query(String.class).single()).isEqualTo("PAID");
        assertThat(jdbc.sql("SELECT trang_thai FROM van_chuyen WHERE ma_don_hang = ?").param(maDonGiao)
            .query(String.class).single()).isEqualTo("DELIVERED");
        assertThat(ton(variant)).isEqualTo(2);
        var tongQuanDaThu = tongQuanAdmin();
        assertThat(tongQuanDaThu.soDonDaThu()).isEqualTo(tongQuanChuaThu.soDonDaThu() + 1);
        assertThat(tongQuanDaThu.doanhThuDaThu())
            .isEqualByComparingTo(tongQuanChuaThu.doanhThuDaThu().add(new BigDecimal("600000")));
    }

    @Test
    void checkoutHetHangGiuNguyenMucKhacVaAdminHuyHoanTonMotLan() throws Exception {
        var khach = taiKhoan(false);
        var admin = taiKhoan(true);
        var phienKhach = login(khach.email(), new MockHttpSession());
        var phienAdmin = login(admin.email(), new MockHttpSession());
        long skuConHang = taoSku(2, 510000);
        long skuHetHang = taoSku(1, 100000);
        gioHangService.themGioCustomer(khach.id(), skuConHang);
        gioHangService.themGioCustomer(khach.id(), skuHetHang);
        jdbc.sql("UPDATE bien_the_san_pham SET so_luong_trong_kho = 0 WHERE ma_bien_the = ?")
            .param(skuHetHang).update();
        mvc.perform(post("/checkout").session(phienKhach).with(csrf()).param("addressId", Long.toString(khach.maDiaChi())))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"))
            .andExpect(content().string(containsString("Không còn đủ số lượng đã chọn")));
        assertThat(donHangService.danhSachCustomer(khach.id())).isEmpty();
        assertThat(ton(skuConHang)).isEqualTo(2);
        mvc.perform(post("/cart/items/" + skuHetHang + "/quantity").session(phienKhach).with(csrf()).param("quantity", "0"))
            .andExpect(redirectedUrl("/cart"));
        long maDon = datDon(phienKhach, khach.maDiaChi(), "");
        assertThat(ton(skuConHang)).isEqualTo(1);
        for (int lan = 0; lan < 2; lan++) {
            mvc.perform(post("/admin/orders/" + maDon + "/status").session(phienAdmin).with(csrf()).param("status", "CANCELLED"))
                .andExpect(redirectedUrl("/admin/orders/" + maDon));
            assertThat(ton(skuConHang)).isEqualTo(2);
            assertThat(trangThai(maDon)).isEqualTo("CANCELLED");
        }
    }

    @Test
    void haiCustomerTranhSkuVaRollbackKhoVoucherVaGio() throws Exception {
        var a = taiKhoan(false);
        var b = taiKhoan(false);
        long variant = taoSku(1, 510000);
        String voucher = taoVoucher("PERCENT", 10, 20000, 1, false);
        String hetHan = taoVoucher("FIXED", 10000, null, 1, true);
        gioHangService.themGioCustomer(a.id(), variant);
        gioHangService.themGioCustomer(b.id(), variant);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> donHangService.datCOD(a.id(), a.maDiaChi(), hetHan))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(ton(variant)).isEqualTo(1);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            donHangService.datCOD(a.id(), a.maDiaChi(), voucher);
            status.setRollbackOnly();
        });
        assertThat(ton(variant)).isEqualTo(1);
        assertThat(luot(voucher)).isZero();
        assertThat(gioHangRepository.docGio(a.id())).hasSize(1);
        assertThat(donHangService.danhSachCustomer(a.id())).isEmpty();

        var batDau = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var ra = executor.submit(() -> canhTranh(a, voucher, batDau));
            var rb = executor.submit(() -> canhTranh(b, voucher, batDau));
            batDau.countDown();
            var ketQua = List.of(ra.get(15, TimeUnit.SECONDS), rb.get(15, TimeUnit.SECONDS));
            assertThat(ketQua.stream().filter(ma -> ma > 0).count()).isEqualTo(1);
            assertThat(gioHangRepository.docGio(ketQua.getFirst() == 0 ? a.id() : b.id())).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
        assertThat(ton(variant)).isZero();
        assertThat(luot(voucher)).isEqualTo(1);
    }

    private long canhTranh(TaiKhoan taiKhoan, String voucher, CountDownLatch batDau) throws InterruptedException {
        if (!batDau.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Không bắt đầu được checkout.");
        }
        try {
            return donHangService.datCOD(taiKhoan.id(), taiKhoan.maDiaChi(), voucher);
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).contains("Không còn đủ số lượng đã chọn");
            return 0;
        }
    }

    @Test
    void voucherConMotLuotChiChoMotDonKhiHaiSkuKhacNhau() throws Exception {
        var a = taiKhoan(false);
        var b = taiKhoan(false);
        var phienAdmin = login(taiKhoan(true).email(), new MockHttpSession());
        long skuA = taoSku(1, 510000);
        long skuB = taoSku(1, 510000);
        String voucher = UUID.randomUUID().toString();
        String ship = taoVoucher("FIXED",10000,null,1,false);
        jdbc.sql("UPDATE phieu_giam_gia SET slot='VAN_CHUYEN' WHERE ma_ap_dung=?").param(ship).update();
        var now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
        mvc.perform(post("/admin/vouchers/new").session(phienAdmin).with(csrf())
                .param("maApDung", voucher).param("loaiGiamGia", "PERCENT").param("giaTriGiam", "10")
                .param("mucGiamToiDa", "20000").param("donToiThieu", "0").param("gioiHanSuDung", "1")
                .param("batDauLuc", now.minusDays(1).toString()).param("ketThucLuc", now.plusDays(1).toString())
                .param("dangHoatDong", "true"))
            .andExpect(redirectedUrl("/admin/vouchers"));
        gioHangService.themGioCustomer(a.id(), skuA);
        gioHangService.themGioCustomer(b.id(), skuB);
        var batDau = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var ra = executor.submit(() -> tranhVoucher(a, voucher, ship, batDau));
            var rb = executor.submit(() -> tranhVoucher(b, voucher, ship, batDau));
            batDau.countDown();
            var ketQua = List.of(ra.get(15, TimeUnit.SECONDS), rb.get(15, TimeUnit.SECONDS));
            assertThat(ketQua.stream().filter(ma -> ma > 0).count()).isEqualTo(1);
            long maDon = ketQua.stream().filter(ma -> ma > 0).findFirst().orElseThrow();
            assertThat(jdbc.sql("SELECT tong_thanh_toan FROM don_hang WHERE ma_don_hang = ?").param(maDon)
                .query(java.math.BigDecimal.class).single()).isEqualByComparingTo("505000");
        } finally {
            executor.shutdownNow();
        }
        assertThat(ton(skuA) + ton(skuB)).isEqualTo(1);
        assertThat(luot(ship)).isEqualTo(1);
        assertThat(luot(voucher)).isEqualTo(1);
    }

    @Test
    void quotaKhachVaRollbackHaiSlot() throws Exception {
        var a = taiKhoan(false);
        long sku = taoSku(5,520000);
        String hang = taoVoucher("FIXED",30000,null,5,false);
        String ship = taoVoucher("FIXED",10000,null,0,false);
        jdbc.sql("UPDATE phieu_giam_gia SET slot='VAN_CHUYEN',gioi_han_khach=1 WHERE ma_ap_dung=?").param(ship).update();
        gioHangService.themGioCustomer(a.id(),sku);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> donHangService.datCOD(a.id(),a.maDiaChi(),hang,ship))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(luot(hang)).isZero();
        jdbc.sql("UPDATE phieu_giam_gia SET gioi_han_su_dung=4 WHERE ma_ap_dung=?").param(ship).update();
        long shipId = jdbc.sql("SELECT ma_phieu_giam_gia FROM phieu_giam_gia WHERE ma_ap_dung=?").param(ship).query(Long.class).single();
        // Inject a fixture-only error precisely between the first and second durable slot.
        String trigger = "qa_slot_" + shipId;
        jdbc.sql("CREATE TRIGGER " + trigger + " BEFORE INSERT ON tam_giu_phieu_giam_gia FOR EACH ROW BEGIN "
            + "IF NEW.ma_phieu_giam_gia=" + shipId + " THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture'; END IF; END").update();
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> donHangService.datCOD(a.id(),a.maDiaChi(),hang,ship))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        } finally {
            jdbc.sql("DROP TRIGGER " + trigger).update();
        }
        assertThat(luot(hang)).isZero();
        assertThat(luot(ship)).isZero();
        assertThat(ton(sku)).isEqualTo(5);
        assertThat(donHangService.danhSachCustomer(a.id())).isEmpty();
        long don = donHangService.datCOD(a.id(),a.maDiaChi(),hang,ship);
        assertThat(jdbc.sql("SELECT giam_hang FROM chi_tiet_don_hang WHERE ma_don_hang=?").param(don)
            .query(BigDecimal.class).single()).isEqualByComparingTo("30000");
        gioHangService.themGioCustomer(a.id(),sku);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> donHangService.datCOD(a.id(),a.maDiaChi(),hang,ship))
            .hasMessageContaining("hết lượt");
        var phien = login(a.email(),new MockHttpSession());
        for (int lan=0; lan<2; lan++) mvc.perform(post("/orders/"+don+"/cancel").session(phien).with(csrf()))
            .andExpect(flash().attributeExists("message"));
        assertThat(luot(hang)).isZero();
        assertThat(luot(ship)).isZero();
        assertThat(ton(sku)).isEqualTo(5);
        assertThat(donHangService.datCOD(a.id(),a.maDiaChi(),hang,ship)).isPositive();
    }

    @Test
    void haiPostMuaLaiCungYeuCauChiGhiMotLan() throws Exception {
        var chu = taiKhoan(false);
        var khac = taiKhoan(false);
        var phien = login(chu.email(), new MockHttpSession());
        long sku = taoSku(12, 510000);
        long ngung = taoSku(3, 9000);
        long het = taoSku(1, 1000);
        String hang = taoVoucher("FIXED", 30000, null, 4, false);
        String ship = taoVoucher("FIXED", 10000, null, 4, false);
        jdbc.sql("UPDATE phieu_giam_gia SET slot='VAN_CHUYEN' WHERE ma_ap_dung=?").param(ship).update();
        gioHangService.themGioCustomer(chu.id(), sku);
        gioHangService.themGioCustomer(chu.id(), ngung);
        gioHangService.themGioCustomer(chu.id(), het);
        long don = donHangService.datCOD(chu.id(), chu.maDiaChi(), hang, ship);
        gioHangService.themGioCustomer(chu.id(), sku);
        jdbc.sql("UPDATE bien_the_san_pham SET gia_ban=490000 WHERE ma_bien_the=?").param(sku).update();
        jdbc.sql("UPDATE thuong_hieu t JOIN san_pham s USING(ma_thuong_hieu) JOIN bien_the_san_pham b USING(ma_san_pham) SET t.dang_hoat_dong=false WHERE b.ma_bien_the=?").param(ngung).update();
        String url = "/orders/" + don + "/reorder";
        var form = formMuaLai(phien,don);
        mvc.perform(post(url).with(csrf()).session(login(khac.email(), new MockHttpSession())).params(form))
            .andExpect(status().isNotFound());
        mvc.perform(post(url).session(phien).params(form)).andExpect(status().isForbidden());
        mvc.perform(post(url).with(csrf()).params(form)).andExpect(status().is3xxRedirection());
        mvc.perform(post(url).with(csrf()).session(login(taiKhoan(true).email(), new MockHttpSession())).params(form))
            .andExpect(status().isForbidden());
        String truoc = chungTu(don);
        var batDau = new java.util.concurrent.CyclicBarrier(2);
        java.util.concurrent.Callable<Void> gui = () -> {
            batDau.await(10, TimeUnit.SECONDS);
            mvc.perform(post(url).with(csrf()).session(phien).params(form))
                .andExpect(redirectedUrl("/cart"));
            return null;
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(gui);
            var b = pool.submit(gui);
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
        }
        var gio = gioHangService.docGioCustomer(chu.id());
        assertThat(gio).hasSize(1);
        assertThat(gio.getFirst().soLuong()).isEqualTo(2);
        assertThat(gio.getFirst().giaBan()).isEqualByComparingTo("490000");
        mvc.perform(post(url).with(csrf()).session(phien).params(formMuaLai(phien,don)))
            .andExpect(redirectedUrl("/cart"));
        assertThat(gioHangService.docGioCustomer(chu.id()).getFirst().soLuong()).isEqualTo(3);
        assertThat(chungTu(don)).isEqualTo(truoc);
        assertThat(ton(sku)).isEqualTo(11);
        assertThat(ton(ngung)).isEqualTo(2);
        assertThat(luot(hang)).isOne();
        assertThat(luot(ship)).isOne();
        assertThat(ton(het)).isZero();
        assertThat(donHangService.danhSachCustomer(chu.id())).hasSize(1);
        assertThat(jdbc.sql("SELECT so_tien FROM lan_thanh_toan WHERE ma_don_hang=?").param(don)
            .query(BigDecimal.class).single()).isEqualByComparingTo("505000");
        assertThat(jdbc.sql("SELECT CONCAT(slot, ':', so_tien_giam, ':', trang_thai) FROM tam_giu_phieu_giam_gia WHERE ma_don_hang=? ORDER BY slot")
            .param(don).query(String.class).list()).containsExactly("HANG:30000:CONSUMED", "VAN_CHUYEN:10000:CONSUMED");
        assertThat(jdbc.sql("SELECT tong_thanh_toan FROM don_hang WHERE ma_don_hang=?")
            .param(don).query(BigDecimal.class).single()).isEqualByComparingTo("505000");
        assertThat(jdbc.sql("SELECT don_gia FROM chi_tiet_don_hang WHERE ma_don_hang=? AND ma_bien_the=?")
            .params(don,sku).query(BigDecimal.class).single()).isEqualByComparingTo("510000");
    }

    @Test
    void locDonCustomerCungPredicateChoCountVaTrang() throws Exception {
        var chu = taiKhoan(false);
        var khac = taiKhoan(false);
        var loc = new com.beautyshop.dto.BoLocDonHangAdmin(List.of(com.beautyshop.order.OrderStatus.PENDING_CONFIRMATION),
            null, null, "QA_", com.beautyshop.dto.BoLocDonHangAdmin.SapXep.totalAsc, 10,
            java.time.LocalDate.of(2026,11,28), java.time.LocalDate.of(2026,11,28));
        String insert = """
            INSERT INTO don_hang(ma_don_hang_hien_thi,ma_nguoi_dung,ten_nguoi_nhan,sdt_nguoi_nhan,dia_chi_khi_dat,
                tam_tinh,so_tien_giam,phi_van_chuyen,tong_thanh_toan,trang_thai,phuong_thuc_thanh_toan,ngay_tao)
            VALUES(?,?,'Fixture','0','Fixture',1000,0,0,1000,'PENDING_CONFIRMATION','COD',?) RETURNING ma_don_hang
            """;
        var ids = new java.util.ArrayList<Long>();
        for (int i=0; i<11; i++) {
            ids.add(jdbc.sql(insert).params("QA_"+UUID.randomUUID(),chu.id(),"2026-11-27 17:00:00").query(Long.class).single());
        }
        jdbc.sql(insert).params("QA_"+UUID.randomUUID(),khac.id(),"2026-11-27 17:00:00").query(Long.class).single();
        jdbc.sql(insert).params("QA_"+UUID.randomUUID(),chu.id(),"2026-11-28 17:00:00").query(Long.class).single();
        jdbc.sql(insert).params("QA-"+UUID.randomUUID(),chu.id(),"2026-11-27 17:00:00").query(Long.class).single();
        var trang = donHangService.danhSachCustomer(chu.id(),loc,org.springframework.data.domain.PageRequest.of(1,10));
        assertThat(trang.getTotalElements()).isEqualTo(11);
        assertThat(trang.getContent()).extracting("maDonHang").containsExactly(ids.getFirst());
    }

    @Test
    void khoaTrangThaiSanPhamDenKhiGhiGioHoanTat() throws Exception {
        var chu = taiKhoan(false);
        long sku = taoSku(3, 100000);
        var sanPham = jdbc.sql("SELECT ma_san_pham FROM bien_the_san_pham WHERE ma_bien_the=?")
            .param(sku).query(Long.class).single();
        var daDoc = new CountDownLatch(1);
        var choGhi = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var ghiGio = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                long gio = gioHangRepository.khoaGio(chu.id());
                assertThat(gioHangRepository.timBienThe(sku,true)).isPresent();
                daDoc.countDown();
                try {
                    if (!choGhi.await(10,TimeUnit.SECONDS)) throw new AssertionError("Chờ ghi giỏ quá hạn");
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(err);
                }
                gioHangRepository.luuSoLuong(gio,sku,1);
            }));
            assertThat(daDoc.await(10,TimeUnit.SECONDS)).isTrue();
            try {
                var sua = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                    // NOWAIT makes a conflicting admin write observable without timing assumptions.
                    jdbc.sql("SELECT ma_san_pham FROM san_pham WHERE ma_san_pham=? FOR UPDATE NOWAIT")
                        .param(sanPham).query(Long.class).single();
                }));
                org.assertj.core.api.Assertions.assertThatThrownBy(() -> sua.get(5,TimeUnit.SECONDS))
                    .isInstanceOf(java.util.concurrent.ExecutionException.class)
                    .rootCause().isInstanceOfSatisfying(java.sql.SQLException.class,
                        loi -> assertThat(loi.getErrorCode()).isEqualTo(1205));
            } finally {
                choGhi.countDown();
            }
            ghiGio.get(5,TimeUnit.SECONDS);
        }
        jdbc.sql("UPDATE san_pham SET dang_hoat_dong=false WHERE ma_san_pham=?").param(sanPham).update();
        assertThat(gioHangService.docGioCustomer(chu.id()).getFirst().khaDung()).isZero();
    }

    private org.springframework.util.MultiValueMap<String,String> formMuaLai(MockHttpSession phien, long don) throws Exception {
        var model = mvc.perform(get("/orders/" + don).session(phien)).andExpect(status().isOk())
            .andReturn().getModelAndView().getModel();
        var form = new org.springframework.util.LinkedMultiValueMap<String,String>();
        form.add("requestId", model.get("maMuaLai").toString());
        form.add("proof", String.valueOf(model.get("chuKyMuaLai")));
        return form;
    }

    private String chungTu(long don) {
        // Compare complete fixture rows in memory. Only the test verdict is exported.
        var bang = List.of("don_hang", "chi_tiet_don_hang", "lan_thanh_toan", "tam_giu_phieu_giam_gia",
            "tam_giu_ton_kho", "ghi_nhan_tru_kho_cod", "van_chuyen");
        var snapshot = new StringBuilder();
        for (String ten : bang) {
            jdbc.sql("SELECT * FROM " + ten + " WHERE ma_don_hang=? ORDER BY 1,2")
                .param(don).query().listOfRows().forEach(snapshot::append);
        }
        return snapshot.toString();
    }

    @Test
    void formMuaLaiChiApDungChoDonVaPhienDaCap() throws Exception {
        var chu = taiKhoan(false);
        var phien = login(chu.email(), new MockHttpSession());
        long sku = taoSku(8, 100000);
        gioHangService.themGioCustomer(chu.id(),sku);
        long a = donHangService.datCOD(chu.id(),chu.maDiaChi(),null);
        gioHangService.themGioCustomer(chu.id(),sku);
        long b = donHangService.datCOD(chu.id(),chu.maDiaChi(),null);
        var form = formMuaLai(phien,a);
        mvc.perform(post("/orders/"+b+"/reorder").session(phien).with(csrf()).params(form))
            .andExpect(status().isNotFound());
        mvc.perform(post("/orders/"+a+"/reorder").session(login(chu.email(),new MockHttpSession())).with(csrf()).params(form))
            .andExpect(status().isNotFound());
        assertThat(gioHangService.docGioCustomer(chu.id())).isEmpty();
    }

    @Test
    void rollbackRetryVaHaiYeuCauTranhDonViCuoi() throws Exception {
        var chu = taiKhoan(false);
        var phien = login(chu.email(), new MockHttpSession());
        long sku = taoSku(2, 100000);
        gioHangService.themGioCustomer(chu.id(),sku);
        long don = donHangService.datCOD(chu.id(),chu.maDiaChi(),null);
        var form = formMuaLai(phien,don);
        String url = "/orders/"+don+"/reorder";
        String trigger = "qa_mua_lai_" + sku;
        jdbc.sql("CREATE TRIGGER " + trigger + " AFTER INSERT ON chi_tiet_gio_hang FOR EACH ROW BEGIN "
            + "IF NEW.ma_bien_the=" + sku + " THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='ORDER fixture rollback'; END IF; END").update();
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> mvc.perform(
                post(url).session(phien).with(csrf()).params(form)))
                .rootCause().hasMessageContaining("ORDER fixture rollback");
        } finally {
            jdbc.sql("DROP TRIGGER " + trigger).update();
        }
        assertThat(gioHangService.docGioCustomer(chu.id())).isEmpty();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM ghi_nhan_gop_gio_hang WHERE ma_nguoi_dung=?")
            .param(chu.id()).query(Long.class).single()).isZero();
        var id = UUID.fromString(form.getFirst("requestId"));
        // Pre-existing v4 merge receipt with exactly the same raw UUID as the reorder form.
        assertThat(gioHangRepository.ghiNhanGop(id.toString(),chu.id())).isTrue();
        mvc.perform(post(url).session(phien).with(csrf()).params(form)).andExpect(redirectedUrl("/cart"));
        assertThat(gioHangService.docGioCustomer(chu.id()).getFirst().soLuong()).isOne();
        gioHangService.datSoLuongCustomer(chu.id(),sku,0);
        mvc.perform(post(url).session(phien).with(csrf()).params(form)).andExpect(redirectedUrl("/cart"));
        assertThat(gioHangService.docGioCustomer(chu.id())).isEmpty();
        var start = new java.util.concurrent.CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Void>>();
            for (int i=0; i<2; i++) {
                var moi = formMuaLai(phien,don);
                futures.add(pool.submit(() -> {
                    start.await(10,TimeUnit.SECONDS);
                    mvc.perform(post(url).session(phien).with(csrf()).params(moi)).andExpect(redirectedUrl("/cart"));
                    return null;
                }));
            }
            for (var future : futures) future.get(15,TimeUnit.SECONDS);
        }
        assertThat(gioHangService.docGioCustomer(chu.id()).getFirst().soLuong()).isOne();
        assertThat(ton(sku)).isOne();
    }

    private long tranhVoucher(TaiKhoan taiKhoan, String voucher, String ship, CountDownLatch batDau) throws InterruptedException {
        if (!batDau.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Không bắt đầu được checkout.");
        }
        try {
            return donHangService.datCOD(taiKhoan.id(), taiKhoan.maDiaChi(), voucher, ship);
        } catch (IllegalArgumentException exception) {
            assertThat(exception.getMessage()).contains("Mã giảm giá");
            return 0;
        }
    }

    private long datDon(MockHttpSession session, long diaChi, String voucher) throws Exception {
        String url = mvc.perform(post("/checkout").session(session).with(csrf()).param("addressId", Long.toString(diaChi))
                .param("voucher", voucher).param("price", "1"))
            .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        assertThat(url).startsWith("/orders/");
        return Long.parseLong(url.substring("/orders/".length()));
    }

    private TaiKhoan taiKhoan(boolean admin) throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        DangKyOtp_TestSupport.dangKy(mvc, otp, email, matKhau);
        long id = nguoiDungRepository.findByEmail(email).orElseThrow().getMaNguoiDung();
        if (admin) {
            jdbc.sql("UPDATE nguoi_dung SET vai_tro = 'ADMIN' WHERE ma_nguoi_dung = ?").param(id).update();
            return new TaiKhoan(email, id, 0);
        }
        long diaChi = jdbc.sql("""
            INSERT INTO dia_chi_nguoi_dung (ma_nguoi_dung, ten_nguoi_nhan, so_dien_thoai, dia_chi_chi_tiet, la_mac_dinh)
            VALUES (?, 'Người nhận', '0901234567', 'Địa chỉ snapshot', TRUE) RETURNING ma_dia_chi
            """).param(id).query(Long.class).single();
        return new TaiKhoan(email, id, diaChi);
    }

    private MockHttpSession login(String email, MockHttpSession session) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").session(session).with(csrf())
                .param("username", email).param("password", matKhau))
            .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }

    private long taoSku(int ton, int gia) {
        String ma = UUID.randomUUID().toString();
        long danhMuc = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Test COD', ?) RETURNING ma_danh_muc")
            .param(ma).query(Long.class).single();
        long thuongHieu = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Test COD', ?) RETURNING ma_thuong_hieu")
            .param(ma).query(Long.class).single();
        long sanPham = jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
            VALUES (?, ?, 'SKU COD', ?) RETURNING ma_san_pham
            """).params(danhMuc, thuongHieu, ma).query(Long.class).single();
        return jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, '30 ml', ?, ?) RETURNING ma_bien_the
            """).params(sanPham, ma, gia, ton).query(Long.class).single();
    }

    private String taoVoucher(String loai, int giaTri, Integer toiDa, int soLuot, boolean hetHan) {
        String ma = UUID.randomUUID().toString();
        jdbc.sql("""
            INSERT INTO phieu_giam_gia (ma_ap_dung, loai_giam_gia, gia_tri_giam, muc_giam_toi_da, gioi_han_su_dung,
                bat_dau_luc, ket_thuc_luc) VALUES (:ma, :loai, :giaTri, :toiDa, :soLuot, UTC_TIMESTAMP() - INTERVAL 2 DAY,
                TIMESTAMPADD(DAY, :soNgay, UTC_TIMESTAMP()))
            """).param("ma", ma).param("loai", loai).param("giaTri", giaTri).param("toiDa", toiDa, java.sql.Types.INTEGER)
            .param("soLuot", soLuot).param("soNgay", hetHan ? -1 : 1).update();
        return ma;
    }

    private int ton(long variant) {
        return jdbc.sql("SELECT so_luong_trong_kho FROM bien_the_san_pham WHERE ma_bien_the = ?")
            .param(variant).query(Integer.class).single();
    }

    private int luot(String voucher) {
        return jdbc.sql("SELECT so_luot_da_dung FROM phieu_giam_gia WHERE ma_ap_dung = ?").param(voucher).query(Integer.class).single();
    }

    private String trangThai(long id) {
        return jdbc.sql("SELECT trang_thai FROM don_hang WHERE ma_don_hang = ?").param(id).query(String.class).single();
    }

    private record TaiKhoan(String email, long id, long maDiaChi) {}

    /** P20: tongQuanAdmin() có @PreAuthorize ADMIN; test gọi trực tiếp service nên đặt ngữ cảnh Admin tạm thời. */
    private com.beautyshop.repository.DonHang_Repository.TongQuan tongQuanAdmin() {
        var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
            "admin-test", null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        try {
            return donHangService.tongQuanAdmin();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
