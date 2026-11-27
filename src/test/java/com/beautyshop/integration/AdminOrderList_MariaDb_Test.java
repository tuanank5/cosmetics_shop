package com.beautyshop.integration;

import com.beautyshop.dto.PhanTrang;
import com.beautyshop.repository.DonHang_Repository.DongDonAdmin;
import com.beautyshop.repository.DonHang_Repository.TongQuan;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.model.NguoiDung;
import com.beautyshop.service.DonHang_Service.ChiTietDon;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
@WithMockUser(roles = "ADMIN")
class AdminOrderList_MariaDb_Test {
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired NguoiDung_Repository nguoiDung;

    @Test
    void locHieuLucVaDemCungMotTapDon() throws Exception {
        long owner = nguoiDung.saveAndFlush(new NguoiDung(UUID.randomUUID() + "@example.invalid", null, "Fixture")).getMaNguoiDung();
        String ma = "ORDER-" + UUID.randomUUID();
        long cod = tao(owner, ma + "_COD", "COD", "CONFIRMED", 2000);
        lan(cod, "COD-" + cod, "COD", "UNPAID", 2000);
        lan(cod, "extra-" + cod, "COD", "PAID", 1000);
        long codPaid = tao(owner, ma + "-CODPAID", "COD", "DELIVERED", 2000);
        lan(codPaid, "COD-" + codPaid, "COD", "PAID", 2000);

        long paid = tao(owner, ma + "-PAID", "MOMO", "PACKING", 5000);
        lan(paid, UUID.randomUUID().toString(), "MOMO_MOCK", "PAID", 5000);
        lan(paid, UUID.randomUUID().toString(), "MOMO_MOCK", "REFUND_PENDING", 5000);
        lan(paid, UUID.randomUUID().toString(), "MOMO_MOCK", "FAILED", 5000);

        long pending = tao(owner, ma + "-PENDING", "MOMO", "PENDING_PAYMENT", 1000);
        lan(pending, UUID.randomUUID().toString(), "MOMO", "FAILED", 1000);
        lan(pending, UUID.randomUUID().toString(), "MOMO", "PENDING", 1000);
        long failed = tao(owner, ma + "-EXPIRED", "MOMO", "EXPIRED", 1000);
        lan(failed, UUID.randomUUID().toString(), "MOMO", "FAILED", 1000);
        long refund = tao(owner, ma + "-REFUND", "MOMO", "EXPIRED", 1000);
        lan(refund, UUID.randomUUID().toString(), "MOMO", "REFUND_PENDING", 1000);
        var tongQuan = mvc.perform(get("/admin/orders")).andExpect(status().isOk())
            .andReturn().getModelAndView().getModel().get("tongQuan");

        var tra = mvc.perform(get("/admin/orders").param("q", ma).param("method", "MOMO")
                .param("status", "PACKING", "EXPIRED").param("paymentStatus", "PAID"))
            .andExpect(status().isOk()).andExpect(model().attribute("tongQuan", tongQuan))
            .andExpect(content().string(containsString("1 đơn phù hợp")))
            .andExpect(content().string(not(containsString("Hidden fixture recipient"))));
        kiemTra(tra, 0, 20, 1, paid);
        for (var muc : List.of(new GiaoDich(cod, "UNPAID"), new GiaoDich(codPaid, "PAID"), new GiaoDich(paid, "PAID"),
                new GiaoDich(pending, "PENDING"), new GiaoDich(failed, "FAILED"), new GiaoDich(refund, "REFUND_PENDING"))) {
            assertThat(chiTiet(muc.id()).thanhToan()).isEqualTo(muc.trangThai());
            assertThat(dong(loc(ma).param("paymentStatus", muc.trangThai())))
                .anySatisfy(don -> {
                    assertThat(don.maDonHang()).isEqualTo(muc.id());
                    assertThat(don.thanhToan()).isEqualTo(muc.trangThai());
                });
        }
        kiemTra(mvc.perform(loc(ma).param("paymentStatus", "REFUND_PENDING")), 0, 20, 1, refund);
        kiemTra(mvc.perform(loc(ma).param("sort", "totalDesc")), 0, 20, 6, paid, codPaid, cod, refund, failed, pending);
        kiemTra(mvc.perform(loc(ma).param("sort", "totalAsc")), 0, 20, 6, refund, failed, pending, codPaid, cod, paid);
        kiemTra(mvc.perform(loc(ma + "_")), 0, 20, 1, cod);
        kiemTra(mvc.perform(loc(ma + "-NONE")), 0, 20, 0);
        mvc.perform(get("/admin/orders")).andExpect(model().attribute("tongQuan", tongQuan));
        assertThat(chiTiet(pending).don().trangThai().name()).isEqualTo("PENDING_PAYMENT");
    }

