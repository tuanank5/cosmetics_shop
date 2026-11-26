
package com.beautyshop.security;

import com.beautyshop.model.NguoiDung;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class PhienNguoiDung extends User implements OAuth2User {

    private final long maNguoiDung;
    private final long phienBanXacThuc;
    private final Map<String, Object> attributes;

    // Đăng nhập bằng email và mật khẩu
    PhienNguoiDung(NguoiDung nguoiDung) {
        this(nguoiDung, Map.of());
    }

    // Đăng nhập bằng Google hoặc GitHub
    PhienNguoiDung(NguoiDung nguoiDung, Map<String, Object> attributes) {
        this(nguoiDung, nguoiDung.getMatKhauBam(), attributes);
    }

    static PhienNguoiDung oauth(NguoiDung nguoiDung) {
        var phien = new PhienNguoiDung(nguoiDung, "", Map.of());
        phien.eraseCredentials();
        return phien;
    }

    private PhienNguoiDung(NguoiDung nguoiDung, String hash, Map<String, Object> attributes) {
        super(nguoiDung.getEmail(), hash, nguoiDung.isKichHoat(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().name()))
        );

        this.maNguoiDung = nguoiDung.getMaNguoiDung();
        this.phienBanXacThuc = nguoiDung.getPhienBanXacThuc();
        this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
    }

    public long getMaNguoiDung() {
        return maNguoiDung;
    }

    public long getPhienBanXacThuc() {
        return phienBanXacThuc;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(maNguoiDung);
    }
}
