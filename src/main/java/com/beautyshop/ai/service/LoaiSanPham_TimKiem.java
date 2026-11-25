package com.beautyshop.ai.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

final class LoaiSanPham_TimKiem {
    private static final List<LoaiSanPham> CAC_LOAI = List.of(
            new LoaiSanPham("serum", List.of("serum", "tinh chất", "tinh chat")),
            new LoaiSanPham("sữa rửa mặt", List.of(
                    "sữa rửa mặt", "sua rua mat", "srm", "face wash", "facial cleanser")),
            new LoaiSanPham("toner", List.of("toner", "nước hoa hồng", "nuoc hoa hong")),
            new LoaiSanPham("kem dưỡng", List.of(
                    "kem dưỡng", "kem duong", "moisturizer")),
            new LoaiSanPham("chống nắng", List.of(
                    "chống nắng", "chong nang", "sunscreen", "suncream", "sunblock")),
            new LoaiSanPham("tẩy trang", List.of(
                    "tẩy trang", "tay trang", "micellar water", "dầu tẩy trang", "dau tay trang")),
            new LoaiSanPham("tẩy tế bào chết", List.of(
                    "tẩy tế bào chết", "tay te bao chet", "exfoliant", "peeling")),
            new LoaiSanPham("mặt nạ", List.of("mặt nạ", "mat na", "face mask"))
    );

    private LoaiSanPham_TimKiem() {}

    static List<LoaiSanPham> nhanDien(String noiDung) {
        String daChuanHoa = chuanHoa(noiDung);
        var ketQua = new ArrayList<LoaiSanPham>();
        for (LoaiSanPham loai : CAC_LOAI) {
            if (loai.tuKhoa().stream().anyMatch(tuKhoa -> coCumTu(daChuanHoa, chuanHoa(tuKhoa)))) {
                ketQua.add(loai);
            }
        }
        return List.copyOf(ketQua);
    }

    private static boolean coCumTu(String noiDung, String tuKhoa) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(tuKhoa)
                + "(?![\\p{L}\\p{N}])").matcher(noiDung).find();
    }

    private static String chuanHoa(String noiDung) {
        if (noiDung == null) {
            return "";
        }
        return Normalizer.normalize(noiDung.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "d")
                .replaceAll("\\s+", " ")
                .trim();
    }

    record LoaiSanPham(String tenHienThi, List<String> tuKhoa) {}
}
