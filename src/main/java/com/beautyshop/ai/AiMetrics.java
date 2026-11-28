package com.beautyshop.ai;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.stereotype.Component;

@Component
public class AiMetrics implements ObservationHandler<ClientRequestObservationContext> {
    public enum Operation {
        REQUEST("Yêu cầu người dùng"), QUERY("Embedding truy vấn"), INDEX("Embedding lập chỉ mục"), CHAT("Chat provider");
        private final String label;
        Operation(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
    public enum Result {
        SUCCESS("Thành công"), REJECTED("Từ chối"), ERROR("Lỗi"), TIMEOUT("Quá hạn"), RATE_LIMIT("HTTP 429");
        private final String label;
        Result(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
    public enum Refusal { VALIDATION, CONTENT, ACCESS }
    public enum WorkerFailure { BUSY, TIMEOUT, ERROR }
    public record Row(Operation operation, long total, long success, long rejected, long errors,
                      long timeouts, long rateLimited, long averageMs) {}
    public record Snapshot(List<Row> rows, Instant lastAttempt, Result lastResult, Instant lastResponse,
                           long busy, long deadline, long workerErrors, long invalid, long sensitive, long denied) {}
    private static final class Counts {
        final long[] results = new long[Result.values().length];
        long elapsed;
        long completed;
    }

    private final EnumMap<Operation, Counts> counts = new EnumMap<>(Operation.class);
    private final ThreadLocal<Boolean> indexing = new ThreadLocal<>();
    private final long[] workerFailures = new long[WorkerFailure.values().length];
    private final long[] refusals = new long[Refusal.values().length];
    private Instant lastAttempt;
    private Instant lastResponse;
    private Result lastResult;

    public AiMetrics() {
        for (var operation : Operation.values()) counts.put(operation, new Counts());
    }

    public <T> T duringIndex(Supplier<T> work) {
        indexing.set(true);
        try { return work.get(); }
        finally { indexing.remove(); }
    }

    public synchronized void workerFailed(WorkerFailure result) { workerFailures[result.ordinal()]++; }

    public synchronized void requestFinished(Result result, Refusal refusal, long nanos) {
        if (result == Result.REJECTED && refusal != null) refusals[refusal.ordinal()]++;
        record(Operation.REQUEST, result, nanos, false);
    }

    private synchronized void record(Operation operation, Result result, long nanos, boolean receivedResponse) {
        var row = counts.get(operation);
        row.completed++;
        row.results[result.ordinal()]++;
        row.elapsed += nanos;
        if (operation != Operation.REQUEST) {
            lastAttempt = Instant.now();
            lastResult = result;
            if (receivedResponse) lastResponse = lastAttempt;
        }
    }

    public synchronized Snapshot snapshot() {
        var rows = Arrays.stream(Operation.values()).map(op -> {
            var c = counts.get(op);
            var r = c.results;
            return new Row(op, c.completed, r[Result.SUCCESS.ordinal()], r[Result.REJECTED.ordinal()],
                r[Result.ERROR.ordinal()], r[Result.TIMEOUT.ordinal()], r[Result.RATE_LIMIT.ordinal()],
                c.completed == 0 ? 0 : c.elapsed / c.completed / 1_000_000);
        }).toList();
        return new Snapshot(rows, lastAttempt, lastResult, lastResponse, workerFailures[WorkerFailure.BUSY.ordinal()],
            workerFailures[WorkerFailure.TIMEOUT.ordinal()], workerFailures[WorkerFailure.ERROR.ordinal()],
            refusals[Refusal.VALIDATION.ordinal()], refusals[Refusal.CONTENT.ordinal()], refusals[Refusal.ACCESS.ordinal()]);
    }

    @Override public boolean supportsContext(Observation.Context context) {
        return context instanceof ClientRequestObservationContext;
    }

    @Override public void onStart(ClientRequestObservationContext context) {
        context.put(AiMetrics.class, System.nanoTime());
    }

    @Override public void onStop(ClientRequestObservationContext context) {
        var request = context.getCarrier();
        if (request == null) return;
        var path = request.getURI().getPath();
        Operation op;
        if (path.endsWith("/api/chat")) op = Operation.CHAT;
        else if (path.endsWith("/api/embed")) op = Boolean.TRUE.equals(indexing.get()) ? Operation.INDEX : Operation.QUERY;
        else return;
        int status = 0;
        try {
            if (context.getResponse() != null) status = context.getResponse().getStatusCode().value();
        } catch (IOException ignored) { /* Transport failed before a status was available. */ }
        Result result = Result.ERROR;
        if (status == 429) result = Result.RATE_LIMIT;
        else if (status == 408 || status == 504 || timedOut(context.getError())) result = Result.TIMEOUT;
        else if (status >= 200 && status < 300 && context.getError() == null) result = Result.SUCCESS;
        long start = context.getRequired(AiMetrics.class);
        record(op, result, System.nanoTime() - start, status != 0);
    }

    private static boolean timedOut(Throwable error) {
        for (var e = error; e != null; e = e.getCause()) {
            if (e instanceof SocketTimeoutException || e instanceof java.net.http.HttpTimeoutException) return true;
        }
        return false;
    }
}
