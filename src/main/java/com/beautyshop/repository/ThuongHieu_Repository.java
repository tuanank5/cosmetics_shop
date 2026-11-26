package com.beautyshop.repository;

import com.beautyshop.dto.MucCatalogAdmin;
import com.beautyshop.model.ThuongHieu;
import java.util.List;
import org.springframework.data.jpa.repository.Query;

public interface ThuongHieu_Repository extends DanhMucSanPhamThuongHieu_Repository<ThuongHieu> {
    List<ThuongHieu> findAllByDangHoatDongTrueOrderByTenThuongHieuAsc();

    @Query("""
        select new com.beautyshop.dto.MucCatalogAdmin(h.maThuongHieu, h.tenThuongHieu, h.duongDan, h.dangHoatDong)
        from ThuongHieu h order by h.tenThuongHieu, h.maThuongHieu
        """)
    List<MucCatalogAdmin> docDanhSachAdmin();
}
