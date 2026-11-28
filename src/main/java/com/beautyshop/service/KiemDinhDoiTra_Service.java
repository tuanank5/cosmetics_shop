package com.beautyshop.service;

import com.beautyshop.order.ReturnStatus;
import com.beautyshop.repository.DoiTra_Repository;
import com.beautyshop.service.SuKienQuanTri_Service.HanhDong;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(isolation = Isolation.READ_COMMITTED)
public class KiemDinhDoiTra_Service {
    private final JdbcClient jdbc;
    private final DoiTra_Repository doiTra;
    private final SuKienQuanTri_Service audit;

    KiemDinhDoiTra_Service(JdbcClient jdbc, DoiTra_Repository doiTra, SuKienQuanTri_Service audit) {
        this.jdbc = jdbc;
        this.doiTra = doiTra;
        this.audit = audit;
    }

    public record Lenh(long hoSo, UUID ma, long phienBan) {}
    public record DongNhan(long maKien, String thamChieu, long maDong, String ten, int nhan,
                           Integer duBan, Integer daMo, Integer hong, Integer khac, String lyDo, boolean daChot,
                           long nguoiNhan, LocalDateTime lucNhan, Long nguoiKiem, LocalDateTime lucKiem,
                           Long nguoiChot, LocalDateTime lucChot) {}
    public record Kiem(int duBan, int daMo, int hong, int khac, String lyDo, boolean datChecklist) {
        void kiemTra(int nhan) {
            can(duBan >= 0 && daMo >= 0 && hong >= 0 && khac >= 0
                && (long) duBan + daMo + hong + khac == nhan, "Tổng phân loại phải bằng lượng nhận trên dòng.");
            can(duBan == 0 || datChecklist, "Cần xác nhận đầy đủ checklist cho phần đủ bán.");
            can(lyDo != null && lyDo.length() <= 160 && (khac == 0 || !lyDo.isBlank()),
                "Ghi lý do cho lượng cách ly khác, tối đa 160 ký tự.");
        }
    }

    @Transactional(readOnly = true)
    public List<DongNhan> dongNhan(long hoSo) {
        return jdbc.sql("""
            SELECT k.ma_kien AS maKien,k.tham_chieu AS thamChieu,n.ma_dong_nhan AS maDong,
                CONCAT(ct.ten_san_pham_khi_dat,' · ',ct.ten_bien_the_khi_dat) AS ten,
                n.thuc_nhan AS nhan,q.du_ban AS duBan,q.da_mo AS daMo,q.hong,q.khac,
                q.ly_do_khac AS lyDo,k.ma_xac_nhan IS NOT NULL AS daChot,
                tn.ma_admin AS nguoiNhan,tn.luc_ghi AS lucNhan,tk.ma_admin AS nguoiKiem,tk.luc_ghi AS lucKiem,
                tx.ma_admin AS nguoiChot,tx.luc_ghi AS lucChot
            FROM chi_tiet_doi_tra d JOIN chi_tiet_don_hang ct USING(ma_chi_tiet_don_hang)
            JOIN dong_nhan_doi_tra n ON n.ma_dong_tra=d.ma_chi_tiet_doi_tra
            JOIN kien_doi_tra k USING(ma_kien)
            JOIN thao_tac_doi_tra tn ON tn.ma_thao_tac=k.ma_nhan
            LEFT JOIN ket_qua_kiem_doi_tra q ON q.ma_kiem=n.ma_kiem
            LEFT JOIN thao_tac_doi_tra tk ON tk.ma_thao_tac=q.ma_kiem
            LEFT JOIN thao_tac_doi_tra tx ON tx.ma_thao_tac=k.ma_xac_nhan
            WHERE d.ma_yeu_cau_doi_tra=? ORDER BY k.ma_kien,n.ma_dong_nhan
            """).param(hoSo).query(DongNhan.class).list();
    }

    public void duyet(Lenh lenh, ReturnStatus dich) {
        can(dich == ReturnStatus.APPROVED || dich == ReturnStatus.REJECTED,
            "Hãy dùng phiếu tiếp nhận hoặc chứng từ đóng hồ sơ.");
        ghi(lenh, dich == ReturnStatus.APPROVED ? HanhDong.RETURN_APPROVE : HanhDong.RETURN_REJECT,
            dich.name(), trangThai -> {
                chuyen(lenh.hoSo(), ReturnStatus.PENDING, dich);
                return new ThayDoi(trangThai, dich);
            });
    }

