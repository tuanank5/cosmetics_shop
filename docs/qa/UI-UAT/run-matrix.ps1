# Run from the isolated checkout after packaging with JDK 21.
$ErrorActionPreference = 'Stop'
$fixture = Get-Content -Raw -Encoding UTF8 docs/qa/CART-01/run-fixture.ps1
$entry = '& node docs/qa/CART-01/browser-check.mjs $site $scratch'
if (!$fixture.Contains($entry)) { throw 'CART-01 fixture entry changed' }
$fixture = $fixture.Replace('C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe', "$env:JAVA_HOME/bin/java.exe").Replace('33381','33483').Replace('18096','18189')
$check = @'
    & "$maria/mariadb.exe" @sql cart_01_test --execute="
      INSERT INTO nguoi_dung(email,mat_khau_bam,ho_ten,vai_tro)
      SELECT 'admin@example.invalid',mat_khau_bam,'Fixture Admin','ADMIN' FROM nguoi_dung WHERE ma_nguoi_dung=1;
      UPDATE san_pham SET ten_san_pham=CONCAT('LongNameFixture ',REPEAT('serum ',22)) WHERE ma_san_pham=1;
      INSERT INTO don_hang(ma_don_hang,ma_don_hang_hien_thi,ma_nguoi_dung,ten_nguoi_nhan,sdt_nguoi_nhan,dia_chi_khi_dat,tam_tinh,tong_thanh_toan,trang_thai,phuong_thuc_thanh_toan,ngay_giao_thanh_cong)
      SELECT seq,CONCAT('UAT-',seq),1,'Fixture','0000000000','Fixture',100000,100000,'DELIVERED','COD',UTC_TIMESTAMP() FROM seq_701_to_702;
      INSERT INTO chi_tiet_don_hang(ma_chi_tiet_don_hang,ma_don_hang,ma_bien_the,sku_khi_dat,ten_san_pham_khi_dat,ten_bien_the_khi_dat,don_gia,so_luong)
      SELECT seq,seq,1,'FIXTURE-A','Fixture','Fixture',100000,1 FROM seq_701_to_702;
      INSERT INTO yeu_cau_doi_tra(ma_yeu_cau_doi_tra,ma_don_hang,ma_nguoi_dung,trang_thai,ly_do) VALUES (701,701,1,'PENDING','Fixture');
      INSERT INTO chi_tiet_doi_tra(ma_yeu_cau_doi_tra,ma_chi_tiet_don_hang,so_luong) VALUES (701,701,1);
      INSERT INTO nhat_ky_quan_tri(ma_nhat_ky,ma_nguoi_thuc_hien,vai_tro_nguoi_thuc_hien,hanh_dong,loai_doi_tuong,ma_doi_tuong,thoi_gian,ket_qua,correlation_id)
      VALUES (701,3,'ADMIN','FIXTURE_READ','SKU',1,UTC_TIMESTAMP(),'SUCCESS',UUID());"
    if ($LASTEXITCODE) { throw 'UI-UAT seed failed' }
    & node docs/qa/UI-UAT/browser.cjs $site
    if ($LASTEXITCODE) { throw 'Matrix setup failed' }
    & node docs/qa/UI-UAT/native.cjs $site $scratch
    if ($LASTEXITCODE) { throw 'Chrome native zoom setup failed' }
    & "$maria/mariadb.exe" @sql cart_01_test --execute="
      INSERT INTO dia_chi_nguoi_dung(ma_nguoi_dung,ten_nguoi_nhan,so_dien_thoai,dia_chi_chi_tiet,la_mac_dinh)
      SELECT 1,'Fixture','0000000000',CONCAT('Fixture ',seq),FALSE FROM seq_1_to_8;
      UPDATE don_hang SET dia_chi_khi_dat='Fixture Address' WHERE ma_don_hang=701;"
    if ($LASTEXITCODE) { throw 'Address setup failed' }
    & node docs/qa/UI-UAT/journeys.cjs $site
    if ($LASTEXITCODE) { throw 'Browser journey failed' }
    $snapshot = & "$maria/mariadb.exe" @sql -N -B cart_01_test --execute="SELECT dia_chi_khi_dat FROM don_hang WHERE ma_don_hang=701"
    if ($LASTEXITCODE -or $snapshot -ne 'Fixture Address') { throw 'Snapshot mismatch' }
    Write-Output 'Final SQL order snapshot after address edit and delete PASS'
'@
& ([scriptblock]::Create($fixture.Replace($entry,$check)))
