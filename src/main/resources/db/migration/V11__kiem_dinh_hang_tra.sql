-- NULL nhận diện hồ sơ trước V11, không suy diễn số thực nhận từ trạng thái cũ.
ALTER TABLE yeu_cau_doi_tra ADD COLUMN phien_ban_kiem BIGINT NULL;

CREATE TABLE thao_tac_doi_tra (
    ma_thao_tac CHAR(36) PRIMARY KEY,
    ma_yeu_cau BIGINT NOT NULL,
    ma_admin BIGINT NOT NULL,
    hanh_dong VARCHAR(40) NOT NULL,
    dau_vao CHAR(64) NOT NULL,
    luc_ghi DATETIME(6) NOT NULL,
    FOREIGN KEY (ma_yeu_cau) REFERENCES yeu_cau_doi_tra(ma_yeu_cau_doi_tra),
    FOREIGN KEY (ma_admin) REFERENCES nguoi_dung(ma_nguoi_dung)
) ENGINE=InnoDB;

CREATE TABLE kien_doi_tra (
    ma_kien BIGINT AUTO_INCREMENT PRIMARY KEY,
    tham_chieu VARCHAR(80) NOT NULL UNIQUE,
    ma_nhan CHAR(36) NOT NULL UNIQUE,
    luong_lech INT NOT NULL DEFAULT 0 CHECK (luong_lech >= 0),
    ly_do_lech VARCHAR(24) NULL,
    ma_doi_chieu CHAR(36) NULL UNIQUE,
    chung_tu_lech VARCHAR(80) NULL,
    luong_khop_bo_sung INT NOT NULL DEFAULT 0,
    ma_xac_nhan CHAR(36) NULL UNIQUE,
    CHECK (luong_khop_bo_sung BETWEEN 0 AND luong_lech),
    CHECK ((luong_lech=0 AND ly_do_lech IS NULL) OR (luong_lech>0 AND ly_do_lech IS NOT NULL)),
    CHECK ((ma_doi_chieu IS NULL AND chung_tu_lech IS NULL AND luong_khop_bo_sung=0)
        OR (ma_doi_chieu IS NOT NULL AND chung_tu_lech IS NOT NULL AND luong_lech>0)),
    FOREIGN KEY (ma_doi_chieu) REFERENCES thao_tac_doi_tra(ma_thao_tac),
    FOREIGN KEY (ma_nhan) REFERENCES thao_tac_doi_tra(ma_thao_tac),
    FOREIGN KEY (ma_xac_nhan) REFERENCES thao_tac_doi_tra(ma_thao_tac)
) ENGINE=InnoDB;

CREATE TABLE dong_nhan_doi_tra (
    ma_dong_nhan BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_kien BIGINT NOT NULL,
    ma_dong_tra BIGINT NOT NULL,
    thuc_nhan INT NOT NULL CHECK (thuc_nhan > 0),
    ma_kiem CHAR(36) NULL,
    UNIQUE (ma_kien, ma_dong_tra),
    FOREIGN KEY (ma_kien) REFERENCES kien_doi_tra(ma_kien),
    FOREIGN KEY (ma_dong_tra) REFERENCES chi_tiet_doi_tra(ma_chi_tiet_doi_tra)
) ENGINE=InnoDB;

-- Lưu từng lần kiểm, kể cả khi phải phân loại lại SKU trước xác nhận.
CREATE TABLE ket_qua_kiem_doi_tra (
    ma_kiem CHAR(36) PRIMARY KEY,
    ma_dong_nhan BIGINT NOT NULL,
    du_ban INT NOT NULL,
    da_mo INT NOT NULL,
    hong INT NOT NULL,
    khac INT NOT NULL,
    ly_do_khac VARCHAR(160) NOT NULL,
    dat_checklist BOOLEAN NOT NULL,
    CHECK (LEAST(du_ban,da_mo,hong,khac) >= 0 AND (du_ban=0 OR dat_checklist=1)),
    UNIQUE (ma_dong_nhan,ma_kiem),
    FOREIGN KEY (ma_kiem) REFERENCES thao_tac_doi_tra(ma_thao_tac),
    FOREIGN KEY (ma_dong_nhan) REFERENCES dong_nhan_doi_tra(ma_dong_nhan)
) ENGINE=InnoDB;

ALTER TABLE dong_nhan_doi_tra ADD FOREIGN KEY (ma_dong_nhan,ma_kiem)
    REFERENCES ket_qua_kiem_doi_tra(ma_dong_nhan,ma_kiem);

CREATE TABLE nhap_kho_doi_tra (
    ma_dong_nhan BIGINT PRIMARY KEY,
    ma_bien_the BIGINT NOT NULL,
    so_luong INT NOT NULL CHECK (so_luong > 0),
    ma_xac_nhan CHAR(36) NOT NULL,
    FOREIGN KEY (ma_dong_nhan) REFERENCES dong_nhan_doi_tra(ma_dong_nhan),
    FOREIGN KEY (ma_bien_the) REFERENCES bien_the_san_pham(ma_bien_the),
    FOREIGN KEY (ma_xac_nhan) REFERENCES thao_tac_doi_tra(ma_thao_tac)
) ENGINE=InnoDB;

CREATE TABLE ket_thuc_doi_tra (
    ma_yeu_cau BIGINT PRIMARY KEY,
    ma_dong CHAR(36) NOT NULL UNIQUE,
    chung_tu_nghia_vu VARCHAR(80) NOT NULL,
    chung_tu_thieu VARCHAR(80) NULL,
    FOREIGN KEY (ma_yeu_cau) REFERENCES yeu_cau_doi_tra(ma_yeu_cau_doi_tra),
    FOREIGN KEY (ma_dong) REFERENCES thao_tac_doi_tra(ma_thao_tac)
) ENGINE=InnoDB;
