package com.beautyshop.service;

import org.springframework.security.access.prepost.PreAuthorize;
import com.beautyshop.dto.BienTheSanPham_Form;
import com.beautyshop.model.BienTheSanPham;
import com.beautyshop.model.SanPham;
import com.beautyshop.repository.BienTheSanPham_Repository;
import com.beautyshop.repository.DonHangCatalog_Repository.PhamVi;
import com.beautyshop.repository.SanPham_Repository;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.beautyshop.dto.PhanTrang;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class BienTheSanPham_Service {
    private final SuKienQuanTri_Service audit;
    private final BienTheSanPham_Repository bienTheRepository;
    private final SanPham_Repository sanPhamRepository;
    private final AnhBienTheSanPham_Service anhService;
    private final NgungHoatDongCatalog_Service ngungHoatDong;

    public BienTheSanPham_Service(BienTheSanPham_Repository bienTheRepository,
            SanPham_Repository sanPhamRepository, AnhBienTheSanPham_Service anhService,
            NgungHoatDongCatalog_Service ngungHoatDong, SuKienQuanTri_Service audit) {
        this.audit = audit;
        this.bienTheRepository = bienTheRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.anhService = anhService;
        this.ngungHoatDong = ngungHoatDong;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public SanPham sanPham(long maSanPham) {
        return sanPhamRepository.findById(maSanPham).orElseThrow(() -> khongTimThay());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<BienTheSanPham> danhSach(long maSanPham) {
        return bienTheRepository.findAllBySanPhamMaSanPhamOrderBySkuAsc(maSanPham);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<BienTheSanPham> danhSach(long maSanPham, Pageable pageable) {
        return PhanTrang.doc(pageable, trang -> bienTheRepository.findAllBySanPhamMaSanPhamOrderBySkuAsc(maSanPham, trang));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public BienTheSanPham chiTiet(long maSanPham, long maBienThe) {
        return bienTheRepository.findByMaBienTheAndSanPhamMaSanPham(maBienThe, maSanPham)
            .orElseThrow(() -> khongTimThay());
    }

    public Optional<Path> anhCongKhai(String tenTapTin) {
        return anhService.tim(tenTapTin)
            .filter(path -> bienTheRepository.existsByUrlHinhAnh(AnhBienTheSanPham_Service.URL_PREFIX + tenTapTin));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Map<Long, Long> demDonDangXuLy(long maSanPham) {
        return ngungHoatDong.demTheoBienThe(maSanPham);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public long demDonCuaBienThe(long maBienThe) {
        return ngungHoatDong.dem(PhamVi.BIEN_THE, maBienThe);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void luu(long maSanPham, Long maBienThe, BienTheSanPham_Form form, Errors errors) {
        BienTheSanPham bienThe = maBienThe == null ? new BienTheSanPham(sanPham(maSanPham))
            : khoa(maSanPham, maBienThe);
        Boolean activeCu = maBienThe == null ? null : bienThe.isDangHoatDong();
        int tonCu = bienThe.getSoLuongTrongKho();
        var suKien = maBienThe != null && tonCu != form.getSoLuongTrongKho()
            ? audit.batDau(SuKienQuanTri_Service.HanhDong.SKU_STOCK, "SKU", maBienThe) : null;
        String sku = form.getSku().toUpperCase(Locale.ROOT);
        String ten = form.getTenBienThe().strip();
        String maLo = form.getMaLoMinhHoa();
        maLo = maLo == null || maLo.isBlank() ? null : maLo.strip();
        boolean coAnhMoi = form.getHinhAnh() != null && !form.getHinhAnh().isEmpty();
        boolean doiCatalog = maBienThe == null || activeCu != form.isDangHoatDong()
            || !sku.equals(bienThe.getSku()) || !ten.equals(bienThe.getTenBienThe())
            || form.getGiaBan().compareTo(bienThe.getGiaBan()) != 0
            || !java.util.Objects.equals(maLo,bienThe.getMaLoMinhHoa())
            || !java.util.Objects.equals(form.getHanSuDungMinhHoa(),bienThe.getHanSuDungMinhHoa())
            || coAnhMoi || (form.isXoaHinhAnh() && bienThe.getUrlHinhAnh() != null);
        var catalog = !doiCatalog ? null :
            audit.batDau(maBienThe == null ? SuKienQuanTri_Service.HanhDong.CATALOG_CREATE
                : SuKienQuanTri_Service.HanhDong.CATALOG_UPDATE, "SKU", maBienThe,
                suKien == null ? java.util.UUID.randomUUID() : suKien.correlation());
        if (bienThe.isDangHoatDong() && !form.isDangHoatDong()) {
            ngungHoatDong.kiemTra(PhamVi.BIEN_THE, maBienThe);
        }
        if (form.getSoLuongTrongKho() < bienThe.getSoLuongTamGiu()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            errors.rejectValue("soLuongTrongKho", "sku.inventory", "Số lượng trong kho phải đủ cho số hàng đang giữ cho đơn hàng.");
            return;
        }

        if (form.isXoaHinhAnh() && coAnhMoi) {
            errors.rejectValue("hinhAnh", "sku.image.action", "Chọn thay ảnh hoặc xóa ảnh hiện có.");
            return;
        }
        if (coAnhMoi) {
            String urlMoi;
            try {
                urlMoi = anhService.luu(form.getHinhAnh());
            } catch (IllegalArgumentException exception) {
                errors.rejectValue("hinhAnh", "sku.image", exception.getMessage());
                return;
            } catch (UncheckedIOException exception) {
                errors.rejectValue("hinhAnh", "sku.image.storage", "Hiện không thể lưu ảnh. Vui lòng thử lại.");
                return;
            }
            theoDoiThayAnh(bienThe.getUrlHinhAnh(), urlMoi);
            bienThe.doiAnh(urlMoi);
        } else if (form.isXoaHinhAnh()) {
            theoDoiThayAnh(bienThe.getUrlHinhAnh(), null);
            bienThe.doiAnh(null);
        }

        bienThe.capNhat(sku, ten, form.getGiaBan(),
            form.getSoLuongTrongKho(), maLo,
            form.getHanSuDungMinhHoa(), form.isDangHoatDong());
        bienTheRepository.saveAndFlush(bienThe);
        if (catalog != null) audit.thanhCong(catalog, bienThe.getMaBienThe(), activeCu, bienThe.isDangHoatDong());
        if (suKien != null) audit.thanhCong(suKien, maBienThe, tonCu, bienThe.getSoLuongTrongKho());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void ngungHoatDong(long maSanPham, long maBienThe) {
        datTrangThai(maSanPham, maBienThe, false);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @PreAuthorize("hasRole('ADMIN')")
    public void datTrangThai(long maSanPham, long maBienThe, boolean hoatDong) {
        var bienThe = khoa(maSanPham, maBienThe);
        boolean truoc = bienThe.isDangHoatDong();
        if (truoc == hoatDong) return;
        var event = audit.batDau(SuKienQuanTri_Service.HanhDong.CATALOG_ACTIVE, "SKU", maBienThe);
        if (truoc && !hoatDong) ngungHoatDong.kiemTra(PhamVi.BIEN_THE, maBienThe);
        bienThe.datTrangThai(hoatDong);
        audit.thanhCong(event, maBienThe, truoc, hoatDong);
    }

    private BienTheSanPham khoa(long maSanPham, long maBienThe) {
        return bienTheRepository.khoaTrongSanPham(maSanPham, maBienThe)
            .orElseThrow(() -> khongTimThay());
    }

    private ResponseStatusException khongTimThay() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private void theoDoiThayAnh(String urlCu, String urlMoi) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    anhService.xoa(urlCu);
                } else if (status == STATUS_ROLLED_BACK) {
                    anhService.xoa(urlMoi);
                }
            }
        });
    }
}
