<#
.SYNOPSIS
    P20: tạo đơn hàng demo (80–150 đơn trong 90 ngày) cho Dashboard Admin trên database demo.

.DESCRIPTION
    Chỉ chạy với database khớp ^beautyshop_demo(?:_[a-z0-9]+)?$ và đã có catalog demo (scripts/seed-demo.ps1).
    Tái hiện đúng các bản ghi mà luồng ứng dụng tạo ra, trong MỘT transaction:
      - COD: don_hang PENDING_CONFIRMATION + chi_tiet_don_hang + lan_thanh_toan COD-<id> UNPAID,
        trừ so_luong_trong_kho và ghi ghi_nhan_tru_kho_cod; hủy -> cộng trả kho (bản ghi V2 giữ nguyên);
        gửi hàng -> van_chuyen SHIPPING; giao -> van_chuyen DELIVERED, don_hang.ngay_giao_thanh_cong, thanh toán PAID.
      - MoMo (cổng MOMO_MOCK): tam_giu_ton_kho; thanh toán PAID -> CONSUMED và trừ tồn, đơn chờ xác nhận rồi xử lý tiếp;
        thanh toán FAILED -> RELEASED, đơn EXPIRED (tồn không đổi), giống ThanhToanMomo_Service.ketThucDon.
      - Không áp voucher. Phí giao hàng theo ShippingFee_Policy (25.000 ₫, miễn phí từ 500.000 ₫). Giá = gia_ban hiện hành.
      - Đánh giá demo chỉ cho dòng hàng của đơn DELIVERED thuộc chính người mua, tối đa một lần mỗi dòng, sau ngày giao.
    Đánh dấu demo: mã đơn DEMO-ORD-NNN, tên người nhận/địa chỉ/nội dung đánh giá bắt đầu "[DEMO]"
    (don_hang không có cột ghi chú). Chạy lại: nếu đã có đơn DEMO-ORD-% thì dừng, không tạo trùng.
    Tồn kho: kế hoạch lập từ tồn khả dụng hiện tại, mỗi SKU giữ lại tối thiểu 2; CHECK của schema
    (tồn >= 0, tạm giữ <= tồn) chặn mọi sai lệch và lỗi bất kỳ làm rollback toàn bộ transaction.
    Mật khẩu khách demo: chuỗi ngẫu nhiên dạng bcrypt sinh bằng RandomNumberGenerator, không in và không ghi ra file;
    không ai biết mật khẩu gốc nên các tài khoản này không đăng nhập được (chỉ phục vụ thống kê).

.EXAMPLE
    $env:DB_PASSWORD = '<mật khẩu DB local>'   # chỉ trong phiên hiện tại
    ./scripts/seed-demo-orders.ps1 -Database beautyshop_demo -User beautyshop
#>
param(
    [Parameter(Mandatory)]
    [ValidatePattern('^beautyshop_demo(?:_[a-z0-9]+)?$')]
    [string] $Database,
    [Parameter(Mandatory)]
    [string] $User,
    [ValidateRange(1, 65535)]
    [int] $Port = 3306,
    [string] $Client = 'mariadb.exe',
    [ValidateRange(80, 150)]
    [int] $OrderCount = 120,
    [ValidateRange(5, 8)]
    [int] $CustomerCount = 6,
    [int] $Seed = 20261123
)

$ErrorActionPreference = 'Stop'
if ($Database -cnotmatch '^beautyshop_demo(?:_[a-z0-9]+)?$') { throw 'Only beautyshop_demo databases are allowed.' }
if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD for the local demo database.' }

function Invoke-DemoSql([string] $Sql, [switch] $Query) {
    $oldPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $env:DB_PASSWORD
        $cliArgs = @('--no-defaults', '--protocol=tcp', '--skip-ssl', '-h', '127.0.0.1', '-P', $Port, '-u', $User,
            '--default-character-set=utf8mb4')
        if ($Query) { $cliArgs += @('-N', '-B') }
        $output = $Sql | & $Client @cliArgs $Database
        if ($LASTEXITCODE -ne 0) { throw "MariaDB command failed (exit $LASTEXITCODE)." }
        return $output
    } finally {
        $env:MYSQL_PWD = $oldPassword
    }
}

function SqlText([string] $Value) {
    $hex = -join ([Text.Encoding]::UTF8.GetBytes($Value) | ForEach-Object { $_.ToString('x2') })
    return "CONVERT(X'$hex' USING utf8mb4)"
}

