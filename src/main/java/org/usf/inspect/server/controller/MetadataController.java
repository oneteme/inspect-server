package org.usf.inspect.server.controller;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.inspect.server.config.TraceApiColumn.ADDRESS;
import static org.usf.inspect.server.config.TraceApiColumn.API_NAME;
import static org.usf.inspect.server.config.TraceApiColumn.APP_NAME;
import static org.usf.inspect.server.config.TraceApiColumn.AUTH;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_ERROR;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_ERROR_CLIENT;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_ERROR_SERVER;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_FAST;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_FASTEST;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_MEDIUM;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_SLOW;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_SLOWEST;
import static org.usf.inspect.server.config.TraceApiColumn.COUNT_SUCCES;
import static org.usf.inspect.server.config.TraceApiColumn.ELAPSEDTIME;
import static org.usf.inspect.server.config.TraceApiColumn.END;
import static org.usf.inspect.server.config.TraceApiColumn.ENVIRONEMENT;
import static org.usf.inspect.server.config.TraceApiColumn.HOST;
import static org.usf.inspect.server.config.TraceApiColumn.MEDIA;
import static org.usf.inspect.server.config.TraceApiColumn.METHOD;
import static org.usf.inspect.server.config.TraceApiColumn.OS;
import static org.usf.inspect.server.config.TraceApiColumn.PATH;
import static org.usf.inspect.server.config.TraceApiColumn.PORT;
import static org.usf.inspect.server.config.TraceApiColumn.PROTOCOL;
import static org.usf.inspect.server.config.TraceApiColumn.QUERY;
import static org.usf.inspect.server.config.TraceApiColumn.RE;
import static org.usf.inspect.server.config.TraceApiColumn.SIZE_IN;
import static org.usf.inspect.server.config.TraceApiColumn.SIZE_OUT;
import static org.usf.inspect.server.config.TraceApiColumn.START;
import static org.usf.inspect.server.config.TraceApiColumn.STATUS;
import static org.usf.inspect.server.config.TraceApiColumn.THREAD;
import static org.usf.inspect.server.config.TraceApiColumn.USER;
import static org.usf.inspect.server.config.TraceApiColumn.VERSION;
import static org.usf.inspect.server.config.TraceApiTable.REST_REQUEST;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.usf.inspect.server.metadata.CombinedFieldMetadata;
import org.usf.inspect.server.metadata.FieldMetadata;
import org.usf.inspect.server.metadata.SimpleFieldMetadata;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping(value = "metadata", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MetadataController {

    private static final String COUNT= "count";
    @GetMapping("aggregate")
    List<FieldMetadata> fetch() {
        return asList(
                new SimpleFieldMetadata(REST_REQUEST, ELAPSEDTIME, ELAPSEDTIME.reference(), "temps de réponse (s)", "s"),
                new CombinedFieldMetadata("nombre d'appels OK / KO", asList(
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_ERROR, COUNT_ERROR.reference(), "nombre d'appels en erreur", COUNT),
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_SUCCES, COUNT_SUCCES.reference(), "nombre d'appels OK", COUNT))),

                new CombinedFieldMetadata("nombre d'appels 2xx/4xx/5xx", asList(
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_ERROR_CLIENT, COUNT_ERROR_CLIENT.reference(), "nombre d'appels en erreur client", COUNT),
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_ERROR_SERVER, COUNT_ERROR_SERVER.reference(), "nombre d'appels en erreur serveur", COUNT),
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_SUCCES, COUNT_SUCCES.reference(), "nombre d'appels OK", "c°"))),

                new CombinedFieldMetadata("nombre d'appels par temps de réponse", asList(
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_SLOWEST, COUNT_SLOWEST.reference(), "temps de réponse les plus lents ", COUNT),
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_SLOW, COUNT_SLOW.reference(), "temps de réponse lent", COUNT),
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_MEDIUM, COUNT_MEDIUM.reference(), "temps de réponse moyen", COUNT),
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_FAST, COUNT_FAST.reference(), "temps de réponse rapide", COUNT),
                        new SimpleFieldMetadata(REST_REQUEST, COUNT_FASTEST, COUNT_FASTEST.reference(), "temps de réponse les plus rapides", COUNT)))
        );
    }

    @GetMapping("filter")
    List<SimpleFieldMetadata> fetchFilters() {
        return asList(
                new SimpleFieldMetadata(REST_REQUEST, METHOD, METHOD.reference(), "Methode", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, PROTOCOL, PROTOCOL.reference(), "Protocole", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, HOST, HOST.reference(), "Hôte", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, PORT, PORT.reference(), "Port", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, PATH, PATH.reference(), "Path", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, QUERY, QUERY.reference(), "Query", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, MEDIA, MEDIA.reference(), "Content-Type", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, AUTH, AUTH.reference(), "Schéma d'authentification", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, STATUS, STATUS.reference(), "Status", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, SIZE_IN, SIZE_IN.reference(), "Taille d'entrée", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, SIZE_OUT, SIZE_OUT.reference(), "Taille de sortie", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, START, START.reference(), "Date de début", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, END, END.reference(), "Date de fin", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, THREAD, THREAD.reference(), "Thread", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, API_NAME, API_NAME.reference(), "Nom d'Api", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, USER, USER.reference(), "Utilisateur", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, APP_NAME, APP_NAME.reference(), "Nom d'application", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, VERSION, VERSION.reference(), "Version", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, ADDRESS, ADDRESS.reference(), "Adresse", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, ENVIRONEMENT, ENVIRONEMENT.reference(), "Environement", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, OS, OS.reference(), "Sytèm d'exploitation", COUNT),
                new SimpleFieldMetadata(REST_REQUEST, RE, RE.reference(), "Environement d'exécution", COUNT)
        );
    }
}
