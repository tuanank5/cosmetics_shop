package com.beautyshop.integration;

import com.beautyshop.repository.ThuongHieu_Repository;
import com.beautyshop.repository.DanhMucSanPhamThuongHieu_Repository;
import com.beautyshop.repository.DanhMuc_Repository;
import com.beautyshop.repository.SanPham_Repository;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
@WithMockUser(roles = "ADMIN")
class DanhMucSanPhamThuongHieu_MariaDb_Test {
    private static final String ADMIN = "/admin/catalog/";
    @Autowired MockMvc mvc;
    @Autowired DanhMuc_Repository categories;
    @Autowired ThuongHieu_Repository brands;
    @Autowired SanPham_Repository products;
    @Autowired JdbcClient jdbc;

    @Test
    void catalogCrudUsesV1ConstraintsAndPreservesProducts() throws Exception {
        String suffix = UUID.randomUUID().toString();
        String categorySlug = "category-" + suffix;
        String brandSlug = "brand-" + suffix;
        String productSlug = "product-" + suffix;
        mvc.perform(post(ADMIN + "categories/new").with(csrf())
                .param("name", "Chăm sóc da").param("slug", categorySlug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "categories"));
        mvc.perform(post(ADMIN + "brands/new").with(csrf())
                .param("name", "Thương hiệu kiểm thử").param("slug", brandSlug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "brands"));
        long category = idFor(categories, categorySlug);
        long brand = idFor(brands, brandSlug);

        mvc.perform(post(ADMIN + "categories/" + category + "/edit").with(csrf())
                .param("name", "Danh mục đã cập nhật").param("slug", categorySlug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "categories"));
        mvc.perform(post(ADMIN + "brands/" + brand + "/edit").with(csrf())
                .param("name", "Thương hiệu đã cập nhật").param("slug", brandSlug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "brands"));
        assertThat(categories.findById(category).orElseThrow().getTen()).isEqualTo("Danh mục đã cập nhật");
        assertThat(brands.findById(brand).orElseThrow().getTen()).isEqualTo("Thương hiệu đã cập nhật");

        mvc.perform(post(ADMIN + "products/new").with(csrf())
                .param("name", "Sản phẩm kiểm thử").param("slug", productSlug))
            .andExpect(model().attributeHasFieldErrors("form", "categoryId", "brandId"));
        mvc.perform(post(ADMIN + "products/new").with(csrf())
                .param("name", "Sản phẩm kiểm thử").param("slug", productSlug)
                .param("categoryId", Long.toString(category)).param("brandId", Long.toString(brand))
                .param("ingredients", "Thành phần kiểm thử").param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "products"));
        long product = idFor(products, productSlug);
        assertThat(products.findById(product).orElseThrow().getThanhPhan()).isEqualTo("Thành phần kiểm thử");

