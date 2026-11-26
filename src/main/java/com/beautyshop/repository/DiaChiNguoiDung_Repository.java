package com.beautyshop.repository;

import com.beautyshop.dto.DiaChiHienThi;
import com.beautyshop.model.DiaChiNguoiDung;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaChiNguoiDung_Repository extends JpaRepository<DiaChiNguoiDung, Long> {
    List<DiaChiNguoiDung> findAllByNguoiDungMaNguoiDungOrderByMaDiaChiAsc(long maNguoiDung);
    Optional<DiaChiNguoiDung> findByMaDiaChiAndNguoiDungMaNguoiDung(long maDiaChi, long maNguoiDung);

    @Query("""
        select new com.beautyshop.dto.DiaChiHienThi(d.maDiaChi, d.tenNguoiNhan, d.soDienThoai, d.diaChiChiTiet, d.laMacDinh)
        from DiaChiNguoiDung d where d.nguoiDung.maNguoiDung = :maNguoiDung order by d.maDiaChi asc
        """)
    List<DiaChiHienThi> docDanhSach(@Param("maNguoiDung") long maNguoiDung);
}
