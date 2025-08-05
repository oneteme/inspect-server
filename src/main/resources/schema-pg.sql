CREATE TABLE IF NOT EXISTS e_main_ses (
    id_ses UUID,
    va_typ varchar,
    va_nam varchar,
    va_usr varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_lct varchar,
    va_thr varchar,
    va_err_typ varchar,
    va_err_msg varchar,
    va_msk int,
    cd_ins UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_rst_ses (
    id_ses UUID,
    va_mth varchar,
    va_pcl varchar, 
    va_hst varchar,
    cd_prt int,
    va_pth varchar,
    va_qry varchar,
    va_cnt_typ varchar,
    va_ath_sch varchar, 
    cd_stt int,
    va_i_sze bigint,
    va_o_sze bigint,
    va_i_cnt_enc varchar,
    va_o_cnt_enc varchar,
    dh_str timestamp(6), 
    dh_end timestamp(6),
    va_bdy_cnt varchar,
    va_thr varchar,
    va_err_typ varchar,
    va_err_msg varchar,
    va_nam varchar,
    va_usr varchar,
    va_usr_agt varchar,
    va_cch_ctr varchar,
    va_msk int,
    cd_ins UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_rst_ses_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    cd_ord smallint,
    cd_prn_ses UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_rst_rqt (
    id_rst_rqt UUID,
    va_mth varchar,
    va_pcl varchar,
    va_hst varchar,
    cd_prt int,
    va_pth varchar,
    va_qry varchar,
    va_cnt_typ varchar,
    va_ath_sch varchar,
    cd_stt int,
    va_i_sze bigint,
    va_o_sze bigint,
    va_i_cnt_enc varchar,
    va_o_cnt_enc varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_bdy_cnt varchar,
    va_thr varchar,
    cd_prn_ses UUID,
    cd_ins UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_rst_rqt_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    cd_ord smallint,
    cd_rst_rqt UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_smtp_rqt (
    id_smtp_rqt UUID,
    va_hst varchar,
    cd_prt int,
    va_usr varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_thr varchar,
    va_fail boolean,
    cd_prn_ses UUID,
    cd_ins UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_smtp_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    cd_ord smallint,
    cd_smtp_rqt UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_smtp_mail (
    va_sbj varchar,
    va_cnt_typ varchar,
    va_frm varchar,
    va_rcp varchar,
    va_rpl varchar,
    va_sze bigint,
    cd_smtp_rqt UUID
);

CREATE TABLE IF NOT EXISTS e_ftp_rqt (
    id_ftp_rqt UUID,
    va_hst varchar,
    cd_prt int,
    va_pcl varchar,
    va_srv_vrs varchar,
    va_clt_vrs varchar,
    va_usr varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_thr varchar,
    va_fail boolean,
    cd_prn_ses UUID, -- index
    cd_ins UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_ftp_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_arg varchar,
    cd_ord smallint,
    cd_ftp_rqt UUID -- index
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_ldap_rqt (
    id_ldap_rqt UUID,
    va_hst varchar,
    cd_prt int,
    va_pcl varchar,
    va_usr varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_thr varchar,
    va_fail boolean,
    cd_prn_ses UUID, -- index
    cd_ins UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_ldap_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_arg varchar,
    cd_ord smallint,
    cd_ldap_rqt UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_dtb_rqt (
    id_dtb_rqt UUID,
    va_hst varchar,
    cd_prt int,
    va_nam varchar,
    va_sch varchar,
    dh_str timestamp(6), 
    dh_end timestamp(6), 
    va_usr varchar,
    va_thr varchar, 
    va_drv varchar,
    va_prd_nam varchar, 
    va_prd_vrs varchar, 
    va_cmd varchar,
    va_fail boolean,
    cd_prn_ses UUID,
    cd_ins UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_dtb_stg (
    va_nam varchar,
    dh_str timestamp(6),   
    dh_end timestamp(6),   
    va_cnt varchar,
    va_cmd varchar,
    cd_ord smallint,
    cd_dtb_rqt UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_lcl_rqt (
    id_lcl_rqt UUID,
    va_typ varchar,
    va_nam varchar,  
    va_lct varchar,
    dh_str timestamp(6),  
    dh_end timestamp(6), 
    va_usr varchar,
    va_thr varchar,
    va_fail boolean,
    cd_prn_ses UUID,
    cd_ins UUID
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_exc_inf (
    va_typ varchar, 
    va_err_typ varchar,
    va_err_msg varchar,
    va_stk json,
    cd_ord smallint,
    cd_rqt UUID
);

CREATE TABLE IF NOT EXISTS e_env_ins (
    id_ins UUID,
    va_typ varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_app varchar,
    va_vrs varchar,
    va_adr varchar,
    va_env varchar,
    va_os varchar,
    va_re varchar,
    va_usr varchar,
    va_clr varchar,
    va_brch varchar,
    va_hsh varchar,
    va_cnf json,
    va_rsr json,
    va_add_prp json
);

CREATE TABLE IF NOT EXISTS e_usr_acn (
    id_acn bigint,
    va_typ varchar,
    dh_str timestamp(6),
    va_nam varchar,
    va_nde_nam varchar,
    cd_prn_ses varchar
);

create table if not exists e_ins_trc (
    va_pnd int,
    va_atp int,
    va_ses_sze int,
    dh_str timestamp(6),
    va_fln varchar,
    cd_ins uuid
);

create table if not exists e_log_ent (
    dh_str timestamp(6),
    va_lvl varchar,
    va_msg varchar,
    va_stk json,
    cd_prn_ses uuid,
    cd_ins uuid
);

create table if not exists e_rsc_usg (
    dh_str timestamp(6),
    va_usd_hep int,
    va_cmt_hep int,
    va_usd_met int,
    va_cmt_met int,
    va_usd_dsk int,
    cd_ins uuid
);

create table if not exists e_cmp_mtc (
    id_cmp_mtc uuid,
    va_typ smallint
);

-- Ajouter les index du cd instance dans les requests

CREATE UNIQUE INDEX IF NOT EXISTS idx_main_ses_id_ses_dh_str ON e_main_ses(id_ses, dh_str);
CREATE INDEX IF NOT EXISTS idx_main_ses_cd_ins ON e_main_ses(cd_ins);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rst_ses_id_ses_dh_str ON e_rst_ses(id_ses, dh_str);
CREATE INDEX IF NOT EXISTS idx_rst_ses_cd_ins ON e_rst_ses(cd_ins);
CREATE INDEX IF NOT EXISTS idx_rst_ses_stg_cd_prn_ses ON e_rst_ses_stg(cd_prn_ses);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rst_rqt_id_rst_rqt_dh_str ON e_rst_rqt(id_rst_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_cd_prn_ses ON e_rst_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_va_hst ON e_rst_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_cd_ins ON e_rst_rqt(cd_ins);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_stg_cd_rst_rqt ON e_rst_rqt_stg(cd_rst_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_smtp_rqt_id_smtp_rqt_dh_str ON e_smtp_rqt(id_smtp_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_smtp_rqt_cd_prn_ses ON e_smtp_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_smtp_rqt_va_hst ON e_smtp_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_smtp_rqt_cd_ins ON e_smtp_rqt(cd_ins);
CREATE INDEX IF NOT EXISTS idx_smtp_stg_cd_smtp_rqt ON e_smtp_stg(cd_smtp_rqt);
CREATE INDEX IF NOT EXISTS idx_smtp_mail_cd_smtp_rqt ON e_smtp_mail(cd_smtp_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ftp_rqt_id_ftp_rqt_dh_str ON e_ftp_rqt(id_ftp_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_ftp_rqt_cd_prn_ses ON e_ftp_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_ftp_rqt_va_hst ON e_ftp_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_ftp_rqt_cd_ins ON e_ftp_rqt(cd_ins);
CREATE INDEX IF NOT EXISTS idx_ftp_stg_cd_ftp_rqt ON e_ftp_stg(cd_ftp_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ldap_rqt_id_ldap_rqt_dh_str ON e_ldap_rqt(id_ldap_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_ldap_rqt_cd_prn_ses ON e_ldap_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_ldap_rqt_va_hst ON e_ldap_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_ldap_rqt_cd_ins ON e_ldap_rqt(cd_ins);
CREATE INDEX IF NOT EXISTS idx_ldap_stg_cd_ldap_rqt ON e_ldap_stg(cd_ldap_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_dtb_rqt_id_dtb_rqt_dh_str ON e_dtb_rqt(id_dtb_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_dtb_rqt_cd_prn_ses ON e_dtb_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_dtb_rqt_va_hst ON e_dtb_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_dtb_rqt_cd_ins ON e_dtb_rqt(cd_ins);
CREATE INDEX IF NOT EXISTS idx_dtb_stg_cd_dtb_rqt ON e_dtb_stg(cd_dtb_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_lcl_rqt_id_lcl_rqt_dh_str ON e_lcl_rqt(id_lcl_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_lcl_rqt_cd_prn_ses ON e_lcl_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_exc_inf_cd_rqt ON e_exc_inf(cd_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_env_ins_id_ins ON e_env_ins(id_ins);
CREATE INDEX IF NOT EXISTS idx_env_ins_va_app_va_env ON e_env_ins(va_app, va_env);
CREATE UNIQUE INDEX IF NOT EXISTS idx_usr_acn_id_acn_dh_str ON e_usr_acn(id_acn);
CREATE INDEX IF NOT EXISTS idx_usr_acn_cd_prn_ses ON e_usr_acn(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_ins_trc_cd_ins ON e_ins_trc(cd_ins);
CREATE INDEX IF NOT EXISTS idx_log_ent_cd_ins ON e_log_ent(cd_ins);
CREATE INDEX IF NOT EXISTS idx_log_ent_cd_prn_ses ON e_log_ent(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_rsc_usg_cd_ins ON e_rsc_usg(cd_ins);
CREATE UNIQUE INDEX IF NOT EXISTS idx_cmp_mtc_id_cmp_mtc_va_typ ON e_cmp_mtc(id_cmp_mtc, va_typ);