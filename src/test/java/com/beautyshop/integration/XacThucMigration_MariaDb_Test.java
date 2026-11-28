package com.beautyshop.integration;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "AUTH_02_MIGRATION_FIXTURE", matches = "true")
class XacThucMigration_MariaDb_Test {
    @ParameterizedTest
    @ValueSource(ints = {5, 6})
    void baoToanIdLichSuVaChiChuyenDanhTinhCoBangChung(int from) throws Exception {
        String url = System.getenv("DB_URL").replaceFirst("/auth_02_test$", "/auth_02_v" + from);
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        Flyway.configure().dataSource(url, user, password).target(Integer.toString(from)).load().migrate();
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO nguoi_dung (ma_nguoi_dung,email,mat_khau_bam,ho_ten)
                VALUES (500,'legacy@example.invalid','fixture','Legacy')
                """);
            if (from == 6) {
                statement.executeUpdate("""
                    UPDATE nguoi_dung SET nha_cung_cap_oauth='github', ma_danh_tinh_oauth='901'
                    WHERE ma_nguoi_dung=500
                    """);
            }
            statement.executeUpdate("""
                INSERT INTO gio_hang (ma_nguoi_dung) VALUES (500)
                """);
        }
        Flyway.configure().dataSource(url,user,password).target("7").load().migrate();
        try (var connection = DriverManager.getConnection(url,user,password); var s = connection.createStatement()) {
            s.executeUpdate("""
                INSERT INTO nhat_ky_quan_tri (vai_tro_nguoi_thuc_hien,hanh_dong,loai_doi_tuong,
                    ma_doi_tuong,thoi_gian,ket_qua,correlation_id)
                VALUES ('SYSTEM','SESSION_REVOKED','ACCOUNT',500,UTC_TIMESTAMP(),'SUCCESS',UUID())
                """);
        }
        Flyway.configure().dataSource(url,user,password).target("8").load().migrate();
        try (var connection = DriverManager.getConnection(url,user,password); var st = connection.createStatement()) {
            st.executeUpdate("""
                INSERT INTO phieu_giam_gia(ma_ap_dung,loai_giam_gia,gia_tri_giam,gioi_han_su_dung,bat_dau_luc,ket_thuc_luc,so_luot_da_dung,so_luot_tam_giu)
                VALUES ('LEGACY','FIXED',10000,5,'2020-01-01','2099-01-01',1,1)
                """);
            for (String method : java.util.List.of("COD","MOMO")) {
                st.executeUpdate("""
                    INSERT INTO don_hang(ma_nguoi_dung,ma_phieu_giam_gia,ma_don_hang_hien_thi,ten_nguoi_nhan,sdt_nguoi_nhan,dia_chi_khi_dat,tam_tinh,so_tien_giam,phi_van_chuyen,tong_thanh_toan,trang_thai,phuong_thuc_thanh_toan)
                    SELECT 500,ma_phieu_giam_gia,'%s','Fixture','0','Fixture',50000,10000,25000,65000,'%s','%s'
                    FROM phieu_giam_gia WHERE ma_ap_dung='LEGACY'
                    """.formatted(method,method.equals("COD") ? "CONFIRMED" : "PENDING_PAYMENT",method));
            }
            st.executeUpdate("""
                INSERT INTO tam_giu_phieu_giam_gia(ma_don_hang,ma_phieu_giam_gia,trang_thai)
                SELECT ma_don_hang,ma_phieu_giam_gia,'HELD' FROM don_hang WHERE ma_don_hang_hien_thi='MOMO'
                """);
        }
        var flyway = Flyway.configure().dataSource(url, user, password).load();
        flyway.migrate();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try (var connection = DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("""
                SELECT COUNT(*),SUM(d.tong_thanh_toan),SUM(g.trang_thai='HELD'),SUM(g.trang_thai='CONSUMED')
                FROM don_hang d JOIN tam_giu_phieu_giam_gia g USING(ma_don_hang)
                WHERE g.slot='HANG' AND g.so_tien_giam=10000 AND d.phi_chuan IS NULL
                """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(2);
                assertThat(result.getInt(2)).isEqualTo(130000);
                assertThat(result.getInt(3)).isEqualTo(1);
                assertThat(result.getInt(4)).isEqualTo(1);
            }
            try (var index = statement.executeQuery("""
                SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index)
                FROM information_schema.statistics WHERE table_schema=DATABASE()
                AND table_name='nhat_ky_quan_tri' AND index_name='ix_nhat_ky_thoi_gian'
                """)) {
                assertThat(index.next()).isTrue();
                assertThat(index.getString(1)).isEqualTo("thoi_gian,ma_nhat_ky");
            }
            try (var fk = statement.executeQuery("""
                SELECT delete_rule FROM information_schema.referential_constraints
                WHERE constraint_schema=DATABASE() AND table_name='bao_luu_nhat_ky'
                AND referenced_table_name='nhat_ky_quan_tri'
                """)) {
                assertThat(fk.next()).isTrue();
                assertThat(fk.getString(1)).isEqualTo("RESTRICT");
            }
            try (var plan = statement.executeQuery("""
                EXPLAIN SELECT ma_nhat_ky FROM nhat_ky_quan_tri
                WHERE thoi_gian >= '2000-01-01' AND thoi_gian < '2099-01-01'
                ORDER BY thoi_gian DESC, ma_nhat_ky DESC LIMIT 20
                """)) {
                assertThat(plan.next()).isTrue();
                assertThat(plan.getString("possible_keys")).contains("ix_nhat_ky_thoi_gian");
                System.out.println("AUDIT date/page EXPLAIN key=" + plan.getString("key") + " type=" + plan.getString("type"));
            }
            try (var audit = statement.executeQuery("""
                SELECT ma_doi_tuong,truoc,sau FROM nhat_ky_quan_tri WHERE hanh_dong='SESSION_REVOKED'
                """)) {
                assertThat(audit.next()).isTrue();
                assertThat(audit.getLong(1)).isEqualTo(500);
                assertThat(audit.getString(2)).isNull();
                assertThat(audit.getString(3)).isNull();
            }
            try (var users = statement.executeQuery("""
                 SELECT n.mat_khau_bam, n.ngay_xac_minh_email FROM nguoi_dung n
                 JOIN gio_hang g USING (ma_nguoi_dung) WHERE n.ma_nguoi_dung=500
                 """)) {
                assertThat(users.next()).isTrue();
                assertThat(users.getString(1)).isEqualTo("fixture");
                assertThat(users.getObject(2)).isNull();
            }
            try (var identities = statement.executeQuery("""
                 SELECT ma_nguoi_dung, ma_dinh_danh_nha_cung_cap, ngay_lien_ket FROM danh_tinh_oauth
                 """)) {
                assertThat(identities.next()).isEqualTo(from == 6);
                if (from == 6) {
                    assertThat(identities.getLong(1)).isEqualTo(500);
                    assertThat(identities.getString(2)).isEqualTo("901");
                    assertThat(identities.getObject(3)).isNull();
                }
            }
        }
    }
}
