package com.beautyshop.service;

import com.beautyshop.repository.TriThuc_Repository;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class KhoTriThuc_Service {
    private static final Set<String> LINK_NOI_BO = Set.of("/checkout", "/account/returns", "/account/addresses",
        "/products", "/cart", "/register", "/login");

    private final TriThuc_Repository repository;
    KhoTriThuc_Service(TriThuc_Repository repository) { this.repository = repository; }

    public List<Nguon> faq() {
        return repository.faqCongKhai().stream().filter(faq -> hopLe(faq.url()))
            .map(faq -> new Nguon("faq:" + faq.ma(), faq.cauHoi(), faq.cauTraLoi(), faq.url(), faq.ngayCapNhat(), null))
            .toList();
    }

    public List<Nguon> tatCa() {
        var sanPham = repository.sanPhamCongKhai().stream()
            .filter(sp -> sp.slug().matches("[a-z0-9]+(?:-[a-z0-9]+)*"))
            .map(sp -> new Nguon("product:" + sp.slug(), sp.ten(),
                "Sản phẩm " + sp.ten() + " thuộc thương hiệu " + sp.thuongHieu() + ", danh mục " + sp.danhMuc() + ".",
                "/products/" + sp.slug(), null, sp.slug()));
        return Stream.concat(faq().stream(), sanPham).toList();
    }

    private boolean hopLe(String url) {
        if (LINK_NOI_BO.contains(url)) {
            return true;
        }
        try {
            var uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    record Nguon(String ma, String tieuDe, String noiDung, String url, LocalDateTime capNhat, String slug) {}
}
