package com.beautyshop.controller.admin;

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
import static org.hamcrest.Matchers.matchesPattern;
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

/** P23: trang Admin /admin/reviews — lọc trạng thái/sao/sản phẩm, phân trang, duyệt theo phiên bản đã xem, kho lỗi vẫn hiện trang. */
@WebMvcTest(AdminSauMua_Controller.class)
@Import(Security_Config.class)
class AdminDanhGiaPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean SauMua_Service sauMua;
    @MockitoBean com.beautyshop.service.KiemDinhDoiTra_Service kiemDinh;
    @MockitoBean LoginSuccess_Handler login;
    @MockitoBean NguoiDung_Repository users;
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService userDetails;

    @Test
    void adminLocTrangThaiSaoSanPhamPhanTrangVaGuiPhienBan() throws Exception {
        var choDuyet = new DanhGiaAdmin("65f000000000000000000001", 30, "Serum", 9, "Thịnh P.", 4, "Thấm <i>", false, false, "0",
            Instant.parse("2026-11-20T18:30:00Z"));
        var daAn = new DanhGiaAdmin("65f000000000000000000002", 30, "Serum", 10, "An N.", 4, "", false, true, "3",
            Instant.parse("2026-11-19T01:00:00Z"));
        when(sauMua.danhSachDanhGiaAdmin(any(), anyInt())).thenReturn(new SauMua_Service.TrangDanhGiaAdmin(
            new PageImpl<>(List.of(choDuyet, daAn), PageRequest.of(1, 20), 42), List.of(new SanPhamLoc(30, "Serum")), false));

        mvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN"))
                .param("status", "CHO_DUYET").param("stars", "4").param("product", "30").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<option value=\"CHO_DUYET\" selected=\"selected\">Chờ duyệt</option>")))
            .andExpect(content().string(containsString("<option value=\"4\" selected=\"selected\">4 sao</option>")))
            .andExpect(content().string(containsString("<option value=\"30\" selected=\"selected\">Serum</option>")))
            .andExpect(content().string(containsString("21–40 trong tổng 42 đánh giá")))
            .andExpect(content().string(not(containsString("id=\"review-notice\""))))
            .andExpect(content().string(containsString("Thấm &lt;i&gt;")))
            .andExpect(content().string(containsString("4/5 · Thịnh P. · 21/11/2026")))
            .andExpect(content().string(containsString("action=\"/admin/reviews/65f000000000000000000001/visibility\"")))
            .andExpect(model().attributeDoesNotExist("focusReviewId", "focusReviewResults"))
            .andExpect(content().string(containsString("name=\"revision\" value=\"3\"")))
            .andExpect(content().string(containsString("name=\"page\" value=\"1\"")))
            .andExpect(content().string(containsString("<a href=\"/admin/reviews\" aria-current=\"page\">")))
            .andExpect(content().string(containsString(">Chờ duyệt</span>")))
            .andExpect(content().string(containsString(">Đã ẩn</span>")))
            .andExpect(content().string(containsString("Trang 2 / 3")))
            .andExpect(content().string(containsString("/admin/reviews?status=CHO_DUYET&amp;stars=4&amp;product=30&amp;page=2")));
        verify(sauMua).danhSachDanhGiaAdmin(new BoLocDanhGia(TrangThaiLoc.CHO_DUYET, 4, 30L), 1);

        mvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN"))
                .param("status", "CHO_DUYET").param("stars", "4").param("product", "30").param("page", "1")
                .param("focus", choDuyet.ma()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("focusReviewOrder", choDuyet.ma()))
            .andExpect(content().string(matchesPattern("(?s).*<li\\b(?=[^>]*id=\"review-" + choDuyet.ma()
                + "\")(?=[^>]*tabindex=\"-1\")(?=[^>]*data-review-return-focus=\"true\")[^>]*>.*")));
    }

    @Test
    void clampTrangCuoiGiuFlashVaDatFocusVaoKetQua() throws Exception {
        var danhGia = new DanhGiaAdmin("65f000000000000000000002", 30, "Serum", 10, "Fixture", 4, "Ghi chú",
            false, false, "0", Instant.EPOCH);
        when(sauMua.danhSachDanhGiaAdmin(any(), anyInt())).thenReturn(new SauMua_Service.TrangDanhGiaAdmin(
            new PageImpl<>(List.of(danhGia), PageRequest.of(0, 20), 20), List.of(new SanPhamLoc(30, "Serum")), false));
        var doiTrang = mvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN"))
                .param("status", "CHO_DUYET").param("stars", "4").param("product", "30").param("page", "1")
                .flashAttr("danhGiaVuaXuLy", "65f000000000000000000001").flashAttr("message", "Đã duyệt đánh giá."))
            .andExpect(redirectedUrl("/admin/reviews?status=CHO_DUYET&stars=4&product=30&page=0"))
            .andExpect(flash().attribute("message", "Đã duyệt đánh giá.")).andReturn();

        mvc.perform(get(java.net.URI.create(doiTrang.getResponse().getRedirectedUrl()))
                .with(user("admin").roles("ADMIN")).flashAttrs(doiTrang.getFlashMap()))
            .andExpect(status().isOk())
            .andExpect(model().attribute("focusReviewResults", true))
            .andExpect(content().string(containsString("autofocus=\"autofocus\" aria-describedby=\"review-notice\"")))
            .andExpect(content().string(containsString("20 đánh giá")))
            .andExpect(content().string(containsString("Đã duyệt đánh giá.")))
            .andExpect(content().string(not(containsString("65f000000000000000000001"))));

        mvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN"))
                .flashAttr("danhGiaVuaXuLy", danhGia.ma()).flashAttr("error", "Vui lòng kiểm tra lại."))
            .andExpect(model().attribute("focusReviewId", danhGia.ma()))
            .andExpect(model().attribute("focusReviewResults", false));
    }

    @Test
    void giuBoLocVaTrangSauKhiDuyetHoacAn() throws Exception {
        for (String visible : List.of("true", "false")) {
            mvc.perform(post("/admin/reviews/123/visibility").with(user("admin").roles("ADMIN")).with(csrf())
                    .param("visible", visible).param("revision", "0").param("status", "CHO_DUYET")
                    .param("stars", "4").param("product", "30").param("page", "2").param("next", "/"))
                .andExpect(redirectedUrl("/admin/reviews?status=CHO_DUYET&stars=4&product=30&page=2"));
        }
        mvc.perform(post("/admin/reviews/123/visibility").with(user("admin").roles("ADMIN")).with(csrf())
                .param("visible", "true").param("status", "invalid").param("stars", "abc")
                .param("product", "9999999999999999999").param("page", "10001"))
            .andExpect(redirectedUrl("/admin/reviews"));
        verify(sauMua).datHienThi("123", true, "0");
        verify(sauMua).datHienThi("123", false, "0");
    }

    @Test
    void adminBoLocKhongHopLeVeMacDinhVaKhoLoiVanHienTrang() throws Exception {
        when(sauMua.danhSachDanhGiaAdmin(any(), anyInt())).thenReturn(new SauMua_Service.TrangDanhGiaAdmin(
            Page.empty(PageRequest.of(0, 20)), List.of(), true));
        mvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN")).param("status", "'; db.drop()").param("stars", "9"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tạm thời chưa tải được đánh giá")))
            .andExpect(content().string(not(containsString("Phân trang duyệt đánh giá"))))
            .andExpect(content().string(not(containsString("Không có đánh giá phù hợp."))));
        verify(sauMua).danhSachDanhGiaAdmin(BoLocDanhGia.TAT_CA, 0);
    }

    @Test
    void adminDuyetSaiPhienBanHoacKhoLoiThiBaoLoiQuaFlash() throws Exception {
        doThrow(new IllegalStateException("Đánh giá đã thay đổi. Vui lòng kiểm tra lại."))
            .when(sauMua).datHienThi("65f000000000000000000001", true, "0");
        mvc.perform(post("/admin/reviews/65f000000000000000000001/visibility").with(user("admin").roles("ADMIN")).with(csrf())
                .param("visible", "true").param("revision", "0"))
            .andExpect(redirectedUrl("/admin/reviews"))
            .andExpect(flash().attribute("error", "Đánh giá đã thay đổi. Vui lòng kiểm tra lại."));
        mvc.perform(post("/admin/reviews/65f000000000000000000001/visibility").with(user("khach").roles("CUSTOMER")).with(csrf())
                .param("visible", "true").param("revision", "0"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/reviews/65f000000000000000000001/visibility").with(user("admin").roles("ADMIN"))
                .param("visible", "true").param("revision", "0"))
            .andExpect(status().isForbidden());
    }
}
