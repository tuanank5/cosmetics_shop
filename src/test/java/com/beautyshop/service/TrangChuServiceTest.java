package com.beautyshop.service;

import com.beautyshop.repository.CuaHang_Repository;
import com.beautyshop.repository.CuaHang_Repository.DanhMucDangBan;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrangChuServiceTest {
    private final CuaHang_Repository repository = mock(CuaHang_Repository.class);
    private final CuaHang_Service cuaHang = mock(CuaHang_Service.class);
    private final TrangChu_Service service = new TrangChu_Service(repository, cuaHang);

    private void ketQua(String q, String skinType, long tong) {
        when(cuaHang.timSanPham(eq(q), eq(""), eq(""), eq(skinType), eq("newest"), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), tong));
    }

    @Test
    void loaiDaTachChuanHoaDemTheoBoLocVaLayToiDaNam() {
        when(repository.timLoaiDaDangBan()).thenReturn(List.of(
            "Da khô, da nhạy cảm", "da KHÔ", " Da   thiếu ẩm ,Da dầu", "Da thường", "Da hỗn hợp", "Da mụn", ","));
        when(cuaHang.timSanPham(anyString(), anyString(), anyString(), anyString(), anyString(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        Map.of("Da khô", 9L, "Da nhạy cảm", 7L, "Da thiếu ẩm", 3L, "Da thường", 2L, "Da dầu", 1L, "Da hỗn hợp", 1L)
            .forEach((ten, tong) -> ketQua("", ten, tong));

        var loaiDa = service.loaiDa();

        assertThat(loaiDa).extracting(TrangChu_Service.LoaiDa::ten)
            .containsExactly("Da khô", "Da nhạy cảm", "Da thiếu ẩm", "Da thường", "Da dầu");
        assertThat(loaiDa.getFirst().soSanPham()).isEqualTo(9);
        assertThat(loaiDa.getFirst().giaTriLoc()).isEqualTo("Da khô");
        assertThat(loaiDa.getFirst().moTa()).isEqualTo("Thường cảm thấy căng, cần thêm độ ẩm.");
        // "Da mụn" has no matching products, so it is never shown.
        assertThat(loaiDa).noneMatch(loai -> loai.ten().equals("Da mụn"));
    }

    @Test
    void nghiThucLayDuongDanTheoTenDanhMucVaBoLienKetKhiKhongCoSanPham() {
        when(repository.demDanhMucDangBan()).thenReturn(List.of(
            danhMuc("Làm sạch", "lam-sach-db", 6), danhMuc("toner & xịt khoáng", "toner-db", 3),
            danhMuc("Tinh chất", "tinh-chat-db", 0), danhMuc("Dưỡng ẩm", "duong-am-db", 10)));

        var buoc = service.nghiThuc();

        assertThat(buoc).extracting(TrangChu_Service.BuocNghiThuc::ten)
            .containsExactly("Làm sạch", "Cân bằng", "Tinh chất", "Dưỡng ẩm", "Chống nắng");
        assertThat(buoc).extracting(TrangChu_Service.BuocNghiThuc::duongDanDanhMuc)
            .containsExactly("lam-sach-db", "toner-db", null, "duong-am-db", null);
        assertThat(TrangChu_Service.nghiThucTinh()).hasSize(5).allMatch(b -> b.duongDanDanhMuc() == null);
    }

    @Test
    void hoatChatChiHienKhiCoSanPhamKhopTuKhoa() {
        ketQua("hyaluronic", "", 5);
        ketQua("ceramide", "", 3);
        ketQua("salicylic", "", 1);
        ketQua("niacinamide", "", 0);

        assertThat(service.hoatChat()).extracting(TrangChu_Service.HoatChat::ten, TrangChu_Service.HoatChat::soSanPham)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Hyaluronic Acid", 5L),
                org.assertj.core.groups.Tuple.tuple("Ceramide", 3L),
                org.assertj.core.groups.Tuple.tuple("Salicylic Acid (BHA)", 1L));
    }

    @Test
    void chuanHoaVietHoaChuDau() {
        assertThat(TrangChu_Service.chuanHoa("  da   NHẠY cảm ")).isEqualTo("Da nhạy cảm");
        assertThat(TrangChu_Service.chuanHoa("")).isEmpty();
        assertThat(TrangChu_Service.chuanHoa(null)).isEmpty();
    }

    private static DanhMucDangBan danhMuc(String ten, String duongDan, long soSanPham) {
        return new DanhMucDangBan() {
            @Override public String getTenDanhMuc() { return ten; }
            @Override public String getDuongDan() { return duongDan; }
            @Override public long getSoSanPham() { return soSanPham; }
        };
    }
}
