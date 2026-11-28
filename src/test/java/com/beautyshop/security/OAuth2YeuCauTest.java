package com.beautyshop.security;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2YeuCauTest {
    @Test
    void chiMotLanLayDuocYeuCauDaLuuTrongPhien() throws Exception {
        var repository = new OAuth2ThaoTac.YeuCau();
        var session = new MockHttpSession();
        var request = new MockHttpServletRequest();
        request.setSession(session);
        var authorization = OAuth2AuthorizationRequest.authorizationCode()
            .clientId("fixture").authorizationUri("https://provider.invalid/authorize")
            .state(UUID.randomUUID().toString()).build();
        repository.saveAuthorizationRequest(authorization, request, new MockHttpServletResponse());

        var start = new CyclicBarrier(2);
        Callable<Boolean> consume = () -> {
            var callback = new MockHttpServletRequest();
            callback.setSession(session);
            callback.setParameter("state", authorization.getState());
            start.await(5, TimeUnit.SECONDS);
            return repository.removeAuthorizationRequest(callback, new MockHttpServletResponse()) != null;
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var results = pool.invokeAll(List.of(consume, consume), 10, TimeUnit.SECONDS);
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(true, false);
        }
    }
}
