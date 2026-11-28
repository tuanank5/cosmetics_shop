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
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
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

        if (version == 10) taoHoSoTraCu(url);
        LocalDatabaseMigration.migrate(url, USER, PASSWORD);
        LocalDatabaseMigration.migrate(url, USER, PASSWORD);
        if (version == 10) kiemTraHoSoTraCu(url);

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
        assertThat(info.current().getVersion().toString()).isEqualTo("11");
        assertThat(info.applied()).hasSize(11);
        try (var c = DriverManager.getConnection(url, USER, PASSWORD); var st = c.createStatement();
             var rows = st.executeQuery("SELECT COUNT(*) FROM thu_trang_thai_tai_khoan")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong(1)).isZero();
        }
        if (version == 8) kiemTraRangBuocSlot(url);
    }

    private void taoHoSoTraCu(String url) throws Exception {
        try (var cn = DriverManager.getConnection(url, USER, PASSWORD); var st = cn.createStatement()) {
            st.executeUpdate("INSERT INTO danh_muc(ten_danh_muc,duong_dan) VALUES ('Test','test')");
            st.executeUpdate("INSERT INTO thuong_hieu(ten_thuong_hieu,duong_dan) VALUES ('Test','test')");
            st.executeUpdate("INSERT INTO san_pham(ma_danh_muc,ma_thuong_hieu,ten_san_pham,duong_dan) VALUES(1,1,'Test','test')");
            st.executeUpdate("INSERT INTO bien_the_san_pham(ma_san_pham,sku,ten_bien_the,gia_ban,so_luong_trong_kho) VALUES(1,'A','A',100,8)");
            st.executeUpdate("""
                INSERT INTO don_hang(ma_don_hang_hien_thi,ma_nguoi_dung,ten_nguoi_nhan,sdt_nguoi_nhan,
                 dia_chi_khi_dat,tam_tinh,tong_thanh_toan,trang_thai,phuong_thuc_thanh_toan)
                VALUES('LEGACY',500,'Test','0','Test',500,500,'DELIVERED','COD')
                """);
            st.executeUpdate("""
                INSERT INTO chi_tiet_don_hang(ma_don_hang,ma_bien_the,sku_khi_dat,ten_san_pham_khi_dat,
                  ten_bien_the_khi_dat,don_gia,so_luong) VALUES(1,1,'A','Test','A',100,5)
                """);
            for (var trangThai : com.beautyshop.order.ReturnStatus.values()) {
                st.executeUpdate("INSERT INTO yeu_cau_doi_tra(ma_don_hang,ma_nguoi_dung,trang_thai,ly_do) VALUES(1,500,'"
                    + trangThai.name() + "','Fixture')");
                st.executeUpdate("INSERT INTO chi_tiet_doi_tra(ma_yeu_cau_doi_tra,ma_chi_tiet_don_hang,so_luong) VALUES(LAST_INSERT_ID(),1,1)");
            }
        }
    }

    private void kiemTraHoSoTraCu(String url) throws Exception {
        try (var cn = DriverManager.getConnection(url, USER, PASSWORD); var st = cn.createStatement();
             var rows = st.executeQuery("""
                SELECT COUNT(DISTINCT y.trang_thai),SUM(d.so_luong),
                    (SELECT COUNT(*) FROM nhap_kho_doi_tra),(SELECT so_luong_trong_kho FROM bien_the_san_pham WHERE ma_bien_the=1)
                FROM yeu_cau_doi_tra y JOIN chi_tiet_doi_tra d USING(ma_yeu_cau_doi_tra)
                WHERE y.phien_ban_kiem IS NULL
                """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(5);
            assertThat(rows.getInt(2)).isEqualTo(5);
            assertThat(rows.getInt(3)).isZero();
            assertThat(rows.getInt(4)).isEqualTo(8);
        }
    }

    private void kiemTraRangBuocSlot(String url) throws Exception {
        try (var cn = DriverManager.getConnection(url, USER, PASSWORD); var st = cn.createStatement()) {
            st.executeUpdate("""
                INSERT INTO phieu_giam_gia(ma_phieu_giam_gia,ma_ap_dung,loai_giam_gia,gia_tri_giam,gioi_han_su_dung,bat_dau_luc,ket_thuc_luc)
                VALUES (10,'A','FIXED',1000,5,'2020-01-01','2099-01-01'),(20,'B','FIXED',1000,5,'2020-01-01','2099-01-01')
                """);
            st.executeUpdate("""
                INSERT INTO don_hang(ma_don_hang,ma_don_hang_hien_thi,ma_nguoi_dung,ten_nguoi_nhan,sdt_nguoi_nhan,
                    dia_chi_khi_dat,tam_tinh,tong_thanh_toan,trang_thai,phuong_thuc_thanh_toan)
                VALUES(1,'test',500,'Fixture','0','Fixture',5000,5000,'PENDING_PAYMENT','MOMO')
                """);
            String insert = "INSERT INTO tam_giu_phieu_giam_gia(ma_don_hang,ma_phieu_giam_gia,slot,trang_thai) VALUES ";
            st.executeUpdate(insert + "(1,10,'HANG','HELD')");
            assertThatThrownBy(() -> st.executeUpdate(insert + "(1,20,'HANG','HELD')"))
                .isInstanceOf(java.sql.SQLException.class).hasMessageContaining("uq_don_slot");
            assertThatThrownBy(() -> st.executeUpdate(insert + "(1,10,'VAN_CHUYEN','HELD')"))
                .isInstanceOf(java.sql.SQLException.class).hasMessageContaining("uq_don_phieu");
            assertThatThrownBy(() -> st.executeUpdate(insert + "(1,999,'VAN_CHUYEN','HELD')"))
                .isInstanceOf(java.sql.SQLException.class).hasMessageContaining("foreign key");
        }
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
