package com.beautyshop.repository;

import com.beautyshop.domain.catalog.NhomDanhMucSanPhamThuongHieu;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DonHangCatalog_Repository {
    private final JdbcClient jdbc;

    DonHangCatalog_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    // Column names come only from these internal constants.
    public enum PhamVi {
        BIEN_THE("b.ma_bien_the"), SAN_PHAM("b.ma_san_pham"),
        DANH_MUC("s.ma_danh_muc"), THUONG_HIEU("s.ma_thuong_hieu");

        private final String cot;

        PhamVi(String cot) { this.cot = cot; }

        public static PhamVi tu(NhomDanhMucSanPhamThuongHieu nhom) {
            return switch (nhom) {
                case SAN_PHAM -> SAN_PHAM;
                case DANH_MUC -> DANH_MUC;
                case THUONG_HIEU -> THUONG_HIEU;
            };
        }
    }

    public Map<Long, Long> dem(PhamVi phamVi, Long ma) {
        return dem(phamVi, phamVi, ma);
    }

    public Map<Long, Long> dem(PhamVi phamVi, PhamVi loc, Long ma) {
        String phamViLoc = ma == null ? "" : " AND " + loc.cot + " = :ma";
        var truyVan = jdbc.sql("""
            SELECT ma_muc, COUNT(DISTINCT ma_don) AS so_don FROM (
                SELECT %1$s AS ma_muc, d.ma_don_hang AS ma_don
                FROM bien_the_san_pham b
                JOIN san_pham s ON s.ma_san_pham = b.ma_san_pham
                JOIN chi_tiet_don_hang c ON c.ma_bien_the = b.ma_bien_the
                JOIN don_hang d ON d.ma_don_hang = c.ma_don_hang
                WHERE (
                    d.trang_thai NOT IN ('DELIVERED', 'CANCELLED', 'EXPIRED')
                    OR EXISTS (
                        SELECT 1 FROM tam_giu_phieu_giam_gia g
                        WHERE g.ma_don_hang = d.ma_don_hang AND g.trang_thai = 'HELD'
                    )
                    OR EXISTS (
                        SELECT 1 FROM lan_thanh_toan t
                        WHERE t.ma_don_hang = d.ma_don_hang AND (
                            t.trang_thai IN ('PENDING', 'REFUND_PENDING')
                            OR (d.trang_thai IN ('CANCELLED', 'EXPIRED') AND t.trang_thai = 'PAID')
                        )
                    )
                    OR (d.trang_thai = 'DELIVERED' AND NOT EXISTS (
                        SELECT 1 FROM lan_thanh_toan t
                        WHERE t.ma_don_hang = d.ma_don_hang AND t.trang_thai = 'PAID'
                          AND t.so_tien = d.tong_thanh_toan
                          AND ((d.phuong_thuc_thanh_toan = 'COD' AND t.cong_thanh_toan = 'COD')
                            OR (d.phuong_thuc_thanh_toan = 'MOMO' AND t.cong_thanh_toan IN ('MOMO', 'MOMO_MOCK')))
                    ))
                    OR EXISTS (
                        SELECT 1 FROM chi_tiet_doi_tra ct
                        JOIN yeu_cau_doi_tra dt ON dt.ma_yeu_cau_doi_tra = ct.ma_yeu_cau_doi_tra
                        WHERE ct.ma_chi_tiet_don_hang = c.ma_chi_tiet_don_hang
                          AND dt.trang_thai IN ('PENDING', 'APPROVED', 'RECEIVED')
                    )
                ) %2$s
                UNION ALL
                SELECT %1$s, g.ma_don_hang
                FROM tam_giu_ton_kho g
                JOIN bien_the_san_pham b ON b.ma_bien_the = g.ma_bien_the
                JOIN san_pham s ON s.ma_san_pham = b.ma_san_pham
                WHERE g.trang_thai = 'HELD' %2$s
            ) don_lien_quan GROUP BY ma_muc
            """.formatted(phamVi.cot, phamViLoc));
        if (ma != null) truyVan.param("ma", ma);
        return truyVan.query((rs, row) -> Map.entry(rs.getLong("ma_muc"), rs.getLong("so_don"))).list()
            .stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
