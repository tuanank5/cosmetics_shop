package com.beautyshop.integration;

import com.beautyshop.domain.catalog.NhomDanhMucSanPhamThuongHieu;
import com.beautyshop.order.OrderStatus;
import com.beautyshop.repository.DonHangCatalog_Repository.PhamVi;
import com.beautyshop.service.BienTheSanPham_Service;
import com.beautyshop.service.DanhMucSanPhamThuongHieu_Service;
import com.beautyshop.service.DonHang_Service;
import com.beautyshop.service.GioHang_Service;
import com.beautyshop.service.NgungHoatDongCatalog_Service;
import com.beautyshop.service.NgungHoatDongCatalog_Service.ConDonDangXuLy;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"app.payment.momo.enabled=true", "app.payment.momo.mode=mock"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
@WithMockUser(roles = "ADMIN")
class CatalogDonHang_MariaDb_Test {
    @Autowired JdbcClient jdbc;
    @Autowired MockMvc mvc;
    @Autowired DonHang_Service donHang;
    @Autowired GioHang_Service gioHang;
    @Autowired BienTheSanPham_Service bienThe;
    @Autowired DanhMucSanPhamThuongHieu_Service catalog;
    @Autowired NgungHoatDongCatalog_Service ngungHoatDong;
    @Autowired PlatformTransactionManager transactionManager;

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void trangThaiDonQuyetDinhCoDuocNgungSku(OrderStatus trangThai) throws Exception {
        var f = fixture();
        long don = taoDon(f, trangThai);
        boolean chan = switch (trangThai) {
            case DELIVERED, CANCELLED, EXPIRED -> false;
            default -> true;
        };
        if (chan) {
            mvc.perform(post(f.skuPath() + "/delete").with(csrf()))
                .andExpect(redirectedUrl(f.skuList())).andExpect(flash().attributeExists("error"));
        } else {
            mvc.perform(post(f.skuPath() + "/status").param("active", "false").with(csrf()))
                .andExpect(flash().attributeExists("message"));
        }
        assertThat(hoatDong("bien_the_san_pham", "ma_bien_the", f.sku())).isEqualTo(chan);
        mvc.perform(get("/admin/orders/" + don)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Tên sản phẩm lúc đặt")));
    }

    @Test
    void kiemTraToanBoSkuVaCacDuongLuuTrangThai() throws Exception {
        var f = fixture();
        mvc.perform(post(f.skuPath() + "/delete").with(csrf())).andExpect(flash().attributeExists("message"));
        mvc.perform(post(f.skuPath() + "/status").param("active", "true").with(csrf()))
            .andExpect(flash().attributeExists("message"));
        var don = taoDon(f, OrderStatus.PACKING);
        long skuKhac = taoSku(f.sanPham());
        themDong(don, skuKhac);
        jdbc.sql("UPDATE bien_the_san_pham SET dang_hoat_dong = FALSE WHERE ma_bien_the = ?").param(skuKhac).update();
        for (var phamVi : PhamVi.values()) {
            assertThat(ngungHoatDong.dem(phamVi, f.ma(phamVi))).isEqualTo(1);
        }
        mvc.perform(get(f.skuList())).andExpect(status().isOk())
            .andExpect(content().string(containsString("Còn 1 đơn đang xử lý")))
            .andExpect(content().string(containsString("disabled=\"disabled\"")));
        mvc.perform(get(f.skuPath() + "/edit")).andExpect(status().isOk())
            .andExpect(content().string(containsString("sku-status-help")));
        mvc.perform(multipart(f.skuPath() + "/edit").param("sku", "KEPT-SKU")
                .param("tenBienThe", "Thay đổi bị từ chối").param("giaBan", "100000").param("soLuongTrongKho", "5")
                .param("_dangHoatDong", "on").with(csrf()))
            .andExpect(model().attributeHasFieldErrors("form", "dangHoatDong"));
        assertThat(hoatDong("bien_the_san_pham", "ma_bien_the", f.sku())).isTrue();
        assertThat(jdbc.sql("SELECT ten_bien_the FROM bien_the_san_pham WHERE ma_bien_the = ?")
            .param(f.sku()).query(String.class).single()).isEqualTo("30 ml");
        for (var nhom : NhomDanhMucSanPhamThuongHieu.values()) {
            String path = "/admin/catalog/" + nhom.path + "/" + f.ma(PhamVi.tu(nhom));
            mvc.perform(post(path + "/status").param("active", "false").with(csrf()))
                .andExpect(flash().attributeExists("error"));
            mvc.perform(post(path + "/edit").param("name", "Thay đổi bị từ chối")
                    .param("slug", "blocked-" + UUID.randomUUID())
                    .param("categoryId", Long.toString(f.danhMuc())).param("brandId", Long.toString(f.thuongHieu()))
                    .param("_active", "on").with(csrf()))
                .andExpect(model().attributeHasErrors("form"));
            String bang = switch (nhom) {
                case SAN_PHAM -> "san_pham";
                case DANH_MUC -> "danh_muc";
                case THUONG_HIEU -> "thuong_hieu";
            };
            assertThat(hoatDong(bang, "ma_" + bang, f.ma(PhamVi.tu(nhom)))).isTrue();
        }
        mvc.perform(post("/admin/catalog/products/" + f.sanPham() + "/delete").with(csrf()))
            .andExpect(flash().attributeExists("error"));
        mvc.perform(post(f.skuPath() + "/delete").with(csrf()).with(user("khach").roles("CUSTOMER")))
            .andExpect(status().isForbidden());
        mvc.perform(post(f.skuPath() + "/delete")).andExpect(status().isForbidden());
    }

    @Test
    void donKetThucVanChanNeuTonThanhToanHoacDoiTraConTreo() {
        var f = fixture();
        long don = taoDon(f, OrderStatus.EXPIRED);
        jdbc.sql("""
            INSERT INTO tam_giu_ton_kho (ma_don_hang, ma_bien_the, so_luong, trang_thai, het_han_luc)
            VALUES (?, ?, 1, 'HELD', UTC_TIMESTAMP() - INTERVAL 1 MINUTE)
            """).params(don, f.sku()).update();
        assertThat(ngungHoatDong.dem(PhamVi.BIEN_THE, f.sku())).isEqualTo(1);
        jdbc.sql("UPDATE tam_giu_ton_kho SET trang_thai = 'RELEASED' WHERE ma_don_hang = ?").param(don).update();
        long lan = jdbc.sql("""
            INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai)
            VALUES (?, ?, 'MOMO_MOCK', 125000, 'PENDING') RETURNING ma_lan_thanh_toan
            """).params(don, UUID.randomUUID().toString()).query(Long.class).single();
        assertThat(ngungHoatDong.dem(PhamVi.BIEN_THE, f.sku())).isEqualTo(1);
        jdbc.sql("UPDATE lan_thanh_toan SET trang_thai = 'REFUND_PENDING' WHERE ma_lan_thanh_toan = ?").param(lan).update();
        assertThat(ngungHoatDong.dem(PhamVi.BIEN_THE, f.sku())).isEqualTo(1);
        jdbc.sql("UPDATE lan_thanh_toan SET trang_thai = 'FAILED' WHERE ma_lan_thanh_toan = ?").param(lan).update();
        assertThat(ngungHoatDong.dem(PhamVi.BIEN_THE, f.sku())).isZero();

        long daGiao = taoDon(f, OrderStatus.DELIVERED);
        jdbc.sql("UPDATE lan_thanh_toan SET so_tien = 100000 WHERE ma_don_hang = ?").param(daGiao).update();
        assertThat(ngungHoatDong.dem(PhamVi.BIEN_THE, f.sku())).isEqualTo(1);
        jdbc.sql("UPDATE lan_thanh_toan SET so_tien = 125000 WHERE ma_don_hang = ?").param(daGiao).update();
        long doiTra = jdbc.sql("""
            INSERT INTO yeu_cau_doi_tra (ma_don_hang, ma_nguoi_dung, trang_thai, ly_do)
            VALUES (?, ?, 'RECEIVED', 'Kiểm thử') RETURNING ma_yeu_cau_doi_tra
            """).params(daGiao, f.khach()).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO chi_tiet_doi_tra (ma_yeu_cau_doi_tra, ma_chi_tiet_don_hang, so_luong)
            SELECT ?, ma_chi_tiet_don_hang, 1 FROM chi_tiet_don_hang WHERE ma_don_hang = ?
            """).params(doiTra, daGiao).update();
        assertThat(ngungHoatDong.dem(PhamVi.BIEN_THE, f.sku())).isEqualTo(1);
        jdbc.sql("UPDATE yeu_cau_doi_tra SET trang_thai = 'COMPLETED' WHERE ma_yeu_cau_doi_tra = ?").param(doiTra).update();
        bienThe.ngungHoatDong(f.sanPham(), f.sku());
        assertThat(hoatDong("bien_the_san_pham", "ma_bien_the", f.sku())).isFalse();
    }

    static Stream<Arguments> tranhChap() {
        return Stream.of(PhamVi.values())
            .flatMap(phamVi -> Stream.of("COD", "MOMO").flatMap(phuongThuc -> Stream.of(
                Arguments.of(phamVi, phuongThuc, true), Arguments.of(phamVi, phuongThuc, false))));
    }

    @ParameterizedTest(name = "{0} {1} checkout giữ khóa trước = {2}")
    @MethodSource("tranhChap")
    void checkoutVaNgungHoatDongTuanTu(PhamVi phamVi, String phuongThuc, boolean checkoutTruoc) throws Exception {
        var f = fixture();
        gioHang.themGioCustomer(f.khach(), f.sku());
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        var batDau = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var ketQua = transaction.execute(status -> {
                if (checkoutTruoc) datDon(f, phuongThuc);
                else tat(f, phamVi);

                var future = executor.submit(() -> {
                    SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("fixture-admin", null, "ROLE_ADMIN"));
                    try {
                        batDau.countDown();
                        if (checkoutTruoc) {
                            assertThatThrownBy(() -> tat(f, phamVi)).isInstanceOf(ConDonDangXuLy.class);
                        } else {
                            assertThatThrownBy(() -> datDon(f, phuongThuc)).isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("Không còn đủ số lượng");
                        }
                        return true;
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                });
                assertThat(await(batDau)).isTrue();
                assertThatThrownBy(() -> future.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                return future;
            });
            assertThat(ketQua.get(12, TimeUnit.SECONDS)).isTrue();
        }
        long soDon = jdbc.sql("SELECT count(*) FROM don_hang WHERE ma_nguoi_dung = ?").param(f.khach()).query(Long.class).single();
        assertThat(soDon).isEqualTo(checkoutTruoc ? 1 : 0);
        assertThat(jdbc.sql("SELECT so_luong_trong_kho, so_luong_tam_giu FROM bien_the_san_pham WHERE ma_bien_the = ?")
            .param(f.sku()).query((rs, row) -> rs.getInt(1) + ":" + rs.getInt(2)).single())
            .isEqualTo(!checkoutTruoc ? "5:0" : "COD".equals(phuongThuc) ? "4:0" : "5:1");
    }

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private long datDon(Fixture f, String phuongThuc) {
        return "COD".equals(phuongThuc) ? donHang.datCOD(f.khach(), f.diaChi(), null) : donHang.datMomo(f.khach(), f.diaChi(), null);
    }

    private void tat(Fixture f, PhamVi phamVi) {
        switch (phamVi) {
            case BIEN_THE -> bienThe.ngungHoatDong(f.sanPham(), f.sku());
            case SAN_PHAM -> catalog.datTrangThai(NhomDanhMucSanPhamThuongHieu.SAN_PHAM, f.sanPham(), false);
            case DANH_MUC -> catalog.datTrangThai(NhomDanhMucSanPhamThuongHieu.DANH_MUC, f.danhMuc(), false);
            case THUONG_HIEU -> catalog.datTrangThai(NhomDanhMucSanPhamThuongHieu.THUONG_HIEU, f.thuongHieu(), false);
        }
    }

    private boolean hoatDong(String bang, String cot, long ma) {
        return jdbc.sql("SELECT dang_hoat_dong FROM " + bang + " WHERE " + cot + " = ?").param(ma).query(Boolean.class).single();
    }

    private Fixture fixture() {
        String ma = UUID.randomUUID().toString();
        long danhMuc = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Serum', ?) RETURNING ma_danh_muc").param(ma).query(Long.class).single();
        long thuongHieu = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Thương hiệu QA', ?) RETURNING ma_thuong_hieu").param(ma).query(Long.class).single();
        long sanPham = jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
            VALUES (?, ?, 'Sản phẩm QA', ?) RETURNING ma_san_pham
            """).params(danhMuc, thuongHieu, ma).query(Long.class).single();
        long khach = jdbc.sql("INSERT INTO nguoi_dung (email, ho_ten) VALUES (?, 'Khách QA') RETURNING ma_nguoi_dung")
            .param(ma + "@example.invalid").query(Long.class).single();
        long diaChi = jdbc.sql("""
            INSERT INTO dia_chi_nguoi_dung (ma_nguoi_dung, ten_nguoi_nhan, so_dien_thoai, dia_chi_chi_tiet)
            VALUES (?, 'Người nhận QA', '0000000000', 'Địa chỉ QA') RETURNING ma_dia_chi
            """).param(khach).query(Long.class).single();
        return new Fixture(danhMuc, thuongHieu, sanPham, taoSku(sanPham), khach, diaChi);
    }

