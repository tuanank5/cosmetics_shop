package com.beautyshop.repository;

import com.beautyshop.entity.MucDanhMucSanPhamThuongHieu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface DanhMucSanPhamThuongHieu_Repository<T extends MucDanhMucSanPhamThuongHieu> extends JpaRepository<T, Long> {
}
