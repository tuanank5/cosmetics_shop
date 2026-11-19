package com.beautyshop.integration;

import com.beautyshop.entity.BienTheSanPham;
import com.beautyshop.repository.BienTheSanPham_Repository;
import com.beautyshop.service.TonKho_Service;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.uploads.sku-dir=target/p02-test-uploads")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class BienTheSanPham_MariaDb_Test {
    @Autowired JdbcClient jdbc;
    @Autowired TonKho_Service tonKho;
    @Autowired BienTheSanPham_Repository bienTheRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"COD", "MOMO"})
    void haiDonCanhTranhMotSkuVaRetryKhongCapLanHai(String phuongThuc) throws Exception {
        long maBienThe = taoBienThe(taoSanPham(), 1);
        long donA = taoDon(phuongThuc);
        long donB = taoDon(phuongThuc);
        var sanSang = new CountDownLatch(2);
        var batDau = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> canhTranh(phuongThuc, donA, maBienThe, sanSang, batDau));
            var b = executor.submit(() -> canhTranh(phuongThuc, donB, maBienThe, sanSang, batDau));
            assertThat(sanSang.await(10, TimeUnit.SECONDS)).isTrue();
            batDau.countDown();
            List<Boolean> ketQua = List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
            assertThat(ketQua).containsExactlyInAnyOrder(true, false);
            capTon(phuongThuc, ketQua.getFirst() ? donA : donB, maBienThe);
            assertThat(ton(maBienThe).getSoLuongTrongKho()).isEqualTo("COD".equals(phuongThuc) ? 0 : 1);
            assertThat(ton(maBienThe).getSoLuongKhaDung()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void checkoutDaDocSkuVanDungSoTonMoiNhatSauKhiKhoa() {
        long maBienThe = taoBienThe(taoSanPham(), 1);
        long donA = taoDon("COD");
        long donB = taoDon("COD");
        var executor = Executors.newSingleThreadExecutor();
        var checkout = new TransactionTemplate(transactionManager);
        checkout.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try {
            assertThatThrownBy(() -> checkout.executeWithoutResult(status -> {
                assertThat(ton(maBienThe).getSoLuongKhaDung()).isEqualTo(1);
                try {
                    executor.submit(() -> tonKho.deductCOD(donB, maBienThe, 1)).get(10, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new AssertionError("Giao dịch thứ hai chưa trừ được tồn.", exception);
                }
                tonKho.deductCOD(donA, maBienThe, 1);
            })).isInstanceOf(IllegalStateException.class).hasMessageContaining("tồn khả dụng");
        } finally {
            executor.shutdownNow();
        }
        assertThat(ton(maBienThe).getSoLuongTrongKho()).isZero();
    }

    @Test
    void giuChotVaTraHangCoIdempotencyVaKhongTuGiaiPhongKhiHetHan() {
        long maBienThe = taoBienThe(taoSanPham(), 3);
        long donChot = taoDon("MOMO");
        long donTra = taoDon("MOMO");
        long donCod = taoDon("COD");
        tonKho.reserveAtomically(donChot, maBienThe, 2);
        tonKho.reserveAtomically(donChot, maBienThe, 2);
        tonKho.reserveAtomically(donTra, maBienThe, 1);
        assertThat(ton(maBienThe).getSoLuongTamGiu()).isEqualTo(3);
        assertThatThrownBy(() -> tonKho.deductCOD(donCod, maBienThe, 1)).isInstanceOf(IllegalStateException.class);
        jdbc.sql("UPDATE tam_giu_ton_kho SET het_han_luc = UTC_TIMESTAMP() - INTERVAL 1 MINUTE WHERE ma_don_hang = ?")
            .param(donChot).update();
        assertThat(ton(maBienThe).getSoLuongKhaDung()).isZero();

        tonKho.consumeReservation(donChot, maBienThe);
        tonKho.consumeReservation(donChot, maBienThe);
        tonKho.releaseReservation(donTra, maBienThe);
        tonKho.releaseReservation(donTra, maBienThe);
        assertThatThrownBy(() -> tonKho.consumeReservation(donTra, maBienThe)).isInstanceOf(IllegalStateException.class);
        assertThat(ton(maBienThe).getSoLuongTrongKho()).isEqualTo(1);
        assertThat(ton(maBienThe).getSoLuongTamGiu()).isZero();
    }

    @Test
    void rollbackCheckoutHoanCaTonVaDauGhiNhan() {
        long maBienThe = taoBienThe(taoSanPham(), 1);
        long maDonHang = taoDon("COD");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            tonKho.deductCOD(maDonHang, maBienThe, 1);
            status.setRollbackOnly();
        });
        assertThat(ton(maBienThe).getSoLuongTrongKho()).isEqualTo(1);
        tonKho.deductCOD(maDonHang, maBienThe, 1);
        assertThat(ton(maBienThe).getSoLuongTrongKho()).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crudSkuVaAnhKiemTraQuyenSoHuuBienTheVaTonDangGiu() throws Exception {
        long maSanPham = taoSanPham();
        String base = "/admin/catalog/products/" + maSanPham + "/variants";
        String sku = UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT);
        byte[] png;
        try (var bytes = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
            png = bytes.toByteArray();
        }
        var anh = new MockMultipartFile("hinhAnh", "sku.png", "image/png", png);
        var sai = new MockMultipartFile("hinhAnh", "sku.png", "image/png", new byte[]{1, 2, 3});

        mvc.perform(get(base + "/new")).andExpect(status().isOk());
        mvc.perform(form(base + "/new", sku, 2, sai).with(csrf()))
            .andExpect(model().attributeHasFieldErrors("form", "hinhAnh"));
        mvc.perform(form(base + "/new", sku, 2, anh)).andExpect(status().isForbidden());
        mvc.perform(form(base + "/new", sku, 2, anh).with(csrf())).andExpect(redirectedUrl(base));
        String skuKhac = sku + "-B";
        mvc.perform(form(base + "/new", skuKhac, 7).with(request -> {
            request.setParameter("giaBan", "200000");
            return request;
        }).with(csrf()))
            .andExpect(redirectedUrl(base));
        var danhSach = bienTheRepository.findAllBySanPhamMaSanPhamOrderBySkuAsc(maSanPham);
        assertThat(danhSach).hasSize(2);
        assertThat(danhSach.getLast().getGiaBan()).isEqualByComparingTo("200000");
        assertThat(danhSach.getLast().getSoLuongTrongKho()).isEqualTo(7);
        var bienThe = danhSach.getFirst();
        long maBienThe = bienThe.getMaBienThe();
        String urlAnh = bienThe.getUrlHinhAnh();
        mvc.perform(get(urlAnh)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("image/png"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/uploads/sku/unknown.png")).andExpect(status().isNotFound());
        mvc.perform(get(base)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Lô minh họa: LOT-DEMO")));
        mvc.perform(form(base + "/new", sku, 2).with(csrf()))
            .andExpect(model().attributeHasFieldErrors("form", "sku"));
        mvc.perform(get("/admin/catalog/products/" + taoSanPham() + "/variants/" + maBienThe + "/edit"))
            .andExpect(status().isNotFound());

        tonKho.reserveAtomically(taoDon("MOMO"), maBienThe, 1);
        String edit = base + "/" + maBienThe + "/edit";
        mvc.perform(form(edit, sku, 0).with(csrf()))
            .andExpect(model().attributeHasFieldErrors("form", "soLuongTrongKho"));
        mvc.perform(form(edit, sku, 4).with(csrf())).andExpect(redirectedUrl(base));
        assertThat(ton(maBienThe).getSoLuongKhaDung()).isEqualTo(3);
        assertThat(ton(maBienThe).getUrlHinhAnh()).isEqualTo(urlAnh);
        mvc.perform(form(edit, sku, 4).param("xoaHinhAnh", "true").with(csrf())).andExpect(redirectedUrl(base));
        assertThat(ton(maBienThe).getUrlHinhAnh()).isNull();
        mvc.perform(get(urlAnh)).andExpect(status().isNotFound());
        mvc.perform(post(base + "/" + maBienThe + "/delete").with(csrf())).andExpect(redirectedUrl(base));
        assertThat(ton(maBienThe).isDangHoatDong()).isFalse();
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerKhongDuocQuanTriSku() throws Exception {
        mvc.perform(get("/admin/catalog/products/1/variants")).andExpect(status().isForbidden());
    }

    private MockMultipartHttpServletRequestBuilder form(String path, String sku, int stock, MockMultipartFile... anh) {
        var builder = multipart(path);
        for (var file : anh) {
            builder.file(file);
        }
        builder.param("sku", sku).param("tenBienThe", "30 ml").param("giaBan", "150000")
            .param("soLuongTrongKho", Integer.toString(stock)).param("dangHoatDong", "true")
            .param("maLoMinhHoa", "LOT-DEMO").param("hanSuDungMinhHoa", "2027-01-02");
        return builder;
    }

    private boolean canhTranh(String phuongThuc, long maDonHang, long maBienThe,
            CountDownLatch sanSang, CountDownLatch batDau)
            throws InterruptedException {
        sanSang.countDown();
        if (!batDau.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Không khởi động được hai transaction.");
        }
        try {
            capTon(phuongThuc, maDonHang, maBienThe);
            return true;
        } catch (IllegalStateException exception) {
            assertThat(exception.getMessage()).contains("tồn khả dụng");
            return false;
        }
    }

    private void capTon(String phuongThuc, long maDonHang, long maBienThe) {
        if ("COD".equals(phuongThuc)) {
            tonKho.deductCOD(maDonHang, maBienThe, 1);
        } else {
            tonKho.reserveAtomically(maDonHang, maBienThe, 1);
        }
    }

    private BienTheSanPham ton(long maBienThe) {
        return bienTheRepository.findById(maBienThe).orElseThrow();
    }

    private long taoBienThe(long maSanPham, int ton) {
        return jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, 'SKU kiểm thử', 100000, ?) RETURNING ma_bien_the
            """).params(maSanPham, UUID.randomUUID().toString(), ton).query(Long.class).single();
    }

    private long taoSanPham() {
        long danhMuc = jdbc.sql("""
            INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Danh mục kiểm thử', ?) RETURNING ma_danh_muc
            """).param(UUID.randomUUID().toString()).query(Long.class).single();
        long thuongHieu = jdbc.sql("""
            INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Thương hiệu kiểm thử', ?) RETURNING ma_thuong_hieu
            """).param(UUID.randomUUID().toString()).query(Long.class).single();
        return jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
            VALUES (?, ?, 'Sản phẩm kiểm thử', ?) RETURNING ma_san_pham
            """).params(danhMuc, thuongHieu, UUID.randomUUID().toString()).query(Long.class).single();
    }

    private long taoDon(String phuongThuc) {
        long khachHang = jdbc.sql("""
            INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, kich_hoat)
            VALUES (?, '', 'Khách kiểm thử', FALSE) RETURNING ma_nguoi_dung
            """).param(UUID.randomUUID() + "@example.invalid").query(Long.class).single();
        String trangThai = "COD".equals(phuongThuc) ? "PENDING_CONFIRMATION" : "PENDING_PAYMENT";
        return jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan,
                sdt_nguoi_nhan, dia_chi_khi_dat, tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan)
            VALUES (?, ?, 'Khách kiểm thử', '0000000000', 'Địa chỉ kiểm thử', 100000, 100000, ?, ?)
            RETURNING ma_don_hang
            """).params(UUID.randomUUID().toString(), khachHang, trangThai, phuongThuc).query(Long.class).single();
    }
}
