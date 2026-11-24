package com.beautyshop.service;

import com.beautyshop.recommendation.DiemPhoBien_Policy.UngVien;
import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.repository.CuaHang_Repository.TheSanPhamCoMa;
import com.beautyshop.repository.GoiYSanPham_Repository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** P21: gợi ý phổ biến giữ thứ tự xếp hạng, cắt đúng số lượng, cửa hàng mới thì rơi về sản phẩm mới nhất. */
class GoiYPhoBienServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T05:00:00Z"), ZoneOffset.UTC);
    private final GoiYSanPham_Repository goiY = mock(GoiYSanPham_Repository.class);
    private final CuaHang_Repository cuaHang = mock(CuaHang_Repository.class);
    private final GoiYPhoBien_Service service = new GoiYPhoBien_Service(goiY, cuaHang, CLOCK);

    private static TheSanPhamCoMa the(long ma) {
        var the = mock(TheSanPhamCoMa.class);
        when(the.getMaSanPham()).thenReturn(ma);
        when(the.getDuongDan()).thenReturn("sp-" + ma);
        return the;
    }

    @Test
    void giuThuTuXepHangVaCatSoLuong() {
        LocalDateTime tu = LocalDateTime.of(2026, 8, 25, 5, 0);
        when(goiY.danhGiaCuaHang(tu)).thenReturn(new long[] {0, 0});
        when(goiY.ungVien(tu)).thenReturn(List.of(
            new UngVien(1, LocalDateTime.of(2026, 1, 1, 0, 0), 5, 0, 0, 0),
            new UngVien(2, LocalDateTime.of(2026, 1, 2, 0, 0), 50, 0, 0, 0),
            new UngVien(3, LocalDateTime.of(2026, 1, 3, 0, 0), 20, 0, 0, 0)));
        // Repository returns cards in arbitrary order; the service restores the ranking.
        var ba = the(3);
        var hai = the(2);
        when(cuaHang.timTheTheoMa(List.of(2L, 3L))).thenReturn(List.of(ba, hai));

        List<TheSanPham> ketQua = service.goiY(null, 2);

        assertThat(ketQua).extracting(TheSanPham::getDuongDan).containsExactly("sp-2", "sp-3");
        verify(cuaHang, never()).timNoiBat(any());
    }

    @Test
    void cuaHangMoiRoiVeSanPhamMoiNhat() {
        when(goiY.danhGiaCuaHang(any())).thenReturn(new long[] {0, 0});
        when(goiY.ungVien(any())).thenReturn(List.of(new UngVien(1, LocalDateTime.of(2026, 1, 1, 0, 0), 0, 0, 0, 0)));
        var moiNhat = the(9);
        when(cuaHang.timNoiBat(PageRequest.of(0, 5))).thenReturn(List.<TheSanPham>of(moiNhat));

        assertThat(service.goiY(42L, 5)).containsExactly(moiNhat);
        verify(cuaHang, never()).timTheTheoMa(any());
    }
}
