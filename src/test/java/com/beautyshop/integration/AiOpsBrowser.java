package com.beautyshop.integration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

final class AiOpsBrowser {
    private AiOpsBrowser() {}

    static void run(int port, JdbcClient jdbc, String mode) throws Exception {
        String user = UUID.randomUUID() + "@example.invalid";
        String password = UUID.randomUUID().toString();
        jdbc.sql("INSERT INTO nguoi_dung(email,mat_khau_bam,ho_ten,vai_tro) VALUES(?,?,'Fixture','ADMIN')")
            .params(user,new BCryptPasswordEncoder().encode(password)).update();
        var command = new ProcessBuilder("node",System.getenv("AI_OPS_BROWSER"),"http://127.0.0.1:"+port,mode);
        command.environment().put("FIXTURE_USER",user);
        command.environment().put("FIXTURE_PASS",password);
        var process = command.redirectErrorStream(true).start();
        try {
            assertThat(process.waitFor(90,TimeUnit.SECONDS)).isTrue();
            String output = new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
            assertThat(output).contains("[AI-OPS WebKit] PASS "+mode);
            assertThat(process.exitValue()).isZero();
            System.out.println("[AI-OPS WebKit] PASS "+mode);
        } finally {
            process.destroyForcibly();
        }
    }
}
