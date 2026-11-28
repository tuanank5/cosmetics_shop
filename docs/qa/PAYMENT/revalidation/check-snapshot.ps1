# Run inside the isolated merge checkout, after package.
$runner = Get-Content -Raw -Encoding UTF8 docs/qa/VOUCHER/run-browser.ps1
$marker = "    Write-Output 'COD orders and attempts"
if (!$runner.Contains($marker)) { throw 'Fixture entry changed' }
$probe = @'
    function Read-Snapshot {
        $rows = & "$maria/mariadb.exe" @sql -NB cart_01_test --execute="
          SELECT c.don_gia,c.so_luong,c.giam_hang,d.tam_tinh,d.so_tien_giam,d.phi_chuan,d.giam_van_chuyen,d.tong_thanh_toan,p.so_tien
          FROM chi_tiet_don_hang c JOIN don_hang d USING(ma_don_hang) JOIN lan_thanh_toan p USING(ma_don_hang)
          WHERE d.ma_don_hang=(SELECT MIN(ma_don_hang) FROM don_hang) ORDER BY c.ma_bien_the;
          SELECT slot,so_tien_giam,trang_thai FROM tam_giu_phieu_giam_gia
          WHERE ma_don_hang=(SELECT MIN(ma_don_hang) FROM don_hang) ORDER BY slot;"
        if ($LASTEXITCODE) { throw 'Snapshot read failed' }
        ($rows -join '|').Replace([char]9,[char]44)
    }
    $before = Read-Snapshot
    if ($before -ne '370000,1,21346,520000,30000,25000,10000,505000,505000|150000,1,8654,520000,30000,25000,10000,505000,505000|HANG,30000,CONSUMED|VAN_CHUYEN,10000,CONSUMED') { throw "Unexpected snapshot $before" }
    & "$maria/mariadb.exe" @sql cart_01_test --execute="UPDATE phieu_giam_gia SET gia_tri_giam=1,ket_thuc_luc=UTC_TIMESTAMP()-INTERVAL 1 DAY,gioi_han_su_dung=20 WHERE ma_ap_dung IN ('H30','S10'); SELECT ROW_COUNT();"
    if ($LASTEXITCODE) { throw 'Fixture update failed' }
    if ((Read-Snapshot) -ne $before) { throw 'Historic snapshot changed' }
    Write-Output "SQL before and after changing value, expiry, quota PASS $before"
'@
& ([scriptblock]::Create($runner.Replace($marker, $probe + [Environment]::NewLine + $marker)))
