package com.beautyshop.integration;

import com.beautyshop.cart.GioKhach;
import com.beautyshop.dto.DiaChiNguoiDung_Form;
import com.beautyshop.model.NguoiDung.VaiTro;
import com.beautyshop.repository.NguoiDung_Repository;
import com.beautyshop.repository.DiaChiNguoiDung_Repository;
import com.beautyshop.service.DiaChiNguoiDung_Service;
import com.beautyshop.service.GioHang_Service;
import com.beautyshop.service.QuyenDonHang_Service;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "P01_MARIADB_TEST", matches = "true")
class XacThuc_MariaDb_Test {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.beautyshop.service.EmailVerification_Service otp;
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired NguoiDung_Repository nguoiDungRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired GioHang_Service gioHang;
    @Autowired QuyenDonHang_Service quyenDonHang;
    @Autowired DiaChiNguoiDung_Service diaChiService;
    @Autowired DiaChiNguoiDung_Repository diaChiRepository;

    private final String matKhau = UUID.randomUUID().toString();

    @Test
    void dangKyBcryptEmailDuyNhatVaKhongNhanVaiTroTuClient() throws Exception {
        String email = email();
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(post("/register")).andExpect(status().isForbidden());
        mvc.perform(post("/register").with(csrf()).param("email", "sai")
                .param("hoTen", " ").param("matKhau", "ế".repeat(25)))
            .andExpect(model().attributeHasFieldErrors("form", "email", "hoTen", "doDaiBcryptHopLe"))
            .andExpect(content().string(not(containsString("ế".repeat(25)))));
        dangKy(email.toUpperCase(java.util.Locale.ROOT));
        var nguoiDung = nguoiDungRepository.findByEmail(email).orElseThrow();
        assertThat(nguoiDung.getEmail()).isEqualTo(email);
        assertThat(nguoiDung.getVaiTro()).isEqualTo(VaiTro.CUSTOMER);
        assertThat(nguoiDung.getMatKhauBam()).isNotEqualTo(matKhau);
        assertThat(passwordEncoder.matches(matKhau, nguoiDung.getMatKhauBam())).isTrue();
        mvc.perform(post("/register").with(csrf()).param("email", email).param("hoTen", "Khách thử")
                .param("matKhau", matKhau))
            .andExpect(model().attributeHasFieldErrors("form", "email"))
            .andExpect(content().string(not(containsString(matKhau))));
        mvc.perform(post("/login").with(csrf()).param("username", email).param("password", "a".repeat(73)))
            .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        jdbc.sql("UPDATE nguoi_dung SET kich_hoat = FALSE WHERE email = ?").param(email).update();
        mvc.perform(post("/login").with(csrf()).param("username", email).param("password", matKhau))
            .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
    }

    @Test
    void customerVaAdminDangNhapDungQuyenVaLogoutCoCsrf() throws Exception {
        String customer = email();
        String admin = email();
        dangKy(customer);
        dangKy(admin);
        jdbc.sql("UPDATE nguoi_dung SET vai_tro = 'ADMIN' WHERE email = ?").param(admin).update();

        var guest = new MockHttpSession();
        String idGuest = guest.getId();
        mvc.perform(get("/account").session(guest)).andExpect(redirectedUrl("http://localhost/login"));
        var session = dangNhap(customer, guest, "CUSTOMER", "http://localhost/account?continue");
        assertThat(session.getId()).isNotEqualTo(idGuest);
        mvc.perform(get("/account").session(session).param("userId", "999"))
            .andExpect(status().isOk()).andExpect(content().string(containsString(customer)));
        mvc.perform(get("/admin/catalog").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf()))
            .andExpect(redirectedUrl("/?logout")).andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();

