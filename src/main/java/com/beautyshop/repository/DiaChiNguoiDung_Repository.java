package com.beautyshop.repository;

import com.beautyshop.model.DiaChiNguoiDung;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaChiNguoiDung_Repository extends JpaRepository<DiaChiNguoiDung, Long> {
    List<DiaChiNguoiDung> findAllByNguoiDungMaNguoiDungOrderByMaDiaChiAsc(long maNguoiDung);
    Optional<DiaChiNguoiDung> findByMaDiaChiAndNguoiDungMaNguoiDung(long maDiaChi, long maNguoiDung);
}
