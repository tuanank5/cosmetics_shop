# Run from the project root after packaging, with JAVA_HOME set to JDK 21.
$ErrorActionPreference = 'Stop'
$fixture = Get-Content -Raw -Encoding UTF8 docs/qa/CART-01/run-fixture.ps1
$fixture = $fixture.Replace('C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe', "$env:JAVA_HOME/bin/java.exe")
$fixture = $fixture.Replace('(1, 1, 7)', '(1, 1, 1)').Replace('100000, 10','370000, 10')
$entry = '& node docs/qa/CART-01/browser-check.mjs $site $scratch'
if (!$fixture.Contains($entry)) { throw 'CART-01 browser entry point has changed' }
$check = @'
    & "$maria/mariadb.exe" @sql cart_01_test --execute="
        INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro)
        SELECT 'admin@example.invalid', mat_khau_bam, 'Fixture Admin', 'ADMIN' FROM nguoi_dung WHERE ma_nguoi_dung=1;
        INSERT INTO nguoi_dung(email,mat_khau_bam,ho_ten,vai_tro)
        SELECT CONCAT(x.n,'@example.invalid'),b.mat_khau_bam,'Fixture','CUSTOMER'
        FROM nguoi_dung b CROSS JOIN (SELECT 'js' AS n UNION ALL SELECT 'ssr') x WHERE b.ma_nguoi_dung=1;
        INSERT INTO gio_hang(ma_nguoi_dung) SELECT ma_nguoi_dung FROM nguoi_dung WHERE email IN ('js@example.invalid','ssr@example.invalid');
        INSERT INTO chi_tiet_gio_hang(ma_gio_hang,ma_bien_the,so_luong)
        SELECT g.ma_gio_hang,b.ma_bien_the,1 FROM gio_hang g JOIN nguoi_dung n USING(ma_nguoi_dung)
        CROSS JOIN bien_the_san_pham b WHERE n.email IN ('js@example.invalid','ssr@example.invalid');
        INSERT INTO dia_chi_nguoi_dung(ma_nguoi_dung,ten_nguoi_nhan,so_dien_thoai,dia_chi_chi_tiet,la_mac_dinh)
        SELECT ma_nguoi_dung,'Fixture','0000000000','Fixture',TRUE FROM nguoi_dung WHERE email IN ('js@example.invalid','ssr@example.invalid');
        INSERT INTO phieu_giam_gia(ma_ap_dung,slot,loai_giam_gia,gia_tri_giam,gioi_han_su_dung,bat_dau_luc,ket_thuc_luc)
        VALUES ('H30','HANG','FIXED',30000,10,'2020-01-01','2099-01-01'),
               ('S10','VAN_CHUYEN','FIXED',10000,10,'2020-01-01','2099-01-01'),
               ('EXPIRED','HANG','FIXED',10000,10,'2020-01-01','2021-01-01');"
    if ($LASTEXITCODE) { throw 'Order fixture failed' }
    & node docs/qa/VOUCHER/browser.cjs $site
    if ($LASTEXITCODE) { throw 'Voucher WebKit failed' }
    $money = & "$maria/mariadb.exe" @sql -N -B cart_01_test --execute="
        SELECT CONCAT_WS(',',d.tong_thanh_toan,d.phi_chuan,d.giam_van_chuyen,COUNT(g.ma_don_hang),p.so_tien)
        FROM don_hang d JOIN lan_thanh_toan p USING(ma_don_hang)
        LEFT JOIN tam_giu_phieu_giam_gia g USING(ma_don_hang)
        GROUP BY d.ma_don_hang,d.tong_thanh_toan,d.phi_chuan,d.giam_van_chuyen,p.so_tien ORDER BY d.ma_don_hang;"
    if ($LASTEXITCODE -or ($money -join '|') -ne '505000,25000,10000,2,505000|505000,25000,10000,2,505000|860000,0,0,1,860000') {
        throw 'Order, attempt or slot reconciliation mismatch'
    }
    Write-Output 'COD orders and attempts 505000/505000/860000 VND, slot counts 2/2/1 PASS'
'@
& ([scriptblock]::Create($fixture.Replace($entry, $check)))
