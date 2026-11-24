package com.beautyshop.security;

import com.beautyshop.repository.NguoiDung_Repository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

final class TrangThaiTaiKhoan_Filter extends OncePerRequestFilter {
    private final NguoiDung_Repository repository;
    private final SecurityContextLogoutHandler logout = new SecurityContextLogoutHandler();

    TrangThaiTaiKhoan_Filter(NguoiDung_Repository repository) {
        this.repository = repository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/img/")
            || path.startsWith("/uploads/") || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PhienNguoiDung phien) {
            boolean hopLe = repository.findTrangThai(phien.getMaNguoiDung())
                .filter(trangThai -> trangThai.isKichHoat()
                    && trangThai.getPhienBanXacThuc() == phien.getPhienBanXacThuc()
                    && authentication.getAuthorities().stream().anyMatch(
                        role -> role.getAuthority().equals("ROLE_" + trangThai.getVaiTro().name())))
                .isPresent();
            if (!hopLe) {
                logout.logout(request, response, authentication);
                response.sendRedirect(request.getContextPath() + "/login?inactive");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
