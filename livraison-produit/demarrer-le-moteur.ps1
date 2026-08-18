# =====================================================================
#  Démarrage du moteur de planification  (Windows)
# =====================================================================
#  Double-cliquez sur ce fichier, ou lancez-le depuis une invite :
#     powershell -ExecutionPolicy Bypass -File demarrer-le-moteur.ps1
#
#  La fenêtre reste ouverte : c'est normal, le moteur attend du travail.
#  Pour l'arrêter, fermez la fenêtre ou faites Ctrl+C.
# =====================================================================

Set-Location -Path $PSScriptRoot

$Jar = "moteur\moteur-planification.jar"

if (-Not (Test-Path $Jar)) {
    Write-Host "Le programme est introuvable : $Jar" -ForegroundColor Red
    Write-Host "Verifiez que le dossier a ete copie en entier."
    Read-Host "Appuyez sur Entree pour fermer"
    exit 1
}

try {
    $version = (java -version 2>&1 | Select-Object -First 1)
} catch {
    Write-Host "Java n'est pas installe sur ce poste." -ForegroundColor Red
    Write-Host "Installez 'Eclipse Temurin JRE 21' depuis adoptium.net, puis relancez."
    Read-Host "Appuyez sur Entree pour fermer"
    exit 1
}

Write-Host ""
Write-Host "  Moteur de planification" -ForegroundColor Green
Write-Host "  -----------------------"
Write-Host "  Java      : $version"
Write-Host "  Adresse   : http://localhost:8082/scenarios/ping"
Write-Host "  Depot     : data\file-adapter\inbox"
Write-Host "  Reponses  : data\file-adapter\outbox"
Write-Host ""
Write-Host "  Demarrage en cours, quelques secondes..." -ForegroundColor Yellow
Write-Host ""

java -jar $Jar

Write-Host ""
Write-Host "Le moteur s'est arrete." -ForegroundColor Yellow
Read-Host "Appuyez sur Entree pour fermer"
