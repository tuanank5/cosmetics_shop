package com.beautyshop.controller;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.repository.GioHang_Repository.MucGio;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.security.Security_Config;
import com.beautyshop.service.GioHang_Service;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GioHang_Controller.class)
@Import(Security_Config.class)
class GioHangPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean GioHang_Service gioHang;
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDung;

    @Test
    void gioTrongCoLoiMuaSamVaKhongCoNutThanhToan() throws Exception {
        mvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Giỏ hàng đang trống.")))
            .andExpect(content().string(containsString("Khám phá sản phẩm")))
            .andExpect(content().string(not(containsString("href=\"/checkout\""))));
    }

    @Test
    void tamTinhChinhXacTheoSoLuongTrongGioKeCaKhiTonGiam() throws Exception {
        var lon = new MucGio(11, "Tinh chất <mẫu>", "30 ml", new BigDecimal("999999999999999"), 3, 10, "QA-11",
            null, null, null);
        var nho = new MucGio(22, "Kem dưỡng", "50 g", new BigDecimal("7"), 0, 1, "QA-22",
            null, null, null);
        when(gioHang.docGioKhach(any())).thenReturn(List.of(lon, nho));
        mvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("9.999.999.999.999.990 ₫")))
            .andExpect(content().string(containsString("9.999.999.999.999.997 ₫")))
            .andExpect(content().string(containsString("Tinh chất &lt;mẫu&gt;")))
            .andExpect(content().string(containsString("Số lượng trong giỏ vượt tồn hiện tại")))
            .andExpect(content().string(containsString("Biến thể hiện không còn hàng.")))
            .andExpect(content().string(containsString("action=\"/cart/items/11/quantity\"")))
            .andExpect(content().string(containsString("name=\"quantity\" value=\"0\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("href=\"/checkout\"")));
    }

    @Test
    void haiSkuCungSanPhamGiuDungAnhThuongHieuVaLienKet() throws Exception {
        String url = "/uploads/sku/00000000-0000-4000-8000-000000000026.png";
        var lon = new MucGio(11, "Moisturizing Cream", "16 oz", new BigDecimal("410000"), 6, 2,
            "DEMO-026", url, "Vanicream", "demo-r026");
        var nho = new MucGio(22, "Moisturizing Cream", "4 oz", new BigDecimal("245000"), 0, 1,
            "DEMO-026-B", null, "Vanicream", "demo-r026");
        when(gioHang.docGioKhach(any())).thenReturn(List.of(lon, nho));
        mvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Vanicream")))
            .andExpect(content().string(containsString("data=\"" + url + "\"")))
            .andExpect(content().string(containsString("href=\"/products/demo-r026?variant=11\"")))
            .andExpect(content().string(containsString("href=\"/products/demo-r026?variant=22\"")))
            .andExpect(content().string(containsString("aria-label=\"Moisturizing Cream, 4 oz (SKU DEMO-026-B)\"")))
            .andExpect(content().string(containsString("1.065.000 ₫")));
    }

    @Test
    void thieuMetadataVanHienDongGioVaKhongNapAnhNgoaiRoute() throws Exception {
        var muc = new MucGio(11, "Moisturizing Cream", "16 oz", new BigDecimal("410000"), 0, 1,
            "DEMO-026", "https://example.invalid/photo.png", null, null);
        when(gioHang.docGioKhach(any())).thenReturn(List.of(muc));
        mvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<span>Moisturizing Cream</span>")))
            .andExpect(content().string(containsString("class=\"cart-monogram\"")))
            .andExpect(content().string(not(containsString("<object"))))
            .andExpect(content().string(not(containsString("?variant="))))
            .andExpect(content().string(containsString("action=\"/cart/items/11/quantity\"")))
            .andExpect(content().string(containsString("Biến thể hiện không còn hàng.")));
    }

    @Test
    void xoaVanGuiQuantityBangKhongVaCanCsrf() throws Exception {
        mvc.perform(post("/cart/items/11/quantity").param("quantity", "0"))
            .andExpect(status().isForbidden());
        when(gioHang.datSoLuongKhach(any(), eq(11L), eq(0))).thenReturn(new GioKhach(null, java.util.Map.of()));
        mvc.perform(post("/cart/items/11/quantity").param("quantity", "0").with(csrf()))
            .andExpect(redirectedUrl("/cart"))
            .andExpect(flash().attribute("message", "Đã xóa SKU khỏi giỏ."));
        verify(gioHang).datSoLuongKhach(any(), eq(11L), eq(0));
    }
}
