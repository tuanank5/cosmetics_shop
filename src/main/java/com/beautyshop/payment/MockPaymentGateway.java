package com.beautyshop.payment;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class MockPaymentGateway implements MomoGateway {
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis());
    private final ConcurrentHashMap<String, KetQua> ketQua = new ConcurrentHashMap<>();

    @Override public String maGateway() { return MA_MOCK; }

    @Override
    public Optional<URI> tao(Request request) {
        return Optional.of(URI.create("/payment/momo/mock/" + request.maDonHang()));
    }

    @Override
    public Optional<KetQua> doiSoat(Request request) {
        return Optional.ofNullable(ketQua.get(request.requestId()));
    }

    KetQua ghiKetQua(Request request, int resultCode) {
        return ketQua.computeIfAbsent(request.requestId(), key -> new KetQua(resultCode, sequence.incrementAndGet()));
    }
}
