package com.beautyshop.payment;

import com.beautyshop.repository.ThanhToanMomo_Repository;
import com.beautyshop.service.Momo_Facade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
class MomoReconciliation_Job {
    private static final Logger logger = LoggerFactory.getLogger(MomoReconciliation_Job.class);
    private final Momo_Settings settings;
    private final Momo_Facade facade;
    private final ThanhToanMomo_Repository repository;
    private long conTro;

    MomoReconciliation_Job(Momo_Settings settings, Momo_Facade facade, ThanhToanMomo_Repository repository) {
        this.settings = settings;
        this.facade = facade;
        this.repository = repository;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    void doiSoatDonHetHan() {
        settings.gateway().ifPresent(gateway -> {
            var danhSach = repository.donQuaHan(gateway.maGateway(), conTro);
            for (long maDon : danhSach) {
                try {
                    facade.doiSoat(maDon);
                } catch (RuntimeException exception) {
                    logger.warn("Chưa thể hoàn tất đối soát đơn MoMo {}.", maDon);
                }
            }
            conTro = danhSach.size() == 3 ? danhSach.getLast() : 0;
        });
    }
}
