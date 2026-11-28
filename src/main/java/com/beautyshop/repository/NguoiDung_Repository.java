package com.beautyshop.repository;

import com.beautyshop.model.NguoiDung;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NguoiDung_Repository extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmail(String email);

    @Query("select n.maNguoiDung from NguoiDung n where n.email = :email")
    Optional<Long> timMaTheoEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NguoiDung n where n.maNguoiDung = :id")
    Optional<NguoiDung> khoa(@Param("id") long id);

    @Query("""
        select n.kichHoat as kichHoat, n.vaiTro as vaiTro, n.phienBanXacThuc as phienBanXacThuc
        from NguoiDung n where n.maNguoiDung = :id
        """)
    Optional<TrangThaiPhien> findTrangThai(@Param("id") long id);

    @Query(value = """
        select n.maNguoiDung as maNguoiDung, n.hoTen as hoTen, n.email as email,
               n.kichHoat as kichHoat, n.vaiTro as vaiTro, n.phienBanXacThuc as phienBanXacThuc
        from NguoiDung n
        where (:q = '' or lower(n.hoTen) like :q escape '!' or lower(n.email) like :q escape '!')
          and (:kichHoat is null or n.kichHoat = :kichHoat)
          and (:vaiTro is null or n.vaiTro = :vaiTro)
        order by n.maNguoiDung desc
        """, countQuery = """
        select count(n) from NguoiDung n
        where (:q = '' or lower(n.hoTen) like :q escape '!' or lower(n.email) like :q escape '!')
          and (:kichHoat is null or n.kichHoat = :kichHoat)
          and (:vaiTro is null or n.vaiTro = :vaiTro)
        """)
    Page<TomTat> timNguoiDung(@Param("q") String q, @Param("kichHoat") Boolean kichHoat,
                            @Param("vaiTro") NguoiDung.VaiTro vaiTro, Pageable pageable);

    @Query("""
        select n.email as email, (n.matKhauBam is not null) as coMatKhau,
               (n.ngayXacMinhEmail is not null) as daXacMinhEmail
        from NguoiDung n where n.maNguoiDung = :id
        """)
    Optional<BaoMatTomTat> docBaoMat(@Param("id") long id);

    @Query("""
        select n.email as email, (n.matKhauBam is not null) as coMatKhau,
               (n.ngayXacMinhEmail is not null) as daXacMinhEmail, n.hoTen as hoTen,
               n.kichHoat as kichHoat, n.vaiTro as vaiTro
        from NguoiDung n where n.maNguoiDung = :id
        """)
    Optional<BaoMatAdmin> docBaoMatAdmin(@Param("id") long id);

    interface BaoMatTomTat {
        String getEmail();
        boolean getCoMatKhau();
        boolean getDaXacMinhEmail();
    }

    interface BaoMatAdmin extends BaoMatTomTat, TrangThai {
        String getHoTen();
    }

    interface TrangThai {
        boolean isKichHoat();
        NguoiDung.VaiTro getVaiTro();
    }

    interface TrangThaiPhien extends TrangThai {
        long getPhienBanXacThuc();
    }

    interface TomTat extends TrangThaiPhien {
        long getMaNguoiDung();
        String getHoTen();
        String getEmail();
    }
}
