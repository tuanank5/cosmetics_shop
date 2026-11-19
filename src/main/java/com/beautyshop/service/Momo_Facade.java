package com.beautyshop.service;

import com.beautyshop.payment.MomoGateway;
import com.beautyshop.payment.MomoSignature.Ipn;
import com.beautyshop.payment.Momo_Settings;
import com.beautyshop.repository.ThanhToanMomo_Repository.LanThanhToan;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Momo_Facade {
    private final Momo_Settings settings;
    private final ThanhToanMomo_Service thanhToan;
    private final QuyenDonHang_Service quyen;

    public Momo_Facade(Momo_Settings settings, ThanhToanMomo_Service thanhToan, QuyenDonHang_Service quyen) {
        this.settings = settings;
        this.thanhToan = thanhToan;
        this.quyen = quyen;
    }

    public boolean enabled() { return settings.enabled(); }
    public boolean mock() { return settings.mock(); }

    // HTTP tới gateway luôn ở ngoài transaction ghi đơn và tồn kho.
    public Optional<URI> batDau(long maDon) {
        quyen.kiemTraSoHuu(maDon);
        var gateway = gateway();
        return gateway.tao(thanhToan.chuanBi(maDon, gateway.maGateway()));
    }

    public void nhanIPN(Ipn ipn) {
        var gateway = gateway();
        if (!settings.verify(ipn)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        thanhToan.nhanIPN(ipn, gateway.maGateway());
    }

    public boolean doiSoat(long maDon) {
        var gateway = gateway();
        var request = thanhToan.moiNhat(maDon, gateway.maGateway()).request();
        var ketQua = gateway.doiSoat(request);
        ketQua.ifPresent(kq -> thanhToan.nhanDoiSoat(request, kq, gateway.maGateway()));
        return ketQua.isPresent();
    }

    public LanThanhToan xemMock(long maDon) {
        if (!settings.mock()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        quyen.kiemTraSoHuu(maDon);
        return thanhToan.moiNhat(maDon, MomoGateway.MA_MOCK);
    }

    public void moPhong(long maDon, String requestId, int resultCode) {
        if (resultCode != 0 && resultCode != 1005 && resultCode != 1006) {
            throw new IllegalArgumentException("Kết quả mô phỏng không hợp lệ.");
        }
        var lan = xemMock(maDon);
        if (!lan.request().requestId().equals(requestId)) {
            throw new IllegalStateException("Trang mô phỏng đã cũ. Mở lại lần thanh toán hiện tại.");
        }
        nhanIPN(settings.moPhong(lan.request(), resultCode));
    }

    private MomoGateway gateway() {
        return settings.gateway().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
