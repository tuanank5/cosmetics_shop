ALTER TABLE phieu_giam_gia
    ADD COLUMN slot ENUM('HANG','VAN_CHUYEN') NOT NULL DEFAULT 'HANG',
    ADD COLUMN gioi_han_khach INT UNSIGNED NULL;

CREATE TABLE pham_vi_phieu_san_pham (
    ma_phieu_giam_gia BIGINT NOT NULL,
    ma_san_pham BIGINT NOT NULL,
    PRIMARY KEY (ma_phieu_giam_gia, ma_san_pham),
    FOREIGN KEY (ma_phieu_giam_gia) REFERENCES phieu_giam_gia(ma_phieu_giam_gia) ON DELETE CASCADE,
    FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham)
) ENGINE=InnoDB;

CREATE TABLE pham_vi_phieu_danh_muc (
    ma_phieu_giam_gia BIGINT NOT NULL,
    ma_danh_muc BIGINT NOT NULL,
    PRIMARY KEY (ma_phieu_giam_gia, ma_danh_muc),
    FOREIGN KEY (ma_phieu_giam_gia) REFERENCES phieu_giam_gia(ma_phieu_giam_gia) ON DELETE CASCADE,
    FOREIGN KEY (ma_danh_muc) REFERENCES danh_muc(ma_danh_muc)
) ENGINE=InnoDB;

CREATE TABLE pham_vi_phieu_thuong_hieu (
    ma_phieu_giam_gia BIGINT NOT NULL,
    ma_thuong_hieu BIGINT NOT NULL,
    PRIMARY KEY (ma_phieu_giam_gia, ma_thuong_hieu),
    FOREIGN KEY (ma_phieu_giam_gia) REFERENCES phieu_giam_gia(ma_phieu_giam_gia) ON DELETE CASCADE,
    FOREIGN KEY (ma_thuong_hieu) REFERENCES thuong_hieu(ma_thuong_hieu)
) ENGINE=InnoDB;

CREATE TABLE pham_vi_phieu_nguoi_dung (
    ma_phieu_giam_gia BIGINT NOT NULL,
    ma_nguoi_dung BIGINT NOT NULL,
    PRIMARY KEY (ma_phieu_giam_gia, ma_nguoi_dung),
    FOREIGN KEY (ma_phieu_giam_gia) REFERENCES phieu_giam_gia(ma_phieu_giam_gia) ON DELETE CASCADE,
    FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung)
) ENGINE=InnoDB;

ALTER TABLE tam_giu_phieu_giam_gia
    ADD COLUMN slot ENUM('HANG','VAN_CHUYEN') NOT NULL DEFAULT 'HANG',
    ADD COLUMN so_tien_giam DECIMAL(15,0) NULL,
    ADD UNIQUE KEY uq_don_slot (ma_don_hang, slot),
    ADD UNIQUE KEY uq_don_phieu (ma_don_hang, ma_phieu_giam_gia);
ALTER TABLE tam_giu_phieu_giam_gia DROP INDEX uq_tam_giu_phieu_giam_gia_01;

UPDATE tam_giu_phieu_giam_gia g JOIN don_hang d
    ON d.ma_don_hang=g.ma_don_hang AND d.ma_phieu_giam_gia=g.ma_phieu_giam_gia
SET g.so_tien_giam=d.so_tien_giam;

INSERT INTO tam_giu_phieu_giam_gia (ma_don_hang, ma_phieu_giam_gia, trang_thai, ngay_tao, so_tien_giam)
SELECT d.ma_don_hang, d.ma_phieu_giam_gia,
       IF(d.trang_thai='CANCELLED','RELEASED','CONSUMED'), d.ngay_tao, d.so_tien_giam
FROM don_hang d WHERE d.phuong_thuc_thanh_toan='COD' AND d.ma_phieu_giam_gia IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM tam_giu_phieu_giam_gia g WHERE g.ma_don_hang=d.ma_don_hang);

ALTER TABLE chi_tiet_don_hang ADD COLUMN giam_hang DECIMAL(15,0) NULL;
ALTER TABLE don_hang ADD COLUMN phi_chuan DECIMAL(15,0) NULL, ADD COLUMN giam_van_chuyen DECIMAL(15,0) NULL;
