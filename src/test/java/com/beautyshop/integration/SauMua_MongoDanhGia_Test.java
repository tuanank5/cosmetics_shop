package com.beautyshop.integration;

import com.beautyshop.model.DanhGia_Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P23: chạy lại toàn bộ kịch bản sau mua của {@link SauMua_MariaDb_Test} với store {@code mongo}: BR16 kiểm ở MariaDB thật,
 * nội dung đánh giá ghi/đọc ở MongoDB thật ({@code beautyshop_test_mongo}). Cần cả {@code P01_MARIADB_TEST} và
 * {@code P23_MONGO_TEST}.
 */
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
@EnabledIfEnvironmentVariable(named = "P23_MONGO_TEST", matches = "true")
class SauMua_MongoDanhGia_Test extends SauMua_MariaDb_Test {
    @Autowired MongoTemplate mongo;

    @BeforeEach
    void xoaDanhGiaMongo() {
        assertThat(mongo.getDb().getName()).isEqualTo("beautyshop_test_mongo");
        // Database MariaDB kiểm thử mới tạo nên mã dòng đơn bắt đầu lại từ 1: xóa document cũ, giữ index.
        mongo.remove(new Query(), DanhGia_Document.class);
    }
}
