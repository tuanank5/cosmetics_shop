-- Executed only inside a fresh private datadir after Flyway.
INSERT INTO nguoi_dung(email,mat_khau_bam,ho_ten,vai_tro)
SELECT CONCAT('perf',seq,'@example.invalid'),'__BCRYPT__',CONCAT('Perf ',seq),IF(seq<=6,'CUSTOMER','ADMIN') FROM seq_1_to_8;
INSERT INTO dia_chi_nguoi_dung(ma_nguoi_dung,ten_nguoi_nhan,so_dien_thoai,dia_chi_chi_tiet,la_mac_dinh)
SELECT seq,'Perf','0000000000','Fixture',TRUE FROM seq_1_to_6;
INSERT INTO danh_muc(ten_danh_muc,duong_dan,dang_hoat_dong)
SELECT CONCAT('Category ',seq),CONCAT('cat-',seq),MOD(seq,10)<>0 FROM seq_1_to_24;
INSERT INTO thuong_hieu(ten_thuong_hieu,duong_dan,dang_hoat_dong)
SELECT CONCAT('Brand ',seq),CONCAT('brand-',seq),MOD(seq,10)<>0 FROM seq_1_to_40;
INSERT INTO san_pham(ma_danh_muc,ma_thuong_hieu,ten_san_pham,duong_dan,dang_hoat_dong)
SELECT 1+MOD(seq-1,24),1+MOD(FLOOR((seq-1)/3),40),CONCAT(IF(MOD(seq,3)=1,'Serum ','Cream '),seq),
       CONCAT('perf-',seq),MOD(seq,10)<>0 FROM seq_1_to_1200;
INSERT INTO bien_the_san_pham(ma_san_pham,sku,ten_bien_the,gia_ban,so_luong_trong_kho,dang_hoat_dong,url_hinh_anh)
SELECT CEIL(seq/3),CONCAT('PF-',seq),CONCAT('Size ',MOD(seq,3)),90000+MOD(seq,23)*1000,40,MOD(seq,10)<>0,
       CONCAT('/uploads/sku/00000000-0000-4000-8000-',LPAD(CEIL(seq/3),12,'0'),'.png') FROM seq_1_to_3600;
INSERT INTO gio_hang(ma_nguoi_dung) SELECT seq FROM seq_1_to_6;
INSERT INTO chi_tiet_gio_hang(ma_gio_hang,ma_bien_the,so_luong)
SELECT g.ma_gio_hang,v.id,1 FROM gio_hang g JOIN (
    SELECT b.ma_bien_the id,ROW_NUMBER() OVER (ORDER BY b.ma_bien_the) n
    FROM bien_the_san_pham b JOIN san_pham p USING(ma_san_pham)
    JOIN danh_muc c USING(ma_danh_muc) JOIN thuong_hieu h USING(ma_thuong_hieu)
    WHERE b.dang_hoat_dong AND p.dang_hoat_dong AND c.dang_hoat_dong AND h.dang_hoat_dong
) v ON v.n <= CASE WHEN g.ma_nguoi_dung<=2 THEN 1 WHEN g.ma_nguoi_dung<=4 THEN 10 ELSE 100 END;
INSERT INTO don_hang(ma_don_hang_hien_thi,ma_nguoi_dung,ten_nguoi_nhan,sdt_nguoi_nhan,dia_chi_khi_dat,
tam_tinh,so_tien_giam,phi_van_chuyen,tong_thanh_toan,trang_thai,phuong_thuc_thanh_toan)
SELECT CONCAT('PERF-',seq),1+MOD(seq,6),'Perf','0000000000','Fixture',100000,0,0,100000,
IF(MOD(seq,4)=0,'PENDING_CONFIRMATION','DELIVERED'),'COD' FROM seq_1_to_480;
INSERT INTO chi_tiet_don_hang(ma_don_hang,ma_bien_the,sku_khi_dat,ten_san_pham_khi_dat,ten_bien_the_khi_dat,don_gia,so_luong)
SELECT seq,seq,CONCAT('PF-',seq),'Fixture product','Fixture size',100000,1 FROM seq_1_to_480;
INSERT INTO lan_thanh_toan(ma_don_hang,ma_yeu_cau,cong_thanh_toan,so_tien,trang_thai)
SELECT seq,CONCAT('perf-cod-',seq),'COD',100000,IF(MOD(seq,4)=0,'UNPAID','PAID') FROM seq_1_to_480;
