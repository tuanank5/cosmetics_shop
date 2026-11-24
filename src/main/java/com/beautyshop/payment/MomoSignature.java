package com.beautyshop.payment;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class MomoSignature {
    private MomoSignature() {}

    public static String sign(String secret, String raw) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Không thể tạo chữ ký MoMo.", exception);
        }
    }

    public static boolean verify(String secret, String raw, String signature) {
        if (signature == null || !signature.matches("[0-9a-fA-F]{64}")) {
            return false;
        }
        return MessageDigest.isEqual(HexFormat.of().parseHex(sign(secret, raw)), HexFormat.of().parseHex(signature));
    }

    public record Ipn(String partnerCode, String orderId, String requestId, Long amount, String orderInfo,
                      String orderType, Long transId, Integer resultCode, String message, String payType,
                      Long responseTime, String extraData, String signature) {
        public boolean dayDu() {
            return partnerCode != null && orderId != null && requestId != null && amount != null
                && orderInfo != null && orderType != null && transId != null && resultCode != null
                && message != null && payType != null && responseTime != null && extraData != null;
        }

        public String raw(String accessKey) {
            return "accessKey=" + accessKey + "&amount=" + amount + "&extraData=" + extraData
                + "&message=" + message + "&orderId=" + orderId + "&orderInfo=" + orderInfo
                + "&orderType=" + orderType + "&partnerCode=" + partnerCode + "&payType=" + payType
                + "&requestId=" + requestId + "&responseTime=" + responseTime + "&resultCode=" + resultCode
                + "&transId=" + transId;
        }

        Ipn voiChuKy(String chuKy) {
            return new Ipn(partnerCode, orderId, requestId, amount, orderInfo, orderType, transId,
                resultCode, message, payType, responseTime, extraData, chuKy);
        }
    }
}
