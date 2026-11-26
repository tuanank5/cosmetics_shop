package com.beautyshop.repository;

import com.beautyshop.model.DanhGia_Document;
import com.beautyshop.repository.DanhGia_Store.BoLocDanhGia;
import com.beautyshop.repository.DanhGia_Store.DanhGiaMoi;
import com.beautyshop.repository.DanhGia_Store.TrangThaiLoc;
import com.beautyshop.review.MongoDanhGia_Config;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P23: MongoDB thật (local, không Docker/embedded). Bật bằng {@code P23_MONGO_TEST=true}; chạy trên database
 * {@code beautyshop_test_mongo} (src/test/resources/config/application.properties), collection được xóa trước mỗi test.
 */
@DataMongoTest
@Import(MongoDanhGia_Config.class)
@EnabledIfEnvironmentVariable(named = "P23_MONGO_TEST", matches = "true")
class MongoDanhGia_Store_Test {
    private static final Instant T0 = Instant.parse("2026-11-01T03:00:00Z");

    @Autowired MongoTemplate mongo;
    @Autowired DanhGiaMongo_Repository repository;
    MongoDanhGia_Store store;

    @BeforeEach
    void sach() {
        assertThat(mongo.getDb().getName()).isEqualTo("beautyshop_test_mongo");
        mongo.dropCollection(DanhGia_Document.class);
        store = luc(T0);
        store.taoIndex();
    }

    private MongoDanhGia_Store luc(Instant thoiDiem) {
        return new MongoDanhGia_Store(mongo, repository, Clock.fixed(thoiDiem, ZoneOffset.UTC));
    }

    private static DanhGiaMoi moi(long muc, long sanPham, int sao) {
        return new DanhGiaMoi(muc, 900 + muc, 70, sanPham, 300 + sanPham, "Thịnh P.", sao, "Nhận xét " + muc);
    }

    private String ma(long muc) {
        return store.cuaChuMuc(muc, 70).orElseThrow().ma();
    }

    private void duyet(long muc) {
        assertThat(store.datHienThi(ma(muc), true, "0")).isTrue();
    }

    @Test
    @EnabledIfSystemProperty(named = "data01.backup", matches = "true")
    void taoMauSaoLuu() {
        store.tao(moi(1, 10, 5));
        duyet(1);
        assertThat(mongo.count(new Query(), DanhGia_Document.class)).isEqualTo(1);
    }

