package org.usf.inspect.server.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PartitionedTable {
    SES_HTTP("e_rst_ses"),
    SES_MAIN("e_main_ses"),
    REQ_HTTP("e_rst_rqt"),
    REQ_JDBC("e_dtb_rqt"),
    REQ_SMTP("e_smtp_rqt"),
    REQ_LDAP("e_ldap_rqt"),
    REQ_FTP("e_ftp_rqt"),
    REQ_LOCAL("e_lcl_rqt");

    static final Set<String> tables = Set.of(
            "e_rst_rqt", "e_main_ses", "e_rst_ses",
            "e_smtp_rqt", "e_smtp_stg", "e_ftp_rqt",
            "e_ftp_stg", "e_ldap_rqt", "e_ldap_stg",
            "e_dtb_rqt", "e_dtb_stg", "e_lcl_rqt",
            "e_rst_rqt_stg", "e_rst_ses_stg"
    );

    private final String table;

    public static Optional<PartitionedTable> enumOf(String table) {
        return Arrays.stream(values()).filter(e-> e.table.equals(table)).findAny();
    }
}
