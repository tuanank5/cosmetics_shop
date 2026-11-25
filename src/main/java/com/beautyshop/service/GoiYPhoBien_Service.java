package com.beautyshop.service;

import com.beautyshop.recommendation.DiemPhoBien_Policy;
import com.beautyshop.recommendation.DiemPhoBien_Policy.UngVien;
import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.repository.DanhGia_Store.TomTatDanhGia;
import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.repository.CuaHang_Repository.TheSanPhamCoMa;
import com.beautyshop.repository.GoiYSanPham_Repository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gợi ý phiên bản 1: sản phẩm phổ biến nhất, giống nhau cho mọi người. Công thức: {@link DiemPhoBien_Policy}. */
@Service
@Transactional(readOnly = true)
public class GoiYPhoBien_Service implements GoiYSanPham_Service {
    static final int SO_LUONG_TOI_DA = 12;

    private static final Logger log = LoggerFactory.getLogger(GoiYPhoBien_Service.class);
    private final GoiYSanPham_Repository goiYRepository;
    private final CuaHang_Repository cuaHangRepository;
    private final DanhGia_Store danhGia;
    private final Clock clock;

    @Autowired
    public GoiYPhoBien_Service(GoiYSanPham_Repository goiYRepository, CuaHang_Repository cuaHangRepository, DanhGia_Store danhGia) {
        this(goiYRepository, cuaHangRepository, danhGia, Clock.systemUTC());
    }

    GoiYPhoBien_Service(GoiYSanPham_Repository goiYRepository, CuaHang_Repository cuaHangRepository, DanhGia_Store danhGia,
                        Clock clock) {
        this.goiYRepository = goiYRepository;
        this.cuaHangRepository = cuaHangRepository;
        this.danhGia = danhGia;
        this.clock = clock;
    }

    @Override
    public List<TheSanPham> goiY(Long maNguoiDungHoacNull, int soLuong) {
        int n = Math.clamp(soLuong, 1, SO_LUONG_TOI_DA);
        DiemPhoBien_Policy.KetQua ketQua = xepHang();
        if (!ketQua.coDuLieu()) {
            // Cửa hàng mới (chưa bán, chưa đánh giá, chưa ai yêu thích): rơi về sản phẩm mới nhất.
            return List.copyOf(cuaHangRepository.timNoiBat(PageRequest.of(0, n)));
        }
        List<Long> ma = ketQua.xepHang().stream().limit(n).map(d -> d.ungVien().maSanPham()).toList();
        if (ma.isEmpty()) {
            return List.of();
        }
        Map<Long, TheSanPhamCoMa> the = cuaHangRepository.timTheTheoMa(ma).stream()
            .collect(Collectors.toMap(TheSanPhamCoMa::getMaSanPham, Function.identity()));
        return ma.stream().map(the::get).filter(Objects::nonNull).map(TheSanPham.class::cast).toList();
    }

    /** Bảng xếp hạng đầy đủ kèm từng thành phần điểm (dùng cho báo cáo/QA và kiểm thử). */
    public DiemPhoBien_Policy.KetQua xepHang() {
        LocalDateTime tu = LocalDateTime.now(clock.withZone(java.time.ZoneOffset.UTC)).minusDays(DiemPhoBien_Policy.SO_NGAY_TINH);
        List<UngVien> ungVien = goiYRepository.ungVien(tu);
        // P23: số đánh giá/tổng sao đến từ DanhGia_Store (MongoDB), một aggregation cho mọi ứng viên + một cho toàn cửa hàng.
        Instant tuUtc = tu.toInstant(java.time.ZoneOffset.UTC);
        Map<Long, TomTatDanhGia> theoSanPham;
        TomTatDanhGia cuaHang;
        try {
            theoSanPham = danhGia.tomTatNhieu(ungVien.stream().map(UngVien::maSanPham).toList(), tuUtc);
            cuaHang = danhGia.tomTatCuaHang(tuUtc);
        } catch (DataAccessException exception) {
            log.warn("Goi y pho bien: khong doc duoc danh gia, tinh diem khong co thanh phan danh gia: {}", exception.getMessage());
            theoSanPham = Map.of();
            cuaHang = TomTatDanhGia.RONG;
        }
        Map<Long, TomTatDanhGia> dg = theoSanPham;
        List<UngVien> day = ungVien.stream().map(u -> {
            TomTatDanhGia t = dg.getOrDefault(u.maSanPham(), TomTatDanhGia.RONG);
            return new UngVien(u.maSanPham(), u.ngayTao(), u.soLuongDaBan(), t.soLuong(), t.tongSao(), u.soLuotYeuThich());
        }).toList();
        return DiemPhoBien_Policy.xepHang(day, cuaHang.soLuong(), cuaHang.tongSao());
    }
}
