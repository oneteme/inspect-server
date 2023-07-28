package org.usf.trace.api.server;

import static java.sql.Types.*;



public enum Filters {
    ID_IN_REQ(VARCHAR),
    ID_MAIN_REQ(VARCHAR),
    VA_APP_NME(VARCHAR),
    VA_ENV(VARCHAR),
    CD_PRT(INTEGER),
    DH_DBT(TIMESTAMP),
    DH_FIN(TIMESTAMP),
    LNCH(VARCHAR);
    private int columnType;

    Filters(int columnType) {
        this.columnType = columnType;
    }



    public int getType() {
        return this.columnType;
    }
}
