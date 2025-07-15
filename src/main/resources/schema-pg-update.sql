ALTER TABLE E_ENV_INS add IF NOT EXISTS VA_BRCH VARCHAR;
ALTER TABLE E_ENV_INS add IF NOT EXISTS VA_HSH VARCHAR;
ALTER TABLE E_ENV_INS add IF NOT EXISTS DH_END TIMESTAMP(6);
ALTER TABLE E_DTB_STG add IF NOT EXISTS VA_CMD VARCHAR;
ALTER TABLE E_LCL_RQT add IF NOT EXISTS VA_TYP VARCHAR;
ALTER TABLE E_RST_RQT add IF NOT EXISTS CD_INS VARCHAR;
ALTER TABLE E_SMTP_RQT add IF NOT EXISTS CD_INS VARCHAR;
ALTER TABLE E_FTP_RQT add IF NOT EXISTS CD_INS VARCHAR;
ALTER TABLE E_LDAP_RQT add IF NOT EXISTS CD_INS VARCHAR;
ALTER TABLE E_DTB_RQT add IF NOT EXISTS CD_INS VARCHAR;
ALTER TABLE E_LCL_RQT add IF NOT EXISTS CD_INS VARCHAR;
ALTER TABLE E_SMTP_RQT RENAME VA_STT TO VA_FAIL;
ALTER TABLE E_FTP_RQT RENAME VA_STT TO VA_FAIL;
ALTER TABLE E_LDAP_RQT RENAME VA_STT TO VA_FAIL;
ALTER TABLE E_DTB_RQT RENAME VA_STT TO VA_FAIL;
ALTER TABLE E_LCL_RQT RENAME VA_STT TO VA_FAIL;
create table if not exists e_ins_trc (
    va_pnd int,
    va_atp int,
    va_ses_sze int,
    dh_str timestamp(6),
    cd_ins uuid
);

create table if not exists e_log_ent (
    dh_str timestamp(6),
    va_lvl varchar,
    va_msg varchar,
    cd_ses uuid,
    cd_ins uuid
);

create table if not exists e_rsc_usg (
    dh_str timestamp(6),
    va_low_hep int,
    va_hig_hep int,
    va_low_met int,
    va_hig_met int,
    cd_ins uuid
);