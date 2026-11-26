package com.beautyshop.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * P23: nơi lưu nội dung đánh giá sao + bình luận. Điều kiện được đánh giá (BR16) luôn kiểm ở MariaDB
 * ({@link DanhGia_Repository#mucMua}) trước khi gọi store.
 *
 * MongoDB lưu nội dung đánh giá.
 * Lỗi kết nối được ném dưới dạng {@link org.springframework.dao.DataAccessException}; caller quyết định ẩn/thông báo.
 */
public interface DanhGia_Store {
    /** Tên hiển thị khi không có họ tên. */
    String TEN_MAC_DINH = "Khách hàng";

    /** Đánh giá của chính chủ dòng đơn (để mở form sửa). */
    Optional<DanhGiaCuaKhach> cuaChuMuc(long maChiTietDonHang, long maNguoiDung);

    /** Tạo mới ở trạng thái chờ duyệt. Dòng đơn đã có đánh giá -> {@link org.springframework.dao.DuplicateKeyException}. */
    void tao(DanhGiaMoi moi);

    /** Chủ dòng đơn sửa: đánh giá trở về chờ duyệt. Trả false khi không có đánh giá. */
    boolean sua(long maChiTietDonHang, long maNguoiDung, int soSao, String noiDung);

    /** Đánh giá đang hiển thị của sản phẩm, mới nhất trước. */
    Page<DanhGiaCongKhai> congKhai(long maSanPham, Pageable pageable);

    /** Danh sách Admin theo bộ lọc, mới nhất trước. {@code tenSanPham} để trống; service điền theo lô từ MariaDB. */
    Page<DanhGiaAdmin> danhSachAdmin(BoLocDanhGia boLoc, Pageable pageable);

    /**
     * Duyệt ({@code hienThi = true}) chỉ thành công khi {@code phienBan} đúng phiên bản Admin đã xem; ẩn không cần phiên bản.
     * Trả false khi không tìm thấy hoặc phiên bản đã đổi.
     */
    boolean datHienThi(String ma, boolean hienThi, String phienBan);

    /** Tóm tắt đánh giá đang hiển thị của nhiều sản phẩm trong một truy vấn; {@code tuHoacNull} lọc theo ngày tạo. */
    Map<Long, TomTatDanhGia> tomTatNhieu(Collection<Long> maSanPham, Instant tuHoacNull);

    default TomTatDanhGia tomTat(long maSanPham) {
        return tomTatNhieu(java.util.List.of(maSanPham), null).getOrDefault(maSanPham, TomTatDanhGia.RONG);
    }

    /** Toàn cửa hàng, chỉ đánh giá đang hiển thị, tạo từ {@code tu} (điểm phổ biến P21: m = tổng sao / số đánh giá). */
    TomTatDanhGia tomTatCuaHang(Instant tu);

    /** Dashboard P20: số đánh giá chờ duyệt (mọi thời điểm) và phân bố sao của đánh giá tạo trong [a, b). */
    ThongKeDanhGia thongKe(Instant a, Instant b);

    enum TrangThaiLoc {
        TAT_CA, CHO_DUYET, DANG_HIEN, DA_AN;

        public static TrangThaiLoc tu(String giaTri) {
            if (giaTri != null) {
                for (TrangThaiLoc t : values()) {
                    if (t.name().equalsIgnoreCase(giaTri)) {
                        return t;
                    }
                }
            }
            return TAT_CA;
        }
    }

    record BoLocDanhGia(TrangThaiLoc trangThai, Integer soSao, Long maSanPham) {
        public static final BoLocDanhGia TAT_CA = new BoLocDanhGia(TrangThaiLoc.TAT_CA, null, null);
    }

    record DanhGiaCuaKhach(String ma, int soSao, String noiDung, boolean hienThi) {}

    record DanhGiaMoi(long maChiTietDonHang, long maDonHang, long maNguoiDung, long maSanPham, long maBienThe,
                      String tenHienThi, int soSao, String noiDung) {}

    record DanhGiaCongKhai(String tenHienThi, int soSao, String noiDung, Instant ngayTao) {
        public String ngayHienThi() { return ngay(ngayTao); }
        public String ngayIso() { return ngayTao == null ? "" : ngayTao.toString(); }
    }

    /** {@code daDuyet}: đã từng được duyệt ở phiên bản hiện tại (phân biệt "chờ duyệt" với "đã ẩn"). */
    record DanhGiaAdmin(String ma, long maSanPham, String tenSanPham, long maDonHang, String tenHienThi, int soSao,
                        String noiDung, boolean hienThi, boolean daDuyet, String phienBan, Instant ngayTao) {
        public String ngayHienThi() { return ngay(ngayTao); }

        public DanhGiaAdmin voiTenSanPham(String ten) {
            return new DanhGiaAdmin(ma, maSanPham, ten, maDonHang, tenHienThi, soSao, noiDung, hienThi, daDuyet,
                phienBan, ngayTao);
        }
    }

    /** {@code phanBo[i]} = số đánh giá {@code i + 1} sao. */
    record TomTatDanhGia(long soLuong, long tongSao, long[] phanBo) {
        public static final TomTatDanhGia RONG = new TomTatDanhGia(0, 0, new long[5]);

        public double trungBinh() { return soLuong == 0 ? 0 : (double) tongSao / soLuong; }

        /** Điểm làm tròn 1 chữ số thập phân, dấu phẩy kiểu Việt Nam: "4,5". */
        public String trungBinhHienThi() {
            return String.format(java.util.Locale.ROOT, "%.1f", trungBinh()).replace('.', ',');
        }

        public long soSao(int sao) { return phanBo[sao - 1]; }

        /** Phần trăm (0–100, làm tròn) của mức sao trong tổng; dùng cho thanh phân bố. */
        public int phanTram(int sao) { return soLuong == 0 ? 0 : (int) Math.round(100.0 * phanBo[sao - 1] / soLuong); }

        /** Số nửa sao được tô (0–10) cho hiển thị sao, làm tròn tới 0,5. */
        public int nuaSao() { return (int) Math.round(trungBinh() * 2); }

        @Override
        public boolean equals(Object o) {
            return o instanceof TomTatDanhGia t && t.soLuong == soLuong && t.tongSao == tongSao
                && java.util.Arrays.equals(t.phanBo, phanBo);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(soLuong, tongSao, java.util.Arrays.hashCode(phanBo)); }

        @Override
        public String toString() { return "TomTatDanhGia[" + soLuong + ", " + tongSao + ", " + java.util.Arrays.toString(phanBo) + "]"; }
    }

    /** {@code phanBoTrongKy[i]} = số đánh giá {@code i + 1} sao tạo trong kỳ (cả đang hiện và chờ duyệt/ẩn). */
    record ThongKeDanhGia(long choDuyet, long[] phanBoTrongKy) {
        public static final ThongKeDanhGia RONG = new ThongKeDanhGia(0, new long[5]);
    }

    java.time.ZoneId MUI_GIO = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    /** Ngày theo giờ Việt Nam, dạng 24/11/2026. */
    static String ngay(Instant thoiDiem) {
        return thoiDiem == null ? "" : java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(MUI_GIO).format(thoiDiem);
    }

    /** Tên rút gọn công khai: "Phạm Văn Thịnh" -> "Thịnh P."; không bao giờ là email hay số điện thoại. */
    static String tenRutGon(String hoTen) {
        if (hoTen == null) {
            return TEN_MAC_DINH;
        }
        String[] tu = java.util.Arrays.stream(hoTen.strip().split("\\s+"))
            .filter(t -> !t.isEmpty() && Character.isLetter(t.codePointAt(0)) && !t.contains("@"))
            .toArray(String[]::new);
        if (tu.length == 0) {
            return TEN_MAC_DINH;
        }
        if (tu.length == 1) {
            return tu[0];
        }
        String ho = tu[0];
        return tu[tu.length - 1] + " " + ho.substring(0, ho.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT) + ".";
    }
}
