package com.beautyshop.service;

import com.beautyshop.dto.DanhGia_Form;
import com.beautyshop.repository.DanhGia_Repository;
import com.beautyshop.repository.DanhGia_Repository.MucMua;
import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.repository.DoiTra_Repository;
import com.beautyshop.repository.YeuThich_Repository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P23: quy tắc đánh giá ở tầng service với store giả trong bộ nhớ (cùng hợp đồng với MongoDanhGia_Store):
 * BR16 kiểm ở MariaDB trước khi ghi, sửa thì ẩn chờ duyệt lại, Admin duyệt sai phiên bản bị từ chối, nội dung quá dài bị chặn.
 */
class SauMuaDanhGiaServiceTest {
    private static final long KHACH = 7;
    private static final long MUC_DA_GIAO = 100;
    private static final long MUC_DANG_GIAO = 101;
    private final DanhGia_Repository danhGia = mock(DanhGia_Repository.class);
    private final KhoGia kho = new KhoGia();
    private final SauMua_Service service = new SauMua_Service(danhGia, kho, mock(DoiTra_Repository.class),
        mock(YeuThich_Repository.class));

    @BeforeEach
    void mucMua() {
        when(danhGia.mucMua(MUC_DA_GIAO, KHACH)).thenReturn(Optional.of(
            new MucMua(30, "Serum", "30 ml", 9, "DELIVERED", 300, "Thịnh P.")));
        when(danhGia.mucMua(MUC_DANG_GIAO, KHACH)).thenReturn(Optional.of(
            new MucMua(30, "Serum", "30 ml", 10, "SHIPPING", 300, "Thịnh P.")));
        // Dòng đơn của người khác: truy vấn có điều kiện chủ đơn nên không trả gì.
        when(danhGia.mucMua(MUC_DA_GIAO, 8L)).thenReturn(Optional.empty());
    }

    private static DanhGia_Form form(Integer soSao, String noiDung) {
        var form = new DanhGia_Form();
        form.setSoSao(soSao);
        form.setNoiDung(noiDung);
        return form;
    }

    @Test
    void br16DonDaGiaoCuaChinhNguoiMuaThiTaoChoDuyet() {
        long maDon = service.luuDanhGia(KHACH, MUC_DA_GIAO, form(5, "  Thấm nhanh  "), false);

        assertThat(maDon).isEqualTo(9);
        var dg = kho.theoMuc.get(MUC_DA_GIAO);
        assertThat(dg.hienThi).isFalse();
        assertThat(dg.noiDung).isEqualTo("Thấm nhanh");
        assertThat(dg.tenHienThi).isEqualTo("Thịnh P.");
        assertThat(dg.maSanPham).isEqualTo(30);
        assertThat(service.danhGiaSanPham(30, 0).danhSach()).isEmpty();
    }

