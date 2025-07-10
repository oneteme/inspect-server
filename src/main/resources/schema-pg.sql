CREATE TABLE IF NOT EXISTS e_main_ses (
    id_ses varchar,
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
    cd_ins varchar
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_rst_ses (
    id_ses varchar,
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
    va_thr varchar,  
    va_err_typ varchar, 
    va_err_msg varchar,
    va_nam varchar, 
    va_usr varchar,
    va_usr_agt varchar,
    va_cch_ctr varchar,
    va_msk int,
    cd_ins varchar 
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_rst_rqt (
    id_rst_rqt bigint,
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
    va_thr varchar,
    cd_prn_ses varchar, 
    cd_rmt_ses varchar,
    cd_ins varchar
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_smtp_rqt (
    id_smtp_rqt bigint,
    va_hst varchar,
    cd_prt int,
    va_usr varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_thr varchar,
    va_fail boolean,
    cd_prn_ses varchar,
    cd_ins varchar
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_smtp_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    cd_ord int,
    cd_smtp_rqt bigint -- index
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_smtp_mail (
    va_sbj varchar,
    va_cnt_typ varchar,
    va_frm varchar,
    va_rcp varchar,
    va_rpl varchar,
    va_sze bigint,
    cd_smtp_rqt bigint
);

CREATE TABLE IF NOT EXISTS e_ftp_rqt (
    id_ftp_rqt bigint,
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
    cd_prn_ses varchar, -- index
    cd_ins varchar
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_ftp_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_arg varchar,
    cd_ord int,
    cd_ftp_rqt bigint -- index
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_ldap_rqt (
    id_ldap_rqt bigint,
    va_hst varchar,
    cd_prt int,
    va_pcl varchar,
    va_usr varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_thr varchar,
    va_fail boolean,
    cd_prn_ses varchar, -- index
    cd_ins varchar
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_ldap_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_arg varchar,
    cd_ord int,
    cd_ldap_rqt bigint
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_dtb_rqt (
    id_dtb_rqt bigint, 
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
    cd_prn_ses varchar,
    cd_ins varchar
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_dtb_stg (
    va_nam varchar,
    dh_str timestamp(6),   
    dh_end timestamp(6),   
    va_cnt varchar,
    va_cmd varchar,
    cd_ord bigint,
    cd_dtb_rqt bigint
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_lcl_rqt (
    id_lcl_rqt bigint,
    va_typ varchar,
    va_nam varchar,  
    va_lct varchar,
    dh_str timestamp(6),  
    dh_end timestamp(6), 
    va_usr varchar,
    va_thr varchar,
    va_fail boolean,
    cd_prn_ses varchar,
    cd_ins varchar
)
PARTITION BY RANGE (dh_str);

CREATE TABLE IF NOT EXISTS e_exc_inf (
    va_typ varchar, 
    va_err_typ varchar,
    va_err_msg varchar,
    cd_ord int,
    cd_rqt bigint 
);

CREATE TABLE IF NOT EXISTS e_env_ins (
    id_ins varchar,
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
    va_hsh varchar
);

CREATE TABLE IF NOT EXISTS e_usr_acn (
    id_acn bigint,
    va_typ varchar,
    dh_str timestamp(6),
    va_nam varchar,
    va_nde_nam varchar,
    cd_prn_ses varchar
);


CREATE UNIQUE INDEX IF NOT EXISTS idx_main_ses_id_ses_dh_str ON e_main_ses(id_ses, dh_str);
CREATE INDEX IF NOT EXISTS idx_main_ses_cd_ins ON e_main_ses(cd_ins);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rst_ses_id_ses_dh_str ON e_rst_ses(id_ses, dh_str);
CREATE INDEX IF NOT EXISTS idx_rst_ses_cd_ins ON e_rst_ses(cd_ins);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rst_rqt_id_rst_rqt_dh_str ON e_rst_rqt(id_rst_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_cd_prn_ses ON e_rst_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_cd_rmt_ses ON e_rst_rqt(cd_rmt_ses);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_va_hst ON e_rst_rqt(va_hst);
CREATE UNIQUE INDEX IF NOT EXISTS idx_smtp_rqt_id_smtp_rqt_dh_str ON e_smtp_rqt(id_smtp_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_smtp_rqt_cd_prn_ses ON e_smtp_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_va_hst ON e_smtp_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_smtp_stg_cd_smtp_rqt ON e_smtp_stg(cd_smtp_rqt);
CREATE INDEX IF NOT EXISTS idx_smtp_mail_cd_smtp_rqt ON e_smtp_mail(cd_smtp_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ftp_rqt_id_ftp_rqt_dh_str ON e_ftp_rqt(id_ftp_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_ftp_rqt_cd_prn_ses ON e_ftp_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_va_hst ON e_ftp_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_ftp_stg_cd_ftp_rqt ON e_ftp_stg(cd_ftp_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ldap_rqt_id_ldap_rqt_dh_str ON e_ldap_rqt(id_ldap_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_ldap_rqt_cd_prn_ses ON e_ldap_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_va_hst ON e_ldap_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_ldap_stg_cd_ldap_rqt ON e_ldap_stg(cd_ldap_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_dtb_rqt_id_dtb_rqt_dh_str ON e_dtb_rqt(id_dtb_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_dtb_rqt_cd_prn_ses ON e_dtb_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_rst_rqt_va_hst ON e_dtb_rqt(va_hst);
CREATE INDEX IF NOT EXISTS idx_dtb_stg_cd_dtb_rqt ON e_dtb_stg(cd_dtb_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_lcl_rqt_id_lcl_rqt_dh_str ON e_lcl_rqt(id_lcl_rqt, dh_str);
CREATE INDEX IF NOT EXISTS idx_lcl_rqt_cd_prn_ses ON e_lcl_rqt(cd_prn_ses);
CREATE INDEX IF NOT EXISTS idx_exc_inf_cd_rqt ON e_exc_inf(cd_rqt);
CREATE UNIQUE INDEX IF NOT EXISTS idx_env_ins_id_ins ON e_env_ins(id_ins);
CREATE INDEX IF NOT EXISTS idx_env_ins_va_app_va_env ON e_env_ins(va_app, va_env);
CREATE UNIQUE INDEX IF NOT EXISTS idx_usr_acn_id_acn_dh_str ON e_usr_acn(id_acn);
CREATE INDEX IF NOT EXISTS idx_usr_acn_cd_prn_ses ON e_usr_acn(cd_prn_ses);