package com.beautyshop.repository;

import com.beautyshop.model.YeuCauDatLaiMatKhau;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface YeuCauDatLaiMatKhau_Repository extends JpaRepository<YeuCauDatLaiMatKhau, Long> {
    @Query("select y.nguoiDung.maNguoiDung from YeuCauDatLaiMatKhau y where y.tokenBam = :hash")
    Optional<Long> timChuSoHuu(@Param("hash") byte[] hash);

    @Query("""
        select count(y) from YeuCauDatLaiMatKhau y join y.nguoiDung n
        where y.tokenBam = :hash and y.hetHanLuc > :now and y.daSuDungLuc is null
          and y.phienBanXacThuc = n.phienBanXacThuc and n.kichHoat = true
          and n.vaiTro = :role and n.matKhauBam is not null and n.ngayXacMinhEmail is not null
        """)
    long demLinkHopLe(@Param("hash") byte[] hash, @Param("now") LocalDateTime now,
                      @Param("role") com.beautyshop.model.NguoiDung.VaiTro role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<YeuCauDatLaiMatKhau> findByTokenBam(byte[] tokenBam);

    boolean existsByNguoiDung_MaNguoiDungAndNgayTaoAfter(long user, LocalDateTime since);

    @Modifying
    @Query("""
        update YeuCauDatLaiMatKhau y set y.hetHanLuc = :now
        where y.nguoiDung.maNguoiDung = :user and y.hetHanLuc > :now and y.daSuDungLuc is null
        """)
    void hetHanCacLink(@Param("user") long user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        update YeuCauDatLaiMatKhau y set y.hetHanLuc = :now
        where y.maYeuCau = :id and y.hetHanLuc > :now and y.daSuDungLuc is null
        """)
    void hetHanLink(@Param("id") long id, @Param("now") LocalDateTime now);
}