function FromHex([string] $Hex) {
    $bytes = New-Object byte[] ($Hex.Length / 2)
    for ($i = 0; $i -lt $bytes.Length; $i++) { $bytes[$i] = [Convert]::ToByte($Hex.Substring($i * 2, 2), 16) }
    return [Text.Encoding]::UTF8.GetString($bytes)
}

function New-RandomBcryptShape {
    $alphabet = './ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    $bytes = New-Object byte[] 53
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return '$2a$10$' + (-join ($bytes | ForEach-Object { $alphabet[$_ % 64] }))
}

$existing = [int](Invoke-DemoSql "SELECT COUNT(*) FROM don_hang WHERE ma_don_hang_hien_thi LIKE 'DEMO-ORD-%';" -Query)
if ($existing -gt 0) {
    Write-Output "Demo orders already exist ($existing). Nothing to do."
    return
}

$skuRows = Invoke-DemoSql @'
SELECT bt.ma_bien_the, bt.ma_san_pham, bt.sku, HEX(sp.ten_san_pham), HEX(bt.ten_bien_the), bt.gia_ban,
       bt.so_luong_trong_kho - bt.so_luong_tam_giu
FROM bien_the_san_pham bt JOIN san_pham sp ON sp.ma_san_pham = bt.ma_san_pham
WHERE bt.dang_hoat_dong = TRUE AND sp.dang_hoat_dong = TRUE AND bt.sku LIKE 'DEMO-%'
ORDER BY bt.ma_bien_the;
'@ -Query
$skus = @($skuRows | ForEach-Object {
    $c = $_ -split "`t"
    [pscustomobject]@{ Id = [long]$c[0]; ProductId = [long]$c[1]; Sku = $c[2]; Product = FromHex $c[3]; Variant = FromHex $c[4]
        Price = [decimal]$c[5]; Budget = [Math]::Max(0, [int]$c[6] - 2) }
})
if ($skus.Count -lt 10) { throw 'Demo catalog missing: run scripts/seed-demo.ps1 first.' }

$rand = New-Object Random $Seed
$statements = New-Object 'Collections.Generic.List[string]'
$statements.Add('START TRANSACTION;')

# Customers are created at different ages so "Khách hàng mới" has data in several periods.
# nguoi_dung.ngay_tao follows the column default (DB-local CURRENT_TIMESTAMP), so it is written in DB-local time.
$customerAges = @(150, 110, 84, 60, 41, 22, 12, 4) | Select-Object -First $CustomerCount
for ($k = 1; $k -le $CustomerCount; $k++) {
    $email = "demo-khach-$k@example.test"
    $name = SqlText "[DEMO] Khách demo $k"
    $phone = '09000000{0:D2}' -f $k
    $age = $customerAges[$k - 1]
    $statements.Add(@"
INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, so_dien_thoai, vai_tro, kich_hoat, ngay_tao)
SELECT '$email', '$(New-RandomBcryptShape)', $name, '$phone', 'CUSTOMER', TRUE, CURRENT_TIMESTAMP(6) - INTERVAL $age DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nguoi_dung WHERE email = '$email');
SET @kh$k = (SELECT ma_nguoi_dung FROM nguoi_dung WHERE email = '$email');
"@)
}

