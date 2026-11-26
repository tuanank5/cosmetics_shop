package com.beautyshop.service;

import org.springframework.security.access.prepost.PreAuthorize;
import com.beautyshop.domain.catalog.NhomDanhMucSanPhamThuongHieu;
import com.beautyshop.dto.DanhMucSanPhamThuongHieu_Form;
import com.beautyshop.dto.MucCatalogAdmin;
import com.beautyshop.model.ThuongHieu;
import com.beautyshop.model.MucDanhMucSanPhamThuongHieu;
import com.beautyshop.model.DanhMuc;
import com.beautyshop.model.SanPham;
import com.beautyshop.repository.ThuongHieu_Repository;
import com.beautyshop.repository.DanhMucSanPhamThuongHieu_Repository;
import com.beautyshop.repository.DanhMuc_Repository;
import com.beautyshop.repository.SanPham_Repository;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class DanhMucSanPhamThuongHieu_Service {
    private final DanhMuc_Repository danhMucRepository;
    private final ThuongHieu_Repository thuongHieuRepository;
    private final SanPham_Repository sanPhamRepository;

    DanhMucSanPhamThuongHieu_Service(DanhMuc_Repository danhMucRepository, ThuongHieu_Repository thuongHieuRepository, SanPham_Repository sanPhamRepository) {
        this.danhMucRepository = danhMucRepository;
        this.thuongHieuRepository = thuongHieuRepository;
        this.sanPhamRepository = sanPhamRepository;
    }

    @Transactional(readOnly = true)
    public List<MucCatalogAdmin> list(NhomDanhMucSanPhamThuongHieu nhom) {
        return switch (nhom) {
            case DANH_MUC -> danhMucRepository.docDanhSachAdmin();
            case THUONG_HIEU -> thuongHieuRepository.docDanhSachAdmin();
            case SAN_PHAM -> sanPhamRepository.docDanhSachAdmin();
        };
    }

    @Transactional(readOnly = true)
    public DanhMucSanPhamThuongHieu_Form edit(NhomDanhMucSanPhamThuongHieu nhom, long ma) {
        return DanhMucSanPhamThuongHieu_Form.from(find(repository(nhom), ma));
    }

    @Transactional
    public void save(NhomDanhMucSanPhamThuongHieu nhom, Long ma, DanhMucSanPhamThuongHieu_Form form, Errors errors) {
        switch (nhom) {
            case DANH_MUC -> luuDanhMucHoacThuongHieu(danhMucRepository, ma, new DanhMuc(), form, errors);
            case THUONG_HIEU -> luuDanhMucHoacThuongHieu(thuongHieuRepository, ma, new ThuongHieu(), form, errors);
            case SAN_PHAM -> luuSanPham(ma, form, errors);
        }
    }

    private <T extends MucDanhMucSanPhamThuongHieu> void luuDanhMucHoacThuongHieu(
            DanhMucSanPhamThuongHieu_Repository<T> repository, Long ma, T mucMoi, DanhMucSanPhamThuongHieu_Form form, Errors errors) {
        if (form.getName().strip().length() > 160) {
            errors.rejectValue("name", "catalog.name.length", "Tên tối đa 160 ký tự.");
            return;
        }
        T muc = ma == null ? mucMoi : khoa(repository, ma);
        muc.update(form);
        repository.saveAndFlush(muc);
    }

    private void luuSanPham(Long ma, DanhMucSanPhamThuongHieu_Form form, Errors errors) {
        DanhMuc danhMuc = select(danhMucRepository, form.getCategoryId(), "categoryId", errors);
        ThuongHieu thuongHieu = select(thuongHieuRepository, form.getBrandId(), "brandId", errors);
        if (errors.hasErrors()) {
            return;
        }
        SanPham sanPham = ma == null ? new SanPham() : khoa(sanPhamRepository, ma);
        sanPham.update(form, danhMuc, thuongHieu);
        sanPhamRepository.saveAndFlush(sanPham);
    }

    private <T extends MucDanhMucSanPhamThuongHieu> T select(
            DanhMucSanPhamThuongHieu_Repository<T> repository, Long ma, String field, Errors errors) {
        T muc = ma == null ? null : repository.findById(ma).orElse(null);
        if (muc == null) {
            errors.rejectValue(field, "catalog.selection", "Vui lòng chọn một mục hợp lệ.");
        }
        return muc;
    }

    @Transactional
    public void datTrangThai(NhomDanhMucSanPhamThuongHieu nhom, long ma, boolean hoatDong) {
        khoa(repository(nhom), ma).datTrangThai(hoatDong);
    }

    private <T extends MucDanhMucSanPhamThuongHieu> T khoa(DanhMucSanPhamThuongHieu_Repository<T> repository, long ma) {
        return repository.khoa(ma).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private DanhMucSanPhamThuongHieu_Repository<? extends MucDanhMucSanPhamThuongHieu> repository(NhomDanhMucSanPhamThuongHieu nhom) {
        return switch (nhom) {
            case DANH_MUC -> danhMucRepository;
            case THUONG_HIEU -> thuongHieuRepository;
            case SAN_PHAM -> sanPhamRepository;
        };
    }

    private <T extends MucDanhMucSanPhamThuongHieu> T find(DanhMucSanPhamThuongHieu_Repository<T> repository, long ma) {
        return repository.findById(ma)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
