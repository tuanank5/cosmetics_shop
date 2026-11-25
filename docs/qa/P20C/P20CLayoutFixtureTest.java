package com.beautyshop.controller.admin;

import com.beautyshop.repository.DanhGia_Store.ThongKeDanhGia;
import com.beautyshop.repository.ThongKe_Repository.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Optional QA fixture compiled into target/test-classes only. No database or account is created. */
class P20CLayoutFixtureTest extends AdminDashboardPagesTest {
    @Test
    @WithMockUser(roles = "ADMIN")
    void exportLayoutFixtures() throws Exception {
        when(repository.doanhThu(any(), any(), any())).thenReturn(new DoanhThu(money(30_250_000), 108, money(24_200_000), 95));
        when(repository.demDon(any(), any(), any())).thenReturn(new DemDon(142, 9, 125, 11));
        when(repository.demDonDaGiao(any(), any(), any())).thenReturn(new DemGiao(97, 89));
        when(repository.khachMoi(any(), any(), any())).thenReturn(new long[] {12, 10});
        when(repository.hienTai(any())).thenReturn(new HienTai(14, 1, 1));
        when(repository.demSkuSapHet(anyInt())).thenReturn(6L);
        when(repository.tachDoanhThu(any(), any())).thenReturn(new TachDoanhThu(money(28_250_000), money(2_000_000), true));
        when(repository.doanhThuTheoNgay(any(), any())).thenAnswer(call -> daily(call.getArgument(0), call.getArgument(1)));
        when(repository.donTheoNgay(any(), any())).thenAnswer(call -> daily(call.getArgument(0), call.getArgument(1)));
        when(repository.donTheoTrangThai(any(), any())).thenReturn(List.of(
            new Nhom("DELIVERED", 97, money(24_000_000)), new Nhom("CANCELLED", 9, money(2_000_000)),
            new Nhom("SHIPPING", 12, money(3_000_000)), new Nhom("PACKING", 8, money(2_500_000)),
            new Nhom("CONFIRMED", 4, money(1_200_000)), new Nhom("PENDING_CONFIRMATION", 2, money(500_000)),
            new Nhom("PENDING_PAYMENT", 2, money(750_000)), new Nhom("EXPIRED", 5, money(1_300_000))));
        when(repository.topSanPhamChiTiet(any(), any(), anyInt())).thenReturn(List.of(
            product(1, "Serum dịu nhẹ", "Atelier", 26, 5_200_000),
            product(2, "Kem phục hồi", "Jardin", 21, 4_300_000),
            product(3, "Sữa rửa mặt", "Lumière", 17, 3_100_000),
            product(4, "Kem chống nắng", "Atelier", 13, 2_000_000),
            product(5, "Mặt nạ dưỡng ẩm", "Lumière", 8, 1_400_000)));
        when(repository.doanhThuTheoDanhMuc(any(), any())).thenReturn(List.of(
            new Nhom("Dưỡng da", 56, money(12_400_000)), new Nhom("Làm sạch", 17, money(3_100_000))));
        when(repository.doanhThuTheoThuongHieu(any(), any())).thenReturn(List.of(
            new Nhom("Atelier", 39, money(7_200_000)), new Nhom("Lumière", 25, money(4_500_000)),
            new Nhom("Jardin", 21, money(4_300_000))));
        when(repository.phuongThucThanhToan(any(), any())).thenReturn(List.of(
            new Nhom("COD", 71, money(19_000_000)), new Nhom("MOMO", 37, money(11_250_000))));
        when(repository.trangThaiThanhToan(any(), any(), any())).thenReturn(List.of(
            new NhomThanhToan("COD", 71, 11, 9), new NhomThanhToan("MOMO", 37, 2, 5)));
        when(danhGiaStore.thongKe(any(), any())).thenReturn(new ThongKeDanhGia(4, new long[] {1, 2, 7, 14, 22}));
        var utc = LocalDateTime.of(2026, 11, 22, 4, 15);
        when(repository.doiTraCho()).thenReturn(List.of(new DoiTraCho(1, "FIXTURE-01", "Lý do mô phỏng để kiểm tra bảng.", utc)));
        when(repository.canhBaoThanhToan(any(), any(), any(), nullable(String.class))).thenReturn(List.of(
            new CanhBaoThanhToan(2, "FIXTURE-02", money(250_000), "FAILED", utc),
            new CanhBaoThanhToan(3, "FIXTURE-03", money(340_000), "PENDING", utc.minusHours(1))));
        when(repository.voucherSapHetHan(any())).thenReturn(List.of(new VoucherSapHetHan(1, "FIXTURE", utc.plusDays(4), 12)));
        var directory = Files.createDirectories(Path.of("target/p20c-layout"));
        for (String range : List.of("7d", "30d", "90d")) {
            var result = mvc.perform(get("/admin/dashboard").param("range", range)).andExpect(status().isOk()).andReturn();
            write(directory.resolve(range + ".html"), result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        }
        var invalid = mvc.perform(get("/admin/dashboard").param("from", "2026-11-10").param("to", "2026-11-01"))
            .andExpect(status().isOk()).andReturn();
        write(directory.resolve("invalid.html"), invalid.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static BigDecimal money(long value) { return BigDecimal.valueOf(value); }

    private static SanPhamBanChay product(long id, String name, String brand, long quantity, long amount) {
        return new SanPhamBanChay(name, quantity, money(amount), brand, "fixture-" + id, id, true);
    }

    private static List<Ngay> daily(LocalDateTime start, LocalDateTime end) {
        return start.plusHours(7).toLocalDate().datesUntil(end.plusHours(7).toLocalDate()).map(date -> {
            long count = (date.toEpochDay() * 13 % 9) + date.getDayOfWeek().getValue() % 4;
            return new Ngay(date, money(count * (200_000L + (date.getDayOfMonth() % 7) * 52_000L)), count);
        }).toList();
    }

    private static void write(Path file, String html) throws Exception {
        Files.writeString(file, html.replaceAll("name=\"_csrf\" value=\"[^\"]*\"", "name=\"_csrf\" value=\"\""));
    }
}
