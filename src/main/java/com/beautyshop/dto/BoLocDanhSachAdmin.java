package com.beautyshop.dto;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record BoLocDanhSachAdmin(String q, Boolean active, Long category, Long brand) {
    public BoLocDanhSachAdmin {
        q = q.strip();
        if (q.codePointCount(0, q.length()) > 190) q = q.substring(0, q.offsetByCodePoints(0, 190));
    }

    public String mauTim() {
        return q.isEmpty() ? "" : "%" + q.toLowerCase(Locale.ROOT)
            .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    public Map<String, String> thamSo() {
        var query = new LinkedHashMap<String, String>();
        if (!q.isEmpty()) query.put("q", q);
        if (active != null) query.put("active", active.toString());
        if (category != null) query.put("category", category.toString());
        if (brand != null) query.put("brand", brand.toString());
        return query;
    }
}
