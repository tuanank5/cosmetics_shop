package com.beautyshop.dto;

import com.beautyshop.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;

public record BoLocDonHangAdmin(List<OrderStatus> trangThai, PhuongThuc phuongThuc,
                               ThanhToan thanhToan, String maDon, SapXep sapXep, int kichThuoc,
                               LocalDate tuNgay, LocalDate denNgay) {
    public static final List<Integer> KICH_THUOC = List.of(10, 20, 50);

    public BoLocDonHangAdmin {
        trangThai = trangThai == null ? List.of() : trangThai.stream().distinct().toList();
        maDon = maDon == null ? "" : maDon.strip();
        if (maDon.length() > 80 || !maDon.matches("[A-Za-z0-9_-]*")) maDon = "";
        if (sapXep == null) sapXep = SapXep.newest;
        if (!KICH_THUOC.contains(kichThuoc)) kichThuoc = 20;
        if (tuNgay != null && denNgay != null && denNgay.isBefore(tuNgay)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
    }

    public BoLocDonHangAdmin(List<OrderStatus> trangThai, PhuongThuc phuongThuc, ThanhToan thanhToan,
                            String maDon, SapXep sapXep, int kichThuoc) {
        this(trangThai, phuongThuc, thanhToan, maDon, sapXep, kichThuoc, null, null);
    }

    public enum PhuongThuc {
        COD("COD"), MOMO("MoMo");

        public final String nhan;
        PhuongThuc(String nhan) { this.nhan = nhan; }
    }

    public enum ThanhToan {
        UNPAID("Chưa thu tiền"), PENDING("Chờ thanh toán"), FAILED("Thanh toán thất bại"),
        PAID("Đã thanh toán"), REFUND_PENDING("Chờ hoàn tiền");

        public final String nhan;
        ThanhToan(String nhan) { this.nhan = nhan; }

        public static String nhan(String ma) {
            for (var muc : values()) {
                if (muc.name().equals(ma)) return muc.nhan;
            }
            return "Chưa có giao dịch";
        }
    }

    public enum SapXep {
        newest("Mới nhất"), oldest("Cũ nhất"), totalDesc("Tổng tiền giảm dần"), totalAsc("Tổng tiền tăng dần");

        public final String nhan;
        SapXep(String nhan) { this.nhan = nhan; }
    }
}
