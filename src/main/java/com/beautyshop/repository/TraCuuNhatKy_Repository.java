package com.beautyshop.repository;

import com.beautyshop.dto.PhanTrang;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
@PreAuthorize("hasRole('ADMIN')")
public class TraCuuNhatKy_Repository {
    private final JdbcClient jdbc;
    public TraCuuNhatKy_Repository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public record Dong(long id, Long actor, String role, String action, String loai, long target,
                       LocalDateTime utc, String result, String reason, String correlation, String truoc, String sau) {}
    private static final String SELECT = """
        SELECT ma_nhat_ky AS id,ma_nguoi_thuc_hien AS actor,vai_tro_nguoi_thuc_hien AS role,
        hanh_dong AS action,loai_doi_tuong AS loai,ma_doi_tuong AS target,thoi_gian AS utc,
        ket_qua AS result,ly_do AS reason,correlation_id AS correlation,truoc,sau FROM nhat_ky_quan_tri
        """;

    public Dong chiTiet(long id) {
        return jdbc.sql(SELECT + " WHERE ma_nhat_ky=?").param(id).query(Dong.class).optional()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    static String thuTu(String sort) {
        return "oldest".equals(sort) ? "thoi_gian,ma_nhat_ky" : "thoi_gian DESC,ma_nhat_ky DESC";
    }

    public Page<Dong> trang(Map<String,String> loc, int page) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String,Object> params = new LinkedHashMap<>();
        if (loc.containsKey("q")) {
            where.append(" AND (LOCATE(:q,hanh_dong)>0 OR CAST(ma_doi_tuong AS CHAR)=:q OR CAST(ma_nhat_ky AS CHAR)=:q)");
            params.put("q",loc.get("q"));
        }
        for (var f : Map.of("actor","ma_nguoi_thuc_hien","type","loai_doi_tuong","result","ket_qua").entrySet()) {
            if (loc.containsKey(f.getKey())) {
                where.append(" AND ").append(f.getValue()).append("=:").append(f.getKey());
                params.put(f.getKey(),loc.get(f.getKey()));
            }
        }
        if (loc.containsKey("date")) {
            var ngay = LocalDate.parse(loc.get("date")).atStartOfDay();
            where.append(" AND thoi_gian>=:dau AND thoi_gian<=:cuoi");
            params.put("dau",ngay);
            params.put("cuoi",ngay.with(java.time.LocalTime.of(23,59,59,999999000)));
        }
        String order = thuTu(loc.get("sort"));
        long total = jdbc.sql("SELECT COUNT(*) FROM nhat_ky_quan_tri" + where).params(params).query(Long.class).single();
        return PhanTrang.doc(PhanTrang.yeuCau(page,20), p -> {
            var rows = jdbc.sql(SELECT + where + " ORDER BY " + order + " LIMIT 20 OFFSET :offset")
                .params(params).param("offset",p.getOffset()).query(Dong.class).list();
            return new PageImpl<>(rows,p,total);
        });
    }
}
