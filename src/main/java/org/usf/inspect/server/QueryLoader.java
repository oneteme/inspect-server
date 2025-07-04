package org.usf.inspect.server;

import static org.usf.inspect.server.Utils.nArg;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.usf.inspect.server.model.Query;

import lombok.NonNull;

public class QueryLoader {

    private static final String INSERT_TEMP_REQUEST = "insert into temp_request_table ";
    private static final String DELETE_TEMP_REQUEST = "delete from temp_request_table;";
    private static final String E_EXC_INF = "e_exc_inf";

    private QueryLoader(){}
    public static List<Query> loadQueries (@NonNull List<String> env, List<String> appName, @NonNull Instant before, List<String> version) {

        if(env.isEmpty()){
            throw new IllegalArgumentException("need atleast one environement");
        }

        List<Object> cteParams = new ArrayList<>();
        cteParams.add(Timestamp.from(before));

        String envCondition = "and va_env in ("+ nArg(env.size()) +")";
        cteParams.addAll(env);

        String appNameCondition = "";
        if(appName != null && !appName.isEmpty()){
            appNameCondition = "and va_app in ("+ nArg(appName.size()) +")";
            cteParams.addAll(appName);
        }

        String versionCondition = "";
        if(version != null && !version.isEmpty()) {
            versionCondition = "and va_vrs in ("+ nArg(version.size()) +")";
            cteParams.addAll(version);
        }
        cteParams.add(Timestamp.from(before));
        cteParams.add(Timestamp.from(before));


        List<Query> queries = new ArrayList<>();
        //create session temp table
        queries.add(new Query(
                "create  temporary table IF NOT EXISTs temp_session_table( id varchar(255), va_typ char(1) );")
        );

        //create request temp table
        queries.add(new Query(
                "create  temporary table IF NOT EXISTs temp_request_table( id int);"
        ));

        //fill temp session temp table
        queries.add(new Query(
                """
with deleted_instances as( select eei.id_ins as id, 'i' as va_typ from e_env_ins eei where dh_str < ? %s %s %s )
insert into temp_session_table
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
                         """.formatted(envCondition, appNameCondition, versionCondition),cteParams.toArray()));
        //fill temp request table
        queries.add(new Query(
                INSERT_TEMP_REQUEST + createRequestQuery("id_rst_rqt","e_rst_rqt"
        )));

        // delete rest exception
        queries.add(new Query(
                "delete from e_exc_inf where cd_rqt in (select cast(id as int) from temp_request_table)  and va_typ = 'REST';",E_EXC_INF
        ));

        // delete rest request
        queries.add(new Query(
                "delete from e_rst_rqt   where id_rst_rqt in (select id from temp_request_table);","e_rst_rqt"
        ));

        // empty temp_request_table
        queries.add(new Query(
                DELETE_TEMP_REQUEST
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                INSERT_TEMP_REQUEST + createRequestQuery("id_dtb_rqt","e_dtb_rqt")
        ));

        // delete jdbc exception
        queries.add(new Query(
                "delete from e_exc_inf where cd_rqt in (select id from temp_request_table) and va_typ = 'JDBC';",E_EXC_INF
        ));

        // delete db stage
        queries.add(new Query(
                "delete from e_dtb_stg where cd_dtb_rqt in  (select id from temp_request_table);","e_dtb_stg"));

        // delete db request
        queries.add(new Query(
                "delete from e_dtb_rqt where id_dtb_rqt in (select id from temp_request_table);","e_dtb_rqt"
        ));

        // empty temp_request_table
        queries.add(new Query(
                DELETE_TEMP_REQUEST
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                INSERT_TEMP_REQUEST + createRequestQuery("id_ftp_rqt","e_ftp_rqt")
        ));

        // delete ftp exception
        queries.add(new Query(
                "delete from e_exc_inf where cd_rqt in (select id from temp_request_table) and va_typ = 'FTP';",E_EXC_INF
        ));

        // delete ftp stage
        queries.add(new Query(
                "delete from e_ftp_stg where cd_ftp_rqt in (select id from temp_request_table);","e_ftp_stg"
        ));

        // delete ftp request
        queries.add(new Query(
                "delete from e_ftp_rqt where id_ftp_rqt in (select id from temp_request_table);","e_ftp_rqt"
        ));

        // empty temp_request_table
        queries.add(new Query(
                DELETE_TEMP_REQUEST
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                INSERT_TEMP_REQUEST + createRequestQuery("id_smtp_rqt","e_smtp_rqt")
        ));

        // delete smtp exception
        queries.add(new Query(
                "delete from e_exc_inf where cd_rqt in (select id from temp_request_table) and va_typ = 'SMTP';",E_EXC_INF
        ));

        // delete smtp stage
        queries.add(new Query(
                "delete from e_smtp_stg where cd_smtp_rqt in (select id from temp_request_table)","e_smtp_stg"
        ));

        // delete smtp mail
        queries.add(new Query(
                "delete from e_smtp_mail where cd_smtp_rqt in (select id from temp_request_table);","e_smtp_mail"
        ));

        // delete smtp request
        queries.add(new Query(
                "delete from e_smtp_rqt where id_smtp_rqt in (select id from temp_request_table);","e_smtp_rqt"
        ));

        // empty temp_request_table
        queries.add(new Query(
                DELETE_TEMP_REQUEST
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                INSERT_TEMP_REQUEST + createRequestQuery("id_ldap_rqt","e_ldap_rqt")
        ));

        // delete ldap exception
        queries.add(new Query(
                "delete from e_exc_inf where cd_rqt in (select id from temp_request_table) and va_typ = 'LDAP';",E_EXC_INF
        ));

        // delete ldap stage
        queries.add(new Query(
                "delete from e_ldap_stg where cd_ldap_rqt in (select id from temp_request_table);","e_ldap_stg"
        ));

        // delete ldap request
        queries.add(new Query(
                "delete from e_ldap_rqt where id_ldap_rqt in (select id from temp_request_table);","e_ldap_rqt"
        ));
        // empty temp_request_table
        queries.add(new Query(
                DELETE_TEMP_REQUEST
        ));
        //---------------------
        //fill temp request table
        queries.add(new Query(
                INSERT_TEMP_REQUEST + createRequestQuery("id_lcl_rqt","e_lcl_rqt")
        ));

        // delete local exception
        queries.add(new Query(
                "delete from e_exc_inf where cd_rqt in (select id from temp_request_table) and va_typ = 'LOCAL';",E_EXC_INF
        ));

        // delete local request
        queries.add(new Query(
                "delete from e_lcl_rqt where id_lcl_rqt in (select id from temp_request_table);","e_lcl_rqt"
        ));

        //drop temp request table
        queries.add(new Query(
                "drop table if exists temp_request_table;"
        ));
        //----------------
        // delete rest session
        queries.add(new Query(
                "delete from e_rst_ses where id_ses in (select id from temp_session_table where va_typ ='r');","e_rst_ses"
        ));

        // delete main request
        queries.add(new Query(
                " delete from e_main_ses where id_ses in (select id from temp_session_table where va_typ ='m');","e_main_ses"
        ));

        // delete instances
        /*queries.add(new Query(
                "delete from e_env_ins where id_ins in (select id from temp_session_table where va_typ ='i');","e_env_ins"
        ));*/

        //delete temp table
        queries.add(new Query(
                "drop table if exists temp_session_table;"
        ));

        return queries;
    }

    private static String createRequestQuery(String column, String table){
        return """
 select %s as id
 from %s
 where cd_prn_ses in (select id from temp_session_table)""".formatted(column,table);
    }
}