    @Test
    void br16ChuaGiaoHoacKhongPhaiChuDonThiKhongGhiStore() {
        assertThatThrownBy(() -> service.luuDanhGia(KHACH, MUC_DANG_GIAO, form(5, ""), false))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.luuDanhGia(8, MUC_DA_GIAO, form(5, ""), false))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.xemDanhGia(KHACH, MUC_DANG_GIAO)).isInstanceOf(ResponseStatusException.class);
        assertThat(kho.theoMuc).isEmpty();
        assertThat(kho.soLanGoiGhi).isZero();
    }

    @Test
    void moiDongDonChiMotDanhGia() {
        service.luuDanhGia(KHACH, MUC_DA_GIAO, form(5, ""), false);
        assertThatThrownBy(() -> service.luuDanhGia(KHACH, MUC_DA_GIAO, form(4, ""), false))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(kho.theoMuc.get(MUC_DA_GIAO).soSao).isEqualTo(5);
    }

    @Test
    void suaThiAnChoDuyetLaiVaAdminDuyetSaiPhienBanBiTuChoi() {
        service.luuDanhGia(KHACH, MUC_DA_GIAO, form(5, "Ban đầu"), false);
        var dg = kho.theoMuc.get(MUC_DA_GIAO);
        String phienBanDaXem = String.valueOf(dg.phienBan);
        service.datHienThi(dg.ma, true, phienBanDaXem);
        assertThat(service.danhGiaSanPham(30, 0).tomTat().soLuong()).isEqualTo(1);

        service.luuDanhGia(KHACH, MUC_DA_GIAO, form(2, "Đã sửa"), true);
        assertThat(dg.hienThi).isFalse();
        assertThat(dg.daDuyet).isFalse();
        var trang = service.danhGiaSanPham(30, 0);
        assertThat(trang.tomTat().soLuong()).isZero();
        assertThat(trang.danhSach()).isEmpty();

        // Admin vẫn cầm phiên bản cũ (đã xem trước khi khách sửa) -> không được duyệt nội dung mới chưa xem.
        assertThatThrownBy(() -> service.datHienThi(dg.ma, true, phienBanDaXem))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("đã thay đổi");
        assertThat(dg.hienThi).isFalse();
        service.datHienThi(dg.ma, true, String.valueOf(dg.phienBan));
        assertThat(service.danhGiaSanPham(30, 0).danhSach().getContent()).singleElement()
            .satisfies(c -> assertThat(c.noiDung()).isEqualTo("Đã sửa"));
    }

    @Test
    void phienBanKhongHopLeHoacThieuBiChanTruocKhiGoiStore() {
        assertThatThrownBy(() -> service.datHienThi("abc", true, null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.datHienThi("abc", true, "0; drop")).isInstanceOf(IllegalStateException.class);
        assertThat(kho.soLanGoiGhi).isZero();
    }

    @Test
    void noiDungQuaDaiHoacSoSaoNgoaiKhoangBiChan() {
        assertThatThrownBy(() -> service.luuDanhGia(KHACH, MUC_DA_GIAO, form(5, "a".repeat(1001)), false))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1.000");
        assertThatThrownBy(() -> service.luuDanhGia(KHACH, MUC_DA_GIAO, form(0, ""), false))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.luuDanhGia(KHACH, MUC_DA_GIAO, form(null, ""), false))
            .isInstanceOf(IllegalArgumentException.class);
        // Đúng 1.000 ký tự sau khi bỏ khoảng trắng hai đầu thì hợp lệ.
        service.luuDanhGia(KHACH, MUC_DA_GIAO, form(5, "  " + "a".repeat(1000) + "  "), false);
        assertThat(kho.theoMuc.get(MUC_DA_GIAO).noiDung).hasSize(1000);
    }

    @Test
    void khoDanhGiaLoiThiTrangSanPhamVaTheVanChayCoCoBao() {
        kho.loi = true;
        var trang = service.danhGiaSanPham(30, 0);
        assertThat(trang.khongTaiDuoc()).isTrue();
        assertThat(trang.tomTat().soLuong()).isZero();
        assertThat(service.tomTatDanhGia(List.of(30L))).isEmpty();
        assertThat(service.xemDanhGia(KHACH, MUC_DA_GIAO).khongTaiDuoc()).isTrue();
        // Ghi lỗi -> DataAccessException cho controller báo lỗi thân thiện; Admin nhận thông báo, không 500.
        assertThatThrownBy(() -> service.luuDanhGia(KHACH, MUC_DA_GIAO, form(5, ""), false))
            .isInstanceOf(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> service.datHienThi("abc", false, null))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("Tạm thời");
    }

    /** Store giả: giữ đúng hợp đồng của DanhGia_Store (khóa duy nhất theo dòng đơn, phiên bản tăng khi sửa). */
    static final class KhoGia implements DanhGia_Store {
        final Map<Long, Doc> theoMuc = new LinkedHashMap<>();
        boolean loi;
        int soLanGoiGhi;
        private int dem;

        static final class Doc {
            String ma;
            long maMuc;
            long maDon;
            long maNguoiDung;
            long maSanPham;
            String tenHienThi;
            int soSao;
            String noiDung;
            boolean hienThi;
            boolean daDuyet;
            long phienBan;
            Instant ngayTao;
        }

        private void kiemLoi() {
            if (loi) {
                throw new DataAccessResourceFailureException("kho tat");
            }
        }

        @Override
        public Optional<DanhGiaCuaKhach> cuaChuMuc(long maChiTietDonHang, long maNguoiDung) {
            kiemLoi();
            return Optional.ofNullable(theoMuc.get(maChiTietDonHang)).filter(d -> d.maNguoiDung == maNguoiDung)
                .map(d -> new DanhGiaCuaKhach(d.ma, d.soSao, d.noiDung, d.hienThi));
        }

        @Override
        public void tao(DanhGiaMoi moi) {
            kiemLoi();
            soLanGoiGhi++;
            if (theoMuc.containsKey(moi.maChiTietDonHang())) {
                throw new DuplicateKeyException("trung");
            }
            var d = new Doc();
            d.ma = "dg" + (++dem);
            d.maMuc = moi.maChiTietDonHang();
            d.maDon = moi.maDonHang();
            d.maNguoiDung = moi.maNguoiDung();
            d.maSanPham = moi.maSanPham();
            d.tenHienThi = moi.tenHienThi();
            d.soSao = moi.soSao();
            d.noiDung = moi.noiDung();
            d.ngayTao = Instant.parse("2026-11-20T00:00:00Z").plusSeconds(dem);
            theoMuc.put(d.maMuc, d);
        }

        @Override
        public boolean sua(long maChiTietDonHang, long maNguoiDung, int soSao, String noiDung) {
            kiemLoi();
            soLanGoiGhi++;
            var d = theoMuc.get(maChiTietDonHang);
            if (d == null || d.maNguoiDung != maNguoiDung) {
                return false;
            }
            d.soSao = soSao;
            d.noiDung = noiDung;
            d.hienThi = false;
            d.daDuyet = false;
            d.phienBan++;
            return true;
        }

        @Override
        public Page<DanhGiaCongKhai> congKhai(long maSanPham, Pageable pageable) {
            kiemLoi();
            var ds = theoMuc.values().stream().filter(d -> d.maSanPham == maSanPham && d.hienThi)
                .sorted(Comparator.comparing((Doc d) -> d.ngayTao).reversed())
                .map(d -> new DanhGiaCongKhai(d.tenHienThi, d.soSao, d.noiDung, d.ngayTao)).toList();
            return new PageImpl<>(ds, pageable, ds.size());
        }

        @Override
        public Page<DanhGiaAdmin> danhSachAdmin(BoLocDanhGia boLoc, Pageable pageable) {
            kiemLoi();
            List<DanhGiaAdmin> ds = new ArrayList<>();
            for (var d : theoMuc.values()) {
                ds.add(new DanhGiaAdmin(d.ma, d.maSanPham, null, d.maDon, d.tenHienThi, d.soSao, d.noiDung, d.hienThi,
                    d.daDuyet, String.valueOf(d.phienBan), d.ngayTao));
            }
            return new PageImpl<>(ds, PageRequest.of(0, 20), ds.size());
        }

        @Override
        public boolean datHienThi(String ma, boolean hienThi, String phienBan) {
            kiemLoi();
            soLanGoiGhi++;
            var d = theoMuc.values().stream().filter(x -> x.ma.equals(ma)).findFirst().orElse(null);
            if (d == null || (hienThi && !String.valueOf(d.phienBan).equals(phienBan))) {
                return false;
            }
            d.hienThi = hienThi;
            d.daDuyet |= hienThi;
            d.phienBan++;
            return true;
        }

        @Override
        public Map<Long, TomTatDanhGia> tomTatNhieu(Collection<Long> maSanPham, Instant tuHoacNull) {
            kiemLoi();
            Map<Long, long[]> phanBo = new HashMap<>();
            for (var d : theoMuc.values()) {
                if (d.hienThi && maSanPham.contains(d.maSanPham)) {
                    phanBo.computeIfAbsent(d.maSanPham, k -> new long[5])[d.soSao - 1]++;
                }
            }
            Map<Long, TomTatDanhGia> kq = new HashMap<>();
            phanBo.forEach((ma, pb) -> {
                long n = 0;
                long tong = 0;
                for (int s = 1; s <= 5; s++) {
                    n += pb[s - 1];
                    tong += s * pb[s - 1];
                }
                kq.put(ma, new TomTatDanhGia(n, tong, pb));
            });
            return kq;
        }

        @Override
        public TomTatDanhGia tomTatCuaHang(Instant tu) {
            kiemLoi();
            return TomTatDanhGia.RONG;
        }

        @Override
        public ThongKeDanhGia thongKe(Instant a, Instant b) {
            kiemLoi();
            return ThongKeDanhGia.RONG;
        }
    }
}
