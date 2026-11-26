package com.beautyshop.controller;

import com.beautyshop.model.NguoiDung;
import com.beautyshop.repository.DanhGia_Repository.MucMua;
import com.beautyshop.repository.DanhGia_Repository.SanPhamLoc;
import com.beautyshop.repository.DanhGia_Store.BoLocDanhGia;
import com.beautyshop.repository.DanhGia_Store.DanhGiaAdmin;
import com.beautyshop.repository.DanhGia_Store.DanhGiaCuaKhach;
import com.beautyshop.repository.DanhGia_Store.TrangThaiLoc;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.security.NguoiDungDetails_Service;
import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.security.Security_Config;
import com.beautyshop.service.SauMua_Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** P23: form đánh giá (sao dạng radio, bộ đếm ký tự, lỗi kho thân thiện). Trang Admin: AdminDanhGiaPagesTest. */
@WebMvcTest(SauMua_Controller.class)
@Import(Security_Config.class)
class DanhGiaPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean SauMua_Service sauMua;
    @MockitoBean LoginSuccess_Handler login;
    @MockitoBean NguoiDung_Repository users;
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService userDetails;
    PhienNguoiDung khach;
    private final MucMua muc = new MucMua(30, "Serum <b>", "30 ml", 9, "DELIVERED", 300, "Thịnh P.");

    @BeforeEach
    void setup() {
        var account = spy(new NguoiDung("fixture@example.invalid", "not-used-for-login", "Khách fixture"));
        doReturn(7L).when(account).getMaNguoiDung();
        when(users.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        var state = mock(NguoiDung_Repository.TrangThaiPhien.class);
        when(state.isKichHoat()).thenReturn(true);
        when(state.getVaiTro()).thenReturn(NguoiDung.VaiTro.CUSTOMER);
        when(users.findTrangThai(7L)).thenReturn(Optional.of(state));
        khach = (PhienNguoiDung) new NguoiDungDetails_Service(users).loadUserByUsername(account.getEmail());
        when(sauMua.xemDanhGia(7L, 100L)).thenReturn(new SauMua_Service.TrangDanhGia(muc, null, false));
    }

    @Test
    void formDanhGiaLaNhomRadioSaoCoBoDemVaGioiHan1000() throws Exception {
        String html = mvc.perform(get("/orders/items/100/review").with(user(khach)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<legend>Số sao", "maxlength=\"1000\"", "data-char-count", "/js/review-form.js",
            "action=\"/orders/items/100/review\"", "Serum &lt;b&gt; · 30 ml", "Gửi đánh giá");
        // 5 radio cùng name, mỗi radio có nhãn đọc được "n sao" (dùng được bằng bàn phím, không cần JS).
        assertThat(html.split("type=\"radio\"", -1)).hasSize(6);
        for (int sao = 1; sao <= 5; sao++) {
            assertThat(html).contains("value=\"" + sao + "\"", "<span class=\"visually-hidden\">" + sao + " sao</span>");
        }
        assertThat(html).doesNotContain("<select", "style=\"", "onclick", "th:field", "th:text");
    }

    @Test
    void formSuaHienSaoDaChonVaBaoChoDuyet() throws Exception {
        when(sauMua.xemDanhGia(7L, 100L)).thenReturn(new SauMua_Service.TrangDanhGia(muc,
            new DanhGiaCuaKhach("65f000000000000000000001", 4, "Thấm nhanh", false), false));
        mvc.perform(get("/orders/items/100/review").with(user(khach)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"4\" required id=\"soSao4\" name=\"soSao\" checked=\"checked\"")))
            .andExpect(content().string(containsString("action=\"/orders/items/100/review/edit\"")))
            .andExpect(content().string(containsString("chờ duyệt hoặc đã ẩn")))
            .andExpect(content().string(containsString("<span data-char-current>10</span>")))
            .andExpect(content().string(containsString("Lưu và gửi duyệt lại")));
    }

    @Test
    void khoDanhGiaLoiKhiLuuThiHienLoiThanThienGiuNoiDung() throws Exception {
        when(sauMua.luuDanhGia(eq(7L), eq(100L), any(), anyBoolean())).thenThrow(new DataAccessResourceFailureException("mongo tat"));
        mvc.perform(post("/orders/items/100/review").with(user(khach)).with(csrf()).param("soSao", "5").param("noiDung", "Giữ lại"))
            .andExpect(status().isOk())
            .andExpect(view().name("order/review"))
            .andExpect(content().string(containsString("Tạm thời chưa lưu được đánh giá")))
            .andExpect(content().string(containsString(">Giữ lại</textarea>")));
    }

    @Test
    void noiDungQuaDaiBiChanTaiForm() throws Exception {
        mvc.perform(post("/orders/items/100/review").with(user(khach)).with(csrf()).param("soSao", "5")
                .param("noiDung", "a".repeat(1001)))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "noiDung"))
            .andExpect(content().string(containsString("Nội dung tối đa 1.000 ký tự.")));
        verify(sauMua, org.mockito.Mockito.never()).luuDanhGia(anyLong(), anyLong(), any(), anyBoolean());
    }
}
