package org.usf.trace.api.server;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.usf.trace.api.server.config.TraceApiColumn.ACTION;
import static org.usf.trace.api.server.config.TraceApiColumn.API;
import static org.usf.trace.api.server.config.TraceApiColumn.AS_DATE;
import static org.usf.trace.api.server.config.TraceApiColumn.AVG_ELAPSEDTIME;
import static org.usf.trace.api.server.config.TraceApiColumn.BY_DAY;
import static org.usf.trace.api.server.config.TraceApiColumn.BY_MONTH;
import static org.usf.trace.api.server.config.TraceApiColumn.BY_YEAR;
import static org.usf.trace.api.server.config.TraceApiColumn.CLIENT;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_ELAPSEDTIME_FAST;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_ELAPSEDTIME_FASTEST;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_ELAPSEDTIME_MEDIUM;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_ELAPSEDTIME_SLOW;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_ELAPSEDTIME_SLOWEST;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_STATUS_ERROR;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_STATUS_ERROR_CLIENT;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_STATUS_ERROR_SERVER;
import static org.usf.trace.api.server.config.TraceApiColumn.COUNT_STATUS_SUCCES;
import static org.usf.trace.api.server.config.TraceApiColumn.DMN;
import static org.usf.trace.api.server.config.TraceApiColumn.ELAPSEDTIME;
import static org.usf.trace.api.server.config.TraceApiColumn.FINISH_DATETIME;
import static org.usf.trace.api.server.config.TraceApiColumn.MAX_ELAPSEDTIME;
import static org.usf.trace.api.server.config.TraceApiColumn.MIN_ELAPSEDTIME;
import static org.usf.trace.api.server.config.TraceApiColumn.MTH;
import static org.usf.trace.api.server.config.TraceApiColumn.RESOURCE;
import static org.usf.trace.api.server.config.TraceApiColumn.START_DATETIME;
import static org.usf.trace.api.server.config.TraceApiColumn.STATUS;
import static org.usf.trace.api.server.config.TraceApiColumn.URI;
import static org.usf.trace.api.server.config.TraceApiTable.AGREEMENTS;

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
@RequestMapping(value="metadata", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class MetadataController {

    @GetMapping("aggregate")
    List<FieldMetadata> fetch() {
        return asList(
                new SimpleFieldMetadata(AGREEMENTS, COUNT,COUNT.reference(), "Nombre d'appels", "count"),
                new SimpleFieldMetadata(AGREEMENTS, ELAPSEDTIME,ELAPSEDTIME.reference(), "temps de réponse (s)", "s"),
                new SimpleFieldMetadata(AGREEMENTS, AVG_ELAPSEDTIME,AVG_ELAPSEDTIME.reference(), "temps de réponse moyen(s)", "s"),
                new SimpleFieldMetadata(AGREEMENTS, MAX_ELAPSEDTIME,MAX_ELAPSEDTIME.reference(), "temps de réponse max(s)", "s"),
                new SimpleFieldMetadata(AGREEMENTS, MIN_ELAPSEDTIME,MIN_ELAPSEDTIME.reference(), "temps de réponse min(s)", "s"),

                new CombinedFieldMetadata("nombre d\'appels OK / KO", asList(
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_STATUS_ERROR,COUNT_STATUS_ERROR.reference(), "nombre d'appels en erreur", "count"),
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_STATUS_SUCCES,COUNT_STATUS_SUCCES.reference(), "nombre d'appels OK", "count"))),

                new CombinedFieldMetadata("nombre d'appels 2xx/4xx/5xx", asList(
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_STATUS_ERROR_CLIENT,COUNT_STATUS_ERROR_CLIENT.reference(), "nombre d'appels en erreur client", "count"),
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_STATUS_ERROR_SERVER,COUNT_STATUS_ERROR_SERVER.reference(), "nombre d'appels en erreur serveur", "count"),
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_STATUS_SUCCES,COUNT_STATUS_SUCCES.reference(), "nombre d'appels OK", "c°"))),

                new CombinedFieldMetadata("nombre d'appels par temps de réponse", asList(
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_ELAPSEDTIME_SLOWEST,COUNT_ELAPSEDTIME_SLOWEST.reference(), "temps de réponse les plus lents ", "count"),
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_ELAPSEDTIME_SLOW,COUNT_ELAPSEDTIME_SLOW.reference(), "temps de réponse lent", "count"),
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_ELAPSEDTIME_MEDIUM,COUNT_ELAPSEDTIME_MEDIUM.reference(), "temps de réponse moyen", "count"),
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_ELAPSEDTIME_FAST,COUNT_ELAPSEDTIME_FAST.reference(), "temps de réponse rapide", "count"),
                        new SimpleFieldMetadata(AGREEMENTS, COUNT_ELAPSEDTIME_FASTEST,COUNT_ELAPSEDTIME_FASTEST.reference(), "temps de réponse les plus rapides", "count")))
        );
    }
    
    @GetMapping("filter")
    List<SimpleFieldMetadata> fetchFilters() {
        return asList(
                new SimpleFieldMetadata(AGREEMENTS, STATUS,STATUS.reference(), "Status", " "),
                new SimpleFieldMetadata(AGREEMENTS, MTH,MTH.reference(), "Methode", " "),
                new SimpleFieldMetadata(AGREEMENTS, URI,URI.reference(), "URI", " "),
                new SimpleFieldMetadata(AGREEMENTS, START_DATETIME,START_DATETIME.reference(), "Date de début", " "),
                new SimpleFieldMetadata(AGREEMENTS, FINISH_DATETIME,FINISH_DATETIME.reference(), "Date de fin", " "),
                new SimpleFieldMetadata(AGREEMENTS, RESOURCE,RESOURCE.reference(), "Ressource", " "),
                new SimpleFieldMetadata(AGREEMENTS, CLIENT,CLIENT.reference(), "Client", " "),
                new SimpleFieldMetadata(AGREEMENTS, ACTION,ACTION.reference(), "Endpoints", " "),
                new SimpleFieldMetadata(AGREEMENTS, DMN,DMN.reference(), "Domaine", " "),
                new SimpleFieldMetadata(AGREEMENTS, API,API.reference(), "API", " "),
                new SimpleFieldMetadata(AGREEMENTS, AS_DATE,AS_DATE.reference(), "Format date", " "),
                new SimpleFieldMetadata(AGREEMENTS, BY_DAY,BY_DAY.reference(), "Format jour", " "),
                new SimpleFieldMetadata(AGREEMENTS, BY_MONTH,BY_MONTH.reference(), "Format mois", " "),
                new SimpleFieldMetadata(AGREEMENTS, BY_YEAR,BY_YEAR.reference(), "Format Year", " ")
        );
    }
}
