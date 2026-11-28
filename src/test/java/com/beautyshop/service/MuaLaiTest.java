package com.beautyshop.service;

import com.beautyshop.repository.DonHang_Repository;
import com.beautyshop.repository.GioHang_Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MuaLaiTest {
    @ParameterizedTest
    @CsvSource({"true,5,1,4,Đã thêm đủ", "true,3,1,3,Chỉ thêm", "true,1,1,1,giỏ đã đủ",
        "true,0,0,0,hết hàng", "false,0,1,1,không còn bán"})
    void dungTonVaSoLuongHienTai(boolean ban, int ton, int co, int sau, String nhan) {
        var gio = mock(GioHang_Repository.class);
        var don = mock(DonHang_Repository.class);
        var quyen = mock(QuyenDonHang_Service.class);
        when(quyen.kiemTraSoHuu(3)).thenReturn(1L);
        var service = new GioHang_Service(gio, don, quyen);
        when(gio.ghiNhanGop(anyString(), eq(1L))).thenReturn(true);
        when(gio.khoaGio(1)).thenReturn(2L);
        when(don.chiTiet(3)).thenReturn(List.of(new DonHang_Repository.ChiTiet(4,"sku","Hàng","A",BigDecimal.TEN,3,null)));
        when(gio.timBienThe(4,true)).thenReturn(Optional.of(new GioHang_Repository.MucGio(4,"Hàng","A",
            BigDecimal.ONE,ton,0,"sku",null,"Hãng",ban ? "hang" : null)));
        when(gio.soLuong(2,4)).thenReturn(co);
        assertThat(service.muaLai(3,UUID.randomUUID())).singleElement().asString().contains(nhan);
        if (co == sau) verify(gio, never()).luuSoLuong(anyLong(),anyLong(),anyInt());
        else verify(gio).luuSoLuong(2,4,sau);
    }
}
