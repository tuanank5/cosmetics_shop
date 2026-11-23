package com.beautyshop.security;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.service.GioHang_Service;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class LoginSuccess_Handler extends SavedRequestAwareAuthenticationSuccessHandler {
    public static final String CART_NOTICE = LoginSuccess_Handler.class.getName() + ".notice";
    public static final String ADMIN_DASHBOARD = "/admin/dashboard";
    private static final Logger logger = LoggerFactory.getLogger(LoginSuccess_Handler.class);
    private final GioHang_Service gioHangService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    LoginSuccess_Handler(GioHang_Service gioHangService) {
        this.gioHangService = gioHangService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getAuthorities().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getAuthority()))) {
            // P20: Admin luôn vào Dashboard, trừ khi bị chuyển tới /login từ một URL /admin/** hợp lệ của chính site này.
            var saved = requestCache.getRequest(request, response);
            requestCache.removeRequest(request, response);
            clearAuthenticationAttributes(request);
            String dich = saved == null ? null : duongDanAdminHopLe(saved.getRedirectUrl(), request);
            getRedirectStrategy().sendRedirect(request, response, dich == null ? ADMIN_DASHBOARD : dich);
            return;
        }
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

    /**
     * Trả path + query tương đối nếu URL đã lưu cùng scheme/host/port với request hiện tại và nằm dưới /admin/;
     * ngược lại null. Path có ".." hoặc ký tự mã hóa nguy hiểm bị từ chối.
     */
    static String duongDanAdminHopLe(String url, HttpServletRequest request) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            if (uri.getScheme() != null && !uri.getScheme().equalsIgnoreCase(request.getScheme())) return null;
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) return null;
            if (uri.getHost() != null && cong(uri) != (request.getServerPort() == -1 ? macDinh(request.getScheme()) : request.getServerPort())) return null;
            if (uri.getHost() == null && (uri.getScheme() != null || uri.getRawAuthority() != null)) return null;
            String path = uri.getRawPath();
            String goc = request.getContextPath() + "/admin/";
            if (path == null || !path.startsWith(goc) || path.contains("..") || path.contains("//")
                || path.toLowerCase(java.util.Locale.ROOT).matches(".*%(2e|2f|5c).*") || path.contains("\\")) {
                return null;
            }
            // DefaultRedirectStrategy tự thêm context path cho URL bắt đầu bằng "/".
            String tuongDoi = path.substring(request.getContextPath().length());
            // Bỏ tham số đánh dấu "continue" của HttpSessionRequestCache: request đã lưu được xóa ở trên.
            String query = uri.getRawQuery() == null ? "" : java.util.Arrays.stream(uri.getRawQuery().split("&"))
                .filter(thamSo -> !thamSo.equals("continue") && !thamSo.startsWith("continue="))
                .collect(java.util.stream.Collectors.joining("&"));
            return query.isEmpty() ? tuongDoi : tuongDoi + "?" + query;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static int cong(URI uri) { return uri.getPort() == -1 ? macDinh(uri.getScheme()) : uri.getPort(); }

    private static int macDinh(String scheme) { return "https".equalsIgnoreCase(scheme) ? 443 : 80; }
}
