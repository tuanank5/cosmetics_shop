
package com.beautyshop.repository;

import com.beautyshop.dto.MucCatalogAdmin;
import com.beautyshop.model.SanPham;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SanPham_Repository
        extends DanhMucSanPhamThuongHieu_Repository<SanPham> {

    String LOC = """
        where (:q = '' or lower(s.tenSanPham) like :q escape '!' or lower(s.duongDan) like :q escape '!')
          and (:active is null or s.dangHoatDong = :active)
          and (:category is null or d.maDanhMuc = :category)
          and (:brand is null or t.maThuongHieu = :brand)
        """;

    @Query(value = """
        select new com.beautyshop.dto.MucCatalogAdmin(s.maSanPham, s.tenSanPham, s.duongDan, s.dangHoatDong,
               d.tenDanhMuc, t.tenThuongHieu, count(b), min(b.urlHinhAnh))
        from SanPham s join s.danhMuc d join s.thuongHieu t left join BienTheSanPham b on b.sanPham = s
        """ + LOC + """
        group by s.maSanPham, s.tenSanPham, s.duongDan, s.dangHoatDong, d.tenDanhMuc, t.tenThuongHieu
        order by s.tenSanPham, s.maSanPham
        """, countQuery = """
        select count(s) from SanPham s join s.danhMuc d join s.thuongHieu t
        """ + LOC)
    Page<MucCatalogAdmin> docTrangAdmin(String q, Boolean active, Long category, Long brand, Pageable pageable);

    default Page<MucCatalogAdmin> danhSachAdmin(String q, Boolean active, Long category, Long brand, Pageable pageable) {
        return docTrangAdmin(q, active, category, brand, pageable)
            .map(muc -> new MucCatalogAdmin(muc.ma(), muc.ten(), muc.duongDan(), muc.dangHoatDong(), muc.danhMuc(),
                muc.thuongHieu(), muc.soBienThe(), AnhBienTheHienThi.hopLe(muc.urlHinhAnh())));
    }
}
