package com.beautyshop.repository;

import com.beautyshop.model.MucDanhMucSanPhamThuongHieu;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@NoRepositoryBean
public interface DanhMucSanPhamThuongHieu_Repository<T extends MucDanhMucSanPhamThuongHieu> extends Repository<T, Long> {
    List<T> findAll();
    Optional<T> findById(Long ma);
    boolean existsById(Long ma);
    <S extends T> S saveAndFlush(S muc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from #{#entityName} m where id(m) = :ma")
    Optional<T> khoa(@Param("ma") long ma);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select m from #{#entityName} m where id(m) = :ma")
    Optional<T> docCoKhoa(@Param("ma") long ma);
}
