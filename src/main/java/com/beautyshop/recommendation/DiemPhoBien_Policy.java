package com.beautyshop.recommendation;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Điểm phổ biến của sản phẩm (gợi ý phiên bản 1, chưa cá nhân hóa). Công thức:
 * <pre>
 * diem = TRONG_SO_DA_BAN    × chuanHoa(số lượng đã bán trong kỳ, đơn không hủy)
 *      + TRONG_SO_DANH_GIA  × (bayes − 1) / 4
 *      + TRONG_SO_YEU_THICH × chuanHoa(số lượt yêu thích)
 * bayes = (HE_SO_BAYES_C × m + tổng sao) / (HE_SO_BAYES_C + số đánh giá)
 * </pre>
 * {@code m} là điểm sao trung bình toàn cửa hàng trong kỳ; chuẩn hóa min-max trong tập ứng viên, mẫu số 0 thì thành phần
 * bằng 0; không có đánh giá nào trong cửa hàng thì thành phần đánh giá bằng 0. Hòa điểm thì sản phẩm tạo mới hơn đứng trước.
 * Trọng số là hằng số để P24 chỉnh mà không đổi template.
 */
public final class DiemPhoBien_Policy {
    public static final double TRONG_SO_DA_BAN = 0.5;
    public static final double TRONG_SO_DANH_GIA = 0.3;
    public static final double TRONG_SO_YEU_THICH = 0.2;
    public static final int HE_SO_BAYES_C = 5;
    public static final int SO_NGAY_TINH = 90;
    static final double SAO_TOI_THIEU = 1.0;
    static final double SAO_TOI_DA = 5.0;

    private DiemPhoBien_Policy() {}

    /**
     * @param ungVien các sản phẩm đủ điều kiện hiển thị
     * @param soDanhGiaCuaHang tổng số đánh giá toàn cửa hàng trong kỳ
     * @param tongSaoCuaHang tổng số sao toàn cửa hàng trong kỳ
     */
    public static KetQua xepHang(List<UngVien> ungVien, long soDanhGiaCuaHang, long tongSaoCuaHang) {
        boolean coDanhGia = soDanhGiaCuaHang > 0;
        double m = coDanhGia ? (double) tongSaoCuaHang / soDanhGiaCuaHang : 0;
        long banMin = ungVien.stream().mapToLong(UngVien::soLuongDaBan).min().orElse(0);
        long banMax = ungVien.stream().mapToLong(UngVien::soLuongDaBan).max().orElse(0);
        long thichMin = ungVien.stream().mapToLong(UngVien::soLuotYeuThich).min().orElse(0);
        long thichMax = ungVien.stream().mapToLong(UngVien::soLuotYeuThich).max().orElse(0);
        List<Diem> diem = ungVien.stream().map(u -> {
            double ban = chuanHoa(u.soLuongDaBan(), banMin, banMax);
            double thich = chuanHoa(u.soLuotYeuThich(), thichMin, thichMax);
            Double bayes = coDanhGia ? bayes(m, u.soDanhGia(), u.tongSao()) : null;
            double danhGia = bayes == null ? 0 : (bayes - SAO_TOI_THIEU) / (SAO_TOI_DA - SAO_TOI_THIEU);
            double tong = TRONG_SO_DA_BAN * ban + TRONG_SO_DANH_GIA * danhGia + TRONG_SO_YEU_THICH * thich;
            return new Diem(u, ban, bayes, danhGia, thich, tong);
        }).sorted(Comparator.comparingDouble(Diem::diem).reversed()
            .thenComparing((Diem d) -> d.ungVien().ngayTao(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing((Diem d) -> d.ungVien().maSanPham(), Comparator.reverseOrder()))
            .toList();
        boolean coDuLieu = coDanhGia || banMax > 0 || thichMax > 0;
        return new KetQua(diem, coDuLieu, coDanhGia ? m : null);
    }

    /** Điểm Bayes trên thang 1–5: kéo sản phẩm ít đánh giá về trung bình cửa hàng {@code m}. */
    public static double bayes(double m, long soDanhGia, long tongSao) {
        return (HE_SO_BAYES_C * m + tongSao) / (HE_SO_BAYES_C + soDanhGia);
    }

    /** Min-max về 0–1; mẫu số 0 (mọi ứng viên bằng nhau) thì trả 0. */
    public static double chuanHoa(long giaTri, long min, long max) {
        return max == min ? 0 : (double) (giaTri - min) / (max - min);
    }

    public record UngVien(long maSanPham, LocalDateTime ngayTao, long soLuongDaBan, long soDanhGia, long tongSao,
                          long soLuotYeuThich) {}

    /** {@code bayes} null khi cửa hàng chưa có đánh giá nào trong kỳ. */
    public record Diem(UngVien ungVien, double daBanChuanHoa, Double bayes, double danhGiaChuanHoa,
                       double yeuThichChuanHoa, double diem) {}

    /** {@code coDuLieu} false nghĩa là cửa hàng mới: caller rơi về sản phẩm mới nhất. */
    public record KetQua(List<Diem> xepHang, boolean coDuLieu, Double diemTrungBinhCuaHang) {}
}
