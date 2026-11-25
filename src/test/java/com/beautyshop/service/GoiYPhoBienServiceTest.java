package com.beautyshop.service;

import com.beautyshop.recommendation.DiemPhoBien_Policy.UngVien;
import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.repository.CuaHang_Repository.TheSanPhamCoMa;
import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.repository.DanhGia_Store.TomTatDanhGia;
import com.beautyshop.repository.GoiYSanPham_Repository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** P21/P23: gợi ý phổ biến giữ thứ tự xếp hạng, ghép đánh giá từ DanhGia_Store, cửa hàng mới thì rơi về sản phẩm mới nhất. */
class GoiYPhoBienServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T05:00:00Z"), ZoneOffset.UTC);
    private static final Instant TU = Instant.parse("2026-08-25T05:00:00Z");
    private final GoiYSanPham_Repository goiY = mock(GoiYSanPham_Repository.class);
    private final CuaHang_Repository cuaHang = mock(CuaHang_Repository.class);
    private final DanhGia_Store danhGia = mock(DanhGia_Store.class);
    private final GoiYPhoBien_Service service = new GoiYPhoBien_Service(goiY, cuaHang, danhGia, CLOCK);

    private static TheSanPhamCoMa the(long ma) {
        var the = mock(TheSanPhamCoMa.class);
        when(the.getMaSanPham()).thenReturn(ma);
        when(the.getDuongDan()).thenReturn("sp-" + ma);
        return the;
    }

    private static UngVien uv(long ma, int ngay, long ban) {
        return new UngVien(ma, LocalDateTime.of(2026, 1, ngay, 0, 0), ban, 0, 0, 0);
    }

    @Test
    void giuThuTuXepHangVaCatSoLuong() {
        LocalDateTime tu = LocalDateTime.of(2026, 8, 25, 5, 0);
        when(goiY.ungVien(tu)).thenReturn(List.of(uv(1, 1, 5), uv(2, 2, 50), uv(3, 3, 20)));
        when(danhGia.tomTatNhieu(List.of(1L, 2L, 3L), TU)).thenReturn(Map.of());
        when(danhGia.tomTatCuaHang(TU)).thenReturn(TomTatDanhGia.RONG);
        // Repository returns cards in arbitrary order; the service restores the ranking.
        var ba = the(3);
        var hai = the(2);
        when(cuaHang.timTheTheoMa(List.of(2L, 3L))).thenReturn(List.of(ba, hai));

        List<TheSanPham> ketQua = service.goiY(null, 2);

        assertThat(ketQua).extracting(TheSanPham::getDuongDan).containsExactly("sp-2", "sp-3");
        verify(cuaHang, never()).timNoiBat(any());
    }

    @Test
    void danhGiaTuStoreDuocGhepVaoDiemBayes() {
        when(goiY.ungVien(any())).thenReturn(List.of(uv(1, 1, 10), uv(2, 2, 10)));
        // Cùng lượng bán; sản phẩm 1 có 10 đánh giá 5 sao -> đứng đầu dù ngày tạo cũ hơn.
        when(danhGia.tomTatNhieu(any(), any())).thenReturn(Map.of(1L, new TomTatDanhGia(10, 50, new long[] {0, 0, 0, 0, 10})));
        when(danhGia.tomTatCuaHang(any())).thenReturn(new TomTatDanhGia(20, 70, new long[] {0, 0, 10, 0, 10}));

        var kq = service.xepHang();

        assertThat(kq.xepHang()).extracting(d -> d.ungVien().maSanPham()).containsExactly(1L, 2L);
        assertThat(kq.xepHang().get(0).ungVien().soDanhGia()).isEqualTo(10);
        assertThat(kq.diemTrungBinhCuaHang()).isEqualTo(3.5);
    }

    @Test
    void storeLoiThiBoThanhPhanDanhGiaVanXepHangTheoBan() {
        when(goiY.ungVien(any())).thenReturn(List.of(uv(1, 1, 1), uv(2, 2, 9)));
        when(danhGia.tomTatNhieu(any(), any())).thenThrow(new DataAccessResourceFailureException("mongo tat"));

        var kq = service.xepHang();

        assertThat(kq.diemTrungBinhCuaHang()).isNull();
        assertThat(kq.xepHang()).extracting(d -> d.ungVien().maSanPham()).containsExactly(2L, 1L);
    }

    @Test
    void cuaHangMoiRoiVeSanPhamMoiNhat() {
        when(goiY.ungVien(any())).thenReturn(List.of(uv(1, 1, 0)));
        when(danhGia.tomTatNhieu(any(), any())).thenReturn(Map.of());
        when(danhGia.tomTatCuaHang(any())).thenReturn(TomTatDanhGia.RONG);
        var moiNhat = the(9);
        when(cuaHang.timNoiBat(PageRequest.of(0, 5))).thenReturn(List.<TheSanPham>of(moiNhat));

        assertThat(service.goiY(42L, 5)).containsExactly(moiNhat);
        verify(cuaHang, never()).timTheTheoMa(any());
    }
}
