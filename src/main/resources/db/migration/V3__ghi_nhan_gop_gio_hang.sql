CREATE TABLE ghi_nhan_gop_gio_hang (
  ma_gop CHAR(36) NOT NULL PRIMARY KEY,
  ma_nguoi_dung BIGINT NOT NULL,
  ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_ghi_nhan_gop_gio_hang_01 (ma_nguoi_dung),
  CONSTRAINT fk_ghi_nhan_gop_gio_hang_01 FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
