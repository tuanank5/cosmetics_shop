package com.beautyshop.service;

import com.beautyshop.recommendation.DiemPhoBien_Policy;
import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.repository.CuaHang_Repository.TheSanPhamCoMa;
import com.beautyshop.repository.GoiYSanPham_Repository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gợi ý phiên bản 1: sản phẩm phổ biến nhất, giống nhau cho mọi người. Công thức: {@link DiemPhoBien_Policy}. */
@Service
@Transactional(readOnly = true)
public class GoiYPhoBien_Service implements GoiYSanPham_Service {
    static final int SO_LUONG_TOI_DA = 12;

    private final GoiYSanPham_Repository goiYRepository;
    private final CuaHang_Repository cuaHangRepository;
    private final Clock clock;

    @Autowired
    public GoiYPhoBien_Service(GoiYSanPham_Repository goiYRepository, CuaHang_Repository cuaHangRepository) {
        this(goiYRepository, cuaHangRepository, Clock.systemUTC());
    }

    GoiYPhoBien_Service(GoiYSanPham_Repository goiYRepository, CuaHang_Repository cuaHangRepository, Clock clock) {
        this.goiYRepository = goiYRepository;
        this.cuaHangRepository = cuaHangRepository;
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
        long[] cuaHang = goiYRepository.danhGiaCuaHang(tu);
        return DiemPhoBien_Policy.xepHang(goiYRepository.ungVien(tu), cuaHang[0], cuaHang[1]);
    }
}
