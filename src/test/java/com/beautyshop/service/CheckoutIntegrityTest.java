package com.beautyshop.service;

import com.beautyshop.model.DiaChiNguoiDung;
import com.beautyshop.order.OrderStatus;
import com.beautyshop.payment.MomoGateway;
import com.beautyshop.payment.Momo_Settings;
import com.beautyshop.repository.*;
import com.beautyshop.repository.GioHang_Repository.MucGio;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Real service logic with isolated repository doubles; no JDBC/transactions/gateway traffic.
class CheckoutIntegrityTest {
    DonHang_Repository orders;
    GioHang_Repository cart;
    DiaChiNguoiDung_Repository addresses;
    PhieuGiamGia_Repository voucherRepository;
    TonKho_Service stock;
    ThanhToanMomo_Repository payments;
    Momo_Settings settings;
    PhieuGiamGia_Service vouchers;
    DonHang_Service service;

    private PhieuGiamGia_Service.GiamGia xemTruoc(String code, BigDecimal amount) {
        var line = new com.beautyshop.repository.GioHang_Repository.MucGio(1,"","",amount,1,1,"",null,null,null);
        return vouchers.xemGio(code,null,null,java.util.List.of(line)).hang();
    }
    @BeforeEach void setup() {
        orders=mock(DonHang_Repository.class);cart=mock(GioHang_Repository.class);
        addresses=mock(DiaChiNguoiDung_Repository.class);voucherRepository=mock(PhieuGiamGia_Repository.class);
        stock=mock(TonKho_Service.class);payments=mock(ThanhToanMomo_Repository.class);settings=mock(Momo_Settings.class);
        vouchers=new PhieuGiamGia_Service(voucherRepository,mock(SuKienQuanTri_Service.class));
        service=new DonHang_Service(orders,cart,addresses,stock,vouchers,mock(QuyenDonHang_Service.class),payments,settings,mock(SuKienQuanTri_Service.class));
        when(addresses.findByMaDiaChiAndNguoiDungMaNguoiDung(1L,7L)).thenReturn(Optional.of(mock(DiaChiNguoiDung.class)));
        when(voucherRepository.ghiSlot(anyLong(),anyLong(),anyString(),any(),anyBoolean())).thenReturn(true);
        when(voucherRepository.khoaMa("SAVE50","")).thenReturn(java.util.Set.of(50L));
        when(voucherRepository.phamVi(anyLong())).thenReturn(new PhieuGiamGia_Repository.PhamVi("HANG",null,java.util.Map.of()));
        when(cart.khoaGio(7L)).thenReturn(8L);
        when(cart.docGio(7L)).thenReturn(List.of(line("1",10,2)));
        when(cart.timBienThe(22L,true)).thenReturn(Optional.of(line("245000",10,0)));
        when(voucherRepository.timPhieuHopLe("SAVE50")).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(50,"FIXED",new BigDecimal("50000"),null,BigDecimal.ZERO)));
        when(orders.tao(anyString(),eq(7L),any(),any(),any(),any(),any(),any(),any(),any(),anyBoolean())).thenReturn(901L);
    }
    MucGio line(String price,int stockCount,int quantity) {
        return new MucGio(22,"Kem dưỡng","4 oz",new BigDecimal(price),stockCount,quantity,"QA-22",null,null,null);
    }
    @Test void quoteDoesNotConsumeVoucherAndSubmissionUsesCurrentSkuPrice() {
        assertThat(xemTruoc(" save50 ",new BigDecimal("490000")).soTien()).isEqualByComparingTo("50000");
        verify(voucherRepository,never()).ghiNhanSuDung(anyLong());
        assertThat(service.datCOD(7,1,"SAVE50")).isEqualTo(901L);
        verify(voucherRepository).khoaMa("SAVE50","");
        verify(voucherRepository).ghiSlot(901,50,"HANG",new BigDecimal("50000"),false);
        verify(orders).tao(anyString(),eq(7L),eq(50L),any(),any(),any(),eq(new BigDecimal("490000")),eq(new BigDecimal("50000")),eq(new BigDecimal("25000")),eq(new BigDecimal("465000")),eq(false));
        verify(stock).deductCOD(901,22,2);verify(orders).taoThanhToanCOD(901,new BigDecimal("465000"));
        verify(cart).xoaCacMuc(8);verifyNoInteractions(payments);
    }
    @Test void giaoNhieuChieuVaMinSpendTrenHangDuDieuKien() {
        var a = new MucGio(10,"A","",new BigDecimal("120000"),1,1,"",null,null,null,1,3,4);
        var c = new MucGio(20,"C","",new BigDecimal("80000"),1,1,"",null,null,null,2,3,4);
        var filter = java.util.Map.of(PhieuGiamGia_Repository.Chieu.SAN_PHAM,java.util.Set.of(1L,5L),
            PhieuGiamGia_Repository.Chieu.THUONG_HIEU,java.util.Set.of(4L),
            PhieuGiamGia_Repository.Chieu.NGUOI_DUNG,java.util.Set.of(7L));
        when(voucherRepository.phamVi(50L)).thenReturn(new PhieuGiamGia_Repository.PhamVi("HANG",1,filter));
        when(voucherRepository.timPhieuHopLe("SAVE50")).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(50,"PERCENT",BigDecimal.valueOf(20),BigDecimal.valueOf(30000),BigDecimal.valueOf(150000))));
        assertThatThrownBy(() -> vouchers.xemGio("SAVE50",null,7L,List.of(a,c))).hasMessageContaining("tối thiểu");
        assertThatThrownBy(() -> vouchers.xemGio("SAVE50",null,8L,List.of(a,c))).hasMessageContaining("khách");
        when(voucherRepository.luotKhach(50L,7L)).thenReturn(1);
        assertThatThrownBy(() -> vouchers.xemGio("SAVE50",null,7L,List.of(a,c))).hasMessageContaining("hết lượt");
        verify(voucherRepository,never()).ghiSlot(anyLong(),anyLong(),anyString(),any(),anyBoolean());
    }

    @Test void shipXetMinSpendSauPhanBoVaTuBoKhiPhiBangKhong() {
        var dong = List.of(line("520000",2,1));
        when(voucherRepository.timPhieuHopLe("SAVE50")).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(50,"FIXED",BigDecimal.valueOf(30000),null,BigDecimal.ZERO)));
        when(voucherRepository.phamVi(51L)).thenReturn(new PhieuGiamGia_Repository.PhamVi("VAN_CHUYEN",null,java.util.Map.of()));
        when(voucherRepository.timPhieuHopLe("SHIP")).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(51,"FIXED",BigDecimal.valueOf(10000),null,BigDecimal.valueOf(490001))));
        assertThatThrownBy(() -> vouchers.xemGio("SAVE50","SHIP",7L,dong)).hasMessageContaining("tối thiểu");
        when(voucherRepository.timPhieuHopLe("SHIP")).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(51,"FIXED",BigDecimal.valueOf(10000),null,BigDecimal.valueOf(490000))));
        assertThat(vouchers.xemGio("SAVE50","SHIP",7L,dong).tong()).isEqualByComparingTo("505000");
        var mien = vouchers.xemGio("SAVE50","SHIP",7L,List.of(line("530000",2,1)));
        assertThat(mien.tong()).isEqualByComparingTo("500000");
        assertThat(mien.ship().maVoucher()).isNull();
        var tuBo = vouchers.xemGio("SAVE50","SAVE50",7L,List.of(line("530000",2,1)));
        assertThat(tuBo.ship().maVoucher()).isNull();
    }

    @Test void shipChiXetDongThuocPhamViSauKhiTruPhanBoVaGiamToiDaBangPhi() {
        var dong = List.of(
            new MucGio(1,"A","",BigDecimal.valueOf(200000),1,1,"",null,null,null,1,1,1),
            new MucGio(2,"B","",BigDecimal.valueOf(300000),1,1,"",null,null,null,2,1,1));
        when(voucherRepository.phamVi(50L)).thenReturn(new PhieuGiamGia_Repository.PhamVi("HANG",null,
            java.util.Map.of(PhieuGiamGia_Repository.Chieu.SAN_PHAM,java.util.Set.of(1L,2L))));
        when(voucherRepository.phamVi(51L)).thenReturn(new PhieuGiamGia_Repository.PhamVi("VAN_CHUYEN",null,
            java.util.Map.of(PhieuGiamGia_Repository.Chieu.SAN_PHAM,java.util.Set.of(1L))));
        when(voucherRepository.timPhieuHopLe("SHIP")).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(51,"FIXED",BigDecimal.valueOf(90000),null,BigDecimal.valueOf(180001))));
        assertThatThrownBy(() -> vouchers.xemGio("SAVE50","SHIP",7L,dong)).hasMessageContaining("tối thiểu");
        when(voucherRepository.timPhieuHopLe("SHIP")).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(51,"FIXED",BigDecimal.valueOf(90000),null,BigDecimal.valueOf(180000))));
        var q = vouchers.xemGio("SAVE50","SHIP",7L,dong);
        assertThat(q.phanBo().get(1L)).isEqualByComparingTo("20000");
        assertThat(q.ship().soTien()).isEqualByComparingTo("25000");
        assertThat(q.tong()).isEqualByComparingTo("450000");
    }

    @Test void phanBoDongDuTheoIdOnDinh() {
        var dong = java.util.stream.LongStream.of(30,10,20).mapToObj(id ->
            new MucGio(id,"","",BigDecimal.valueOf(100000),1,1,"",null,null,null)).toList();
        var kq = PhieuGiamGia_Service.phanBo(dong,BigDecimal.valueOf(300000),BigDecimal.valueOf(10000));
        assertThat(kq.get(10L)).isEqualByComparingTo("3334");
        assertThat(kq.get(20L)).isEqualByComparingTo("3333");
        assertThat(kq.get(30L)).isEqualByComparingTo("3333");
    }

    @Test void previouslyValidQuoteDoesNotBypassExpiredVoucherAtSubmit() {
        xemTruoc("SAVE50",new BigDecimal("490000"));
        when(voucherRepository.timPhieuHopLe("SAVE50")).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.datCOD(7,1,"SAVE50")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(orders,stock,payments);verify(cart,never()).xoaCacMuc(anyLong());
    }
    @Test void foreignAddressCannotCreateOrder() {
        assertThatThrownBy(()->service.datCOD(7,999,"SAVE50")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(orders,stock,payments,voucherRepository);
    }
    @Test void emptyCartCannotCreateOrder() {
        when(cart.docGio(7L)).thenReturn(List.of());
        assertThatThrownBy(()->service.datCOD(7,1,"SAVE50")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(orders,stock,payments,voucherRepository);
    }
    @Test void stockChangeAfterQuoteIsCheckedAtSubmit() {
        xemTruoc("SAVE50",new BigDecimal("490000"));
        when(cart.timBienThe(22L,true)).thenReturn(Optional.of(line("245000",1,0)));
        assertThatThrownBy(()->service.datCOD(7,1,"SAVE50")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(orders,stock,payments);verify(voucherRepository,never()).ghiNhanSuDung(anyLong());
    }
    @Test void validSmallPercentageCanRoundToZeroAndBlankCodeDoesNotApplyVoucher() {
        when(voucherRepository.timPhieuHopLe("ONE")).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(1,"PERCENT",BigDecimal.ONE,null,BigDecimal.ZERO)));
        assertThat(xemTruoc("ONE",BigDecimal.ONE).soTien()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(xemTruoc("   ",new BigDecimal("490000")).maVoucher()).isNull();
        verify(voucherRepository,never()).ghiNhanSuDung(anyLong());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"PAID", "PENDING"})
    void chiTietMomoDungCungTrangThaiChoHienThiVaBuocTiepTheo(String trangThai) {
        var don = mock(DonHang_Repository.Don.class);
        when(don.maDonHang()).thenReturn(901L);
        when(don.phuongThuc()).thenReturn("MOMO");
        when(don.trangThai()).thenReturn(OrderStatus.PENDING_CONFIRMATION);
        when(orders.timDon(901L, false)).thenReturn(Optional.of(don));
        when(payments.trangThaiHienThi(901L)).thenReturn(Optional.of(trangThai));

        var chiTiet = service.chiTietAdmin(901L);

        assertThat(chiTiet.thanhToan()).isEqualTo(trangThai);
        assertThat(chiTiet.tiepTheo()).isEqualTo("PAID".equals(trangThai) ? OrderStatus.CONFIRMED : null);
        verify(payments).trangThaiHienThi(901L);
    }

    @Test void momoDisabledIsRejectedByUnchangedService() {
        assertThatThrownBy(()->service.datMomo(7,1,"SAVE50")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(orders,cart,stock,payments);
    }
    @Test void momoUsesOnlineReservationAndServerAmountsWithoutCallingGateway() {
        var gateway=mock(MomoGateway.class);when(gateway.maGateway()).thenReturn(MomoGateway.MA_MOCK);
        when(settings.gateway()).thenReturn(Optional.of(gateway));
        service.datMomo(7,1,"SAVE50");
        verify(voucherRepository).khoaMa("SAVE50","");verify(voucherRepository).ghiSlot(901,50,"HANG",new BigDecimal("50000"),true);
        verify(voucherRepository,never()).ghiNhanSuDung(anyLong());
        verify(stock).reserveAtomically(901,22,2);verify(stock,never()).deductCOD(anyLong(),anyLong(),anyInt());
        verify(payments).tao(eq(901L),anyString(),eq(new BigDecimal("465000")),eq(MomoGateway.MA_MOCK));
        verify(gateway,never()).tao(any());
    }
}
