package com.beautyshop.service;

import com.beautyshop.dto.BienTheSanPham_Form;
import com.beautyshop.entity.BienTheSanPham;
import com.beautyshop.entity.SanPham;
import com.beautyshop.repository.BienTheSanPham_Repository;
import com.beautyshop.repository.SanPham_Repository;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.Errors;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class BienTheSanPham_Service {
    private final BienTheSanPham_Repository bienTheRepository;
    private final SanPham_Repository sanPhamRepository;
    private final AnhBienTheSanPham_Service anhService;

    public BienTheSanPham_Service(BienTheSanPham_Repository bienTheRepository,
            SanPham_Repository sanPhamRepository, AnhBienTheSanPham_Service anhService) {
        this.bienTheRepository = bienTheRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.anhService = anhService;
    }

    public SanPham sanPham(long maSanPham) {
        return sanPhamRepository.findById(maSanPham).orElseThrow(() -> khongTimThay());
    }

    public List<BienTheSanPham> danhSach(long maSanPham) {
        return bienTheRepository.findAllBySanPhamMaSanPhamOrderBySkuAsc(maSanPham);
    }

    public BienTheSanPham chiTiet(long maSanPham, long maBienThe) {
        return bienTheRepository.findByMaBienTheAndSanPhamMaSanPham(maBienThe, maSanPham)
            .orElseThrow(() -> khongTimThay());
    }

    public Optional<Path> anhCongKhai(String tenTapTin) {
        return anhService.tim(tenTapTin)
            .filter(path -> bienTheRepository.existsByUrlHinhAnh(AnhBienTheSanPham_Service.URL_PREFIX + tenTapTin));
    }

    @Transactional
    public void luu(long maSanPham, Long maBienThe, BienTheSanPham_Form form, Errors errors) {
        BienTheSanPham bienThe = maBienThe == null ? new BienTheSanPham(sanPham(maSanPham))
            : khoa(maSanPham, maBienThe);
        if (form.getSoLuongTrongKho() < bienThe.getSoLuongTamGiu()) {
            errors.rejectValue("soLuongTrongKho", "sku.inventory", "Tồn kho phải đủ cho số lượng đang tạm giữ.");
            return;
        }

        boolean coAnhMoi = form.getHinhAnh() != null && !form.getHinhAnh().isEmpty();
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

        String maLo = form.getMaLoMinhHoa();
        bienThe.capNhat(form.getSku().toUpperCase(Locale.ROOT), form.getTenBienThe().strip(), form.getGiaBan(),
            form.getSoLuongTrongKho(), maLo == null || maLo.isBlank() ? null : maLo.strip(),
            form.getHanSuDungMinhHoa(), form.isDangHoatDong());
        bienTheRepository.saveAndFlush(bienThe);
    }

    @Transactional
    public void ngungHoatDong(long maSanPham, long maBienThe) {
        khoa(maSanPham, maBienThe).ngungHoatDong();
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
