package com.beautyshop.service;

import com.beautyshop.dto.DiaChiHienThi;
import com.beautyshop.dto.DiaChiNguoiDung_Form;
import com.beautyshop.model.DiaChiNguoiDung;
import com.beautyshop.model.NguoiDung;
import com.beautyshop.repository.DiaChiNguoiDung_Repository;
import com.beautyshop.repository.NguoiDung_Repository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DiaChiNguoiDung_Service {
    public static final int GIOI_HAN = 10;
    public static final String THONG_BAO_GIOI_HAN =
        "Bạn đã lưu tối đa " + GIOI_HAN + " địa chỉ. Hãy xóa một địa chỉ trước khi thêm.";

    public static class DatGioiHanDiaChi extends RuntimeException {
        public DatGioiHanDiaChi() {
            super(THONG_BAO_GIOI_HAN);
        }
    }

    private final DiaChiNguoiDung_Repository diaChiRepository;
    private final NguoiDung_Repository nguoiDungRepository;

    DiaChiNguoiDung_Service(DiaChiNguoiDung_Repository diaChiRepository, NguoiDung_Repository nguoiDungRepository) {
        this.diaChiRepository = diaChiRepository;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    public List<DiaChiHienThi> danhSach(long maNguoiDung) {
        return diaChiRepository.docDanhSach(maNguoiDung);
    }

    public DiaChiNguoiDung_Form form(long maNguoiDung, long maDiaChi) {
        return DiaChiNguoiDung_Form.from(timDiaChi(maNguoiDung, maDiaChi));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void luu(long maNguoiDung, Long maDiaChi, DiaChiNguoiDung_Form form) {
        var nguoiDung = khoaCustomer(maNguoiDung);
        var danhSach = diaChiRepository.findAllByNguoiDungMaNguoiDungOrderByMaDiaChiAsc(maNguoiDung);
        if (maDiaChi == null && danhSach.size() >= GIOI_HAN) throw new DatGioiHanDiaChi();
        var diaChi = maDiaChi == null ? new DiaChiNguoiDung(nguoiDung) : timDiaChi(maNguoiDung, maDiaChi);
        diaChi.capNhat(form.getTenNguoiNhan(), form.getSoDienThoai(), form.getDiaChiChiTiet());
        if (form.isLaMacDinh() || danhSach.stream().noneMatch(DiaChiNguoiDung::isLaMacDinh)) {
            danhSach.forEach(muc -> muc.datMacDinh(false));
            diaChi.datMacDinh(true);
        }
        diaChiRepository.save(diaChi);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void xoa(long maNguoiDung, long maDiaChi) {
        khoaCustomer(maNguoiDung);
        var diaChi = timDiaChi(maNguoiDung, maDiaChi);
        if (diaChi.isLaMacDinh()) {
            diaChiRepository.findAllByNguoiDungMaNguoiDungOrderByMaDiaChiAsc(maNguoiDung).stream()
                .filter(muc -> !muc.getMaDiaChi().equals(maDiaChi))
                .findFirst().ifPresent(muc -> muc.datMacDinh(true));
        }
        diaChiRepository.delete(diaChi);
    }

    private DiaChiNguoiDung timDiaChi(long maNguoiDung, long maDiaChi) {
        return diaChiRepository.findByMaDiaChiAndNguoiDungMaNguoiDung(maDiaChi, maNguoiDung)
            .orElseThrow(DiaChiNguoiDung_Service::khongTimThay);
    }

    private NguoiDung khoaCustomer(long maNguoiDung) {
        var nguoiDung = nguoiDungRepository.khoa(maNguoiDung).orElseThrow(DiaChiNguoiDung_Service::khongTimThay);
        if (!nguoiDung.isKichHoat() || nguoiDung.getVaiTro() != NguoiDung.VaiTro.CUSTOMER) {
            throw new AccessDeniedException("Phiên Customer không còn quyền thay đổi địa chỉ.");
        }
        return nguoiDung;
    }

    private static ResponseStatusException khongTimThay() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
