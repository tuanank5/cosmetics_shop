package com.beautyshop.service;

import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.repository.ThongKe_Repository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** P20: lớp bảo vệ thứ hai — service chỉ dành cho Admin từ chối Customer/khách ngay cả khi bị gọi ngoài controller. */
@SpringJUnitConfig(AdminServiceMethodSecurityTest.Config.class)
class AdminServiceMethodSecurityTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean NguoiDung_Repository nguoiDungRepository() {
            var repository = mock(NguoiDung_Repository.class);
            when(repository.timNguoiDung(any(), any(), any(), any())).thenReturn(Page.empty());
            when(repository.findById(anyLong())).thenReturn(Optional.empty());
            return repository;
        }
        @Bean ThongKe_Repository thongKeRepository() { return mock(ThongKe_Repository.class); }
        @Bean ThongKe_Service thongKeService(ThongKe_Repository repository) { return new ThongKe_Service(repository, org.mockito.Mockito.mock(com.beautyshop.repository.DanhGia_Store.class)); }
        @Bean NguoiDung_Service nguoiDungService(NguoiDung_Repository repository) {
            return new NguoiDung_Service(repository, NoOpPasswordEncoder.getInstance());
        }
    }

    @Autowired ThongKe_Service thongKeService;
    @Autowired NguoiDung_Service nguoiDungService;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerBiTuChoiKhiGoiServiceAdmin() {
        assertThatThrownBy(() -> thongKeService.homNay()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> nguoiDungService.timNguoiDung("", false, null, PageRequest.of(0, 10))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> nguoiDungService.datTrangThai(1, 0, 2, false)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void khachChuaDangNhapBiTuChoi() {
        assertThatThrownBy(() -> thongKeService.homNay()).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void methodCustomerDungChungKhongBiChan() {
        // hoSo() không gắn @PreAuthorize: Customer vẫn gọi được (repository giả trả rỗng nên service báo 404, không phải 403).
        assertThatThrownBy(() -> nguoiDungService.hoSo(7)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDuocGoi() {
        assertThatCode(() -> thongKeService.homNay()).doesNotThrowAnyException();
        assertThatCode(() -> nguoiDungService.timNguoiDung("", false, null, PageRequest.of(0, 10))).doesNotThrowAnyException();
    }
}