    public record KienLech(long ma, String thamChieu, int luongLech, LyDoLech lyDo, String chungTu, int khopBoSung) {}
    public enum LyDoLech {
        SAI_SKU("Sai SKU"), GUI_THUA("Gửi thừa"), CHUA_XAC_MINH("Chưa xác minh được hàng");
        private final String nhan;
        LyDoLech(String nhan) { this.nhan = nhan; }
        public String getNhan() { return nhan; }
    }

    @Transactional(readOnly = true)
    public List<KienLech> kienLech(long hoSo) {
        return jdbc.sql("""
            SELECT k.ma_kien AS ma,k.tham_chieu AS thamChieu,k.luong_lech AS luongLech,k.ly_do_lech AS lyDo,
                   k.chung_tu_lech AS chungTu,k.luong_khop_bo_sung AS khopBoSung
              FROM kien_doi_tra k JOIN thao_tac_doi_tra t ON t.ma_thao_tac=k.ma_nhan
             WHERE t.ma_yeu_cau=? AND k.luong_lech>0 ORDER BY k.ma_kien
            """).param(hoSo).query(KienLech.class).list();
    }

    public void nhan(Lenh lenh, String thamChieu, Map<Long, Integer> luongNhan, int lech, LyDoLech lyDo) {
        String kien = chungTu(thamChieu);
        var luong = luongDuong(luongNhan);
        can(lech >= 0 && (lech == 0 || lyDo != null), "Ghi số lượng cách ly và lý do sai lệch.");
        can(!luong.isEmpty() || lech > 0, "Chọn ít nhất một đơn vị thực nhận hoặc cách ly.");
        ghi(lenh, HanhDong.RETURN_RECEIVE, kien + "|" + luong + "|" + lech + "|" + lyDo, trangThai -> {
            can(trangThai == ReturnStatus.APPROVED || trangThai == ReturnStatus.RECEIVED, "Hồ sơ chưa thể nhận kiện.");
            can(jdbc.sql("SELECT COUNT(*) FROM kien_doi_tra WHERE tham_chieu=?").param(kien)
                .query(Integer.class).single() == 0, "Mã kiện đã được tiếp nhận.");
            long id = jdbc.sql("""
                INSERT INTO kien_doi_tra(tham_chieu,ma_nhan,luong_lech,ly_do_lech)
                VALUES (?,?,?,NULLIF(?,'')) RETURNING ma_kien
                """).params(kien,lenh.ma().toString(),lech,lech == 0 ? "" : lyDo.name()).query(Long.class).single();
            return ghiLuongKhop(lenh, trangThai, id, luong);
        });
    }

    public void doiChieu(Lenh lenh,long kien,String thamChieu, Map<Long,Integer> khop) {
        String chungTu = chungTu(thamChieu);
        var luong = luongDuong(khop);
        long tong = luong.values().stream().mapToLong(Integer::longValue).sum();
        ghi(lenh,HanhDong.RETURN_RECONCILE,kien + "|" + chungTu + "|" + luong, trangThai -> {
            can(trangThai == ReturnStatus.APPROVED || trangThai == ReturnStatus.RECEIVED, "Hồ sơ đã kết thúc.");
            var lech = kienLech(lenh.hoSo()).stream().filter(k -> k.ma() == kien && k.chungTu() == null)
                .findFirst().orElseThrow(() -> new IllegalStateException("Kiện không còn chờ đối chiếu."));
            can(tong <= lech.luongLech(), "Lượng nhận bổ sung vượt phần cách ly.");
            jdbc.sql("UPDATE kien_doi_tra SET ma_doi_chieu=?,chung_tu_lech=?,luong_khop_bo_sung=? WHERE ma_kien=?")
                .params(lenh.ma().toString(),chungTu,tong,kien).update();
            return ghiLuongKhop(lenh,trangThai,kien,luong);
        });
    }

    private static Map<Long,Integer> luongDuong(Map<Long,Integer> duLieu) {
        var luong = new TreeMap<>(duLieu);
        can(luong.values().stream().allMatch(n -> n != null && n >= 0), "Lượng nhận không hợp lệ.");
        luong.values().removeIf(n -> n == 0);
        return luong;
    }

