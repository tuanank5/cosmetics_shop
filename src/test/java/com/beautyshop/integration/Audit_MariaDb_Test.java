package com.beautyshop.integration;

import com.beautyshop.service.DonNhatKy_Service;
import com.beautyshop.service.DanhMucSanPhamThuongHieu_Service;
import com.beautyshop.domain.catalog.NhomDanhMucSanPhamThuongHieu;
import com.beautyshop.security.NguoiDungDetails_Service;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"app.audit.operator-ids=9100001","app.audit.clearance-source-enabled=true"})
@EnabledIfEnvironmentVariable(named="P01_MARIADB_TEST", matches="true")
@QuanTriFixture
class Audit_MariaDb_Test {
    @Autowired JdbcClient jdbc;
    @Autowired DonNhatKy_Service don;
    @Autowired DanhMucSanPhamThuongHieu_Service catalog;
    @Autowired PlatformTransactionManager manager;
    @Autowired NguoiDungDetails_Service users;

    @org.junit.jupiter.api.BeforeEach void taoNguonFixture() {
        jdbc.sql("""
            CREATE TABLE IF NOT EXISTS audit_purge_clearance (batch_id CHAR(36), audit_id BIGINT,
                expires_utc DATETIME(6) NOT NULL, PRIMARY KEY(batch_id,audit_id))
            """).update();
    }

    private void duyet(UUID batch,long id) {
        jdbc.sql("INSERT INTO audit_purge_clearance VALUES (?,?,UTC_TIMESTAMP(6)+INTERVAL 1 HOUR)")
            .params(batch.toString(),id).update();
    }

