package com.beautyshop.domain.catalog;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum NhomDanhMucSanPhamThuongHieu {
    DANH_MUC("categories", "Danh mục"),
    THUONG_HIEU("brands", "Thương hiệu"),
    SAN_PHAM("products", "Sản phẩm");

    public final String path;
    public final String label;

    NhomDanhMucSanPhamThuongHieu(String path, String label) {
        this.path = path;
        this.label = label;
    }

    public static NhomDanhMucSanPhamThuongHieu from(String path) {
        return Arrays.stream(values())
            .filter(section -> section.path.equals(path))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