    private ThayDoi ghiLuongKhop(Lenh lenh,ReturnStatus trangThai,long kien,Map<Long,Integer> luong) {
        var muc = doiTra.mucDaYeuCau(lenh.hoSo());
        int soDong = 0;
        for (var dong : muc) {
            Integer sl = luong.get(dong.ma());
            if (sl == null) continue;
            can(sl <= dong.chuaNhan(), "Lượng nhận vượt phần đã duyệt còn lại.");
            // Khi đối chiếu ra thêm hàng của dòng này phải kiểm lại toàn dòng trước xác nhận.
            jdbc.sql("""
                INSERT INTO dong_nhan_doi_tra(ma_kien,ma_dong_tra,thuc_nhan) VALUES (?,?,?)
                ON DUPLICATE KEY UPDATE thuc_nhan=thuc_nhan+VALUES(thuc_nhan),ma_kiem=NULL
                """).params(kien,dong.ma(),sl).update();
            soDong++;
        }
        can(soDong == luong.size(), "Dòng nhận không thuộc yêu cầu.");
        if (!luong.isEmpty() && trangThai == ReturnStatus.APPROVED) chuyen(lenh.hoSo(), trangThai, ReturnStatus.RECEIVED);
        long truoc = muc.stream().mapToLong(DoiTra_Repository.MucTra::thucNhan).sum();
        return new ThayDoi(truoc, truoc + luong.values().stream().mapToLong(Integer::longValue).sum());
    }

    public void kiem(Lenh lenh, long dong, Kiem k) {
        ghi(lenh, HanhDong.RETURN_INSPECT, dong + "/" + k, trangThai -> {
            can(trangThai == ReturnStatus.RECEIVED, "Hồ sơ chưa thể kiểm định.");
            var muc = dongNhan(lenh.hoSo()).stream().filter(d -> d.maDong() == dong && !d.daChot())
                .findFirst().orElseThrow(() -> new IllegalStateException("Dòng nhận không còn chờ xác nhận."));
            k.kiemTra(muc.nhan());
            can(!java.util.Objects.equals(muc.duBan(), k.duBan()) || !java.util.Objects.equals(muc.daMo(), k.daMo())
                || !java.util.Objects.equals(muc.hong(), k.hong()) || !java.util.Objects.equals(muc.khac(), k.khac())
                || !java.util.Objects.equals(muc.lyDo(), k.lyDo()), "Kết quả kiểm định chưa thay đổi.");
            jdbc.sql("""
                INSERT INTO ket_qua_kiem_doi_tra(ma_kiem,ma_dong_nhan,du_ban,da_mo,hong,khac,ly_do_khac,dat_checklist)
                VALUES (?,?,?,?,?,?,?,?)
                """).params(lenh.ma().toString(),dong,k.duBan(),k.daMo(),k.hong(),k.khac(),k.lyDo(),k.datChecklist()).update();
            jdbc.sql("""
                UPDATE dong_nhan_doi_tra SET ma_kiem=? WHERE ma_dong_nhan=?
                """).params(lenh.ma().toString(),dong).update();
            return new ThayDoi(muc.duBan(), k.duBan());
        });
    }

    private record DongKho(long dong, long sku, Integer duBan) {}

    public void xacNhan(Lenh lenh, long kien) {
        ghi(lenh, HanhDong.RETURN_CONFIRM, Long.toString(kien), trangThai -> {
            can(trangThai == ReturnStatus.RECEIVED, "Hồ sơ không còn chờ xác nhận.");
            var muc = jdbc.sql("""
                SELECT n.ma_dong_nhan AS dong,ct.ma_bien_the AS sku,q.du_ban AS duBan
                FROM dong_nhan_doi_tra n JOIN kien_doi_tra k USING(ma_kien)
                LEFT JOIN ket_qua_kiem_doi_tra q ON q.ma_kiem=n.ma_kiem
                JOIN chi_tiet_doi_tra d ON d.ma_chi_tiet_doi_tra=n.ma_dong_tra
                JOIN chi_tiet_don_hang ct USING(ma_chi_tiet_don_hang)
                WHERE d.ma_yeu_cau_doi_tra=? AND k.ma_kien=? AND k.ma_xac_nhan IS NULL
                AND (k.luong_lech=0 OR k.ma_doi_chieu IS NOT NULL)
                ORDER BY ct.ma_bien_the,n.ma_dong_nhan
                """).params(lenh.hoSo(),kien).query(DongKho.class).list();
            can(!muc.isEmpty(), "Kiện không thuộc hồ sơ, còn sai lệch hoặc đã xác nhận.");
            can(muc.stream().allMatch(d -> d.duBan() != null), "Còn dòng chưa kiểm định trong kiện.");
            for (var d : muc) {
                if (d.duBan() == 0) continue;
                boolean banDuoc = jdbc.sql("""
                    SELECT b.dang_hoat_dong AND s.dang_hoat_dong AND h.dang_hoat_dong AND dm.dang_hoat_dong
                    FROM bien_the_san_pham b JOIN san_pham s USING(ma_san_pham)
                    JOIN thuong_hieu h USING(ma_thuong_hieu) JOIN danh_muc dm USING(ma_danh_muc)
                    WHERE b.ma_bien_the=? FOR UPDATE
                    """).param(d.sku()).query(Boolean.class).single();
                can(banDuoc, "SKU ngừng bán. Hãy kiểm định lại phần cách ly.");
                jdbc.sql("INSERT INTO nhap_kho_doi_tra(ma_dong_nhan,ma_bien_the,so_luong,ma_xac_nhan) VALUES (?,?,?,?)")
                    .params(d.dong(),d.sku(),d.duBan(),lenh.ma().toString()).update();
                can(jdbc.sql("""
                    UPDATE bien_the_san_pham SET so_luong_trong_kho=so_luong_trong_kho+?
                    WHERE ma_bien_the=? AND so_luong_trong_kho<=2147483647-?
                    """).params(d.duBan(),d.sku(),d.duBan()).update() == 1, "Lượng nhập vượt giới hạn kho.");
            }
            jdbc.sql("UPDATE kien_doi_tra SET ma_xac_nhan=? WHERE ma_kien=?")
                .params(lenh.ma().toString(),kien).update();
            return new ThayDoi(0L, muc.stream().mapToLong(DongKho::duBan).sum());
        });
    }

