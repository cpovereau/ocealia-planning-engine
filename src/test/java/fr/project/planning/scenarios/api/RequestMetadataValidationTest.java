package fr.project.planning.scenarios.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4.1 — Validation requestId + metadata sur SC-01 et SC-03.
 * Phase 4.2 — Vérification du format de réponse d'erreur unifié.
 *
 * Vérifie que l'API rejette (HTTP 400) toute requête manquant :
 * - requestId
 * - metadata
 * - metadata.clientId
 * - metadata.timestamp
 *
 * Et accepte (HTTP 200) une requête avec ces champs valides.
 *
 * Les erreurs 400 doivent respecter le format :
 *   { "error": { "code": "INVALID_REQUEST", "message": "...", "details": [...] } }
 */
@SpringBootTest(classes = fr.project.planning.TestSpringConfig.class)
@AutoConfigureMockMvc
class RequestMetadataValidationTest {

    @Autowired
    private MockMvc mockMvc;

    // =========================================================
    // SC-01
    // =========================================================

    @Test
    void sc01_sans_requestId_retourne_400() throws Exception {
        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc01Payload(null, "CLIENT-TEST", "2026-05-11T08:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details").isArray());
    }

    @Test
    void sc01_requestId_vide_retourne_400() throws Exception {
        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc01Payload("", "CLIENT-TEST", "2026-05-11T08:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void sc01_sans_metadata_retourne_400() throws Exception {
        String json = """
                {
                  "requestId": "REQ-TEST-001",
                  "scenarioType": "SC-01",
                  "planningContext": {
                    "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
                    "strategieScoring": "EXPLOITATION"
                  },
                  "scenarioParameters": {
                    "resourceRef": { "kind": "SALARIE", "id": "1041" },
                    "dailyAmplitudeHours": 8.0,
                    "shiftStart": "08:00",
                    "workedDays": ["MONDAY"]
                  },
                  "dataSet": {
                    "ressources": { "salaries": [], "postesVirtuels": [] }
                  }
                }
                """;
        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void sc01_sans_metadata_clientId_retourne_400() throws Exception {
        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc01Payload("REQ-TEST-001", null, "2026-05-11T08:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void sc01_sans_metadata_timestamp_retourne_400() throws Exception {
        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc01Payload("REQ-TEST-001", "CLIENT-TEST", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void sc01_avec_requestId_et_metadata_valides_retourne_200() throws Exception {
        mockMvc.perform(post("/scenarios/sc01/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc01PayloadComplet()))
                .andExpect(status().isOk());
    }

    // =========================================================
    // SC-03
    // =========================================================

    @Test
    void sc03_sans_requestId_retourne_400() throws Exception {
        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc03Payload(null, "CLIENT-TEST", "2026-05-11T08:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.details").isArray());
    }

    @Test
    void sc03_requestId_vide_retourne_400() throws Exception {
        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc03Payload("", "CLIENT-TEST", "2026-05-11T08:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void sc03_sans_metadata_clientId_retourne_400() throws Exception {
        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc03Payload("REQ-TEST-001", null, "2026-05-11T08:00:00Z")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void sc03_sans_metadata_timestamp_retourne_400() throws Exception {
        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc03Payload("REQ-TEST-001", "CLIENT-TEST", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void sc03_avec_requestId_et_metadata_valides_retourne_200() throws Exception {
        mockMvc.perform(post("/scenarios/sc03/solve")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(sc03PayloadComplet()))
                .andExpect(status().isOk());
    }

    // =========================================================
    // Helpers
    // =========================================================

    /**
     * Payload SC-01 minimal avec requestId/metadata paramétrables.
     * null = champ absent du JSON.
     */
    private String sc01Payload(String requestId, String clientId, String timestamp) {
        String reqLine = requestId != null ? "\"requestId\": \"" + requestId + "\"," : "";
        String meta;
        if (clientId == null && timestamp == null) {
            meta = "";
        } else {
            String cId = clientId != null ? "\"clientId\": \"" + clientId + "\"" : "";
            String ts  = timestamp  != null ? "\"timestamp\": \"" + timestamp + "\"" : "";
            String sep = (!cId.isEmpty() && !ts.isEmpty()) ? "," : "";
            meta = "\"metadata\": {" + cId + sep + ts + "},";
        }
        return """
                {
                  %s
                  %s
                  "scenarioType": "SC-01",
                  "planningContext": {
                    "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
                    "strategieScoring": "EXPLOITATION"
                  },
                  "scenarioParameters": {
                    "resourceRef": { "kind": "SALARIE", "id": "1041" },
                    "dailyAmplitudeHours": 8.0,
                    "shiftStart": "08:00",
                    "workedDays": ["MONDAY"]
                  },
                  "dataSet": {
                    "ressources": { "salaries": [], "postesVirtuels": [] }
                  }
                }
                """.formatted(reqLine, meta);
    }

    private String sc01PayloadComplet() {
        return """
                {
                  "requestId": "REQ-VALID-SC01-001",
                  "metadata": {
                    "clientId": "CLIENT-TEST",
                    "timestamp": "2026-05-11T08:00:00Z",
                    "source": "WINDEV"
                  },
                  "scenarioType": "SC-01",
                  "planningContext": {
                    "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
                    "strategieScoring": "EXPLOITATION"
                  },
                  "scenarioParameters": {
                    "resourceRef": { "kind": "SALARIE", "id": "1041" },
                    "dailyAmplitudeHours": 8.0,
                    "shiftStart": "08:00",
                    "shiftEndAlert": "17:00",
                    "workedDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
                  },
                  "dataSet": {
                    "ressources": {
                      "salaries": [
                        {
                          "id": "1041",
                          "activitesAutorisees": ["TRAVAIL"],
                          "lieuxAutorises": ["CASTRES"]
                        }
                      ],
                      "postesVirtuels": []
                    },
                    "referentiels": {
                      "activites": [
                        { "codeActiviteId": "travail", "compteDansCharge": true, "genereDetteRepos": false }
                      ]
                    }
                  }
                }
                """;
    }

    /**
     * Payload SC-03 minimal avec requestId/metadata paramétrables.
     * null = champ absent du JSON.
     */
    private String sc03Payload(String requestId, String clientId, String timestamp) {
        String reqLine = requestId != null ? "\"requestId\": \"" + requestId + "\"," : "";
        String meta;
        if (clientId == null && timestamp == null) {
            meta = "";
        } else {
            String cId = clientId != null ? "\"clientId\": \"" + clientId + "\"" : "";
            String ts  = timestamp  != null ? "\"timestamp\": \"" + timestamp + "\"" : "";
            String sep = (!cId.isEmpty() && !ts.isEmpty()) ? "," : "";
            meta = "\"metadata\": {" + cId + sep + ts + "},";
        }
        return """
                {
                  %s
                  %s
                  "scenarioType": "SC-03",
                  "planningContext": {
                    "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
                    "strategieScoring": "EXPLOITATION"
                  },
                  "dataSet": {
                    "ressources": {
                      "salaries": [
                        { "id": "SAL-001", "activitesCompatibles": ["ACT-SOIN"] }
                      ],
                      "postesVirtuels": []
                    },
                    "creneaux": [
                      {
                        "id": "CRE-001",
                        "date": "2026-05-12",
                        "heureDebut": "08:00",
                        "heureFin": "16:00",
                        "codeActiviteId": "ACT-SOIN"
                      }
                    ],
                    "referentiels": {
                      "activites": [
                        { "codeActiviteId": "ACT-SOIN", "compteDansCharge": true, "genereDetteRepos": false }
                      ]
                    }
                  }
                }
                """.formatted(reqLine, meta);
    }

    private String sc03PayloadComplet() {
        return """
                {
                  "requestId": "REQ-VALID-SC03-001",
                  "metadata": {
                    "clientId": "CLIENT-TEST",
                    "timestamp": "2026-05-11T08:00:00Z",
                    "source": "WINDEV"
                  },
                  "scenarioType": "SC-03",
                  "planningContext": {
                    "horizon": { "dateDebut": "2026-05-11", "dateFin": "2026-05-17" },
                    "strategieScoring": "EXPLOITATION"
                  },
                  "dataSet": {
                    "ressources": {
                      "salaries": [
                        { "id": "SAL-001", "activitesCompatibles": ["ACT-SOIN"] }
                      ],
                      "postesVirtuels": []
                    },
                    "creneaux": [
                      {
                        "id": "CRE-001",
                        "date": "2026-05-12",
                        "heureDebut": "08:00",
                        "heureFin": "16:00",
                        "codeActiviteId": "ACT-SOIN"
                      }
                    ],
                    "referentiels": {
                      "activites": [
                        { "codeActiviteId": "ACT-SOIN", "compteDansCharge": true, "genereDetteRepos": false }
                      ]
                    }
                  }
                }
                """;
    }
}
