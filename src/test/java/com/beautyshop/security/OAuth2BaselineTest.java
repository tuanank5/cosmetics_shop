package com.beautyshop.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2BaselineTest {
    private final ApplicationContextRunner runner =
        new ApplicationContextRunner().withUserConfiguration(OAuth2Client_Config.class);

    @Test
    void missingOrPartialCredentialsLeaveOAuthDisabled() {
        runner.run(context -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class));
        runner.withPropertyValues("GOOGLE_CLIENT_ID=testing", "GITHUB_CLIENT_SECRET=testing")
            .run(context -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class));
    }

    @Test
    void onlyRegistersProvidersWithBothCredentials() {
        runner.withPropertyValues("GOOGLE_CLIENT_ID=testing", "GOOGLE_CLIENT_SECRET=testing",
                "GITHUB_CLIENT_ID=testing")
            .run(context -> {
                var clients = context.getBean(ClientRegistrationRepository.class);
                assertThat(clients.findByRegistrationId("google")).isNotNull();
                assertThat(clients.findByRegistrationId("github")).isNull();
            });
        runner.withPropertyValues("GITHUB_CLIENT_ID=testing", "GITHUB_CLIENT_SECRET=testing")
            .run(context -> {
                var clients = context.getBean(ClientRegistrationRepository.class);
                assertThat(clients.findByRegistrationId("github")).isNotNull();
                assertThat(clients.findByRegistrationId("google")).isNull();
            });
    }

    @Test
    void placeholderDoesNotEnableProviderAndBothCanBeConfigured() {
        runner.withPropertyValues("GOOGLE_CLIENT_ID=local-placeholder", "GOOGLE_CLIENT_SECRET=testing")
            .run(context -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class));
        runner.withPropertyValues("GOOGLE_CLIENT_ID=testing", "GOOGLE_CLIENT_SECRET=testing",
                "GITHUB_CLIENT_ID=testing", "GITHUB_CLIENT_SECRET=testing")
            .run(context -> {
                var clients = context.getBean(ClientRegistrationRepository.class);
                assertThat(clients.findByRegistrationId("google").getScopes()).contains("openid");
                assertThat(clients.findByRegistrationId("github").getScopes()).contains("user:email");
            });
    }
}
