package com.beautyshop.integration;

import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class CuaHang_MariaDb_Test {
    @Autowired JdbcClient jdbc;
    @Autowired MockMvc mvc;

    @Test
    void locTiengVietSapXepTheoSkuVaPhanTrang() throws Exception {
        String hauTo = UUID.randomUUID().toString();
        String brand = "brand-" + hauTo;
        String category = "category-" + hauTo;
        long maThuongHieu = thuongHieu(brand);
        long maDanhMuc = danhMuc(category);
        long re = sanPham(maThuongHieu, maDanhMuc, "Dưỡng ẩm nhẹ", "light-" + hauTo, "Da dầu", true, 1);
        long dat = sanPham(maThuongHieu, maDanhMuc, "Dưỡng ẩm sâu", "rich-" + hauTo, "Da dầu, da hỗn hợp", true, 2);
        long daKho = sanPham(maThuongHieu, maDanhMuc, "Dưỡng ẩm da khô", "dry-" + hauTo, "Da khô", true, 3);
        long an = sanPham(maThuongHieu, maDanhMuc, "Dưỡng ẩm ẩn", "hidden-" + hauTo, "Da dầu", false, 4);
        long khac = sanPham(thuongHieu("other-" + hauTo), maDanhMuc, "Dưỡng ẩm khác", "other-" + hauTo, "Da dầu", true, 5);
        long saiDanhMuc = sanPham(maThuongHieu, danhMuc("other-" + hauTo), "Dưỡng ẩm khác mục",
            "other-cat-" + hauTo, "Da dầu", true, 5);
        bienThe(re, "LIGHT-" + hauTo, 80000, 3, 0, true, null);
        bienThe(re, "EXTRA-" + hauTo, 125000, 3, 0, true, null);
        bienThe(re, "INACTIVE-" + hauTo, 1000, 3, 0, false, null);
        bienThe(dat, "RICH-" + hauTo, 150000, 2, 2, true, null);
        bienThe(daKho, "DRY-" + hauTo, 10, 2, 0, true, null);
        bienThe(an, "HIDDEN-" + hauTo, 10, 2, 0, true, null);
        bienThe(khac, "OTHER-" + hauTo, 10, 2, 0, true, null);
        bienThe(saiDanhMuc, "OTHER-CAT-" + hauTo, 10, 2, 0, true, null);
        sanPham(maThuongHieu, maDanhMuc, "Dưỡng ẩm chưa có SKU", "empty-" + hauTo, "Da dầu", true, 6);

        var trangDau = ketQua(mvc.perform(get("/products").param("q", "DƯỠNG ẨM")
                .param("brand", brand).param("category", category).param("skinType", "DA DẦU")
                .param("sort", "priceAsc").param("size", "1").param("page", "0"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("80.000")))
            .andExpect(content().string(containsString("Trang sau"))).andReturn());
        assertThat(trangDau.getTotalElements()).isEqualTo(2);
        assertThat(trangDau.getContent().getFirst().getDuongDan()).isEqualTo("light-" + hauTo);
        assertThat(trangDau.getContent().getFirst().getGiaTu()).isEqualByComparingTo("80000");

        var trangHai = ketQua(mvc.perform(get("/products").param("q", "DƯỠNG ẨM")
                .param("brand", brand).param("category", category).param("skinType", "da dầu")
                .param("sort", "priceAsc").param("size", "1").param("page", "1"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Hết hàng"))).andReturn());
        assertThat(trangHai.getContent().getFirst().getDuongDan()).isEqualTo("rich-" + hauTo);

        for (String sort : new String[]{"priceDesc", "newest"}) {
            var trang = ketQua(mvc.perform(get("/products").param("brand", brand)
                    .param("category", category).param("skinType", "da dầu").param("sort", sort))
                .andExpect(status().isOk()).andReturn());
            assertThat(trang.getContent()).extracting(TheSanPham::getDuongDan)
                .containsExactly("rich-" + hauTo, "light-" + hauTo);
        }

        var theoSku = ketQua(mvc.perform(get("/products").param("q", "EXTRA-" + hauTo))
            .andExpect(status().isOk()).andReturn());
        assertThat(theoSku.getContent().getFirst().getGiaTu()).isEqualByComparingTo("125000");
        mvc.perform(get("/products").param("q", "%").param("brand", brand))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Không tìm thấy sản phẩm phù hợp")));
    }

    @Test
    void chonSkuDocGiaTonVaAnhHienHanh() throws Exception {
        String hauTo = UUID.randomUUID().toString();
        long maThuongHieu = thuongHieu("brand-" + hauTo);
        long maDanhMuc = danhMuc("category-" + hauTo);
        String slug = "detail-" + hauTo;
        long maSanPham = sanPham(maThuongHieu, maDanhMuc, "Tinh chất dịu nhẹ", slug, "Da nhạy cảm", true, 1);
        long het = bienThe(maSanPham, "HET-" + hauTo, 90000, 2, 2, true, null);
        long con = bienThe(maSanPham, "CON-" + hauTo, 180000, 5, 1, true, "/uploads/sku/test.png");
        long an = bienThe(maSanPham, "AN-" + hauTo, 100, 5, 0, false, null);
        long sanPhamKhac = sanPham(maThuongHieu, maDanhMuc, "Khác", "other-" + hauTo, null, true, 1);
        long bienTheKhac = bienThe(sanPhamKhac, "OTHER-" + hauTo, 100, 1, 0, true, null);
        String url = "/products/" + slug;
        mvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(content().string(containsString("180.000")))
            .andExpect(content().string(containsString("Còn 4 sản phẩm")))
            .andExpect(content().string(containsString("/uploads/sku/test.png")))
            .andExpect(content().string(containsString("Da nhạy cảm")))
            .andExpect(content().string(not(containsString("AN-" + hauTo))));

        mvc.perform(get(url).param("variant", Long.toString(het))).andExpect(status().isOk())
            .andExpect(content().string(containsString("90.000")))
            .andExpect(content().string(containsString("Hết hàng")))
            .andExpect(content().string(containsString("store-placeholder")));

        jdbc.sql("UPDATE bien_the_san_pham SET gia_ban = 200000 WHERE ma_bien_the = ?").param(con).update();
        mvc.perform(get(url).param("variant", Long.toString(con))).andExpect(status().isOk())
            .andExpect(content().string(containsString("200.000")));
        for (long khongHopLe : new long[]{an, bienTheKhac}) {
            mvc.perform(get(url).param("variant", Long.toString(khongHopLe))).andExpect(status().isNotFound());
        }
        mvc.perform(get("/products/not-found-" + hauTo)).andExpect(status().isNotFound());
        jdbc.sql("UPDATE thuong_hieu SET dang_hoat_dong = FALSE WHERE ma_thuong_hieu = ?").param(maThuongHieu).update();
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @SuppressWarnings("unchecked")
    private Page<TheSanPham> ketQua(MvcResult response) {
        return (Page<TheSanPham>) response.getModelAndView().getModel().get("ketQua");
    }

    private long thuongHieu(String slug) {
        return jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Thương hiệu thử', ?) RETURNING ma_thuong_hieu")
            .param(slug).query(Long.class).single();
    }

    private long danhMuc(String slug) {
        return jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Danh mục thử', ?) RETURNING ma_danh_muc")
            .param(slug).query(Long.class).single();
    }

    private long sanPham(long maThuongHieu, long maDanhMuc, String ten, String slug, String loaiDa, boolean active, int ngay) {
        return jdbc.sql("""
            INSERT INTO san_pham (ma_thuong_hieu, ma_danh_muc, ten_san_pham, duong_dan, loai_da_phu_hop, dang_hoat_dong, ngay_tao)
            VALUES (?, ?, ?, ?, ?, ?, TIMESTAMPADD(DAY, ?, '2026-01-01 00:00:00')) RETURNING ma_san_pham
            """).param(maThuongHieu).param(maDanhMuc).param(ten).param(slug).param(loaiDa).param(active).param(ngay)
            .query(Long.class).single();
    }

    private long bienThe(long maSanPham, String sku, int gia, int ton, int giu, boolean active, String url) {
        return jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho,
                so_luong_tam_giu, dang_hoat_dong, url_hinh_anh)
            VALUES (?, ?, '30 ml', ?, ?, ?, ?, ?) RETURNING ma_bien_the
            """).param(maSanPham).param(sku).param(gia).param(ton).param(giu).param(active).param(url)
            .query(Long.class).single();
    }
}
