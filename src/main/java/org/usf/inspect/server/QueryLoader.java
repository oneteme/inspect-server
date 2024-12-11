package org.usf.inspect.server;

import org.usf.inspect.server.model.Query;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.util.Optional.ofNullable;

public class QueryLoader {

    public static List<Query> loadQueries (String env, List<String> appName, Instant before, List<String> version) {

        //String appNameCondition =  (appName != null && !appName.isEmpty())? "and va_app in("+ String.join(", ", "?".repeat(appName.size())) +")" : "";
        // String versionCondition =  (version != null && !version.isEmpty())? "and va_vrs in("+ String.join(", ", "?".repeat(version.size())) +")" : "";

        List<Query> queries = new ArrayList<>();

        //create temp table
        queries.add(new Query(
                """
                        create  temporary table IF NOT EXISTs  temp_table(
                        id varchar(255),
                        va_typ varchar(255)
                        );
                        """)
        );

        //fill temp table
        queries.add(new Query(
                """
                        with deleted_instances as(
                          select eei.id_ins as id, '0' as va_typ
                          from e_env_ins eei
                          where dh_str < ?
                          and va_env = ?
                        ),
                                                
                        deleted_sessions as(
                         select id_ses as id, '1a' as va_typ
                         from e_rst_ses ers
                         where ers.cd_ins in (select id from deleted_instances)
                         and ers.dh_str < ?
                         union
                         select id_ses as id, '1a' as va_typ
                         from e_main_ses ems
                         where ems.cd_ins in (select id from deleted_instances)
                         and ems.dh_str <  ?
                        ),
                                                
                        deleted_rest_requests as (
                         select cast(id_rst_rqt as varchar) as id, '2a' as va_typ
                         from e_rst_rqt err
                         where err.cd_prn_ses in (select id from deleted_sessions)
                         and err.dh_str <  ?
                        ),
                                                
                        deleted_db_requests as(
                        select cast( id_dtb_rqt as varchar) as id, '2b' as va_typ
                        from e_dtb_rqt edr
                        where edr.cd_prn_ses in (select id from deleted_sessions)
                        and edr.dh_str <   ?
                        ),
                                                
                        deleted_ftp_requests as (
                        select cast(id_ftp_rqt as varchar) as id, '2c' as va_typ
                        from e_ftp_rqt efr
                        where efr.cd_prn_ses in (select id from deleted_sessions)
                        and efr.dh_str <  ?
                        ),
                                                
                        deleted_smtp_requests as (
                        select cast(id_smtp_rqt as varchar) as id, '2d' as va_typ
                        from e_smtp_rqt esr
                        where esr.cd_prn_ses in (select id from deleted_sessions)
                        and esr.dh_str <  ?
                        ),
                                                
                        deleted_ldap_requests as (
                        select cast(id_ldap_rqt as varchar) as id, '2e' as va_typ
                        from e_ldap_rqt elr
                        where elr.cd_prn_ses in (select id  from deleted_sessions)
                        and elr.dh_str <  ?
                        ),
                                                
                        deleted_local_requests as (
                        select cast(id_lcl_rqt as varchar) as id, '2f' as va_typ
                        from e_lcl_rqt elr
                        where elr.cd_prn_ses in (select id from deleted_sessions)
                        and elr.dh_str <  ?
                        )insert into temp_table select * from deleted_instances
                        union
                        select * from deleted_sessions
                        union
                        select * from deleted_rest_requests
                        union
                        select * from deleted_db_requests
                        union
                        select * from deleted_ftp_requests
                        union
                        select * from deleted_smtp_requests
                        union
                        select * from deleted_ldap_requests
                        union
                        select * from deleted_local_requests;
                        """

        ,new Object[]{fromNullableInstant(before),
                env,
                fromNullableInstant(before),
                //appName
                //version,
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before)}));

