CREATE TABLE ghi_nhan_tru_kho_cod (
  ma_don_hang BIGINT NOT NULL,
  ma_bien_the BIGINT NOT NULL,
  so_luong INT NOT NULL,
  PRIMARY KEY (ma_don_hang, ma_bien_the),
  KEY idx_ghi_nhan_tru_kho_cod_01 (ma_bien_the),
  CONSTRAINT fk_ghi_nhan_tru_kho_cod_01 FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang),
  CONSTRAINT fk_ghi_nhan_tru_kho_cod_02 FOREIGN KEY (ma_bien_the) REFERENCES bien_the_san_pham(ma_bien_the),
  CONSTRAINT ck_ghi_nhan_tru_kho_cod_01 CHECK (so_luong > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
