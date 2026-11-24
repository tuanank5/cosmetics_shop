package com.beautyshop.repository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.beautyshop.entity.DanhGia_Document;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

/**
 * P23: lưu đánh giá trong MongoDB. Mọi điều kiện truy vấn dùng {@link Criteria}/tham số có kiểu, không ghép chuỗi JSON từ
 * dữ liệu người dùng. Khi MongoDB không phản hồi, store "nghỉ" {@value #NGHI_SAU_LOI_MS} ms để các trang không phải
 * chờ timeout ở mỗi lần gọi; trong lúc nghỉ, mọi lời gọi ném {@link DataAccessResourceFailureException} ngay.
 */
@Repository
@ConditionalOnProperty(name = "app.reviews.store", havingValue = "mongo", matchIfMissing = true)
public class MongoDanhGia_Store implements DanhGia_Store {
    private static final Logger log = LoggerFactory.getLogger(MongoDanhGia_Store.class);
    static final long NGHI_SAU_LOI_MS = 15_000;

    private final MongoTemplate mongo;
    private final DanhGiaMongo_Repository repository;
    private final Clock clock;
    private volatile long nghiDen;

    @Autowired
    public MongoDanhGia_Store(MongoTemplate mongo, DanhGiaMongo_Repository repository) {
        this(mongo, repository, Clock.systemUTC());
    }

    MongoDanhGia_Store(MongoTemplate mongo, DanhGiaMongo_Repository repository, Clock clock) {
        this.mongo = mongo;
        this.repository = repository;
        this.clock = clock;
    }

    /** Tạo index từ @Indexed/@CompoundIndex bằng IndexOperations; MongoDB tắt thì chỉ log, app vẫn chạy. */
    @EventListener(ApplicationReadyEvent.class)
    public void taoIndex() {
        try {
            IndexOperations ops = mongo.indexOps(DanhGia_Document.class);
            var resolver = new MongoPersistentEntityIndexResolver(mongo.getConverter().getMappingContext());
            for (var index : resolver.resolveIndexFor(DanhGia_Document.class)) {
                ops.createIndex(index);
            }
            log.info("MongoDB danh_gia: index san sang {}", ops.getIndexInfo().stream().map(i -> i.getName()).toList());
        } catch (RuntimeException exception) {
            log.warn("MongoDB danh_gia: khong tao duoc index (MongoDB co the dang tat): {}", exception.getMessage());
        }
    }

    @Override
    public Optional<DanhGiaCuaKhach> cuaChuMuc(long maChiTietDonHang, long maNguoiDung) {
        return goi(() -> repository.findByMaChiTietDonHangAndMaNguoiDung(maChiTietDonHang, maNguoiDung)
            .map(d -> new DanhGiaCuaKhach(d.getId(), d.getSoSao(), d.getNoiDung(), Boolean.TRUE.equals(d.getHienThi()))));
    }

    /** Upsert theo khóa duy nhất maChiTietDonHang, chỉ ghi khi chưa có ($setOnInsert): đã có -> trùng, không đè. */
    @Override
    public void tao(DanhGiaMoi moi) {
        Instant now = clock.instant();
        var update = new Update()
            .setOnInsert("maDonHang", moi.maDonHang()).setOnInsert("maNguoiDung", moi.maNguoiDung())
            .setOnInsert("maSanPham", moi.maSanPham()).setOnInsert("maBienThe", moi.maBienThe())
            .setOnInsert("tenHienThi", moi.tenHienThi()).setOnInsert("soSao", moi.soSao())
            .setOnInsert("noiDung", moi.noiDung() == null ? "" : moi.noiDung()).setOnInsert("hienThi", false)
            .setOnInsert("phienBan", 0L).setOnInsert("ngayTao", now).setOnInsert("ngayCapNhat", now);
        var ketQua = goi(() -> mongo.upsert(Query.query(where("maChiTietDonHang").is(moi.maChiTietDonHang())), update,
            DanhGia_Document.class));
        if (ketQua.getUpsertedId() == null) {
            throw new DuplicateKeyException("Dong don " + moi.maChiTietDonHang() + " da co danh gia");
        }
    }

