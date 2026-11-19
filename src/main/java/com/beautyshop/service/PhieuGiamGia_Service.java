package com.beautyshop.service;

import com.beautyshop.repository.PhieuGiamGia_Repository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class PhieuGiamGia_Service {
    private final PhieuGiamGia_Repository repository;

    PhieuGiamGia_Service(PhieuGiamGia_Repository repository) { this.repository = repository; }

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
        var phieu = repository.timPhieuHopLe(ma.strip().toUpperCase(Locale.ROOT), khoa)
            .orElseThrow(() -> new IllegalArgumentException("Voucher không hợp lệ, đã hết hạn hoặc hết lượt."));
        if (tamTinh.compareTo(phieu.toiThieu()) < 0) {
            throw new IllegalArgumentException("Đơn chưa đạt mức tối thiểu của voucher.");
        }
        BigDecimal giam = switch (phieu.loai()) {
            case "FIXED" -> phieu.giaTri();
            case "PERCENT" -> tamTinh.multiply(phieu.giaTri().min(BigDecimal.valueOf(100)))
                .movePointLeft(2).setScale(0, RoundingMode.DOWN);
            default -> throw new IllegalArgumentException("Loại voucher không hợp lệ.");
        };
        if (phieu.toiDa() != null) {
            giam = giam.min(phieu.toiDa());
        }
        return new GiamGia(phieu.ma(), giam.min(tamTinh));
    }

    public void hoanCOD(Long ma) {
        if (ma != null && !repository.hoanLuot(ma)) {
            throw new IllegalStateException("Không thể hoàn lượt voucher của đơn.");
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
            throw new IllegalStateException("Không thể cập nhật lượt voucher đã giữ.");
        }
    }

    public record GiamGia(Long maVoucher, BigDecimal soTien) {}
}
