package com.beautyshop.review;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.beautyshop.entity.DanhGia_Document;
import com.beautyshop.repository.DanhGia_Repository;
import com.beautyshop.repository.DanhGia_Repository.DanhGiaCu;
import com.beautyshop.repository.MongoDanhGia_Store;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * P23: chuyển một lần bảng {@code danh_gia} MariaDB sang MongoDB khi {@code app.reviews.migrate-from-mariadb=true}.
 * Idempotent: upsert theo {@code maChiTietDonHang} chỉ với {@code $setOnInsert}, nên chạy lại không nhân đôi và không đè
 * đánh giá đã có trong MongoDB (kể cả bản khách đã sửa hoặc Admin đã duyệt sau lần chuyển trước). Bảng MariaDB giữ nguyên.
 * Đánh giá đang hiển thị được coi là đã duyệt ({@code ngayDuyet = ngayTao}) để Admin phân biệt với chờ duyệt.
 */
@Component
@ConditionalOnProperty(name = "app.reviews.migrate-from-mariadb", havingValue = "true")
public class ChuyenDanhGiaSangMongo_Runner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ChuyenDanhGiaSangMongo_Runner.class);

    private final DanhGia_Repository nguon;
    private final MongoTemplate mongo;
    private final ObjectProvider<MongoDanhGia_Store> store;

    public ChuyenDanhGiaSangMongo_Runner(DanhGia_Repository nguon, MongoTemplate mongo, ObjectProvider<MongoDanhGia_Store> store) {
        this.nguon = nguon;
        this.mongo = mongo;
        this.store = store;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Index duy nhất maChiTietDonHang phải có trước khi upsert (runner chạy trước ApplicationReadyEvent).
            store.ifAvailable(MongoDanhGia_Store::taoIndex);
            KetQua ketQua = chuyen();
            log.info("P23 migrate danh_gia MariaDB -> MongoDB: doc={}, ghi={}, bo qua={} (da co trong MongoDB={}, khong hop le={})",
                ketQua.doc(), ketQua.ghi(), ketQua.boQua(), ketQua.daCo(), ketQua.khongHopLe());
        } catch (DataAccessException exception) {
            log.warn("P23 migrate danh_gia: khong chuyen duoc (MariaDB/MongoDB loi): {}", exception.getMessage());
        }
    }

    public KetQua chuyen() {
        return ghi(nguon.tatCaDanhGiaCu());
    }

    /** Ghi danh sách bản cũ vào MongoDB; tách riêng để kiểm thử chạy hai lần. */
    public KetQua ghi(List<DanhGiaCu> danhSach) {
        int ghi = 0;
        int daCo = 0;
        int khongHopLe = 0;
        for (DanhGiaCu cu : danhSach) {
            if (cu.soSao() < 1 || cu.soSao() > 5 || cu.ngayTao() == null) {
                khongHopLe++;
                continue;
            }
            var update = new Update()
                .setOnInsert("maDonHang", cu.maDonHang()).setOnInsert("maNguoiDung", cu.maNguoiDung())
                .setOnInsert("maSanPham", cu.maSanPham()).setOnInsert("maBienThe", cu.maBienThe())
                .setOnInsert("tenHienThi", cu.tenHienThi()).setOnInsert("soSao", cu.soSao())
                .setOnInsert("noiDung", cu.noiDung() == null ? "" : cu.noiDung().strip())
                .setOnInsert("hienThi", cu.hienThi()).setOnInsert("phienBan", 0L)
                .setOnInsert("ngayTao", cu.ngayTao()).setOnInsert("ngayCapNhat", cu.ngayTao());
            if (cu.hienThi()) {
                update.setOnInsert("ngayDuyet", cu.ngayTao());
            }
            var ketQua = mongo.upsert(Query.query(where("maChiTietDonHang").is(cu.maChiTietDonHang())), update,
                DanhGia_Document.class);
            if (ketQua.getUpsertedId() != null) {
                ghi++;
            } else {
                daCo++;
            }
        }
        return new KetQua(danhSach.size(), ghi, daCo, khongHopLe);
    }

    public record KetQua(int doc, int ghi, int daCo, int khongHopLe) {
        public int boQua() { return daCo + khongHopLe; }
    }
}