    @Test
    void haiNguoiGhiCungMucChiMotLanThanhCong() throws Exception {
        var batDau = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            java.util.concurrent.Callable<Boolean> tao = () -> {
                batDau.await();
                try {
                    store.tao(moi(1, 10, 5));
                    return true;
                } catch (DuplicateKeyException exception) {
                    return false;
                }
            };
            var a = executor.submit(tao);
            var b = executor.submit(tao);
            batDau.countDown();
            assertThat(List.of(a.get(10, java.util.concurrent.TimeUnit.SECONDS),
                b.get(10, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        }
        assertThat(mongo.count(new Query(), DanhGia_Document.class)).isEqualTo(1);
    }

    @Test
    void indexSaiChanGhiVaKhongSuaDuLieu() {
        store.tao(moi(1, 10, 5));
        var indexes = mongo.indexOps(DanhGia_Document.class);
        indexes.dropIndex("uq_ma_chi_tiet_don_hang");
        indexes.createIndex(new org.springframework.data.mongodb.core.index.Index()
            .on("maChiTietDonHang", org.springframework.data.domain.Sort.Direction.ASC)
            .named("uq_ma_chi_tiet_don_hang"));
        assertThatThrownBy(() -> store.tao(moi(2, 10, 1)))
            .isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
        assertThat(mongo.count(new Query(), DanhGia_Document.class)).isEqualTo(1);
        assertThat(indexes.getIndexInfo()).anyMatch(index ->
            index.getName().equals("uq_ma_chi_tiet_don_hang") && !index.isUnique());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DATA_01_MONGOD", matches = ".+")
    void khoiDongKhiMongoTatRoiTaoIndexTruocGhiSauPhucHoi() throws Exception {
        int port;
        try (var socket = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        var data = java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("target"), "data-01-recovery-");
        var now = new java.util.concurrent.atomic.AtomicReference<>(T0);
        var clock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return Clock.fixed(instant(), zone); }
            @Override public Instant instant() { return now.get(); }
        };
        String uri = "mongodb://127.0.0.1:" + port + "/data_01_recovery?serverSelectionTimeoutMS=1000";
        try (var client = com.mongodb.client.MongoClients.create(uri)) {
            var template = new MongoTemplate(client, "data_01_recovery");
            var recovering = new MongoDanhGia_Store(template, repository, clock);
            recovering.taoIndex();
            assertThatThrownBy(() -> recovering.tao(moi(1, 10, 5)))
                .isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
            var process = new ProcessBuilder(System.getenv("DATA_01_MONGOD"), "--dbpath", data.toString(),
                "--bind_ip", "127.0.0.1", "--port", Integer.toString(port), "--wiredTigerCacheSizeGB", "0.25")
                .redirectErrorStream(true).redirectOutput(data.resolve("mongod.log").toFile()).start();
            try {
                boolean ready = false;
                for (int i = 0; i < 10 && process.isAlive(); i++) {
                    try {
                        template.getDb().runCommand(new Document("ping", 1));
                        ready = true;
                        break;
                    } catch (com.mongodb.MongoTimeoutException exception) {
                        // Wait for this isolated process to listen.
                    }
                }
                assertThat(ready).isTrue();
                assertThatThrownBy(() -> recovering.tao(moi(1, 10, 5)))
                    .isInstanceOf(org.springframework.dao.DataAccessResourceFailureException.class);
                now.set(T0.plusMillis(MongoDanhGia_Store.NGHI_SAU_LOI_MS));
                recovering.tao(moi(1, 10, 5));
                assertThatThrownBy(() -> recovering.tao(moi(1, 10, 1))).isInstanceOf(DuplicateKeyException.class);
                assertThat(template.count(new Query(), DanhGia_Document.class)).isEqualTo(1);
            } finally {
                process.destroy();
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) process.destroyForcibly();
            }
        }
    }

    @Test
    void indexDuocTaoVaKhoaDuyNhatChanTrungDongDon() {
        var ten = mongo.indexOps(DanhGia_Document.class).getIndexInfo().stream().map(i -> i.getName()).toList();
        assertThat(ten).contains("uq_ma_chi_tiet_don_hang", "idx_san_pham_hien_thi_ngay", "idx_nguoi_dung", "idx_hien_thi_ngay");
        assertThat(mongo.indexOps(DanhGia_Document.class).getIndexInfo().stream()
            .filter(i -> i.getName().equals("uq_ma_chi_tiet_don_hang")).findFirst().orElseThrow().isUnique()).isTrue();

        store.tao(moi(1, 10, 5));
        assertThatThrownBy(() -> store.tao(moi(1, 10, 1))).isInstanceOf(DuplicateKeyException.class);
        // Ghi thẳng bỏ qua store vẫn bị index duy nhất chặn.
        var trung = new DanhGia_Document();
        trung.setMaChiTietDonHang(1L);
        trung.setSoSao(3);
        assertThatThrownBy(() -> mongo.insert(trung)).isInstanceOf(DuplicateKeyException.class);

        assertThat(mongo.count(new Query(), DanhGia_Document.class)).isEqualTo(1);
        var doc = mongo.findOne(Query.query(Criteria.where("maChiTietDonHang").is(1L)), Document.class, DanhGia_Document.COLLECTION);
        assertThat(doc.get("soSao")).isEqualTo(5);
        assertThat(doc.get("hienThi")).isEqualTo(false);
        assertThat(doc.get("phienBan")).isEqualTo(0L);
        assertThat(doc.get("ngayDuyet")).isNull();
        assertThat(doc.get("ngayTao")).isInstanceOf(java.util.Date.class);
    }

