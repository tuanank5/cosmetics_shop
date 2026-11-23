package com.beautyshop.service;

import com.beautyshop.repository.CuaHang_Repository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dữ liệu chỉ đọc cho các chương kể chuyện trên trang chủ (P18).
 * Số lượng hiển thị được đếm bằng đúng bộ lọc của /products, nên mọi liên kết luôn có kết quả.
 * Nội dung mô tả là văn bản trung lập đã được owner duyệt, không phải khẳng định công dụng điều trị.
 */
@Service
@Transactional(readOnly = true)
public class TrangChu_Service {
    static final int SO_LOAI_DA_TOI_DA = 5;
    private static final Locale VI = Locale.forLanguageTag("vi");

    private static final Map<String, String> MO_TA_LOAI_DA = Map.of(
        "da khô", "Thường cảm thấy căng, cần thêm độ ẩm.",
        "da nhạy cảm", "Dễ phản ứng, nên ưu tiên công thức dịu nhẹ.",
        "da thiếu ẩm", "Có thể đổ dầu nhưng vẫn thiếu nước.",
        "da thường", "Cân bằng, ít khi khô hoặc bóng dầu.",
        "da dầu", "Dễ bóng nhờn, thường hợp kết cấu mỏng nhẹ.",
        "da hỗn hợp", "Vùng chữ T dễ dầu, hai má có thể khô.");

    static final List<BuocMau> NGHI_THUC = List.of(
        new BuocMau(1, "Làm sạch", "Làm sạch", "Loại bỏ bụi bẩn và dầu thừa."),
        new BuocMau(2, "Cân bằng", "Toner & xịt khoáng", "Chuẩn bị da cho các bước tiếp theo."),
        new BuocMau(3, "Tinh chất", "Tinh chất", "Bổ sung hoạt chất theo mục tiêu riêng."),
        new BuocMau(4, "Dưỡng ẩm", "Dưỡng ẩm", "Giữ ẩm và làm mềm da."),
        new BuocMau(5, "Chống nắng", "Chống nắng", "Bước cuối mỗi buổi sáng."));

    static final List<HoatChatMau> HOAT_CHAT = List.of(
        new HoatChatMau("Hyaluronic Acid", "hyaluronic",
            "Hoạt chất giữ nước, thường có trong sản phẩm dưỡng ẩm và tinh chất."),
        new HoatChatMau("Ceramide", "ceramide",
            "Thành phần lipid vốn có trong da, thường dùng trong sản phẩm hỗ trợ hàng rào bảo vệ da."),
        new HoatChatMau("Salicylic Acid (BHA)", "salicylic",
            "Acid tan trong dầu, thường có trong sản phẩm làm sạch; nên dùng theo hướng dẫn trên nhãn và chống nắng đầy đủ."),
        new HoatChatMau("Niacinamide", "niacinamide", "Dạng vitamin B3, thường gặp trong tinh chất và kem dưỡng."));

    private final CuaHang_Repository cuaHangRepository;
    private final CuaHang_Service cuaHangService;

    public TrangChu_Service(CuaHang_Repository cuaHangRepository, CuaHang_Service cuaHangService) {
        this.cuaHangRepository = cuaHangRepository;
        this.cuaHangService = cuaHangService;
    }

    /** Tối đa 5 loại da có nhiều sản phẩm đang bán nhất; chỉ loại da thật sự có trong dữ liệu. */
    public List<LoaiDa> loaiDa() {
        Map<String, String> tenTheoKhoa = new LinkedHashMap<>();
        for (String chuoi : cuaHangRepository.timLoaiDaDangBan()) {
            for (String phan : chuoi.split(",")) {
                String ten = chuanHoa(phan);
                if (!ten.isEmpty()) {
                    tenTheoKhoa.putIfAbsent(ten.toLowerCase(VI), ten);
                }
            }
        }
        return tenTheoKhoa.entrySet().stream()
            .map(muc -> new LoaiDa(muc.getValue(), muc.getValue(), demSanPham("", muc.getValue()),
                MO_TA_LOAI_DA.get(muc.getKey())))
            .filter(loai -> loai.soSanPham() > 0)
            .sorted(Comparator.comparingLong(LoaiDa::soSanPham).reversed().thenComparing(LoaiDa::ten))
            .limit(SO_LOAI_DA_TOI_DA)
            .toList();
    }

    /** Năm bước cố định; bước chỉ có liên kết khi danh mục cùng tên tồn tại và đang có sản phẩm bán. */
    public List<BuocNghiThuc> nghiThuc() {
        Map<String, String> duongDanTheoTen = new LinkedHashMap<>();
        for (var danhMuc : cuaHangRepository.demDanhMucDangBan()) {
            if (danhMuc.getSoSanPham() > 0) {
                duongDanTheoTen.putIfAbsent(chuanHoa(danhMuc.getTenDanhMuc()).toLowerCase(VI), danhMuc.getDuongDan());
            }
        }
        return NGHI_THUC.stream()
            .map(buoc -> new BuocNghiThuc(buoc.so(), buoc.ten(), buoc.moTa(),
                duongDanTheoTen.get(buoc.tenDanhMuc().toLowerCase(VI))))
            .toList();
    }

    /** Bản tĩnh khi không đọc được catalog: đủ năm bước nhưng không có liên kết. */
    public static List<BuocNghiThuc> nghiThucTinh() {
        return NGHI_THUC.stream().map(buoc -> new BuocNghiThuc(buoc.so(), buoc.ten(), buoc.moTa(), null)).toList();
    }

    /** Chỉ các hoạt chất có ít nhất một sản phẩm đang bán khớp từ khóa tìm kiếm của /products. */
    public List<HoatChat> hoatChat() {
        return HOAT_CHAT.stream()
            .map(mau -> new HoatChat(mau.ten(), mau.tuKhoa(), mau.moTa(), demSanPham(mau.tuKhoa(), "")))
            .filter(hoatChat -> hoatChat.soSanPham() > 0)
            .toList();
    }

    private long demSanPham(String q, String skinType) {
        return cuaHangService.timSanPham(q, "", "", skinType, "newest", PageRequest.of(0, 1)).getTotalElements();
    }

    static String chuanHoa(String giaTri) {
        String gon = Objects.requireNonNullElse(giaTri, "").strip().replaceAll("\\s+", " ").toLowerCase(VI);
        return gon.isEmpty() ? gon : gon.substring(0, 1).toUpperCase(VI) + gon.substring(1);
    }

    public record LoaiDa(String ten, String giaTriLoc, long soSanPham, String moTa) {}
    public record BuocNghiThuc(int so, String ten, String moTa, String duongDanDanhMuc) {}
    public record HoatChat(String ten, String tuKhoa, String moTa, long soSanPham) {}
    record BuocMau(int so, String ten, String tenDanhMuc, String moTa) {}
    record HoatChatMau(String ten, String tuKhoa, String moTa) {}
}
