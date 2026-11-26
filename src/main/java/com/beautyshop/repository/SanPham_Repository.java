
package com.beautyshop.repository;

import com.beautyshop.dto.MucCatalogAdmin;
import com.beautyshop.model.SanPham;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SanPham_Repository
        extends DanhMucSanPhamThuongHieu_Repository<SanPham> {

    @Query("""
        select new com.beautyshop.dto.MucCatalogAdmin(s.maSanPham, s.tenSanPham, s.duongDan, s.dangHoatDong)
        from SanPham s order by s.tenSanPham, s.maSanPham
        """)
    List<MucCatalogAdmin> docDanhSachAdmin();
}
