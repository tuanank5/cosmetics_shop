package com.beautyshop.service;

import com.beautyshop.security.NguoiDungDetails_Service;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {"spring.datasource.url=${ACCOUNT_OUTBOX_DB_URL}", "app.account.mail-enabled=false"})
@EnabledIfEnvironmentVariable(named = "ACCOUNT_OUTBOX_DB_URL", matches = "jdbc:mariadb://127[.]0[.]0[.]1:[0-9]+/account_outbox_fixture")
class ThuTaiKhoan_MariaDb_Test {
    @Autowired JdbcClient jdbc;
    @Autowired NguoiDung_Service users;
    @Autowired NguoiDungDetails_Service details;
    @Autowired PlatformTransactionManager manager;

    private long tao(boolean admin, boolean verified) {
        return jdbc.sql("""
            INSERT INTO nguoi_dung (email,mat_khau_bam,ho_ten,vai_tro,ngay_xac_minh_email)
            VALUES (?,'fixture','Fixture',?,?) RETURNING ma_nguoi_dung
            """).params(UUID.randomUUID()+"@example.invalid", admin ? "ADMIN" : "CUSTOMER",
                verified ? LocalDateTime.now() : null).query(Long.class).single();
    }

    private String email(long id) {
        return jdbc.sql("SELECT email FROM nguoi_dung WHERE ma_nguoi_dung=?").param(id).query(String.class).single();
    }

    private <T> T admin(long id, Callable<T> task) throws Exception {
        var principal = details.loadUserByUsername(email(id));
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(ctx);
        try { return task.call(); } finally { SecurityContextHolder.clearContext(); }
    }

    private NguoiDung_Service.KetQua khoa(long admin, long dich, UUID id) throws Exception {
        return admin(admin, () -> users.datTrangThai(dich, false, 0, id, LyDoTaiKhoan.BAO_VE_TAI_KHOAN));
    }

