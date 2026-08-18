#!/bin/sh
# =====================================================================
#  Démarrage du moteur de planification  (Linux / macOS)
# =====================================================================
#     ./demarrer-le-moteur.sh
#
#  Le terminal reste occupé : c'est normal, le moteur attend du travail.
#  Pour l'arrêter : Ctrl+C.
# =====================================================================

cd "$(dirname "$0")" || exit 1

JAR="moteur/moteur-planification.jar"

if [ ! -f "$JAR" ]; then
    echo "Le programme est introuvable : $JAR"
    echo "Verifiez que le dossier a ete copie en entier."
    exit 1
fi

if ! command -v java > /dev/null 2>&1; then
    echo "Java n'est pas installe sur ce poste."
    echo "Installez un JRE 21 (paquet 'temurin-21-jre' ou equivalent), puis relancez."
    exit 1
fi

echo ""
echo "  Moteur de planification"
echo "  -----------------------"
echo "  Java      : $(java -version 2>&1 | head -n 1)"
echo "  Adresse   : http://localhost:8082/scenarios/ping"
echo "  Depot     : data/file-adapter/inbox"
echo "  Reponses  : data/file-adapter/outbox"
echo ""
echo "  Demarrage en cours, quelques secondes..."
echo ""

exec java -jar "$JAR"
