package org.usf.trace.api.server;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.trace.api.server.config.TraceApiColumn.*;
import static org.usf.trace.api.server.config.TraceApiTable.INCOMING_REQUEST_TABLE;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.trace.api.server.metadata.CombinedFieldMetadata;
import org.usf.trace.api.server.metadata.FieldMetadata;
import org.usf.trace.api.server.metadata.SimpleFieldMetadata;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "metadata", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MetadataController {

    @GetMapping("aggregate")
    List<FieldMetadata> fetch() {
        return asList(
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT, COUNT.reference(), "Nombre d'appels", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, ELAPSEDTIME, ELAPSEDTIME.reference(), "temps de réponse (s)", "s"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, AVG_ELAPSEDTIME, AVG_ELAPSEDTIME.reference(), "temps de réponse moyen(s)", "s"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, MAX_ELAPSEDTIME, MAX_ELAPSEDTIME.reference(), "temps de réponse max(s)", "s"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, MIN_ELAPSEDTIME, MIN_ELAPSEDTIME.reference(), "temps de réponse min(s)", "s"),

                new CombinedFieldMetadata("nombre d'appels OK / KO", asList(
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_STATUS_ERROR, COUNT_STATUS_ERROR.reference(), "nombre d'appels en erreur", "count"),
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_STATUS_SUCCES, COUNT_STATUS_SUCCES.reference(), "nombre d'appels OK", "count"))),

                new CombinedFieldMetadata("nombre d'appels 2xx/4xx/5xx", asList(
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_STATUS_ERROR_CLIENT, COUNT_STATUS_ERROR_CLIENT.reference(), "nombre d'appels en erreur client", "count"),
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_STATUS_ERROR_SERVER, COUNT_STATUS_ERROR_SERVER.reference(), "nombre d'appels en erreur serveur", "count"),
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_STATUS_SUCCES, COUNT_STATUS_SUCCES.reference(), "nombre d'appels OK", "c°"))),

                new CombinedFieldMetadata("nombre d'appels par temps de réponse", asList(
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_ELAPSEDTIME_SLOWEST, COUNT_ELAPSEDTIME_SLOWEST.reference(), "temps de réponse les plus lents ", "count"),
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_ELAPSEDTIME_SLOW, COUNT_ELAPSEDTIME_SLOW.reference(), "temps de réponse lent", "count"),
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_ELAPSEDTIME_MEDIUM, COUNT_ELAPSEDTIME_MEDIUM.reference(), "temps de réponse moyen", "count"),
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_ELAPSEDTIME_FAST, COUNT_ELAPSEDTIME_FAST.reference(), "temps de réponse rapide", "count"),
                        new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, COUNT_ELAPSEDTIME_FASTEST, COUNT_ELAPSEDTIME_FASTEST.reference(), "temps de réponse les plus rapides", "count")))
        );
    }

    @GetMapping("filter")
    List<SimpleFieldMetadata> fetchFilters() {
        return asList(

                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, MTH, MTH.reference(), "Methode", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, PROTOCOL, PROTOCOL.reference(), "Protocole", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, HOST, HOST.reference(), "Hôte", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, PORT, PORT.reference(), "Port", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, PATH, PATH.reference(), "Path", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, QUERY, QUERY.reference(), "Query", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, CONTENT_TYPE, CONTENT_TYPE.reference(), "Content-Type", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, AUTH, AUTH.reference(), "Schéma d'authentification", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, STATUS, STATUS.reference(), "Status", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, SIZE_IN, SIZE_IN.reference(), "Taille d'entrée", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, SIZE_OUT, SIZE_OUT.reference(), "Taille de sortie", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, START_DATETIME, START_DATETIME.reference(), "Date de début", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, FINISH_DATETIME, FINISH_DATETIME.reference(), "Date de fin", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, THREAD, THREAD.reference(), "Thread", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, NAME_API, NAME_API.reference(), "Nom d'Api", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, USER, USER.reference(), "Utilisateur", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, NAME_APP, NAME_APP.reference(), "Nom d'application", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, VERSION, VERSION.reference(), "Version", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, ADDRESS, ADDRESS.reference(), "Adresse", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, ENVIRONEMENT, ENVIRONEMENT.reference(), "Environement", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, OS, OS.reference(), "Sytèm d'exploitation", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, RE, RE.reference(), "Environement d'exécution", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, AS_DATE, AS_DATE.reference(), "Format date", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, BY_DAY, BY_DAY.reference(), "Format jour", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, BY_MONTH, BY_MONTH.reference(), "Format mois", "count"),
                new SimpleFieldMetadata(INCOMING_REQUEST_TABLE, BY_YEAR, BY_YEAR.reference(), "Format Year", "count")
        );
    }
}
