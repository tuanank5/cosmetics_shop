package com.beautyshop.dto;

/** Dữ liệu địa chỉ của chủ tài khoản dùng ở sổ địa chỉ và checkout. */
public record DiaChiHienThi(Long maDiaChi, String tenNguoiNhan, String soDienThoai,
                            String diaChiChiTiet, boolean laMacDinh) {}
