package com.beautyshop.service;

import com.beautyshop.repository.CuaHang_Repository.TheSanPham;
import java.util.List;

/**
 * Điểm mở rộng gợi ý sản phẩm. Template chỉ biết danh sách {@link TheSanPham}; P24 thay bản cài đặt
 * (cá nhân hóa theo người dùng) mà không phải sửa template hay controller.
 */
public interface GoiYSanPham_Service {
    /**
     * @param maNguoiDungHoacNull người đang xem (null với Guest/Admin); bản phổ biến bỏ qua tham số này
     * @param soLuong số sản phẩm tối đa
     */
    List<TheSanPham> goiY(Long maNguoiDungHoacNull, int soLuong);
}
