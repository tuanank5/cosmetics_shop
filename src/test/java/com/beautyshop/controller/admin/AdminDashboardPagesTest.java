package com.beautyshop.controller.admin;

import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.repository.ThongKe_Repository;
import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.security.Security_Config;
import com.beautyshop.service.ThongKe_Service;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** P20: quyền truy cập, bộ lọc kỳ và khung trang Dashboard (repository thống kê được giả lập). */
@WebMvcTest(AdminTongQuan_Controller.class)
@Import({Security_Config.class, ThongKe_Service.class})
class AdminDashboardPagesTest {
    @Autowired MockMvc mvc;
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDungRepository;
    @MockitoBean ThongKe_Repository repository;

    @BeforeEach
    void stub() {
        when(repository.doanhThu(any(), any(), any())).thenReturn(
            new ThongKe_Repository.DoanhThu(new BigDecimal("1250000"), 3, BigDecimal.ZERO, 0));
        when(repository.demDon(any(), any(), any())).thenReturn(new ThongKe_Repository.DemDon(4, 1, 2, 0));
        when(repository.khachMoi(any(), any(), any())).thenReturn(new long[] {2, 1});
        when(repository.hienTai(any())).thenReturn(new ThongKe_Repository.HienTai(3, 1, 0, 2));
        when(repository.skuSapHet(anyInt(), anyInt())).thenReturn(java.util.List.of(
            new ThongKe_Repository.SkuSapHet(9, 4, "DEMO-004", "Serum", "30 ml", 1)));
    }

    @Test
    void guestBiChuyenToiDangNhap() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerBiTuChoi() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminXemDashboardVaDuongDanGocChuyenHuong() throws Exception {
        mvc.perform(get("/admin")).andExpect(redirectedUrl("/admin/dashboard"));
        mvc.perform(get("/admin/dashboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/dashboard"))
            .andExpect(model().attributeExists("tongQuan"))
            .andExpect(content().string(containsString("<a href=\"/admin/dashboard\" aria-current=\"page\">Tổng quan</a>")))
            .andExpect(content().string(containsString("Doanh thu thực thu")))
            .andExpect(content().string(containsString("1.250.000 ₫")))
            .andExpect(content().string(containsString("Kỳ trước chưa có dữ liệu để so sánh")))
            .andExpect(content().string(containsString("Tăng 100,0% so với kỳ trước")))
            .andExpect(content().string(containsString("Tăng 25,0 điểm % so với kỳ trước")))
            .andExpect(content().string(containsString("/admin/orders?status=PENDING_CONFIRMATION&amp;status=CONFIRMED&amp;status=PACKING")))
            .andExpect(content().string(containsString("/admin/catalog/products/4/variants/9/edit")))
            .andExpect(content().string(containsString("<details class=\"chart-table\" open data-chart-table>")))
            .andExpect(content().string(containsString("src=\"/js/vendor/chart-4.6.1.umd.min.js\"")))
            .andExpect(content().string(containsString("src=\"/js/dashboard.js\"")))
            .andExpect(content().string(containsString("Kỳ này chưa có đơn đã thu tiền.")))
            // Admin header: logout icon button inside a CSRF-protected POST form, light lockup.
            .andExpect(content().string(containsString("class=\"header-account-form\" method=\"post\" action=\"/logout\"")))
            .andExpect(content().string(containsString("<span class=\"header-account-label\">Đăng xuất</span>")))
            .andExpect(content().string(containsString("beautyshop-lockup-light.svg")))
            .andExpect(content().string(not(containsString("style="))))
            .andExpect(content().string(not(containsString("onclick"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void boLocSaiHienThongBaoKhongPhai500() throws Exception {
        mvc.perform(get("/admin/dashboard").param("range", "999d"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Kỳ chỉ nhận 7d, 30d hoặc 90d")));
        mvc.perform(get("/admin/dashboard").param("from", "2026-03-10").param("to", "2026-03-01"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc")));
        mvc.perform(get("/admin/dashboard").param("from", "abc").param("to", "2026-03-01"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Ngày phải có dạng yyyy-MM-dd")));
        mvc.perform(get("/admin/dashboard").param("from", "2024-01-01").param("to", "2025-12-31"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Kỳ thống kê tối đa 366 ngày")));
        mvc.perform(get("/admin/dashboard").param("from", "2099-01-01").param("to", "2099-01-02"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Kỳ thống kê không được ở tương lai")));
        mvc.perform(get("/admin/dashboard").param("nguong", "-3"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Ngưỡng tồn phải là số nguyên từ 0 đến 1000")));
    }
}
