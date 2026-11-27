package com.beautyshop.repository;

import java.util.regex.Pattern;

final class AnhBienTheHienThi {
    private static final Pattern ANH_SKU = Pattern.compile(
        "/uploads/sku/[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}\\.(?:jpg|png)"
            + "|/img/products/[a-z0-9](?:[a-z0-9-]{0,98}[a-z0-9])?\\.(?:webp|jpg|png)");

    private AnhBienTheHienThi() {}

    static String hopLe(String url) {
        return url != null && ANH_SKU.matcher(url).matches() ? url : null;
    }
}
