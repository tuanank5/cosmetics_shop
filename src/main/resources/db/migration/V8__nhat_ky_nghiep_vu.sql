ALTER TABLE nhat_ky_quan_tri
    ADD COLUMN truoc VARCHAR(80) NULL,
    ADD COLUMN sau VARCHAR(80) NULL,
    ADD INDEX ix_nhat_ky_thoi_gian (thoi_gian, ma_nhat_ky);

CREATE TABLE bao_luu_nhat_ky (
    ma_nhat_ky BIGINT NOT NULL,
    ly_do VARCHAR(20) NOT NULL,
    PRIMARY KEY (ma_nhat_ky, ly_do),
    FOREIGN KEY (ma_nhat_ky) REFERENCES nhat_ky_quan_tri(ma_nhat_ky) ON DELETE RESTRICT,
    CHECK (ly_do IN ('DOI_SOAT', 'TRANH_CHAP', 'NGHIA_VU'))
) ENGINE=InnoDB;