    public void dong(Lenh lenh, String nghiaVu, String thieu) {
        String chungTu = chungTu(nghiaVu);
        String xuLyThieu = thieu == null || thieu.isBlank() ? "" : chungTu(thieu);
        ghi(lenh, HanhDong.RETURN_CLOSE, chungTu + "|" + xuLyThieu, trangThai -> {
            can(trangThai == ReturnStatus.RECEIVED, "Chỉ đóng hồ sơ đã thực nhận.");
            can(kienLech(lenh.hoSo()).stream().allMatch(k -> k.chungTu() != null), "Còn kiện cách ly chờ đối chiếu.");
            var muc = doiTra.mucDaYeuCau(lenh.hoSo());
            can(!muc.isEmpty() && muc.stream().allMatch(d -> d.choKiem() == 0), "Còn phần chờ kiểm hoặc xác nhận.");
            can(muc.stream().allMatch(d -> d.chuaNhan() == 0) || !xuLyThieu.isEmpty(),
                "Cần chứng từ giải quyết cuối cho toàn bộ phần thiếu.");
            can(jdbc.sql("""
                SELECT COUNT(*) FROM yeu_cau_doi_tra y JOIN don_hang d USING(ma_don_hang)
                WHERE y.ma_yeu_cau_doi_tra=? AND (
                    d.trang_thai <> 'DELIVERED' OR NOT EXISTS (
                        SELECT 1 FROM lan_thanh_toan t WHERE t.ma_don_hang=d.ma_don_hang
                        AND t.trang_thai IN ('PAID','REFUNDED') AND t.so_tien=d.tong_thanh_toan
                        AND ((d.phuong_thuc_thanh_toan='COD' AND t.cong_thanh_toan='COD')
                        OR (d.phuong_thuc_thanh_toan='MOMO' AND t.cong_thanh_toan IN ('MOMO','MOMO_MOCK'))))
                    OR EXISTS (SELECT 1 FROM lan_thanh_toan t WHERE t.ma_don_hang=d.ma_don_hang
                        AND t.trang_thai IN ('PENDING','REFUND_PENDING'))
                    OR EXISTS (SELECT 1 FROM tam_giu_phieu_giam_gia g WHERE g.ma_don_hang=d.ma_don_hang AND g.trang_thai='HELD')
                    OR EXISTS (SELECT 1 FROM tam_giu_ton_kho g WHERE g.ma_don_hang=d.ma_don_hang AND g.trang_thai='HELD'))
                """).param(lenh.hoSo()).query(Integer.class).single() == 0, "Đơn còn nghĩa vụ thanh toán hoặc đối soát.");
            jdbc.sql("""
                INSERT INTO ket_thuc_doi_tra(ma_yeu_cau,ma_dong,chung_tu_nghia_vu,chung_tu_thieu)
                VALUES (?,?,?,NULLIF(?,''))
                """).params(lenh.hoSo(),lenh.ma().toString(),chungTu,xuLyThieu).update();
            chuyen(lenh.hoSo(),trangThai,ReturnStatus.COMPLETED);
            return new ThayDoi(trangThai, ReturnStatus.COMPLETED);
        });
    }

