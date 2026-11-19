-- V1: Lược đồ MariaDB BeautyShop - đặt tên bảng và cột bằng tiếng Việt không dấu.
-- Các trường DATETIME lưu theo UTC do ứng dụng quy định; DATETIME không lưu thông tin múi giờ.
-- Sau khi áp dụng V1 bằng Flyway, không sửa V1; mọi thay đổi tiếp theo dùng V2__, V3__ ...

CREATE TABLE nguoi_dung (
  ma_nguoi_dung BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(190) NOT NULL,
  mat_khau_bam VARCHAR(255) NOT NULL,
  ho_ten VARCHAR(160) NOT NULL,
  so_dien_thoai VARCHAR(30) NULL,
  vai_tro VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
  kich_hoat BOOLEAN NOT NULL DEFAULT TRUE,
  ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_nguoi_dung_01 (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dia_chi_nguoi_dung (
  ma_dia_chi BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_nguoi_dung BIGINT NOT NULL,
  ten_nguoi_nhan VARCHAR(160) NOT NULL,
  so_dien_thoai VARCHAR(30) NOT NULL,
  dia_chi_chi_tiet VARCHAR(600) NOT NULL,
  la_mac_dinh BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_dia_chi_nguoi_dung_01 FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung),
  KEY idx_dia_chi_nguoi_dung_01 (ma_nguoi_dung)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE danh_muc (
  ma_danh_muc BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ten_danh_muc VARCHAR(160) NOT NULL,
  duong_dan VARCHAR(190) NOT NULL,
  dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_danh_muc_01 (duong_dan)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE thuong_hieu (
  ma_thuong_hieu BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ten_thuong_hieu VARCHAR(160) NOT NULL,
  duong_dan VARCHAR(190) NOT NULL,
  dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_thuong_hieu_01 (duong_dan)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE san_pham (
  ma_san_pham BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_danh_muc BIGINT NOT NULL,
  ma_thuong_hieu BIGINT NOT NULL,
  ten_san_pham VARCHAR(255) NOT NULL,
  duong_dan VARCHAR(190) NOT NULL,
  mo_ta TEXT NULL,
  thanh_phan TEXT NULL,
  huong_dan_su_dung TEXT NULL,
  loai_da_phu_hop VARCHAR(600) NULL,
  dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
  ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_san_pham_01 (duong_dan),
  KEY idx_san_pham_01 (ma_danh_muc),
  KEY idx_san_pham_02 (ma_thuong_hieu),
  CONSTRAINT fk_san_pham_01 FOREIGN KEY (ma_danh_muc) REFERENCES danh_muc(ma_danh_muc),
  CONSTRAINT fk_san_pham_02 FOREIGN KEY (ma_thuong_hieu) REFERENCES thuong_hieu(ma_thuong_hieu)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bien_the_san_pham (
  ma_bien_the BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_san_pham BIGINT NOT NULL,
  sku VARCHAR(100) NOT NULL,
  ten_bien_the VARCHAR(200) NOT NULL,
  gia_ban DECIMAL(15,0) NOT NULL,
  so_luong_trong_kho INT NOT NULL DEFAULT 0,
  so_luong_tam_giu INT NOT NULL DEFAULT 0,
  url_hinh_anh VARCHAR(1000) NULL,
  -- Thông tin lô và hạn sử dụng chỉ để hiển thị demo, KHÔNG phải quản lý tồn kho theo lô/FEFO.
  ma_lo_minh_hoa VARCHAR(100) NULL,
  han_su_dung_minh_hoa DATE NULL,
  dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_bien_the_san_pham_01 (sku),
  KEY idx_bien_the_san_pham_01 (ma_san_pham),
  CONSTRAINT fk_bien_the_san_pham_01 FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham),
  CONSTRAINT ck_bien_the_san_pham_01 CHECK (gia_ban >= 0),
  CONSTRAINT ck_bien_the_san_pham_02 CHECK (so_luong_trong_kho >= 0),
  CONSTRAINT ck_bien_the_san_pham_03 CHECK (so_luong_tam_giu >= 0 AND so_luong_tam_giu <= so_luong_trong_kho)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE gio_hang (
  ma_gio_hang BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_nguoi_dung BIGINT NOT NULL,
  ngay_cap_nhat DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_gio_hang_01 (ma_nguoi_dung),
  CONSTRAINT fk_gio_hang_01 FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chi_tiet_gio_hang (
  ma_chi_tiet_gio_hang BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_gio_hang BIGINT NOT NULL,
  ma_bien_the BIGINT NOT NULL,
  so_luong INT NOT NULL,
  UNIQUE KEY uq_chi_tiet_gio_hang_01 (ma_gio_hang, ma_bien_the),
  KEY idx_chi_tiet_gio_hang_01 (ma_bien_the),
  CONSTRAINT fk_chi_tiet_gio_hang_01 FOREIGN KEY (ma_gio_hang) REFERENCES gio_hang(ma_gio_hang),
  CONSTRAINT fk_chi_tiet_gio_hang_02 FOREIGN KEY (ma_bien_the) REFERENCES bien_the_san_pham(ma_bien_the),
  CONSTRAINT ck_chi_tiet_gio_hang_01 CHECK (so_luong > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE phieu_giam_gia (
  ma_phieu_giam_gia BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_ap_dung VARCHAR(80) NOT NULL,
  loai_giam_gia VARCHAR(20) NOT NULL,
  gia_tri_giam DECIMAL(15,0) NOT NULL,
  muc_giam_toi_da DECIMAL(15,0) NULL,
  don_toi_thieu DECIMAL(15,0) NOT NULL DEFAULT 0,
  gioi_han_su_dung INT NOT NULL,
  so_luot_da_dung INT NOT NULL DEFAULT 0,
  so_luot_tam_giu INT NOT NULL DEFAULT 0,
  bat_dau_luc DATETIME(6) NOT NULL,
  ket_thuc_luc DATETIME(6) NOT NULL,
  dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE KEY uq_phieu_giam_gia_01 (ma_ap_dung),
  CONSTRAINT ck_phieu_giam_gia_01 CHECK (gioi_han_su_dung >= 0 AND so_luot_da_dung >= 0 AND so_luot_tam_giu >= 0 AND so_luot_da_dung + so_luot_tam_giu <= gioi_han_su_dung),
  CONSTRAINT ck_phieu_giam_gia_02 CHECK (gia_tri_giam >= 0 AND don_toi_thieu >= 0 AND (muc_giam_toi_da IS NULL OR muc_giam_toi_da >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE don_hang (
  ma_don_hang BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_don_hang_hien_thi VARCHAR(80) NOT NULL,
  ma_nguoi_dung BIGINT NOT NULL,
  ma_phieu_giam_gia BIGINT NULL,
  ten_nguoi_nhan VARCHAR(160) NOT NULL,
  sdt_nguoi_nhan VARCHAR(30) NOT NULL,
  dia_chi_khi_dat VARCHAR(600) NOT NULL,
  tam_tinh DECIMAL(15,0) NOT NULL,
  so_tien_giam DECIMAL(15,0) NOT NULL DEFAULT 0,
  phi_van_chuyen DECIMAL(15,0) NOT NULL DEFAULT 0,
  tong_thanh_toan DECIMAL(15,0) NOT NULL,
  trang_thai VARCHAR(30) NOT NULL,
  phuong_thuc_thanh_toan VARCHAR(20) NOT NULL,
  ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ngay_giao_thanh_cong DATETIME(6) NULL,
  UNIQUE KEY uq_don_hang_01 (ma_don_hang_hien_thi),
  KEY idx_don_hang_01 (ma_nguoi_dung, ngay_tao),
  KEY idx_don_hang_02 (ma_phieu_giam_gia),
  CONSTRAINT fk_don_hang_01 FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung),
  CONSTRAINT fk_don_hang_02 FOREIGN KEY (ma_phieu_giam_gia) REFERENCES phieu_giam_gia(ma_phieu_giam_gia),
  CONSTRAINT ck_don_hang_01 CHECK (tam_tinh >= 0 AND so_tien_giam >= 0 AND phi_van_chuyen >= 0 AND tong_thanh_toan >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chi_tiet_don_hang (
  ma_chi_tiet_don_hang BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_don_hang BIGINT NOT NULL,
  ma_bien_the BIGINT NOT NULL,
  sku_khi_dat VARCHAR(100) NOT NULL,
  ten_san_pham_khi_dat VARCHAR(255) NOT NULL,
  ten_bien_the_khi_dat VARCHAR(200) NOT NULL,
  don_gia DECIMAL(15,0) NOT NULL,
  so_luong INT NOT NULL,
  KEY idx_chi_tiet_don_hang_01 (ma_don_hang),
  KEY idx_chi_tiet_don_hang_02 (ma_bien_the),
  CONSTRAINT fk_chi_tiet_don_hang_01 FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang),
  CONSTRAINT fk_chi_tiet_don_hang_02 FOREIGN KEY (ma_bien_the) REFERENCES bien_the_san_pham(ma_bien_the),
  CONSTRAINT ck_chi_tiet_don_hang_01 CHECK (so_luong > 0),
  CONSTRAINT ck_chi_tiet_don_hang_02 CHECK (don_gia >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tam_giu_phieu_giam_gia (
  ma_giu_phieu_giam_gia BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_phieu_giam_gia BIGINT NOT NULL,
  ma_don_hang BIGINT NOT NULL,
  trang_thai VARCHAR(20) NOT NULL,
  ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_tam_giu_phieu_giam_gia_01 (ma_don_hang),
  KEY idx_tam_giu_phieu_giam_gia_01 (ma_phieu_giam_gia),
  CONSTRAINT fk_tam_giu_phieu_giam_gia_01 FOREIGN KEY (ma_phieu_giam_gia) REFERENCES phieu_giam_gia(ma_phieu_giam_gia),
  CONSTRAINT fk_tam_giu_phieu_giam_gia_02 FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tam_giu_ton_kho (
  ma_giu_ton_kho BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_don_hang BIGINT NOT NULL,
  ma_bien_the BIGINT NOT NULL,
  so_luong INT NOT NULL,
  trang_thai VARCHAR(20) NOT NULL,
  het_han_luc DATETIME(6) NOT NULL,
  ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_tam_giu_ton_kho_01 (ma_don_hang, ma_bien_the),
  KEY idx_tam_giu_ton_kho_01 (ma_bien_the, trang_thai),
  KEY idx_tam_giu_ton_kho_02 (trang_thai, het_han_luc),
  CONSTRAINT fk_tam_giu_ton_kho_01 FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang),
  CONSTRAINT fk_tam_giu_ton_kho_02 FOREIGN KEY (ma_bien_the) REFERENCES bien_the_san_pham(ma_bien_the),
  CONSTRAINT ck_tam_giu_ton_kho_01 CHECK (so_luong > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lan_thanh_toan (
  ma_lan_thanh_toan BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_don_hang BIGINT NOT NULL,
  ma_yeu_cau VARCHAR(150) NOT NULL,
  ma_giao_dich_cong VARCHAR(160) NULL,
  cong_thanh_toan VARCHAR(20) NOT NULL,
  so_tien DECIMAL(15,0) NOT NULL,
  trang_thai VARCHAR(30) NOT NULL,
  ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  ngay_thanh_toan DATETIME(6) NULL,
  UNIQUE KEY uq_lan_thanh_toan_01 (ma_yeu_cau),
  UNIQUE KEY uq_lan_thanh_toan_02 (cong_thanh_toan, ma_giao_dich_cong),
  KEY idx_lan_thanh_toan_01 (ma_don_hang),
  CONSTRAINT fk_lan_thanh_toan_01 FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang),
  CONSTRAINT ck_lan_thanh_toan_01 CHECK (so_tien >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE van_chuyen (
  ma_van_chuyen BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_don_hang BIGINT NOT NULL,
  trang_thai VARCHAR(30) NOT NULL,
  ngay_gui_hang DATETIME(6) NULL,
  ngay_giao_thanh_cong DATETIME(6) NULL,
  UNIQUE KEY uq_van_chuyen_01 (ma_don_hang),
  CONSTRAINT fk_van_chuyen_01 FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE danh_gia (
  ma_danh_gia BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_nguoi_dung BIGINT NOT NULL,
  ma_san_pham BIGINT NOT NULL,
  ma_chi_tiet_don_hang BIGINT NOT NULL,
  so_sao INT NOT NULL,
  noi_dung TEXT NULL,
  hien_thi BOOLEAN NOT NULL DEFAULT TRUE,
  ngay_tao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_danh_gia_01 (ma_chi_tiet_don_hang),
  KEY idx_danh_gia_01 (ma_nguoi_dung),
  KEY idx_danh_gia_02 (ma_san_pham),
  CONSTRAINT fk_danh_gia_01 FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung),
  CONSTRAINT fk_danh_gia_02 FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham),
  CONSTRAINT fk_danh_gia_03 FOREIGN KEY (ma_chi_tiet_don_hang) REFERENCES chi_tiet_don_hang(ma_chi_tiet_don_hang),
  CONSTRAINT ck_danh_gia_01 CHECK (so_sao BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE san_pham_yeu_thich (
  ma_yeu_thich BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_nguoi_dung BIGINT NOT NULL,
  ma_san_pham BIGINT NOT NULL,
  UNIQUE KEY uq_san_pham_yeu_thich_01 (ma_nguoi_dung, ma_san_pham),
  KEY idx_san_pham_yeu_thich_01 (ma_san_pham),
  CONSTRAINT fk_san_pham_yeu_thich_01 FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung),
  CONSTRAINT fk_san_pham_yeu_thich_02 FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE yeu_cau_doi_tra (
  ma_yeu_cau_doi_tra BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_don_hang BIGINT NOT NULL,
  ma_nguoi_dung BIGINT NOT NULL,
  trang_thai VARCHAR(30) NOT NULL,
  ly_do TEXT NOT NULL,
  ngay_yeu_cau DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_yeu_cau_doi_tra_01 (ma_don_hang),
  KEY idx_yeu_cau_doi_tra_02 (ma_nguoi_dung),
  CONSTRAINT fk_yeu_cau_doi_tra_01 FOREIGN KEY (ma_don_hang) REFERENCES don_hang(ma_don_hang),
  CONSTRAINT fk_yeu_cau_doi_tra_02 FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chi_tiet_doi_tra (
  ma_chi_tiet_doi_tra BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  ma_yeu_cau_doi_tra BIGINT NOT NULL,
  ma_chi_tiet_don_hang BIGINT NOT NULL,
  so_luong INT NOT NULL,
  UNIQUE KEY uq_chi_tiet_doi_tra_01 (ma_yeu_cau_doi_tra, ma_chi_tiet_don_hang),
  KEY idx_chi_tiet_doi_tra_01 (ma_chi_tiet_don_hang),
  CONSTRAINT fk_chi_tiet_doi_tra_01 FOREIGN KEY (ma_yeu_cau_doi_tra) REFERENCES yeu_cau_doi_tra(ma_yeu_cau_doi_tra),
  CONSTRAINT fk_chi_tiet_doi_tra_02 FOREIGN KEY (ma_chi_tiet_don_hang) REFERENCES chi_tiet_don_hang(ma_chi_tiet_don_hang),
  CONSTRAINT ck_chi_tiet_doi_tra_01 CHECK (so_luong > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bai_viet_faq (
  ma_faq BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cau_hoi VARCHAR(600) NOT NULL,
  cau_tra_loi TEXT NOT NULL,
  url_nguon VARCHAR(1000) NULL,
  ngay_cap_nhat DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