    @Test void haiAdminVaRollback() throws Exception {
        long a = tao(true,false), b = tao(true,false), dich = tao(false,false);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var batDau = new CountDownLatch(1);
            var x = pool.submit(() -> { batDau.await(); return khoa(a, dich, UUID.randomUUID()); });
            var y = pool.submit(() -> { batDau.await(); return khoa(b, dich, UUID.randomUUID()); });
            batDau.countDown();
            assertThat(List.of(x.get(10,TimeUnit.SECONDS), y.get(10,TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(NguoiDung_Service.KetQua.DA_DOI, NguoiDung_Service.KetQua.KHONG_DOI);
        }
        assertThat(jdbc.sql("SELECT trang_thai FROM thu_trang_thai_tai_khoan WHERE ma_nguoi_dung=?")
            .param(dich).query(String.class).list()).containsExactly("UNVERIFIED");
        var id = UUID.randomUUID();
        admin(a, () -> new TransactionTemplate(manager).execute(status -> {
            users.datTrangThai(dich,true,1,id,LyDoTaiKhoan.DA_XU_LY_RUI_RO);
            status.setRollbackOnly();
            return true;
        }));
        assertThat(jdbc.sql("SELECT ket_qua FROM nhat_ky_quan_tri WHERE correlation_id=?")
            .param(id.toString()).query(String.class).list()).containsExactly("FAILED");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM thu_trang_thai_tai_khoan WHERE ma_thao_tac=?")
            .param(id.toString()).query(Long.class).single()).isZero();
        assertThat(jdbc.sql("SELECT kich_hoat FROM nguoi_dung WHERE ma_nguoi_dung=?").param(dich)
            .query(Boolean.class).single()).isFalse();
    }

    @Test void replayDongThoiVaThuHoiQuyenAdmin() throws Exception {
        long a = tao(true,false), dich = tao(false,false);
        var id = UUID.randomUUID();
        var start = new CountDownLatch(1);
        Callable<NguoiDung_Service.KetQua> gui = () -> {
            start.await();
            return khoa(a,dich,id);
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var x = pool.submit(gui);
            var y = pool.submit(gui);
            start.countDown();
            assertThat(List.of(x.get(10,TimeUnit.SECONDS),y.get(10,TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(NguoiDung_Service.KetQua.DA_DOI, NguoiDung_Service.KetQua.DA_GHI);
        }
        assertThat(jdbc.sql("SELECT CONCAT(ket_qua,':',ly_do,':',truoc,':',sau) FROM nhat_ky_quan_tri WHERE correlation_id=?")
            .param(id.toString()).query(String.class).list()).containsExactly("SUCCESS:BAO_VE_TAI_KHOAN:true:false");
        admin(a, () -> {
            jdbc.sql("UPDATE nguoi_dung SET phien_ban_xac_thuc=phien_ban_xac_thuc+1 WHERE ma_nguoi_dung=?").param(a).update();
            assertThatThrownBy(() -> users.datTrangThai(dich,true,1,UUID.randomUUID(),LyDoTaiKhoan.DA_XU_LY_RUI_RO))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            return true;
        });
    }

    @Test void hetLeaseKhongGuiLai() throws Exception {
        var mail = mock(Email_Service.class);
        var id = UUID.randomUUID();
        khoa(tao(true,false),tao(false,true),id);
        jdbc.sql("""
            UPDATE thu_trang_thai_tai_khoan SET trang_thai='SENDING',ma_lease=UUID(),
                het_lease=TIMESTAMPADD(MINUTE,-1,UTC_TIMESTAMP(6)),so_lan_gui=1 WHERE ma_thao_tac=?
            """).param(id.toString()).update();
        var worker = new GuiThuTaiKhoan_Job(jdbc,manager,mail);
        worker.chay();
        worker.chay();
        assertThat(trangThai(id)).isEqualTo("UNKNOWN");
        verifyNoInteractions(mail);
    }

    @Test void leaseNguoiNhanVaKetQuaGui() throws Exception {
        var mail = mock(Email_Service.class);
        var worker = new GuiThuTaiKhoan_Job(jdbc, manager, mail);
        long a = tao(true,false), dich = tao(false,true);
        String original = email(dich);
        var id = UUID.randomUUID();
        khoa(a,dich,id);
        jdbc.sql("UPDATE nguoi_dung SET email=? WHERE ma_nguoi_dung=?")
            .params(UUID.randomUUID()+"@example.invalid",dich).update();
        var sending = new CountDownLatch(1);
        var finish = new CountDownLatch(1);
        doAnswer(call -> { sending.countDown(); return finish.await(10,TimeUnit.SECONDS); })
            .when(mail).guiTrangThai(eq(id),eq(original),eq(false),any());
        try (var pool = Executors.newSingleThreadExecutor()) {
            var first = pool.submit(worker::chay);
            try {
                assertThat(sending.await(10,TimeUnit.SECONDS)).isTrue();
                worker.chay();
                verify(mail,times(1)).guiTrangThai(eq(id),eq(original),eq(false),any());
            } finally { finish.countDown(); }
            first.get(10,TimeUnit.SECONDS);
        }
        assertThat(trangThai(id)).isEqualTo("SENT");
        var unknown = UUID.randomUUID();
        admin(a, () -> users.datTrangThai(dich,true,1,unknown,LyDoTaiKhoan.DA_XU_LY_RUI_RO));
        doThrow(new MailSendException("fixture",new SocketTimeoutException())).when(mail).guiTrangThai(any(),any(),eq(true),any());
        worker.chay();
        assertThat(trangThai(unknown)).isEqualTo("UNKNOWN");
        var next = UUID.randomUUID();
        admin(a, () -> users.datTrangThai(dich,false,1,next,LyDoTaiKhoan.BAO_VE_TAI_KHOAN));
        worker.chay();
        assertThat(trangThai(next)).isEqualTo("PENDING");
        assertThat(jdbc.sql("SELECT kich_hoat FROM nguoi_dung WHERE ma_nguoi_dung=?").param(dich)
            .query(Boolean.class).single()).isFalse();
    }

    @Test void thuLaiDungLichVaDungSauBonLan() throws Exception {
        var mail = mock(Email_Service.class);
        var worker = new GuiThuTaiKhoan_Job(jdbc, manager, mail);
        var id = UUID.randomUUID();
        khoa(tao(true,false),tao(false,true),id);
        doThrow(new MailSendException("fixture",new ConnectException())).when(mail).guiTrangThai(any(),any(),anyBoolean(),any());
        for (int lan = 1; lan <= 4; lan++) {
            worker.chay();
            assertThat(trangThai(id)).isEqualTo(lan == 4 ? "FAILED" : "PENDING");
            if (lan < 4) {
                int phut = List.of(1,5,30).get(lan-1);
                assertThat(jdbc.sql("SELECT TIMESTAMPDIFF(SECOND,UTC_TIMESTAMP(6),gui_tu) FROM thu_trang_thai_tai_khoan WHERE ma_thao_tac=?")
                    .param(id.toString()).query(Integer.class).single()).isBetween(phut*60-5,phut*60);
                worker.chay();
                jdbc.sql("UPDATE thu_trang_thai_tai_khoan SET gui_tu=UTC_TIMESTAMP(6) WHERE ma_thao_tac=?")
                    .param(id.toString()).update();
            }
        }
        worker.chay();
        verify(mail,times(4)).guiTrangThai(any(),any(),anyBoolean(),any());
    }

    @Test void workerHetLeaseTruocKhiGhiKetQua() throws Exception {
        var mail = mock(Email_Service.class);
        var id = UUID.randomUUID();
        khoa(tao(true,false), tao(false,true), id);
        doAnswer(call -> {
            jdbc.sql("UPDATE thu_trang_thai_tai_khoan SET het_lease=UTC_TIMESTAMP(6) WHERE ma_thao_tac=?")
                .param(id.toString()).update();
            return null;
        }).when(mail).guiTrangThai(eq(id),any(),anyBoolean(),any());
        new GuiThuTaiKhoan_Job(jdbc,manager,mail).chay();
        assertThat(trangThai(id)).isEqualTo("UNKNOWN");
    }

    @Test void phanBietKhongDoiVaFormCuSauKhoaMo() throws Exception {
        long a = tao(true,false), b = tao(true,false), dich = tao(false,false);
        khoa(a,dich,UUID.randomUUID());
        assertThat(khoa(b,dich,UUID.randomUUID())).isEqualTo(NguoiDung_Service.KetQua.KHONG_DOI);
        admin(a, () -> users.datTrangThai(dich,true,1,UUID.randomUUID(),LyDoTaiKhoan.DA_XU_LY_RUI_RO));
        assertThat(khoa(b,dich,UUID.randomUUID())).isEqualTo(NguoiDung_Service.KetQua.FORM_CU);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM thu_trang_thai_tai_khoan WHERE ma_nguoi_dung=?")
            .param(dich).query(Long.class).single()).isEqualTo(2);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"beforeAudit", "afterAudit", "outbox"})
    void rollbackTheoTungDiemVaRetry(String diem) throws Exception {
        long actor = tao(true,false), dich = tao(false,true);
        var id = UUID.randomUUID();
        String sql = switch (diem) {
            case "beforeAudit" -> "BEFORE INSERT ON nhat_ky_quan_tri";
            case "afterAudit" -> "AFTER INSERT ON nhat_ky_quan_tri";
            default -> "BEFORE INSERT ON thu_trang_thai_tai_khoan";
        };
        String dieuKien = diem.equals("outbox") ? "NEW.ma_nguoi_dung=" + dich
            : "NEW.ket_qua='SUCCESS' AND NEW.ma_doi_tuong=" + dich;
        jdbc.sql("CREATE TRIGGER account_test_fault " + sql + " FOR EACH ROW BEGIN IF " + dieuKien
            + " THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fixture fault'; END IF; END").update();
        try {
            assertThatThrownBy(() -> khoa(actor,dich,id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        } finally {
            jdbc.sql("DROP TRIGGER account_test_fault").update();
        }
        assertThat(jdbc.sql("SELECT CONCAT(kich_hoat,':',phien_ban_xac_thuc) FROM nguoi_dung WHERE ma_nguoi_dung=?")
            .param(dich).query(String.class).single()).isEqualTo("1:0");
        assertThat(jdbc.sql("""
            SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE correlation_id=? AND hanh_dong='ACCOUNT_LOCK'
            AND ma_nguoi_thuc_hien=? AND vai_tro_nguoi_thuc_hien='ADMIN' AND loai_doi_tuong='ACCOUNT'
            AND ma_doi_tuong=? AND ket_qua='FAILED' AND ly_do='ROLLBACK' AND truoc IS NULL AND sau IS NULL
            AND thoi_gian BETWEEN UTC_TIMESTAMP(6)-INTERVAL 1 MINUTE AND UTC_TIMESTAMP(6)
            """).params(id.toString(),actor,dich).query(Long.class).single()).isOne();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM thu_trang_thai_tai_khoan WHERE ma_nguoi_dung=?")
            .param(dich).query(Long.class).single()).isZero();
        assertThat(khoa(actor,dich,id)).isEqualTo(NguoiDung_Service.KetQua.FORM_CU);
        assertThat(khoa(actor,dich,UUID.randomUUID())).isEqualTo(NguoiDung_Service.KetQua.DA_DOI);
        assertThat(jdbc.sql("SELECT ket_qua FROM nhat_ky_quan_tri WHERE ma_doi_tuong=? AND hanh_dong='ACCOUNT_LOCK' ORDER BY ma_nhat_ky")
            .param(dich).query(String.class).list()).containsExactly("FAILED","SUCCESS");
        // Hoàn tất event của riêng fixture này trước ca worker kế tiếp.
        new GuiThuTaiKhoan_Job(jdbc,manager,mock(Email_Service.class)).chay();
    }

    @Autowired javax.sql.DataSource datasource;

    @Test void sauSmtpMatKetNoiKhiGhiSent() throws Exception {
        long actor = tao(true,false), dich = tao(false,true);
        var id = UUID.randomUUID();
        var mail = mock(Email_Service.class);
        khoa(actor,dich,id);
        try (var c = datasource.getConnection()) {
            var ds = new org.springframework.jdbc.datasource.SingleConnectionDataSource(c,true);
            var worker = new GuiThuTaiKhoan_Job(JdbcClient.create(ds),
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds), mail);
            doAnswer(call -> { c.close(); return null; }).when(mail).guiTrangThai(eq(id),any(),eq(false),any());
            assertThatThrownBy(worker::chay).isInstanceOf(org.springframework.dao.DataAccessException.class);
        }
        assertThat(trangThai(id)).isEqualTo("SENDING");
        jdbc.sql("UPDATE thu_trang_thai_tai_khoan SET het_lease=UTC_TIMESTAMP(6) WHERE ma_thao_tac=?")
            .param(id.toString()).update();
        new GuiThuTaiKhoan_Job(jdbc,manager,mail).chay();
        assertThat(trangThai(id)).isEqualTo("UNKNOWN");
        verify(mail,times(1)).guiTrangThai(eq(id),any(),eq(false),any());
    }

    @Test void workerChiThayEventSauCommitVaGiuThuTu() throws Exception {
        long actor = tao(true,false), dich = tao(false,true);
        var lock = UUID.randomUUID();
        var unlock = UUID.randomUUID();
        var mail = mock(Email_Service.class);
        try (var pool = Executors.newSingleThreadExecutor()) {
            admin(actor, () -> new TransactionTemplate(manager).execute(status -> {
                users.datTrangThai(dich,false,0,lock,LyDoTaiKhoan.BAO_VE_TAI_KHOAN);
                var other = pool.submit(() -> new GuiThuTaiKhoan_Job(jdbc,manager,mail).chay());
                assertThatCode(() -> other.get(10,TimeUnit.SECONDS)).doesNotThrowAnyException();
                verifyNoInteractions(mail);
                return true;
            }));
        }
        admin(actor, () -> users.datTrangThai(dich,true,1,unlock,LyDoTaiKhoan.DA_XU_LY_RUI_RO));
        new GuiThuTaiKhoan_Job(jdbc,manager,mail).chay();
        new GuiThuTaiKhoan_Job(jdbc,manager,mail).chay();
        var order = inOrder(mail);
        order.verify(mail).guiTrangThai(eq(lock),eq(email(dich)),eq(false),any());
        order.verify(mail).guiTrangThai(eq(unlock),eq(email(dich)),eq(true),any());
        assertThat(trangThai(lock)).isEqualTo("SENT");
        assertThat(trangThai(unlock)).isEqualTo("SENT");
    }

    @Test void khoaAdminTrongKhiGhi() throws Exception {
        long a = tao(true,false), dich = tao(false,false);
        var id = UUID.randomUUID();
        var daCoPhien = new CountDownLatch(1);
        var ghi = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var request = pool.submit(() -> admin(a, () -> {
                daCoPhien.countDown();
                if (!ghi.await(10,TimeUnit.SECONDS)) throw new AssertionError("Fixture timeout");
                return users.datTrangThai(dich,false,0,id,LyDoTaiKhoan.BAO_VE_TAI_KHOAN);
            }));
            assertThat(daCoPhien.await(10,TimeUnit.SECONDS)).isTrue();
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                jdbc.sql("UPDATE nguoi_dung SET kich_hoat=FALSE, phien_ban_xac_thuc=phien_ban_xac_thuc+1 WHERE ma_nguoi_dung=?")
                    .param(a).update();
                ghi.countDown();
            });
            assertThatThrownBy(() -> request.get(10,TimeUnit.SECONDS))
                .hasCauseInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
        assertThat(jdbc.sql("SELECT kich_hoat FROM nguoi_dung WHERE ma_nguoi_dung=?").param(dich)
            .query(Boolean.class).single()).isTrue();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE correlation_id=?")
            .param(id.toString()).query(Long.class).single()).isZero();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM thu_trang_thai_tai_khoan WHERE ma_thao_tac=?")
            .param(id.toString()).query(Long.class).single()).isZero();
    }

    @Test void workerCuTraVeSauKhiPollerDaThuHoiLease() throws Exception {
        var mail = mock(Email_Service.class);
        var id = UUID.randomUUID();
        khoa(tao(true,false),tao(false,true),id);
        doAnswer(call -> {
            jdbc.sql("UPDATE thu_trang_thai_tai_khoan SET het_lease=UTC_TIMESTAMP(6) WHERE ma_thao_tac=?")
                .param(id.toString()).update();
            new GuiThuTaiKhoan_Job(jdbc,manager,mail).chay();
            assertThat(trangThai(id)).isEqualTo("UNKNOWN");
            return null;
        }).when(mail).guiTrangThai(eq(id),any(),eq(false),any());
        new GuiThuTaiKhoan_Job(jdbc,manager,mail).chay();
        verify(mail,times(1)).guiTrangThai(eq(id),any(),eq(false),any());
        assertThat(trangThai(id)).isEqualTo("UNKNOWN");
    }

    private String trangThai(UUID id) {
        return jdbc.sql("SELECT trang_thai FROM thu_trang_thai_tai_khoan WHERE ma_thao_tac=?")
            .param(id.toString()).query(String.class).single();
    }
}
