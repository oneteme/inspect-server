BEGIN;

-- Migration des anciens champs boolean va_fail vers cd_stt sur les tables request
-- true  -> 500 (SERVER_ERROR)
-- false -> 200 (SUCCESS)

ALTER TABLE e_smtp_rqt ADD COLUMN IF NOT EXISTS cd_stt SMALLINT;
UPDATE e_smtp_rqt
SET cd_stt = CASE
                 WHEN va_fail = true THEN 500
                 WHEN va_fail = false THEN 200
                 ELSE NULL
    END;
ALTER TABLE e_smtp_rqt DROP COLUMN IF EXISTS va_fail;

ALTER TABLE e_ftp_rqt ADD COLUMN IF NOT EXISTS cd_stt SMALLINT;
UPDATE e_ftp_rqt
SET cd_stt = CASE
                 WHEN va_fail = true THEN 500
                 WHEN va_fail = false THEN 200
                 ELSE NULL
    END;
ALTER TABLE e_ftp_rqt DROP COLUMN IF EXISTS va_fail;

ALTER TABLE e_ldap_rqt ADD COLUMN IF NOT EXISTS cd_stt SMALLINT;
UPDATE e_ldap_rqt
SET cd_stt = CASE
                 WHEN va_fail = true THEN 500
                 WHEN va_fail = false THEN 200
                 ELSE NULL
    END;
ALTER TABLE e_ldap_rqt DROP COLUMN IF EXISTS va_fail;

ALTER TABLE e_dtb_rqt ADD COLUMN IF NOT EXISTS cd_stt SMALLINT;
UPDATE e_dtb_rqt
SET cd_stt = CASE
                 WHEN va_fail = true THEN 500
                 WHEN va_fail = false THEN 200
                 ELSE NULL
    END;
ALTER TABLE e_dtb_rqt DROP COLUMN IF EXISTS va_fail;

ALTER TABLE e_lcl_rqt ADD COLUMN IF NOT EXISTS cd_stt SMALLINT;
UPDATE e_lcl_rqt
SET cd_stt = CASE
                 WHEN va_fail = true THEN 500
                 WHEN va_fail = false THEN 200
                 ELSE NULL
    END;
ALTER TABLE e_lcl_rqt DROP COLUMN IF EXISTS va_fail;

-- Migration des anciennes donnees vers cd_stt sur e_main_ses
-- si va_err_typ est renseigne => erreur serveur
-- sinon => succes
ALTER TABLE e_main_ses ADD COLUMN IF NOT EXISTS cd_stt SMALLINT;

UPDATE e_main_ses
SET cd_stt = CASE
                 WHEN va_err_typ IS NOT NULL THEN 500
                 WHEN dh_end is not null then  200
    END;

-- Migration du type de session principal legacy
UPDATE e_main_ses
SET va_typ = 'SCHEDULE'
WHERE va_typ = 'BATCH';

ALTER TABLE e_main_ses DROP COLUMN IF EXISTS va_err_typ;
ALTER TABLE e_main_ses DROP COLUMN IF EXISTS va_err_msg;
ALTER TABLE e_main_ses DROP COLUMN IF EXISTS va_stk;
ALTER TABLE e_main_ses
    ADD COLUMN IF NOT EXISTS va_asy_drt BIGINT;

-- Nettoyage des exceptions legacy
ALTER TABLE e_rst_ses DROP COLUMN IF EXISTS va_err_typ;
ALTER TABLE e_rst_ses DROP COLUMN IF EXISTS va_err_msg;
ALTER TABLE e_rst_ses DROP COLUMN IF EXISTS va_stk;
ALTER TABLE e_rst_ses
    ADD COLUMN IF NOT EXISTS va_asy_drt BIGINT;

-- Migration du type du statut HTTP : INT -> SMALLINT
ALTER TABLE e_rst_ses
ALTER COLUMN cd_stt TYPE SMALLINT;

ALTER TABLE e_rst_rqt
ALTER COLUMN cd_stt TYPE SMALLINT;


-- Migration des stages vers payload JSON
ALTER TABLE e_ftp_stg ADD COLUMN IF NOT EXISTS va_pld json;
ALTER TABLE e_ldap_stg ADD COLUMN IF NOT EXISTS va_pld json;
ALTER TABLE e_dtb_stg ADD COLUMN IF NOT EXISTS va_pld json;

