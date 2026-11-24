package com.beautyshop.recommendation;

import com.beautyshop.recommendation.DiemPhoBien_Policy.Diem;
import com.beautyshop.recommendation.DiemPhoBien_Policy.UngVien;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** P21: công thức điểm phổ biến 0.5 × bán + 0.3 × Bayes + 0.2 × yêu thích. */
class DiemPhoBienPolicyTest {
    private static final LocalDateTime GOC = LocalDateTime.of(2026, 1, 1, 0, 0);

    private static UngVien sp(long ma, int ngay, long ban, long soDanhGia, long tongSao, long thich) {
        return new UngVien(ma, GOC.plusDays(ngay), ban, soDanhGia, tongSao, thich);
    }

    private static Diem diem(DiemPhoBien_Policy.KetQua kq, long ma) {
        return kq.xepHang().stream().filter(d -> d.ungVien().maSanPham() == ma).findFirst().orElseThrow();
    }

    @Test
    void banNhieuNhungDanhGiaThapVanBiSanPhamCanBangVuotQua() {
        // A bán chạy nhất nhưng 10 đánh giá 1 sao; B bán ít hơn, 10 đánh giá 5 sao, nhiều lượt thích; C không có gì.
        var kq = DiemPhoBien_Policy.xepHang(List.of(
            sp(1, 1, 100, 10, 10, 0),
            sp(2, 2, 60, 10, 50, 8),
            sp(3, 3, 0, 0, 0, 0)), 20, 60);
        // m = 60/20 = 3. A: bán 1.0; Bayes (15+10)/15 = 1.667 -> (0.667)/4 = 0.1667; thích 0.
        Diem a = diem(kq, 1);
        assertThat(a.daBanChuanHoa()).isEqualTo(1.0);
        assertThat(a.bayes()).isCloseTo(25.0 / 15, within(1e-9));
        assertThat(a.diem()).isCloseTo(0.5 + 0.3 * ((25.0 / 15 - 1) / 4), within(1e-9));
        // B: bán 0.6; Bayes (15+50)/15 = 4.333 -> 0.8333; thích 1.0.
        Diem b = diem(kq, 2);
        assertThat(b.daBanChuanHoa()).isCloseTo(0.6, within(1e-9));
        assertThat(b.diem()).isCloseTo(0.5 * 0.6 + 0.3 * ((65.0 / 15 - 1) / 4) + 0.2, within(1e-9));
        // C không có đánh giá: Bayes = m = 3 -> 0.5 ở thành phần đánh giá.
        assertThat(diem(kq, 3).bayes()).isCloseTo(3.0, within(1e-9));
        assertThat(kq.xepHang()).extracting(d -> d.ungVien().maSanPham()).containsExactly(2L, 1L, 3L);
        assertThat(kq.coDuLieu()).isTrue();
        assertThat(kq.diemTrungBinhCuaHang()).isEqualTo(3.0);
    }

    @Test
    void itDanhGiaBiKeoVeTrungBinhCuaHang() {
        // Một đánh giá 5 sao không thắng mười đánh giá 4.8 sao khi trung bình cửa hàng là 4.
        var kq = DiemPhoBien_Policy.xepHang(List.of(
            sp(1, 1, 0, 1, 5, 0),
            sp(2, 1, 0, 10, 48, 0)), 50, 200);
        Diem motDanhGia = diem(kq, 1);
        Diem muoiDanhGia = diem(kq, 2);
        assertThat(motDanhGia.bayes()).isCloseTo((5 * 4.0 + 5) / 6, within(1e-9)).isLessThan(4.2);
        assertThat(muoiDanhGia.bayes()).isCloseTo((5 * 4.0 + 48) / 15, within(1e-9));
        assertThat(muoiDanhGia.diem()).isGreaterThan(motDanhGia.diem());
        // Mọi ứng viên bán 0 và thích 0: mẫu số min-max = 0 -> hai thành phần đó bằng 0.
        assertThat(motDanhGia.daBanChuanHoa()).isZero();
        assertThat(motDanhGia.yeuThichChuanHoa()).isZero();
    }

    @Test
    void khongCoDuLieuThiBaoCuaHangMoiVaHoaDiemTheoNgayTaoMoiHon() {
        var kq = DiemPhoBien_Policy.xepHang(List.of(sp(1, 1, 0, 0, 0, 0), sp(2, 9, 0, 0, 0, 0), sp(3, 5, 0, 0, 0, 0)), 0, 0);
        assertThat(kq.coDuLieu()).isFalse();
        assertThat(kq.diemTrungBinhCuaHang()).isNull();
        assertThat(kq.xepHang()).allSatisfy(d -> {
            assertThat(d.diem()).isZero();
            assertThat(d.bayes()).isNull();
        });
        assertThat(kq.xepHang()).extracting(d -> d.ungVien().maSanPham()).containsExactly(2L, 3L, 1L);
        assertThat(DiemPhoBien_Policy.xepHang(List.of(), 0, 0).xepHang()).isEmpty();
    }

    @Test
    void trongSoCongLaiBangMot() {
        assertThat(DiemPhoBien_Policy.TRONG_SO_DA_BAN + DiemPhoBien_Policy.TRONG_SO_DANH_GIA + DiemPhoBien_Policy.TRONG_SO_YEU_THICH)
            .isCloseTo(1.0, within(1e-12));
        assertThat(DiemPhoBien_Policy.chuanHoa(5, 5, 5)).isZero();
        assertThat(DiemPhoBien_Policy.chuanHoa(7, 2, 12)).isEqualTo(0.5);
    }
}
