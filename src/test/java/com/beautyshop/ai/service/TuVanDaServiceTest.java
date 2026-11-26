package com.beautyshop.ai.service;

import com.beautyshop.ai.RagProvider;
import com.beautyshop.ai.dto.TuVanDaRequest;
import com.beautyshop.model.BienTheSanPham;
import com.beautyshop.model.DanhMuc;
import com.beautyshop.model.SanPham;
import com.beautyshop.repository.BienTheSanPham_Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TuVanDaServiceTest {
    private BienTheSanPham_Repository repository;
    private ObjectProvider<RagProvider> provider;
    private ChatModel chatModel;
    private TuVanDaService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(BienTheSanPham_Repository.class);
        provider = mock(ObjectProvider.class);
        chatModel = mock(ChatModel.class);
        when(provider.getIfAvailable()).thenReturn(
                new RagProvider(mock(EmbeddingModel.class), chatModel));
        service = new TuVanDaService(repository, provider, new ObjectMapper());
    }

    @Test
    void chiTimSerumKhiKhachGhiRoLoaiSanPham() {
        when(repository.findDangBanConHangTheoLoai(anyString(), any(Pageable.class)))
                .thenReturn(List.of());

        String ketQua = service.tuVan(yeuCau("Da dầu, tôi muốn tìm serum."));

        assertThat(ketQua).contains("serum", "không thay bằng loại sản phẩm khác");
        verify(repository).findDangBanConHangTheoLoai(eq("serum"), any(Pageable.class));
        verify(repository).findDangBanConHangTheoLoai(eq("tinh chất"), any(Pageable.class));
        verify(repository, never()).findAllDangBanConHang(any(Pageable.class));
        verifyNoInteractions(chatModel);
    }

    @Test
    void nhanDienSuaRuaMatVaTonerKhongNhamLoaiDa() {
        assertThat(LoaiSanPham_TimKiem.nhanDien("Sữa rửa mặt cho da nhạy cảm"))
                .extracting(LoaiSanPham_TimKiem.LoaiSanPham::tenHienThi)
                .containsExactly("sữa rửa mặt");
        assertThat(LoaiSanPham_TimKiem.nhanDien("sua rua mat cho da nhay cam"))
                .extracting(LoaiSanPham_TimKiem.LoaiSanPham::tenHienThi)
                .containsExactly("sữa rửa mặt");
        assertThat(LoaiSanPham_TimKiem.nhanDien("ToNeR cho da khô"))
                .extracting(LoaiSanPham_TimKiem.LoaiSanPham::tenHienThi)
                .containsExactly("toner");
        assertThat(LoaiSanPham_TimKiem.nhanDien("tinh chat cho da dau"))
                .extracting(LoaiSanPham_TimKiem.LoaiSanPham::tenHienThi)
                .containsExactly("serum");
        assertThat(LoaiSanPham_TimKiem.nhanDien("Da dầu, thường xuyên nổi mụn"))
                .isEmpty();
    }

    @Test
    void nhuCauDuongAmKhongTuDongBiCoiLaYeuCauKemDuong() {
        when(repository.findAllDangBanConHang(any(Pageable.class))).thenReturn(List.of());

        service.tuVan(yeuCau("Da dầu, hay nổi mụn và dễ kích ứng."));

        verify(repository).findAllDangBanConHang(any(Pageable.class));
        verify(repository, never()).findDangBanConHangTheoLoai(anyString(), any(Pageable.class));
    }

    @Test
    void hienThiThongTinSanPhamTuDatabaseKhongDungVanBanModelSinhRa() {
        BienTheSanPham bienThe = sanPhamMau(42L);
        when(repository.findDangBanConHangTheoLoai(anyString(), any(Pageable.class)))
                .thenReturn(List.of(bienThe));
        when(chatModel.call(any(Prompt.class))).thenReturn(phanHoi("{\"ids\":[42]}"));

        String ketQua = service.tuVan(yeuCau("Serum cho da dầu."));

        assertThat(ketQua)
                .contains("Serum chính xác từ database", "125.000 ₫", "Niacinamide",
                        "Dùng theo hướng dẫn DB", "Số lượng còn mua được: 3")
                .doesNotContain("42");

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getContents()).contains("JSON", "ids");
        assertThat(prompt.getValue().getOptions()).isInstanceOfSatisfying(
                OllamaChatOptions.class,
                options -> {
                    assertThat(options.getFormat()).isInstanceOf(Map.class);
                    assertThat(options.getFormat().toString()).contains("ids", "integer");
                });
    }

    @Test
    void khongChapNhanIdSanPhamModelTuBia() {
        BienTheSanPham bienThe = sanPhamMau(42L);
        when(repository.findDangBanConHangTheoLoai(anyString(), any(Pageable.class)))
                .thenReturn(List.of(bienThe));
        when(chatModel.call(any(Prompt.class))).thenReturn(phanHoi("{\"ids\":[999999]}"));

        assertThatThrownBy(() -> service.tuVan(yeuCau("Serum cho da dầu.")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không có trong dữ liệu truy vấn");
    }

    @Test
    void khongChapNhanPhanHoiJsonSaiSchema() {
        BienTheSanPham bienThe = sanPhamMau(42L);
        when(repository.findDangBanConHangTheoLoai(anyString(), any(Pageable.class)))
                .thenReturn(List.of(bienThe));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(phanHoi("{\"ids\":[42],\"explanation\":\"untrusted\"}"));

        assertThatThrownBy(() -> service.tuVan(yeuCau("Serum cho da dầu.")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không đúng định dạng");
    }

    @Test
    void danhSachRongTuModelKhongTuTaoGoiY() {
        BienTheSanPham bienThe = sanPhamMau(42L);
        when(repository.findDangBanConHangTheoLoai(anyString(), any(Pageable.class)))
                .thenReturn(List.of(bienThe));
        when(chatModel.call(any(Prompt.class))).thenReturn(phanHoi("{\"ids\":[]}"));

        assertThat(service.tuVan(yeuCau("Serum cho da dầu.")))
                .contains("chưa có sản phẩm", "đủ thông tin");
    }

    @Test
    void khongChapNhanPhanHoiRongTuModel() {
        BienTheSanPham bienThe = sanPhamMau(42L);
        when(repository.findDangBanConHangTheoLoai(anyString(), any(Pageable.class)))
                .thenReturn(List.of(bienThe));
        when(chatModel.call(any(Prompt.class))).thenReturn(phanHoi("   "));

        assertThatThrownBy(() -> service.tuVan(yeuCau("Serum cho da dầu.")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không trả về danh sách sản phẩm hợp lệ");
    }

    private ChatResponse phanHoi(String noiDung) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(noiDung))));
    }

    private TuVanDaRequest yeuCau(String vanDeDa) {
        return new TuVanDaRequest("Da dầu", vanDeDa, "Ít nhạy cảm", "Dưỡng ẩm");
    }

    private BienTheSanPham sanPhamMau(long maSanPham) {
        DanhMuc danhMuc = mock(DanhMuc.class);
        when(danhMuc.getTenDanhMuc()).thenReturn("Tinh chất");
        SanPham sanPham = mock(SanPham.class);
        when(sanPham.getMaSanPham()).thenReturn(maSanPham);
        when(sanPham.getTenSanPham()).thenReturn("Serum chính xác từ database");
        when(sanPham.getDanhMuc()).thenReturn(danhMuc);
        when(sanPham.getLoaiDaPhuHop()).thenReturn("Da dầu");
        when(sanPham.getThanhPhan()).thenReturn("Niacinamide");
        when(sanPham.getMoTa()).thenReturn("Mô tả đã lưu");
        when(sanPham.getHuongDanSuDung()).thenReturn("Dùng theo hướng dẫn DB");

        BienTheSanPham bienThe = mock(BienTheSanPham.class);
        when(bienThe.getSanPham()).thenReturn(sanPham);
        when(bienThe.getTenBienThe()).thenReturn("30 ml");
        when(bienThe.getGiaBan()).thenReturn(new BigDecimal("125000"));
        when(bienThe.getSoLuongKhaDung()).thenReturn(3);
        return bienThe;
    }
}
