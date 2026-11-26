package com.beautyshop.payment;

import com.beautyshop.repository.ThanhToanMomo_Repository;
import com.beautyshop.service.Momo_Facade;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class MomoReconciliationJobTest {
    @Test
    void tiepTucKhiMotDonLoiVaQuayLaiSauLoCuoi() {
        var settings = mock(Momo_Settings.class);
        var gateway = mock(MomoGateway.class);
        var repository = mock(ThanhToanMomo_Repository.class);
        var facade = mock(Momo_Facade.class);
        when(settings.gateway()).thenReturn(Optional.of(gateway));
        when(gateway.maGateway()).thenReturn(MomoGateway.MA_MOCK);
        when(repository.donQuaHan(MomoGateway.MA_MOCK, 0)).thenReturn(List.of(1L, 2L, 3L), List.of());
        when(repository.donQuaHan(MomoGateway.MA_MOCK, 3)).thenReturn(List.of(4L));
        when(facade.doiSoat(2L)).thenThrow(new IllegalStateException("Fixture gateway unavailable"));
        var job = new MomoReconciliation_Job(settings, facade, repository);

        job.doiSoatDonHetHan();
        job.doiSoatDonHetHan();
        job.doiSoatDonHetHan();

        var thuTu = inOrder(repository, facade);
        thuTu.verify(repository).donQuaHan(MomoGateway.MA_MOCK, 0);
        for (long id : List.of(1L, 2L, 3L)) thuTu.verify(facade).doiSoat(id);
        thuTu.verify(repository).donQuaHan(MomoGateway.MA_MOCK, 3);
        thuTu.verify(facade).doiSoat(4L);
        thuTu.verify(repository).donQuaHan(MomoGateway.MA_MOCK, 0);
        thuTu.verifyNoMoreInteractions();
    }
}
