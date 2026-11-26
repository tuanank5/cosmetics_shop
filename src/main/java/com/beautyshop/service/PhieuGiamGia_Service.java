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
    private final PhieuGiamGia_Repository repository;

    PhieuGiamGia_Service(PhieuGiamGia_Repository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<ThongTin> danhSachAdmin() {
        return repository.danhSach();
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
        var cauHinh = new CauHinh(maApDung, form.getLoaiGiamGia().name(), form.getGiaTriGiam(),
            form.getMucGiamToiDa(), form.getDonToiThieu(), form.getGioiHanSuDung(),
            batDauLuc, ketThucLuc, form.isDangHoatDong());
        if (ma == null) {
            repository.tao(cauHinh);
            return;
        }
        if (cauHinh.gioiHanSuDung() < (long) hienTai.soLuotDaDung() + hienTai.soLuotTamGiu()) {
            errors.rejectValue("gioiHanSuDung", "voucher.limit", "Giới hạn phải đủ cho lượt đã dùng và đang giữ.");
            return;
        }
        repository.sua(ma, cauHinh);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void xoaAdmin(long ma) {
        timTheoMa(ma, true);
        if (!repository.xoaChuaSuDung(ma)) {
            throw new IllegalStateException("Mã giảm giá đã gắn với đơn hàng hoặc có lượt sử dụng. Hãy tắt mã này.");
        }
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

    public GiamGia apDungCOD(String ma, BigDecimal tamTinh) {
        var giam = tinh(ma, tamTinh, true);
        if (giam.maVoucher() != null) {
            repository.ghiNhanSuDung(giam.maVoucher());
        }
        return giam;
    }

    @Transactional(readOnly = true)
    public GiamGia xemTruoc(String ma, BigDecimal tamTinh) {
        return tinh(ma, tamTinh, false);
    }

    private GiamGia tinh(String ma, BigDecimal tamTinh, boolean khoa) {
        if (ma == null || ma.isBlank()) {
            return new GiamGia(null, BigDecimal.ZERO);
        }
        var phieu = repository.timPhieuHopLe(chuanHoa(ma), khoa)
            .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không hợp lệ, đã hết hạn hoặc hết lượt sử dụng."));
        if (tamTinh.compareTo(phieu.toiThieu()) < 0) {
            throw new IllegalArgumentException("Tiền hàng chưa đạt mức tối thiểu để dùng mã giảm giá này.");
        }
        BigDecimal giam = switch (phieu.loai()) {
            case "FIXED" -> phieu.giaTri();
            case "PERCENT" -> tamTinh.multiply(phieu.giaTri().min(BigDecimal.valueOf(100)))
                .movePointLeft(2).setScale(0, RoundingMode.DOWN);
            default -> throw new IllegalArgumentException("Loại giảm giá không hợp lệ.");
        };
        if (phieu.toiDa() != null) {
            giam = giam.min(phieu.toiDa());
        }
        return new GiamGia(phieu.ma(), giam.min(tamTinh));
    }

    public void hoanCOD(Long ma) {
        if (ma != null && !repository.hoanLuot(ma)) {
            throw new IllegalStateException("Không thể trả lại lượt dùng mã giảm giá của đơn hàng.");
        }
    }

    public GiamGia giuOnline(String ma, BigDecimal tamTinh) {
        var giam = tinh(ma, tamTinh, true);
        if (giam.maVoucher() != null) {
            repository.tamGiu(giam.maVoucher());
        }
        return giam;
    }

    public void ghiTamGiu(long maDon, Long maVoucher) {
        if (maVoucher != null) {
            repository.ghiTamGiu(maDon, maVoucher);
        }
    }

    public void ketThucTamGiu(long maDon, Long maVoucher, boolean suDung) {
        if (maVoucher != null && !repository.ketThucTamGiu(maDon, maVoucher, suDung)) {
            throw new IllegalStateException("Không thể cập nhật lượt dùng mã giảm giá đã giữ.");
        }
    }

    public record GiamGia(Long maVoucher, BigDecimal soTien) {}
}
