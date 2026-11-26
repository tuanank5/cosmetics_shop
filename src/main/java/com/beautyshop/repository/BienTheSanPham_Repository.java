
package com.beautyshop.repository;

import com.beautyshop.model.BienTheSanPham;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BienTheSanPham_Repository extends JpaRepository<BienTheSanPham, Long> {

    List<BienTheSanPham> findAllBySanPhamMaSanPhamOrderBySkuAsc(long maSanPham);

    List<BienTheSanPham> findAllBySanPhamMaSanPhamAndDangHoatDongTrueOrderByGiaBanAscMaBienTheAsc(
            long maSanPham);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BienTheSanPham b where b.maBienThe = :maBienThe")
    Optional<BienTheSanPham> khoa(@Param("maBienThe") long maBienThe);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b from BienTheSanPham b
        where b.maBienThe = :maBienThe
          and b.sanPham.maSanPham = :maSanPham
        """)
    Optional<BienTheSanPham> khoaTrongSanPham(
            @Param("maSanPham") long maSanPham,
            @Param("maBienThe") long maBienThe);

    Optional<BienTheSanPham> findByMaBienTheAndSanPhamMaSanPham(
            long maBienThe, long maSanPham);

    boolean existsByUrlHinhAnh(String urlHinhAnh);

    @Query("""
        select b
        from BienTheSanPham b
        join fetch b.sanPham sp
        join fetch sp.thuongHieu th
        join fetch sp.danhMuc dm
        where b.dangHoatDong = true
          and sp.dangHoatDong = true
          and th.dangHoatDong = true
          and dm.dangHoatDong = true
          and b.soLuongTrongKho > b.soLuongTamGiu
          and (
              lower(dm.tenDanhMuc) like lower(concat('%', :tuKhoa, '%'))
              or lower(sp.tenSanPham) like lower(concat('%', :tuKhoa, '%'))
          )
        order by b.giaBan asc
        """)
    List<BienTheSanPham> findDangBanConHangTheoLoai(
            @Param("tuKhoa") String tuKhoa,
            Pageable pageable);

    @Query("""
        select b
        from BienTheSanPham b
        join fetch b.sanPham sp
        join fetch sp.thuongHieu th
        join fetch sp.danhMuc dm
        where b.dangHoatDong = true
          and sp.dangHoatDong = true
          and th.dangHoatDong = true
          and dm.dangHoatDong = true
          and b.soLuongTrongKho > b.soLuongTamGiu
        order by b.giaBan asc
        """)
    List<BienTheSanPham> findAllDangBanConHang(Pageable pageable);
}