    @Test
    void aggregationTrungBinhPhanBoNhieuSanPhamVaToanCuaHang() {
        store.tao(moi(1, 10, 5));
        store.tao(moi(2, 10, 4));
        store.tao(moi(3, 10, 1));
        store.tao(moi(4, 11, 3));
        store.tao(moi(5, 10, 2)); // chờ duyệt: không tính vào tóm tắt công khai
        luc(T0.plusSeconds(86_400 * 10)).tao(moi(6, 11, 5)); // tạo muộn hơn 10 ngày
        for (long muc : new long[] {1, 2, 3, 4, 6}) {
            duyet(muc);
        }

        var tomTat = store.tomTatNhieu(List.of(10L, 11L, 12L), null);
        assertThat(tomTat).containsOnlyKeys(10L, 11L);
        assertThat(tomTat.get(10L).soLuong()).isEqualTo(3);
        assertThat(tomTat.get(10L).tongSao()).isEqualTo(10);
        assertThat(tomTat.get(10L).phanBo()).containsExactly(1, 0, 0, 1, 1);
        assertThat(tomTat.get(10L).trungBinhHienThi()).isEqualTo("3,3");
        assertThat(tomTat.get(11L).phanBo()).containsExactly(0, 0, 1, 0, 1);
        assertThat(store.tomTat(12L).soLuong()).isZero();

        // Lọc theo ngày tạo (điểm phổ biến P21 chỉ xét 90 ngày).
        var tuNgay5 = store.tomTatNhieu(List.of(10L, 11L), T0.plusSeconds(86_400 * 5));
        assertThat(tuNgay5).containsOnlyKeys(11L);
        assertThat(tuNgay5.get(11L).soLuong()).isEqualTo(1);

        var cuaHang = store.tomTatCuaHang(T0.minusSeconds(1));
        assertThat(cuaHang.soLuong()).isEqualTo(5);
        assertThat(cuaHang.tongSao()).isEqualTo(18);
        assertThat(store.tomTatCuaHang(T0.plusSeconds(86_400 * 20)).soLuong()).isZero();

        // Dashboard: chờ duyệt mọi thời điểm; phân bố theo ngày tạo trong kỳ (kể cả chờ duyệt).
        var thongKe = store.thongKe(T0.minusSeconds(1), T0.plusSeconds(86_400));
        assertThat(thongKe.choDuyet()).isEqualTo(1);
        assertThat(thongKe.phanBoTrongKy()).containsExactly(1, 1, 1, 1, 1);
    }

