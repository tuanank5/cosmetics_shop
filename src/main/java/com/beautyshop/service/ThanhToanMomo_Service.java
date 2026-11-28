package com.beautyshop.service;

import com.beautyshop.order.OrderStatus;
import com.beautyshop.payment.MomoGateway.KetQua;
import com.beautyshop.payment.MomoGateway.Request;
import com.beautyshop.payment.MomoSignature.Ipn;
import com.beautyshop.repository.DonHang_Repository;
import com.beautyshop.repository.DonHang_Repository.Don;
import com.beautyshop.repository.ThanhToanMomo_Repository;
import com.beautyshop.repository.ThanhToanMomo_Repository.LanThanhToan;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ThanhToanMomo_Service {
    // Các lỗi thanh toán cuối cùng theo bảng result code MoMo.
    private static final Set<Integer> THAT_BAI_CUOI = Set.of(1001, 1002, 1003, 1004, 1005, 1006, 1007, 1017, 1026, 4001, 4002, 4100);
    private final DonHang_Repository donHang;
    private final ThanhToanMomo_Repository thanhToan;
    private final TonKho_Service tonKho;
    private final PhieuGiamGia_Service voucher;

    ThanhToanMomo_Service(DonHang_Repository donHang, ThanhToanMomo_Repository thanhToan,
                         TonKho_Service tonKho, PhieuGiamGia_Service voucher) {
        this.donHang = donHang;
        this.thanhToan = thanhToan;
        this.tonKho = tonKho;
        this.voucher = voucher;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Request chuanBi(long maDon, String gateway) {
        var don = khoaDon(maDon);
        var lan = moiNhat(maDon, gateway);
        if (don.trangThai() != OrderStatus.PENDING_PAYMENT || !thanhToan.conHan(maDon)) {
            throw new IllegalStateException("Đơn đã hết thời gian thử lại. Vui lòng chờ xác nhận kết quả thanh toán.");
        }
        if (thanhToan.soLanCanHoan(maDon) > 0) {
            throw new IllegalStateException("Đơn có giao dịch cần đối soát. Vui lòng chờ xác nhận.");
        }
        if ("PENDING".equals(lan.trangThai())) {
            return lan.request();
        }
        if (!"FAILED".equals(lan.trangThai())) {
            throw new IllegalStateException("Lần thanh toán này cần được kiểm tra lại.");
        }
        String requestId = UUID.randomUUID().toString();
        thanhToan.tao(maDon, requestId, don.tong(), gateway);
        return new Request(maDon, don.maHienThi(), requestId, don.tong().longValueExact());
    }

    @Transactional(readOnly = true)
    public LanThanhToan moiNhat(long maDon, String gateway) {
        return thanhToan.moiNhat(maDon).filter(lan -> gateway.equals(lan.gateway()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void nhanIPN(Ipn ipn, String gateway) {
        var lan = theoRequest(ipn.requestId(), gateway);
        var request = lan.request();
        if (!request.orderId().equals(ipn.orderId()) || request.amount() != ipn.amount()
            || !request.orderInfo().equals(ipn.orderInfo()) || !"".equals(ipn.extraData())
            || !"momo_wallet".equals(ipn.orderType()) || ipn.responseTime() <= 0) {
            throw new IllegalArgumentException("IPN không khớp lần thanh toán.");
        }
        apDung(request, new KetQua(ipn.resultCode(), ipn.transId()), gateway);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void nhanDoiSoat(Request request, KetQua ketQua, String gateway) {
        apDung(request, ketQua, gateway);
    }

    private void apDung(Request request, KetQua ketQua, String gateway) {
        long maDon = request.maDonHang();
        Don don = khoaDon(maDon);
        var lan = theoRequest(request.requestId(), gateway);
        if (!request.equals(lan.request()) || don.tong().longValueExact() != request.amount()) {
            throw new IllegalArgumentException("Kết quả không khớp số tiền đơn.");
        }
        boolean moiNhat = moiNhat(maDon, gateway).request().requestId().equals(request.requestId());
        if (ketQua.resultCode() == 0) {
            apDungThanhCong(don, lan, ketQua.transId(), moiNhat);
        } else if (THAT_BAI_CUOI.contains(ketQua.resultCode())
            && ("PENDING".equals(lan.trangThai()) || "FAILED".equals(lan.trangThai()))) {
            thanhToan.capNhat(request.requestId(), "FAILED", null);
            if (moiNhat && don.trangThai() == OrderStatus.PENDING_PAYMENT
                && (ketQua.resultCode() == 1005 || !thanhToan.conHan(maDon))) {
                ketThucDon(don);
            }
        }
    }

    private void apDungThanhCong(Don don, LanThanhToan lan, long transId, boolean moiNhat) {
        if (transId <= 0) {
            throw new IllegalArgumentException("Thiếu mã giao dịch thành công.");
        }
        String requestId = lan.request().requestId();
        if ("PAID".equals(lan.trangThai()) || "REFUND_PENDING".equals(lan.trangThai())) {
            if (!Long.toString(transId).equals(lan.transId())) {
                throw new IllegalArgumentException("Mã giao dịch không khớp.");
            }
            return;
        }

        long maDon = don.maDonHang();
        var giu = thanhToan.dangGiu(maDon);
        var muc = donHang.chiTiet(maDon);
        boolean coTheChot = moiNhat && "PENDING".equals(lan.trangThai())
            && don.trangThai() == OrderStatus.PENDING_PAYMENT && !muc.isEmpty()
            && giu.equals(muc.stream().map(DonHang_Repository.ChiTiet::maBienThe).toList());
        if (!coTheChot) {
            thanhToan.capNhat(requestId, "REFUND_PENDING", transId);
            if (moiNhat && don.trangThai() == OrderStatus.PENDING_PAYMENT) {
                ketThucDon(don);
            }
            return;
        }
        giu.forEach(ma -> tonKho.consumeReservation(maDon, ma));
        voucher.ketThucTamGiu(maDon, don.maVoucher(), true);
        thanhToan.capNhat(requestId, "PAID", transId);
        donHang.doiTrangThai(maDon, OrderStatus.PENDING_CONFIRMATION);
    }

    private void ketThucDon(Don don) {
        long maDon = don.maDonHang();
        // Giao dịch thành công đến muộn còn nghĩa vụ tiền. Giữ tài nguyên chờ kết quả cuối.
        if (thanhToan.soLanCanHoan(maDon) > 0) return;
        thanhToan.dangGiu(maDon).forEach(sku -> tonKho.releaseReservation(maDon, sku));
        voucher.ketThucTamGiu(maDon, don.maVoucher(), false);
        donHang.doiTrangThai(maDon, OrderStatus.EXPIRED);
    }

    private Don khoaDon(long maDon) {
        return donHang.timDon(maDon, true).filter(don -> "MOMO".equals(don.phuongThuc()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private LanThanhToan theoRequest(String requestId, String gateway) {
        return thanhToan.theoRequest(requestId).filter(lan -> gateway.equals(lan.gateway()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
