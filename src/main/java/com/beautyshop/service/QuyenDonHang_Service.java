package com.beautyshop.service;

import com.beautyshop.security.PhienNguoiDung;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuyenDonHang_Service {
    private final JdbcClient jdbc;

    QuyenDonHang_Service(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    public long kiemTraSoHuu(long maDonHang) {
        if (!(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof PhienNguoiDung nguoiDung)) {
            throw new AccessDeniedException("Không xác định được chủ tài khoản.");
        }
        jdbc.sql("SELECT ma_don_hang FROM don_hang WHERE ma_don_hang = ? AND ma_nguoi_dung = ?")
            .params(maDonHang, nguoiDung.getMaNguoiDung()).query(Long.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return nguoiDung.getMaNguoiDung();
    }
}
