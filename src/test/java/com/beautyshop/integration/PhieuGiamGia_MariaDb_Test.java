package com.beautyshop.integration;

import com.beautyshop.repository.PhieuGiamGia_Repository;
import com.beautyshop.service.PhieuGiamGia_Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
@QuanTriFixture
class PhieuGiamGia_MariaDb_Test {
    private static final String BASE = "/admin/vouchers";

    private PhieuGiamGia_Service.GiamGia xemTruoc(String code, BigDecimal amount) {
        var line = new com.beautyshop.repository.GioHang_Repository.MucGio(1,"","",amount,1,1,"",null,null,null);
        return phieuGiamGiaService.xemGio(code,null,null,java.util.List.of(line)).hang();
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired PhieuGiamGia_Repository repository;
    @Autowired PhieuGiamGia_Service phieuGiamGiaService;

    @Test
    void luuPhamViQuotaVaAuditChiGhiKhiThayDoi() throws Exception {
        String code = UUID.randomUUID().toString();
        mvc.perform(form(BASE+"/new",code,"FIXED","10000").param("slot","VAN_CHUYEN").param("gioiHanKhach","1"))
            .andExpect(redirectedUrl(BASE));
        long id=maPhieu(code);
        var before=repository.phamVi(id);
        assertThat(before.slot()).isEqualTo("VAN_CHUYEN");
        assertThat(before.gioiHanKhach()).isOne();
        String path=BASE+"/"+id+"/edit";
        var edit = form(path,code,"FIXED","10000").param("slot","VAN_CHUYEN").param("gioiHanKhach","2");
        for (int i=0;i<2;i++) mvc.perform(edit)
            .andExpect(redirectedUrl(BASE));
        assertThat(repository.phamVi(id).gioiHanKhach()).isEqualTo(2);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE hanh_dong='VOUCHER_UPDATE' AND ma_doi_tuong=?")
            .param(id).query(Integer.class).single()).isOne();
    }

