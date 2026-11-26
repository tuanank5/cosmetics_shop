package com.beautyshop.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.util.UriUtils;

/** Chỉ tạo đích nội bộ từ các nguồn và tham số đã biết. */
public final class NguonDieuHuong {
    private static final String CHECKOUT = "checkout";

    private NguonDieuHuong() {}

    static boolean tuCheckout(String nguon) {
        return CHECKOUT.equals(nguon);
    }

    static String veDiaChi(String nguon) {
        return tuCheckout(nguon) ? "/checkout" : "/account/addresses";
    }

    public static Map<String, String> kyDashboard(Map<String, String> request) {
        if (!"dashboard".equals(request.get("nav"))) return Map.of();
        String range = request.getOrDefault("range", "");
        if (range.equals("7d") || range.equals("30d") || range.equals("90d")) {
            return Map.of("nav", "dashboard", "range", range);
        }
        String tu = request.getOrDefault("from", "");
        String den = request.getOrDefault("to", "");
        if (tu.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") && den.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            try {
                long ngay = ChronoUnit.DAYS.between(LocalDate.parse(tu), LocalDate.parse(den));
                if (ngay >= 0 && ngay < 366) return Map.of("nav", "dashboard", "from", tu, "to", den);
            } catch (DateTimeParseException ignored) {
                // Dashboard dùng kỳ mặc định cho ngày không hợp lệ.
            }
        }
        return Map.of("nav", "dashboard");
    }

    public static String veDashboard(Map<String, String> ky) {
        var thamSo = new LinkedHashMap<>(ky);
        thamSo.remove("nav");
        return coThamSo("/admin/dashboard", thamSo);
    }

    /** Caller truyền route cố định và bộ lọc đã chuẩn hóa. */
    public static String trangDanhSach(String path, Map<String, String> boLoc, int page) {
        var thamSo = new LinkedHashMap<>(boLoc);
        thamSo.put("page", Integer.toString(page));
        return coThamSo(path, thamSo);
    }

    static NguonSanPham sanPham(Map<String, String> request, Long maSanPham, boolean customer) {
        String nguon = request.getOrDefault("from", "");
        var thamSo = new LinkedHashMap<String, String>();
        String ve;
        String nhan;
        switch (nguon) {
            case "home-featured", "home-popular", "home-all-products" -> {
                String sort = request.getOrDefault("sort", "");
                if (sort.equals("priceAsc") || sort.equals("priceDesc")) thamSo.put("sort", sort);
                ve = coThamSo("/", thamSo) + "#" + nguon + "-product-" + maSanPham;
                nhan = "Quay lại trang chủ";
            }
            case "wishlist" -> {
                if (!customer) return macDinh();
                ve = "/account/wishlist";
                nhan = "Quay lại sản phẩm yêu thích";
            }
            case "cart" -> {
                ve = "/cart";
                nhan = "Quay lại giỏ hàng";
            }
            case "catalog" -> {
                them(thamSo, "q", request.get("q"), 160);
                them(thamSo, "brand", request.get("brand"), 190);
                them(thamSo, "category", request.get("category"), 190);
                them(thamSo, "skinType", request.get("skinType"), 100);
                String sort = request.getOrDefault("sort", "");
                if (sort.equals("newest") || sort.equals("priceAsc") || sort.equals("priceDesc")) thamSo.put("sort", sort);
                so(thamSo, "page", request.get("page"), 0, 100_000);
                so(thamSo, "size", request.get("size"), 1, 48);
                String filterPanel = request.getOrDefault("filterPanel", "");
                if (filterPanel.equals("open") || filterPanel.equals("closed")) thamSo.put("filterPanel", filterPanel);
                ve = coThamSo("/products", thamSo);
                nhan = "Quay lại danh sách sản phẩm";
            }
            default -> { return macDinh(); }
        }
        thamSo.put("from", nguon);
        return new NguonSanPham(ve, nhan, Map.copyOf(thamSo));
    }

    private static NguonSanPham macDinh() {
        return new NguonSanPham("/products", "Tất cả sản phẩm", Map.of());
    }

    private static void them(Map<String, String> thamSo, String ten, String value, int max) {
        if (value == null || value.isBlank() || value.codePoints().anyMatch(Character::isISOControl)) return;
        String sach = value.strip();
        thamSo.put(ten, sach.substring(0, sach.offsetByCodePoints(0, Math.min(max, sach.codePointCount(0, sach.length())))));
    }

    private static void so(Map<String, String> thamSo, String ten, String value, int min, int max) {
        if (value == null) return;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed >= min && parsed <= max) thamSo.put(ten, Integer.toString(parsed));
        } catch (NumberFormatException ignored) {
            // Một bộ lọc hỏng không làm mất đường về mặc định.
        }
    }

    private static String coThamSo(String path, Map<String, String> thamSo) {
        var query = new java.util.StringJoiner("&", "?", "").setEmptyValue("");
        thamSo.forEach((key, value) -> query.add(key + "=" + UriUtils.encode(value, StandardCharsets.UTF_8)));
        return path + query;
    }

    public record NguonSanPham(String duongVe, String nhan, Map<String, String> thamSo) {
        public String trangDanhGia(String slug, Long variant, int page) {
            var query = new LinkedHashMap<>(thamSo);
            query.put("variant", variant.toString());
            query.put("reviewPage", Integer.toString(Math.max(0, page)));
            return coThamSo("/products/" + UriUtils.encodePathSegment(slug, StandardCharsets.UTF_8), query) + "#danh-gia";
        }
    }
}
