package com.beautyshop.service;

import org.springframework.security.access.prepost.PreAuthorize;
import com.beautyshop.dto.PhieuGiamGia_Form;
import com.beautyshop.dto.PhieuGiamGia_Form.LoaiGiamGia;
import com.beautyshop.repository.PhieuGiamGia_Repository;
import com.beautyshop.repository.PhieuGiamGia_Repository.CauHinh;
import com.beautyshop.repository.PhieuGiamGia_Repository.ThongTin;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Comparator;
import com.beautyshop.repository.GioHang_Repository.MucGio;
import com.beautyshop.repository.PhieuGiamGia_Repository.Chieu;
import com.beautyshop.shipping.ShippingFee_Policy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.beautyshop.dto.BoLocDanhSachAdmin;
import com.beautyshop.dto.PhanTrang;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PhieuGiamGia_Service {
    private final SuKienQuanTri_Service audit;
    private final PhieuGiamGia_Repository repository;

    PhieuGiamGia_Service(PhieuGiamGia_Repository repository, SuKienQuanTri_Service audit) {
        this.audit = audit;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<ThongTin> danhSachAdmin() {
        return repository.danhSach();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ThongTin> danhSachAdmin(BoLocDanhSachAdmin loc, Pageable pageable) {
        return PhanTrang.doc(pageable, trang -> repository.docTrang(loc.mauTim(), loc.active(), trang));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PhieuGiamGia_Form docFormAdmin(long ma) {
        var cauHinh = timTheoMa(ma, false).cauHinh();
        var form = new PhieuGiamGia_Form();
        form.setMaApDung(cauHinh.maApDung());
        form.setLoaiGiamGia(LoaiGiamGia.valueOf(cauHinh.loaiGiamGia()));
        form.setGiaTriGiam(cauHinh.giaTriGiam());
        form.setMucGiamToiDa(cauHinh.mucGiamToiDa());
        form.setDonToiThieu(cauHinh.donToiThieu());
        form.setGioiHanSuDung(cauHinh.gioiHanSuDung());
        form.setBatDauLuc(cauHinh.batDauLuc().truncatedTo(ChronoUnit.MILLIS));
        form.setKetThucLuc(cauHinh.ketThucLuc().truncatedTo(ChronoUnit.MILLIS));
        form.setDangHoatDong(cauHinh.dangHoatDong());
        var pv = repository.phamVi(ma);
        form.setSlot(PhieuGiamGia_Form.Slot.valueOf(pv.slot()));
        form.setGioiHanKhach(pv.gioiHanKhach());
        form.setSanPham(hienIds(pv.dich().get(Chieu.SAN_PHAM)));
        form.setDanhMuc(hienIds(pv.dich().get(Chieu.DANH_MUC)));
        form.setThuongHieu(hienIds(pv.dich().get(Chieu.THUONG_HIEU)));
        form.setNguoiDung(hienIds(pv.dich().get(Chieu.NGUOI_DUNG)));
        return form;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void luuAdmin(Long ma, PhieuGiamGia_Form form, BindingResult errors) {
        String maApDung = chuanHoa(form.getMaApDung());
        if (maApDung.length() > 80) {
            errors.rejectValue("maApDung", "voucher.code", "Mã giảm giá sau khi định dạng dài quá 80 ký tự.");
        }
        if (form.getLoaiGiamGia() == LoaiGiamGia.PERCENT && form.getGiaTriGiam().compareTo(BigDecimal.valueOf(100)) > 0) {
            errors.rejectValue("giaTriGiam", "voucher.percent", "Phần trăm giảm phải từ 1 đến 100.");
        }
        var hienTai = ma == null ? null : timTheoMa(ma, true);
        var cauHinhCu = hienTai == null ? null : hienTai.cauHinh();
        LocalDateTime batDauLuc = giuDoChinhXac(form.getBatDauLuc(), cauHinhCu == null ? null : cauHinhCu.batDauLuc());
        LocalDateTime ketThucLuc = giuDoChinhXac(form.getKetThucLuc(), cauHinhCu == null ? null : cauHinhCu.ketThucLuc());
        kiemTraNam(batDauLuc, "batDauLuc", errors);
        kiemTraNam(ketThucLuc, "ketThucLuc", errors);
        if (!ketThucLuc.isAfter(batDauLuc)) {
            errors.rejectValue("ketThucLuc", "voucher.end", "Thời điểm kết thúc phải sau thời điểm bắt đầu.");
        }
        if (errors.hasErrors()) {
            return;
        }
        var dich = new java.util.EnumMap<Chieu,java.util.Set<Long>>(Chieu.class);
        try {
            dich.put(Chieu.SAN_PHAM,docIds(form.getSanPham()));
            dich.put(Chieu.DANH_MUC,docIds(form.getDanhMuc()));
            dich.put(Chieu.THUONG_HIEU,docIds(form.getThuongHieu()));
            dich.put(Chieu.NGUOI_DUNG,docIds(form.getNguoiDung()));
        } catch (NumberFormatException e) {
            errors.reject("voucher.scope","Nhập danh sách ID nguyên dương, cách nhau bằng dấu phẩy.");
            return;
        }
        var pv = new PhieuGiamGia_Repository.PhamVi(form.getSlot().name(), form.getGioiHanKhach(), dich);
        if (!repository.dichHopLe(pv) || (ma != null && !repository.quotaHopLe(ma,pv.gioiHanKhach()))) {
            errors.reject("voucher.scope","Đối tượng không hợp lệ hoặc quota khách thấp hơn số lượt đã giữ và dùng.");
            return;
        }
        var cauHinh = new CauHinh(maApDung, form.getLoaiGiamGia().name(), form.getGiaTriGiam(),
            form.getMucGiamToiDa(), form.getDonToiThieu(), form.getGioiHanSuDung(),
            batDauLuc, ketThucLuc, form.isDangHoatDong());
        if (ma == null) {
            var event = audit.batDau(SuKienQuanTri_Service.HanhDong.VOUCHER_CREATE, "VOUCHER", null);
            long id = repository.tao(cauHinh);
            repository.luuPhamVi(id,pv);
            audit.thanhCong(event, id, null, cauHinh.dangHoatDong());
            return;
        }
        if (cauHinh.gioiHanSuDung() < (long) hienTai.soLuotDaDung() + hienTai.soLuotTamGiu()) {
            errors.rejectValue("gioiHanSuDung", "voucher.limit", "Giới hạn phải đủ cho lượt đã dùng và đang giữ.");
            return;
        }
        var event = audit.batDau(SuKienQuanTri_Service.HanhDong.VOUCHER_UPDATE, "VOUCHER", ma);
        boolean doiPhamVi = repository.luuPhamVi(ma,pv);
        if (repository.sua(ma, cauHinh) || doiPhamVi) {
            audit.thanhCong(event, ma, cauHinhCu.dangHoatDong(), cauHinh.dangHoatDong());
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void xoaAdmin(long ma) {
        var truoc = timTheoMa(ma, true).cauHinh();
        var event = audit.batDau(SuKienQuanTri_Service.HanhDong.VOUCHER_DELETE, "VOUCHER", ma);
        if (!repository.xoaChuaSuDung(ma)) {
            throw new IllegalStateException("Mã giảm giá đã gắn với đơn hàng hoặc có lượt sử dụng. Hãy tắt mã này.");
        }
        audit.thanhCong(event, ma, truoc.dangHoatDong(), null);
    }

    private String hienIds(java.util.Set<Long> ids) {
        return ids.stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
    }

    private java.util.Set<Long> docIds(String text) {
        if (text == null || text.isBlank()) return java.util.Set.of();
        var ids = new java.util.TreeSet<Long>();
        for (String value : text.split(",", -1)) {
            long id = Long.parseLong(value.strip());
            if (id <= 0) throw new NumberFormatException();
            ids.add(id);
        }
        return ids;
    }

    private ThongTin timTheoMa(long ma, boolean khoa) {
        return repository.timTheoMa(ma, khoa).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void kiemTraNam(LocalDateTime thoiDiem, String field, BindingResult errors) {
        if (thoiDiem.getYear() < 1000 || thoiDiem.getYear() > 9999) {
            errors.rejectValue(field, "voucher.dateRange", "Chọn năm từ 1000 đến 9999.");
        }
    }

    private LocalDateTime giuDoChinhXac(LocalDateTime daNhap, LocalDateTime hienTai) {
        // datetime-local hiển thị tới mili giây. Giữ phần micro giây nếu Admin không sửa giờ.
        if (hienTai != null && hienTai.truncatedTo(ChronoUnit.MILLIS).equals(daNhap)) {
            return hienTai;
        }
        return daNhap.truncatedTo(ChronoUnit.MICROS);
    }

    private String chuanHoa(String ma) {
        return ma.strip().toUpperCase(Locale.ROOT);
    }

    public record GiamGia(Long maVoucher, BigDecimal soTien, String dieuKien) {
        public GiamGia(Long maVoucher, BigDecimal soTien) { this(maVoucher,soTien,null); }
    }
    public record BaoGia(GiamGia hang, GiamGia ship, BigDecimal phiChuan, BigDecimal tong, Map<Long,BigDecimal> phanBo) {}
    private static final GiamGia KHONG_GIAM = new GiamGia(null, BigDecimal.ZERO);

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public BaoGia xemGio(String hang, String ship, Long khach, List<MucGio> muc) {
        return tinhGio(hang, ship, khach, muc, null);
    }

    public BaoGia chotGio(String hang, String ship, long khach, List<MucGio> muc) {
        var daKhoa = coMa(hang) || coMa(ship)
            ? repository.khoaMa(hang == null ? "" : chuanHoa(hang), ship == null ? "" : chuanHoa(ship)) : Set.<Long>of();
        return tinhGio(hang, ship, khach, muc, daKhoa);
    }

    private boolean coMa(String ma) { return ma != null && !ma.isBlank(); }

    private BaoGia tinhGio(String hang, String ship, Long khach, List<MucGio> muc, Set<Long> daKhoa) {
        var phanBo = new HashMap<Long,BigDecimal>();
        var giamHang = tinhSlot(hang, "HANG", khach, muc, phanBo, null, daKhoa);
        var sauGiam = muc.stream().map(MucGio::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add).subtract(giamHang.soTien());
        var phi = ShippingFee_Policy.calculate(sauGiam);
        var giamShip = KHONG_GIAM;
        if (phi.signum() > 0 && coMa(ship)) {
            if (coMa(hang) && chuanHoa(hang).equals(chuanHoa(ship))) {
                throw new MaVanChuyenKhongHopLe("Mỗi mã chỉ được dùng ở một ô.");
            }
            try {
                giamShip = tinhSlot(ship,"VAN_CHUYEN",khach,muc,phanBo,phi,daKhoa);
            } catch (IllegalArgumentException e) {
                throw new MaVanChuyenKhongHopLe(e.getMessage());
            }
        }
        return new BaoGia(giamHang, giamShip, phi, sauGiam.add(phi).subtract(giamShip.soTien()), Map.copyOf(phanBo));
    }

    public static final class MaVanChuyenKhongHopLe extends IllegalArgumentException {
        MaVanChuyenKhongHopLe(String thongBao) { super(thongBao); }
    }

    private GiamGia tinhSlot(String code, String slot, Long khach, List<MucGio> muc, Map<Long,BigDecimal> phanBo,
                             BigDecimal phi, Set<Long> daKhoa) {
        if (!coMa(code)) return KHONG_GIAM;
        var phieu = timHopLe(code);
        if (daKhoa != null && !daKhoa.contains(phieu.ma()))
            throw new IllegalStateException("Mã giảm giá vừa thay đổi. Vui lòng báo giá lại.");
        var pv = repository.phamVi(phieu.ma());
        if (!slot.equals(pv.slot()) || !pv.choPhep(Chieu.NGUOI_DUNG, khach))
            throw new IllegalArgumentException("Mã không áp dụng cho khách hàng hoặc loại ưu đãi này.");
        if (khach != null && pv.gioiHanKhach() != null && repository.luotKhach(phieu.ma(),khach) >= pv.gioiHanKhach())
            throw new IllegalArgumentException("Đã hết lượt mã giảm giá dành cho bạn.");
        var dong = muc.stream().filter(m -> pv.choPhep(Chieu.SAN_PHAM,m.sanPham())
            && pv.choPhep(Chieu.DANH_MUC,m.danhMuc()) && pv.choPhep(Chieu.THUONG_HIEU,m.thuongHieu())).toList();
        var duDieuKien = dong.stream().map(m -> m.thanhTien().subtract(phanBo.getOrDefault(m.maBienThe(),BigDecimal.ZERO)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dong.isEmpty() || duDieuKien.compareTo(phieu.toiThieu()) < 0)
            throw new IllegalArgumentException("Tiền hàng chưa đạt mức tối thiểu để dùng mã giảm giá này.");
        var giam = soTien(phieu, phi == null ? duDieuKien : phi);
        if (phi == null) phanBo.putAll(phanBo(dong, duDieuKien, giam));
        String nhan = phi == null ? "Mã hàng" : "Mã vận chuyển";
        String dieuKien = nhan + " áp dụng trên " + dong.size() + " dòng hàng đủ điều kiện. "
            + "Tiền hàng hợp lệ " + duDieuKien.toPlainString() + " ₫, mức tối thiểu " + phieu.toiThieu().toPlainString() + " ₫.";
        return new GiamGia(phieu.ma(), giam, dieuKien);
    }

    static Map<Long,BigDecimal> phanBo(List<MucGio> dong, BigDecimal tong, BigDecimal giam) {
        var ketQua = new HashMap<Long,BigDecimal>();
        if (giam.signum() == 0) return ketQua;
        var phanDu = new HashMap<Long,BigDecimal>();
        BigDecimal conLai = giam;
        for (var m : dong) {
            var chia = m.thanhTien().multiply(giam).divideAndRemainder(tong);
            ketQua.put(m.maBienThe(),chia[0]);
            phanDu.put(m.maBienThe(),chia[1]);
            conLai = conLai.subtract(chia[0]);
        }
        phanDu.keySet().stream().sorted(Comparator.<Long,BigDecimal>comparing(phanDu::get).reversed()
            .thenComparingLong(Long::longValue)).limit(conLai.longValueExact())
            .forEach(id -> ketQua.merge(id,BigDecimal.ONE,BigDecimal::add));
        return ketQua;
    }

    private PhieuGiamGia_Repository.Phieu timHopLe(String ma) {
        String code = chuanHoa(ma);
        return repository.timPhieuHopLe(code).orElseThrow(() -> new IllegalArgumentException(
            repository.lyDoKhongTheXemTruoc(code).orElse("Mã giảm giá không hợp lệ.")));
    }

    private BigDecimal soTien(PhieuGiamGia_Repository.Phieu phieu, BigDecimal coSo) {
        var giam = switch (phieu.loai()) {
            case "FIXED" -> phieu.giaTri();
            case "PERCENT" -> coSo.multiply(phieu.giaTri()).movePointLeft(2).setScale(0,RoundingMode.DOWN);
            default -> throw new IllegalArgumentException("Loại giảm giá không hợp lệ.");
        };
        return giam.min(phieu.toiDa() == null ? coSo : phieu.toiDa()).min(coSo);
    }

    public void ghiLuot(long maDon, BaoGia gia, boolean online) {
        ghiSlot(maDon, gia.hang(), "HANG", online);
        ghiSlot(maDon, gia.ship(), "VAN_CHUYEN", online);
    }

    private void ghiSlot(long don, GiamGia giam, String slot, boolean online) {
        if (giam.maVoucher() != null && !repository.ghiSlot(don, giam.maVoucher(), slot, giam.soTien(), online)) {
            throw new IllegalArgumentException("Đã hết lượt mã giảm giá dành cho bạn.");
        }
    }

    public void traLuotCOD(long maDon) { repository.traLuotCOD(maDon); }

    public void ketThucTamGiu(long maDon, Long maVoucher, boolean suDung) {
        var ids = repository.cacPhieu(maDon);
        if (maVoucher != null && !ids.contains(maVoucher)) throw new IllegalStateException("Thiếu chứng từ giữ mã hàng.");
        for (long id : ids) {
            repository.timTheoMa(id,true);
            if (!repository.ketThucTamGiu(maDon,id,suDung)) throw new IllegalStateException("Không thể cập nhật lượt đã giữ.");
        }
    }
}
