package com.beautyshop.controller.admin;

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
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRenderFormButNeedsCsrfToSubmit() throws Exception {
        mvc.perform(get("/admin/catalog/products/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/catalog/form"));
        mvc.perform(post("/admin/catalog/products/new")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/catalog/products/new").with(csrf()).param("slug", "Invalid Slug"))
            .andExpect(model().attributeHasFieldErrors("form", "name", "slug"));
    }
}
