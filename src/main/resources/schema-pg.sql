set _schema = inspect_dev;

CREATE TABLE ${_schema}.e_main_ses (
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

CREATE TABLE ${_schema}.e_rst_ses ( 
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

CREATE TABLE ${_schema}.e_rst_rqt ( 
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
    cd_rmt_ses varchar 
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_smtp_rqt (
    id_smtp_rqt bigint,
    va_hst varchar,
    cd_prt int,
    va_usr varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_thr varchar,
    va_stt boolean,
    cd_prn_ses varchar
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_smtp_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    cd_ord int,
    cd_smtp_rqt bigint -- index
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_smtp_mail (
    va_sbj varchar,
    va_cnt_typ varchar,
    va_frm varchar,
    va_rcp varchar,
    va_rpl varchar,
    va_sze bigint,
    cd_smtp_rqt bigint
);

CREATE TABLE ${_schema}.e_ftp_rqt (
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
    va_stt boolean,
    cd_prn_ses varchar -- index
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_ftp_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_arg varchar,
    cd_ord int,
    cd_ftp_rqt bigint -- index
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_ldap_rqt (
    id_ldap_rqt bigint,
    va_hst varchar,
    cd_prt int,
    va_pcl varchar,
    va_usr varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_thr varchar,
    va_stt boolean,
    cd_prn_ses varchar -- index
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_ldap_stg (
    va_nam varchar,
    dh_str timestamp(6),
    dh_end timestamp(6),
    va_arg varchar,
    cd_ord int,
    cd_ldap_rqt bigint
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_dtb_rqt ( 
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
    VA_STT boolean,
    cd_prn_ses varchar 
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_dtb_stg ( 
    va_nam varchar,
    dh_str timestamp(6),   
    dh_end timestamp(6),   
    va_cnt varchar,
    va_cmd varchar,
    cd_ord bigint,
    cd_dtb_rqt bigint
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_lcl_rqt ( 
    id_lcl_rqt bigint,
    va_nam varchar,  
    VA_LCT varchar,  
    dh_str timestamp(6),  
    dh_end timestamp(6), 
    va_usr varchar,
    va_thr varchar,
    va_stt boolean,
    cd_prn_ses varchar 
)
PARTITION BY RANGE (dh_str);

CREATE TABLE ${_schema}.e_exc_inf (
    va_typ varchar, 
    va_err_typ varchar,
    va_err_msg varchar,
    cd_ord int,
    cd_rqt bigint 
);

CREATE TABLE ${_schema}.e_env_ins (
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

CREATE UNIQUE INDEX idx_main_ses_id_ses_dh_str ON ${_schema}.e_main_ses(id_ses,dh_str);
CREATE INDEX idx_main_ses_cd_ins ON ${_schema}.e_main_ses(cd_ins);
CREATE UNIQUE INDEX idx_rst_ses_id_ses_dh_str ON ${_schema}.e_rst_ses(id_ses,dh_str);
CREATE INDEX idx_rst_ses_cd_ins ON ${_schema}.e_rst_ses(cd_ins);
CREATE UNIQUE INDEX idx_rst_rqt_cd_ins_dh_str ON ${_schema}.e_rst_rqt(id_rst_rqt,dh_str);
CREATE INDEX idx_rst_rqt_cd_prn_ses ON ${_schema}.e_rst_rqt(cd_prn_ses);
CREATE UNIQUE INDEX idx_smtp_rqt_id_smtp_rqt_dh_str ON ${_schema}.e_smtp_rqt(id_smtp_rqt,dh_str);
CREATE INDEX idx_smtp_rqt_cd_prn_ses ON ${_schema}.e_smtp_rqt(cd_prn_ses);
CREATE INDEX idx_smtp_stg_cd_smtp_rqt ON ${_schema}.e_smtp_stg(cd_smtp_rqt);
CREATE INDEX idx_smtp_mail_cd_smtp_rqt ON ${_schema}.e_smtp_mail(cd_smtp_rqt);
CREATE UNIQUE INDEX idx_ftp_rqt_id_ftp_rqt_dh_str ON ${_schema}.e_ftp_rqt(id_ftp_rqt,dh_str);
CREATE INDEX idx_ftp_rqt_cd_prn_ses ON ${_schema}.e_ftp_rqt(cd_prn_ses);
CREATE INDEX idx_ftp_stg_cd_ftp_rqt ON ${_schema}.e_ftp_stg(cd_ftp_rqt);
CREATE UNIQUE INDEX idx_ldap_rqt_id_ldap_rqt_dh_str ON ${_schema}.e_ldap_rqt(id_ldap_rqt,dh_str);
CREATE INDEX idx_ldap_rqt_cd_prn_ses ON ${_schema}.e_ldap_rqt(cd_prn_ses);
CREATE INDEX idx_ldap_stg_cd_ldap_rqt ON ${_schema}.e_ldap_stg(cd_ldap_rqt);
CREATE UNIQUE INDEX idx_dtb_rqt_id_dtb_rqt_dh_str ON ${_schema}.e_dtb_rqt(id_dtb_rqt,dh_str);
CREATE INDEX idx_dtb_rqt_cd_prn_ses ON ${_schema}.e_dtb_rqt(cd_prn_ses);
CREATE INDEX idx_dtb_stg_cd_dtb_rqt ON ${_schema}.e_dtb_stg(cd_dtb_rqt);
CREATE UNIQUE INDEX idx_lcl_rqt_id_lcl_rqt_dh_str ON ${_schema}.e_lcl_rqt(id_lcl_rqt,dh_str);
CREATE INDEX idx_lcl_rqt_cd_prn_ses ON ${_schema}.e_lcl_rqt(cd_prn_ses);
CREATE INDEX idx_exc_inf_cd_rqt ON ${_schema}.e_exc_inf(cd_rqt);
CREATE UNIQUE INDEX idx_env_ins_id_ins ON ${_schema}.e_env_ins(id_ins);
CREATE INDEX idx_env_ins_va_app_va_env ON ${_schema}.e_env_ins(va_app, va_env);

set _debut = '2024-07-01 00:00:00';
set _fin = '2024-08-01 00:00:00';
set _suffix = '2024_07';

CREATE TABLE ${_schema}.e_rst_rqt_partitioned_${_suffix} PARTITION OF ${_schema}.e_rst_rqt FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_main_ses_partitioned_${_suffix} PARTITION OF ${_schema}.e_main_ses FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_rst_ses_partitioned_${_suffix} PARTITION OF ${_schema}.e_rst_ses FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_smtp_rqt_partitioned_${_suffix} PARTITION OF ${_schema}.e_smtp_rqt FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_smtp_stg_partitioned_${_suffix} PARTITION OF ${_schema}.e_smtp_stg FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_ftp_rqt_partitioned_${_suffix} PARTITION OF ${_schema}.e_ftp_rqt FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_ftp_stg_partitioned_${_suffix} PARTITION OF ${_schema}.e_ftp_stg FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_ldap_rqt_partitioned_${_suffix} PARTITION OF ${_schema}.e_ldap_rqt FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_ldap_stg_partitioned_${_suffix} PARTITION OF ${_schema}.e_ldap_stg FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_dtb_rqt_partitioned_${_suffix} PARTITION OF ${_schema}.e_dtb_rqt FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_dtb_stg_partitioned_${_suffix} PARTITION OF ${_schema}.e_dtb_stg FOR VALUES FROM (${_debut}) TO (${_fin});
CREATE TABLE ${_schema}.e_lcl_rqt_partitioned_${_suffix} PARTITION OF ${_schema}.e_lcl_rqt FOR VALUES FROM (${_debut}) TO (${_fin});