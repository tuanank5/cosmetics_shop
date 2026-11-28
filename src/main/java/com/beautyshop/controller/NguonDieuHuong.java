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
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

/** Chỉ tạo đích nội bộ từ các nguồn và tham số đã biết. */
public final class NguonDieuHuong {
    private static final String CHECKOUT = "checkout";
    private static final String SO_DIA_CHI = "addresses";

    private NguonDieuHuong() {}

    static boolean tuCheckout(String nguon) {
        return CHECKOUT.equals(nguon);
    }

    static String nguonSuaDiaChi(String nguon, Long id) {
        if (tuCheckout(nguon)) return CHECKOUT;
        return id != null && SO_DIA_CHI.equals(nguon) ? SO_DIA_CHI : null;
    }

    static String veDiaChi(String nguon, Long id) {
        if (tuCheckout(nguon)) return "/checkout";
        return "/account/addresses" + (SO_DIA_CHI.equals(nguonSuaDiaChi(nguon, id)) ? "#address-" + id : "");
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
            q, giaTri(SapXep.class, sort), size, ngayLoc(request.get(tuChiTiet ? "listFromDate" : "fromDate")),
            ngayLoc(request.get(tuChiTiet ? "listToDate" : "toDate")));
        return new NguonDonAdmin(loc, page);
    }

    static NguonDonAdmin donKhach(Map<String, String> request, List<OrderStatus> trangThai, boolean chiTiet) {
        var loc = new BoLocDonHangAdmin(trangThai, null, null, request.get(chiTiet ? "listQ" : "q"),
            giaTri(SapXep.class, request.get(chiTiet ? "listSort" : "sort")), 10,
            ngayLoc(request.get(chiTiet ? "listFromDate" : "fromDate")),
            ngayLoc(request.get(chiTiet ? "listToDate" : "toDate")));
        return new NguonDonAdmin(loc, soNguyen(request.get(chiTiet ? "listPage" : "page"), -1));
    }

    private static LocalDate ngayLoc(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") && value.compareTo("1000-01-02") >= 0
                    && value.compareTo("9999-12-30") <= 0) {
                return LocalDate.parse(value);
            }
        } catch (DateTimeParseException ignored) {
            // The GET filter accepts ISO calendar dates.
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày lọc đơn hàng không hợp lệ.");
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
        public String danhSachKhach(int soTrang) {
            var query = thamSo(false, soTrang);
            query.remove("size");
            return url("/orders", query, "status");
        }

        public String chiTietKhach(long maDon, int soTrang) {
            var query = thamSo(true, soTrang);
            query.remove("listSize");
            return url("/orders/" + maDon, query, "listStatus");
        }

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
            if (loc.tuNgay() != null) thamSo.put(chiTiet ? "listFromDate" : "fromDate", loc.tuNgay().toString());
            if (loc.denNgay() != null) thamSo.put(chiTiet ? "listToDate" : "toDate", loc.denNgay().toString());
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

    public static Map<String, String> nguonDanhGia(Map<String, String> request) {
        String id = request.get("review");
        if (id == null || !id.matches("[0-9a-fA-F]{24}")) return Map.of();
        var query = new LinkedHashMap<String, String>();
        query.put("review", id);
        String status = request.getOrDefault("reviewStatus", "");
        if (Set.of("TAT_CA", "CHO_DUYET", "DANG_HIEN", "DA_AN").contains(status)) query.put("reviewStatus", status);
        so(query, "reviewStars", request.get("reviewStars"), 1, 5);
        soLong(query, "reviewProduct", request.get("reviewProduct"));
        so(query, "reviewPage", request.get("reviewPage"), 0, 10_000);
        return query;
    }

    public static String veDanhGia(Map<String, String> nguon) {
        var query = new LinkedHashMap<String, String>();
        query.put("focus", nguon.get("review"));
        for (String field : List.of("Status", "Stars", "Product", "Page")) {
            String value = nguon.get("review" + field);
            if (value != null) query.put(field.toLowerCase(java.util.Locale.ROOT), value);
        }
        return coThamSo("/admin/reviews", query) + "#review-" + nguon.get("review");
    }

    /** Caller truyền route cố định và bộ lọc đã chuẩn hóa. */
    public static String trangDanhSach(String path, Map<String, String> boLoc, int page) {
        var thamSo = new LinkedHashMap<>(boLoc);
        thamSo.put("page", Integer.toString(page));
        return coThamSo(path, thamSo);
    }

    public static Integer trangNguon(Map<String, String> request, String ten) {
        int trang = soNguyen(request.get(ten), -1);
        return trang >= 0 && trang <= 100_000 ? trang : null;
    }

    static String veDanhSachKhach(String path, Map<String, String> request, String moc, long id) {
        Integer trang = trangNguon(request, "listPage");
        return trang == null ? path : trangDanhSach(path, Map.of(), trang) + "#" + moc + "-" + id;
    }

    public static NguonDanhSachAdmin nguonDanhSachAdmin(Map<String, String> request) {
        var loc = new LinkedHashMap<String, String>();
        them(loc, "q", request.get("listQ"), 190);
        String active = request.get("listActive");
        if ("true".equals(active) || "false".equals(active)) loc.put("active", active);
        soLong(loc, "category", request.get("listCategory"));
        soLong(loc, "brand", request.get("listBrand"));
        return new NguonDanhSachAdmin(loc, trangNguon(request, "listPage"));
    }

    public record NguonDanhSachAdmin(Map<String, String> boLoc, Integer trang) {
        public Map<String, String> truongAn() {
            var fields = new LinkedHashMap<String, String>();
            boLoc.forEach((ten, value) -> fields.put("list" + Character.toUpperCase(ten.charAt(0)) + ten.substring(1), value));
            if (trang != null) fields.put("listPage", trang.toString());
            return fields;
        }

        public String vao(String path) {
            return coThamSo(path, truongAn());
        }

        public String quayLai(String path, Long id) {
            return trang == null ? path : trangDanhSach(path, boLoc, trang) + mocAdmin(id);
        }
    }

    public static String mocAdmin(Long id) {
        return id == null ? "" : "#admin-row-" + id;
    }

    public static NguonBienTheAdmin nguonBienTheAdmin(Map<String, String> request) {
        return new NguonBienTheAdmin(nguonDanhSachAdmin(request), trangNguon(request, "variantPage"));
    }

    public record NguonBienTheAdmin(NguonDanhSachAdmin sanPham, Integer trang) {
        public Map<String, String> truongAn() {
            var fields = new LinkedHashMap<>(sanPham.truongAn());
            if (trang != null) fields.put("variantPage", trang.toString());
            return fields;
        }

        public String veDanhSach(long maSanPham, Long maBienThe) {
            String path = "/admin/catalog/products/" + maSanPham + "/variants";
            var fields = sanPham.truongAn();
            return trang == null ? coThamSo(path, fields)
                : trangDanhSach(path, fields, trang) + mocAdmin(maBienThe);
        }
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
                String neo = nguon.equals("home-featured") ? nguon : nguon + "-product-" + maSanPham;
                ve = coThamSo("/", thamSo) + "#" + neo;
                nhan = "Quay lại trang chủ";
            }
            case "wishlist" -> {
                if (!customer) return macDinh();
                so(thamSo, "wishlistPage", request.get("wishlistPage"), 1, 100_000);
                ve = coThamSo("/account/wishlist", thamSo.containsKey("wishlistPage")
                    ? Map.of("page", thamSo.get("wishlistPage")) : Map.of());
                nhan = "Quay lại sản phẩm yêu thích";
            }
            case "cart" -> {
                soLong(thamSo, "cartVariant", request.get("cartVariant"));
                ve = "/cart" + (thamSo.containsKey("cartVariant") ? "#cart-variant-" + thamSo.get("cartVariant") : "");
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
                ve = coThamSo("/products", thamSo) + "#catalog-product-" + maSanPham;
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
        return sanPham(thamSo, maSanPham, true).duongVe();
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

    public static String coThamSo(String path, Map<String, String> thamSo) {
        var query = new java.util.StringJoiner("&", "?", "").setEmptyValue("");
        thamSo.forEach((key, value) -> query.add(key + "=" + UriUtils.encode(value, StandardCharsets.UTF_8)));
        return path + query;
    }

    public record NguonSanPham(String duongVe, String nhan, Map<String, String> thamSo) {
        public String duongVeChiTiet(Long maSanPham) {
            return maSanPham != null && "wishlist".equals(thamSo.get("from"))
                ? duongVe + "#wishlist-product-" + maSanPham : duongVe;
        }

        public String trangDanhGia(String slug, Long variant, int page) {
            var query = new LinkedHashMap<>(thamSo);
            query.put("variant", variant.toString());
            query.put("reviewPage", Integer.toString(Math.max(0, page)));
            return coThamSo("/products/" + UriUtils.encodePathSegment(slug, StandardCharsets.UTF_8), query) + "#danh-gia";
        }
    }
}