    @Test void commitRollbackVaKetQuaRieng() {
        long id = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc,duong_dan) VALUES ('Fixture',?) RETURNING ma_danh_muc")
            .param(UUID.randomUUID().toString()).query(Long.class).single();
        catalog.datTrangThai(NhomDanhMucSanPhamThuongHieu.DANH_MUC,id,false);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            catalog.datTrangThai(NhomDanhMucSanPhamThuongHieu.DANH_MUC,id,true);
            tx.setRollbackOnly();
        });
        assertThat(jdbc.sql("""
            SELECT CONCAT(ket_qua,':',COALESCE(truoc,'-'),':',COALESCE(sau,'-')) FROM nhat_ky_quan_tri
            WHERE hanh_dong='CATALOG_ACTIVE' AND ma_doi_tuong=? AND loai_doi_tuong='DANH_MUC'
            AND ma_nguoi_thuc_hien=9100001 ORDER BY ma_nhat_ky
            """).param(id).query(String.class).list()).containsExactly("SUCCESS:true:false","FAILED:-:-");
        assertThat(jdbc.sql("SELECT dang_hoat_dong FROM danh_muc WHERE ma_danh_muc=?")
            .param(id).query(Boolean.class).single()).isFalse();
    }

    @Test void dryRunKhongTinhHoSoChuaCoNguonDuyet() {
        long id = seed("1990-01-01");
        var batch = UUID.randomUUID();
        duyet(UUID.randomUUID(),id);
        jdbc.sql("INSERT INTO audit_purge_clearance VALUES (?,?,UTC_TIMESTAMP(6)-INTERVAL 1 SECOND)")
            .params(batch.toString(),id).update();
        assertThat(don.chay(batch,true,180,500).duDieuKien()).isZero();
        assertThat(don.chay(batch,false,180,500).daDon()).isZero();
        assertThatThrownBy(() -> don.chay(UUID.randomUUID(),false,180,501)).isInstanceOf(IllegalArgumentException.class);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE ma_nhat_ky=?")
            .param(id).query(Long.class).single()).isOne();
    }

    @Test void luuLaiDanhMucKhongSinhSuccess() {
        long id = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc,duong_dan) VALUES ('Fixture',?) RETURNING ma_danh_muc")
            .param(UUID.randomUUID().toString()).query(Long.class).single();
        var nhom = NhomDanhMucSanPhamThuongHieu.DANH_MUC;
        var form = catalog.edit(nhom,id);
        catalog.save(nhom,id,form,new org.springframework.validation.BeanPropertyBindingResult(form,"form"));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE loai_doi_tuong='DANH_MUC' AND ma_doi_tuong=?")
            .param(id).query(Long.class).single()).isZero();
    }

    private long seed(String luc) {
        return jdbc.sql("""
            INSERT INTO nhat_ky_quan_tri (vai_tro_nguoi_thuc_hien,hanh_dong,loai_doi_tuong,ma_doi_tuong,
                thoi_gian,ket_qua,correlation_id) VALUES ('SYSTEM','LEGACY','ACCOUNT',1,?,'SUCCESS',UUID())
                RETURNING ma_nhat_ky
            """).param(luc).query(Long.class).single();
    }

    @Test void batchBoundaryBaoLuuReplayVaRollback() {
        long a = seed("2000-01-01"), b = seed("2000-01-02");
        var replay = UUID.randomUUID();
        duyet(replay,a);
        duyet(replay,b);
        for (String lyDo : java.util.List.of("DOI_SOAT","TRANH_CHAP","NGHIA_VU")) {
            long id = seed("1999-01-01");
            duyet(replay,id);
            jdbc.sql("INSERT INTO bao_luu_nhat_ky VALUES (?,?)").params(id,lyDo).update();
        }
        assertThat(don.chay(UUID.randomUUID(),true,Integer.MAX_VALUE,1).duDieuKien()).isZero();
        var preview = don.chay(replay,true,180,1);
        assertThat(preview.daDon()).isZero();
        assertThat(preview.duDieuKien()).isGreaterThanOrEqualTo(2);
        var first = don.chay(replay,false,180,1);
        assertThat(first.daDon()).isEqualTo(1);
        assertThat(don.chay(replay,false,180,1)).isEqualTo(first);
        var failure = UUID.randomUUID();
        duyet(failure,b);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            don.chay(failure,false,180,1);
            tx.setRollbackOnly();
        });
        assertThat(jdbc.sql("SELECT ket_qua FROM nhat_ky_quan_tri WHERE correlation_id=?")
            .param(failure.toString()).query(String.class).single()).isEqualTo("FAILED");
        assertThat(jdbc.sql("SELECT ma_nhat_ky FROM nhat_ky_quan_tri WHERE ma_nhat_ky IN (?,?)")
            .params(a,b).query(Long.class).list()).containsExactly(b);
        var next = UUID.randomUUID();
        duyet(next,b);
        don.chay(next,false,180,1);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM bao_luu_nhat_ky").query(Long.class).single()).isGreaterThanOrEqualTo(3);
        assertThatThrownBy(() -> don.chay(UUID.randomUUID(),false,179,1)).isInstanceOf(IllegalArgumentException.class);
        // All checks below share one fixed MariaDB session clock.
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            jdbc.sql("SET timestamp=1800000000").update();
            try {
                var boundary = UUID.randomUUID();
                for (int micro : new int[]{-1,0,1}) {
                    String luc = jdbc.sql("SELECT CAST(UTC_TIMESTAMP(6) - INTERVAL 180 DAY + INTERVAL ? MICROSECOND AS CHAR)")
                        .param(micro).query(String.class).single();
                    duyet(boundary,seed(luc));
                }
                assertThat(don.chay(boundary,true,180,10).duDieuKien()).isEqualTo(1);
            } finally { jdbc.sql("SET timestamp=DEFAULT").update(); }
        });
    }

    @Test void rangBuocCorrelationActionVaBaoLuuDonChoDoiSoat() {
        String ref = UUID.randomUUID().toString();
        long id = jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi,ma_nguoi_dung,ten_nguoi_nhan,sdt_nguoi_nhan,
              dia_chi_khi_dat,tam_tinh,so_tien_giam,phi_van_chuyen,tong_thanh_toan,trang_thai,phuong_thuc_thanh_toan)
            VALUES (?,9100001,'Fixture','0000000000','Fixture',10,0,0,10,'PENDING_PAYMENT','MOMO') RETURNING ma_don_hang
            """).param(ref).query(Long.class).single();
        jdbc.sql("""
            INSERT INTO lan_thanh_toan (ma_don_hang,ma_yeu_cau,cong_thanh_toan,so_tien,trang_thai)
            VALUES (?,?,'MOMO_MOCK',10,'REFUND_PENDING')
            """).params(id,ref).update();
        String sql = """
            INSERT INTO nhat_ky_quan_tri (vai_tro_nguoi_thuc_hien,hanh_dong,loai_doi_tuong,ma_doi_tuong,
              thoi_gian,ket_qua,correlation_id) VALUES ('ADMIN','ORDER_STATUS','ORDER',?,'1998-01-01','SUCCESS',?)
            """;
        jdbc.sql(sql).params(id,ref).update();
        assertThatThrownBy(() -> jdbc.sql(sql).params(id,ref).update())
            .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        var batch = UUID.randomUUID();
        duyet(batch,jdbc.sql("SELECT ma_nhat_ky FROM nhat_ky_quan_tri WHERE correlation_id=?")
            .param(ref).query(Long.class).single());
        don.chay(batch,false,3650,1);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE correlation_id=?")
            .param(ref).query(Long.class).single()).isOne();
    }

    @Test void adminKhongDuocPhanCong() {
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            jdbc.sql("UPDATE nguoi_dung SET phien_ban_xac_thuc=phien_ban_xac_thuc+1 WHERE ma_nguoi_dung=9100001").update();
            assertThatThrownBy(() -> don.chay(UUID.randomUUID(),false,180,1)).isInstanceOf(AccessDeniedException.class);
            tx.setRollbackOnly();
        });
        String email = UUID.randomUUID()+"@example.invalid";
        jdbc.sql("INSERT INTO nguoi_dung (email,mat_khau_bam,ho_ten,vai_tro) VALUES (?,'disabled','Fixture','ADMIN')")
            .param(email).update();
        var principal = users.loadUserByUsername(email);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            principal,null,principal.getAuthorities()));
        assertThatThrownBy(() -> don.chay(UUID.randomUUID(),false,180,1)).isInstanceOf(AccessDeniedException.class);
    }
}
