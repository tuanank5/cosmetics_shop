package com.beautyshop.bootstrap;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "LOCAL_BOOTSTRAP_MIGRATION_FIXTURE", matches = "true")
class LocalDatabaseMigration_MariaDb_Test {
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void nangCapVaGiuNguoiDungTuLichSuChinhThuc(int version) throws Exception {
        String url = fixtureUrl("v" + version);
        Flyway.configure().dataSource(url, USER, PASSWORD).target(Integer.toString(version)).load().migrate();
        try (var connection = DriverManager.getConnection(url, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO nguoi_dung (ma_nguoi_dung, email, mat_khau_bam, ho_ten)
                VALUES (500, 'legacy@example.invalid', 'fixture', 'Fixture Legacy')
                """);
        }

        LocalDatabaseMigration.migrate(url, USER, PASSWORD);
        LocalDatabaseMigration.migrate(url, USER, PASSWORD);

        try (var connection = DriverManager.getConnection(url, USER, PASSWORD);
             var statement = connection.createStatement();
             var users = statement.executeQuery("""
                 SELECT ma_nguoi_dung, mat_khau_bam, ngay_xac_minh_email
                 FROM nguoi_dung ORDER BY ma_nguoi_dung
                 """)) {
            assertThat(users.next()).isTrue();
            assertThat(users.getLong("ma_nguoi_dung")).isEqualTo(500L);
            assertThat(users.getString("mat_khau_bam")).isEqualTo("fixture");
            assertThat(users.getObject("ngay_xac_minh_email")).isNull();
            assertThat(users.next()).isFalse();
        }
        var info = Flyway.configure().dataSource(url, USER, PASSWORD).load().info();
        assertThat(info.current().getVersion().toString()).isEqualTo("7");
        assertThat(info.applied()).hasSize(7);
    }

    @Test
    void tuChoiSchemaCuKhongCoLichSu() throws Exception {
        String url = fixtureUrl("unmanaged");
        try (var connection = DriverManager.getConnection(url, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE legacy_marker (id INT PRIMARY KEY)");
            statement.executeUpdate("INSERT INTO legacy_marker VALUES (1)");
        }
        assertThatThrownBy(() -> LocalDatabaseMigration.migrate(url, USER, PASSWORD))
            .isInstanceOf(FlywayException.class).hasMessageContaining("non-empty");
        try (var connection = DriverManager.getConnection(url, USER, PASSWORD);
             var statement = connection.createStatement();
             var marker = statement.executeQuery("SELECT id FROM legacy_marker")) {
            assertThat(marker.next()).isTrue();
            assertThat(marker.getInt("id")).isEqualTo(1);
        }
    }

    @Test
    void tuChoiChecksumKhongKhopTruocKhiChayBanMoi() throws Exception {
        String url = fixtureUrl("drift");
        Flyway.configure().dataSource(url, USER, PASSWORD).target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE flyway_schema_history SET checksum = 1 WHERE version = '1'");
        }
        assertThatThrownBy(() -> LocalDatabaseMigration.migrate(url, USER, PASSWORD))
            .isInstanceOf(FlywayException.class).hasMessageContaining("checksum");
        try (var connection = DriverManager.getConnection(url, USER, PASSWORD);
             var statement = connection.createStatement();
             var history = statement.executeQuery("SELECT version FROM flyway_schema_history WHERE success")) {
            assertThat(history.next()).isTrue();
            assertThat(history.getString("version")).isEqualTo("1");
            assertThat(history.next()).isFalse();
        }
    }

    private String fixtureUrl(String suffix) {
        String url = System.getenv("DB_URL");
        assertThat(url).matches("jdbc:mariadb://127\\.0\\.0\\.1:\\d+/beautyshop_demo_setup");
        return url + "_" + suffix;
    }
}