    /** Sửa có điều kiện chủ sở hữu; nội dung đổi -> tăng phiên bản, ẩn chờ duyệt lại. */
    @Override
    public boolean sua(long maChiTietDonHang, long maNguoiDung, int soSao, String noiDung) {
        var query = Query.query(where("maChiTietDonHang").is(maChiTietDonHang).and("maNguoiDung").is(maNguoiDung));
        var update = new Update().set("soSao", soSao).set("noiDung", noiDung == null ? "" : noiDung)
            .set("hienThi", false).unset("ngayDuyet").set("ngayCapNhat", clock.instant()).inc("phienBan", 1);
        return goi(() -> mongo.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true),
            DanhGia_Document.class)) != null;
    }

    @Override
    public Page<DanhGiaCongKhai> congKhai(long maSanPham, Pageable pageable) {
        return goi(() -> repository.findByMaSanPhamAndHienThiTrueOrderByNgayTaoDesc(maSanPham, pageable)
            .map(d -> new DanhGiaCongKhai(d.getTenHienThi(), d.getSoSao(), d.getNoiDung(), d.getNgayTao())));
    }

    @Override
    public Page<DanhGiaAdmin> danhSachAdmin(BoLocDanhGia boLoc, Pageable pageable) {
        Criteria criteria = new Criteria();
        switch (boLoc.trangThai()) {
            case CHO_DUYET -> criteria.and("hienThi").is(false).and("ngayDuyet").is(null);
            case DANG_HIEN -> criteria.and("hienThi").is(true);
            case DA_AN -> criteria.and("hienThi").is(false).and("ngayDuyet").ne(null);
            case TAT_CA -> { }
        }
        if (boLoc.soSao() != null) {
            criteria.and("soSao").is(boLoc.soSao());
        }
        if (boLoc.maSanPham() != null) {
            criteria.and("maSanPham").is(boLoc.maSanPham());
        }
        Query query = Query.query(criteria).with(Sort.by(Sort.Order.desc("ngayTao"), Sort.Order.desc("_id"))).with(pageable);
        return goi(() -> {
            List<DanhGiaAdmin> trang = mongo.find(query, DanhGia_Document.class).stream().map(MongoDanhGia_Store::admin).toList();
            return PageableExecutionUtils.getPage(trang, pageable,
                () -> mongo.count(Query.of(query).limit(-1).skip(-1), DanhGia_Document.class));
        });
    }

    /**
     * findAndModify theo id + phienBan khi duyệt (đúng phiên bản Admin đã xem, kể cả không ai khác vừa duyệt/ẩn);
     * ẩn chỉ theo id (giữ hành vi bản MariaDB). Mọi thay đổi tăng phienBan một lần, ghi tường minh.
     */
    @Override
    public boolean datHienThi(String ma, boolean hienThi, String phienBan) {
        if (ma == null || !ObjectId.isValid(ma)) {
            return false;
        }
        Criteria criteria = where("_id").is(new ObjectId(ma));
        Update update = new Update().set("hienThi", hienThi).set("ngayCapNhat", clock.instant()).inc("phienBan", 1);
        if (hienThi) {
            long daXem;
            try {
                daXem = Long.parseLong(phienBan);
            } catch (NumberFormatException | NullPointerException exception) {
                return false;
            }
            criteria = criteria.and("phienBan").is(daXem);
            update.set("ngayDuyet", clock.instant());
        }
        Query query = Query.query(criteria);
        return goi(() -> mongo.findAndModify(query, update, DanhGia_Document.class)) != null;
    }

    @Override
    public Map<Long, TomTatDanhGia> tomTatNhieu(Collection<Long> maSanPham, Instant tuHoacNull) {
        if (maSanPham.isEmpty()) {
            return Map.of();
        }
        Criteria criteria = where("maSanPham").in(maSanPham).and("hienThi").is(true);
        if (tuHoacNull != null) {
            criteria = criteria.and("ngayTao").gte(tuHoacNull);
        }
        // match -> group (theo sản phẩm: count, avg, sum, phân bố 1–5) -> project (đưa _id ra thành maSanPham).
        TypedAggregation<DanhGia_Document> aggregation = newAggregation(DanhGia_Document.class,
            match(criteria),
            phanBo(group("maSanPham")),
            project("soLuong", "tongSao", "trungBinh", "sao1", "sao2", "sao3", "sao4", "sao5").and("maSanPham").previousOperation());
        return goi(() -> {
            Map<Long, TomTatDanhGia> ketQua = new HashMap<>();
            for (Document d : mongo.aggregate(aggregation, Document.class).getMappedResults()) {
                ketQua.put(((Number) d.get("maSanPham")).longValue(), tomTat(d));
            }
            return ketQua;
        });
    }

    @Override
    public TomTatDanhGia tomTatCuaHang(Instant tu) {
        TypedAggregation<DanhGia_Document> aggregation = newAggregation(DanhGia_Document.class,
            match(where("hienThi").is(true).and("ngayTao").gte(tu)),
            phanBo(group()),
            project("soLuong", "tongSao", "trungBinh", "sao1", "sao2", "sao3", "sao4", "sao5"));
        return goi(() -> {
            Document d = mongo.aggregate(aggregation, Document.class).getUniqueMappedResult();
            return d == null ? TomTatDanhGia.RONG : tomTat(d);
        });
    }

    @Override
    public ThongKeDanhGia thongKe(Instant a, Instant b) {
        TypedAggregation<DanhGia_Document> aggregation = newAggregation(DanhGia_Document.class,
            match(where("ngayTao").gte(a).lt(b)),
            group("soSao").count().as("soLuong"),
            project("soLuong").and("soSao").previousOperation());
        return goi(() -> {
            long[] phanBo = new long[5];
            for (Document d : mongo.aggregate(aggregation, Document.class).getMappedResults()) {
                int sao = ((Number) d.get("soSao")).intValue();
                if (sao >= 1 && sao <= 5) {
                    phanBo[sao - 1] = ((Number) d.get("soLuong")).longValue();
                }
            }
            return new ThongKeDanhGia(repository.countByHienThiFalseAndNgayDuyetIsNull(), phanBo);
        });
    }

    private static GroupOperation phanBo(GroupOperation group) {
        GroupOperation g = group.count().as("soLuong").sum("soSao").as("tongSao").avg("soSao").as("trungBinh");
        for (int sao = 1; sao <= 5; sao++) {
            g = g.sum(ConditionalOperators.when(where("soSao").is(sao)).then(1).otherwise(0)).as("sao" + sao);
        }
        return g;
    }

    private static TomTatDanhGia tomTat(Document d) {
        long[] phanBo = new long[5];
        for (int sao = 1; sao <= 5; sao++) {
            phanBo[sao - 1] = ((Number) d.get("sao" + sao)).longValue();
        }
        return new TomTatDanhGia(((Number) d.get("soLuong")).longValue(), ((Number) d.get("tongSao")).longValue(), phanBo);
    }

    private static DanhGiaAdmin admin(DanhGia_Document d) {
        return new DanhGiaAdmin(d.getId(), d.getMaSanPham(), null, d.getMaDonHang(), d.getTenHienThi(), d.getSoSao(),
            d.getNoiDung(), Boolean.TRUE.equals(d.getHienThi()), d.getNgayDuyet() != null, String.valueOf(d.getPhienBan()),
            d.getNgayTao());
    }

    private <T> T goi(Supplier<T> truyVan) {
        if (clock.millis() < nghiDen) {
            throw new DataAccessResourceFailureException("MongoDB tam ngung sau loi ket noi");
        }
        try {
            return truyVan.get();
        } catch (DataAccessResourceFailureException exception) {
            nghiDen = clock.millis() + NGHI_SAU_LOI_MS;
            log.warn("MongoDB danh_gia khong phan hoi; tam ngung {} ms: {}", NGHI_SAU_LOI_MS, exception.getMessage());
            throw exception;
        }
    }
}
