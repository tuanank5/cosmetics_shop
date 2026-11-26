package com.beautyshop.integration;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P23: MongoDB không chạy (URI trỏ tới cổng không có server) -> app vẫn khởi động; trang chủ, danh sách, chi tiết sản phẩm
 * và giỏ hàng vẫn 200; chi tiết sản phẩm báo "Tạm thời chưa tải được đánh giá". MariaDB thật ({@code P01_MARIADB_TEST}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.data.mongodb.uri=mongodb://127.0.0.1:27999/beautyshop_mongo_tat"})
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class MongoTat_MariaDb_Test {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;

    @Test
    void trangChinhVanChayKhiMongoTat() throws Exception {
        String slug = taoSanPham();
        long batDau = System.nanoTime();
        mvc.perform(get("/products/" + slug)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Tạm thời chưa tải được đánh giá")))
            .andExpect(content().string(containsString("Sản phẩm Mongo tắt")));
        // Sau lần lỗi đầu, store nghỉ 15 s: các trang tiếp theo không chờ timeout chọn server (2 s) nữa.
        long sauLoiDau = System.nanoTime();
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/products")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Sản phẩm Mongo tắt")))
            .andExpect(content().string(containsString("Đánh giá tạm thời không khả dụng.")));
        mvc.perform(get("/products/" + slug)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Tạm thời chưa tải được đánh giá")));
        mvc.perform(get("/cart")).andExpect(status().isOk());
        long ketThuc = System.nanoTime();
        assertThat((sauLoiDau - batDau) / 1_000_000).as("lần đầu: chờ tối đa timeout chọn server").isLessThan(6_000);
        assertThat((ketThuc - sauLoiDau) / 1_000_000).as("các trang sau: fail-fast").isLessThan(1_500);
        mvc.perform(get("/admin/dashboard").with(user("fixture").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Thống kê đánh giá tạm thời không khả dụng.")))
            .andExpect(content().string(not(containsString("id=\"table-rating\""))))
            .andExpect(content().string(containsString("Doanh thu thực thu")));
    }

    private String taoSanPham() {
        String slug = "mongo-tat-" + UUID.randomUUID();
        long dm = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Test', ?) RETURNING ma_danh_muc")
            .param(slug).query(Long.class).single();
        long th = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Test', ?) RETURNING ma_thuong_hieu")
            .param(slug).query(Long.class).single();
        long sp = jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
            VALUES (?, ?, 'Sản phẩm Mongo tắt', ?) RETURNING ma_san_pham
            """).params(dm, th, slug).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, 'Test', 100000, 10)
            """).params(sp, slug).update();
        return slug;
    }
}
