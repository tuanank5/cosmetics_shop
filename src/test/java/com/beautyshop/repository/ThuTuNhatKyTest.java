package com.beautyshop.repository;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ThuTuNhatKyTest {
    @Test void sortCoKhoaPhanXuVaMacDinhAnToan() {
        assertThat(TraCuuNhatKy_Repository.thuTu("oldest")).isEqualTo("thoi_gian,ma_nhat_ky");
        assertThat(TraCuuNhatKy_Repository.thuTu("khac")).isEqualTo("thoi_gian DESC,ma_nhat_ky DESC");
    }
}
