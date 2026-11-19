package com.beautyshop.repository;

import com.beautyshop.entity.BienTheSanPham;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BienTheSanPham_Repository extends JpaRepository<BienTheSanPham, Long> {
    List<BienTheSanPham> findAllBySanPhamMaSanPhamOrderBySkuAsc(long maSanPham);

    List<BienTheSanPham> findAllBySanPhamMaSanPhamAndDangHoatDongTrueOrderByGiaBanAscMaBienTheAsc(long maSanPham);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BienTheSanPham b where b.maBienThe = :maBienThe")
    Optional<BienTheSanPham> khoa(@Param("maBienThe") long maBienThe);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b from BienTheSanPham b
        where b.maBienThe = :maBienThe and b.sanPham.maSanPham = :maSanPham
        """)
    Optional<BienTheSanPham> khoaTrongSanPham(@Param("maSanPham") long maSanPham,
                                             @Param("maBienThe") long maBienThe);

    Optional<BienTheSanPham> findByMaBienTheAndSanPhamMaSanPham(long maBienThe, long maSanPham);

    boolean existsByUrlHinhAnh(String urlHinhAnh);
}
