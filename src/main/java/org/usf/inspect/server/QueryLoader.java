package org.usf.inspect.server;

import org.usf.inspect.server.model.Query;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static java.util.Optional.ofNullable;

public class QueryLoader {

    public static List<Query> loadQueries (String env, List<String> appName, Instant before, List<String> version) {

        List<Object> cteParams = new ArrayList<>(Arrays.asList(fromNullableInstant(before),env));
        String appNameCondition = "";
        String versionCondition = "";
        if(appName != null && !appName.isEmpty()){
            appNameCondition = "and va_app in ("+ String.join(", ", Collections.nCopies(appName.size(),"?")) +")";
            cteParams.addAll(appName);
        }

        if(version != null && !version.isEmpty()) {
            versionCondition = "and va_vrs in ("+ String.join(", ", Collections.nCopies(version.size(),"?")) +")";
            cteParams.addAll(version);
        }
        cteParams.add(fromNullableInstant(before));
        cteParams.add(fromNullableInstant(before));


        List<Query> queries = new ArrayList<>();
        //create session temp table
        queries.add(new Query(
                """
                        create  temporary table IF NOT EXISTs  temp_session_table(
                        id varchar(255),
                        va_typ char(1)
                        );
                        """)
        );

        //create request temp table
        queries.add(new Query(
                """
                        create  temporary table IF NOT EXISTs  temp_request_table(
                        id int
                        );
                        """
        ));

        //fill temp session temp table
        queries.add(new Query(
                """
                        with deleted_instances as(
                          select eei.id_ins as id, 'i' as va_typ
                          from e_env_ins eei
                          where dh_str < ?
                          and va_env = ?
                          %s
                          %s
                        ) insert into temp_session_table
                           select id_ses as id, 'r' as va_typ
                           from e_rst_ses ers
                           where ers.cd_ins in (select id from deleted_instances)
                           and ers.dh_str < ?
                           union
                           select id_ses as id, 'm' as va_typ
                           from e_main_ses ems
                           where ems.cd_ins in (select id from deleted_instances)
                           and ems.dh_str < ?
                           union
                           select * from deleted_instances;
                         """.formatted(appNameCondition, versionCondition),cteParams.toArray()));
        //fill temp request table
        queries.add(new Query(
                """
                    insert into temp_request_table %s
                """.formatted(createRequestQuery("id_rst_rqt","e_rst_rqt")
        )));

        // delete rest exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select cast(id as int) from temp_request_table)
                        and va_typ = 'REST';
                        """
        ));

        // delete rest request
        queries.add(new Query(
                """
                        delete from e_rst_rqt
                        where id_rst_rqt in (select id from temp_request_table);
                        """
        ));

        // empty temp_request_table
        queries.add(new Query(
                """
                        delete from temp_request_table;
                        """
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                """
                    insert into temp_request_table %s
                """.formatted(createRequestQuery("id_dtb_rqt","e_dtb_rqt")
        )));

        // delete jdbc exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select id from temp_request_table)
                        and va_typ = 'JDBC';
                        """
        ));

        // delete db stage
        queries.add(new Query(
                """
                        delete from e_dtb_stg
                        where cd_dtb_rqt in  (select id from temp_request_table)
                        """));

        // delete db request
        queries.add(new Query(
                """
                        delete from e_dtb_rqt
                        where id_dtb_rqt in (select id from temp_request_table);
                        """
        ));

        // empty temp_request_table
        queries.add(new Query(
                """
                        delete from temp_request_table;
                        """
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                """
                    insert into temp_request_table %s
                """.formatted(createRequestQuery("id_ftp_rqt","e_ftp_rqt")
        )));

        // delete ftp exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select id from temp_request_table)
                        and va_typ = 'FTP';
                        """
        ));

        // delete ftp stage
        queries.add(new Query(
                """
                        delete from e_ftp_stg
                        where cd_ftp_rqt in (select id from temp_request_table)
                        """
        ));

        // delete ftp request
        queries.add(new Query(
                """
                        delete from e_ftp_rqt
                        where id_ftp_rqt in (select id from temp_request_table);
                        """
        ));

        // empty temp_request_table
        queries.add(new Query(
                """
                        delete from temp_request_table;
                        """
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                """
                    insert into temp_request_table %s
                """.formatted(createRequestQuery("id_smtp_rqt","e_smtp_rqt")
        )));

        // delete smtp exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select id from temp_request_table)
                        and va_typ = 'SMTP';
                        """
        ));

        // delete smtp stage
        queries.add(new Query(
                """
                        delete from e_smtp_stg
                        where cd_smtp_rqt in (select id from temp_request_table)
                        """));

        // delete smtp mail
        queries.add(new Query(
                """
                        delete from e_smtp_mail
                        where cd_smtp_rqt in (select id from temp_request_table);
                        """
        ));

        // delete smtp request
        queries.add(new Query(
                """
                        delete from e_smtp_rqt
                        where id_smtp_rqt in (select id from temp_request_table);
                        """
        ));

        // empty temp_request_table
        queries.add(new Query(
                """
                        delete from temp_request_table;
                        """
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                """
                    insert into temp_request_table %s
                """.formatted(createRequestQuery("id_ldap_rqt","e_ldap_rqt")
        )));

        // delete ldap exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select id from temp_request_table)
                        and va_typ = 'LDAP';
                        """
        ));

        // delete ldap stage
        queries.add(new Query(
                """
                        delete from e_ldap_stg
                        where cd_ldap_rqt in (select id from temp_request_table)
                        """
        ));

        // delete ldap request
        queries.add(new Query(
                """
                        delete from e_ldap_rqt
                        where id_ldap_rqt in (select id from temp_request_table);
                        """
        ));
        // empty temp_request_table
        queries.add(new Query(
                """
                        delete from temp_request_table;
                        """
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                """
                    insert into temp_request_table %s
                """.formatted(createRequestQuery("id_lcl_rqt","e_lcl_rqt")
        )));

        // delete local exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select id from temp_request_table)
                        and va_typ = 'LOCAL';
                        """
        ));

        // delete local request
        queries.add(new Query(
                """
                        delete from e_lcl_rqt
                        where id_lcl_rqt in (select id from temp_request_table);
                        """
        ));

        //drop temp request table
        queries.add(new Query(
                """
                      drop table if exists temp_request_table;
                        """
        ));
        //----------------
        // delete rest session
        queries.add(new Query(
                """
                        delete from e_rst_ses
                        where id_ses in (select id from temp_session_table where va_typ ='r');
                        """
        ));

        // delete main request
        queries.add(new Query(
                """
                        delete from e_main_ses
                        where id_ses in (select id from temp_session_table where va_typ ='m');
                        """
        ));

        // delete instances
        queries.add(new Query(
                """
                        delete from e_env_ins
                        where id_ins in (select id from temp_session_table where va_typ ='i');
                        """
        ));

        //delete temp table
        queries.add(new Query(
                """
                        drop table if exists temp_session_table;
                        """
        ));
        return queries;
    }

    private static Timestamp fromNullableInstant(Instant instant) {
        return ofNullable(instant).map(Timestamp::from).orElse(null);
    }

    private static String createRequestQuery(String column, String table){
        return """
                     select %s as id
                     from %s
                     where cd_prn_ses in (select id from temp_session_table)
                """.formatted(column,table);
    }
}
