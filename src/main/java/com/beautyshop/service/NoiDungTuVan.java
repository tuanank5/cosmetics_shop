package com.beautyshop.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class NoiDungTuVan {
    private NoiDungTuVan() {}
    private static final Pattern PRIVATE = Pattern.compile(
        "(?iu)[\\w.+-]+@[\\w.-]+\\.[\\p{L}]{2,}|(?<!\\d)(?:\\+?\\d[ ().-]?){8,15}(?!\\d)"
        + "|\\b(?:otp|pin code|bearer|password|passcode|(?:access[ _-]?|refresh[ _-]?|id[ _-]?)?token|"
        + "(?:client[ _-]?)?secret|api[ _-]?key|cccd|cmnd|cvv)\\b"
        + "|\\beyj[a-z0-9_-]+\\.[a-z0-9_-]+\\.[a-z0-9_-]+|\\b(?:sk-|gh[pousr]_)[a-z0-9_-]{12,}"
        + "|\\b(?:ma (?:xac (?:thuc|minh)|bao mat)|can cuoc|so the|mat khau|thong tin ca nhan|lich su don|"
        + "don hang cua|dia chi (?:cua|nha toi)|toi (?:ten la|song tai)|ho ten (?:cua toi|la)|so dien thoai cua)\\b");
    private static final Pattern OUT_OF_SCOPE = Pattern.compile(
        "\\b(chan doan|dieu tri|chua benh|ke don|toa thuoc|benh da|system prompt|bo qua (?:chi dan|huong dan)|"
        + "ignore (?:all |previous )?(?:previous |prior )?(?:instructions|rules)|ignore previous|xuat du lieu)\\b");
    private static final Pattern MARKUP = Pattern.compile("(?i)<\\s*/?\\s*[a-z][^>]*>|\\[/?inst\\]|<\\|(?:system|user|assistant)");

    static String chuanHoa(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "").replace('đ', 'd');
    }

    static boolean choPhep(String text, int limit) {
        if (text == null || text.isBlank() || text.length() > limit) return false;
        String normalized = chuanHoa(Normalizer.normalize(text, Normalizer.Form.NFKC))
            .replaceAll("\\p{Cf}", "").replaceAll("(?U)\\s+", " ");
        return !PRIVATE.matcher(normalized).find() && !OUT_OF_SCOPE.matcher(normalized).find()
            && !MARKUP.matcher(normalized).find() && !normalized.strip().matches("[0-9]{4,7}");
    }
}
