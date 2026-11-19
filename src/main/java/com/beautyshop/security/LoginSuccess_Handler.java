package com.beautyshop.security;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.service.GioHang_Service;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class LoginSuccess_Handler extends SavedRequestAwareAuthenticationSuccessHandler {
    public static final String CART_NOTICE = LoginSuccess_Handler.class.getName() + ".notice";
    private static final Logger logger = LoggerFactory.getLogger(LoginSuccess_Handler.class);
    private final GioHang_Service gioHangService;

    LoginSuccess_Handler(GioHang_Service gioHangService) {
        this.gioHangService = gioHangService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof PhienNguoiDung nguoiDung
            && authentication.getAuthorities().stream().anyMatch(role -> "ROLE_CUSTOMER".equals(role.getAuthority()))) {
            var session = request.getSession();
            synchronized (WebUtils.getSessionMutex(session)) {
                try {
                    if (gioHangService.gopSauDangNhap(nguoiDung.getMaNguoiDung(), GioKhach.doc(session))) {
                        session.setAttribute(CART_NOTICE, Boolean.TRUE);
                    }
                    GioKhach.xoa(session);
                } catch (DataAccessException exception) {
                    logger.warn("Không thể hợp nhất giỏ khách trong lần đăng nhập này.");
                    var logout = new SecurityContextLogoutHandler();
                    logout.setInvalidateHttpSession(false);
                    logout.logout(request, response, authentication);
                    getRedirectStrategy().sendRedirect(request, response, "/login?cartError");
                    return;
                }
            }
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
