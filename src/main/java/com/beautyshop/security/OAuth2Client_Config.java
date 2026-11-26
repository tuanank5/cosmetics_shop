package com.beautyshop.security;

import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
class OAuth2Client_Config {
    @Bean
    @Conditional(CoNhaCungCap.class)
    ClientRegistrationRepository beautyShopOAuthClients(Environment env) {
        List<ClientRegistration> clients = List.of(CommonOAuth2Provider.GOOGLE, CommonOAuth2Provider.GITHUB)
            .stream().filter(provider -> daCauHinh(env, provider))
            .map(provider -> taoClient(env, provider))
            .toList();
        return new InMemoryClientRegistrationRepository(clients);
    }

    private static ClientRegistration taoClient(Environment env, CommonOAuth2Provider provider) {
        String id = provider == CommonOAuth2Provider.GOOGLE ? "google" : "github";
        String[] scopes = provider == CommonOAuth2Provider.GOOGLE
            ? new String[]{"openid", "profile", "email"} : new String[]{"read:user", "user:email"};
        var builder = provider.getBuilder(id);
        if (provider == CommonOAuth2Provider.GOOGLE) {
            builder.issuerUri("https://accounts.google.com");
        }
        return builder
            .clientId(Objects.requireNonNull(env.getProperty(provider.name() + "_CLIENT_ID")).strip())
            .clientSecret(env.getProperty(provider.name() + "_CLIENT_SECRET"))
            .scope(scopes)
            .build();
    }

    private static boolean daCauHinh(Environment env, CommonOAuth2Provider provider) {
        return giaTriHopLe(env.getProperty(provider.name() + "_CLIENT_ID"))
            && giaTriHopLe(env.getProperty(provider.name() + "_CLIENT_SECRET"));
    }

    private static boolean giaTriHopLe(String value) {
        return StringUtils.hasText(value) && !"local-placeholder".equalsIgnoreCase(value.strip());
    }

    static class CoNhaCungCap implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return daCauHinh(context.getEnvironment(), CommonOAuth2Provider.GOOGLE)
                || daCauHinh(context.getEnvironment(), CommonOAuth2Provider.GITHUB);
        }
    }
}