$counts = @{}
$reviews = 0
$address = SqlText '[DEMO] Địa chỉ mô phỏng, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh'
$reviewTexts = @{
    5 = 'Sản phẩm dùng rất thích, giao hàng nhanh.'; 4 = 'Chất lượng tốt, đóng gói cẩn thận.'
    3 = 'Tạm ổn, cần dùng thêm để đánh giá.'; 2 = 'Kết cấu chưa hợp với da mình.'; 1 = 'Không như mong đợi.'
}
for ($n = 1; $n -le $OrderCount; $n++) {
    $code = 'DEMO-ORD-{0:D3}' -f $n
    # Spread over the last 90 days, denser in recent weeks.
    $daysAgo = [int][Math]::Floor(90 * [Math]::Pow($rand.NextDouble(), 1.35))
    if ($daysAgo -gt 89) { $daysAgo = 89 }
    $minutesAgo = $daysAgo * 1440 + $rand.Next(60, 1440)
    $eligible = @(1..$CustomerCount | Where-Object { $customerAges[$_ - 1] * 1440 -gt $minutesAgo + 1440 })
    if ($eligible.Count -eq 0) { $eligible = @(1) }
    $customer = $eligible[$rand.Next($eligible.Count)]
    $momo = $rand.NextDouble() -lt 0.35

    if ($daysAgo -ge 6) {
        $status = if ($rand.NextDouble() -lt 0.14) { if ($momo) { 'EXPIRED' } else { 'CANCELLED' } } else { 'DELIVERED' }
    } elseif ($daysAgo -ge 3) {
        $status = @('SHIPPING', 'SHIPPING', 'PACKING', 'DELIVERED', 'CANCELLED')[$rand.Next(5)]
        if ($status -eq 'DELIVERED' -and $daysAgo -lt 4) { $status = 'SHIPPING' }
    } else {
        $status = @('PENDING_CONFIRMATION', 'PENDING_CONFIRMATION', 'CONFIRMED', 'PACKING', 'CANCELLED')[$rand.Next(5)]
    }
    # A paid MoMo order cannot be cancelled without the refund flow, so an unsuccessful MoMo order is EXPIRED instead.
    if ($status -eq 'CANCELLED' -and $momo) { $status = 'EXPIRED' }
    # COD cancellation is only allowed before PACKING; CANCELLED here means cancelled while pending/confirmed.
    $consumes = $status -notin @('CANCELLED', 'EXPIRED')

    # 1–3 distinct SKUs, quantity 1–2, never beyond the planned budget (current available − 2).
    $lines = @()
    $want = 1 + [int][Math]::Floor([Math]::Pow($rand.NextDouble(), 1.6) * 3)
    $tries = 0
    while ($lines.Count -lt $want -and $tries -lt 40) {
        $tries++
        $sku = $skus[$rand.Next($skus.Count)]
        if (@($lines | Where-Object { $_.Sku.Id -eq $sku.Id }).Count -gt 0) { continue }
        $qty = if ($rand.NextDouble() -lt 0.25) { 2 } else { 1 }
        if ($sku.Budget -lt $qty) { continue }
        if ($consumes) { $sku.Budget -= $qty }
        $lines += [pscustomobject]@{ Sku = $sku; Qty = $qty }
    }
    if ($lines.Count -eq 0) { continue }

    $subtotal = [decimal]0
    foreach ($line in $lines) { $subtotal += $line.Sku.Price * $line.Qty }
    $fee = if ($subtotal -ge 500000) { [decimal]0 } else { [decimal]25000 }
    $total = $subtotal + $fee
    $created = "UTC_TIMESTAMP(6) - INTERVAL $minutesAgo MINUTE"
    $shipMinutes = [Math]::Max(30, $minutesAgo - 1440 - $rand.Next(0, 600))
    $deliverMinutes = [Math]::Max(20, $minutesAgo - 2 * 1440 - $rand.Next(0, 1440))
    $receiver = SqlText "[DEMO] Khách demo $customer"
    $phone = '09000000{0:D2}' -f $customer
    $initial = if ($momo) { 'PENDING_PAYMENT' } else { 'PENDING_CONFIRMATION' }
    $method = if ($momo) { 'MOMO' } else { 'COD' }
    $statements.Add(@"
INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_khi_dat,
    tam_tinh, so_tien_giam, phi_van_chuyen, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_tao)
VALUES ('$code', @kh$customer, $receiver, '$phone', $address, $subtotal, 0, $fee, $total, '$initial', '$method', $created);
SET @d = LAST_INSERT_ID();
"@)
    $i = 0
    foreach ($line in $lines) {
        $i++
        $s = $line.Sku
        $statements.Add(@"
INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat, don_gia, so_luong)
VALUES (@d, $($s.Id), '$($s.Sku)', $(SqlText $s.Product), $(SqlText $s.Variant), $($s.Price), $($line.Qty));
SET @c$i = LAST_INSERT_ID();
"@)
        if ($momo) {
            $hold = if ($consumes) { 'CONSUMED' } else { 'RELEASED' }
            $statements.Add("INSERT INTO tam_giu_ton_kho (ma_don_hang, ma_bien_the, so_luong, trang_thai, het_han_luc, ngay_tao) VALUES (@d, $($s.Id), $($line.Qty), '$hold', $created + INTERVAL 15 MINUTE, $created);")
        } else {
            $statements.Add("INSERT INTO ghi_nhan_tru_kho_cod (ma_don_hang, ma_bien_the, so_luong) VALUES (@d, $($s.Id), $($line.Qty));")
        }
        if ($consumes) {
            # Net effect of COD deduction / MoMo HELD->CONSUMED. Cancelled COD returns its stock, so it nets to zero.
            $statements.Add("UPDATE bien_the_san_pham SET so_luong_trong_kho = so_luong_trong_kho - $($line.Qty) WHERE ma_bien_the = $($s.Id);")
        }
    }
    if ($momo) {
        $requestId = [guid]::NewGuid().ToString()
        if ($consumes) {
            $tx = 'DEMO-TX-{0:D3}' -f $n
            $statements.Add("INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, ma_giao_dich_cong, cong_thanh_toan, so_tien, trang_thai, ngay_tao, ngay_thanh_toan) VALUES (@d, '$requestId', '$tx', 'MOMO_MOCK', $total, 'PAID', $created, $created + INTERVAL 3 MINUTE);")
        } else {
            $statements.Add("INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_tao) VALUES (@d, '$requestId', 'MOMO_MOCK', $total, 'FAILED', $created);")
        }
    } else {
        if ($status -eq 'DELIVERED') {
            $statements.Add("INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_tao, ngay_thanh_toan) VALUES (@d, CONCAT('COD-', @d), 'COD', $total, 'PAID', $created, UTC_TIMESTAMP(6) - INTERVAL $deliverMinutes MINUTE);")
        } else {
            $statements.Add("INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_tao) VALUES (@d, CONCAT('COD-', @d), 'COD', $total, 'UNPAID', $created);")
        }
    }
    if ($status -eq 'SHIPPING') {
        $statements.Add("INSERT INTO van_chuyen (ma_don_hang, trang_thai, ngay_gui_hang) VALUES (@d, 'SHIPPING', UTC_TIMESTAMP(6) - INTERVAL $shipMinutes MINUTE);")
    } elseif ($status -eq 'DELIVERED') {
        $statements.Add("INSERT INTO van_chuyen (ma_don_hang, trang_thai, ngay_gui_hang, ngay_giao_thanh_cong) VALUES (@d, 'DELIVERED', UTC_TIMESTAMP(6) - INTERVAL $shipMinutes MINUTE, UTC_TIMESTAMP(6) - INTERVAL $deliverMinutes MINUTE);")
        $statements.Add("UPDATE don_hang SET ngay_giao_thanh_cong = UTC_TIMESTAMP(6) - INTERVAL $deliverMinutes MINUTE WHERE ma_don_hang = @d;")
        # Reviews: only delivered items of the buyer's own order, at most once per order item, after delivery.
        for ($j = 1; $j -le $lines.Count; $j++) {
            if ($rand.NextDouble() -ge 0.45 -or $deliverMinutes -lt 1500) { continue }
            $stars = @(5, 5, 5, 4, 4, 4, 3, 2, 1)[$rand.Next(9)]
            $visible = if ($rand.NextDouble() -lt 0.75) { 'TRUE' } else { 'FALSE' }
            $reviewMinutes = $deliverMinutes - $rand.Next(60, 1440)
            $text = SqlText ('[DEMO] ' + $reviewTexts[$stars])
            $statements.Add("INSERT INTO danh_gia (ma_nguoi_dung, ma_san_pham, ma_chi_tiet_don_hang, so_sao, noi_dung, hien_thi, ngay_tao) VALUES (@kh$customer, $($lines[$j - 1].Sku.ProductId), @c$j, $stars, $text, $visible, UTC_TIMESTAMP(6) - INTERVAL $reviewMinutes MINUTE);")
            $reviews++
        }
    }
    $statements.Add("UPDATE don_hang SET trang_thai = '$status' WHERE ma_don_hang = @d;")
    $counts[$status] = 1 + [int]$counts[$status]
}
$statements.Add('COMMIT;')

Invoke-DemoSql ($statements -join "`n") | Out-Null
$total = ($counts.Values | Measure-Object -Sum).Sum
$summary = ($counts.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ', '
Write-Output "Demo orders created: $total ($summary); reviews: $reviews; customers: $CustomerCount."
$check = Invoke-DemoSql 'SELECT COUNT(*), SUM(so_luong_trong_kho < 0), SUM(so_luong_tam_giu > so_luong_trong_kho), MIN(so_luong_trong_kho) FROM bien_the_san_pham;' -Query
Write-Output "Stock check (SKUs, negative, reserved>onHand, min onHand): $check"
