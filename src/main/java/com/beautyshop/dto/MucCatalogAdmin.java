package com.beautyshop.dto;

public record MucCatalogAdmin(Long ma, String ten, String duongDan, boolean dangHoatDong,
                              String danhMuc, String thuongHieu, long soBienThe, String urlHinhAnh) {
    public MucCatalogAdmin(Long ma, String ten, String duongDan, boolean dangHoatDong) {
        this(ma, ten, duongDan, dangHoatDong, null, null, 0, null);
    }
}
