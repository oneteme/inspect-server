
CREATE TABLE IF NOT EXISTS e_main_req (
	id_main_req varchar NOT NULL,
	va_name varchar NULL,
	va_usr varchar NULL,
	dh_dbt timestamp(3) NULL,
	dh_fin timestamp(3) NULL,
	lnch varchar NULL,
	loc varchar NULL,
	va_thred varchar NULL,
	va_app_nme varchar NULL,
	va_vrs varchar NULL,
	va_adrs varchar NULL,
	va_env varchar NULL,
	va_os varchar NULL,
	va_re varchar NULL,
	va_err_cls varchar NULL,
	va_err_msg varchar NULL
);

CREATE TABLE IF NOT EXISTS e_in_req (
	id_in_req varchar NOT NULL,
	va_mth varchar NULL,
	va_prtcl varchar NULL,
	va_hst varchar NULL,
	cd_prt int4 NULL,
	va_pth varchar NULL,
	va_qry varchar NULL,
	va_cnt_typ varchar NULL,
	va_auth varchar NULL,
	cd_stt int4 NULL,
	va_i_sze int8 NULL,
	va_o_sze int8 NULL,
	dh_dbt timestamp(3) NULL,
	dh_fin timestamp(3) NULL,
	va_thred varchar NULL,
	va_err_cls varchar NULL,
	va_err_msg varchar NULL,
	va_api_nme varchar NULL,
	va_usr varchar NULL,
	va_app_nme varchar NULL,
	va_vrs varchar NULL,
	va_adrs varchar NULL,
	va_env varchar NULL,
	va_os varchar NULL,
	va_re varchar NULL
);

CREATE TABLE IF NOT EXISTS e_out_req (
	id_out_req varchar NULL,
	va_mth varchar NULL,
	va_prtcl varchar NULL,
	va_hst varchar NULL,
	cd_prt int4 NULL,
	va_pth varchar NULL,
	va_qry varchar NULL,
	va_cnt_typ varchar NULL,
	va_auth varchar NULL,
	cd_stt int4 NULL,
	va_i_sze int8 NULL,
	va_o_sze int8 NULL,
	dh_dbt timestamp(3) NULL,
	dh_fin timestamp(3) NULL,
	va_thred varchar NULL,
	va_err_cls varchar NULL,
	va_err_msg varchar NULL,
	cd_in_req varchar NULL
);

CREATE TABLE IF NOT EXISTS e_out_qry (
	id_out_qry int8 NULL,
	va_hst varchar NULL,
	cd_prt int4 NULL,
	va_schma varchar NULL,
	dh_dbt timestamp(3) NULL,
	dh_fin timestamp(3) NULL,
	va_usr varchar NULL,
	va_thred varchar NULL,
	va_drv varchar NULL,
	va_db_nme varchar NULL,
	va_db_vrs varchar NULL,
	va_cmplt char NULL,
	cd_in_req varchar NULL
);

CREATE TABLE IF NOT EXISTS e_out_stg (
    va_name varchar NULL,
    loc varchar NULL,
    dh_dbt timestamp(3) NULL,
    dh_fin timestamp(3) NULL,
    va_usr varchar NULL,
    va_thred varchar NULL,
    va_err_cls varchar NULL,
    va_err_msg varchar NULL,
    cd_in_req varchar NULL
);

CREATE TABLE IF NOT EXISTS e_db_act (
	va_typ varchar NULL,
	dh_dbt timestamp(3) NULL,
	dh_fin timestamp(3) NULL,
	va_err_cls varchar NULL,
	va_err_msg varchar NULL,
	cd_out_qry int8 NULL
);
