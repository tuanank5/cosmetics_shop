package com.beautyshop.integration;

import com.beautyshop.recommendation.DiemPhoBien_Policy;
import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import com.beautyshop.service.GoiYPhoBien_Service;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P21 QA, chỉ đọc: in bảng điểm phổ biến trên database demo (DB_URL trỏ tới beautyshop_demo) để ghi vào báo cáo
 * và đối chiếu thứ tự với đúng danh sách trang chủ đang dùng. Bật bằng P21_DEMO_REPORT=true.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "P21_DEMO_REPORT", matches = "true")
class GoiYPhoBienDemoReport_Test {
    @Autowired GoiYPhoBien_Service goiYService;
    @Autowired JdbcClient jdbc;

    @Test
    void inBangDiemTop5() {
        DiemPhoBien_Policy.KetQua kq = goiYService.xepHang();
        List<TheSanPham> trangChu = goiYService.goiY(null, 5);
        StringBuilder out = new StringBuilder("P21-DEMO-REPORT ung vien=" + kq.xepHang().size() + ", coDuLieu=" + kq.coDuLieu()
            + ", m=" + kq.diemTrungBinhCuaHang() + "\n");
        out.append("P21-DEMO-REPORT | # | ma | duong_dan | da_ban | chuan_hoa_ban | so_dg | tong_sao | bayes | dg_0_1 | yeu_thich | chuan_hoa_yt | diem |\n");
        int i = 0;
        for (var d : kq.xepHang().subList(0, Math.min(5, kq.xepHang().size()))) {
            var u = d.ungVien();
            String slug = jdbc.sql("SELECT duong_dan FROM san_pham WHERE ma_san_pham = ?").param(u.maSanPham()).query(String.class).single();
            out.append(String.format(Locale.ROOT, "P21-DEMO-REPORT | %d | %d | %s | %d | %.4f | %d | %d | %s | %.4f | %d | %.4f | %.4f |%n",
                ++i, u.maSanPham(), slug, u.soLuongDaBan(), d.daBanChuanHoa(), u.soDanhGia(), u.tongSao(),
                d.bayes() == null ? "-" : String.format(Locale.ROOT, "%.4f", d.bayes()), d.danhGiaChuanHoa(),
                u.soLuotYeuThich(), d.yeuThichChuanHoa(), d.diem()));
        }
        out.append("P21-DEMO-REPORT trang chu: ").append(trangChu.stream().map(TheSanPham::getDuongDan).toList());
        System.out.println(out);
        assertThat(trangChu).hasSize(5);
    }
}
