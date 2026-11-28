# Run from the project root after packaging, with JAVA_HOME set to JDK 21.
$ErrorActionPreference = 'Stop'
$fixture = Get-Content -Raw -Encoding UTF8 docs/qa/CART-01/run-fixture.ps1
$fixture = $fixture.Replace('C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe', "$env:JAVA_HOME/bin/java.exe")
$entry = '& node docs/qa/CART-01/browser-check.mjs $site $scratch'
if (!$fixture.Contains($entry)) { throw 'CART-01 browser entry point has changed' }
$check = @'
    & "$maria/mariadb.exe" @sql cart_01_test --execute="
        INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro)
        SELECT 'admin@example.invalid', mat_khau_bam, 'Fixture Admin', 'ADMIN' FROM nguoi_dung WHERE ma_nguoi_dung=1;
        INSERT INTO don_hang (ma_don_hang, ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan,
            dia_chi_khi_dat, tam_tinh, so_tien_giam, phi_van_chuyen, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_tao)
        VALUES (301,'PAYMENT-301',1,'Fixture','0000000000','Fixture',100000,0,0,100000,'PENDING_CONFIRMATION','MOMO',UTC_TIMESTAMP()),
               (302,'PAYMENT-302',1,'Fixture','0000000000','Fixture',100000,0,0,100000,'PENDING_CONFIRMATION','COD',UTC_TIMESTAMP()),
               (303,'PAYMENT-303',1,'Fixture','0000000000','Fixture',100000,0,0,100000,'DELIVERED','COD',UTC_TIMESTAMP());
        INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_tao)
        VALUES (301,'00000000-0000-4000-8000-000000000001','MOMO_MOCK',100000,'FAILED','2026-11-27 20:01:02'),
               (301,'00000000-0000-4000-8000-000000000002','MOMO_MOCK',100000,'PAID','2026-11-27 20:01:02'),
               (302,'COD-302','COD',100000,'UNPAID',UTC_TIMESTAMP());
        UPDATE lan_thanh_toan SET ma_giao_dich_cong='78012345', ngay_thanh_toan='2026-11-27 20:05:00'
        WHERE ma_yeu_cau='00000000-0000-4000-8000-000000000002';"
    if ($LASTEXITCODE) { throw 'Order fixture failed' }
    & node docs/qa/PAYMENT/order-detail.cjs $site
'@
& ([scriptblock]::Create($fixture.Replace($entry, $check)))
