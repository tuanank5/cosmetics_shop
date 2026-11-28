package com.beautyshop.integration;

import org.junit.jupiter.api.BeforeEach;

/** Adds two synthetic logins to the existing SEC-01 dispatch fixture. */
class UiUatErrors_Test extends HttpLoi_MariaDb_Test {
    @BeforeEach
    void prepareBrowserRoles() {
        var hash = encoder.encode(System.getenv("CART01_FIXTURE_PASS"));
        jdbc.sql("""
            INSERT INTO nguoi_dung(email, mat_khau_bam, ho_ten, vai_tro, kich_hoat)
            VALUES ('buyer@example.invalid', ?, 'Fixture Customer', 'CUSTOMER', TRUE),
                   ('admin@example.invalid', ?, 'Fixture Admin', 'ADMIN', TRUE)
            """).params(hash, hash).update();
    }
}
