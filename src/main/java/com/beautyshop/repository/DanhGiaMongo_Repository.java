package com.beautyshop.repository;

import com.beautyshop.entity.DanhGia_Document;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/** P23: truy vấn theo tên method của Spring Data MongoDB (không viết JSON tay). */
public interface DanhGiaMongo_Repository extends MongoRepository<DanhGia_Document, String> {
    Optional<DanhGia_Document> findByMaChiTietDonHangAndMaNguoiDung(Long maChiTietDonHang, Long maNguoiDung);

    Page<DanhGia_Document> findByMaSanPhamAndHienThiTrueOrderByNgayTaoDesc(Long maSanPham, Pageable pageable);

    long countByHienThiFalseAndNgayDuyetIsNull();
}
