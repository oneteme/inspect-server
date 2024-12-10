package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.usf.inspect.core.ExceptionInfo;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class PurgeService {

    private final JdbcTemplate template;
    private static final Logger logger = Logger.getLogger(PurgeService.class.getName());


    /*
         environement [1..1]
         application [0..n]
         before [max]
         version [0..n]
     */
    public boolean purgeData(String env, List<String> appName, Instant before, List<String> version ){
        // String appNameCondition =  (appName != null && !appName.isEmpty())? "and va_app in("+ String.join(", ", "?".repeat(appName.size())) +")" : "";
        // String versionCondition =  (version != null && !version.isEmpty())? "and va_vrs in("+ String.join(", ", "?".repeat(version.size())) +")" : "";
        //                            %s
        //                           %s
        String query = """
                   with deleted_instances as(
                       select eei.id_ins as id, 'e_env_ins' as va_typ
                       from e_env_ins eei
                       where dh_str < '%s'
                       and va_env = '%s'
                   ),
                   deleted_sessions as(
                      select id_ses as id, 'r' as va_typ
                      from e_rst_ses ers
                      where ers.cd_ins in (select id from deleted_instances)
                      and ers.dh_str < '%s'
                      union
                      select id_ses as id, 's' as va_typ
                      from e_main_ses ems
                      where ems.cd_ins in (select id from deleted_instances)
                      and ems.dh_str < '%s'
                  ),
                  deleted_rest_requests as (
                      select id_rst_rqt as id, 'rest_requests' as va_typ
                      from e_rst_rqt err
                      where err.cd_prn_ses in (select id from deleted_sessions)
                      and err.dh_str < '%s'
                  ),
                  deleted_db_requests as(
                  	select id_dtb_rqt as id, 'db_requests' as va_typ
                  	from e_dtb_rqt edr
                  	where edr.cd_prn_ses in (select id from deleted_sessions)
                  	and edr.dh_str < '%s'
                  ),
                  deleted_ftp_requests as (
                  	select id_ftp_rqt as id, 'ftp_sessions' as va_typ
                  	from e_ftp_rqt efr
                  	where efr.cd_prn_ses in (select id from deleted_sessions)
                  	and efr.dh_str < '%s'
                  ),
                  deleted_smtp_requests as (
                  	select id_smtp_rqt as id, 'smtp_sessions' as va_typ
                  	from e_smtp_rqt esr
                  	where esr.cd_prn_ses in (select id from deleted_sessions)
                  	and esr.dh_str < '%s'
                  ),
                  deleted_ldap_requests as (
                  	select id_ldap_rqt as id, 'ldap_sessions' as va_typ
                  	from e_ldap_rqt elr
                  	where elr.cd_prn_ses in (select id  from deleted_sessions)
                  	and elr.dh_str < '%s'
                  ),
                  deleted_local_requests as (
                  	select id_lcl_rqt as id, 'local_requests' as va_typ
                  	from e_lcl_rqt elr
                  	where elr.cd_prn_ses in (select id from deleted_sessions)
                  	and elr.dh_str < '%s'
                  )
                  delete from e_exc_inf
                  where cd_rqt in (select id from deleted_rest_requests)
                  and va_typ = 'REST'
                  ;
                  delete from e_exc_inf
                  where cd_rqt in (select id from deleted_db_requests)
                  and va_typ = 'JDBC'
                  ;
                  delete from e_exc_inf
                  where cd_rqt in (select id from deleted_ftp_requests)
                  and va_typ = 'FTP';
                  
                  delete from e_exc_inf
                  where cd_rqt in (select id from deleted_smtp_requests)
                  and va_typ = 'SMTP';
                  
                  delete from e_exc_inf
                  where cd_rqt in (select id from deleted_ldap_requests)
                  and eei.va_typ = 'LDAP';
                  
                  delete from e_exc_inf
                  where cd_rqt in (select id from deleted_local_requests)
                  and va_typ = 'LOCAL';
                  
                  delete from e_dtb_stg eds
                  where eds.cd_dtb_rqt in (select id from deleted_db_requests)
                  and eds.dh_str < '%s';
                  
                  delete from e_ftp_stg efs
                  where efs.cd_ftp_rqt in (select id from deleted_ftp_requests)
                  and efs.dh_str < '%s';
                  
                  delete from e_smtp_stg ess
                  where ess.cd_smtp_rqt in (select id from deleted_smtp_requests)
                  and ess.dh_str < '%s';
                  
                  delete from e_smtp_mail esm
                  where esm.cd_smtp_rqt in (select id from deleted_smtp_requests)
                  and esm.dh_str < '%s';
                  
                  delete from e_ldap_stg els
                  where els.cd_ldap_rqt in (select id from deleted_ldap_requests)
                  and els.dh_str < '%s';
                  
                  delete from e_rst_rqt err
                  where err.id_rst_rqt in (select id from deleted_rest_requests);
                  
                  delete from e_dtb_rqt edr
                  where edr.id_dtb_rqt in (select id from deleted_db_requests);
                  
                  delete from e_ftp_rqt efr
                  where eft.id_ftp_rqt in (select id from deleted_ftp_requests);
                  
                  delete from e_smtp_rqt esr
                  where esr.id_smtp_rqt in (select id from deleted_smtp_requests);
                  
                  delete from e_ldap_rqt elr
                  where elr.id_ldap_rqt in (select id from deleted_ldap_requests);
                  
                  delete from e_lcl_rqt elr
                  where elr.id_lcl_rqt in (select id from deleted_local_requests);

                  delete from e_rst_ses ers
                  where ers.id_ses in (select id from deleted_instances);
                  
                  delete from e_main_ses ems
                  where ems.id_ses in (select id from deleted_instances);
                  
                  delete from e_env_ins eei
                  where eei.id_ins in (select id from deleted_instances);
                  
                """.formatted(fromNullableInstant(before),
                env,
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before),
                fromNullableInstant(before));

        // set parametre dynamicly
        logger.log(Level.INFO,"+ Purging old Data, parameters in entry");
        logger.log(Level.INFO,"    - Environment: "+env);
        logger.log(Level.INFO,"    - Start: "+before);

        template.execute(query);

        return true;
    }

    private static Timestamp fromNullableInstant(Instant instant) {
        return ofNullable(instant).map(Timestamp::from).orElse(null);
    }

}

        /*var restSessionQuery = "delete from e_rst_ses where id_ses in ("+ (removedIds.containsKey("e_rst_ses") && !removedIds.get("e_rst_ses").isEmpty()? String.join(", ", "?".repeat(removedIds.get("e_rst_ses").size())) +")" : "") +")";
        Object[] params  = removedIds.values().stream().flatMap(List::stream).toArray();*/

