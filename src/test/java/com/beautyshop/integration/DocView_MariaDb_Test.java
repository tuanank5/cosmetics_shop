package com.beautyshop.integration;

import com.beautyshop.cart.GioKhach;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class DocView_MariaDb_Test {
    private static final ThreadLocal<List<String>> CAU_LENH = new ThreadLocal<>();

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired PasswordEncoder encoder;

    @Test
    void gioKhachDocNhieuSkuTrongMotTruyVan() throws Exception {
        long danhMuc = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Fixture Cart', 'fixture-cart') RETURNING ma_danh_muc")
            .query(Long.class).single();
        long thuongHieu = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Fixture Cart', 'fixture-cart') RETURNING ma_thuong_hieu")
            .query(Long.class).single();
        long sanPham = taoSanPham(danhMuc, thuongHieu, 77);
        var gio = GioKhach.doc(null);
        var phien = new MockHttpSession();
        var luotDoc = new ArrayList<Long>();
        for (int i = 1; i <= 7; i++) {
            long sku = taoSku(sanPham, 70 + i);
            gio = gio.them(sku, 2);
            if (i == 7) {
                jdbc.sql("UPDATE bien_the_san_pham SET dang_hoat_dong = FALSE WHERE ma_bien_the = ?").param(sku).update();
            }
            if (i == 1 || i == 7) {
                gio.luu(phien);
                long truoc = soSelectMariaDb();
                var trang = doc("/cart", phien);
                long selects = soSelectMariaDb() - truoc;
                luotDoc.add(selects);
                System.out.println("[BIZ-UX-01] guest-cart sku=" + i + " SELECT=" + selects);
                assertThat(trang.html()).contains("DTO-SKU-71", "200.000 ₫");
                if (i == 7) {
                    assertThat(trang.html()).contains("DTO-SKU-77", "Biến thể hiện không còn hàng.", "1.400.000 ₫");
                }
            }
        }
        assertThat(luotDoc).containsExactly(1L, 1L);
    }

    private long soSelectMariaDb() {
        return jdbc.sql("SHOW GLOBAL STATUS LIKE 'Com_select'").query((rs, row) -> rs.getLong(2)).single();
    }

    @Test
    void doCotSoTruyVanVaHtmlCuaNhungDuongDoc() throws Exception {
        String matKhau = UUID.randomUUID().toString();
        String bam = encoder.encode(matKhau);
        long khach = taiKhoan("buyer-dto@example.invalid", bam, "CUSTOMER");
        long admin = taiKhoan("admin-dto@example.invalid", bam, "ADMIN");
        String subject = "fixture-subject-" + UUID.randomUUID();
        jdbc.sql("INSERT INTO danh_tinh_oauth (ma_nguoi_dung,nha_cung_cap,ma_dinh_danh_nha_cung_cap) VALUES (?, 'github', ?)")
            .params(khach, subject).update();
        long danhMuc = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc,duong_dan) VALUES ('DTO Active','dto-active') RETURNING ma_danh_muc")
            .query(Long.class).single();
        jdbc.sql("INSERT INTO danh_muc (ten_danh_muc,duong_dan,dang_hoat_dong) VALUES ('DTO Category','dto-category',FALSE)").update();
        long thuongHieu = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu,duong_dan) VALUES ('DTO Brand','dto-brand') RETURNING ma_thuong_hieu")
            .query(Long.class).single();
        long sanPham = taoSanPham(danhMuc, thuongHieu, 1);
        long sku = taoSku(sanPham, 1);
        var phienKhach = dangNhap("buyer-dto@example.invalid", matKhau);
        var phienAdmin = dangNhap("admin-dto@example.invalid", matKhau);
        jdbc.sql("INSERT IGNORE INTO gio_hang (ma_nguoi_dung) VALUES (?)").param(khach).update();
        jdbc.sql("INSERT INTO chi_tiet_gio_hang (ma_gio_hang, ma_bien_the, so_luong) SELECT ma_gio_hang, ?, 1 FROM gio_hang WHERE ma_nguoi_dung = ?")
            .params(sku, khach).update();
        jdbc.sql("INSERT INTO dia_chi_nguoi_dung (ma_nguoi_dung, ten_nguoi_nhan, so_dien_thoai, dia_chi_chi_tiet, la_mac_dinh) VALUES (?, 'Fixture', '000', 'Other owner address', TRUE)")
            .param(admin).update();

        doBaoMat("customer-security", "/account/security", phienKhach, bam, subject);
        doBaoMat("admin-security", "/admin/users/" + khach, phienAdmin, bam, subject);
        for (int soLuong : List.of(1, 7)) {
            int themTu = soLuong == 1 ? 1 : 2;
            for (int i = themTu; i <= soLuong; i++) {
                jdbc.sql("""
                    INSERT INTO dia_chi_nguoi_dung (ma_nguoi_dung, ten_nguoi_nhan, so_dien_thoai, dia_chi_chi_tiet, la_mac_dinh)
                    VALUES (?, 'Fixture', '000', ?, ?)
                    """).params(khach, "Fixture address " + i, i == 1).update();
                if (i > 1) {
                    taoSanPham(danhMuc, thuongHieu, i);
                    taoSku(sanPham, i);
                }
            }
            var diaChi = doc("/account/addresses?from=checkout", phienKhach);
            assertThat(diaChi.html()).contains("Fixture address " + soLuong, "Quay lại thanh toán").doesNotContain("Other owner address");
            baoCao("address-" + soLuong, diaChi, 2, "dia_chi_nguoi_dung");
            var checkout = doc("/checkout", phienKhach);
            assertThat(checkout.html()).contains("Fixture address " + soLuong, "name=\"addressId\"").doesNotContain("Other owner address");
            baoCao("checkout-" + soLuong, checkout, 2, "dia_chi_nguoi_dung");
            var catalog = doc("/admin/catalog/products?category=" + danhMuc, phienAdmin);
            assertThat(catalog.html()).contains("DTO Product " + soLuong).doesNotContain("Fixture long description");
            kiemTruyVanCatalog("catalog-" + soLuong, catalog, false);
            var pdp = doc("/products/dto-product-1?variant=" + sku, null);
            assertThat(pdp.html()).contains("DTO Product 1", "DTO SKU 1", "DTO SKU " + soLuong);
            baoCao("pdp-" + soLuong, pdp, 2, "san_pham", "bien_the_san_pham");
            var skuAdmin = doc("/admin/catalog/products/" + sanPham + "/variants", phienAdmin);
            assertThat(skuAdmin.html()).contains(">Trong kho</th>", ">Đang giữ</th>", "DTO SKU " + soLuong);
            baoCao("sku-admin-" + soLuong, skuAdmin, 3, "san_pham", "bien_the_san_pham");
        }
        for (int i = 8; i <= 16; i++) {
            taoSanPham(danhMuc, thuongHieu, i);
        }
        var trangDay = doc("/admin/catalog/products?category=" + danhMuc, phienAdmin);
        kiemTruyVanCatalog("catalog-16", trangDay, true);
        var form = doc("/admin/catalog/products/" + sanPham + "/edit", phienAdmin);
        assertThat(form.html()).contains("DTO Category (ngừng hoạt động)", "Fixture long description");
        baoCao("catalog-edit-options", form, 6, "danh_muc", "thuong_hieu");
    }

    private void kiemTruyVanCatalog(String ten, Trang trang, boolean canDem) throws Exception {
        var sql = trang.sql();
        assertThat(sql).filteredOn(s -> s.contains(" from nguoi_dung ")).hasSize(1);
        assertThat(sql).filteredOn(s -> s.contains(" from danh_muc ")).hasSize(1);
        assertThat(sql).filteredOn(s -> s.contains(" from thuong_hieu ")).hasSize(1);
        assertThat(sql).filteredOn(s -> s.contains(" from san_pham ") && !s.startsWith("select count(")).hasSize(1);
        assertThat(sql).filteredOn(s -> s.contains(" from san_pham ") && s.startsWith("select count("))
            .hasSize(canDem ? 1 : 0);
        baoCao(ten, trang, canDem ? 5 : 4, "san_pham");
    }

    private void doBaoMat(String ten, String url, MockHttpSession phien, String bam, String subject) throws Exception {
        var ketQua = doc(url, phien);
        assertThat(ketQua.html()).contains("buyer-dto@example.invalid").doesNotContain(bam, subject);
        baoCao(ten, ketQua, 3, "nguoi_dung", "danh_tinh_oauth");
        if (!Boolean.getBoolean("dto01.baseline")) {
            assertThat(ketQua.sql()).noneSatisfy(sql -> assertThat(sql).contains("ma_dinh_danh_nha_cung_cap"));
            assertThat(ketQua.sql()).noneSatisfy(sql -> assertThat(sql).contains(".mat_khau_bam,"));
        }
    }

    private Trang doc(String url, MockHttpSession phien) throws Exception {
        var sql = new ArrayList<String>();
        CAU_LENH.set(sql);
        try {
            var yeuCau = get(java.net.URI.create(url));
            if (phien != null) yeuCau.session(phien);
            var ketQua = mvc.perform(yeuCau).andExpect(status().isOk()).andReturn();
            return new Trang(ketQua.getResponse().getContentAsString(), sql);
        } finally {
            CAU_LENH.remove();
        }
    }

    private void baoCao(String ten, Trang trang, int toiDa, String... bang) throws Exception {
        assertThat(trang.sql()).hasSizeLessThanOrEqualTo(toiDa);
        List<String> cot = new ArrayList<>();
        for (String bangDoc : bang) {
            var docBang = trang.sql().stream().filter(sql -> sql.contains(" from " + bangDoc + " ")).toList();
            assertThat(docBang).isNotEmpty();
            for (String sql : docBang) {
                String select = sql.substring(7, sql.indexOf(" from "));
                cot.add(bangDoc + "=" + select.split(",").length);
                if (!Boolean.getBoolean("dto01.baseline") && ten.startsWith("catalog-") && bangDoc.equals("san_pham")) {
                    assertThat(select).doesNotContain("mo_ta", "thanh_phan", "huong_dan_su_dung");
                }
            }
        }
        String html = trang.html().replaceAll("(name=\"_csrf\" value=\")[^\"]*", "$1csrf")
            .replace("\r\n", "\n");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(html.getBytes(StandardCharsets.UTF_8)));
        System.out.println("[DTO-01] " + ten + " selects=" + trang.sql().size() + " fields=" + cot + " html=" + hash);
    }

    private long taoSku(long sanPham, int so) {
        return jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham,sku,ten_bien_the,gia_ban,so_luong_trong_kho)
            VALUES (?, ?, ?, 100000, 8) RETURNING ma_bien_the
            """).params(sanPham, "DTO-SKU-" + so, "DTO SKU " + so).query(Long.class).single();
    }

    private long taiKhoan(String email, String bam, String vaiTro) {
        return jdbc.sql("""
            INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro, ngay_xac_minh_email)
            VALUES (?, ?, 'Fixture', ?, UTC_TIMESTAMP(6)) RETURNING ma_nguoi_dung
            """).params(email, bam, vaiTro).query(Long.class).single();
    }

    private long taoSanPham(long danhMuc, long thuongHieu, int so) {
        return jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan, mo_ta)
            VALUES (?, ?, ?, ?, 'Fixture long description') RETURNING ma_san_pham
            """).params(danhMuc, thuongHieu, "DTO Product " + so, "dto-product-" + so).query(Long.class).single();
    }

    private MockHttpSession dangNhap(String email, String matKhau) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").with(csrf()).param("username", email).param("password", matKhau))
            .andExpect(status().is3xxRedirection()).andReturn().getRequest().getSession(false);
    }

    private record Trang(String html, List<String> sql) {}

    @TestConfiguration
    static class GhiSql {
        @Bean
        HibernatePropertiesCustomizer dto01Sql() {
            return properties -> properties.put("hibernate.session_factory.statement_inspector", (StatementInspector) sql -> {
                var cauLenh = CAU_LENH.get();
                if (cauLenh != null && sql.startsWith("select ")) cauLenh.add(sql.toLowerCase(Locale.ROOT));
                return sql;
            });
        }
    }
}
