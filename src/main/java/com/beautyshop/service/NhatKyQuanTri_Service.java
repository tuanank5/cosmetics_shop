package com.beautyshop.service;

import com.beautyshop.entity.NhatKyQuanTri;
import com.beautyshop.repository.NhatKyQuanTri_Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NhatKyQuanTri_Service {
    private final NhatKyQuanTri_Repository repository;

    NhatKyQuanTri_Service(NhatKyQuanTri_Repository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void ghi(Long actor, String role, String action, String targetType, long target,
                    String result, String reason, String correlation) {
        repository.save(new NhatKyQuanTri(actor, role, action, targetType, target, result, reason, correlation));
    }

    public void thuHoiPhien(long user, String correlation) {
        ghi(null, "SYSTEM", "SESSION_REVOKED", "ACCOUNT", user, "SUCCESS", null, correlation);
    }
}
