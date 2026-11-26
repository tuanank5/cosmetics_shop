package com.beautyshop.service;

import org.springframework.security.access.prepost.PreAuthorize;
import com.beautyshop.dto.DangKy_Form;
import com.beautyshop.dto.HoSo_Form;
import com.beautyshop.model.NguoiDung;
import com.beautyshop.repository.NguoiDung_Repository;
import java.util.Locale;
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

    NguoiDung_Service(NguoiDung_Repository repository, PasswordEncoder passwordEncoder) {
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
    public Page<NguoiDung_Repository.TomTat> timNguoiDung(String q, Pageable pageable) {
        String mau = q.isEmpty() ? "" : "%" + q.toLowerCase(Locale.ROOT)
            .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return repository.timNguoiDung(mau, pageable);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public boolean datTrangThai(long maAdmin, long phienBanXacThuc, long maMucTieu, boolean kichHoat) {
        if (maAdmin == maMucTieu && !kichHoat) {
            return false;
        }
        var dau = repository.khoa(Math.min(maAdmin, maMucTieu)).orElseThrow(NguoiDung_Service::khongTimThay);
        var sau = maAdmin == maMucTieu ? dau
            : repository.khoa(Math.max(maAdmin, maMucTieu)).orElseThrow(NguoiDung_Service::khongTimThay);
        var admin = dau.getMaNguoiDung() == maAdmin ? dau : sau;
        if (admin.getVaiTro() != NguoiDung.VaiTro.ADMIN || !admin.isKichHoat()
            || admin.getPhienBanXacThuc() != phienBanXacThuc) {
            throw new AccessDeniedException("Phiên Admin không còn quyền thay đổi tài khoản.");
        }
        var mucTieu = dau.getMaNguoiDung() == maMucTieu ? dau : sau;
        mucTieu.datKichHoat(kichHoat);
        return true;
    }

    private static ResponseStatusException khongTimThay() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
