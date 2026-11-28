package com.beautyshop.service;

import org.springframework.security.access.prepost.PreAuthorize;
import com.beautyshop.dto.DangKy_Form;
import com.beautyshop.dto.HoSo_Form;
import com.beautyshop.model.NguoiDung;
import com.beautyshop.repository.NguoiDung_Repository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NguoiDung_Service {
    private final NguoiDung_Repository repository;
    private final PasswordEncoder passwordEncoder;

    private final SuKienQuanTri_Service audit;
    private final ThuTrangThaiTaiKhoan thu;

    NguoiDung_Service(NguoiDung_Repository repository, PasswordEncoder passwordEncoder,
                      SuKienQuanTri_Service audit, ThuTrangThaiTaiKhoan thu) {
        this.audit = audit;
        this.thu = thu;
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void dangKy(DangKy_Form form) {
        repository.saveAndFlush(new NguoiDung(chuanHoaEmail(form.getEmail()),
            passwordEncoder.encode(form.getMatKhau()), form.getHoTen().strip()));
    }

    public static String chuanHoaEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }


    // Kiểm tra email đã tồn tại trong hệ thống chưa
    @Transactional(readOnly = true)
    public boolean emailDaTonTai(String email) {
        return repository.findByEmail(chuanHoaEmail(email)).isPresent();
    }

    // Mã hóa mật khẩu trước khi lưu tạm trong Session
    public String bamMatKhau(String matKhau) {
        return passwordEncoder.encode(matKhau);
    }

    // Tạo tài khoản sau khi email đã xác thực OTP thành công
    @Transactional
    public void dangKyDaXacThuc(String email,String hoTen, String matKhauBam) {
        String emailChuanHoa = chuanHoaEmail(email);
        if (repository.findByEmail(emailChuanHoa).isPresent()) {
            throw new org.springframework.dao.DataIntegrityViolationException(
                    "Email đã được sử dụng."
            );
        }
        var nguoiDung = new NguoiDung(emailChuanHoa, matKhauBam, hoTen.strip());
        nguoiDung.xacNhanEmail(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        repository.saveAndFlush(nguoiDung);
    }

    @Transactional(readOnly = true)
    public HoSo_Form hoSo(long maNguoiDung) {
        var nguoiDung = repository.findById(maNguoiDung).orElseThrow(NguoiDung_Service::khongTimThay);
        var form = new HoSo_Form();
        form.setHoTen(nguoiDung.getHoTen());
        form.setSoDienThoai(nguoiDung.getSoDienThoai());
        return form;
    }

    @Transactional
    public void capNhatHoSo(long maNguoiDung, HoSo_Form form) {
        var nguoiDung = repository.khoa(maNguoiDung).orElseThrow(NguoiDung_Service::khongTimThay);
        if (!nguoiDung.isKichHoat() || nguoiDung.getVaiTro() != NguoiDung.VaiTro.CUSTOMER) {
            throw new AccessDeniedException("Phiên Customer không còn quyền cập nhật hồ sơ.");
        }
        String sdt = form.getSoDienThoai();
        nguoiDung.capNhatHoSo(form.getHoTen(), sdt == null || sdt.isEmpty() ? null : sdt);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<NguoiDung_Repository.TomTat> timNguoiDung(String q, Boolean kichHoat, NguoiDung.VaiTro vaiTro, Pageable pageable) {
        String mau = q.isEmpty() ? "" : "%" + q.toLowerCase(Locale.ROOT)
            .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return repository.timNguoiDung(mau, kichHoat, vaiTro, pageable);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public KetQua datTrangThai(long maMucTieu, boolean kichHoat, long version, UUID id, LyDoTaiKhoan lyDo) {
        var phien = SuKienQuanTri_Service.phienAdmin();
        long maAdmin = phien.getMaNguoiDung();
        var dau = repository.khoa(Math.min(maAdmin, maMucTieu)).orElseThrow(NguoiDung_Service::khongTimThay);
        var sau = maAdmin == maMucTieu ? dau
            : repository.khoa(Math.max(maAdmin, maMucTieu)).orElseThrow(NguoiDung_Service::khongTimThay);
        var admin = dau.getMaNguoiDung() == maAdmin ? dau : sau;
        if (admin.getVaiTro() != NguoiDung.VaiTro.ADMIN || !admin.isKichHoat()
            || admin.getPhienBanXacThuc() != phien.getPhienBanXacThuc()) {
            throw new AccessDeniedException("Phiên Admin không còn quyền thay đổi tài khoản.");
        }
        if (maAdmin == maMucTieu && !kichHoat) return KetQua.TU_KHOA;
        var mucTieu = dau.getMaNguoiDung() == maMucTieu ? dau : sau;
        if (id == null || lyDo == null || lyDo.isMoKhoa() != kichHoat) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy chọn lý do phù hợp.");
        }
        var bienNhan = thu.bienNhan(id, maAdmin, maMucTieu, version, lyDo);
        if (bienNhan == ThuTrangThaiTaiKhoan.BienNhan.DA_GHI) return KetQua.DA_GHI;
        if (bienNhan == ThuTrangThaiTaiKhoan.BienNhan.XUNG_DOT) return KetQua.FORM_CU;
        if (mucTieu.isKichHoat() == kichHoat) return KetQua.KHONG_DOI;
        if (mucTieu.getPhienBanXacThuc() != version) return KetQua.FORM_CU;
        var action = kichHoat ? SuKienQuanTri_Service.HanhDong.ACCOUNT_UNLOCK : SuKienQuanTri_Service.HanhDong.ACCOUNT_LOCK;
        var event = audit.batDau(action, "ACCOUNT", maMucTieu, id);
        mucTieu.datKichHoat(kichHoat);
        audit.thanhCongTaiKhoan(event, maMucTieu, kichHoat, lyDo);
        thu.tao(id, maAdmin, mucTieu, version, lyDo);
        return KetQua.DA_DOI;
    }

    public enum KetQua { DA_DOI, KHONG_DOI, DA_GHI, TU_KHOA, FORM_CU }

    private static ResponseStatusException khongTimThay() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
