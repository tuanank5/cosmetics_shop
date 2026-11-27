
package com.beautyshop.repository;

import com.beautyshop.dto.MucCatalogAdmin;
import com.beautyshop.model.SanPham;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SanPham_Repository
        extends DanhMucSanPhamThuongHieu_Repository<SanPham> {

    @Query("""
        select new com.beautyshop.dto.MucCatalogAdmin(s.maSanPham, s.tenSanPham, s.duongDan, s.dangHoatDong,
               d.tenDanhMuc, t.tenThuongHieu, count(b), min(b.urlHinhAnh))
        from SanPham s join s.danhMuc d join s.thuongHieu t left join BienTheSanPham b on b.sanPham = s
        group by s.maSanPham, s.tenSanPham, s.duongDan, s.dangHoatDong, d.tenDanhMuc, t.tenThuongHieu
        order by s.tenSanPham, s.maSanPham
        """)
    List<MucCatalogAdmin> docDanhSachAdmin();

    default List<MucCatalogAdmin> danhSachAdmin() {
        return docDanhSachAdmin().stream()
            .map(muc -> new MucCatalogAdmin(muc.ma(), muc.ten(), muc.duongDan(), muc.dangHoatDong(), muc.danhMuc(),
                muc.thuongHieu(), muc.soBienThe(), AnhBienTheHienThi.hopLe(muc.urlHinhAnh())))
            .toList();
    }
}
