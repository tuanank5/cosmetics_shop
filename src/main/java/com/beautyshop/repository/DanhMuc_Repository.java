package com.beautyshop.repository;

import com.beautyshop.dto.MucCatalogAdmin;
import com.beautyshop.model.DanhMuc;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface DanhMuc_Repository extends DanhMucSanPhamThuongHieu_Repository<DanhMuc> {
    List<DanhMuc> findAllByDangHoatDongTrueOrderByTenDanhMucAsc();

    @Query("""
        select new com.beautyshop.dto.MucCatalogAdmin(d.maDanhMuc, d.tenDanhMuc, d.duongDan, d.dangHoatDong)
        from DanhMuc d order by d.tenDanhMuc, d.maDanhMuc
        """)
    List<MucCatalogAdmin> docDanhSachAdmin();
    @Query("""
        select new com.beautyshop.dto.MucCatalogAdmin(d.maDanhMuc, d.tenDanhMuc, d.duongDan, d.dangHoatDong)
        from DanhMuc d
        where (:q = '' or lower(d.tenDanhMuc) like :q escape '!' or lower(d.duongDan) like :q escape '!')
          and (:active is null or d.dangHoatDong = :active)
        order by d.tenDanhMuc, d.maDanhMuc
        """)
    Page<MucCatalogAdmin> docTrangAdmin(String q, Boolean active, Pageable pageable);
}