        var adminSession = dangNhap(admin, new MockHttpSession(), "ADMIN", "/admin/dashboard"); // P20: Admin vào Dashboard
        mvc.perform(get("/admin/catalog/products").session(adminSession)).andExpect(status().isOk());
        mvc.perform(get("/account").session(adminSession)).andExpect(status().isForbidden());
        mvc.perform(get("/checkout").session(adminSession)).andExpect(status().isForbidden());
        mvc.perform(get("/orders").session(adminSession)).andExpect(status().isForbidden());
        mvc.perform(post("/payment/momo/ipn")).andExpect(status().isForbidden());
        mvc.perform(post("/cart/items").param("variant", "1").session(adminSession).with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    void gioGuestGopMotLanVaKiemTraTonLai() throws Exception {
        String email = email();
        dangKy(email);
        long maNguoiDung = nguoiDungRepository.findByEmail(email).orElseThrow().getMaNguoiDung();
        long maBienThe = taoSku();
        var guest = new MockHttpSession();
        mvc.perform(get("/cart").session(guest)).andExpect(status().isOk());
        mvc.perform(post("/cart/items").session(guest).param("variant", Long.toString(maBienThe)))
            .andExpect(status().isForbidden());
        gioHang.themGioCustomer(maNguoiDung, maBienThe);
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/cart/items").session(guest).with(csrf()).param("variant", Long.toString(maBienThe)))
                .andExpect(redirectedUrl("/cart"));
        }
        var banGioGuest = GioKhach.doc(guest);
        assertThat(banGioGuest.soLuong().get(maBienThe)).isEqualTo(2);
        jdbc.sql("UPDATE bien_the_san_pham SET so_luong_trong_kho = 2, gia_ban = 125000 WHERE ma_bien_the = ?")
            .param(maBienThe).update();
        var session = dangNhap(email, guest, "CUSTOMER", "/");
        assertThat(GioKhach.doc(session).soLuong()).isEmpty();
        mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("điều chỉnh theo tồn hiện tại")))
            .andExpect(content().string(containsString("125.000")));
        gioHang.gopSauDangNhap(maNguoiDung, banGioGuest);
        jdbc.sql("UPDATE bien_the_san_pham SET so_luong_trong_kho = 10 WHERE ma_bien_the = ?").param(maBienThe).update();
        gioHang.gopSauDangNhap(maNguoiDung, banGioGuest);
        assertThat(gioHang.docGioCustomer(maNguoiDung).getFirst().soLuong()).isEqualTo(2);
        assertThat(jdbc.sql("SELECT count(*) FROM ghi_nhan_gop_gio_hang WHERE ma_gop = ?")
            .param(banGioGuest.maGop().toString()).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void haiRequestGopCungSnapshotKhongCongHaiLan() throws Exception {
        String email = email();
        dangKy(email);
        long maNguoiDung = nguoiDungRepository.findByEmail(email).orElseThrow().getMaNguoiDung();
        long maBienThe = taoSku();
        GioKhach gio = GioKhach.doc(null).them(maBienThe, 1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> gioHang.gopSauDangNhap(maNguoiDung, gio));
            var b = executor.submit(() -> gioHang.gopSauDangNhap(maNguoiDung, gio));
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertThat(gioHang.docGioCustomer(maNguoiDung).getFirst().soLuong()).isEqualTo(1);
    }

    @Test
    void serviceChiChoPhepChuDon() throws Exception {
        String chuDon = email();
        String khachKhac = email();
        dangKy(chuDon);
        dangKy(khachKhac);
        long id = nguoiDungRepository.findByEmail(chuDon).orElseThrow().getMaNguoiDung();
        long maDon = jdbc.sql("""
            INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan,
             dia_chi_khi_dat, tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan)
            VALUES (?, ?, 'Test', '0000000000', 'Test', 0, 0, 'PENDING_CONFIRMATION', 'COD') RETURNING ma_don_hang
            """).params(UUID.randomUUID().toString(), id).query(Long.class).single();
        var sessionChuDon = dangNhap(chuDon, new MockHttpSession(), "CUSTOMER", "/");
        var sessionKhac = dangNhap(khachKhac, new MockHttpSession(), "CUSTOMER", "/");
        try {
            SecurityContextHolder.setContext(context(sessionChuDon));
            quyenDonHang.kiemTraSoHuu(maDon);
            SecurityContextHolder.setContext(context(sessionKhac));
            assertThatThrownBy(() -> quyenDonHang.kiemTraSoHuu(maDon)).isInstanceOfSatisfying(
                ResponseStatusException.class, ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private SecurityContext context(MockHttpSession session) {
        return (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }

    @Test
    void hoSoVaDiaChiChiChoPhepChuTaiKhoan() throws Exception {
        String chu = email();
        String khac = email();
        dangKy(chu);
        dangKy(khac);
        long maChu = nguoiDungRepository.findByEmail(chu).orElseThrow().getMaNguoiDung();
        long maKhac = nguoiDungRepository.findByEmail(khac).orElseThrow().getMaNguoiDung();
        var phienChu = dangNhap(chu, new MockHttpSession(), "CUSTOMER", "/");
        var phienKhac = dangNhap(khac, new MockHttpSession(), "CUSTOMER", "/");
        mvc.perform(post("/account").session(phienChu).with(csrf()).param("hoTen", " ")
                .param("soDienThoai", "sai"))
            .andExpect(model().attributeHasFieldErrors("hoSo", "hoTen", "soDienThoai"));
        mvc.perform(post("/account").session(phienChu).with(csrf()).param("hoTen", "Nguyễn Chủ")
                .param("soDienThoai", "0901234567").param("maNguoiDung", Long.toString(maKhac))
                .param("vaiTro", "ADMIN").param("email", khac))
            .andExpect(redirectedUrl("/account"));
        var hoSo = nguoiDungRepository.findById(maChu).orElseThrow();
        assertThat(hoSo.getHoTen()).isEqualTo("Nguyễn Chủ");
        assertThat(hoSo.getSoDienThoai()).isEqualTo("0901234567");
        assertThat(hoSo.getVaiTro()).isEqualTo(VaiTro.CUSTOMER);
        assertThat(hoSo.getEmail()).isEqualTo(chu);
        assertThat(nguoiDungRepository.findById(maKhac).orElseThrow().getHoTen()).isEqualTo("Khách thử");

        String base = "/account/addresses";
        mvc.perform(get(base + "/new").session(phienChu)).andExpect(status().isOk());
        mvc.perform(get(base + "/new").session(phienChu).param("from", "checkout"))
            .andExpect(model().attribute("addressBack", "/checkout"))
            .andExpect(content().string(containsString("name=\"from\" value=\"checkout\"")));
        mvc.perform(get(base + "/new").session(phienChu).param("from", "//other.invalid"))
            .andExpect(model().attribute("addressBack", base));
        mvc.perform(post(base + "/new").session(phienChu).with(csrf()).param("from", "checkout").param("tenNguoiNhan", "Fixture"))
            .andExpect(model().attributeHasFieldErrors("form", "soDienThoai", "diaChiChiTiet"))
            .andExpect(model().attribute("addressBack", "/checkout"))
            .andExpect(content().string(containsString("value=\"Fixture\"")));
        mvc.perform(post(base + "/new").session(phienChu).with(csrf()).param("soDienThoai", "sai"))
            .andExpect(model().attributeHasFieldErrors("form", "tenNguoiNhan", "soDienThoai", "diaChiChiTiet"));
        mvc.perform(diaChi(base + "/new", phienChu, "Địa chỉ A"))
            .andExpect(redirectedUrl(base));
        mvc.perform(diaChi(base + "/new", phienChu, "Địa chỉ B").param("laMacDinh", "true")
                .param("from", "checkout")
                .param("maNguoiDung", Long.toString(maKhac)))
            .andExpect(redirectedUrl("/checkout"));
        var danhSach = diaChiRepository.findAllByNguoiDungMaNguoiDungOrderByMaDiaChiAsc(maChu);
        assertThat(danhSach).hasSize(2);
        long maA = danhSach.getFirst().getMaDiaChi();
        long maB = danhSach.getLast().getMaDiaChi();
        assertThat(danhSach.getFirst().isLaMacDinh()).isFalse();
        assertThat(danhSach.getLast().isLaMacDinh()).isTrue();
        mvc.perform(get(base).session(phienKhac)).andExpect(status().isOk())
            .andExpect(content().string(not(containsString("Địa chỉ B"))));
        mvc.perform(get(base + "/" + maB + "/edit").session(phienKhac)).andExpect(status().isNotFound());
        mvc.perform(diaChi(base + "/" + maB + "/edit", phienKhac, "Không hợp lệ").param("from", "checkout"))
            .andExpect(status().isNotFound());
        mvc.perform(post(base + "/" + maB + "/delete").session(phienKhac).with(csrf()))
            .andExpect(status().isNotFound());
        mvc.perform(post(base + "/" + maB + "/delete").session(phienChu)).andExpect(status().isForbidden());
        mvc.perform(diaChi(base + "/" + maA + "/edit", phienChu, "Địa chỉ A mới").param("from", "checkout"))
            .andExpect(redirectedUrl("/checkout"));
        mvc.perform(post(base + "/" + maB + "/delete").session(phienChu).with(csrf()))
            .andExpect(redirectedUrl(base));
        var conLai = diaChiRepository.findAllByNguoiDungMaNguoiDungOrderByMaDiaChiAsc(maChu);
        assertThat(conLai).hasSize(1);
        assertThat(conLai.getFirst().isLaMacDinh()).isTrue();
        assertThat(conLai.getFirst().getDiaChiChiTiet()).isEqualTo("Địa chỉ A mới");
    }

    @Test
    void adminTimPhanTrangKhoaMoVaThuHoiPhienCu() throws Exception {
        String admin = email();
        String customer = email();
        dangKy(admin);
        dangKy(customer);
        long maAdmin = nguoiDungRepository.findByEmail(admin).orElseThrow().getMaNguoiDung();
        var mucTieu = nguoiDungRepository.findByEmail(customer).orElseThrow();
        String q = "Đợt " + UUID.randomUUID();
        jdbc.sql("UPDATE nguoi_dung SET vai_tro = 'ADMIN' WHERE ma_nguoi_dung = ?").param(maAdmin).update();
        jdbc.sql("UPDATE nguoi_dung SET ho_ten = ? WHERE ma_nguoi_dung = ?")
            .params(q, mucTieu.getMaNguoiDung()).update();
        for (int i = 0; i < 15; i++) {
            jdbc.sql("INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, kich_hoat) VALUES (?, '', ?, FALSE)")
                .params(email(), q).update();
        }
        var phienAdmin = dangNhap(admin, new MockHttpSession(), "ADMIN", "/admin/dashboard"); // P20: Admin vào Dashboard
        var phienCustomer = dangNhap(customer, new MockHttpSession(), "CUSTOMER", "/");
        var phienChuaDung = dangNhap(customer, new MockHttpSession(), "CUSTOMER", "/");
        mvc.perform(get("/admin/users").session(phienCustomer)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users").session(phienAdmin).param("q", q).param("page", "1"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Trang 2 / 2")))
            .andExpect(content().string(containsString(customer)))
            .andExpect(content().string(not(containsString(mucTieu.getMatKhauBam()))));
        mvc.perform(get("/admin/users").session(phienAdmin).param("q", customer).param("page", "99"))
            .andExpect(status().isOk()).andExpect(content().string(containsString(customer)))
            .andExpect(content().string(not(containsString("Phân trang tài khoản"))));
        String url = "/admin/users/" + mucTieu.getMaNguoiDung() + "/status";
        mvc.perform(post(url).session(phienCustomer).with(csrf()).param("enabled", "false"))
            .andExpect(status().isForbidden());
        mvc.perform(post(url).session(phienAdmin).param("enabled", "false")).andExpect(status().isForbidden());
        mvc.perform(post(url).session(phienAdmin).with(csrf()).param("enabled", "false").param("vaiTro", "ADMIN"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("message"));
        mvc.perform(get("/account").session(phienCustomer)).andExpect(redirectedUrl("/login?inactive"));
        assertThat(phienCustomer.isInvalid()).isTrue();
        mvc.perform(post("/login").with(csrf()).param("username", customer).param("password", matKhau))
            .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        mvc.perform(post("/admin/users/" + maAdmin + "/status").session(phienAdmin).with(csrf()).param("enabled", "false"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("error"));
        mvc.perform(post(url).session(phienAdmin).with(csrf()).param("enabled", "true"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("message"));
        mvc.perform(get("/account").session(phienChuaDung)).andExpect(redirectedUrl("/login?inactive"));
        assertThat(phienChuaDung.isInvalid()).isTrue();
        dangNhap(customer, new MockHttpSession(), "CUSTOMER", "/");
    }

    @Test
    void luuDiaChiSongSongChiCoMotMacDinh() throws Exception {
        String email = email();
        dangKy(email);
        long id = nguoiDungRepository.findByEmail(email).orElseThrow().getMaNguoiDung();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var a = executor.submit(() -> diaChiService.luu(id, null, diaChiMacDinh("A")));
            var b = executor.submit(() -> diaChiService.luu(id, null, diaChiMacDinh("B")));
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        var danhSach = diaChiRepository.findAllByNguoiDungMaNguoiDungOrderByMaDiaChiAsc(id);
        assertThat(danhSach).hasSize(2);
        assertThat(danhSach.stream().filter(diaChi -> diaChi.isLaMacDinh()).count()).isEqualTo(1);
    }

    private DiaChiNguoiDung_Form diaChiMacDinh(String ten) {
        var form = new DiaChiNguoiDung_Form();
        form.setTenNguoiNhan(ten);
        form.setSoDienThoai("0901234567");
        form.setDiaChiChiTiet("Địa chỉ " + ten);
        form.setLaMacDinh(true);
        return form;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder diaChi(
            String url, MockHttpSession session, String diaChi) {
        return post(url).session(session).with(csrf()).param("tenNguoiNhan", "Người nhận")
            .param("soDienThoai", "+84901234567").param("diaChiChiTiet", diaChi);
    }

    private MockHttpSession dangNhap(String email, MockHttpSession session, String role, String dich) throws Exception {
        return (MockHttpSession) mvc.perform(post("/login").session(session).with(csrf())
                .param("username", email).param("password", matKhau))
            .andExpect(redirectedUrl(dich)).andExpect(authenticated().withRoles(role))
            .andReturn().getRequest().getSession(false);
    }

    private void dangKy(String email) throws Exception {
        DangKyOtp_TestSupport.dangKy(mvc, otp, email, matKhau);
    }

    private String email() {
        return UUID.randomUUID() + "@example.invalid";
    }

    private long taoSku() {
        String suffix = UUID.randomUUID().toString();
        long danhMuc = jdbc.sql("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ('Test', ?) RETURNING ma_danh_muc")
            .param(suffix).query(Long.class).single();
        long thuongHieu = jdbc.sql("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ('Test', ?) RETURNING ma_thuong_hieu")
            .param(suffix).query(Long.class).single();
        long sanPham = jdbc.sql("""
            INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan)
            VALUES (?, ?, 'Test', ?) RETURNING ma_san_pham
            """).params(danhMuc, thuongHieu, suffix).query(Long.class).single();
        return jdbc.sql("""
            INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho)
            VALUES (?, ?, 'Test', 100000, 3) RETURNING ma_bien_the
            """).params(sanPham, suffix).query(Long.class).single();
    }
}
