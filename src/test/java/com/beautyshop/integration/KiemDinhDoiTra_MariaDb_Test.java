package com.beautyshop.integration;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;
import com.beautyshop.repository.DonHangCatalog_Repository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class KiemDinhDoiTra_MariaDb_Test extends DoiTraFixture {
    @LocalServerPort int port;

    @Test
    void haiAdminVaRollbackGiuaHaiDong() throws Exception {
        var chu = taiKhoan("CUSTOMER");
        var a = taiKhoan("ADMIN");
        var b = taiKhoan("ADMIN");
        long don = taoDon(chu.id(), "DELIVERED", 1);
        long sku1 = taoSku().id(), sku2 = taoSku().id();
        var form = doiTra(themMuc(don,sku1),1);
        form.getSoLuong().put(themMuc(don,sku2),1);
        long id = sauMua.yeuCau(chu.id(),don,form);
        String url = "/admin/returns/" + id;
        mvc.perform(lenh(url+"/status",a,0).param("status","APPROVED")).andExpect(flash().attributeExists("message"));
        var muc = sauMua.chiTiet(id,chu.id()).muc();
        String maKien = UUID.randomUUID().toString();
        mvc.perform(lenh(url+"/receive",a,1).param("thamChieu",maKien)
            .param("soLuong["+muc.getFirst().ma()+"]","1").param("soLuong["+muc.getLast().ma()+"]","1"))
            .andExpect(flash().attributeExists("message"));
        long kien = jdbc.sql("SELECT ma_kien FROM kien_doi_tra WHERE tham_chieu=?").param(maKien).query(Long.class).single();
        var dong = jdbc.sql("SELECT ma_dong_nhan FROM dong_nhan_doi_tra WHERE ma_kien=? ORDER BY ma_dong_nhan")
            .param(kien).query(Long.class).list();
        mvc.perform(kiem(url,a,2,dong.getFirst(),1,0)).andExpect(flash().attributeExists("message"));
        mvc.perform(kiem(url,a,3,dong.getLast(),1,0)).andExpect(flash().attributeExists("message"));
        jdbc.sql("UPDATE bien_the_san_pham SET dang_hoat_dong=0 WHERE ma_bien_the=?").param(sku2).update();
        mvc.perform(lenh(url+"/confirm",a,4).param("kien",Long.toString(kien))).andExpect(flash().attributeExists("error"));
        assertThat(ton(sku1)).isEqualTo(10);
        assertThat(ton(sku2)).isEqualTo(10);
        assertThat(soButToan(kien)).isZero();
        jdbc.sql("UPDATE bien_the_san_pham SET dang_hoat_dong=1 WHERE ma_bien_the=?").param(sku2).update();
        // The ledger insert succeeds before the overflow guard refuses the stock update.
        jdbc.sql("UPDATE bien_the_san_pham SET so_luong_trong_kho=2147483647 WHERE ma_bien_the=?").param(sku1).update();
        assertThat(xacNhan(url,a,kien)).isFalse();
        assertThat(soButToan(kien)).isZero();
        assertThat(ton(sku1)).isEqualTo(Integer.MAX_VALUE);
        jdbc.sql("UPDATE bien_the_san_pham SET so_luong_trong_kho=10 WHERE ma_bien_the=?").param(sku1).update();
        // Fixture DB only. Fail the last write after the stock, ledger and confirmation have been written.
        jdbc.sql("""
            CREATE TRIGGER return_fixture_audit BEFORE INSERT ON nhat_ky_quan_tri FOR EACH ROW
            BEGIN IF NEW.hanh_dong='RETURN_CONFIRM' AND NEW.ket_qua='SUCCESS' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='return fixture rollback'; END IF; END
            """).update();
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> xacNhan(url,a,kien))
                .hasRootCauseInstanceOf(java.sql.SQLException.class);
        } finally {
            jdbc.sql("DROP TRIGGER return_fixture_audit").update();
        }
        assertThat(soButToan(kien)).isZero();
        assertThat(ton(sku1)).isEqualTo(10);
        assertThat(ton(sku2)).isEqualTo(10);
        // Observe SUCCESS inside the real transaction, then roll it back before commit.
        new TransactionTemplate(tx).executeWithoutResult(status -> {
            try {
                assertThat(xacNhan(url,a,kien)).isTrue();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
            assertThat(soButToan(kien)).isEqualTo(2);
            assertThat(ton(sku1)).isEqualTo(11);
            assertThat(jdbc.sql("""
                SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE loai_doi_tuong='RETURN' AND ma_doi_tuong=?
                AND hanh_dong='RETURN_CONFIRM' AND ket_qua='SUCCESS'
                """).param(id).query(Integer.class).single()).isOne();
            status.setRollbackOnly();
        });
        assertThat(soButToan(kien)).isZero();
        assertThat(ton(sku1)).isEqualTo(10);
        assertThat(ton(sku2)).isEqualTo(10);
        var cho = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var mot = pool.submit(() -> { cho.await(); return xacNhan(url,a,kien); });
            var hai = pool.submit(() -> { cho.await(); return xacNhan(url,b,kien); });
            cho.countDown();
            assertThat(mot.get(10,TimeUnit.SECONDS) ^ hai.get(10,TimeUnit.SECONDS)).isTrue();
        }
        assertThat(soButToan(kien)).isEqualTo(2);
        assertThat(ton(sku1)).isEqualTo(11);
        assertThat(ton(sku2)).isEqualTo(11);
        assertThat(jdbc.sql("""
            SELECT ket_qua FROM nhat_ky_quan_tri WHERE loai_doi_tuong='RETURN' AND ma_doi_tuong=?
            AND hanh_dong='RETURN_CONFIRM' ORDER BY ma_nhat_ky
            """).param(id).query(String.class).list()).containsExactly("FAILED","FAILED","FAILED","FAILED","SUCCESS","FAILED");
    }

    @Test
    void kienSaiChuaNhanVaHaiPostCungMa() throws Exception {
        var customer = taiKhoan("CUSTOMER");
        var admin = taiKhoan("ADMIN");
        long sku = taoSku().id();
        long don = taoDon(customer.id(),"DELIVERED",1);
        long id = sauMua.yeuCau(customer.id(),don,doiTra(themMuc(don,sku),1));
        String url = "/admin/returns/"+id;
        mvc.perform(post(url+"/status").session(admin.session()).param("status","APPROVED")).andExpect(status().isForbidden());
        mvc.perform(lenh(url+"/status",admin,0).param("status","APPROVED")).andExpect(flash().attributeExists("message"));
        String code=UUID.randomUUID().toString(), token=UUID.randomUUID().toString();
        var cho = new CountDownLatch(1);
        java.util.concurrent.Callable<Boolean> nhan = () -> {
            cho.await();
            return mvc.perform(post(url+"/receive").session(admin.session()).with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .param("thamChieu",code).param("thaoTac",token).param("phienBan","1")
                .param("lech","2").param("lyDoLech","CHUA_XAC_MINH")).andReturn().getFlashMap().containsKey("message");
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a=pool.submit(nhan);
            var b=pool.submit(nhan);
            cho.countDown();
            assertThat(a.get(10,TimeUnit.SECONDS)).isTrue();
            assertThat(b.get(10,TimeUnit.SECONDS)).isTrue();
        }
        mvc.perform(get(url).session(admin.session())).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa xác minh được hàng")));
        var truoc=sauMua.chiTiet(id,customer.id());
        assertThat(truoc.chiTiet().yeuCau().trangThai().name()).isEqualTo("APPROVED");
        assertThat(truoc.muc().getFirst().thucNhan()).isZero();
        assertThat(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE correlation_id=?")
            .param(token).query(Integer.class).single()).isOne();
        long kien=jdbc.sql("SELECT ma_kien FROM kien_doi_tra WHERE tham_chieu=?").param(code).query(Long.class).single();
        var doiChieu=lenh(url+"/reconcile-parcel",admin,2).param("kien",Long.toString(kien)).param("chungTu","QA-PARCEL")
            .param("soLuong["+truoc.muc().getFirst().ma()+"]","1");
        mvc.perform(doiChieu).andExpect(flash().attributeExists("message"));
        mvc.perform(doiChieu).andExpect(flash().attributeExists("message"));
        var sau=sauMua.chiTiet(id,customer.id());
        assertThat(sau.chiTiet().yeuCau().trangThai().name()).isEqualTo("RECEIVED");
        assertThat(sau.muc().getFirst().choKiem()).isOne();
        assertThat(ton(sku)).isEqualTo(10);
    }

    @Test
    void bienBayNgayVaHoSoCu() throws Exception {
        var chu=taiKhoan("CUSTOMER");
        var admin=taiKhoan("ADMIN");
        long don=taoDon(chu.id(),"DELIVERED",2);
        long muc=themMuc(don,taoSku().id());
        jdbc.sql("UPDATE don_hang SET ngay_giao_thanh_cong='2026-01-01 00:00:00' WHERE ma_don_hang=?").param(don).update();
        new org.springframework.transaction.support.TransactionTemplate(tx).executeWithoutResult(status -> {
            try {
                jdbc.sql("SET timestamp=1767830400").update(); // 2026-01-08 00:00:00 UTC
                assertThat(sauMua.xemDon(chu.id(),don).don().conHan()).isTrue();
                jdbc.sql("UPDATE don_hang SET ngay_giao_thanh_cong=TIMESTAMPADD(MICROSECOND,-1,ngay_giao_thanh_cong) WHERE ma_don_hang=?")
                    .param(don).update();
                assertThat(sauMua.xemDon(chu.id(),don).don().conHan()).isFalse();
            } finally { jdbc.sql("SET timestamp=0").update(); }
        });
        long id=jdbc.sql("""
            INSERT INTO yeu_cau_doi_tra(ma_don_hang,ma_nguoi_dung,trang_thai,ly_do)
            VALUES(?,?,'RECEIVED','Fixture') RETURNING ma_yeu_cau_doi_tra
            """).params(don,chu.id()).query(Long.class).single();
        jdbc.sql("INSERT INTO chi_tiet_doi_tra(ma_yeu_cau_doi_tra,ma_chi_tiet_don_hang,so_luong) VALUES(?,?,1)").params(id,muc).update();
        mvc.perform(get("/account/returns/"+id).session(chu.session())).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa ghi nhận chứng cứ")));
        mvc.perform(lenh("/admin/returns/"+id+"/receive",admin,0).param("thamChieu","QA-LEGACY")
            .param("soLuong[1]","1")).andExpect(flash().attributeExists("error"));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM thao_tac_doi_tra WHERE ma_yeu_cau=?").param(id).query(Integer.class).single()).isZero();
    }

    @org.springframework.beans.factory.annotation.Autowired org.springframework.transaction.PlatformTransactionManager tx;
    @org.springframework.beans.factory.annotation.Autowired DonHangCatalog_Repository catalog;

    private boolean xacNhan(String url,TaiKhoan admin,long kien) throws Exception {
        return mvc.perform(lenh(url+"/confirm",admin,4).param("kien",Long.toString(kien)))
            .andReturn().getFlashMap().containsKey("message");
    }

    private java.util.List<Map<String,Object>> anhChup(long don) {
        var rows = new java.util.ArrayList<Map<String,Object>>();
        for (var bang : java.util.List.of("don_hang","chi_tiet_don_hang","lan_thanh_toan","tam_giu_phieu_giam_gia",
                "ghi_nhan_tru_kho_cod","tam_giu_ton_kho")) {
            rows.addAll(jdbc.sql("SELECT * FROM "+bang+" WHERE ma_don_hang=? ORDER BY 1").param(don).query().listOfRows());
        }
        rows.addAll(jdbc.sql("""
            SELECT p.* FROM phieu_giam_gia p JOIN tam_giu_phieu_giam_gia g USING(ma_phieu_giam_gia)
            WHERE g.ma_don_hang=? ORDER BY p.ma_phieu_giam_gia
            """).param(don).query().listOfRows());
        return rows;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"COD","MOMO_MOCK"})
    void giuChungTuHaiSlotVaAttempt(String gateway) throws Exception {
        var chu=taiKhoan("CUSTOMER");
        var admin=taiKhoan("ADMIN");
        long sku=taoSku().id();
        long don=taoDon(chu.id(),"DELIVERED",1);
        long muc=themMuc(don,sku);
        jdbc.sql("""
            UPDATE don_hang SET phuong_thuc_thanh_toan=?,tam_tinh=520000,so_tien_giam=30000,
            phi_chuan=25000,giam_van_chuyen=10000,phi_van_chuyen=15000,tong_thanh_toan=505000 WHERE ma_don_hang=?
            """).params(gateway.equals("COD") ? "COD" : "MOMO",don).update();
        jdbc.sql("UPDATE chi_tiet_don_hang SET don_gia=260000,giam_hang=30000 WHERE ma_chi_tiet_don_hang=?").param(muc).update();
        jdbc.sql("UPDATE lan_thanh_toan SET cong_thanh_toan=?,so_tien=505000 WHERE ma_don_hang=?").params(gateway,don).update();
        if (gateway.equals("COD")) {
            jdbc.sql("INSERT INTO ghi_nhan_tru_kho_cod(ma_don_hang,ma_bien_the,so_luong) VALUES(?,?,2)").params(don,sku).update();
        } else {
            jdbc.sql("""
                INSERT INTO tam_giu_ton_kho(ma_don_hang,ma_bien_the,so_luong,trang_thai,het_han_luc)
                VALUES(?,?,2,'CONSUMED',UTC_TIMESTAMP(6))
                """).params(don,sku).update();
        }
        long donGiu = taoDon(chu.id(),"PENDING_PAYMENT",0);
        jdbc.sql("""
            INSERT INTO tam_giu_ton_kho(ma_don_hang,ma_bien_the,so_luong,trang_thai,het_han_luc)
            VALUES(?,?,2,'HELD',TIMESTAMPADD(MINUTE,15,UTC_TIMESTAMP(6)))
            """).params(donGiu,sku).update();
        jdbc.sql("UPDATE bien_the_san_pham SET so_luong_tam_giu=2 WHERE ma_bien_the=?").param(sku).update();
        for (String slot:java.util.List.of("HANG","VAN_CHUYEN")) {
            int giam=slot.equals("HANG") ? 30000 : 10000;
            long ma=jdbc.sql("""
                INSERT INTO phieu_giam_gia(ma_ap_dung,slot,loai_giam_gia,gia_tri_giam,gioi_han_su_dung,
                    so_luot_da_dung,bat_dau_luc,ket_thuc_luc)
                VALUES (?,?,'FIXED',?,5,1,'2020-01-01','2099-01-01') RETURNING ma_phieu_giam_gia
                """).params(UUID.randomUUID().toString(),slot,giam).query(Long.class).single();
            jdbc.sql("INSERT INTO tam_giu_phieu_giam_gia(ma_don_hang,ma_phieu_giam_gia,slot,trang_thai,so_tien_giam) VALUES(?,?,?,'CONSUMED',?)")
                .params(don,ma,slot,giam).update();
        }
        var truoc=anhChup(don);
        var giuTruoc=anhChup(donGiu);
        long id=sauMua.yeuCau(chu.id(),don,doiTra(muc,1));
        String url="/admin/returns/"+id;
        mvc.perform(lenh(url+"/status",admin,0).param("status","APPROVED")).andExpect(flash().attributeExists("message"));
        var dong=sauMua.chiTiet(id,chu.id()).muc().getFirst();
        String code=UUID.randomUUID().toString();
        mvc.perform(lenh(url+"/receive",admin,1).param("thamChieu",code).param("soLuong["+dong.ma()+"]","1"))
            .andExpect(flash().attributeExists("message"));
        long kien=jdbc.sql("SELECT ma_kien FROM kien_doi_tra WHERE tham_chieu=?").param(code).query(Long.class).single();
        long nhan=jdbc.sql("SELECT ma_dong_nhan FROM dong_nhan_doi_tra WHERE ma_kien=?").param(kien).query(Long.class).single();
        mvc.perform(kiem(url,admin,2,nhan,1,0)).andExpect(flash().attributeExists("message"));
        mvc.perform(lenh(url+"/confirm",admin,3).param("kien",Long.toString(kien))).andExpect(flash().attributeExists("message"));
        assertThat(jdbc.sql("SELECT so_luong FROM nhap_kho_doi_tra WHERE ma_dong_nhan=? AND ma_bien_the=?")
            .params(nhan,sku).query(Integer.class).single()).isOne();
        mvc.perform(lenh(url+"/close",admin,4).param("nghiaVu","QA-DOC")).andExpect(flash().attributeExists("message"));
        assertThat(anhChup(don)).isEqualTo(truoc);
        assertThat(anhChup(donGiu)).isEqualTo(giuTruoc);
        assertThat(jdbc.sql("SELECT so_luong_tam_giu FROM bien_the_san_pham WHERE ma_bien_the=?").param(sku)
            .query(Integer.class).single()).isEqualTo(2);
        assertThat(ton(sku)).isEqualTo(11);
    }

    private int soButToan(long kien) {
        return jdbc.sql("""
            SELECT COUNT(*) FROM nhap_kho_doi_tra JOIN dong_nhan_doi_tra USING(ma_dong_nhan) WHERE ma_kien=?
            """).param(kien).query(Integer.class).single();
    }

    @Test
    void nhieuDotReplayVaChungTuNghiaVu() throws Exception {
        var chu = taiKhoan("CUSTOMER");
        var admin = taiKhoan("ADMIN");
        long sku = taoSku().id();
        long don = taoDon(chu.id(),"DELIVERED",1);
        long id = sauMua.yeuCau(chu.id(),don,doiTra(themMuc(don,sku),2));
        String url = "/admin/returns/" + id;
        var dong = sauMua.chiTiet(id,chu.id()).muc().getFirst();
        mvc.perform(lenh(url+"/status",admin,0).param("status","APPROVED")).andExpect(flash().attributeExists("message"));
        String code = UUID.randomUUID().toString();
        // Shipping may finish after the seven-day request window.
        jdbc.sql("UPDATE don_hang SET ngay_giao_thanh_cong=TIMESTAMPADD(DAY,-12,UTC_TIMESTAMP(6)) WHERE ma_don_hang=?")
            .param(don).update();
        for(int i=0;i<2;i++) {
            long version=1+i*3;
            mvc.perform(lenh(url+"/receive",admin,version).param("thamChieu",code+"-OVER")
                .param("soLuong["+dong.ma()+"]",Integer.toString(3-i))).andExpect(flash().attributeExists("error"));
            var receive=lenh(url+"/receive",admin,version).param("thamChieu",code+i).param("soLuong["+dong.ma()+"]","1");
            mvc.perform(receive).andExpect(flash().attributeExists("message"));
            mvc.perform(receive).andExpect(flash().attributeExists("message"));
            if (i==0) {
                mvc.perform(lenh(url+"/receive",admin,version+1).param("thamChieu",code+i)
                    .param("soLuong["+dong.ma()+"]","1")).andExpect(flash().attributeExists("error"));
                long donKhac = taoDon(chu.id(),"DELIVERED",1);
                long hoSoKhac = sauMua.yeuCau(chu.id(),donKhac,doiTra(themMuc(donKhac,taoSku().id()),1));
                String urlKhac = "/admin/returns/"+hoSoKhac;
                mvc.perform(lenh(urlKhac+"/status",admin,0).param("status","APPROVED")).andExpect(flash().attributeExists("message"));
                long mucKhac = sauMua.chiTiet(hoSoKhac,chu.id()).muc().getFirst().ma();
                mvc.perform(lenh(urlKhac+"/receive",admin,1).param("thamChieu",code+i)
                    .param("soLuong["+mucKhac+"]","1")).andExpect(flash().attributeExists("error"));
                assertThat(sauMua.chiTiet(hoSoKhac,chu.id()).muc().getFirst().thucNhan()).isZero();
            }
            long kien = jdbc.sql("SELECT ma_kien FROM kien_doi_tra WHERE tham_chieu=?").param(code+i).query(Long.class).single();
            long nhan = jdbc.sql("SELECT ma_dong_nhan FROM dong_nhan_doi_tra WHERE ma_kien=?").param(kien).query(Long.class).single();
            mvc.perform(kiem(url,admin,version+1,nhan,1,0)).andExpect(flash().attributeExists("message"));
            var confirm=lenh(url+"/confirm",admin,version+2).param("kien",Long.toString(kien));
            mvc.perform(confirm).andExpect(flash().attributeExists("message"));
            assertThat(soButToan(kien)).isOne();
            mvc.perform(confirm).andExpect(flash().attributeExists("message"));
            if (i==0) mvc.perform(lenh(url+"/close",admin,4).param("nghiaVu","QA"))
                .andExpect(flash().attributeExists("error"));
        }
        jdbc.sql("UPDATE lan_thanh_toan SET trang_thai='REFUND_PENDING' WHERE ma_don_hang=?").param(don).update();
        mvc.perform(lenh(url+"/close",admin,7).param("nghiaVu","QA")).andExpect(flash().attributeExists("error"));
        var kq=sauMua.chiTiet(id,chu.id());
        assertThat(kq.chiTiet().yeuCau().trangThai().name()).isEqualTo("RECEIVED");
        assertThat(kq.muc().getFirst().duBan()).isEqualTo(2);
        assertThat(kq.muc().getFirst().khongDu()).isZero();
        assertThat(ton(sku)).isEqualTo(12);
        assertThat(catalog.dem(DonHangCatalog_Repository.PhamVi.BIEN_THE,sku)).containsEntry(sku,1L);
        mvc.perform(get("/account/returns/"+id).session(chu.session())).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(code.toUpperCase()))));
    }

    @Test
    @EnabledIfEnvironmentVariable(named="RETURN_BROWSER_SCRIPT",matches=".+")
    void webKitKiemDinhVaXemTheoChu() throws Exception {
        var chu=taiKhoan("CUSTOMER");
        var admin=taiKhoan("ADMIN");
        long don=taoDon(chu.id(),"DELIVERED",1);
        long muc=themMuc(don,taoSku().id());
        jdbc.sql("UPDATE chi_tiet_don_hang SET so_luong=3 WHERE ma_chi_tiet_don_hang=?").param(muc).update();
        var runner=new ProcessBuilder("node",System.getenv("RETURN_BROWSER_SCRIPT"),"http://127.0.0.1:"+port,Long.toString(don));
        for (var entry:Map.of("RETURN_CUSTOMER",chu.id(),"RETURN_ADMIN",admin.id()).entrySet()) {
            runner.environment().put(entry.getKey(),jdbc.sql("SELECT email FROM nguoi_dung WHERE ma_nguoi_dung=?")
                .param(entry.getValue()).query(String.class).single());
        }
        runner.environment().put("RETURN_PASS",password);
        var process=runner.redirectErrorStream(true).start();
        try {
            assertThat(process.waitFor(110,TimeUnit.SECONDS)).isTrue();
            String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            assertThat(output).contains("[RETURN WebKit] PASS");
            assertThat(process.exitValue()).isZero();
        } finally { process.destroyForcibly(); }
        System.out.println("[RETURN WebKit] PASS desktop Admin and mobile Customer");
    }
}
