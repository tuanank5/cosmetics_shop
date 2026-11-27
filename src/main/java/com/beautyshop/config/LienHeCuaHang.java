package com.beautyshop.config;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public record LienHeCuaHang(String dienThoai, String email, String zalo, String facebook, String instagram) {
    private static final Pattern SO_DIEN_THOAI = Pattern.compile("\\+?[0-9][0-9 .()-]{6,23}");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+");

    public LienHeCuaHang {
        dienThoai = dienThoai.strip();
        dienThoai = SO_DIEN_THOAI.matcher(dienThoai).matches()
            && dienThoai.replaceAll("[^0-9]", "").matches("[0-9]{8,15}") ? dienThoai : null;
        email = email.strip();
        email = EMAIL.matcher(email).matches() ? email : null;
        zalo = trangMangXaHoi(zalo, "zalo.me");
        facebook = trangMangXaHoi(facebook, "facebook.com");
        instagram = trangMangXaHoi(instagram, "instagram.com");
    }

    public String goiDien() { return dienThoai == null ? null : "tel:" + dienThoai.replaceAll("[^+0-9]", ""); }
    public String guiEmail() { return email == null ? null : "mailto:" + email; }

    private static String trangMangXaHoi(String url, String domain) {
        try {
            URI diaChi = URI.create(url.strip());
            if (!"https".equalsIgnoreCase(diaChi.getScheme()) || diaChi.getHost() == null || diaChi.getUserInfo() != null
                    || (diaChi.getPort() != -1 && diaChi.getPort() != 443)) return null;
            String host = diaChi.getHost().toLowerCase(Locale.ROOT);
            String path = diaChi.getPath();
            if ((!host.equals(domain) && !host.endsWith("." + domain)) || path == null
                    || path.replace("/", "").isBlank()) return null;
            return diaChi.toASCIIString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
