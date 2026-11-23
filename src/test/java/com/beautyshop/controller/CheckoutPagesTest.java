package com.beautyshop.controller;

import com.beautyshop.entity.DiaChiNguoiDung;
import com.beautyshop.entity.NguoiDung;
import com.beautyshop.repository.GioHang_Repository.MucGio;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.*;
import com.beautyshop.service.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(Checkout_Controller.class)
@Import(Security_Config.class)
class CheckoutPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean DonHang_Service orders;
    @MockitoBean GioHang_Service cart;
    @MockitoBean DiaChiNguoiDung_Service addresses;
    @MockitoBean PhieuGiamGia_Service vouchers;
    @MockitoBean Momo_Facade momo;
    @MockitoBean LoginSuccess_Handler login;
    @MockitoBean NguoiDung_Repository users;
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService userDetails;
    PhienNguoiDung customer;

    @BeforeEach
    void setup() {
        var account = spy(new NguoiDung("fixture@example.invalid", "not-used-for-login", "Khách fixture"));
        doReturn(7L).when(account).getMaNguoiDung();
        when(users.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        var state = mock(NguoiDung_Repository.TrangThaiPhien.class);
        when(state.isKichHoat()).thenReturn(true);
        when(state.getVaiTro()).thenReturn(NguoiDung.VaiTro.CUSTOMER);
        when(users.findTrangThai(7L)).thenReturn(Optional.of(state));
        customer = (PhienNguoiDung) new NguoiDungDetails_Service(users).loadUserByUsername(account.getEmail());
        var address = mock(DiaChiNguoiDung.class);
        when(address.getMaDiaChi()).thenReturn(1L);
        when(address.isLaMacDinh()).thenReturn(true);
        when(address.getTenNguoiNhan()).thenReturn("Khách fixture");
        when(address.getDiaChiChiTiet()).thenReturn("Địa chỉ fixture");
        when(addresses.danhSach(7L)).thenReturn(List.of(address));
        when(cart.docGioCustomer(7L)).thenReturn(List.of(new MucGio(22,"Kem dưỡng","4 oz",new BigDecimal("245000"),10,2,"QA-22",null,null,null)));
        when(vouchers.xemTruoc(any(), any())).thenReturn(new PhieuGiamGia_Service.GiamGia(50L,new BigDecimal("50000")));
    }

    String quote(String voucher) throws Exception {
        return mvc.perform(post("/checkout/quote").with(user(customer)).with(csrf()).param("addressId","1").param("voucher",voucher))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    void unconfirmed(String html) {
        assertThat(html).contains("Cần cập nhật báo giá", "data-quote-valid=\"false\"")
            .doesNotContain("data-checkout-discount", "data-checkout-shipping", "data-checkout-total", "data-quote-amounts");
    }

    @Test void accessFollowsExistingRolesAndCsrf() throws Exception {
        mvc.perform(get("/checkout")).andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(get("/checkout").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        for (String path : List.of("/checkout/quote","/checkout")) {
            mvc.perform(post(path).with(user(customer)).param("addressId","1")).andExpect(status().isForbidden());
            mvc.perform(post(path).with(user("admin").roles("ADMIN")).with(csrf()).param("addressId","1")).andExpect(status().isForbidden());
        }
        verifyNoInteractions(orders,vouchers);
    }

    @Test void getStartsUnconfirmedWithoutInventingZeroDiscount() throws Exception {
        var result=mvc.perform(get("/checkout").with(user(customer))).andExpect(status().isOk())
            .andExpect(model().attribute("baoGiaHopLe",false)).andReturn();
        unconfirmed(result.getResponse().getContentAsString());
        assertThat(result.getResponse().getContentAsString()).contains("490.000 ₫", "name=\"_csrf\"", "formaction=\"/checkout/quote\"")
            .doesNotContain("gsap-", "home-motion.js", "value=\"MOMO\"");
        verifyNoInteractions(vouchers,orders);
    }

    @Test void successfulQuoteUsesOnlyServerAmountsAndRetainsContract() throws Exception {
        String html=quote("SAVE50");
        assertThat(html).contains("data-quote-valid=\"true\"", "50.000 ₫", "25.000 ₫", "465.000 ₫", "Voucher được kiểm tra: SAVE50", "Địa chỉ báo giá: Khách fixture · Địa chỉ fixture")
            .contains("name=\"addressId\"", "name=\"voucher\"", "action=\"/checkout\"");
        verify(vouchers).xemTruoc("SAVE50",new BigDecimal("490000"));
        verifyNoInteractions(orders);
    }

    @ParameterizedTest @ValueSource(strings={"", "ZERO"})
    void confirmedZeroDiscountIsNotAnErrorOrAnInventedPromotion(String voucher) throws Exception {
        when(vouchers.xemTruoc(any(),any())).thenReturn(new PhieuGiamGia_Service.GiamGia(voucher.isEmpty()?null:1L,BigDecimal.ZERO));
        assertThat(quote(voucher)).contains("data-quote-valid=\"true\"", "0 ₫", "515.000 ₫")
            .doesNotContain("đã áp dụng", "role=\"alert\"");
    }

    @ParameterizedTest @ValueSource(strings={"INVALID","EXPIRED","EXHAUSTED","MINIMUM"})
    void voucherErrorsKeepTheCodeWithoutClaimingAConfirmedTotal(String code) throws Exception {
        when(vouchers.xemTruoc(eq(code),any())).thenThrow(new IllegalArgumentException("Voucher không hợp lệ hoặc chưa đủ điều kiện."));
        String html=quote(code);unconfirmed(html);
        assertThat(html).contains("value=\""+code+"\"", "aria-invalid=\"true\"", "voucher-error", "role=\"alert\"");
        verifyNoInteractions(orders);
    }

    @ParameterizedTest @ValueSource(strings={"conflict","state"})
    void quoteFailuresRequireARefresh(String type) throws Exception {
        when(vouchers.xemTruoc(any(),any())).thenThrow(type.equals("conflict")?new ConcurrencyFailureException("fixture"):new IllegalStateException("fixture"));
        String html=quote("SAVE50");unconfirmed(html);assertThat(html).contains("Chưa thể xác nhận báo giá");
    }

    @ParameterizedTest @ValueSource(strings={"", "0", "999", "abc"})
    void invalidAddressCannotProduceConfirmedQuote(String id) throws Exception {
        String html=mvc.perform(post("/checkout/quote").with(user(customer)).with(csrf()).param("addressId",id).param("voucher","SAVE50"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","addressId"))
            .andReturn().getResponse().getContentAsString();
        unconfirmed(html);assertThat(html).contains("Chọn địa chỉ giao hàng hợp lệ của bạn.").doesNotContain("Failed to convert");
        verifyNoInteractions(vouchers,orders);
    }

    @Test void emptyCartAndMissingAddressesNeverShowPaymentActions() throws Exception {
        when(cart.docGioCustomer(7L)).thenReturn(List.of());
        String empty=quote("SAVE50");assertThat(empty).contains("Giỏ hàng đang trống").doesNotContain("data-place-order");
        when(addresses.danhSach(7L)).thenReturn(List.of());
        String missing=mvc.perform(get("/checkout").with(user(customer))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(missing).contains("thêm địa chỉ giao hàng", "/account/addresses/new").doesNotContain("data-place-order");
        verifyNoInteractions(vouchers,orders);
    }

    @ParameterizedTest @ValueSource(strings={"conflict","stock","voucher"})
    void failedSubmitAfterValidQuoteKeepsCodeAndInvalidatesAllDerivedAmounts(String type) throws Exception {
        assertThat(quote("SAVE50")).contains("465.000 ₫");
        RuntimeException failure=switch(type){case "conflict"->new ConcurrencyFailureException("fixture");case "stock"->new IllegalStateException("Không đủ tồn.");default->new IllegalArgumentException("Voucher hết lượt.");};
        when(orders.datCOD(7L,1L,"SAVE50")).thenThrow(failure);
        String html=mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("addressId","1").param("voucher","SAVE50"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        unconfirmed(html);assertThat(html).contains("value=\"SAVE50\"", "role=\"alert\"").doesNotContain("515.000 ₫");
        assertThat(quote("SAVE50")).contains("465.000 ₫");
    }

    @Test void codStillSubmitsDirectlyAndIgnoresClientPriceOrQuoteFlags() throws Exception {
        when(orders.datCOD(7L,1L,"SAVE50")).thenReturn(901L);
        mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("addressId","1").param("voucher","SAVE50")
                .param("price","1").param("tong","1").param("baoGiaHopLe","true"))
            .andExpect(redirectedUrl("/orders/901")).andExpect(flash().attribute("message","Đã đặt đơn COD."));
        verify(orders).datCOD(7L,1L,"SAVE50");verifyNoInteractions(vouchers);
    }

    @ParameterizedTest @ValueSource(strings={"abc", "0", ""})
    void invalidSubmitDoesNotCallOrderService(String address) throws Exception {
        mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("addressId",address))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","addressId"))
            .andExpect(model().attribute("baoGiaHopLe",false));
        verifyNoInteractions(orders);
    }

    @Test void oversizedVoucherAndUnknownPaymentCannotReportSuccess() throws Exception {
        mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("addressId","1").param("voucher","a".repeat(81)))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","voucher"));
        mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("addressId","1").param("payment","OTHER"))
            .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        verifyNoInteractions(orders);
    }

    @Test void momoOffDirectSubmitReliesOnExistingServiceGuard() throws Exception {
        when(orders.datMomo(7L,1L,"")).thenThrow(new IllegalStateException("MoMo chưa khả dụng."));
        String html=mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("addressId","1").param("voucher","").param("payment","MOMO"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        unconfirmed(html);assertThat(html).contains("MoMo chưa khả dụng.").doesNotContain("value=\"MOMO\"");verify(momo,never()).batDau(anyLong());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void momoOnRendersCorrectLabelAndPreservesSubmitter(boolean mockMode) throws Exception {
        when(momo.enabled()).thenReturn(true);when(momo.mock()).thenReturn(mockMode);
        assertThat(quote("SAVE50")).contains("name=\"payment\" value=\"MOMO\"",mockMode?"MÔ PHỎNG - KHÔNG THU TIỀN":"MoMo Sandbox");
        when(orders.datMomo(7L,1L,"SAVE50")).thenReturn(902L);
        when(momo.batDau(902L)).thenReturn(Optional.of(URI.create(mockMode?"/payment/momo/mock/902":"https://sandbox.example.invalid/fixture")));
        mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("addressId","1").param("voucher","SAVE50").param("payment","MOMO"))
            .andExpect(redirectedUrl(mockMode?"/payment/momo/mock/902":"https://sandbox.example.invalid/fixture"));
        verify(orders).datMomo(7L,1L,"SAVE50");verify(orders,never()).datCOD(anyLong(),anyLong(),any());
    }

    @ParameterizedTest @ValueSource(booleans={true,false})
    void momoCommittedButGatewayUnclearReturnsPendingOrder(boolean throwsError) throws Exception {
        when(orders.datMomo(7L,1L,"SAVE50")).thenReturn(902L);
        if(throwsError)when(momo.batDau(902L)).thenThrow(new IllegalStateException("fixture gateway"));
        else when(momo.batDau(902L)).thenReturn(Optional.empty());
        mvc.perform(post("/checkout").with(user(customer)).with(csrf()).param("addressId","1").param("voucher","SAVE50").param("payment","MOMO"))
            .andExpect(redirectedUrl("/orders/902")).andExpect(flash().attributeExists("error")).andExpect(flash().attributeCount(1));
    }
}
