package com.beautyshop.repository;

import com.beautyshop.entity.DanhTinhOAuth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DanhTinhOAuth_Repository extends JpaRepository<DanhTinhOAuth, Long> {
    @EntityGraph(attributePaths = "nguoiDung")
    Optional<DanhTinhOAuth> findByNhaCungCapAndMaDinhDanhNhaCungCap(String provider, String subject);

    List<DanhTinhOAuth> findByNguoiDung_MaNguoiDung(long maNguoiDung);
}
