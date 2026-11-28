package com.beautyshop.controller;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

/** Ràng buộc form với phiên, chủ đơn và đích POST. Biên nhận DB vẫn quyết định đúng một lần. */
@Component
@SessionScope
class PhienMuaLai implements Serializable {
    private final byte[] khoa = new byte[32];

    PhienMuaLai() {
        new SecureRandom().nextBytes(khoa);
    }

    public String ky(long chu, long don, UUID requestId) {
        byte[] duLieu = ByteBuffer.allocate(32).putLong(chu).putLong(don)
            .putLong(requestId.getMostSignificantBits()).putLong(requestId.getLeastSignificantBits()).array();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(khoa, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(duLieu));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Không thể xác nhận thao tác mua lại.", ex);
        }
    }

    public void kiemTra(long chu, long don, UUID requestId, String chuKy) {
        if (!MessageDigest.isEqual(ky(chu,don,requestId).getBytes(StandardCharsets.US_ASCII),
                chuKy.getBytes(StandardCharsets.US_ASCII))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
