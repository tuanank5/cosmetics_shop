package com.beautyshop.payment;

import com.beautyshop.payment.MomoSignature.Ipn;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class Momo_Settings {
    private final MomoGateway gateway;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;

    Momo_Settings(Environment env, ObjectMapper mapper) {
        String mode = env.getProperty("app.payment.momo.enabled", Boolean.class, false)
            ? env.getProperty("app.payment.momo.mode", "disabled") : "disabled";
        if ("mock".equals(mode) && env.acceptsProfiles(Profiles.of("(dev | test) & !prod"))) {
            partnerCode = "BEAUTYSHOP_MOCK";
            accessKey = UUID.randomUUID().toString();
            secretKey = UUID.randomUUID().toString();
            gateway = new MockPaymentGateway();
        } else {
            partnerCode = env.getProperty("MOMO_PARTNER_CODE", "");
            accessKey = env.getProperty("MOMO_ACCESS_KEY", "");
            secretKey = env.getProperty("MOMO_SECRET_KEY", "");
            URI redirect = https(env.getProperty("MOMO_REDIRECT_URL", ""), "/payment/momo/return");
            URI ipn = https(env.getProperty("MOMO_IPN_URL", ""), "/payment/momo/ipn");
            gateway = "sandbox".equals(mode) && !partnerCode.isBlank() && !accessKey.isBlank()
                && !secretKey.isBlank() && redirect != null && ipn != null
                ? new SandboxMomoGateway(mapper, partnerCode, accessKey, secretKey, redirect, ipn) : null;
            if (gateway == null && !"disabled".equals(mode)) {
                LoggerFactory.getLogger(Momo_Settings.class).warn("MoMo chưa bật vì thiếu cấu hình hoặc profile không phù hợp.");
            }
        }
    }

    public boolean enabled() { return gateway != null; }
    public boolean mock() { return gateway instanceof MockPaymentGateway; }
    public Optional<MomoGateway> gateway() { return Optional.ofNullable(gateway); }

    public boolean verify(Ipn ipn) {
        return enabled() && ipn.dayDu() && partnerCode.equals(ipn.partnerCode())
            && MomoSignature.verify(secretKey, ipn.raw(accessKey), ipn.signature());
    }

    public Ipn moPhong(MomoGateway.Request request, int resultCode) {
        if (!(gateway instanceof MockPaymentGateway mockGateway)) {
            throw new IllegalStateException("Chế độ mô phỏng chưa bật.");
        }
        var ketQua = mockGateway.ghiKetQua(request, resultCode);
        var ipn = new Ipn(partnerCode, request.orderId(), request.requestId(), request.amount(), request.orderInfo(),
            "momo_wallet", ketQua.transId(), ketQua.resultCode(), "Mock", "webApp",
            System.currentTimeMillis(), "", null);
        return ipn.voiChuKy(MomoSignature.sign(secretKey, ipn.raw(accessKey)));
    }

    private static URI https(String url, String duoiPath) {
        try {
            var uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
                && uri.getRawPath().endsWith(duoiPath) && uri.getRawQuery() == null
                && uri.getRawFragment() == null ? uri : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
