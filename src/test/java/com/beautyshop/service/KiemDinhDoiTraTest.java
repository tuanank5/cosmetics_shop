package com.beautyshop.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.beautyshop.service.KiemDinhDoiTra_Service.Kiem;

class KiemDinhDoiTraTest {
    @Test void duBanCanChecklistVaPhanLoaiDuLuongNhan() {
        assertThatCode(() -> new Kiem(1,1,1,1,"Hết hạn",true).kiemTra(4)).doesNotThrowAnyException();
        assertThatIllegalStateException().isThrownBy(() -> new Kiem(1,0,0,0,"",false).kiemTra(1));
        assertThatIllegalStateException().isThrownBy(() -> new Kiem(0,1,0,0,"",false).kiemTra(2));
        assertThatIllegalStateException().isThrownBy(() -> new Kiem(0,1,0,1," ",false).kiemTra(2));
        assertThatIllegalStateException().isThrownBy(() -> new Kiem(-1,2,0,0,"",true).kiemTra(1));
    }
}
