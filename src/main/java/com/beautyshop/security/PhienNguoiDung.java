package com.beautyshop.security;

import com.beautyshop.entity.NguoiDung;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public final class PhienNguoiDung extends User {
    private final long maNguoiDung;
    private final long phienBanXacThuc;

    PhienNguoiDung(NguoiDung nguoiDung) {
        super(nguoiDung.getEmail(), nguoiDung.getMatKhauBam(), nguoiDung.isKichHoat(),
            true, true, true, List.of(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().name())));
        this.maNguoiDung = nguoiDung.getMaNguoiDung();
        this.phienBanXacThuc = nguoiDung.getPhienBanXacThuc();
    }

    public long getMaNguoiDung() { return maNguoiDung; }
    public long getPhienBanXacThuc() { return phienBanXacThuc; }
}
