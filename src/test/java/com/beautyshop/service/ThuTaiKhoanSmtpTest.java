package com.beautyshop.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.*;

class ThuTaiKhoanSmtpTest {
    @ParameterizedTest
    @ValueSource(strings={"LOCK", "UNLOCK", "450", "550", "UNKNOWN"})
    void relayFixture(String mode) throws Exception {
        try (var server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
             var thread = Executors.newSingleThreadExecutor()) {
            server.setSoTimeout(5000);
            var relay = thread.submit(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                    var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.US_ASCII);
                    var bytes = new ByteArrayOutputStream();
                    out.println("220 fixture");
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("RCPT TO:") && mode.matches("[45]50")) out.println(mode+" fixture");
                        else if (line.equals("DATA")) {
                            out.println("354 fixture");
                            while ((line = in.readLine()) != null && !line.equals(".")) {
                                bytes.write((line+"\r\n").getBytes(StandardCharsets.US_ASCII));
                            }
                            if (mode.equals("UNKNOWN")) break;
                            out.println("250 fixture");
                        } else if (line.equals("QUIT")) { out.println("221 fixture"); break; }
                        else out.println("250 fixture");
                    }
                    return bytes.toByteArray();
                }
            });
            var sender = new JavaMailSenderImpl();
            sender.setHost("127.0.0.1");
            sender.setPort(server.getLocalPort());
            sender.setDefaultEncoding("UTF-8");
            var service = new Email_Service(sender,"fixture@example.invalid");
            boolean mo = mode.equals("UNLOCK");
            var event = java.util.UUID.randomUUID();
            var ex = catchRuntimeException(() -> service.guiTrangThai(event, "customer@example.invalid", mo,
                LocalDateTime.of(2026,11,28,17,5)));
            byte[] data = relay.get(7,TimeUnit.SECONDS);
            if (mode.equals("LOCK") || mo) {
                assertThat(ex).isNull();
                var message = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(data));
                assertThat(message.getAllRecipients()).extracting(Object::toString)
                    .containsExactly("customer@example.invalid");
                assertThat(message.getContentType()).containsIgnoringCase("UTF-8");
                assertThat(message.getMessageID()).isEqualTo("<" + event + "@account.beautyshop.invalid>");
                assertThat(message.getSubject()).isEqualTo("BeautyShop đã " + (mo ? "mở khóa" : "khóa") + " tài khoản của bạn");
                String body = ((String)message.getContent()).replace("\r\n","\n").stripTrailing();
                assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8))))
                    .isEqualTo(mo ? "1a183604c6458f2b1e890d24e6163702aa85db39c0391ce96cfebbcf46ff61c9" : "2c767c4a99baac0dc013506c6d165cf5b8dbe3f9a1534c3a53c488784d794b1a");
            } else {
                assertThat(ex).isNotNull();
                assertThat(GuiThuTaiKhoan_Job.phanLoai(ex)).isEqualTo(switch(mode) {
                    case "450" -> "PENDING";
                    case "550" -> "FAILED";
                    default -> "UNKNOWN";
                });
            }
        }
    }
}
