param(
    [string[]]$Groups = @('UI-01C','CART-01','WISH-01','ORDER-01','REVIEW-01','DTO-01'),
    [ValidatePattern('^[a-zA-Z0-9-]+$')][string]$EvidenceTag
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path .).Path
$fixtures = 'docs/qa/QA-01'
$qa = $fixtures
if ($EvidenceTag) {
    $qa = "$qa/$EvidenceTag"
    $null = New-Item -ItemType Directory -Path $qa
}
$shots = 'docs/ui/screenshots/QA-01'
$scratch = 'target/qa-01-browser-' + [Guid]::NewGuid().ToString('N')
$null = New-Item -ItemType Directory -Force $scratch, $shots
Remove-Item Env:P01_MARIADB_TEST,Env:P23_MONGO_TEST,Env:AUTH_02_MIGRATION_FIXTURE -ErrorAction SilentlyContinue

function Read-Source($name) { Get-Content -LiteralPath $name -Encoding UTF8 -Raw }
function Write-Source($name, $source) {
    [IO.File]::WriteAllText((Join-Path $root $name), $source, [Text.UTF8Encoding]::new($false))
}

$results = foreach ($group in $Groups) {
    $dir = "$scratch/$group"
    $null = New-Item -ItemType Directory -Force "$dir/captures"
    $fixtureGroup = if ($group -eq 'RAG-HTML') { 'UI-01C' } else { $group }
    $runner = Read-Source "docs/qa/$fixtureGroup/run-fixture.ps1"
    $browser = (Read-Source "docs/qa/$fixtureGroup/browser-check.mjs").Replace("docs/qa/$fixtureGroup/browser-", "$qa/$group-browser-")
    $browser = $browser.Replace("docs/ui/screenshots/$group", "$dir/captures")
    if ($group -eq 'RAG-HTML') {
        $marker = $browser.IndexOf('  const rects = ')
        if ($marker -lt 0) { throw 'Could not locate the Chrome harness preamble' }
        $checks = (Read-Source "$fixtures/rag-html-checks.mjs").Replace('QA_RAG_EVIDENCE', "$qa/RAG-HTML-browser-results.json")
        $browser = $browser.Substring(0, $marker) + $checks
    }
    if ($group -eq 'UI-01C') {
        $snippet = Read-Source "$fixtures/dashboard-checks.mjs"
        $browser = $browser.Replace("    checks.push('admin-reset-confirm-cancel-back-and-desktop-current');", "    checks.push('admin-reset-confirm-cancel-back-and-desktop-current');`n$snippet")
        $seed = Read-Source "$fixtures/dashboard-seed.sql"
        $runner = $runner.Replace('"@ | & "$maria/mariadb.exe" @sql ui_01c_test', "$seed`n" + '"@ | & "$maria/mariadb.exe" @sql ui_01c_test')
        $nav = (Read-Source 'docs/qa/NAV-01/browser-check.mjs').Replace('docs/qa/NAV-01/browser-', "$qa/NAV-01-browser-")
        Write-Source "$dir/nav.mjs" $nav.Replace('docs/ui/screenshots/NAV-01', "$dir/captures")
        $navCall = @'
    if ($LASTEXITCODE -eq 0) {
        $null = New-Item -ItemType Directory -Path "$scratch/nav"
        & node QA_NAV_SCRIPT $site "$scratch/nav" after
    }
'@
        $marker = '& node docs/qa/UI-01C/browser-check.mjs $site $scratch $mode'
        $runner = $runner.Replace($marker, $marker + "`n" + $navCall.Replace('QA_NAV_SCRIPT', "$dir/nav.mjs"))
    }
    if ($group -eq 'DTO-01') {
        $snippet = Read-Source "$fixtures/address-checks.mjs"
        $browser = $browser.Replace("  await writeFile('$qa/DTO-01-browser-results.json'", "$snippet`n  await writeFile('$qa/DTO-01-browser-results.json'")
        $seed = Read-Source "$fixtures/customer-seed.sql"
        $runner = $runner.Replace('"@ | & "$maria/mariadb.exe" @sql dto_01_test', "$seed`n" + '"@ | & "$maria/mariadb.exe" @sql dto_01_test')
    }
    if ($group -eq 'REVIEW-01') {
        $reviewEvidence = "const evidence = '$qa/REVIEW-01-browser-results.json';"
        $browser = [regex]::Replace($browser, '(?m)^  const evidence = .*;\r?$', "  $reviewEvidence")
        if (!$browser.Contains($reviewEvidence)) { throw 'REVIEW-01 evidence path was not redirected' }
        $browser = $browser.Replace('const snapshot = () => evaluate(`(() => {', 'const snapshotExpression = `(() => {')
        $browser = $browser.Replace('  })()`);', "  })()``;`n  const snapshot = () => evaluate(snapshotExpression);")
        $reviewSubmit = @'
  await call('Runtime.enable');
  await call('Runtime.addBinding', { name: 'recordReviewLoading' });
  const submit = async (name, phrase, measureLoading = true) => {
    await focusButton();
    trace[name] = { before: await snapshot() };
    if (measureLoading) {
      const onLoading = event => {
        const { method, params } = JSON.parse(event.data);
        if (method === 'Runtime.bindingCalled' && params.name === 'recordReviewLoading') {
          trace[name].loading = JSON.parse(params.payload);
        }
      };
      socket.addEventListener('message', onLoading);
      try {
        await evaluate(`document.querySelector(${JSON.stringify(button)}).form.addEventListener(
          'submit', () => recordReviewLoading(JSON.stringify(${snapshotExpression})), { once: true })`);
        await key('Enter', 13);
        await until(() => assert.ok(trace[name].loading, 'Missing submit snapshot'));
      } finally {
        socket.removeEventListener('message', onLoading);
      }
    } else {
      await key('Enter', 13);
    }
    await flash(phrase);
    trace[name].after = await snapshot();
  };
'@
        $browser = [regex]::Replace($browser, '(?ms)^  const beginThrottle = .*?^  };\r?$', $reviewSubmit)
        if (!$browser.Contains('const snapshot = () => evaluate(snapshotExpression);') -or
            !$browser.Contains("name: 'recordReviewLoading'")) { throw 'REVIEW-01 loading observation was not configured' }
        $browser = $browser.Replace("    await call('Page.bringToFront');", "    await call('Page.bringToFront');`n    await until(async () => assert.ok(await evaluate('document.hasFocus()')));")
        $focus = @'
    await until(async () => {
      const after = await snapshot();
      assert.equal(after.focus, 'row-button');
      trace.staleTwoTabs.after = after;
    });
'@
        $browser = $browser.Replace("    assert.equal(trace.staleTwoTabs.after.focus, 'row-button');", $focus)
    }
    Write-Source "$dir/browser.mjs" $browser
    Write-Source "$dir/run.ps1" $runner.Replace("docs/qa/$fixtureGroup/browser-check.mjs", "$dir/browser.mjs")
    if ($group -eq 'DTO-01') {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$dir/run.ps1" -Browser | Out-Host
    } else {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$dir/run.ps1" | Out-Host
    }
    $code = $LASTEXITCODE
    [pscustomobject]@{ Group = $group; ExitCode = $code }
    if ($code -eq 0) {
        $chosen = @{
            'UI-01C' = @('dashboard-390-table-nojs.webp','dashboard-1920-90d.webp','popular-return-390-nojs.png')
            'CART-01' = @('checkout-390-notice.webp')
            'WISH-01' = @('pdp-390-saved.webp')
            'ORDER-01' = @('mobile-return.webp')
            'REVIEW-01' = @('mobile-clamped.webp','desktop-unavailable.webp')
            'DTO-01' = @('desktop-quote.webp')
        }
        foreach ($image in $chosen[$group]) {
            $destination = "$shots/$group-$image"
            if (!$EvidenceTag -or !(Test-Path -LiteralPath $destination)) {
                Copy-Item -LiteralPath "$dir/captures/$image" -Destination $destination
            }
        }
    }
}
$resultPath = "$qa/browser-groups.json"
$byGroup = [ordered]@{}
if (Test-Path -LiteralPath $resultPath) {
    foreach ($entry in (Get-Content -Raw -LiteralPath $resultPath | ConvertFrom-Json)) {
        $byGroup[$entry.Group] = $entry
    }
}
foreach ($entry in $results) {
    $byGroup[$entry.Group] = $entry
}
ConvertTo-Json -InputObject @($byGroup.Values) | Set-Content -Encoding utf8 $resultPath
if (@($results | Where-Object ExitCode -ne 0).Count) { exit 1 }
