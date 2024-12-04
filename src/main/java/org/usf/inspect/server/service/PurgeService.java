package org.usf.inspect.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class PurgeService {

    private final JdbcTemplate template;



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
                            select eei.id_ins, 'instance' as va_typ
                            from e_env_ins eei
                            where dh_str < ? 
                            and va_env = ?

                        )
                        select * from deleted_instances;
                     """;//.formatted(appNameCondition,versionCondition);
        System.out.println(appName);
        System.out.println(version);
        System.out.println(query);
        // set parametre dynamicly
        template.query( query,
                        new Object[]{fromNullableInstant(before),env/*,appName,version*/}, (rs -> {

                }));
        return true;
    }

    private static Timestamp fromNullableInstant(Instant instant) {
        return ofNullable(instant).map(Timestamp::from).orElse(null);
    }

}


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