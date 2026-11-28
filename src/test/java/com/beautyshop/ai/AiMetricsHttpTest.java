package com.beautyshop.ai;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;

class AiMetricsHttpTest {
    @Test void readTimeoutKhongTaoPhanHoiGia() throws Exception {
        var done = new CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/embed", exchange -> {
            try (exchange) {
                try { done.await(3, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });
        server.start();
        try {
            var metrics = new AiMetrics();
            var registry = ObservationRegistry.create();
            registry.observationConfig().observationHandler(metrics);
            var factory = new SimpleClientHttpRequestFactory();
            factory.setReadTimeout(Duration.ofMillis(200));
            var client = RestClient.builder().requestFactory(factory).observationRegistry(registry).build();
            assertThatThrownBy(() -> client.post().uri("http://127.0.0.1:"+server.getAddress().getPort()+"/api/embed")
                .retrieve().toBodilessEntity()).isInstanceOf(ResourceAccessException.class);
            var snapshot = metrics.snapshot();
            var query = snapshot.rows().stream().filter(r -> r.operation() == AiMetrics.Operation.QUERY).findFirst().orElseThrow();
            assertThat(query.total()).isOne();
            assertThat(query.timeouts()).isOne();
            assertThat(query.averageMs()).isPositive();
            assertThat(snapshot.lastResponse()).isNull();
            assertThat(snapshot.lastResult()).isEqualTo(AiMetrics.Result.TIMEOUT);
        } finally {
            done.countDown();
            server.stop(0);
        }
    }
}
