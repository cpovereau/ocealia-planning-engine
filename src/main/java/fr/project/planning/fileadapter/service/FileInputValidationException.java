package fr.project.planning.fileadapter.service;

/**
 * Exception levée lors de la validation d'entrée d'un fichier FileAdapter.
 *
 * Contient un {@code code} machine-readable (exploitable par WinDev ou le support)
 * et un {@code message} explicite en français.
 *
 * Codes possibles :
 * - {@code INVALID_EXTENSION}  — extension ≠ .json
 * - {@code FILE_TOO_LARGE}     — taille > maxInputFileSizeBytes
 * - {@code FILE_UNREADABLE}    — impossible de lire les métadonnées du fichier
 * - {@code INVALID_JSON}       — la racine JSON n'est pas un objet
 * - {@code MISSING_FIELD}      — champ obligatoire absent, null ou vide
 */
public class FileInputValidationException extends RuntimeException {

    private final String code;

    public FileInputValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Code court identifiant la cause de rejet, utilisable par WinDev ou le support. */
    public String getCode() {
        return code;
    }
}
