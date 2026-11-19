package com.beautyshop.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

final class SandboxMomoGateway implements MomoGateway {
    private static final String HOST = "test-payment.momo.vn";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper;
    private final String partnerCode;
    private final String accessKey;
    private final String secretKey;
    private final URI redirect;
    private final URI ipn;

    SandboxMomoGateway(ObjectMapper mapper, String partnerCode, String accessKey, String secretKey, URI redirect, URI ipn) {
        this.mapper = mapper;
        this.partnerCode = partnerCode;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.redirect = redirect;
        this.ipn = ipn;
    }

    @Override public String maGateway() { return MA_SANDBOX; }

    @Override
    public Optional<URI> tao(Request request) {
        String raw = "accessKey=" + accessKey + "&amount=" + request.amount() + "&extraData="
            + "&ipnUrl=" + ipn + "&orderId=" + request.orderId() + "&orderInfo=" + request.orderInfo()
            + "&partnerCode=" + partnerCode + "&redirectUrl=" + redirect + "&requestId=" + request.requestId()
            + "&requestType=captureWallet";
        return call("create", Map.ofEntries(Map.entry("partnerCode", partnerCode), Map.entry("amount", request.amount()),
                Map.entry("extraData", ""), Map.entry("ipnUrl", ipn.toString()), Map.entry("redirectUrl", redirect.toString()),
                Map.entry("orderId", request.orderId()), Map.entry("orderInfo", request.orderInfo()),
                Map.entry("requestId", request.requestId()), Map.entry("requestType", "captureWallet"),
                Map.entry("autoCapture", true), Map.entry("lang", "vi"), Map.entry("signature", MomoSignature.sign(secretKey, raw))))
            .filter(body -> khop(body, request, request.requestId()) && body.path("resultCode").asInt(-1) == 0)
            .flatMap(body -> payUrl(body.path("payUrl").asText()));
    }

    @Override
    public Optional<KetQua> doiSoat(Request request) {
        String requestId = java.util.UUID.randomUUID().toString();
        String raw = "accessKey=" + accessKey + "&orderId=" + request.orderId() + "&partnerCode=" + partnerCode
            + "&requestId=" + requestId;
        return call("query", Map.of("partnerCode", partnerCode, "orderId", request.orderId(), "requestId", requestId,
                "lang", "vi", "signature", MomoSignature.sign(secretKey, raw)))
            .filter(body -> khop(body, request, requestId) && body.path("resultCode").isIntegralNumber())
            .map(body -> new KetQua(body.path("resultCode").asInt(), body.path("transId").asLong(0)));
    }

    private boolean khop(JsonNode body, Request request, String requestId) {
        return partnerCode.equals(body.path("partnerCode").asText()) && requestId.equals(body.path("requestId").asText())
            && request.orderId().equals(body.path("orderId").asText())
            && body.path("amount").isIntegralNumber() && body.path("amount").canConvertToLong()
            && body.path("amount").longValue() == request.amount();
    }

    private Optional<URI> payUrl(String raw) {
        try {
            URI uri = URI.create(raw);
            if ("https".equalsIgnoreCase(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost())
                && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)) {
                return Optional.of(uri);
            }
        } catch (IllegalArgumentException ignored) {
            // URL không hợp lệ, giữ đơn chờ đối soát.
        }
        return Optional.empty();
    }

    private Optional<JsonNode> call(String endpoint, Map<String, Object> data) {
        try {
            var request = HttpRequest.newBuilder(URI.create("https://" + HOST + "/v2/gateway/api/" + endpoint))
                .timeout(Duration.ofSeconds(35)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(data))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return response.statusCode() == 200 ? Optional.ofNullable(mapper.readTree(response.body())) : Optional.empty();
        } catch (IOException exception) {
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}
