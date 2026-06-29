-- Update script: modification du type de la colonne cd_ord en bigint
-- Date: 2026-06-29

ALTER TABLE e_rst_ses_stg  ALTER COLUMN cd_ord TYPE bigint USING cd_ord::bigint;
ALTER TABLE e_rst_rqt_stg  ALTER COLUMN cd_ord TYPE bigint USING cd_ord::bigint;
ALTER TABLE e_smtp_stg     ALTER COLUMN cd_ord TYPE bigint USING cd_ord::bigint;
ALTER TABLE e_ftp_stg      ALTER COLUMN cd_ord TYPE bigint USING cd_ord::bigint;
ALTER TABLE e_ldap_stg     ALTER COLUMN cd_ord TYPE bigint USING cd_ord::bigint;
ALTER TABLE e_dtb_stg      ALTER COLUMN cd_ord TYPE bigint USING cd_ord::bigint;
ALTER TABLE e_exc_inf      ALTER COLUMN cd_ord TYPE bigint USING cd_ord::bigint;