    private long taoSku(long sanPham) {
        return jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, '30 ml', 100000, 5) RETURNING ma_bien_the
            """).params(sanPham, UUID.randomUUID().toString()).query(Long.class).single();
    }

    private long taoDon(Fixture f, OrderStatus trangThai) {
        String phuongThuc = trangThai == OrderStatus.PENDING_PAYMENT || trangThai == OrderStatus.EXPIRED ? "MOMO" : "COD";
        long maDon = jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan,
                dia_chi_khi_dat, tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan)
            VALUES (?, ?, 'Người nhận QA', '0000000000', 'Địa chỉ QA', 100000, 125000, ?, ?) RETURNING ma_don_hang
            """).params(UUID.randomUUID().toString(), f.khach(), trangThai.name(), phuongThuc).query(Long.class).single();
        themDong(maDon, f.sku());
        if (phuongThuc.equals("COD")) {
            jdbc.sql("""
                INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai)
                VALUES (?, ?, 'COD', 125000, ?)
                """).params(maDon, "COD-" + maDon, trangThai == OrderStatus.DELIVERED ? "PAID" : "UNPAID").update();
        }
        return maDon;
    }

    private void themDong(long maDon, long sku) {
        jdbc.sql("""
            INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat,
                ten_bien_the_khi_dat, don_gia, so_luong)
            VALUES (?, ?, 'SNAPSHOT-SKU', 'Tên sản phẩm lúc đặt', '30 ml', 100000, 1)
            """).params(maDon, sku).update();
    }

    private record Fixture(long danhMuc, long thuongHieu, long sanPham, long sku, long khach, long diaChi) {
        long ma(PhamVi phamVi) {
            return switch (phamVi) {
                case BIEN_THE -> sku;
                case SAN_PHAM -> sanPham;
                case DANH_MUC -> danhMuc;
                case THUONG_HIEU -> thuongHieu;
            };
        }

        String skuList() { return "/admin/catalog/products/" + sanPham + "/variants"; }
        String skuPath() { return skuList() + "/" + sku; }
    }
}