// script to use
/*
* with deleted_instances as(
	select eei.id_ins, 'instance' as va_typ
	from e_env_ins eei
	where eei.dh_str >= '2024-11-29 00:00:00'
),deleted_rest_sessions as(
	select id_ses, 'rest_session' as va_typ
	from e_rst_ses ers
	where ers.cd_ins in (select id_ins from deleted_instances)
	and ers.dh_str >= '2024-11-29 00:00:00'
),deleted_main_sessions as (
	select id_ses, 'main_session' as va_typ
	from e_main_ses ems
	where ems.cd_ins in (select id_ins from deleted_instances)
	and ems.dh_str >= '2024-11-29 00:00:00'
),deleted_sessions as (
	select * from deleted_rest_sessions
	union
	select * from deleted_main_sessions
),deleted_rest_requests as(
	select id_rst_rqt, 'rest_requests' as va_typ
	from e_rst_rqt err
	where err.cd_prn_ses in (select id_ses from deleted_sessions)
	and err.dh_str >= '2024-11-29 00:00:00'
),
deleted_db_requests as(
	select id_dtb_rqt, 'db_requests' as va_typ
	from e_dtb_rqt edr
	where edr.cd_prn_ses in (select id_ses from deleted_sessions)
	and edr.dh_str >= '2024-11-29 00:00:00'
),
deleted_ftp_requests as (
	select id_ftp_rqt, 'ftp_sessions' as va_typ
	from e_ftp_rqt efr
	where efr.cd_prn_ses in (select id_ses from deleted_sessions)
	and efr.dh_str >= '2024-11-29 00:00:00'
),
deleted_smtp_requests as (
	select id_smtp_rqt, 'smtp_sessions' as va_typ
	from e_smtp_rqt esr
	where esr.cd_prn_ses in (select id_ses from deleted_sessions)
	and esr.dh_str >= '2024-11-29 00:00:00'
),
deleted_ldap_requests as (
	select id_ldap_rqt, 'ldap_sessions' as va_typ
	from e_ldap_rqt elr
	where elr.cd_prn_ses in (select id_ses from deleted_sessions)
	and elr.dh_str >= '2024-11-29 00:00:00'
),
deleted_local_requests as (
	select id_lcl_rqt, 'local_requests' as va_typ
	from e_lcl_rqt elr
	where elr.cd_prn_ses in (select id_ses from deleted_sessions)
	and elr.dh_str >= '2024-11-29 00:00:00'
)



select count(*), '1' from deleted_rest_requests
union
select count(*), '2' from deleted_db_requests
union
select count(*), '3' from deleted_ftp_requests
union
select count(*), '4' from deleted_smtp_requests
union
select count(*), '5' from deleted_ldap_requests
union
select count(*), '6' from deleted_local_requests;

--select * from deleted_instances
--union
--select * from deleted_rest_sessions
--union
--select * from deleted_main_sessions;

* */