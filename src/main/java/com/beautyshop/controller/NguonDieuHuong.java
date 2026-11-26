package com.beautyshop.controller;

import com.beautyshop.dto.BoLocDonHangAdmin;
import com.beautyshop.dto.BoLocDonHangAdmin.PhuongThuc;
import com.beautyshop.dto.BoLocDonHangAdmin.SapXep;
import com.beautyshop.dto.BoLocDonHangAdmin.ThanhToan;
import com.beautyshop.order.OrderStatus;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
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

    public static final String DON_ADMIN = "/admin/orders";

    public static NguonDonAdmin donAdmin(Map<String, String> request, List<OrderStatus> trangThai, boolean tuChiTiet) {
        String q = request.get(tuChiTiet ? "listQ" : "q");
        String method = request.get(tuChiTiet ? "listMethod" : "method");
        String payment = request.get(tuChiTiet ? "listPaymentStatus" : "paymentStatus");
        String sort = request.get(tuChiTiet ? "listSort" : "sort");
        int size = soNguyen(request.get(tuChiTiet ? "listSize" : "size"), 20);
        int page = soNguyen(request.get(tuChiTiet ? "listPage" : "page"), -1);
        var loc = new BoLocDonHangAdmin(trangThai, giaTri(PhuongThuc.class, method), giaTri(ThanhToan.class, payment),
            q, giaTri(SapXep.class, sort), size);
        return new NguonDonAdmin(loc, page);
    }

    private static <E extends Enum<E>> E giaTri(Class<E> loai, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(loai, value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int soNguyen(String value, int macDinh) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return macDinh;
        }
    }

    public record NguonDonAdmin(BoLocDonHangAdmin loc, int page) {
        public String danhSach(int soTrang) {
            return url(DON_ADMIN, thamSo(false, soTrang), "status");
        }

        public String chiTiet(long maDon) {
            return url(DON_ADMIN + "/" + maDon, truongAn(), "listStatus");
        }

        public String quayLai(long maDon) {
            String url = danhSach(page);
            return page < 0 ? url : url + "#admin-order-" + maDon;
        }

        public Map<String, String> truongAn() {
            return thamSo(true, page);
        }

        public void giuQuaRedirect(org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
            truongAn().forEach(flash::addAttribute);
            if (!loc.trangThai().isEmpty()) flash.addAttribute("listStatus", loc.trangThai().stream().map(Enum::name)
                .collect(java.util.stream.Collectors.joining(",")));
        }

        private Map<String, String> thamSo(boolean chiTiet, int soTrang) {
            var thamSo = new LinkedHashMap<String, String>();
            if (!loc.maDon().isEmpty()) thamSo.put(chiTiet ? "listQ" : "q", loc.maDon());
            if (loc.phuongThuc() != null) thamSo.put(chiTiet ? "listMethod" : "method", loc.phuongThuc().name());
            if (loc.thanhToan() != null) thamSo.put(chiTiet ? "listPaymentStatus" : "paymentStatus", loc.thanhToan().name());
            if (loc.sapXep() != SapXep.newest) thamSo.put(chiTiet ? "listSort" : "sort", loc.sapXep().name());
            if (soTrang >= 0) thamSo.put(chiTiet ? "listPage" : "page", Integer.toString(soTrang));
            if (loc.kichThuoc() != 20) thamSo.put(chiTiet ? "listSize" : "size", Integer.toString(loc.kichThuoc()));
            return thamSo;
        }

        private String url(String path, Map<String, String> thamSo, String tenTrangThai) {
            var url = new StringBuilder(coThamSo(path, thamSo));
            String dauPhanCach = thamSo.isEmpty() ? "?" : "&";
            for (var trangThai : loc.trangThai()) {
                url.append(dauPhanCach).append(tenTrangThai).append('=').append(trangThai.name());
                dauPhanCach = "&";
            }
            return url.toString();
        }
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

    static String veYeuThich(Map<String, String> request, long maSanPham, String slugCongKhai) {
        String origin = request.getOrDefault("origin", "wishlist");
        if (origin.equals("pdp")) {
            var nguon = sanPham(request, maSanPham, true);
            if (slugCongKhai == null) return nguon.duongVe();
            var query = new LinkedHashMap<>(nguon.thamSo());
            soLong(query, "variant", request.get("variant"));
            so(query, "reviewPage", request.get("reviewPage"), 0, 10_000);
            return coThamSo("/products/" + UriUtils.encodePathSegment(slugCongKhai, StandardCharsets.UTF_8), query) + "#pdp-actions";
        }
        var thamSo = new LinkedHashMap<>(request);
        thamSo.put("from", origin);
        String ve = sanPham(thamSo, maSanPham, true).duongVe();
        return origin.equals("catalog") ? ve + "#catalog-product-" + maSanPham : ve;
    }

    private static void soLong(Map<String, String> thamSo, String ten, String value) {
        if (value == null) return;
        try {
            long so = Long.parseLong(value);
            if (so > 0) thamSo.put(ten, Long.toString(so));
        } catch (NumberFormatException ignored) {
            // Dùng biến thể mặc định khi mã không hợp lệ.
        }
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
