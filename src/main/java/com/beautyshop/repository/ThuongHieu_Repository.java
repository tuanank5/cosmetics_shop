package com.beautyshop.repository;

import com.beautyshop.model.ThuongHieu;
import java.util.List;

public interface ThuongHieu_Repository extends DanhMucSanPhamThuongHieu_Repository<ThuongHieu> {
    List<ThuongHieu> findAllByDangHoatDongTrueOrderByTenThuongHieuAsc();
}
