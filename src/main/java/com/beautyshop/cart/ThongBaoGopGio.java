package com.beautyshop.cart;

import com.beautyshop.security.PhienNguoiDung;
import com.beautyshop.service.GioHang_Service.KetQuaGop;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.net.URI;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.WebUtils;

/** Dữ liệu một lần trong session. Mã trên URL chỉ phân biệt các lượt điều hướng của cùng trình duyệt. */
public record ThongBaoGopGio(long maNguoiDung, long phienBan, String path, String maDieuHuong,
                             KetQuaGop ketQua) implements Serializable {
    public static final String THAM_SO = "cartMerge";
    private static final String SESSION_KEY = ThongBaoGopGio.class.getName();

    public static void xoaNeuKhacChu(HttpSession session, PhienNguoiDung phien) {
        if (session.getAttribute(SESSION_KEY) instanceof ThongBaoGopGio notice && !notice.cungChu(phien)) {
            session.removeAttribute(SESSION_KEY);
        }
    }

    /** Gọi sau commit và dưới mutex của phiên đăng nhập. */
    public static String ganDich(HttpServletRequest request, PhienNguoiDung phien, KetQuaGop ketQua, String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        String path = uri.getRawPath();
        if (path == null || !path.startsWith("/") || path.startsWith("//")) return null;
        if (uri.isAbsolute()) {
            int port = uri.getPort() < 0 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort();
            if (uri.getUserInfo() != null || !request.getScheme().equalsIgnoreCase(uri.getScheme())
                || !request.getServerName().equalsIgnoreCase(uri.getHost()) || request.getServerPort() != port
                || !path.startsWith(request.getContextPath() + "/")) return null;
        } else {
            if (uri.getRawAuthority() != null) return null;
            path = request.getContextPath() + path;
        }
        String ma = UUID.randomUUID().toString();
        String dich;
        try {
            dich = UriComponentsBuilder.fromUriString(url).replaceQueryParam(THAM_SO, ma).build(true).toUriString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
        request.getSession().setAttribute(SESSION_KEY,
            new ThongBaoGopGio(phien.getMaNguoiDung(), phien.getPhienBanXacThuc(), path, ma, ketQua));
        return dich;
    }

    public static KetQuaGop nhan(HttpServletRequest request, PhienNguoiDung phien) {
        String[] ma = request.getParameterValues(THAM_SO);
        HttpSession session = request.getSession(false);
        if (ma == null || ma.length != 1 || session == null) return null;
        try {
            synchronized (WebUtils.getSessionMutex(session)) {
                if (!(session.getAttribute(SESSION_KEY) instanceof ThongBaoGopGio notice)
                    || !notice.path.equals(request.getRequestURI()) || !notice.maDieuHuong.equals(ma[0])
                    || !notice.cungChu(phien)) return null;
                Object context = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                if (!(context instanceof SecurityContext security) || security.getAuthentication() == null
                    || !(security.getAuthentication().getPrincipal() instanceof PhienNguoiDung chuPhien)
                    || !notice.cungChu(chuPhien)) return null;
                session.removeAttribute(SESSION_KEY);
                return notice.ketQua;
            }
        } catch (IllegalStateException ignored) {
            // Đăng xuất ở tab khác có thể hủy phiên trong lúc GET đang dựng trang.
            return null;
        }
    }

    /** Chuyển tiếp notice qua GET trung gian tới đích nội bộ đã được caller dựng. */
    public static String chuyenDich(HttpServletRequest request, PhienNguoiDung phien, String dich) {
        String[] ma = request.getParameterValues(THAM_SO);
        HttpSession session = request.getSession(false);
        if (ma == null || ma.length != 1 || session == null) return dich;
        synchronized (WebUtils.getSessionMutex(session)) {
            if (!(session.getAttribute(SESSION_KEY) instanceof ThongBaoGopGio notice)
                || !notice.cungChu(phien) || !notice.path.equals(request.getRequestURI())
                || !notice.maDieuHuong.equals(ma[0])) return dich;
            String path = request.getContextPath() + URI.create(dich).getRawPath();
            session.setAttribute(SESSION_KEY, new ThongBaoGopGio(notice.maNguoiDung, notice.phienBan,
                path, notice.maDieuHuong, notice.ketQua));
            return UriComponentsBuilder.fromUriString(dich).replaceQueryParam(THAM_SO, notice.maDieuHuong)
                .build(true).toUriString();
        }
    }

    private boolean cungChu(PhienNguoiDung phien) {
        return maNguoiDung == phien.getMaNguoiDung() && phienBan == phien.getPhienBanXacThuc();
    }
}
