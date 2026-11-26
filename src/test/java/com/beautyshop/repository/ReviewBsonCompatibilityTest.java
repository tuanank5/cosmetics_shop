package com.beautyshop.repository;

import com.beautyshop.model.DanhGia_Document;
import java.time.Instant;
import java.util.Date;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@EnabledIfEnvironmentVariable(named = "ARCH_01_BSON_PHASE", matches = "old-write|new-roundtrip|old-read")
class ReviewBsonCompatibilityTest {
    private static final String OLD = "com.beautyshop.entity.DanhGia_Document";
    private static final String NEW = "com.beautyshop.model.DanhGia_Document";
    private static final Instant CREATED = Instant.parse("2026-11-01T03:00:00Z");
    @Autowired MongoTemplate mongo;
    @Autowired DanhGiaMongo_Repository repository;

    @Test
    void documentRoundTripAcrossPackageVersions() {
        String phase = System.getenv("ARCH_01_BSON_PHASE");
        var collection = mongo.getCollection(DanhGia_Document.COLLECTION);
        assertThat(mongo.getDb().getName()).isEqualTo(phase.equals("old-write") ? "arch_01_source" : "arch_01_copy");
        switch (phase) {
            case "old-write" -> {
                assertThat(DanhGia_Document.class.getName()).isEqualTo(OLD);
                var legacy = repository.save(review(1));
                assertThat(collection.find(new Document("maChiTietDonHang", 1L)).first().getString("_class")).isEqualTo(OLD);
                assertReview(legacy.getId(), 1, 5, 0L);
                collection.insertOne(new Document("_id", new ObjectId(id(2))).append("maChiTietDonHang", 2L)
                    .append("maNguoiDung", 10L).append("maDonHang", 20L).append("maSanPham", 30L)
                    .append("maBienThe", 40L).append("tenHienThi", "Fixture").append("soSao", 5)
                    .append("noiDung", "Fixture review").append("hienThi", false).append("phienBan", 0L)
                    .append("ngayTao", Date.from(CREATED)).append("ngayCapNhat", Date.from(CREATED)));
                assertReview(id(2), 2, 5, 0L);
            }
            case "new-roundtrip" -> {
                assertThat(DanhGia_Document.class.getName()).isEqualTo(NEW);
                assertThat(collection.find(new Document("_id", new ObjectId(id(1)))).first().getString("_class")).isEqualTo(OLD);
                assertThat(collection.find(new Document("_id", new ObjectId(id(2)))).first()).doesNotContainKey("_class");
                for (long item : new long[]{1, 2}) {
                    assertReview(id(item), item, 5, 0L);
                    var previous = repository.findById(id(item)).orElseThrow();
                    previous.setSoSao(4);
                    repository.save(previous);
                    assertReview(id(item), item, 4, 1L);
                }
                repository.save(review(3));
                assertThat(collection.find(new Document("maChiTietDonHang", 3L)).first().getString("_class")).isEqualTo(NEW);
                assertReview(id(3), 3, 5, 0L);
            }
            case "old-read" -> {
                assertThat(DanhGia_Document.class.getName()).isEqualTo(OLD);
                assertReview(id(1), 1, 4, 1L);
                assertReview(id(2), 2, 4, 1L);
                assertThat(collection.find(new Document("maChiTietDonHang", 3L)).first().getString("_class")).isEqualTo(NEW);
                assertReview(id(3), 3, 5, 0L);
                var newer = repository.findById(id(3)).orElseThrow();
                newer.setSoSao(3);
                repository.save(newer);
                assertReview(id(3), 3, 3, 1L);
            }
            default -> throw new AssertionError(phase);
        }
    }

    private void assertReview(String id, long item, int stars, long version) {
        var byRepository = repository.findByMaChiTietDonHangAndMaNguoiDung(item, 10L).orElseThrow();
        var byTemplate = mongo.findById(id, DanhGia_Document.class);
        assertThat(byTemplate).usingRecursiveComparison().isEqualTo(byRepository);
        assertThat(byRepository.getId()).isEqualTo(id);
        assertThat(byRepository.getSoSao()).isEqualTo(stars);
        assertThat(byRepository.getPhienBan()).isEqualTo(version);
        assertThat(byRepository.getNgayTao()).isEqualTo(CREATED);
        assertThat(byRepository.getNoiDung()).isEqualTo("Fixture review");
    }

    private static String id(long item) {
        return "%024x".formatted(item);
    }

    private static DanhGia_Document review(long item) {
        var review = new DanhGia_Document();
        review.setId(id(item));
        review.setMaChiTietDonHang(item);
        review.setMaNguoiDung(10L);
        review.setMaDonHang(20L);
        review.setMaSanPham(30L);
        review.setMaBienThe(40L);
        review.setTenHienThi("Fixture");
        review.setSoSao(5);
        review.setNoiDung("Fixture review");
        review.setHienThi(false);
        review.setNgayTao(CREATED);
        review.setNgayCapNhat(CREATED);
        return review;
    }
}
