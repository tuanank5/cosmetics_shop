package com.beautyshop.repository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** P23: MongoDB không phản hồi -> store nghỉ 15 s, các trang không phải chờ timeout ở mỗi lời gọi. Không cần MongoDB thật. */
class MongoDanhGiaStoreNghiTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final DanhGiaMongo_Repository repository = mock(DanhGiaMongo_Repository.class);
    private final DongHo dongHo = new DongHo();
    private final MongoDanhGia_Store store = new MongoDanhGia_Store(mongo, repository, dongHo);

    @Test
    void loiKetNoiThiNghiRoiThuLaiSauKhoangNghi() {
        var converter = mock(org.springframework.data.mongodb.core.convert.MongoConverter.class);
        when(mongo.getConverter()).thenReturn(converter);
        var mapping = new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        mapping.setSimpleTypeHolder(org.springframework.data.mongodb.core.convert.MongoCustomConversions
            .create(adapter -> {}).getSimpleTypeHolder());
        org.mockito.Mockito.doReturn(mapping).when(converter).getMappingContext();
        when(mongo.indexOps(com.beautyshop.model.DanhGia_Document.class))
            .thenReturn(mock(org.springframework.data.mongodb.core.index.IndexOperations.class));
        when(mongo.execute(any(String.class), any(org.springframework.data.mongodb.core.CollectionCallback.class)))
            .thenReturn(true);
        when(repository.findByMaSanPhamAndHienThiTrueOrderByNgayTaoDescIdDesc(anyLong(), any()))
            .thenThrow(new DataAccessResourceFailureException("timeout"))
            .thenReturn(Page.empty());

        assertThatThrownBy(() -> store.congKhai(1, Pageable.ofSize(10))).isInstanceOf(DataAccessResourceFailureException.class);
        dongHo.troi(MongoDanhGia_Store.NGHI_SAU_LOI_MS - 1);
        // Trong khoảng nghỉ: ném ngay, không gọi MongoDB.
        assertThatThrownBy(() -> store.congKhai(1, Pageable.ofSize(10))).hasMessageContaining("tam ngung");
        verify(repository, times(1)).findByMaSanPhamAndHienThiTrueOrderByNgayTaoDescIdDesc(anyLong(), any());

        dongHo.troi(1);
        assertThat(store.congKhai(1, Pageable.ofSize(10))).isEmpty();
        verify(repository, times(2)).findByMaSanPhamAndHienThiTrueOrderByNgayTaoDescIdDesc(anyLong(), any());
    }

    @Test
    void maKhongPhaiObjectIdThiKhongGoiMongo() {
        assertThat(store.datHienThi("../x", true, "0")).isFalse();
        assertThat(store.datHienThi(null, false, null)).isFalse();
        assertThat(store.datHienThi("65f000000000000000000001", true, "khong-phai-so")).isFalse();
        verify(mongo, times(0)).findAndModify(any(), any(), any(Class.class));
    }

    private static final class DongHo extends Clock {
        private long millis = Instant.parse("2026-11-24T00:00:00Z").toEpochMilli();

        void troi(long ms) { millis += ms; }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public long millis() { return millis; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
    }
}
