package com.beautyshop.service;

import com.beautyshop.dto.BoLocThongKe_Form;
import com.beautyshop.order.OrderStatus;
import com.beautyshop.payment.MomoGateway;
import com.beautyshop.payment.Momo_Settings;
import com.beautyshop.repository.DanhGia_Store;
import com.beautyshop.repository.ThongKe_Repository;
import com.beautyshop.repository.ThongKe_Repository.DonMoi;
import com.beautyshop.repository.ThongKe_Repository.CanhBaoThanhToan;
import com.beautyshop.repository.ThongKe_Repository.DoiTraCho;
import com.beautyshop.repository.ThongKe_Repository.NhomThanhToan;
import com.beautyshop.repository.ThongKe_Repository.SanPhamBanChay;
import com.beautyshop.repository.ThongKe_Repository.TachDoanhThu;
import com.beautyshop.repository.ThongKe_Repository.VoucherSapHetHan;
import com.beautyshop.repository.ThongKe_Repository.Ngay;
import com.beautyshop.repository.ThongKe_Repository.Nhom;
import com.beautyshop.repository.ThongKe_Repository.SkuSapHet;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;

/**
 * Số liệu Dashboard Admin, chỉ đọc. Mọi kỳ tính theo ngày lịch Asia/Ho_Chi_Minh; mốc UTC được suy ra trước khi truy vấn.
 * Định nghĩa từng số liệu: xem docs/reviews/P20-admin-dashboard.md.
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class ThongKe_Service {
    private static final Logger log = LoggerFactory.getLogger(ThongKe_Service.class);
    public static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int NGUONG_TON_MAC_DINH = 5;
    static final int SO_NGAY_TOI_DA = 366;
    private static final Map<String, Integer> KY_NHANH = Map.of("7d", 7, "30d", 30, "90d", 90);

    private final ThongKe_Repository repository;
    private final DanhGia_Store danhGia;
    private final Clock clock;
    private final String momoGateway;

    @Autowired
    public ThongKe_Service(ThongKe_Repository repository, DanhGia_Store danhGia, Optional<Momo_Settings> momo) {
        this(repository, danhGia, Clock.systemUTC(), momo.flatMap(Momo_Settings::gateway).map(MomoGateway::maGateway).orElse(null));
    }

    public ThongKe_Service(ThongKe_Repository repository, DanhGia_Store danhGia) {
        this(repository, danhGia, Clock.systemUTC());
    }

    ThongKe_Service(ThongKe_Repository repository, DanhGia_Store danhGia, Clock clock) {
        this(repository, danhGia, clock, null);
    }

    ThongKe_Service(ThongKe_Repository repository, DanhGia_Store danhGia, Clock clock, String momoGateway) {
        this.repository = repository;
        this.danhGia = danhGia;
        this.clock = clock;
        this.momoGateway = momoGateway;
    }

    private Optional<DanhGia_Store.ThongKeDanhGia> thongKeDanhGia(LocalDateTime a, LocalDateTime b) {
        try {
            return Optional.ofNullable(danhGia.thongKe(a.toInstant(ZoneOffset.UTC), b.toInstant(ZoneOffset.UTC)));
        } catch (DataAccessException exception) {
            log.warn("Dashboard: khong doc duoc thong ke danh gia: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    public LocalDate homNay() { return LocalDate.now(clock.withZone(MUI_GIO)); }

    /**
     * Xác định kỳ từ bộ lọc. Lỗi được ghi vào {@code errors} và trả kỳ mặc định 30 ngày để trang vẫn hiển thị.
     */
    public Ky xacDinhKy(BoLocThongKe_Form form, Errors errors) {
        LocalDate homNay = homNay();
        Ky macDinh = Ky.nhanh(homNay, "30d", 30);
        if (errors.hasFieldErrors("from") || errors.hasFieldErrors("to")) {
            errors.reject("ky.dinhDang", "Ngày phải có dạng yyyy-MM-dd. Đang hiển thị 30 ngày gần nhất.");
            return macDinh;
        }
        if (form.getFrom() != null || form.getTo() != null) {
            if (form.getFrom() == null || form.getTo() == null) {
                errors.reject("ky.thieu", "Cần nhập cả ngày bắt đầu và ngày kết thúc. Đang hiển thị 30 ngày gần nhất.");
                return macDinh;
            }
            if (form.getFrom().isAfter(form.getTo())) {
                errors.reject("ky.thuTu", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc. Đang hiển thị 30 ngày gần nhất.");
                return macDinh;
            }
            if (form.getTo().isAfter(homNay)) {
                errors.reject("ky.tuongLai", "Kỳ thống kê không được ở tương lai. Đang hiển thị 30 ngày gần nhất.");
                return macDinh;
            }
            if (ChronoUnit.DAYS.between(form.getFrom(), form.getTo()) + 1 > SO_NGAY_TOI_DA) {
                errors.reject("ky.dai", "Kỳ thống kê tối đa 366 ngày. Đang hiển thị 30 ngày gần nhất.");
                return macDinh;
            }
            return new Ky(form.getFrom(), form.getTo(), null);
        }
        String range = form.getRange() == null || form.getRange().isEmpty() ? "30d" : form.getRange();
        Integer soNgay = KY_NHANH.get(range);
        if (soNgay == null) {
            errors.reject("ky.range", "Kỳ chỉ nhận 7d, 30d hoặc 90d. Đang hiển thị 30 ngày gần nhất.");
            return macDinh;
        }
        return Ky.nhanh(homNay, range, soNgay);
    }

    public TongQuan tongQuan(Ky ky, int nguongTon) {
        Ky truoc = ky.kyTruoc();
        LocalDateTime pa = utc(truoc.tu()), a = utc(ky.tu()), b = utc(ky.den().plusDays(1));
        var doanhThu = repository.doanhThu(pa, a, b);
        var demDon = repository.demDon(pa, a, b);
        long[] khachMoi = repository.khachMoi(truoc.tu().atStartOfDay(), ky.tu().atStartOfDay(), ky.den().plusDays(1).atStartOfDay());
        LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneOffset.UTC));
        var hienTai = repository.hienTai(now);
        var daGiao = repository.demDonDaGiao(pa, a, b);

        BigDecimal aov = trungBinh(doanhThu.hienTai(), doanhThu.soDonThu());
        BigDecimal aovTruoc = trungBinh(doanhThu.kyTruoc(), doanhThu.soDonThuKyTruoc());
        BigDecimal tiLeHuy = tiLe(demDon.soHuy(), demDon.soDon());
        BigDecimal tiLeHuyTruoc = tiLe(demDon.soHuyKyTruoc(), demDon.soDonKyTruoc());
        List<Kpi> kpi = List.of(
            Kpi.tien("doanh-thu", "Doanh thu thực thu", doanhThu.hienTai(), doanhThu.kyTruoc(),
                "Tính các khoản thanh toán thành công và khớp tổng tiền của đơn. Bỏ qua đơn đã hủy, hết hạn hoặc đang hoàn tiền."),
            Kpi.so("so-don", "Số đơn", demDon.soDon(), demDon.soDonKyTruoc(),
                "Đơn đặt trong kỳ theo ngày tạo, trừ đơn thanh toán trực tuyến chưa trả tiền hoặc hết hạn."),
            Kpi.tien("aov", "Giá trị đơn trung bình", aov, aovTruoc,
                "Doanh thu thực thu chia số đơn đã thu tiền trong kỳ."),
            Kpi.so("khach-moi", "Khách hàng mới", khachMoi[0], khachMoi[1],
                "Tài khoản khách hàng đăng ký trong kỳ."),
            Kpi.tiLe("ti-le-huy", "Tỉ lệ hủy đơn", tiLeHuy, tiLeHuyTruoc,
                "Đơn đã hủy chia số đơn đặt trong kỳ (cùng định nghĩa \"Số đơn\"). So sánh theo điểm phần trăm."),
            Kpi.so("da-giao", "Đơn đã giao", daGiao.hienTai(), daGiao.kyTruoc(),
                "Tính theo ngày giao thành công."));

        List<NgayThongKe> theoNgay = theoNgay(ky, repository.doanhThuTheoNgay(a, b), repository.donTheoNgay(a, b));
        Map<String, Nhom> trangThai = theoMa(repository.donTheoTrangThai(a, b));
        List<Nhom> theoTrangThai = Arrays.stream(OrderStatus.values())
            .map(s -> new Nhom(s.getLabel(), trangThai.getOrDefault(s.name(), new Nhom(s.name(), 0, BigDecimal.ZERO)).soLuong(),
                trangThai.getOrDefault(s.name(), new Nhom(s.name(), 0, BigDecimal.ZERO)).soTien()))
            .toList();
        Map<String, Nhom> phuongThuc = theoMa(repository.phuongThucThanhToan(a, b));
        List<Nhom> theoPhuongThuc = List.of(
            doiNhan(phuongThuc.get("COD"), "COD (thu khi giao)"),
            doiNhan(phuongThuc.get("MOMO"), "MoMo"));
        var thongKeDanhGia = thongKeDanhGia(a, b);
        List<Nhom> theoSao = new ArrayList<>();
        thongKeDanhGia.ifPresent(thongKe -> {
            for (int s = 5; s >= 1; s--) {
                theoSao.add(new Nhom(s + " sao", thongKe.phanBoTrongKy()[s - 1], BigDecimal.ZERO));
            }
        });
        List<DonGanDay> donMoi = repository.donMoiNhat(8).stream().map(this::donGanDay).toList();
        List<SkuSapHet> sapHet = repository.skuSapHet(nguongTon, 20);
        var sanPhamBanChay = repository.topSanPhamChiTiet(a, b, 5);
        var trangThaiThanhToan = repository.trangThaiThanhToan(a, b, now).stream()
            .collect(Collectors.toMap(NhomThanhToan::phuongThuc, Function.identity()));
        return new TongQuan(ky, truoc, kpi, hienTai.choXuLy(), theoNgay, theoTrangThai,
            sanPhamBanChay, repository.doanhThuTheoDanhMuc(a, b), theoPhuongThuc, theoSao,
            donMoi, sapHet, nguongTon, thongKeDanhGia.map(DanhGia_Store.ThongKeDanhGia::choDuyet).orElse(null),
            hienTai.doiTraCho(), hienTai.voucherSapHetHan(),
            demDon.soHuy(), repository.demSkuSapHet(nguongTon), repository.tachDoanhThu(a, b),
            repository.doanhThuTheoThuongHieu(a, b),
            List.of(trangThaiThanhToan.getOrDefault("COD", new NhomThanhToan("COD", 0, 0, 0)),
                trangThaiThanhToan.getOrDefault("MOMO", new NhomThanhToan("MOMO", 0, 0, 0))),
            repository.doiTraCho(), repository.canhBaoThanhToan(a, b, now, momoGateway), repository.voucherSapHetHan(now));
    }

    static LocalDateTime utc(LocalDate ngay) {
        return ngay.atStartOfDay(MUI_GIO).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private DonGanDay donGanDay(DonMoi don) {
        OrderStatus trangThai = OrderStatus.valueOf(don.trangThai());
        return new DonGanDay(don.maDonHang(), don.maHienThi(), don.khachHang(), don.tong(), trangThai, gioVietNam(don.ngayTaoUtc()));
    }

    private static List<NgayThongKe> theoNgay(Ky ky, List<Ngay> doanhThu, List<Ngay> don) {
        Map<LocalDate, Ngay> thu = doanhThu.stream().collect(Collectors.toMap(Ngay::ngay, Function.identity()));
        Map<LocalDate, Ngay> dat = don.stream().collect(Collectors.toMap(Ngay::ngay, Function.identity()));
        List<NgayThongKe> ketQua = new ArrayList<>();
        for (LocalDate ngay = ky.tu(); !ngay.isAfter(ky.den()); ngay = ngay.plusDays(1)) {
            Ngay t = thu.get(ngay), d = dat.get(ngay);
            ketQua.add(new NgayThongKe(ngay, t == null ? BigDecimal.ZERO : t.doanhThu(), d == null ? 0 : d.soDon()));
        }
        return ketQua;
    }

    private static Map<String, Nhom> theoMa(List<Nhom> nhom) {
        Map<String, Nhom> map = new LinkedHashMap<>();
        nhom.forEach(n -> map.put(n.nhan(), n));
        return map;
    }

    private static Nhom doiNhan(Nhom nhom, String nhan) {
        return nhom == null ? new Nhom(nhan, 0, BigDecimal.ZERO) : new Nhom(nhan, nhom.soLuong(), nhom.soTien());
    }

    static BigDecimal trungBinh(BigDecimal tong, long so) {
        return so == 0 ? BigDecimal.ZERO : tong.divide(BigDecimal.valueOf(so), 0, RoundingMode.HALF_UP);
    }

    static BigDecimal tiLe(long tu, long mau) {
        return mau == 0 ? null : BigDecimal.valueOf(tu * 100).divide(BigDecimal.valueOf(mau), 1, RoundingMode.HALF_UP);
    }

    /** Kỳ [tu, den] theo ngày lịch Việt Nam, gồm cả hai đầu. {@code range} khác null khi chọn kỳ nhanh. */
    public record Ky(LocalDate tu, LocalDate den, String range) {
        static Ky nhanh(LocalDate homNay, String range, int soNgay) {
            return new Ky(homNay.minusDays(soNgay - 1L), homNay, range);
        }
        public long soNgay() { return ChronoUnit.DAYS.between(tu, den) + 1; }
        public Ky kyTruoc() { return new Ky(tu.minusDays(soNgay()), tu.minusDays(1), null); }
    }

    public enum Huong { TANG, GIAM, KHONG_DOI, KHONG_SO_SANH }

    /**
     * Thẻ KPI. {@code thayDoi} là % so với kỳ trước (hoặc điểm % với tỉ lệ); null khi kỳ trước bằng 0/không có dữ liệu.
     */
    public record Kpi(String ma, String ten, String loai, BigDecimal giaTri, BigDecimal kyTruoc, BigDecimal thayDoi,
                      Huong huong, String chuThich) {
        static Kpi tien(String ma, String ten, BigDecimal giaTri, BigDecimal kyTruoc, String chuThich) {
            return tao(ma, ten, "TIEN", giaTri, kyTruoc, chuThich, false);
        }
        static Kpi so(String ma, String ten, long giaTri, long kyTruoc, String chuThich) {
            return tao(ma, ten, "SO", BigDecimal.valueOf(giaTri), BigDecimal.valueOf(kyTruoc), chuThich, false);
        }
        static Kpi tiLe(String ma, String ten, BigDecimal giaTri, BigDecimal kyTruoc, String chuThich) {
            return tao(ma, ten, "TI_LE", giaTri, kyTruoc, chuThich, true);
        }
        private static Kpi tao(String ma, String ten, String loai, BigDecimal giaTri, BigDecimal kyTruoc, String chuThich,
                               boolean diemPhanTram) {
            BigDecimal thayDoi = null;
            Huong huong = Huong.KHONG_SO_SANH;
            if (giaTri != null && kyTruoc != null && (diemPhanTram || kyTruoc.signum() != 0)) {
                thayDoi = diemPhanTram ? giaTri.subtract(kyTruoc).setScale(1, RoundingMode.HALF_UP)
                    : giaTri.subtract(kyTruoc).multiply(BigDecimal.valueOf(100)).divide(kyTruoc, 1, RoundingMode.HALF_UP);
                huong = thayDoi.signum() > 0 ? Huong.TANG : thayDoi.signum() < 0 ? Huong.GIAM : Huong.KHONG_DOI;
            }
            return new Kpi(ma, ten, loai, giaTri, kyTruoc, thayDoi == null ? null : thayDoi.abs(), huong, chuThich);
        }
        public boolean diemPhanTram() { return "TI_LE".equals(loai); }
    }

    public record NgayThongKe(LocalDate ngay, BigDecimal doanhThu, long soDon) {}
    public record DonGanDay(long maDonHang, String maHienThi, String khachHang, BigDecimal tong, OrderStatus trangThai,
                            LocalDateTime ngayTao) {}
    public record TongQuan(Ky ky, Ky kyTruoc, List<Kpi> kpi, long donChoXuLy, List<NgayThongKe> theoNgay,
                           List<Nhom> theoTrangThai, List<SanPhamBanChay> sanPhamBanChay, List<Nhom> theoDanhMuc,
                           List<Nhom> theoPhuongThuc, List<Nhom> theoSao, List<DonGanDay> donMoi,
                           List<SkuSapHet> skuSapHet, int nguongTon, Long danhGiaChoDuyet, long doiTraCho,
                           long voucherSapHetHan, long donBiHuy, long demSkuSapHet, TachDoanhThu tachDoanhThu,
                           List<Nhom> theoThuongHieu, List<NhomThanhToan> trangThaiThanhToan,
                           List<DoiTraCho> doiTraChoChiTiet, List<CanhBaoThanhToan> canhBaoThanhToan,
                           List<VoucherSapHetHan> voucherSapHetHanChiTiet) {
        public List<Nhom> topSanPham() {
            return sanPhamBanChay.stream().map(sp -> new Nhom(sp.nhan(), sp.soLuong(), sp.soTien())).toList();
        }
        public Kpi chiSo(String ma) {
            return kpi.stream().filter(k -> k.ma().equals(ma)).findFirst().orElseThrow();
        }
        public boolean coTrangThaiThanhToan() {
            return trangThaiThanhToan.stream().anyMatch(n -> n.daThu() + n.dangCho() + n.ketThuc() > 0);
        }
        public LocalDateTime gioVietNam(LocalDateTime utc) { return ThongKe_Service.gioVietNam(utc); }
        public boolean coDoanhThu() { return theoNgay.stream().anyMatch(n -> n.doanhThu().signum() > 0); }
        public boolean coDon() { return theoNgay.stream().anyMatch(n -> n.soDon() > 0); }
        public boolean coTrangThai() { return theoTrangThai.stream().anyMatch(n -> n.soLuong() > 0); }
        public boolean coThanhToan() { return theoPhuongThuc.stream().anyMatch(n -> n.soLuong() > 0); }
        public boolean coDanhGia() { return theoSao.stream().anyMatch(n -> n.soLuong() > 0); }
        public boolean taiDuocDanhGia() { return danhGiaChoDuyet != null; }
    }

    private static LocalDateTime gioVietNam(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(MUI_GIO).toLocalDateTime();
    }
}
