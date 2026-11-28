package com.beautyshop.controller;

import com.beautyshop.order.OrderStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class BoLocDonKhachTest {
    @Test
    void chiDungNguonNoiBoVaXuLyTrangBien() {
        for (String q : List.of("%","%_","mỹ phẩm")) {
            var nguon = NguonDieuHuong.donKhach(Map.of("q",q,"page","-5","returnUrl","https://example.invalid",
                "maNguoiDung","99"),List.of(OrderStatus.SHIPPING,OrderStatus.DELIVERED),false);
            assertThat(nguon.loc().maDon()).isEmpty();
            assertThat(nguon.danhSachKhach(0)).isEqualTo("/orders?page=0&status=SHIPPING&status=DELIVERED");
            assertThat(com.beautyshop.dto.PhanTrang.yeuCau(nguon.page(),10).getPageNumber()).isZero();
        }
        assertThat(com.beautyshop.dto.PhanTrang.yeuCau(Integer.MAX_VALUE,10).getPageNumber()).isEqualTo(100000);
    }

    @Test
    void duongVeGiuBoLocVaCoTrangCoDinh() {
        var nguon = NguonDieuHuong.donKhach(Map.of("size","50","page","2","q","BS-",
            "fromDate","2026-11-01","sort","oldest"), List.of(OrderStatus.SHIPPING), false);
        assertThat(nguon.loc().kichThuoc()).isEqualTo(10);
        assertThat(nguon.chiTietKhach(42,2)).contains("listQ=BS-", "listStatus=SHIPPING", "listPage=2",
            "listFromDate=2026-11-01", "listSort=oldest").doesNotContain("listSize");
        assertThat(nguon.danhSachKhach(1)).contains("page=1","status=SHIPPING").doesNotContain("size");
    }

    @Test
    void ngaySaiBiTuChoiVaSortLaChiNhanGiaTriDaDuyet() {
        assertThatThrownBy(() -> NguonDieuHuong.donKhach(Map.of("fromDate","2026-02-30"),List.of(),false))
            .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> NguonDieuHuong.donKhach(Map.of("fromDate","2026-02-02","toDate","2026-02-01"),List.of(),false))
            .isInstanceOf(ResponseStatusException.class);
        assertThat(NguonDieuHuong.donKhach(Map.of("sort","unknown"),List.of(),false).loc().sapXep().name())
            .isEqualTo("newest");
    }
}