        mvc.perform(get(ADMIN + "products"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(productSlug)));
        mvc.perform(get(ADMIN + "products/" + product + "/edit"))
            .andExpect(status().isOk());
        mvc.perform(post(ADMIN + "products/" + product + "/edit").with(csrf())
                .param("name", "Tên đã cập nhật").param("slug", productSlug)
                .param("categoryId", Long.toString(category)).param("brandId", Long.toString(brand))
                .param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + "products"));
        assertThat(products.findById(product).orElseThrow().getTen()).isEqualTo("Tên đã cập nhật");

        mvc.perform(post(ADMIN + "categories/new").with(csrf())
                .param("name", "Tên trùng đường dẫn").param("slug", categorySlug))
            .andExpect(model().attributeHasErrors("form"));
        mvc.perform(post(ADMIN + "categories/" + category + "/delete").with(csrf()))
            .andExpect(status().isNotFound());
        mvc.perform(post(ADMIN + "brands/" + brand + "/delete").with(csrf()))
            .andExpect(status().isNotFound());
        assertThat(categories.existsById(category)).isTrue();
        assertThat(brands.existsById(brand)).isTrue();

        mvc.perform(post(ADMIN + "products/" + product + "/delete").with(csrf()))
            .andExpect(redirectedUrl(ADMIN + "products"));
        assertThat(products.findById(product).orElseThrow().isDangHoatDong()).isFalse();
        datTrangThai("products", product, true);
        assertThat(products.findById(product).orElseThrow().isDangHoatDong()).isTrue();
    }

    @Test
    void mucChuaCoSanPhamVanGiuBanGhiVaGanTrangThaiLap() throws Exception {
        String slug = "unused-" + UUID.randomUUID();
        for (String section : java.util.List.of("categories", "brands")) {
            var repo = section.equals("categories") ? categories : brands;
            long id = taoCha(section, repo, slug);
            mvc.perform(post(ADMIN + section + "/" + id + "/delete").with(csrf())).andExpect(status().isNotFound());
            datTrangThai(section, id, false);
            datTrangThai(section, id, false);
            var muc = repo.findById(id).orElseThrow();
            assertThat(muc.isDangHoatDong()).isFalse();
            assertThat(muc.getDuongDan()).isEqualTo(slug);
            mvc.perform(get(ADMIN + section)).andExpect(status().isOk())
                .andExpect(content().string(containsString(ADMIN + section + "/" + id + "/status")))
                .andExpect(content().string(containsString("Kích hoạt lại")))
                .andExpect(content().string(not(containsString(ADMIN + section + "/" + id + "/delete"))));
            datTrangThai(section, id, true);
            datTrangThai(section, id, true);
            assertThat(repo.findById(id).orElseThrow().isDangHoatDong()).isTrue();
            mvc.perform(post(ADMIN + section + "/" + Long.MAX_VALUE + "/status").with(csrf()).param("active", "false"))
                .andExpect(status().isNotFound());
        }
    }

    @Test
    void trangThaiChaChiDoiVisibilityVaGiuSkuFkCungSnapshotDon() throws Exception {
        String slug = "lifecycle-" + UUID.randomUUID();
        long danhMuc = taoCha("categories", categories, slug);
        long thuongHieu = taoCha("brands", brands, slug);
        long sanPham = taoSanPham(danhMuc, thuongHieu, slug, true);
        long sanPhamTat = taoSanPham(danhMuc, thuongHieu, slug + "-tat", false);
        String sku = "ACTIVE-" + slug;
        String skuTat = "INACTIVE-" + slug;
        long bienThe = taoSku(sanPham, sku, true);
        long bienTheTat = taoSku(sanPham, skuTat, false);
        taoSku(sanPhamTat, "PARENT-OFF-" + slug, true);
        long don = taoDonFixture(bienThe, slug);

        kiemCatalog(slug, 1);
        for (String section : java.util.List.of("categories", "brands")) {
            long id = section.equals("categories") ? danhMuc : thuongHieu;
            datTrangThai(section, id, false);
            datTrangThai(section, id, false);
            kiemCatalog(slug, 0);
            mvc.perform(get("/products/" + slug)).andExpect(status().isNotFound());
            mvc.perform(get("/admin/orders/" + don)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Tên sản phẩm lúc đặt")))
                .andExpect(content().string(containsString("Mã biến thể sản phẩm: SNAPSHOT")));
            datTrangThai(section, id, true);
            kiemCatalog(slug, 1);
            mvc.perform(get("/products/" + slug)).andExpect(status().isOk())
                .andExpect(content().string(containsString(sku)))
                .andExpect(content().string(not(containsString(skuTat))));
            mvc.perform(get("/products/" + slug + "-tat")).andExpect(status().isNotFound());
        }
        assertThat(jdbc.sql("SELECT ma_danh_muc FROM san_pham WHERE ma_san_pham = ?")
            .param(sanPham).query(Long.class).single()).isEqualTo(danhMuc);
        assertThat(jdbc.sql("SELECT ma_thuong_hieu FROM san_pham WHERE ma_san_pham = ?")
            .param(sanPham).query(Long.class).single()).isEqualTo(thuongHieu);
        assertThat(jdbc.sql("SELECT dang_hoat_dong FROM bien_the_san_pham WHERE ma_bien_the = ?")
            .param(bienTheTat).query(Boolean.class).single()).isFalse();
        assertThat(jdbc.sql("SELECT ma_bien_the FROM chi_tiet_don_hang WHERE ma_don_hang = ?")
            .param(don).query(Long.class).single()).isEqualTo(bienThe);
    }

    private long taoCha(String section, DanhMucSanPhamThuongHieu_Repository<?> repo, String slug) throws Exception {
        mvc.perform(post(ADMIN + section + "/new").with(csrf())
                .param("name", "Nhóm kiểm thử").param("slug", slug).param("active", "true"))
            .andExpect(redirectedUrl(ADMIN + section));
        return idFor(repo, slug);
    }

    private void datTrangThai(String section, long id, boolean active) throws Exception {
        mvc.perform(post(ADMIN + section + "/" + id + "/status").with(csrf()).param("active", Boolean.toString(active)))
            .andExpect(redirectedUrl(ADMIN + section)).andExpect(flash().attributeExists("message"));
    }

    private void kiemCatalog(String slug, long tong) throws Exception {
        mvc.perform(get("/products").param("category", slug)).andExpect(status().isOk())
            .andExpect(model().attribute("ketQua", hasProperty("totalElements", is(tong))));
    }

    private long taoSanPham(long danhMuc, long thuongHieu, String slug, boolean active) {
        return jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan, dang_hoat_dong)
            VALUES (?, ?, 'Sản phẩm fixture', ?, ?) RETURNING ma_san_pham
            """).params(danhMuc, thuongHieu, slug, active).query(Long.class).single();
    }

    private long taoSku(long sanPham, String sku, boolean active) {
        return jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho, dang_hoat_dong)
            VALUES (?, ?, '30 ml', 100000, 5, ?) RETURNING ma_bien_the
            """).params(sanPham, sku, active).query(Long.class).single();
    }

    private long taoDonFixture(long bienThe, String slug) {
        long chuDon = jdbc.sql("""
            INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten)
            VALUES (?, NULL, 'Fixture') RETURNING ma_nguoi_dung
            """).param(slug + "@example.invalid").query(Long.class).single();
        long don = jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_khi_dat,
                tam_tinh, phi_van_chuyen, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan)
            VALUES (?, ?, 'Fixture', '0000000000', 'Fixture', 100000, 25000, 125000, 'PENDING_CONFIRMATION', 'COD')
            RETURNING ma_don_hang
            """).params(slug, chuDon).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat,
                don_gia, so_luong) VALUES (?, ?, 'SNAPSHOT', 'Tên sản phẩm lúc đặt', '30 ml', 100000, 1)
            """).params(don, bienThe).update();
        return don;
    }

    private long idFor(DanhMucSanPhamThuongHieu_Repository<?> repository, String slug) {
        return repository.findAll().stream()
            .filter(item -> item.getDuongDan().equals(slug))
            .findFirst().orElseThrow().getMa();
    }
}
