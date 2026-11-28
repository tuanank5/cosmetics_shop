package com.beautyshop.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record LanThanhToan_Dto(Long maLan, String cong, BigDecimal soTien, String trangThai,
                               LocalDateTime ngayTao, LocalDateTime ngayThanhToan,
                               String maYeuCau, String maGiaoDich) {
    private static final String THIEU = "Chưa ghi nhận";
    private static final DateTimeFormatter GIO_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public String phuongThuc() {
        return switch (cong) {
            case "COD" -> "Thanh toán khi nhận hàng (COD)";
            case "MOMO" -> "MoMo";
            case "MOMO_MOCK" -> "MoMo mô phỏng";
            case null, default -> THIEU;
        };
    }

    public String nhanTrangThai() {
        for (var muc : BoLocDonHangAdmin.ThanhToan.values()) {
            if (muc.name().equals(trangThai)) return muc.nhan;
        }
        return "REFUNDED".equals(trangThai) ? "Đã hoàn tiền" : THIEU;
    }

    public static String gioVietNam(LocalDateTime utc) {
        return utc == null ? THIEU : GIO_VN.format(utc.plusHours(7));
    }
}
