package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

import org.usf.inspect.server.erm.InspectStore;
import org.usf.inspect.server.erm.RequestCatalog;

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
