package com.beautyshop.dto;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Cửa sổ liên kết GET. Số trang trong URL bắt đầu từ 0. */
public record PhanTrang(int trang, int kichThuoc, long tongMuc, int tongTrang,
                       String dau, String truoc, List<LienKet> cuaSo, String sau, String cuoi,
                       boolean cachDau, boolean cachCuoi, boolean cachDauMobile, boolean cachCuoiMobile) {
    public record LienKet(int soHienThi, String href, boolean hienTai, boolean chiDesktop) {}

    public static Pageable yeuCau(int trang, int kichThuoc) {
        return PageRequest.of(Math.clamp(trang, 0, 100_000), kichThuoc);
    }

    public String moTa(String donVi) {
        if (tongTrang <= 1) return tongMuc + " " + donVi;
        long dau = (long) trang * kichThuoc;
        return (dau + 1) + "–" + Math.min(dau + kichThuoc, tongMuc) + " trong tổng " + tongMuc + " " + donVi;
    }

    public static PhanTrang tu(Page<?> page, IntFunction<String> href) {
        int tong = page.getTotalPages();
        int trang = page.getNumber();
        int cuoi = tong - 1;
        int moc = Math.max(0, Math.min(trang, cuoi));
        List<LienKet> cuaSo = tong <= 1 ? List.of() : IntStream
            .rangeClosed(Math.max(0, moc - 2), moc + Math.min(2, cuoi - moc))
            .mapToObj(so -> new LienKet(so + 1, so == trang ? null : href.apply(so), so == trang, Math.abs(so - moc) == 2))
            .toList();
        return new PhanTrang(trang, page.getSize(), page.getTotalElements(), tong,
            tong > 1 && trang > 0 ? href.apply(0) : null,
            tong > 1 && trang > 0 ? href.apply(Math.min(trang - 1, cuoi)) : null,
            cuaSo, tong > 1 && trang < cuoi ? href.apply(trang + 1) : null,
            tong > 1 && trang < cuoi ? href.apply(cuoi) : null,
            moc > 3, cuoi - moc > 3, moc > 2, cuoi - moc > 2);
    }

    /** Chỉ thử đọc lại một lần nếu trang yêu cầu đã nằm ngoài tập kết quả. */
    public static <T> Page<T> doc(Pageable pageable, Function<Pageable, Page<T>> truyVan) {
        Page<T> ketQua = truyVan.apply(pageable);
        int trangCuoi = Math.max(0, ketQua.getTotalPages() - 1);
        return pageable.getPageNumber() > trangCuoi ? truyVan.apply(pageable.withPage(trangCuoi)) : ketQua;
    }
}
