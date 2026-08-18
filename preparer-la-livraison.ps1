# =====================================================================
#  Regenere le dossier livraison-produit/ pret a etre zippe
# ---------------------------------------------------------------------
#  Le .jar n'est pas suivi en gestion de version (66 Mo) : ce script le
#  reconstruit et le place au bon endroit. Tout le reste du dossier est
#  versionne et n'a pas besoin d'etre regenere.
#
#     .\preparer-la-livraison.ps1
# =====================================================================

$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

Write-Host "1/3  Construction du programme..." -ForegroundColor Cyan
& .\gradlew.bat bootJar
if ($LASTEXITCODE -ne 0) { Write-Host "Echec de la construction." -ForegroundColor Red; exit 1 }

Write-Host "2/3  Copie dans livraison-produit\moteur..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "livraison-produit\moteur" | Out-Null
Copy-Item "build\libs\demo-0.0.1-SNAPSHOT.jar" "livraison-produit\moteur\moteur-planification.jar" -Force

Write-Host "3/3  Rafraichissement des jeux d'essai et des fiches techniques..." -ForegroundColor Cyan
Copy-Item "src\test\resources\scenarios\sc01\sc01_dataset_reference.json"  "livraison-produit\exemples\sc01_exemple.json" -Force
Copy-Item "src\test\resources\scenarios\sc02\sc02_reference.json"          "livraison-produit\exemples\sc02_exemple.json" -Force
Copy-Item "src\test\resources\scenarios\sc03\sc03_migration_reference.json" "livraison-produit\exemples\sc03_exemple.json" -Force
Copy-Item "src\test\resources\scenarios\sc04\sc04_reference.json"          "livraison-produit\exemples\sc04_exemple.json" -Force
Copy-Item "src\test\resources\scenarios\sc05\sc05_arbitrage.json"          "livraison-produit\exemples\sc05_exemple.json" -Force
Copy-Item "src\test\resources\scenarios\sc06\sc06_reference.json"          "livraison-produit\exemples\sc06_exemple.json" -Force
Copy-Item "docs\50_openapi_windev_moteur_v_1.yaml"  "livraison-produit\reference\" -Force
Copy-Item "docs\50_ScenarioContract.schema.json"    "livraison-produit\reference\" -Force
Copy-Item "docs\50_ScenarioResponse.schema.json"    "livraison-produit\reference\" -Force

Write-Host ""
Write-Host "Dossier pret : livraison-produit\" -ForegroundColor Green
Write-Host "Il ne reste qu'a le compresser et a l'envoyer."
