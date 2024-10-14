ALTER TABLE e_ftp_rqt ADD COLUMN va_stt BOOLEAN;
ALTER TABLE e_lcl_rqt  ADD COLUMN va_stt BOOLEAN;
ALTER TABLE e_ldap_rqt  ADD COLUMN va_stt BOOLEAN;
ALTER TABLE e_smtp_rqt  ADD COLUMN va_stt BOOLEAN;
ALTER TABLE e_dtb_rqt  ADD COLUMN va_stt BOOLEAN;
ALTER TABLE e_dtb_rqt  ADD COLUMN va_sch VARCHAR;

with subquery as (
    select edr.id_ftp_rqt, eei.cd_rqt, case when eei.cd_rqt is null then true else false end as complete
    from e_ftp_rqt edr
             left join e_exc_inf eei on eei.cd_rqt = edr.id_ftp_rqt and eei.va_typ = 'LOCAL'
)
update e_ftp_rqt
set va_stt = subquery.complete
    from subquery
where e_ftp_rqt.id_ftp_rqt = subquery.id_ftp_rqt;