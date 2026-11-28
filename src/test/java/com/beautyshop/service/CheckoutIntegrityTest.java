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

    @BeforeEach void setup() {
        orders=mock(DonHang_Repository.class);cart=mock(GioHang_Repository.class);
        addresses=mock(DiaChiNguoiDung_Repository.class);voucherRepository=mock(PhieuGiamGia_Repository.class);
        stock=mock(TonKho_Service.class);payments=mock(ThanhToanMomo_Repository.class);settings=mock(Momo_Settings.class);
        vouchers=new PhieuGiamGia_Service(voucherRepository,mock(SuKienQuanTri_Service.class));
        service=new DonHang_Service(orders,cart,addresses,stock,vouchers,mock(QuyenDonHang_Service.class),payments,settings,mock(SuKienQuanTri_Service.class));
        when(addresses.findByMaDiaChiAndNguoiDungMaNguoiDung(1L,7L)).thenReturn(Optional.of(mock(DiaChiNguoiDung.class)));
        when(cart.khoaGio(7L)).thenReturn(8L);
        when(cart.docGio(7L)).thenReturn(List.of(line("1",10,2)));
        when(cart.timBienThe(22L,true)).thenReturn(Optional.of(line("245000",10,0)));
        when(voucherRepository.timPhieuHopLe(eq("SAVE50"),anyBoolean())).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(50,"FIXED",new BigDecimal("50000"),null,BigDecimal.ZERO)));
        when(orders.tao(anyString(),eq(7L),any(),any(),any(),any(),any(),any(),any(),any(),anyBoolean())).thenReturn(901L);
    }
    MucGio line(String price,int stockCount,int quantity) {
        return new MucGio(22,"Kem dưỡng","4 oz",new BigDecimal(price),stockCount,quantity,"QA-22",null,null,null);
    }
    @Test void quoteDoesNotConsumeVoucherAndSubmissionUsesCurrentSkuPrice() {
        assertThat(vouchers.xemTruoc(" save50 ",new BigDecimal("490000")).soTien()).isEqualByComparingTo("50000");
        verify(voucherRepository,never()).ghiNhanSuDung(anyLong());
        assertThat(service.datCOD(7,1,"SAVE50")).isEqualTo(901L);
        verify(voucherRepository).timPhieuHopLe("SAVE50",true);
        verify(voucherRepository).ghiNhanSuDung(50);
        verify(orders).tao(anyString(),eq(7L),eq(50L),any(),any(),any(),eq(new BigDecimal("490000")),eq(new BigDecimal("50000")),eq(new BigDecimal("25000")),eq(new BigDecimal("465000")),eq(false));
        verify(stock).deductCOD(901,22,2);verify(orders).taoThanhToanCOD(901,new BigDecimal("465000"));
        verify(cart).xoaCacMuc(8);verifyNoInteractions(payments);
    }
    @Test void previouslyValidQuoteDoesNotBypassExpiredVoucherAtSubmit() {
        vouchers.xemTruoc("SAVE50",new BigDecimal("490000"));
        when(voucherRepository.timPhieuHopLe("SAVE50",true)).thenReturn(Optional.empty());
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
        vouchers.xemTruoc("SAVE50",new BigDecimal("490000"));
        when(cart.timBienThe(22L,true)).thenReturn(Optional.of(line("245000",1,0)));
        assertThatThrownBy(()->service.datCOD(7,1,"SAVE50")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(orders,stock,payments);verify(voucherRepository,never()).ghiNhanSuDung(anyLong());
    }
    @Test void validSmallPercentageCanRoundToZeroAndBlankCodeDoesNotApplyVoucher() {
        when(voucherRepository.timPhieuHopLe("ONE",false)).thenReturn(Optional.of(
            new PhieuGiamGia_Repository.Phieu(1,"PERCENT",BigDecimal.ONE,null,BigDecimal.ZERO)));
        assertThat(vouchers.xemTruoc("ONE",BigDecimal.ONE).soTien()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(vouchers.xemTruoc("   ",new BigDecimal("490000")).maVoucher()).isNull();
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
        verify(voucherRepository).timPhieuHopLe("SAVE50",true);verify(voucherRepository).tamGiu(50);
        verify(voucherRepository,never()).ghiNhanSuDung(anyLong());
        verify(stock).reserveAtomically(901,22,2);verify(stock,never()).deductCOD(anyLong(),anyLong(),anyInt());
        verify(payments).tao(eq(901L),anyString(),eq(new BigDecimal("465000")),eq(MomoGateway.MA_MOCK));
        verify(gateway,never()).tao(any());
    }
}