    @Test
    void locNgayTheoGioVietNamVaGiuKhoangNgayKhiQuayLai() throws Exception {
        long owner = nguoiDung.saveAndFlush(new NguoiDung(UUID.randomUUID() + "@example.invalid", null, "Khách lọc ngày")).getMaNguoiDung();
        String ma = "DATE-" + UUID.randomUUID();
        long truoc = tao(owner, ma + "-BEFORE", "COD", "CONFIRMED", 1000);
        long trong = tao(owner, ma + "-IN", "COD", "CONFIRMED", 1000);
        long sau = tao(owner, ma + "-AFTER", "COD", "CONFIRMED", 1000);
        for (var buoc : List.of(new Ngay(truoc, "2026-01-01 16:59:59"), new Ngay(trong, "2026-01-01 17:00:00"),
                new Ngay(sau, "2026-01-02 17:00:00"))) {
            jdbc.sql("UPDATE don_hang SET ngay_tao = ? WHERE ma_don_hang = ?").params(buoc.luc(), buoc.id()).update();
            lan(buoc.id(), "COD-" + buoc.id(), "COD", "UNPAID", 1000);
        }
        var ketQua = mvc.perform(loc(ma).param("fromDate", "2026-01-02").param("toDate", "2026-01-02"))
            .andExpect(content().string(containsString("02/01/2026 00:00")))
            .andExpect(content().string(containsString("listFromDate=2026-01-02&amp;listToDate=2026-01-02")));
        kiemTra(ketQua, 0, 20, 1, trong);
        var post = mvc.perform(post("/admin/orders/" + trong + "/status").with(csrf()).param("status", "PACKING")
                .param("listQ", ma).param("listFromDate", "2026-01-02").param("listToDate", "2026-01-02").param("listPage", "0"))
            .andExpect(flash().attributeExists("message")).andReturn().getResponse().getRedirectedUrl();
        mvc.perform(get(java.net.URI.create(post))).andExpect(status().isOk())
            .andExpect(content().string(containsString("fromDate=2026-01-02&amp;toDate=2026-01-02&amp;page=0#admin-order-")));
        mvc.perform(loc(ma).param("fromDate", "2026-02-30")).andExpect(status().isBadRequest());
        mvc.perform(loc(ma).param("fromDate", "2026-01-03").param("toDate", "2026-01-02")).andExpect(status().isBadRequest());
    }

    @Test
    void phanTrangOnDinhVaGiuBoLocQuaPost() throws Exception {
        long owner = nguoiDung.saveAndFlush(new NguoiDung(UUID.randomUUID() + "@example.invalid", null, "Fixture")).getMaNguoiDung();
        String ma = "ORDER-" + UUID.randomUUID();
        var ids = new Long[12];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = tao(owner, ma + "-" + i, "COD", "CONFIRMED", 1000);
            lan(ids[i], "COD-" + ids[i], "COD", "UNPAID", 1000);
        }
        kiemTra(mvc.perform(loc(ma).param("page", "999").param("size", "10").param("sort", "totalDesc")),
            1, 10, 12, ids[1], ids[0]);
        kiemTra(mvc.perform(loc(ma).param("page", "1").param("size", "10").param("sort", "oldest")),
            1, 10, 12, ids[10], ids[11]);
        assertThat(dong(loc(ma).param("sort", "unknown").param("size", "10000"))).hasSize(12)
            .first().extracting(DongDonAdmin::maDonHang).isEqualTo(ids[11]);

        String path = "/admin/orders/" + ids[0];
        String url = mvc.perform(post(path + "/status").with(csrf()).param("status", "PACKING")
                .param("listStatus", "CONFIRMED", "PACKING").param("listQ", ma).param("listMethod", "COD")
                .param("listPaymentStatus", "UNPAID").param("listSort", "totalDesc").param("listPage", "1")
                .param("listSize", "10").param("next", "https://example.invalid"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("message"))
            .andReturn().getResponse().getRedirectedUrl();
        assertThat(url).startsWith(path + "?").doesNotContain("example.invalid");
        mvc.perform(get(java.net.URI.create(url)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/admin/orders?q=" + ma
                + "&amp;method=COD&amp;paymentStatus=UNPAID&amp;sort=totalDesc&amp;page=1&amp;size=10"
                + "&amp;status=CONFIRMED&amp;status=PACKING#admin-order-" + ids[0] + "\"")));
        mvc.perform(post(path + "/status").param("status", "SHIPPING")).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder loc(String ma) { return get("/admin/orders").param("q", ma); }

    private ChiTietDon chiTiet(long id) throws Exception {
        var model = mvc.perform(get("/admin/orders/" + id)).andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        return (ChiTietDon) model.get("chiTiet");
    }

    @SuppressWarnings("unchecked")
    private List<DongDonAdmin> dong(MockHttpServletRequestBuilder yeuCau) throws Exception {
        var model = mvc.perform(yeuCau).andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        return (List<DongDonAdmin>) model.get("donHang");
    }

    @SuppressWarnings("unchecked")
    private void kiemTra(ResultActions tra, int page, int size, long count, Long... ids) throws Exception {
        var model = tra.andExpect(status().isOk()).andReturn().getModelAndView().getModel();
        var pager = (PhanTrang) model.get("pager");
        assertThat(pager.trang()).isEqualTo(page);
        assertThat(pager.kichThuoc()).isEqualTo(size);
        assertThat(pager.tongMuc()).isEqualTo(count);
        assertThat((List<DongDonAdmin>) model.get("donHang")).extracting(DongDonAdmin::maDonHang).containsExactly(ids);
        assertThat(((TongQuan) model.get("tongQuan")).tongSoDon()).isGreaterThanOrEqualTo(count);
    }

    private long tao(long owner, String ma, String method, String status, int tong) {
        return jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan,
                dia_chi_khi_dat, tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_tao)
            VALUES (?, ?, 'Hidden fixture recipient', '000', 'Fixture', ?, ?, ?, ?, '2026-01-01 12:00:00')
            RETURNING ma_don_hang
            """).params(ma, owner, tong, tong, status, method).query(Long.class).single();
    }

    private void lan(long don, String yeuCau, String gateway, String trangThai, int tien) {
        jdbc.sql("""
            INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai)
            VALUES (?, ?, ?, ?, ?)
            """).params(don, yeuCau, gateway, tien, trangThai).update();
    }

    private record GiaoDich(long id, String trangThai) {}
    private record Ngay(long id, String luc) {}
}