        // delete rest exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select cast(id as int) from temp_table where va_typ ='2a')
                        and va_typ = 'REST';
                        """
        ));

        // delete jdbc exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select cast(id as int) from temp_table where va_typ ='2b')
                        and va_typ = 'JDBC';
                        """
        ));

        // delete ftp exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select cast(id as int) from temp_table where va_typ ='2c')
                        and va_typ = 'FTP';
                        """
        ));

        // delete smtp exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select cast(id as int) from temp_table where va_typ ='2d')
                        and va_typ = 'SMTP';
                        """
        ));

        // delete ldap exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select cast(id as int) from temp_table where va_typ ='2e')
                        and va_typ = 'LDAP';
                        """
        ));

        // delete ldap exception
        queries.add(new Query(
                """
                        delete from e_exc_inf
                        where cd_rqt in (select cast(id as int) from temp_table where va_typ ='2f')
                        and va_typ = 'LOCAL';
                        """
        ));

        // delete db stage
        queries.add(new Query(
                """
                        delete from e_dtb_stg
                        where cd_dtb_rqt in  (select cast(id as int) from temp_table where va_typ ='2b')
                        and dh_str < ?;
                        """
        ,new Object[]{fromNullableInstant(before)}));

        // delete ftp stage
        queries.add(new Query(
                """
                        delete from e_ftp_stg
                        where cd_ftp_rqt in (select cast(id as int) from temp_table where va_typ ='2c')
                        and dh_str < ?;
                        """
        ,new Object[]{fromNullableInstant(before)}));

        // delete smtp stage
        queries.add(new Query(
                """
                        delete from e_smtp_stg
                        where cd_smtp_rqt in (select cast(id as int) from temp_table where va_typ ='2d')
                        and dh_str < ?;
                        """
        ,new Object[]{fromNullableInstant(before)}));

        // delete smtp mail
        queries.add(new Query(
                """
                        delete from e_smtp_mail
                        where cd_smtp_rqt in (select cast(id as int) from temp_table where va_typ ='2d');
                        """
        ));

        // delete ldap stage
        queries.add(new Query(
                """
                        delete from e_ldap_stg
                        where cd_ldap_rqt in (select cast(id as int) from temp_table where va_typ ='2e')
                        and dh_str < ?;
                        """
        ,new Object[]{fromNullableInstant(before)}));

        // delete rest request
        queries.add(new Query(
                """
                        delete from e_rst_rqt
                        where id_rst_rqt in (select cast(id as int) from temp_table where va_typ ='2a');
                        """
        ));


        // delete db request
        queries.add(new Query(
                """
                        delete from e_dtb_rqt
                        where id_dtb_rqt in (select cast(id as int) from temp_table where va_typ ='2b');
                        """
        ));


        // delete ftp request
        queries.add(new Query(
                """
                        delete from e_ftp_rqt
                        where id_ftp_rqt in (select cast(id as int) from temp_table where va_typ ='2c');
                        """
        ));


        // delete smtp request
        queries.add(new Query(
                """
                        delete from e_smtp_rqt
                        where id_smtp_rqt in (select cast(id as int) from temp_table where va_typ ='2d');
                        """
        ));


        // delete ldap request
        queries.add(new Query(
                """
                        delete from e_ldap_rqt
                        where id_ldap_rqt in (select cast(id as int) from temp_table where va_typ ='2e');
                        """
        ));


        // delete local request
        queries.add(new Query(
                """
                        delete from e_lcl_rqt
                        where id_lcl_rqt in (select cast(id as int) from temp_table where va_typ ='2f');
                        """
        ));

        // delete rest session
        queries.add(new Query(
                """
                        delete from e_rst_ses
                        where id_ses in (select id from temp_table where va_typ ='1a');
                        """
        ));

        // delete main request
        queries.add(new Query(
                """
                        delete from e_main_ses
                        where id_ses in (select id from temp_table where va_typ ='1b');
                        """
        ));

        // delete instances
        queries.add(new Query(
                """
                        delete from e_env_ins
                        where id_ins in (select id from temp_table where va_typ ='0');
                        """
        ));

        //delete temp table
        queries.add(new Query(
                """
                        drop table if exists temp_table;
                        """
        ));




        return queries;
    }

    private static Timestamp fromNullableInstant(Instant instant) {
        return ofNullable(instant).map(Timestamp::from).orElse(null);
    }
}
