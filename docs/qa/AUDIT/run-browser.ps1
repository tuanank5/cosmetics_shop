# Run from the project root after packaging, with JAVA_HOME set to JDK 21.
$ErrorActionPreference = 'Stop'
$fixture = Get-Content -Raw -Encoding UTF8 docs/qa/CART-01/run-fixture.ps1
$fixture = $fixture.Replace('C:/Program Files/Java/jdk-21.0.13.10002-hotspot/bin/java.exe', "$env:JAVA_HOME/bin/java.exe")
$fixture = $fixture.Replace("'--app.ai.enabled=false'", "'--app.audit.clearance-source-enabled=true', '--app.audit.operator-ids=3', '--app.ai.enabled=false'")
$entry = '& node docs/qa/CART-01/browser-check.mjs $site $scratch'
if (!$fixture.Contains($entry)) { throw 'CART-01 browser entry point has changed' }
$check = @'
    $env:AUDIT_FIXTURE_BATCH = [guid]::NewGuid().ToString()
    & "$maria/mariadb.exe" @sql cart_01_test --execute="
        INSERT INTO nguoi_dung (email, mat_khau_bam, ho_ten, vai_tro)
        SELECT 'admin@example.invalid', mat_khau_bam, 'Fixture Admin', 'ADMIN' FROM nguoi_dung WHERE ma_nguoi_dung=1;
        INSERT INTO nhat_ky_quan_tri (ma_nguoi_thuc_hien,vai_tro_nguoi_thuc_hien,hanh_dong,loai_doi_tuong,ma_doi_tuong,thoi_gian,ket_qua,correlation_id)
        SELECT 3,'ADMIN','FIXTURE_READ','SKU',seq,'2000-01-01','SUCCESS',UUID() FROM seq_1_to_26;
        CREATE TABLE audit_purge_clearance (batch_id CHAR(36),audit_id BIGINT,expires_utc DATETIME(6),PRIMARY KEY(batch_id,audit_id));
        INSERT INTO audit_purge_clearance SELECT '$env:AUDIT_FIXTURE_BATCH',ma_nhat_ky,UTC_TIMESTAMP()+INTERVAL 1 HOUR
        FROM nhat_ky_quan_tri WHERE hanh_dong='FIXTURE_READ';"
    if ($LASTEXITCODE) { throw 'Order fixture failed' }
    & node docs/qa/AUDIT/browser.cjs $site
'@
& ([scriptblock]::Create($fixture.Replace($entry, $check)))
