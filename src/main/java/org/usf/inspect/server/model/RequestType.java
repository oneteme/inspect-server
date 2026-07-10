package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.usf.inspect.server.repo.InspectStore;
import org.usf.inspect.server.repo.RequestCatalog;

import java.util.function.Function;

@RequiredArgsConstructor
@Getter
public enum RequestType {
    rest(InspectStore::restRequest),
    jdbc(InspectStore::databaseRequest),
    ftp(InspectStore::ftpRequest),
    smtp(InspectStore::smtpRequest),
    ldap(InspectStore::ldapRequest);

    private final Function<InspectStore, RequestCatalog> colFn;
}
