package com.beautyshop.repository;

import com.beautyshop.dto.MucCatalogAdmin;
import com.beautyshop.model.ThuongHieu;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface ThuongHieu_Repository extends DanhMucSanPhamThuongHieu_Repository<ThuongHieu> {
    List<ThuongHieu> findAllByDangHoatDongTrueOrderByTenThuongHieuAsc();

    @Query("""
        select new com.beautyshop.dto.MucCatalogAdmin(h.maThuongHieu, h.tenThuongHieu, h.duongDan, h.dangHoatDong)
        from ThuongHieu h order by h.tenThuongHieu, h.maThuongHieu
        """)
    List<MucCatalogAdmin> docDanhSachAdmin();
    @Query("""
        select new com.beautyshop.dto.MucCatalogAdmin(h.maThuongHieu, h.tenThuongHieu, h.duongDan, h.dangHoatDong)
        from ThuongHieu h
        where (:q = '' or lower(h.tenThuongHieu) like :q escape '!' or lower(h.duongDan) like :q escape '!')
          and (:active is null or h.dangHoatDong = :active)
        order by h.tenThuongHieu, h.maThuongHieu
        """)
    Page<MucCatalogAdmin> docTrangAdmin(String q, Boolean active, Pageable pageable);
}
