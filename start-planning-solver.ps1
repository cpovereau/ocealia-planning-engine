# ---------------------------------------
# Démarrage du service Spring Boot
# ---------------------------------------

$JarPath = "build\libs\demo-0.0.1-SNAPSHOT.jar"

if (-Not (Test-Path $JarPath)) {
    Write-Host "❌ JAR introuvable : $JarPath" -ForegroundColor Red
    Write-Host "➡️ Lancez d'abord : .\gradlew bootJar"
    exit 1
}

Write-Host "🚀 Démarrage du Planning Solver..." -ForegroundColor Green
Write-Host "📦 JAR : $JarPath"
Write-Host "🌐 Port : 8082"
Write-Host ""

java -jar $JarPath
