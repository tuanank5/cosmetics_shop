package com.beautyshop.payment;

import java.net.URI;
import java.util.Optional;

public interface MomoGateway {
    String MA_MOCK = "MOMO_MOCK";
    String MA_SANDBOX = "MOMO";

    String maGateway();
    Optional<URI> tao(Request request);
    Optional<KetQua> doiSoat(Request request);

    record Request(long maDonHang, String maHienThi, String requestId, long amount) {
        public String orderId() { return maHienThi + "-" + requestId; }
        public String orderInfo() { return "BeautyShop " + maHienThi; }
    }

    record KetQua(int resultCode, long transId) {}
}