    private void ghi(Lenh lenh, HanhDong action, String noiDung, Function<ReturnStatus, ThayDoi> viec) {
        var phien = SuKienQuanTri_Service.phienAdmin();
        long admin = phien.getMaNguoiDung();
        if (jdbc.sql("""
            SELECT COUNT(*) FROM nguoi_dung WHERE ma_nguoi_dung=? AND kich_hoat=1 AND vai_tro='ADMIN'
            AND phien_ban_xac_thuc=? FOR UPDATE
            """).params(admin,phien.getPhienBanXacThuc()).query(Integer.class).single() != 1) {
            throw new AccessDeniedException("Phiên Admin không còn hiệu lực.");
        }
        var don = jdbc.sql("SELECT ma_don_hang FROM yeu_cau_doi_tra WHERE ma_yeu_cau_doi_tra=?")
            .param(lenh.hoSo()).query(Long.class).optional().orElseThrow(() -> new IllegalStateException("Hồ sơ không tồn tại."));
        jdbc.sql("SELECT ma_don_hang FROM don_hang WHERE ma_don_hang=? FOR UPDATE").param(don).query(Long.class).single();
        var hoSo = jdbc.sql("SELECT trang_thai,phien_ban_kiem FROM yeu_cau_doi_tra WHERE ma_yeu_cau_doi_tra=? FOR UPDATE")
            .param(lenh.hoSo()).query((rs,n) -> new HoSo(ReturnStatus.valueOf(rs.getString(1)),rs.getObject(2,Long.class))).single();
        String id = lenh.ma().toString();
        String hash = bam(lenh.phienBan() + "/" + noiDung);
        var cu = jdbc.sql("SELECT ma_yeu_cau=?,ma_admin=?,hanh_dong=?,dau_vao=? FROM thao_tac_doi_tra WHERE ma_thao_tac=?")
            .params(lenh.hoSo(),admin,action.name(),hash,id)
            .query((rs,n) -> rs.getBoolean(1) && rs.getBoolean(2) && rs.getBoolean(3) && rs.getBoolean(4)).optional();
        if (cu.isPresent()) {
            can(cu.get(), "Mã thao tác đã dùng cho dữ liệu khác.");
            return;
        }
        can(jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri WHERE correlation_id=?").param(id)
            .query(Integer.class).single()==0, "Thao tác trước đã kết thúc. Hãy tải lại hồ sơ.");
        var sk = audit.batDau(action,"RETURN",lenh.hoSo(),lenh.ma());
        // A legacy PENDING request has never been approved for receipt. Its first decision starts tracking.
        boolean khoiTao = hoSo.phienBan() == null && hoSo.trangThai() == ReturnStatus.PENDING
            && (action == HanhDong.RETURN_APPROVE || action == HanhDong.RETURN_REJECT) && lenh.phienBan() == 0;
        can(khoiTao || hoSo.phienBan() != null && hoSo.phienBan() == lenh.phienBan(),
            "Hồ sơ cũ thiếu chứng cứ hoặc dữ liệu đã thay đổi. Hãy tải lại.");
        jdbc.sql("""
            INSERT INTO thao_tac_doi_tra(ma_thao_tac,ma_yeu_cau,ma_admin,hanh_dong,dau_vao,luc_ghi)
            VALUES (?,?,?,?,?,UTC_TIMESTAMP(6))
            """).params(id,lenh.hoSo(),admin,action.name(),hash).update();
        var thayDoi = viec.apply(hoSo.trangThai());
        jdbc.sql("UPDATE yeu_cau_doi_tra SET phien_ban_kiem=COALESCE(phien_ban_kiem,0)+1 WHERE ma_yeu_cau_doi_tra=?")
            .param(lenh.hoSo()).update();
        audit.thanhCong(sk,lenh.hoSo(),thayDoi.truoc(),thayDoi.sau());
    }

    private record ThayDoi(Object truoc, Object sau) {}
    private record HoSo(ReturnStatus trangThai, Long phienBan) {}

    private void chuyen(long id, ReturnStatus tu, ReturnStatus den) {
        can(doiTra.chuyen(id,tu,den), "Trạng thái hồ sơ đã thay đổi.");
    }

    private static String chungTu(String s) {
        can(s != null && s.strip().matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,79}"), "Mã chứng từ không hợp lệ.");
        return s.strip().toUpperCase(Locale.ROOT);
    }

    private static String bam(String s) {
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void can(boolean dieuKien, String loi) {
        if (!dieuKien) throw new IllegalStateException(loi);
    }
}
