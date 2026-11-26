package com.beautyshop.integration;

import com.beautyshop.service.DonHang_Service;
import com.beautyshop.service.GioHang_Service;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/** Real Tomcat dispatch, isolated SQL fixtures; never logs cookies, CSRF or credentials. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.payment.momo.enabled=true", "app.payment.momo.mode=mock", "server.address=127.0.0.1"})
@Import(HttpLoi_MariaDb_Test.TraceConfig.class)
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class HttpLoi_MariaDb_Test {
    @LocalServerPort int port;
    @Autowired JdbcClient jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired GioHang_Service gioHang;
    @Autowired DonHang_Service donHang;
    static final List<String> traces = new CopyOnWriteArrayList<>();
    final String password = UUID.randomUUID().toString();

    @TestConfiguration
    static class TraceConfig {
        @Bean FilterRegistrationBean<Filter> sec01Trace() {
            FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>((req, res, chain) -> {
                var request = (HttpServletRequest) req;
                var response = (HttpServletResponse) res;
                Object id = request.getAttribute("sec01.request");
                if (id == null) {
                    id = UUID.randomUUID().toString();
                    request.setAttribute("sec01.request", id);
                }
                response.setHeader("X-SEC01-Trace", id.toString());
                try {
                    if (request.getDispatcherType() == DispatcherType.REQUEST
                            && request.getRequestURI().startsWith("/products/sec01-status-")) {
                        response.sendError(Integer.parseInt(request.getRequestURI().substring("/products/sec01-status-".length())));
                    } else if (request.getDispatcherType() == DispatcherType.REQUEST
                            && request.getRequestURI().equals("/products/sec01-filter-error")) {
                        throw new IllegalStateException("SEC01-sensitive-fixture-detail");
                    } else chain.doFilter(req, res);
                }
                finally {
                    traces.add(id + " " + request.getDispatcherType() + " " + request.getMethod()
                        + " " + request.getRequestURI().replaceAll("/[0-9]+", "/{id}") + " " + response.getStatus());
                }
            });
            bean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
            bean.setOrder(Integer.MIN_VALUE);
            return bean;
        }
    }

    @Test
    void httpThucGiuStatusSoHuuRoleCsrfVaErrorDispatch() throws Exception {
        traces.clear();
        var chu = taiKhoan("CUSTOMER");
        var khac = taiKhoan("CUSTOMER");
        var admin = taiKhoan("ADMIN");
        long diaChi = jdbc.sql("""
            INSERT INTO dia_chi_nguoi_dung (ma_nguoi_dung, ten_nguoi_nhan, so_dien_thoai, dia_chi_chi_tiet, la_mac_dinh)
            VALUES (?, 'Fixture SEC', '0000000000', 'Fixture SEC', TRUE) RETURNING ma_dia_chi
            """).param(chu.id()).query(Long.class).single();
        String slug = UUID.randomUUID().toString();
        long dm = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('SEC', ?) RETURNING ma_danh_muc").param(slug).query(Long.class).single();
        long th = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('SEC', ?) RETURNING ma_thuong_hieu").param(slug).query(Long.class).single();
        long sp = jdbc.sql("INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan) VALUES (?, ?, 'SEC', ?) RETURNING ma_san_pham")
            .params(dm, th, slug).query(Long.class).single();
        long sku = jdbc.sql("INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho) VALUES (?, ?, 'SEC', 100000, 2) RETURNING ma_bien_the")
            .params(sp, slug).query(Long.class).single();
        gioHang.themGioCustomer(chu.id(), sku);
        long don = donHang.datCOD(chu.id(), diaChi, "");
        long item = jdbc.sql("SELECT ma_chi_tiet_don_hang FROM chi_tiet_don_hang WHERE ma_don_hang = ?").param(don).query(Long.class).single();
        long tra = jdbc.sql("INSERT INTO yeu_cau_doi_tra (ma_don_hang, ma_nguoi_dung, trang_thai, ly_do) VALUES (?, ?, 'REQUESTED', 'Fixture SEC') RETURNING ma_yeu_cau_doi_tra")
            .params(don, chu.id()).query(Long.class).single();
        var a = login(chu);
        var b = login(khac);
        var ad = login(admin);
        assertThat(get(a, "/orders/" + don).statusCode()).isEqualTo(200);
        var saiChu = get(b, "/orders/" + don);
        var vang = get(b, "/orders/" + Long.MAX_VALUE);
        assert404(saiChu);
        assert404(vang);
        assertThat(saiChu.body().split("<main", 2)[1].split("</main>", 2)[0])
            .isEqualTo(vang.body().split("<main", 2)[1].split("</main>", 2)[0]);
        for (String route : List.of("/orders/" + don, "/orders/" + Long.MAX_VALUE)) {
            assertThat(get(ad, route).statusCode()).isEqualTo(403);
            assertThat(get(client(), route).statusCode()).isEqualTo(302);
            assertThat(post(b, route + "/cancel", "").statusCode()).isEqualTo(403);
            assert404(post(b, route + "/cancel", "_csrf=" + token(b)));
        }
        for (String route : List.of("/account/addresses/%d/edit", "/orders/items/%d/review",
                "/orders/%d/returns/new", "/account/returns/%d", "/payment/momo/mock/%d")) {
            long id = route.contains("addresses") ? diaChi : route.contains("items") ? item : route.startsWith("/account/returns") ? tra : don;
            assert404(get(b, route.formatted(id)));
            assert404(get(b, route.formatted(Long.MAX_VALUE)));
        }
        for (long id : List.of(don, Long.MAX_VALUE)) {
            assert404(post(b, "/orders/" + id + "/momo/retry", "_csrf=" + token(b)));
            assert404(post(b, "/payment/momo/mock/" + id, "_csrf=" + token(b) + "&requestId=fixture&result=0"));
            assert404(post(b, "/orders/" + id + "/returns/new", "_csrf=" + token(b) + "&lyDo=fixture"));
        }
        for (long id : List.of(item, Long.MAX_VALUE)) {
            assert404(post(b, "/orders/items/" + id + "/review", "_csrf=" + token(b) + "&soSao=5&noiDung=fixture"));
        }
        for (long id : List.of(diaChi, Long.MAX_VALUE)) {
            assert404(post(b, "/account/addresses/" + id + "/delete", "_csrf=" + token(b)));
            assert404(post(b, "/account/addresses/" + id + "/edit", "_csrf=" + token(b)
                + "&tenNguoiNhan=Fixture&soDienThoai=0000000000&diaChiChiTiet=Fixture"));
        }
        var role = get(b, "/admin/dashboard");
        assertThat(role.statusCode()).isEqualTo(403);
        assertThat(role.body()).contains("Bạn chưa có quyền truy cập");
        assertThat(get(ad, "/admin/sec01-missing").body()).contains("Về tổng quan quản trị");
        var json = b.send(HttpRequest.newBuilder(uri("/orders/" + Long.MAX_VALUE)).header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(json.statusCode()).isEqualTo(404);
        assertThat(json.body()).contains("\"status\":404").doesNotContain("<html", "trace", "exception");
        for (int code : List.of(400, 403, 404, 409, 413, 429, 500, 503)) {
            var response = get(client(), "/products/sec01-status-" + code);
            assertThat(response.statusCode()).isEqualTo(code);
            assertThat(response.body()).contains("id=\"main\"").doesNotContain("SEC01-sensitive", "Exception", "stackTrace");
        }
        var outsideMvc = get(client(), "/products/sec01-filter-error");
        assertThat(outsideMvc.statusCode()).isEqualTo(500);
        assertThat(outsideMvc.body()).contains("Chưa thể hoàn tất yêu cầu").doesNotContain("SEC01-sensitive-fixture-detail");
        String multipart = "--sec01\r\nContent-Disposition: form-data; name=\"hinhAnh\"; filename=\"fixture.png\"\r\n"
            + "Content-Type: image/png\r\n\r\n" + "x".repeat(2 * 1024 * 1024 + 1) + "\r\n--sec01--\r\n";
        var upload = ad.send(HttpRequest.newBuilder(uri("/admin/catalog/products/" + sp + "/variants/new"))
            .header("Accept", "text/html").header("Content-Type", "multipart/form-data; boundary=sec01")
            .header("X-CSRF-TOKEN", java.net.URLDecoder.decode(token(ad), StandardCharsets.UTF_8))
            .POST(HttpRequest.BodyPublishers.ofString(multipart)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(upload.statusCode()).isEqualTo(413);
        assertThat(upload.body()).contains("Ảnh vượt giới hạn tải lên").doesNotContain("FileSizeLimitExceededException");
        jdbc.sql("UPDATE nguoi_dung SET phien_ban_xac_thuc = phien_ban_xac_thuc + 1 WHERE ma_nguoi_dung = ?").param(khac.id()).update();
        assertThat(get(b, "/orders/" + don).headers().firstValue("location").orElseThrow()).endsWith("/login?inactive");
        assertThat(traces).anyMatch(s -> s.contains("REQUEST GET /orders/{id} 404"))
            .anyMatch(s -> s.contains("ERROR GET /error 404"))
            .anyMatch(s -> s.contains("REQUEST GET /admin/dashboard 403"))
            .anyMatch(s -> s.contains("ERROR GET /error 403"));
        Path qa = Path.of("docs/qa/SEC-01");
        Files.createDirectories(qa);
        Files.write(qa.resolve("http-dispatch-trace.log"), traces);
        if (Boolean.getBoolean("sec01.ui")) {
            Files.writeString(qa.resolve("ui-port.txt"), Integer.toString(port));
            long deadline = System.nanoTime() + Duration.ofMinutes(15).toNanos();
            while (!Files.exists(qa.resolve("ui-finished.signal")) && System.nanoTime() < deadline) Thread.sleep(500);
        }
    }

    void assert404(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Không tìm thấy trang").doesNotContain("java.lang", "SELECT ", "Exception");
    }
    record TaiKhoan(long id, String email) {}
    TaiKhoan taiKhoan(String role) {
        String email = UUID.randomUUID() + "@example.invalid";
        long id = jdbc.sql("INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro, kich_hoat) VALUES (?, ?, 'Fixture SEC', ?, TRUE) RETURNING ma_nguoi_dung")
            .params(email, encoder.encode(password), role).query(Long.class).single();
        return new TaiKhoan(id, email);
    }
    HttpClient client() { return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER).build(); }
    URI uri(String path) { return URI.create("http://127.0.0.1:" + port + path); }
    HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header("Accept", "text/html").GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> post(HttpClient client, String path, String form) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).header("Accept", "text/html").header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
    }
    String token(HttpClient client) throws Exception {
        var matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(get(client, "/login").body());
        assertThat(matcher.find()).isTrue();
        return URLEncoder.encode(matcher.group(1), StandardCharsets.UTF_8);
    }
    HttpClient login(TaiKhoan account) throws Exception {
        var client = client();
        var response = post(client, "/login", "_csrf=" + token(client) + "&username=" + URLEncoder.encode(account.email(), StandardCharsets.UTF_8) + "&password=" + password);
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location").orElseThrow()).doesNotContain("error");
        return client;
    }
}
