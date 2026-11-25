$ErrorActionPreference = 'Stop'
$report = [xml](Get-Content -Raw target/surefire-reports/TEST-com.beautyshop.controller.admin.AdminDashboardPagesTest.xml)
$classpath = ($report.testsuite.properties.property | Where-Object name -eq 'surefire.test.class.path').value
$mavenRepo = "-Dmaven.repo.local=$env:USERPROFILE/.m2/repository"
$classFile = 'target/test-classes/com/beautyshop/controller/admin/P20CLayoutFixtureTest.class'

& javac.exe -encoding UTF-8 -cp $classpath -d target/test-classes docs/qa/P20C/P20CLayoutFixtureTest.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
try {
    & mvn.cmd -o $mavenRepo surefire:test '-Dtest=P20CLayoutFixtureTest#exportLayoutFixtures' *> target/p20c-mvn-layout-export.txt
    $result = $LASTEXITCODE
    Get-Content target/p20c-mvn-layout-export.txt |
        Where-Object { $_ -match '^\[(INFO|ERROR)\] (Tests run:|BUILD |Total time:|Finished at:|Failed to execute goal)' } |
        Set-Content -Encoding UTF8 docs/qa/P20C/layout-fixture-export.txt
    if ($result -ne 0) { exit $result }
} finally {
    Remove-Item -LiteralPath $classFile -ErrorAction SilentlyContinue
}
