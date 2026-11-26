package com.beautyshop.security;

import com.beautyshop.service.DanhTinhOAuth_Service.BangChung;
import com.beautyshop.service.NguoiDung_Service;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Service
public class OAuth2NguoiDung_Service implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final DefaultOAuth2UserService defaultService = new DefaultOAuth2UserService();
    private final RestClient restClient;

    public OAuth2NguoiDung_Service() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));
        var userInfo = new RestTemplate(factory);
        userInfo.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        defaultService.setRestOperations(userInfo);
        restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        if (!"github".equals(request.getClientRegistration().getRegistrationId())) {
            throw loi("unsupported_provider");
        }
        var user = defaultService.loadUser(request);
        Object id = user.getAttribute("id");
        if (!(id instanceof Integer || id instanceof Long) || !id.toString().matches("[1-9][0-9]{0,18}")) {
            throw loi("invalid_subject");
        }
        String name = user.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = user.getAttribute("login");
        }
        return new DefaultOAuth2User(List.of(), Map.of("id", id.toString(),
            "beautyshopProof", bangChung("github", id.toString(), layEmailGitHub(request), name)), "id");
    }

    OidcUser loadGoogle(OidcUserRequest request) {
        var token = request.getIdToken();
        if (!"google".equals(request.getClientRegistration().getRegistrationId())
            || token.getIssuer() == null || !"https://accounts.google.com".equals(token.getIssuer().toString())
            || !Boolean.TRUE.equals(token.getEmailVerified())) {
            throw loi("invalid_google_identity");
        }
        return new DefaultOidcUser(List.of(), token);
    }

    static BangChung bangChung(OAuth2User user) {
        if (user instanceof OidcUser google) {
            return bangChung("google", google.getSubject(), google.getEmail(), google.getFullName());
        }
        BangChung proof = user.getAttribute("beautyshopProof");
        if (proof == null) {
            throw loi("invalid_subject");
        }
        return proof;
    }

    private static BangChung bangChung(String provider, String subject, String email, String name) {
        if (subject == null || !subject.matches("[A-Za-z0-9_.:-]{1,255}")
            || email == null || !email.matches("[^@\\s]+@[^@\\s]+") || email.length() > 190) {
            throw loi("invalid_provider_identity");
        }
        email = NguoiDung_Service.chuanHoaEmail(email);
        String display = name == null || name.isBlank() ? email.substring(0, email.indexOf('@')) : name.strip();
        if (display.codePointCount(0, display.length()) > 160) {
            display = display.substring(0, display.offsetByCodePoints(0, 160));
        }
        return new BangChung(provider, subject, email, display);
    }

    private String layEmailGitHub(OAuth2UserRequest request) {
        try {
            for (int page = 1; page <= 10; page++) {
                var emails = restClient.get()
                    .uri("https://api.github.com/user/emails?per_page=100&page={page}", page)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.getAccessToken().getTokenValue())
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve().body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});
                if (emails == null) {
                    break;
                }
                for (var item : emails) {
                    if (Boolean.TRUE.equals(item.get("primary")) && Boolean.TRUE.equals(item.get("verified"))
                        && item.get("email") instanceof String email) {
                        return email;
                    }
                }
                if (emails.size() < 100) {
                    break;
                }
            }
        } catch (org.springframework.web.client.RestClientException failure) {
            throw loi("github_email_unavailable");
        }
        throw loi("github_email_unverified");
    }

    static OAuth2AuthenticationException loi(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
