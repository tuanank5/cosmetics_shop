package com.beautyshop.repository;

import com.beautyshop.model.DanhTinhOAuth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DanhTinhOAuth_Repository extends JpaRepository<DanhTinhOAuth, Long> {
    @EntityGraph(attributePaths = "nguoiDung")
    Optional<DanhTinhOAuth> findByNhaCungCapAndMaDinhDanhNhaCungCap(String provider, String subject);

    List<DanhTinhOAuth> findByNguoiDung_MaNguoiDung(long maNguoiDung);

    @Query("select d.nhaCungCap from DanhTinhOAuth d where d.nguoiDung.maNguoiDung = :id")
    List<String> docNhaCungCap(@Param("id") long id);
}
