package org.usf.trace.api.server;

import static java.sql.Types.INTEGER;
import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;



public enum Filters {
    ID_SES(VARCHAR),
    VA_MTH(VARCHAR),
    VA_PRTCL(VARCHAR),
    VA_HST(VARCHAR),
    CD_PRT(INTEGER),
    VA_PTH(VARCHAR),
    VA_QRY(VARCHAR),
    VA_CNT_TYP(VARCHAR),
    VA_AUTH(VARCHAR),
    CD_STT(INTEGER),
    DH_DBT(TIMESTAMP),
    DH_FIN(TIMESTAMP),
    VA_API_NME(VARCHAR),
    VA_USR(VARCHAR),
    VA_APP_NME(VARCHAR),
    VA_ENV(VARCHAR),
    LNCH(VARCHAR),
    LOC(VARCHAR),
    VA_NAME(VARCHAR);

    private int columnType;

    Filters(int columnType) {
        this.columnType = columnType;
    }



    public int getType() {
        return this.columnType;
    }
}
