package com.beautyshop.repository;

import com.beautyshop.entity.NhatKyQuanTri;
import org.springframework.data.repository.Repository;

public interface NhatKyQuanTri_Repository extends Repository<NhatKyQuanTri, Long> {
    NhatKyQuanTri save(NhatKyQuanTri nhatKy);
}
