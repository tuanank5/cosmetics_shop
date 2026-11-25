# ERD v5 – SQL tiếng Việt không dấu

Đây là sơ đồ ERD cho migration `V1__khoi_tao_co_so_du_lieu.sql`. Không dùng ERD v4 tiếng Anh làm nguồn tham chiếu nữa.

```mermaid
erDiagram
  nguoi_dung {
    bigint ma_nguoi_dung PK
    varchar email
    varchar mat_khau_bam
    varchar ho_ten
    varchar so_dien_thoai
    varchar vai_tro
    boolean kich_hoat
    date ngay_tao
  }
  dia_chi_nguoi_dung {
    bigint ma_dia_chi PK
    bigint ma_nguoi_dung
    varchar ten_nguoi_nhan
    varchar so_dien_thoai
    varchar dia_chi_chi_tiet
    boolean la_mac_dinh
  }
  danh_muc {
    bigint ma_danh_muc PK
    varchar ten_danh_muc
    varchar duong_dan
    boolean dang_hoat_dong
  }
  thuong_hieu {
    bigint ma_thuong_hieu PK
    varchar ten_thuong_hieu
    varchar duong_dan
    boolean dang_hoat_dong
  }
  san_pham {
    bigint ma_san_pham PK
    bigint ma_danh_muc
    bigint ma_thuong_hieu
    varchar ten_san_pham
    varchar duong_dan
    text mo_ta
    text thanh_phan
    text huong_dan_su_dung
    varchar loai_da_phu_hop
    boolean dang_hoat_dong
    date ngay_tao
  }
  bien_the_san_pham {
    bigint ma_bien_the PK
    bigint ma_san_pham
    varchar sku
    varchar ten_bien_the
    decimal gia_ban
    int so_luong_trong_kho
    int so_luong_tam_giu
    varchar url_hinh_anh
    varchar ma_lo_minh_hoa
    date han_su_dung_minh_hoa
    boolean dang_hoat_dong
  }
  gio_hang {
    bigint ma_gio_hang PK
    bigint ma_nguoi_dung
    date ngay_cap_nhat
  }
  chi_tiet_gio_hang {
    bigint ma_chi_tiet_gio_hang PK
    bigint ma_gio_hang
    bigint ma_bien_the
    int so_luong
  }
  phieu_giam_gia {
    bigint ma_phieu_giam_gia PK
    varchar ma_ap_dung
    varchar loai_giam_gia
    decimal gia_tri_giam
    decimal muc_giam_toi_da
    decimal don_toi_thieu
    int gioi_han_su_dung
    int so_luot_da_dung
    int so_luot_tam_giu
    date bat_dau_luc
    date ket_thuc_luc
    boolean dang_hoat_dong
  }
  don_hang {
    bigint ma_don_hang PK
    varchar ma_don_hang_hien_thi
    bigint ma_nguoi_dung
    bigint ma_phieu_giam_gia
    varchar ten_nguoi_nhan
    varchar sdt_nguoi_nhan
    varchar dia_chi_khi_dat
    decimal tam_tinh
    decimal so_tien_giam
    decimal phi_van_chuyen
    decimal tong_thanh_toan
    varchar trang_thai
    varchar phuong_thuc_thanh_toan
    date ngay_tao
    date ngay_giao_thanh_cong
  }
  chi_tiet_don_hang {
    bigint ma_chi_tiet_don_hang PK
    bigint ma_don_hang
    bigint ma_bien_the
    varchar sku_khi_dat
    varchar ten_san_pham_khi_dat
    varchar ten_bien_the_khi_dat
    decimal don_gia
    int so_luong
  }
  tam_giu_phieu_giam_gia {
    bigint ma_giu_phieu_giam_gia PK
    bigint ma_phieu_giam_gia
    bigint ma_don_hang
    varchar trang_thai
    date ngay_tao
  }
  tam_giu_ton_kho {
    bigint ma_giu_ton_kho PK
    bigint ma_don_hang
    bigint ma_bien_the
    int so_luong
    varchar trang_thai
    date het_han_luc
    date ngay_tao
  }
  lan_thanh_toan {
    bigint ma_lan_thanh_toan PK
    bigint ma_don_hang
    varchar ma_yeu_cau
    varchar ma_giao_dich_cong
    varchar cong_thanh_toan
    decimal so_tien
    varchar trang_thai
    date ngay_tao
    date ngay_thanh_toan
  }
  van_chuyen {
    bigint ma_van_chuyen PK
    bigint ma_don_hang
    varchar trang_thai
    date ngay_gui_hang
    date ngay_giao_thanh_cong
  }
  danh_gia {
    bigint ma_danh_gia PK
    bigint ma_nguoi_dung
    bigint ma_san_pham
    bigint ma_chi_tiet_don_hang
    int so_sao
    text noi_dung
    boolean hien_thi
    date ngay_tao
  }
  san_pham_yeu_thich {
    bigint ma_yeu_thich PK
    bigint ma_nguoi_dung
    bigint ma_san_pham
  }
  yeu_cau_doi_tra {
    bigint ma_yeu_cau_doi_tra PK
    bigint ma_don_hang
    bigint ma_nguoi_dung
    varchar trang_thai
    text ly_do
    date ngay_yeu_cau
  }
  chi_tiet_doi_tra {
    bigint ma_chi_tiet_doi_tra PK
    bigint ma_yeu_cau_doi_tra
    bigint ma_chi_tiet_don_hang
    int so_luong
  }
  bai_viet_faq {
    bigint ma_faq PK
    varchar cau_hoi
    text cau_tra_loi
    varchar url_nguon
    date ngay_cap_nhat
    boolean dang_hoat_dong
  }
  nguoi_dung ||--o{ dia_chi_nguoi_dung : "ma_nguoi_dung"
  danh_muc ||--o{ san_pham : "ma_danh_muc"
  thuong_hieu ||--o{ san_pham : "ma_thuong_hieu"
  san_pham ||--o{ bien_the_san_pham : "ma_san_pham"
  nguoi_dung ||--o{ gio_hang : "ma_nguoi_dung"
  gio_hang ||--o{ chi_tiet_gio_hang : "ma_gio_hang"
  bien_the_san_pham ||--o{ chi_tiet_gio_hang : "ma_bien_the"
  nguoi_dung ||--o{ don_hang : "ma_nguoi_dung"
  phieu_giam_gia ||--o{ don_hang : "ma_phieu_giam_gia"
  don_hang ||--o{ chi_tiet_don_hang : "ma_don_hang"
  bien_the_san_pham ||--o{ chi_tiet_don_hang : "ma_bien_the"
  phieu_giam_gia ||--o{ tam_giu_phieu_giam_gia : "ma_phieu_giam_gia"
  don_hang ||--o{ tam_giu_phieu_giam_gia : "ma_don_hang"
  don_hang ||--o{ tam_giu_ton_kho : "ma_don_hang"
  bien_the_san_pham ||--o{ tam_giu_ton_kho : "ma_bien_the"
  don_hang ||--o{ lan_thanh_toan : "ma_don_hang"
  don_hang ||--o{ van_chuyen : "ma_don_hang"
  nguoi_dung ||--o{ danh_gia : "ma_nguoi_dung"
  san_pham ||--o{ danh_gia : "ma_san_pham"
  chi_tiet_don_hang ||--o{ danh_gia : "ma_chi_tiet_don_hang"
  nguoi_dung ||--o{ san_pham_yeu_thich : "ma_nguoi_dung"
  san_pham ||--o{ san_pham_yeu_thich : "ma_san_pham"
  don_hang ||--o{ yeu_cau_doi_tra : "ma_don_hang"
  nguoi_dung ||--o{ yeu_cau_doi_tra : "ma_nguoi_dung"
  yeu_cau_doi_tra ||--o{ chi_tiet_doi_tra : "ma_yeu_cau_doi_tra"
  chi_tiet_don_hang ||--o{ chi_tiet_doi_tra : "ma_chi_tiet_don_hang"
```

