ALTER TABLE nguoi_dung
    MODIFY COLUMN mat_khau_bam VARCHAR(255) NULL,
    ADD COLUMN ngay_xac_minh_email DATETIME(6) NULL;

CREATE TABLE danh_tinh_oauth (
    ma_danh_tinh BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ma_nguoi_dung BIGINT NOT NULL,
    nha_cung_cap VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    ma_dinh_danh_nha_cung_cap VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    ngay_lien_ket DATETIME(6) NULL,
    CONSTRAINT fk_danh_tinh_oauth_user FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung),
    CONSTRAINT uq_danh_tinh_oauth_subject UNIQUE (nha_cung_cap, ma_dinh_danh_nha_cung_cap),
    CONSTRAINT uq_danh_tinh_oauth_user UNIQUE (ma_nguoi_dung, nha_cung_cap),
    CONSTRAINT ck_danh_tinh_oauth_provider CHECK (nha_cung_cap IN ('google', 'github'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO danh_tinh_oauth (ma_nguoi_dung, nha_cung_cap, ma_dinh_danh_nha_cung_cap)
SELECT ma_nguoi_dung, nha_cung_cap_oauth, ma_danh_tinh_oauth
FROM nguoi_dung WHERE vai_tro = 'CUSTOMER' AND nha_cung_cap_oauth IS NOT NULL;

CREATE TABLE yeu_cau_dat_lai_mat_khau (
    ma_yeu_cau BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ma_nguoi_dung BIGINT NOT NULL,
    token_bam BINARY(32) NOT NULL UNIQUE,
    ngay_tao DATETIME(6) NOT NULL,
    het_han_luc DATETIME(6) NOT NULL,
    da_su_dung_luc DATETIME(6) NULL,
    nguon_yeu_cau VARCHAR(20) NOT NULL,
    phien_ban_xac_thuc BIGINT NOT NULL,
    correlation_id CHAR(36) NOT NULL,
    INDEX ix_dat_lai_user (ma_nguoi_dung, ngay_tao),
    INDEX ix_dat_lai_het_han (het_han_luc),
    CONSTRAINT fk_dat_lai_user FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE nhat_ky_quan_tri (
    ma_nhat_ky BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ma_nguoi_thuc_hien BIGINT NULL,
    vai_tro_nguoi_thuc_hien VARCHAR(20) NOT NULL,
    hanh_dong VARCHAR(60) NOT NULL,
    loai_doi_tuong VARCHAR(30) NOT NULL,
    ma_doi_tuong BIGINT NOT NULL,
    thoi_gian DATETIME(6) NOT NULL,
    ket_qua VARCHAR(20) NOT NULL,
    ly_do VARCHAR(40) NULL,
    correlation_id CHAR(36) NOT NULL,
    UNIQUE KEY uq_nhat_ky_event (correlation_id, hanh_dong),
    INDEX ix_nhat_ky_target (loai_doi_tuong, ma_doi_tuong, thoi_gian)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
