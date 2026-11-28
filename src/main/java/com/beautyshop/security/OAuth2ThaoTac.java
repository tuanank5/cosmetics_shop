package com.beautyshop.security;

import com.beautyshop.service.DanhTinhOAuth_Service;
import com.beautyshop.service.DanhTinhOAuth_Service.BangChung;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

public final class OAuth2ThaoTac {
    private static final String INTENT = OAuth2ThaoTac.class.getName() + ".intent";
    private static final String PROOF = OAuth2ThaoTac.class.getName() + ".proof";
    private static final String NONCE = OAuth2ThaoTac.class.getName() + ".nonce";

    public enum MucDich { LIEN_KET, XAC_THUC_LAI }

    private record YDinh(long id, long version, String provider, MucDich purpose, String nonce,
                        Instant expires) implements Serializable {}

    public record BangChungPhien(long id, long version, MucDich purpose, BangChung proof, Instant expires)
        implements Serializable {
        public boolean hopLe(PhienNguoiDung user) {
            return id == user.getMaNguoiDung() && version == user.getPhienBanXacThuc()
                && Instant.now().isBefore(expires);
        }
    }

    private OAuth2ThaoTac() {}

    public static String batDau(HttpSession session, PhienNguoiDung user, String provider, MucDich purpose) {
        String nonce = UUID.randomUUID().toString();
        synchronized (WebUtils.getSessionMutex(session)) {
            session.removeAttribute(PROOF);
            session.setAttribute(INTENT, new YDinh(user.getMaNguoiDung(), user.getPhienBanXacThuc(),
                provider, purpose, nonce, Instant.now().plusSeconds(300)));
        }
        return nonce;
    }

    public static BangChungPhien bangChung(HttpSession session, PhienNguoiDung user) {
        var proof = (BangChungPhien) session.getAttribute(PROOF);
        return proof != null && proof.hopLe(user) ? proof : null;
    }

    public static void xoaBangChung(HttpSession session) { session.removeAttribute(PROOF); }

    static boolean laThaoTac(HttpServletRequest request) {
        return request.getAttribute(NONCE) != null;
    }

