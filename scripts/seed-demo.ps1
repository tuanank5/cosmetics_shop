param(
    [Parameter(Mandatory)]
    [ValidatePattern('^beautyshop_demo(?:_[a-z0-9]+)?$')]
    [string] $Database,
    [Parameter(Mandatory)]
    [string] $User,
    [ValidateRange(1, 65535)]
    [int] $Port = 3306,
    [string] $Client = 'mariadb.exe'
)

$ErrorActionPreference = 'Stop'
if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD for the local demo database.' }
$csv = Join-Path $PSScriptRoot '../docs/data/product-research.csv'
$rows = @(Import-Csv -LiteralPath $csv -Encoding UTF8 | Where-Object verified -eq 'yes')
$products = @($rows | Group-Object research_id)
if ($products.Count -lt 30 -or $products.Count -gt 50) { throw 'Expected 30 to 50 sourced products.' }
if (($rows.variant_sku | Sort-Object -Unique).Count -ne $rows.Count) { throw 'Duplicate demo SKU.' }

function SqlText([string] $Value) {
    $hex = -join ([Text.Encoding]::UTF8.GetBytes($Value) | ForEach-Object { $_.ToString('x2') })
    return "CONVERT(X'$hex' USING utf8mb4)"
}

# Demo images ship inside the JAR (static/img/products) so they survive redeploys; empty image_path -> NULL placeholder.
$imageDir = Join-Path $PSScriptRoot '../src/main/resources/static/img/products'
function ImageSql([string] $Path) {
    if ([string]::IsNullOrWhiteSpace($Path)) { return 'NULL' }
    if ($Path -cnotmatch '^/img/products/[a-z0-9](?:[a-z0-9-]{0,98}[a-z0-9])?\.(?:webp|jpg|png)$') { throw "Invalid image_path: $Path" }
    $file = Join-Path $imageDir ($Path.Substring('/img/products/'.Length))
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw "image_path not found in static/img/products: $Path" }
    if ((Get-Item -LiteralPath $file).Length -gt 300KB) { throw "Image larger than 300 KB: $Path" }
    return "'$Path'"
}

function Slug([string] $Value) {
    $ascii = $Value.Normalize([Text.NormalizationForm]::FormD).ToLowerInvariant() -replace '\p{Mn}', ''
    $ascii = $ascii.Replace([char]0x0111, [char]'d')
    return 'demo-' + ($ascii -replace '[^a-z0-9]+', '-').Trim('-')
}

$statements = [Collections.Generic.List[string]]::new()
$statements.Add('START TRANSACTION;')
foreach ($product in $products) {
    $row = $product.Group[0]
    $id = $row.research_id.ToLowerInvariant()
    if ($id -notmatch '^r[0-9]{3}$') { throw "Invalid product ID: $id" }
    $slug = "demo-$id"
    $brand = SqlText $row.brand
    $category = SqlText $row.category
    $brandSlug = SqlText (Slug $row.brand)
    $categorySlug = SqlText (Slug $row.category)
    $name = SqlText $row.product_name
    $description = SqlText ("{0} Giá và tồn dùng cho demo. Nguồn đối chiếu: {1} ({2})." -f $row.description, $row.source_url, $row.reviewed_at)
    $ingredients = if ($row.ingredients) { SqlText $row.ingredients } else { 'NULL' }
    $skin = if ($row.skin_type) { SqlText $row.skin_type } else { 'NULL' }
    $statements.Add("INSERT INTO danh_muc (ten_danh_muc, duong_dan) VALUES ($category, $categorySlug) ON DUPLICATE KEY UPDATE ma_danh_muc=ma_danh_muc;")
    $statements.Add("INSERT INTO thuong_hieu (ten_thuong_hieu, duong_dan) VALUES ($brand, $brandSlug) ON DUPLICATE KEY UPDATE ma_thuong_hieu=ma_thuong_hieu;")
    $statements.Add(@"
INSERT INTO san_pham (ma_danh_muc, ma_thuong_hieu, ten_san_pham, duong_dan, mo_ta, thanh_phan, loai_da_phu_hop)
SELECT dm.ma_danh_muc, th.ma_thuong_hieu, $name, '$slug', $description, $ingredients, $skin
FROM danh_muc dm CROSS JOIN thuong_hieu th
WHERE dm.duong_dan=$categorySlug AND th.duong_dan=$brandSlug
ON DUPLICATE KEY UPDATE ma_san_pham=ma_san_pham;
"@)
    foreach ($variant in $product.Group) {
        if ($variant.variant_sku -notmatch '^DEMO-[0-9]{3}(?:-[A-Z])?$' -or
            $variant.demo_price_vnd -notmatch '^[1-9][0-9]{0,8}$' -or
            $variant.source_url -notmatch '^https://' -or $variant.reviewed_at -notmatch '^\d{4}-\d{2}-\d{2}$') {
            throw "Invalid source or demo SKU: $($variant.variant_sku)"
        }
        $label = SqlText $variant.variant_label
        $sku = $variant.variant_sku
        $price = $variant.demo_price_vnd
        $stock = switch ($sku) { 'DEMO-013' { 0 } 'DEMO-004' { 1 } default { 12 } }
        $image = ImageSql $variant.image_path
        # Re-running the seed refreshes demo images only; an Admin-uploaded /uploads/sku image is never overwritten.
        $statements.Add(@"
INSERT INTO bien_the_san_pham (ma_san_pham, sku, ten_bien_the, gia_ban, so_luong_trong_kho, url_hinh_anh)
SELECT sp.ma_san_pham, '$sku', $label, $price, $stock, $image FROM san_pham sp WHERE sp.duong_dan='$slug'
ON DUPLICATE KEY UPDATE url_hinh_anh = IF(url_hinh_anh IS NULL OR url_hinh_anh LIKE '/img/products/%', VALUES(url_hinh_anh), url_hinh_anh);
"@)
    }
}
$statements.Add(@'
INSERT INTO phieu_giam_gia (ma_ap_dung, loai_giam_gia, gia_tri_giam, muc_giam_toi_da, don_toi_thieu,
    gioi_han_su_dung, bat_dau_luc, ket_thuc_luc)
VALUES ('DEMO10', 'PERCENT', 10, 50000, 200000, 20, UTC_TIMESTAMP() - INTERVAL 1 DAY, UTC_TIMESTAMP() + INTERVAL 30 DAY)
ON DUPLICATE KEY UPDATE ma_phieu_giam_gia=ma_phieu_giam_gia;
COMMIT;
'@)

$oldPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $env:DB_PASSWORD
    $statements -join "`n" | & $Client --no-defaults --protocol=tcp --skip-ssl -h 127.0.0.1 -P $Port -u $User --default-character-set=utf8mb4 $Database
    if ($LASTEXITCODE -ne 0) { throw "MariaDB seed failed (exit $LASTEXITCODE)." }
    Write-Output "Demo catalog imported: $($products.Count) products, $($rows.Count) SKUs."
} finally {
    $env:MYSQL_PWD = $oldPassword
}