-- Nettoyage des anciens champs legacy
ALTER TABLE e_ftp_stg DROP COLUMN IF EXISTS va_arg;
ALTER TABLE e_ldap_stg DROP COLUMN IF EXISTS va_arg;
ALTER TABLE e_dtb_stg DROP COLUMN IF EXISTS va_arg;
ALTER TABLE e_dtb_stg DROP COLUMN IF EXISTS va_cnt;


-- Ajout des noeuds intermediaires
ALTER TABLE e_rst_ses ADD COLUMN IF NOT EXISTS va_fwd_add varchar;

-- Migration du type de l'ordre des stages : bigint vers int
ALTER TABLE e_rst_ses_stg
ALTER COLUMN cd_ord TYPE int;

ALTER TABLE e_rst_rqt_stg
ALTER COLUMN cd_ord TYPE int;

ALTER TABLE e_smtp_stg
ALTER COLUMN cd_ord TYPE int;

ALTER TABLE e_ftp_stg
ALTER COLUMN cd_ord TYPE int;

ALTER TABLE e_ldap_stg
ALTER COLUMN cd_ord TYPE int;

ALTER TABLE e_dtb_stg
ALTER COLUMN cd_ord TYPE int;

-- Migration de la table e_ins_trc : ajout de va_seq et suppression de va_fln
ALTER TABLE e_ins_trc ADD COLUMN IF NOT EXISTS va_seq int;
ALTER TABLE e_ins_trc DROP COLUMN IF EXISTS va_fln;

-- Migration de la table e_rsc_usg : ajout des nouvelles colonnes de monitoring
ALTER TABLE e_rsc_usg ADD COLUMN IF NOT EXISTS nb_act_thr int;
ALTER TABLE e_rsc_usg ADD COLUMN IF NOT EXISTS nb_str_thr int;
ALTER TABLE e_rsc_usg ADD COLUMN IF NOT EXISTS va_cpu_usg SMALLINT;

ALTER TABLE e_rpt_evt DROP COLUMN IF EXISTS va_lvl;
ALTER TABLE e_rpt_evt DROP COLUMN IF EXISTS cd_prn_ses;
ALTER TABLE e_rpt_evt ADD COLUMN IF NOT EXISTS va_err_typ varchar;
ALTER TABLE e_rpt_evt ADD COLUMN IF NOT EXISTS va_thr varchar;
ALTER TABLE e_rpt_evt ADD COLUMN IF NOT EXISTS va_act varchar;
DROP INDEX IF EXISTS idx_log_ent_cd_prn_ses;
DROP INDEX IF EXISTS idx_log_ent_cd_ins;

ALTER TABLE e_rpt_evt RENAME TO e_rpt_evt;

-- Migration de la table e_exc_inf : ajout des champs va_cas et va_trc_typ
ALTER TABLE e_exc_inf ADD COLUMN IF NOT EXISTS va_cas json;
ALTER TABLE e_exc_inf RENAME COLUMN va_typ TO va_trc_typ;
ALTER TABLE e_exc_inf RENAME COLUMN cd_rqt TO cd_trc;
ALTER TABLE e_exc_inf ALTER COLUMN cd_ord TYPE bigint;

-- Migration du namespace historique
-- reprise de la valeur de va_env vers cd_nsp
ALTER TABLE e_env_ins ADD COLUMN IF NOT EXISTS cd_nsp varchar;

UPDATE e_env_ins
SET cd_nsp = UPPER(<prefix> || '-' || va_env)
  WHERE va_env IS NOT NULL;

-- Migration de l'identifiant legacy du serveur distant dans la configuration
UPDATE e_env_ins
SET va_cnf = jsonb_set(
        va_cnf::jsonb,
        '{tracing,remote,@type}',
        '"02"'::jsonb,
        false
    )::json
WHERE va_cnf::jsonb #>> '{tracing,remote,@type}' = 'rest-rmt';

--TODO add UPPER(<prefix> || '-' || va_env) + TOKEN => va_enc_tkn

-- Migration des index environnement
DROP INDEX IF EXISTS idx_env_ins_va_app_va_env;

-- Suppression des objets legacy user action
DROP INDEX IF EXISTS idx_usr_acn_cd_prn_ses;
DROP TABLE IF EXISTS e_usr_acn;

DROP INDEX IF EXISTS idx_exc_inf_cd_rqt;
COMMIT;