    static OAuth2AuthenticationToken ketQua(OAuth2LoginAuthenticationToken token, DanhTinhOAuth_Service identities) {
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        var proof = OAuth2NguoiDung_Service.bangChung(token.getPrincipal());
        String nonce = (String) request.getAttribute(NONCE);
        if (nonce != null) {
            xuLyThaoTac(request, nonce, proof, identities);
        }
        try {
            var principal = PhienNguoiDung.oauth(identities.dangNhap(proof));
            return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), proof.provider());
        } catch (DataIntegrityViolationException collision) {
            // Transaction tạo đã rollback. Chỉ đọc lại theo ID nhà cung cấp.
            try {
                var principal = PhienNguoiDung.oauth(identities.danhTinhDaGan(proof));
                return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), proof.provider());
            } catch (AccessDeniedException denied) {
                throw OAuth2NguoiDung_Service.loi("oauth_link_required");
            }
        } catch (AccessDeniedException denied) {
            throw OAuth2NguoiDung_Service.loi("oauth_link_required");
        }
    }

    private static void xuLyThaoTac(HttpServletRequest request, String nonce, BangChung proof,
                                   DanhTinhOAuth_Service identities) {
        var session = request.getSession(false);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (session == null || auth == null || !(auth.getPrincipal() instanceof PhienNguoiDung principal)
            || !auth.getAuthorities().stream().anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()))) {
            throw OAuth2NguoiDung_Service.loi("oauth_operation_invalid");
        }
        synchronized (WebUtils.getSessionMutex(session)) {
            var intent = (YDinh) session.getAttribute(INTENT);
            if (intent == null || !intent.nonce().equals(nonce) || !intent.provider().equals(proof.provider())
                || intent.id() != principal.getMaNguoiDung() || intent.version() != principal.getPhienBanXacThuc()
                || !Instant.now().isBefore(intent.expires())) {
                throw OAuth2NguoiDung_Service.loi("oauth_operation_invalid");
            }
            session.removeAttribute(INTENT);
            if (intent.purpose() == MucDich.XAC_THUC_LAI) {
                try {
                    var owner = identities.danhTinhDaGan(proof);
                    if (owner.getMaNguoiDung() != intent.id() || owner.getPhienBanXacThuc() != intent.version()) {
                        throw OAuth2NguoiDung_Service.loi("oauth_operation_invalid");
                    }
                } catch (AccessDeniedException denied) {
                    throw OAuth2NguoiDung_Service.loi("oauth_operation_invalid");
                }
            }
            session.setAttribute(PROOF, new BangChungPhien(intent.id(), intent.version(), intent.purpose(),
                proof, Instant.now().plusSeconds(300)));
        }
        // Kết thúc trước khi Spring thay SecurityContext, chạy success handler hoặc lưu token.
        throw OAuth2NguoiDung_Service.loi("beautyshop_operation_verified");
    }

    static final class YeuCauDangNhap implements OAuth2AuthorizationRequestResolver {
        private final DefaultOAuth2AuthorizationRequestResolver delegate;

        YeuCauDangNhap(ClientRegistrationRepository clients) {
            delegate = new DefaultOAuth2AuthorizationRequestResolver(clients,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
            var authorization = delegate.resolve(request);
            if (authorization != null && request.getParameter("operation") == null
                && "google".equals(authorization.getAttribute("registration_id"))) {
                return OAuth2AuthorizationRequest.from(authorization)
                    .additionalParameters(params -> params.put("prompt", "select_account")).build();
            }
            return authorization;
        }

        @Override
        public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
            // Spring có thể yêu cầu client theo ID ngoài luồng người dùng bấm nút đăng nhập.
            return delegate.resolve(request, registrationId);
        }
    }

    static final class YeuCau implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
        private final HttpSessionOAuth2AuthorizationRequestRepository delegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();

        @Override
        public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
            return delegate.loadAuthorizationRequest(request);
        }

        @Override
        public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorization, HttpServletRequest request,
                                             HttpServletResponse response) {
            var session = request.getSession(authorization != null);
            if (session == null) {
                return;
            }
            synchronized (WebUtils.getSessionMutex(session)) {
                String nonce = request.getParameter("operation");
                if (authorization != null && nonce != null) {
                    var intent = (YDinh) session.getAttribute(INTENT);
                    if (intent == null || !intent.nonce().equals(nonce) || !Instant.now().isBefore(intent.expires())
                        || !intent.provider().equals(authorization.getAttribute("registration_id"))) {
                        throw OAuth2NguoiDung_Service.loi("oauth_operation_invalid");
                    }
                    authorization = OAuth2AuthorizationRequest.from(authorization)
                        .attributes(attributes -> attributes.put(NONCE, nonce)).build();
                }
                delegate.saveAuthorizationRequest(authorization, request, response);
            }
        }

        @Override
        public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                    HttpServletResponse response) {
            var session = request.getSession(false);
            if (session == null) {
                return null;
            }
            synchronized (WebUtils.getSessionMutex(session)) {
                var authorization = delegate.removeAuthorizationRequest(request, response);
                if (authorization != null) {
                    request.setAttribute(NONCE, authorization.getAttribute(NONCE));
                }
                return authorization;
            }
        }
    }

    static final class KhongLuuToken implements OAuth2AuthorizedClientRepository {
        @Override
        public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id, Authentication principal,
                                                                         HttpServletRequest request) {
            return null;
        }

        @Override
        public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication principal,
                                         HttpServletRequest request, HttpServletResponse response) {}

        @Override
        public void removeAuthorizedClient(String id, Authentication principal, HttpServletRequest request,
                                           HttpServletResponse response) {}
    }
}
