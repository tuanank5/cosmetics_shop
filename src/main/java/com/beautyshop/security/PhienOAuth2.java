package com.beautyshop.security;

import com.beautyshop.entity.NguoiDung;
import java.util.Map;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

sealed class PhienOAuth2 extends PhienNguoiDung implements OAuth2User permits PhienOAuth2.Google {
    private final Map<String, Object> attributes;

    PhienOAuth2(NguoiDung nguoiDung, Map<String, Object> attributes) {
        super(nguoiDung, "");
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return Long.toString(getMaNguoiDung());
    }

    static final class Google extends PhienOAuth2 implements OidcUser {
        private final OidcIdToken idToken;
        private final OidcUserInfo userInfo;

        Google(NguoiDung nguoiDung, OidcUser principal) {
            super(nguoiDung, principal.getClaims());
            this.idToken = principal.getIdToken();
            this.userInfo = principal.getUserInfo();
        }

        @Override
        public Map<String, Object> getClaims() {
            return getAttributes();
        }

        @Override
        public OidcUserInfo getUserInfo() {
            return userInfo;
        }

        @Override
        public OidcIdToken getIdToken() {
            return idToken;
        }
    }
}
