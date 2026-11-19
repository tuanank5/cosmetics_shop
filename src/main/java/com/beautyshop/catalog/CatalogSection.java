package com.beautyshop.catalog;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

enum CatalogSection {
    CATEGORIES("categories", "Danh mục"),
    BRANDS("brands", "Thương hiệu"),
    PRODUCTS("products", "Sản phẩm");

    final String path;
    final String label;

    CatalogSection(String path, String label) {
        this.path = path;
        this.label = label;
    }

    static CatalogSection from(String path) {
        return Arrays.stream(values())
            .filter(section -> section.path.equals(path))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
