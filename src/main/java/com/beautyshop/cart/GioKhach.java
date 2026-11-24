package com.beautyshop.cart;

import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record GioKhach(UUID maGop, Map<Long, Integer> soLuong) implements Serializable {
    private static final String SESSION_KEY = GioKhach.class.getName();

    public GioKhach {
        soLuong = Map.copyOf(soLuong);
    }

    public static GioKhach doc(HttpSession session) {
        if (session != null && session.getAttribute(SESSION_KEY) instanceof GioKhach gio) {
            return gio;
        }
        return new GioKhach(null, Map.of());
    }

    public GioKhach them(long maBienThe, int soLuongThem) {
        var muc = new HashMap<>(soLuong);
        muc.merge(maBienThe, soLuongThem, Math::addExact);
        return new GioKhach(UUID.randomUUID(), muc);
    }

    public GioKhach datSoLuong(long maBienThe, int soLuongMoi) {
        var muc = new HashMap<>(soLuong);
        if (soLuongMoi == 0) {
            muc.remove(maBienThe);
        } else {
            muc.put(maBienThe, soLuongMoi);
        }
        return new GioKhach(UUID.randomUUID(), muc);
    }

    public void luu(HttpSession session) {
        session.setAttribute(SESSION_KEY, this);
    }

    public static void xoa(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }
}