    @Test
    void findAndModifyTheoPhienBanVaTrangThaiLocAdmin() {
        store.tao(moi(1, 10, 5));
        String id = ma(1);
        assertThat(store.datHienThi(id, true, "1")).isFalse();
        assertThat(store.danhSachAdmin(new BoLocDanhGia(TrangThaiLoc.CHO_DUYET, null, null), PageRequest.of(0, 10)))
            .singleElement().satisfies(d -> {
                assertThat(d.ma()).isEqualTo(id);
                assertThat(d.phienBan()).isEqualTo("0");
                assertThat(d.daDuyet()).isFalse();
            });

        assertThat(store.datHienThi(id, true, "0")).isTrue();
        // @Version: mọi thay đổi (kể cả duyệt) tăng đúng 1; duyệt lại bằng phiên bản cũ bị từ chối.
        assertThat(repository.findById(id).orElseThrow().getPhienBan()).isEqualTo(1);
        assertThat(store.datHienThi(id, true, "0")).isFalse();
        assertThat(store.danhSachAdmin(new BoLocDanhGia(TrangThaiLoc.DANG_HIEN, 5, 10L), PageRequest.of(0, 10))).hasSize(1);
        assertThat(store.danhSachAdmin(new BoLocDanhGia(TrangThaiLoc.DANG_HIEN, 4, null), PageRequest.of(0, 10))).isEmpty();
        assertThat(store.congKhai(10, PageRequest.of(0, 10))).singleElement()
            .satisfies(c -> assertThat(c.tenHienThi()).isEqualTo("Thịnh P."));

        assertThat(store.sua(1, 99, 1, "Người khác sửa")).isFalse();
        assertThat(store.sua(1, 70, 2, "Đã sửa")).isTrue();
        var sau = repository.findById(id).orElseThrow();
        assertThat(sau.getPhienBan()).isEqualTo(2);
        assertThat(sau.getHienThi()).isFalse();
        assertThat(sau.getNgayDuyet()).isNull();
        assertThat(store.congKhai(10, PageRequest.of(0, 10))).isEmpty();
        assertThat(store.datHienThi(id, true, "1")).isFalse();
        assertThat(store.datHienThi(id, true, "2")).isTrue();

        assertThat(store.datHienThi(id, false, null)).isTrue();
        assertThat(repository.findById(id).orElseThrow().getPhienBan()).isEqualTo(4);
        assertThat(store.danhSachAdmin(new BoLocDanhGia(TrangThaiLoc.DA_AN, null, null), PageRequest.of(0, 10)))
            .singleElement().satisfies(d -> assertThat(d.daDuyet()).isTrue());
        assertThat(store.danhSachAdmin(new BoLocDanhGia(TrangThaiLoc.CHO_DUYET, null, null), PageRequest.of(0, 10))).isEmpty();
        assertThat(store.datHienThi("65f000000000000000000001", false, null)).isFalse();
    }

    @Test
    void phanTrangAdminVaCongKhaiMoiNhatTruoc() {
        for (int i = 1; i <= 25; i++) {
            luc(T0.plusSeconds(i)).tao(moi(i, 10, 1 + i % 5));
            duyet(i);
        }
        var trang2 = store.danhSachAdmin(BoLocDanhGia.TAT_CA, PageRequest.of(1, 20));
        assertThat(trang2.getTotalElements()).isEqualTo(25);
        assertThat(trang2.getContent()).hasSize(5);
        var congKhai = store.congKhai(10, PageRequest.of(0, 10));
        assertThat(congKhai.getTotalPages()).isEqualTo(3);
        assertThat(congKhai.getContent().get(0).noiDung()).isEqualTo("Nhận xét 25");
    }

    @Test
    void khiTrungThoiDiemThiSapTheoIdVaDemDungBoLocQuaCacTrang() {
        for (int i = 1; i <= 22; i++) {
            store.tao(moi(i, 10, i <= 21 ? 4 : 5));
            duyet(i);
        }
        store.tao(moi(23, 10, 4)); // Cùng sản phẩm nhưng chưa duyệt.
        store.tao(moi(24, 11, 4));
        duyet(24);
        var congKhai = java.util.stream.IntStream.range(0, 3)
            .mapToObj(i -> store.congKhai(10, PageRequest.of(i, 10))).toList();
        assertThat(congKhai).allSatisfy(p -> assertThat(p.getTotalElements()).isEqualTo(22));
        assertThat(congKhai.stream().flatMap(trang -> trang.getContent().stream()))
            .extracting(DanhGia_Store.DanhGiaCongKhai::noiDung)
            .containsExactlyElementsOf(java.util.stream.IntStream.iterate(22, i -> i - 1).limit(22)
                .mapToObj(i -> "Nhận xét " + i).toList());
        var loc = new BoLocDanhGia(TrangThaiLoc.DANG_HIEN, 4, 10L);
        var dau = store.danhSachAdmin(loc, PageRequest.of(0, 20));
        var cuoi = store.danhSachAdmin(loc, PageRequest.of(1, 20));
        assertThat(dau.getTotalElements()).isEqualTo(21);
        assertThat(cuoi.getTotalElements()).isEqualTo(21);
        assertThat(dau.getContent()).extracting(DanhGia_Store.DanhGiaAdmin::ma)
            .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(cuoi.getContent()).extracting(DanhGia_Store.DanhGiaAdmin::ma).containsExactly(ma(1));
    }

}
