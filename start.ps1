# Script para iniciar la aplicación
# Asegúrate de tener configurado credentials.json antes de ejecutar

Write-Host "=== Sistema de Gestión de Pedidos ===" -ForegroundColor Green
Write-Host ""

# Verificar Java
Write-Host "Verificando Java..." -ForegroundColor Yellow
$javaVersion = java -version 2>&1 | Select-String "version"
if ($javaVersion) {
    Write-Host "✓ Java instalado: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "✗ Java no encontrado. Instala Java 21 o superior" -ForegroundColor Red
    exit 1
}

# Verificar Maven
Write-Host "Verificando Maven..." -ForegroundColor Yellow
$mavenVersion = mvn -v 2>&1 | Select-String "Apache Maven"
if ($mavenVersion) {
    Write-Host "✓ Maven instalado: $mavenVersion" -ForegroundColor Green
} else {
    Write-Host "✗ Maven no encontrado. Instala Maven 3.6+" -ForegroundColor Red
    exit 1
}

# Verificar credentials.json
Write-Host "Verificando credentials.json..." -ForegroundColor Yellow
if (Test-Path "credentials.json") {
    Write-Host "✓ credentials.json encontrado" -ForegroundColor Green
} else {
    Write-Host "✗ credentials.json no encontrado" -ForegroundColor Red
    Write-Host "  Lee el archivo credentials.json.example para instrucciones" -ForegroundColor Yellow
    $continue = Read-Host "¿Continuar sin credentials.json? (se requerirá para Gmail/Calendar) [s/N]"
    if ($continue -ne "s" -and $continue -ne "S") {
        exit 1
    }
}

Write-Host ""
Write-Host "Compilando el proyecto..." -ForegroundColor Yellow
mvn clean package -DskipTests -s settings.xml

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Compilación exitosa" -ForegroundColor Green
    Write-Host ""
    Write-Host "Iniciando la aplicación..." -ForegroundColor Yellow
    Write-Host "La aplicación estará disponible en: http://localhost:8080" -ForegroundColor Cyan
    Write-Host "Usuario: admin" -ForegroundColor Cyan
    Write-Host "Contraseña: admin123" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Presiona Ctrl+C para detener la aplicación" -ForegroundColor Yellow
    Write-Host ""

    java -jar target/ingreso-pedidos-1.0-SNAPSHOT.jar
} else {
    Write-Host "✗ Error en la compilación" -ForegroundColor Red
    Write-Host "Revisa los errores arriba" -ForegroundColor Yellow
    exit 1
}

