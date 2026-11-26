package com.beautyshop.service;

import com.beautyshop.entity.DanhTinhOAuth;
import com.beautyshop.entity.NguoiDung;
import com.beautyshop.repository.DanhTinhOAuth_Repository;
import com.beautyshop.repository.NguoiDung_Repository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DanhTinhOAuth_Service {
    public record BangChung(String provider, String subject, String email, String name)
        implements java.io.Serializable {}
    private final DanhTinhOAuth_Repository identities;
    private final NguoiDung_Repository users;
    private final NhatKyQuanTri_Service audit;

    DanhTinhOAuth_Service(DanhTinhOAuth_Repository identities, NguoiDung_Repository users,
                          NhatKyQuanTri_Service audit) {
        this.identities = identities;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public NguoiDung dangNhap(BangChung proof) {
        var identity = identities.findByNhaCungCapAndMaDinhDanhNhaCungCap(proof.provider(), proof.subject());
        if (identity.isPresent()) {
            var user = identity.get().getNguoiDung();
            kiemCustomer(user);
            return user;
        }
        if (users.findByEmail(proof.email()).isPresent()) {
            throw new AccessDeniedException("Đăng nhập bằng phương thức hiện có rồi liên kết trong trang Bảo mật.");
        }
        var user = users.saveAndFlush(new NguoiDung(proof.email(), null, proof.name()));
        luuLienKet(user, proof, UUID.randomUUID().toString());
        return user;
    }

    @Transactional(readOnly = true)
    public NguoiDung danhTinhDaGan(BangChung proof) {
        var user = identities.findByNhaCungCapAndMaDinhDanhNhaCungCap(proof.provider(), proof.subject())
            .orElseThrow(DanhTinhOAuth_Service::tuChoi).getNguoiDung();
        kiemCustomer(user);
        return user;
    }

    @Transactional
    public void lienKet(long id, long version, BangChung proof) {
        var user = khoaCustomer(id, version);
        String correlation = UUID.randomUUID().toString();
        luuLienKet(user, proof, correlation);
        user.thuHoiPhien();
        audit.thuHoiPhien(id, correlation);
    }

    private void luuLienKet(NguoiDung user, BangChung proof, String correlation) {
        identities.saveAndFlush(new DanhTinhOAuth(user, proof.provider(), proof.subject(),
            LocalDateTime.now(ZoneOffset.UTC)));
        audit.ghi(user.getMaNguoiDung(), "CUSTOMER", "OAUTH_LINKED", "ACCOUNT", user.getMaNguoiDung(),
            "SUCCESS", proof.provider(), correlation);
    }

    @Transactional
    public void goLienKet(long id, long version, String provider, String daXacThuc, Set<String> providers) {
        var user = khoaCustomer(id, version);
        var methods = identities.findByNguoiDung_MaNguoiDung(id);
        boolean conDuongDangNhap = "password".equals(daXacThuc) && user.getMatKhauBam() != null
            || !provider.equals(daXacThuc) && providers.contains(daXacThuc)
                && methods.stream().anyMatch(method -> method.getNhaCungCap().equals(daXacThuc));
        if (!conDuongDangNhap) {
            throw tuChoi();
        }
        var identity = methods.stream().filter(method -> method.getNhaCungCap().equals(provider))
            .findFirst().orElseThrow(DanhTinhOAuth_Service::tuChoi);
        identities.delete(identity);
        user.thuHoiPhien();
        String correlation = UUID.randomUUID().toString();
        audit.ghi(id, "CUSTOMER", "OAUTH_UNLINKED", "ACCOUNT", id, "SUCCESS", provider, correlation);
        audit.thuHoiPhien(id, correlation);
    }

    public NguoiDung khoaCustomer(long id, long version) {
        var user = users.khoa(id).orElseThrow(DanhTinhOAuth_Service::tuChoi);
        kiemCustomer(user);
        if (user.getPhienBanXacThuc() != version) {
            throw tuChoi();
        }
        return user;
    }

    @Transactional
    public void xacNhanEmail(long id, long version) {
        khoaCustomer(id, version).xacNhanEmail(LocalDateTime.now(ZoneOffset.UTC));
    }

    static void kiemCustomer(NguoiDung user) {
        if (!user.isKichHoat() || user.getVaiTro() != NguoiDung.VaiTro.CUSTOMER) {
            throw tuChoi();
        }
    }

    private static AccessDeniedException tuChoi() {
        return new AccessDeniedException("Không thể thực hiện với phương thức đăng nhập hiện tại.");
    }
}
