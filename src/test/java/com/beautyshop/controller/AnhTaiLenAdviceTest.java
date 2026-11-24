package com.beautyshop.controller;

import com.beautyshop.entity.SanPham;
import com.beautyshop.service.BienTheSanPham_Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AnhTaiLenAdviceTest {
    private final BienTheSanPham_Service service = mock(BienTheSanPham_Service.class);

    private AnhTaiLen_Advice advice() {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("bienTheService", service);
        return new AnhTaiLen_Advice(beans.getBeanProvider(BienTheSanPham_Service.class));
    }

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void oversizedRequestCannotReadAdminDataWithoutAdminRole() {
        var request = new MockHttpServletRequest("POST", "/admin/catalog/products/1/variants/new");
        var view = advice().quaDungLuong(request);
        assertThat(view.getViewName()).isEqualTo("error/413");
        assertThat(view.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        verifyNoInteractions(service);
    }

    @Test
    void adminGetsExistingSkuFormWithImageFieldErrorAndNoWrite() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "ui-test", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        when(service.sanPham(1L)).thenReturn(mock(SanPham.class));
        var request = new MockHttpServletRequest("POST", "/admin/catalog/products/1/variants/new");
        var view = advice().quaDungLuong(request);
        assertThat(view.getViewName()).isEqualTo("admin/variant/form");
        assertThat(view.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        var errors = (BindingResult) view.getModel().get(BindingResult.MODEL_KEY_PREFIX + "form");
        assertThat(errors.hasFieldErrors("hinhAnh")).isTrue();
        assertThat(view.getModel()).containsEntry("uploadQuaLon", true)
            .containsEntry("action", request.getRequestURI());
        verify(service).sanPham(1L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void unrelatedUploadOnlyGetsGeneric413() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "ui-test", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        var view = advice().quaDungLuong(new MockHttpServletRequest("POST", "/other-upload"));
        assertThat(view.getViewName()).isEqualTo("error/413");
        verifyNoInteractions(service);
    }
}
