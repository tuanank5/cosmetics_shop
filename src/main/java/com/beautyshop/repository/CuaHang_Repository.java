package com.beautyshop.repository;

import com.beautyshop.model.SanPham;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CuaHang_Repository extends Repository<SanPham, Long> {
    String LOC = """
        from BienTheSanPham b
        join b.sanPham s
        join s.thuongHieu t
        join s.danhMuc d
        where s.dangHoatDong = true and b.dangHoatDong = true
          and t.dangHoatDong = true and d.dangHoatDong = true
          and (:brand = '' or t.duongDan = :brand)
          and (:category = '' or d.duongDan = :category)
          and (:skinType = '' or lower(s.loaiDaPhuHop) like :skinType escape '!')
          and (:q = '' or lower(s.tenSanPham) like :q escape '!'
               or lower(s.moTa) like :q escape '!' or lower(t.tenThuongHieu) like :q escape '!'
               or lower(b.sku) like :q escape '!' or lower(s.thanhPhan) like :q escape '!')
        """;

    @Query(value = """
        select s.maSanPham as maSanPham, s.duongDan as duongDan, s.tenSanPham as tenSanPham, t.tenThuongHieu as tenThuongHieu,
               min(b.giaBan) as giaTu, min(b.urlHinhAnh) as urlHinhAnh,
               max(b.soLuongTrongKho - b.soLuongTamGiu) as soLuongKhaDungLonNhat
        """ + LOC + """
        group by s.maSanPham, s.duongDan, s.tenSanPham, t.tenThuongHieu, s.ngayTao
        order by case when :sort = 'priceAsc' then min(b.giaBan) end asc,
                 case when :sort = 'priceDesc' then min(b.giaBan) end desc,
                 s.ngayTao desc, s.maSanPham desc
        """, countQuery = "select count(distinct s.maSanPham) " + LOC)
    Page<TheSanPham> timSanPham(@Param("q") String q, @Param("brand") String brand,
        @Param("category") String category, @Param("skinType") String skinType,
        @Param("sort") String sort, Pageable pageable);

    /** Trang chủ: sản phẩm active mới nhất có ít nhất một SKU active còn hàng khả dụng; không có cờ "nổi bật" trong schema. */
    @Query("""
        select s.maSanPham as maSanPham, s.duongDan as duongDan, s.tenSanPham as tenSanPham, t.tenThuongHieu as tenThuongHieu,
               min(b.giaBan) as giaTu, min(b.urlHinhAnh) as urlHinhAnh,
               max(b.soLuongTrongKho - b.soLuongTamGiu) as soLuongKhaDungLonNhat
        from BienTheSanPham b
        join b.sanPham s
        join s.thuongHieu t
        join s.danhMuc d
        where s.dangHoatDong = true and b.dangHoatDong = true
          and t.dangHoatDong = true and d.dangHoatDong = true
        group by s.maSanPham, s.duongDan, s.tenSanPham, t.tenThuongHieu, s.ngayTao
        having max(b.soLuongTrongKho - b.soLuongTamGiu) > 0
        order by s.ngayTao desc, s.maSanPham desc
        """)
    List<TheSanPham> timNoiBat(Pageable pageable);

    /** P21: thẻ sản phẩm theo danh sách mã (gợi ý); cùng điều kiện hiển thị và còn hàng như dải nổi bật. Caller tự giữ thứ tự. */
    @Query("""
        select s.maSanPham as maSanPham, s.duongDan as duongDan, s.tenSanPham as tenSanPham, t.tenThuongHieu as tenThuongHieu,
               min(b.giaBan) as giaTu, min(b.urlHinhAnh) as urlHinhAnh,
               max(b.soLuongTrongKho - b.soLuongTamGiu) as soLuongKhaDungLonNhat
        from BienTheSanPham b
        join b.sanPham s
        join s.thuongHieu t
        join s.danhMuc d
        where s.maSanPham in :ma and s.dangHoatDong = true and b.dangHoatDong = true
          and t.dangHoatDong = true and d.dangHoatDong = true
        group by s.maSanPham, s.duongDan, s.tenSanPham, t.tenThuongHieu
        having max(b.soLuongTrongKho - b.soLuongTamGiu) > 0
        """)
    List<TheSanPhamCoMa> timTheTheoMa(@Param("ma") Collection<Long> ma);

    /** Trang chủ chương 2: chuỗi loại da thô của các sản phẩm đang bán (cùng điều kiện hiển thị với /products). */
    @Query("""
        select distinct s.loaiDaPhuHop
        from BienTheSanPham b join b.sanPham s join s.thuongHieu t join s.danhMuc d
        where s.dangHoatDong = true and b.dangHoatDong = true
          and t.dangHoatDong = true and d.dangHoatDong = true
          and s.loaiDaPhuHop is not null
        """)
    List<String> timLoaiDaDangBan();

    /** Trang chủ chương 3: số sản phẩm đang bán theo danh mục; danh mục không có sản phẩm thì không xuất hiện. */
    @Query("""
        select d.tenDanhMuc as tenDanhMuc, d.duongDan as duongDan, count(distinct s.maSanPham) as soSanPham
        from BienTheSanPham b join b.sanPham s join s.thuongHieu t join s.danhMuc d
        where s.dangHoatDong = true and b.dangHoatDong = true
          and t.dangHoatDong = true and d.dangHoatDong = true
        group by d.maDanhMuc, d.tenDanhMuc, d.duongDan
        """)
    List<DanhMucDangBan> demDanhMucDangBan();

    @EntityGraph(attributePaths = {"thuongHieu", "danhMuc"})
    @Query("""
        select s from SanPham s where s.duongDan = :slug and s.dangHoatDong = true
          and s.thuongHieu.dangHoatDong = true and s.danhMuc.dangHoatDong = true
        """)
    Optional<SanPham> timChiTiet(@Param("slug") String slug);

    interface DanhMucDangBan {
        String getTenDanhMuc();
        String getDuongDan();
        long getSoSanPham();
    }

    /** Giữ tên cũ của P21; mọi thẻ đều có mã sản phẩm từ P23 (tóm tắt đánh giá theo lô). */
    interface TheSanPhamCoMa extends TheSanPham {
    }

    interface TheSanPham {
        Long getMaSanPham();
        String getDuongDan();
        String getTenSanPham();
        String getTenThuongHieu();
        BigDecimal getGiaTu();
        String getUrlHinhAnh();
        int getSoLuongKhaDungLonNhat();
    }
}
