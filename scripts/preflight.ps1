# Kiểm tra môi trường, không thay đổi máy hoặc database
$ErrorActionPreference = 'Continue'
Write-Host '=== BeautyShop preflight ==='
java -version
mvn -version
Write-Host "DB_URL = $env:DB_URL"
Write-Host 'DB_USER = (masked if supplied)'
Write-Host 'DB_PASSWORD is intentionally NOT printed.'
Write-Host 'Run SQL creation manually before boot: docs/deployment/01-local-db-and-flyway.md'
Write-Host 'mvn test ; mvn spring-boot:run'
