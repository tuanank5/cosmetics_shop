
package com.beautyshop.security;

import com.beautyshop.entity.NguoiDung;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.service.NguoiDung_Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OAuth2NguoiDung_Service
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final NguoiDung_Repository repository;
    private final PasswordEncoder passwordEncoder;

    private final DefaultOAuth2UserService defaultService =
            new DefaultOAuth2UserService();

    private final RestClient restClient = RestClient.create();

    public OAuth2NguoiDung_Service(
            NguoiDung_Repository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request)
            throws OAuth2AuthenticationException {

        OAuth2User providerUser = defaultService.loadUser(request);

        String provider = request.getClientRegistration().getRegistrationId();

        Map<String, Object> attributes = providerUser.getAttributes();

        String email;
        String hoTen;

        if ("google".equals(provider)) {

            email = (String) attributes.get("email");

            if (!Boolean.TRUE.equals(attributes.get("email_verified"))) {
                throw loiOAuth("Email Google chưa được xác minh.");
            }

            hoTen = (String) attributes.get("name");

        } else if ("github".equals(provider)) {

            email = layEmailGitHub(request);

            hoTen = (String) attributes.get("name");

            if (hoTen == null || hoTen.isBlank()) {
                hoTen = (String) attributes.get("login");
            }

        } else {
            throw loiOAuth("Nhà cung cấp đăng nhập không được hỗ trợ.");
        }

        if (email == null || email.isBlank()) {
            throw loiOAuth("Không lấy được email đã xác minh.");
        }

        email = NguoiDung_Service.chuanHoaEmail(email);

        if (hoTen == null || hoTen.isBlank()) {
            hoTen = email.substring(0, email.indexOf('@'));
        }

        final String emailCuoi = email;
        final String tenCuoi = hoTen.strip();

        NguoiDung nguoiDung = repository.findByEmail(emailCuoi)
                .orElseGet(() -> taoNguoiDung(emailCuoi, tenCuoi));

        if (!nguoiDung.isKichHoat()) {
            throw loiOAuth("Tài khoản BeautyShop đã bị khóa.");
        }

        return new PhienNguoiDung(nguoiDung, attributes);
    }

    private NguoiDung taoNguoiDung(String email, String hoTen) {

        String matKhauNgauNhien = UUID.randomUUID().toString();

        NguoiDung moi = new NguoiDung(
                email,
                passwordEncoder.encode(matKhauNgauNhien),
                hoTen
        );

        try {
            return repository.saveAndFlush(moi);

        } catch (DataIntegrityViolationException exception) {

            // Tránh tạo tài khoản trùng nếu hai yêu cầu đăng nhập
            // cùng tạo một email tại cùng thời điểm.

            return repository.findByEmail(email)
                    .orElseThrow(() -> exception);
        }
    }

    private String layEmailGitHub(OAuth2UserRequest request) {

        String accessToken = request.getAccessToken()
                .getTokenValue();

        List<Map<String, Object>> emails;

        try {

            emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + accessToken
                    )
                    .header(
                            HttpHeaders.ACCEPT,
                            "application/vnd.github+json"
                    )
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

        } catch (Exception exception) {

            throw loiOAuth("Không thể lấy email từ GitHub.");
        }

        if (emails == null) {
            throw loiOAuth("GitHub không trả về danh sách email.");
        }

        return emails.stream()
                .filter(item ->
                        Boolean.TRUE.equals(item.get("primary")))
                .filter(item ->
                        Boolean.TRUE.equals(item.get("verified")))
                .map(item -> (String) item.get("email"))
                .filter(email ->
                        email != null && !email.isBlank())
                .findFirst()
                .orElseThrow(() ->
                        loiOAuth(
                                "GitHub không có email chính đã xác minh."
                        ));
    }

    private OAuth2AuthenticationException loiOAuth(String message) {

        return new OAuth2AuthenticationException(
                new OAuth2Error("oauth2_login_error"),
                message
        );
    }
}
