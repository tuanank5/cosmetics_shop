package com.beautyshop.repository;

import com.beautyshop.dto.MucCatalogAdmin;
import com.beautyshop.model.DanhMuc;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface DanhMuc_Repository extends DanhMucSanPhamThuongHieu_Repository<DanhMuc> {
    List<DanhMuc> findAllByDangHoatDongTrueOrderByTenDanhMucAsc();

    @Query("""
        select new com.beautyshop.dto.MucCatalogAdmin(d.maDanhMuc, d.tenDanhMuc, d.duongDan, d.dangHoatDong)
        from DanhMuc d order by d.tenDanhMuc, d.maDanhMuc
        """)
    List<MucCatalogAdmin> docDanhSachAdmin();
}
