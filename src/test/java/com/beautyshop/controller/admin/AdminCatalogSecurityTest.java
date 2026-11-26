package com.beautyshop.controller.admin;

import com.beautyshop.domain.catalog.NhomDanhMucSanPhamThuongHieu;
import com.beautyshop.service.DanhMucSanPhamThuongHieu_Service;

import com.beautyshop.security.LoginSuccess_Handler;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.security.Security_Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDanhMucSanPhamThuongHieu_Controller.class)
@Import(Security_Config.class)
class AdminCatalogSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean LoginSuccess_Handler loginSuccess;
    @MockitoBean NguoiDung_Repository nguoiDungRepository;
    @MockitoBean DanhMucSanPhamThuongHieu_Service catalog;

    @Test
    void guestMustLogIn() throws Exception {
        mvc.perform(get("/admin/catalog/products"))
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotReadOrWriteCatalogAdmin() throws Exception {
        mvc.perform(get("/admin/catalog/products")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/products/new").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/categories/1/status").with(csrf()).param("active", "false"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/brands/1/status").with(csrf()).param("active", "true"))
            .andExpect(status().isForbidden());
        verifyNoMoreInteractions(catalog);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRenderFormButNeedsCsrfToSubmit() throws Exception {
        mvc.perform(get("/admin/catalog/products/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/catalog/form"))
            // P19: the light Admin header uses the light lockup inside a home link.
            .andExpect(content().string(org.hamcrest.Matchers.containsString("src=\"/img/brand/beautyshop-lockup-light.svg\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-label=\"BeautyShop – Trang chủ\"")));
        mvc.perform(post("/admin/catalog/products/new")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/products/new").with(csrf()).param("slug", "Invalid Slug"))
            .andExpect(model().attributeHasFieldErrors("form", "name", "slug"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void statusPostCanCsrfVaChiNhanNhomVaGiaTriHopLe() throws Exception {
        mvc.perform(post("/admin/catalog/categories/1/status").param("active", "false")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/categories/1/status").with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post("/admin/catalog/brands/1/status").with(csrf()).param("active", "toggle"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/admin/catalog/other/1/status").with(csrf()).param("active", "true"))
            .andExpect(status().isNotFound());

        mvc.perform(post("/admin/catalog/categories/1/status").with(csrf()).param("active", "false"))
            .andExpect(redirectedUrl("/admin/catalog/categories"));
        mvc.perform(post("/admin/catalog/brands/1/status").with(csrf()).param("active", "true"))
            .andExpect(redirectedUrl("/admin/catalog/brands"));
        verify(catalog).datTrangThai(NhomDanhMucSanPhamThuongHieu.DANH_MUC, 1, false);
        verify(catalog).datTrangThai(NhomDanhMucSanPhamThuongHieu.THUONG_HIEU, 1, true);
        verifyNoMoreInteractions(catalog);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void routesCuChiConSoftStopSanPham() throws Exception {
        mvc.perform(post("/admin/catalog/categories/1/delete").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/catalog/brands/1/delete").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/catalog/products/1/delete")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/products/1/delete").with(csrf()).param("active", "true"))
            .andExpect(redirectedUrl("/admin/catalog/products"));
        verify(catalog).datTrangThai(NhomDanhMucSanPhamThuongHieu.SAN_PHAM, 1, false);
        verifyNoMoreInteractions(catalog);
    }
}