Chi tiết khóa ngoại, UNIQUE, CHECK xem trực tiếp SQL. Bảng và cột xem `docs/data/bang_doi_chieu_ten.csv`.

P02 bổ sung [V2](../../src/main/resources/db/migration/V2__ghi_nhan_tru_kho_cod.sql) với bảng `ghi_nhan_tru_kho_cod`. Khóa chính ghép `(ma_don_hang, ma_bien_the)` đồng thời tham chiếu `don_hang` và `bien_the_san_pham`. Cột `so_luong` ghi số lượng đã trừ cho lần COD để retry an toàn.

P04 bổ sung [V3](../../src/main/resources/db/migration/V3__ghi_nhan_gop_gio_hang.sql) với bảng `ghi_nhan_gop_gio_hang`. Khóa chính `ma_gop` là mã snapshot giỏ Guest và `ma_nguoi_dung` tham chiếu tài khoản nhận giỏ.

P04-OTP giữ trạng thái xác thực tạm trong HTTP session và Redis. Tài khoản chỉ được ghi vào `nguoi_dung` sau xác thực email. Schema MariaDB giữ nguyên.

P04-OAuth2 bổ sung [V6](../../src/main/resources/db/migration/V6__them_danh_tinh_oauth.sql). Cặp `nha_cung_cap_oauth` và `ma_danh_tinh_oauth` của `nguoi_dung` là duy nhất khi có giá trị. Tài khoản dùng mật khẩu có hai cột này rỗng.

P05 dùng khóa ngoại địa chỉ trong V1. [V4](../../src/main/resources/db/migration/V4__them_phien_ban_xac_thuc.sql) thêm `nguoi_dung.phien_ban_xac_thuc` để khóa tài khoản thu hồi phiên đăng nhập cũ.

P23 không đổi schema MariaDB. Nội dung đánh giá (sao, bình luận, trạng thái duyệt, phiên bản) lưu ở MongoDB, collection `danh_gia`, khóa duy nhất `maChiTietDonHang` (mã dòng đơn MariaDB). Điều kiện đánh giá BR16 vẫn kiểm trên các bảng MariaDB `chi_tiet_don_hang`, `don_hang` trước khi ghi. Bảng MariaDB `danh_gia` được giữ làm nguồn chuyển dữ liệu một lần và làm store dự phòng (`app.reviews.store=mariadb`). Khi store là `mongo`, app không ghi vào bảng này. Cấu trúc document và index xem [báo cáo P23](../reviews/P23-danh-gia-mongodb.md).