    @Test
    void adminCrudChuanHoaMaVaKhoaQuyenGhi() throws Exception {
        String code = UUID.randomUUID().toString();
        mvc.perform(get(BASE).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get(BASE).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/new").with(user("customer").roles("CUSTOMER")).with(csrf()))
            .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/new")).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/new")).andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(form(BASE + "/new", "  " + code + " ", "FIXED", "20000")
                .param("soLuotDaDung", "1").param("soLuotTamGiu", "1"))
            .andExpect(redirectedUrl(BASE));
        long ma = maPhieu(code);
        var phieu = repository.timTheoMa(ma, false).orElseThrow();
        assertThat(phieu.cauHinh().maApDung()).isEqualTo(code.toUpperCase(Locale.ROOT));
        assertThat(phieu.soLuotDaDung()).isZero();
        assertThat(phieu.soLuotTamGiu()).isZero();
        mvc.perform(get(BASE)).andExpect(status().isOk())
            .andExpect(content().string(containsString(code.toUpperCase(Locale.ROOT))))
            .andExpect(content().string(containsString("Đã dùng 0 · Đang giữ 0 · Còn 2")));
        mvc.perform(get(BASE + "/" + ma + "/edit"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("value=\"20000\"")));
        mvc.perform(form(BASE + "/new", code, "FIXED", "1"))
            .andExpect(model().attributeHasFieldErrors("form", "maApDung"));
        mvc.perform(form(BASE + "/" + ma + "/edit", code, "PERCENT", "20").with(request -> {
            request.removeParameter("dangHoatDong");
            request.setParameter("_dangHoatDong", "on");
            return request;
        })).andExpect(redirectedUrl(BASE));
        var hienTai = repository.timTheoMa(ma, false).orElseThrow().cauHinh();
        mvc.perform(form(BASE + "/" + ma + "/edit", code, "PERCENT", "20").with(request -> {
            request.removeParameter("dangHoatDong");
            request.setParameter("_dangHoatDong", "on");
            request.setParameter("batDauLuc", hienTai.batDauLuc().toString());
            request.setParameter("ketThucLuc", hienTai.ketThucLuc().toString());
            return request;
        })).andExpect(redirectedUrl(BASE));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE hanh_dong='VOUCHER_UPDATE' AND ma_doi_tuong=?")
            .param(ma).query(Long.class).single()).isOne();
        var daSua = repository.timTheoMa(ma, false).orElseThrow().cauHinh();
        assertThat(daSua.loaiGiamGia()).isEqualTo("PERCENT");
        assertThat(daSua.dangHoatDong()).isFalse();
        assertThatThrownBy(() -> xemTruoc(code, BigDecimal.TEN))
            .isInstanceOf(IllegalArgumentException.class);
        mvc.perform(post(BASE + "/" + ma + "/delete"))
            .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + ma + "/delete").with(csrf()))
            .andExpect(redirectedUrl(BASE)).andExpect(flash().attributeExists("message"));
        mvc.perform(get(BASE + "/" + ma + "/edit")).andExpect(status().isNotFound());
    }

    @Test
    void kiemTraThoiGianDieuKienDonVaTranGiam() throws Exception {
        String code = UUID.randomUUID().toString();
        mvc.perform(form(BASE + "/new", code, "PERCENT", "101").with(request -> {
            request.setParameter("ketThucLuc", request.getParameter("batDauLuc"));
            return request;
        })).andExpect(model().attributeHasFieldErrors("form", "giaTriGiam", "ketThucLuc"));
        mvc.perform(form(BASE + "/new", code, "FIXED", "50.5"))
            .andExpect(model().attributeHasFieldErrors("form", "giaTriGiam"));
        mvc.perform(form(BASE + "/new", code, "FIXED", "250000").with(request -> {
            request.setParameter("donToiThieu", "100000");
            return request;
        })).andExpect(redirectedUrl(BASE));
        assertThatThrownBy(() -> xemTruoc(code, new BigDecimal("99999")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mức tối thiểu");
        assertThat(xemTruoc(code, new BigDecimal("100000")).soTien())
            .isEqualByComparingTo("100000");
        long ma = maPhieu(code);
        mvc.perform(form(BASE + "/" + ma + "/edit", code, "PERCENT", "10").param("mucGiamToiDa", "20000"))
            .andExpect(redirectedUrl(BASE));
        assertThat(xemTruoc(code, new BigDecimal("100005")).soTien())
            .isEqualByComparingTo("10000");
        assertThat(xemTruoc(code, new BigDecimal("510000")).soTien())
            .isEqualByComparingTo("20000");
        assertThat(repository.timTheoMa(ma, false).orElseThrow().soLuotConLai()).isEqualTo(2);
        var batDau = LocalDateTime.now(ZoneOffset.UTC).minusDays(1).withNano(123456000);
        var ketThuc = batDau.plusDays(2);
        jdbc.sql("UPDATE phieu_giam_gia SET bat_dau_luc = ?, ket_thuc_luc = ? WHERE ma_phieu_giam_gia = ?")
            .params(batDau, ketThuc, ma).update();
        String sua = BASE + "/" + ma + "/edit";
        mvc.perform(get(sua)).andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"" + batDau.truncatedTo(ChronoUnit.MILLIS) + "\"")));
        mvc.perform(form(sua, code, "PERCENT", "10").with(request -> {
            request.setParameter("batDauLuc", batDau.truncatedTo(ChronoUnit.MILLIS).toString());
            request.setParameter("ketThucLuc", ketThuc.truncatedTo(ChronoUnit.MILLIS).toString());
            return request;
        })).andExpect(redirectedUrl(BASE));
        assertThat(repository.timTheoMa(ma, false).orElseThrow().cauHinh().ketThucLuc()).isEqualTo(ketThuc);
        jdbc.sql("""
            UPDATE phieu_giam_gia
            SET bat_dau_luc = UTC_TIMESTAMP() - INTERVAL 2 DAY, ket_thuc_luc = UTC_TIMESTAMP() - INTERVAL 1 DAY
            WHERE ma_phieu_giam_gia = ?
            """).param(ma).update();
        assertThatThrownBy(() -> xemTruoc(code, new BigDecimal("510000")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hết hạn");
    }

    private MockHttpServletRequestBuilder form(String path, String code, String loai, String giaTri) {
        var now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
        return post(path).with(csrf())
            .param("maApDung", code).param("loaiGiamGia", loai).param("giaTriGiam", giaTri)
            .param("donToiThieu", "0").param("gioiHanSuDung", "2").param("dangHoatDong", "true")
            .param("batDauLuc", now.minusDays(1).toString()).param("ketThucLuc", now.plusDays(1).toString());
    }

    private long maPhieu(String code) {
        return jdbc.sql("SELECT ma_phieu_giam_gia FROM phieu_giam_gia WHERE ma_ap_dung = ?")
            .param(code.toUpperCase(Locale.ROOT)).query(Long.class).single();
    }
}
