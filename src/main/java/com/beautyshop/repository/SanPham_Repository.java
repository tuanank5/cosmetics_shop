
package com.beautyshop.repository;

import com.beautyshop.entity.SanPham;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SanPham_Repository
        extends DanhMucSanPhamThuongHieu_Repository<SanPham> {

    @Query("""
        SELECT sp
        FROM SanPham sp
        WHERE LOWER(sp.loaiDaPhuHop) LIKE LOWER(CONCAT('%', :loaiDa, '%'))
    """)
    List<SanPham> findByLoaiDaPhuHop(@Param("loaiDa") String loaiDa);
}
