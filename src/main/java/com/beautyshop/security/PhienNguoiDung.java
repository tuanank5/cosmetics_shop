package com.beautyshop.security;

import com.beautyshop.entity.NguoiDung;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public sealed class PhienNguoiDung extends User permits PhienOAuth2 {
    private final long maNguoiDung;
    private final long phienBanXacThuc;

    PhienNguoiDung(NguoiDung nguoiDung) {
        this(nguoiDung, nguoiDung.getMatKhauBam());
    }

    PhienNguoiDung(NguoiDung nguoiDung, String matKhauBam) {
        super(nguoiDung.getEmail(), matKhauBam, nguoiDung.isKichHoat(),
            true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().name())));
        this.maNguoiDung = nguoiDung.getMaNguoiDung();
        this.phienBanXacThuc = nguoiDung.getPhienBanXacThuc();
    }

    public long getMaNguoiDung() { return maNguoiDung; }
    public long getPhienBanXacThuc() { return phienBanXacThuc; }
}
