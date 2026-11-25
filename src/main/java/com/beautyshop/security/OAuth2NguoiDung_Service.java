package com.beautyshop.security;

import com.beautyshop.entity.NguoiDung;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.service.NguoiDung_Service;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
class OAuth2NguoiDung_Service {
    private final NguoiDung_Repository repository;
    private final PasswordEncoder passwordEncoder;
    private final OidcUserService oidc = new OidcUserService();
    private final DefaultOAuth2UserService oauth2 = new DefaultOAuth2UserService();
    private final RestClient http;

    OAuth2NguoiDung_Service(NguoiDung_Repository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        var requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().requestFactory(requestFactory).build();
    }

    OidcUser dangNhapGoogle(OidcUserRequest request) {
        if (!"google".equals(request.getClientRegistration().getRegistrationId())) {
            throw loi("unsupported_provider");
        }
        OidcUser principal = oidc.loadUser(request);
        if (!Boolean.TRUE.equals(principal.getEmailVerified())) {
            throw loi("unverified_email");
        }
        return new PhienOAuth2.Google(
            taiKhoan("google", principal.getSubject(), principal.getEmail(), principal.getFullName()), principal);
    }

    OAuth2User dangNhapGitHub(OAuth2UserRequest request) {
        if (!"github".equals(request.getClientRegistration().getRegistrationId())) {
            throw loi("unsupported_provider");
        }
        OAuth2User principal = oauth2.loadUser(request);
        String maDanhTinh = principal.getName();
        if (!maDanhTinh.matches("[1-9][0-9]{0,19}")) {
            throw loi("invalid_provider_identity");
        }
        String hoTen = principal.getAttribute("name");
        if (hoTen == null || hoTen.isBlank()) {
            hoTen = principal.getAttribute("login");
        }
        return new PhienOAuth2(taiKhoan("github", maDanhTinh, emailGitHub(request), hoTen),
            Map.of("id", maDanhTinh));
    }

    private NguoiDung taiKhoan(String nhaCungCap, String maDanhTinh, String email, String hoTen) {
        if (maDanhTinh == null || !maDanhTinh.matches("[!-~]{1,255}")) {
            throw loi("invalid_provider_identity");
        }
        try {
            var nguoiDung = repository.findByNhaCungCapOauthAndMaDanhTinhOauth(nhaCungCap, maDanhTinh)
                .orElseGet(() -> taoTaiKhoan(nhaCungCap, maDanhTinh, email, hoTen));
            if (!nguoiDung.isKichHoat() || nguoiDung.getVaiTro() != NguoiDung.VaiTro.CUSTOMER) {
                throw loi("account_unavailable");
            }
            return nguoiDung;
        } catch (DataAccessException exception) {
            throw loi("account_unavailable");
        }
    }

    private NguoiDung taoTaiKhoan(String nhaCungCap, String maDanhTinh, String email, String hoTen) {
        String emailChuan = email == null ? "" : NguoiDung_Service.chuanHoaEmail(email);
        int separator = emailChuan.indexOf('@');
        if (emailChuan.length() > 190 || separator < 1 || separator == emailChuan.length() - 1) {
            throw loi("unverified_email");
        }
        String ten = hoTen == null || hoTen.isBlank() ? emailChuan.substring(0, separator) : hoTen.strip();
        int doDai = Math.min(160, ten.codePointCount(0, ten.length()));
        String matKhauBam = passwordEncoder.encode(UUID.randomUUID().toString());
        var moi = new NguoiDung(emailChuan, matKhauBam, ten.substring(0, ten.offsetByCodePoints(0, doDai)),
            nhaCungCap, maDanhTinh);
        try {
            return repository.saveAndFlush(moi);
        } catch (DataIntegrityViolationException exception) {
            // Lần đăng nhập đồng thời có thể đã tạo tài khoản cho cùng danh tính.
            return repository.findByNhaCungCapOauthAndMaDanhTinhOauth(nhaCungCap, maDanhTinh)
                .orElseThrow(() -> loi("account_unavailable"));
        }
    }

    private String emailGitHub(OAuth2UserRequest request) {
        List<EmailGitHub> emails;
        try {
            var userInfo = request.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri();
            emails = http.get().uri(userInfo + "/emails?per_page=100")
                .header("Accept", "application/vnd.github+json")
                .headers(headers -> headers.setBearerAuth(request.getAccessToken().getTokenValue()))
                .retrieve().body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException exception) {
            throw loi("provider_unavailable");
        }
        if (emails == null) {
            throw loi("unverified_email");
        }
        return emails.stream()
            .filter(email -> email.primary() && email.verified())
            .map(EmailGitHub::email)
            .filter(email -> email != null && !email.isBlank())
            .findFirst().orElseThrow(() -> loi("unverified_email"));
    }

    private static OAuth2AuthenticationException loi(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }

    private record EmailGitHub(String email, boolean primary, boolean verified) {}
}
