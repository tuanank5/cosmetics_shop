package com.beautyshop.controller;

import com.beautyshop.service.Momo_Facade;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MomoHttpContractTest {
    @Test
    void ipnKeepsEmptyProtocolBodiesForMalformedOversizedConflictAndThrottledRequests() {
        var facade = mock(Momo_Facade.class);
        when(facade.enabled()).thenReturn(true);
        var controller = new Momo_Controller(facade, new ObjectMapper());
        for (String json : new String[]{"null", "{", "{} {}", "{\"amount\":1,\"amount\":2}"}) {
            var response = controller.ipn(request(json));
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).isNull();
        }
        assertThat(controller.ipn(request(" ".repeat(8193))).getStatusCode().value()).isEqualTo(413);
        doThrow(new DataIntegrityViolationException("private-sql-detail")).when(facade).nhanIPN(any());
        var conflict = controller.ipn(request("{}"));
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        assertThat(conflict.getBody()).isNull();
        // Exercise the current window boundary; retry once if the monotonic second rolled over.
        ReflectionTestUtils.setField(controller, "giay", System.nanoTime() / 1_000_000_000);
        ReflectionTestUtils.setField(controller, "soRequest", 60);
        var response = controller.ipn(request("{}"));
        if (response.getStatusCode().value() != 429) {
            ReflectionTestUtils.setField(controller, "soRequest", 60);
            response = controller.ipn(request("{}"));
        }
        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(response.getBody()).isNull();
    }

    private MockHttpServletRequest request(String body) {
        var request = new MockHttpServletRequest("POST", "/payment/momo/ipn");
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
