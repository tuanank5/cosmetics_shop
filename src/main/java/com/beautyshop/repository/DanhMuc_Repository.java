package com.beautyshop.repository;

import com.beautyshop.entity.DanhMuc;
import java.util.List;

public interface DanhMuc_Repository extends DanhMucSanPhamThuongHieu_Repository<DanhMuc> {
    List<DanhMuc> findAllByDangHoatDongTrueOrderByTenDanhMucAsc();
}
