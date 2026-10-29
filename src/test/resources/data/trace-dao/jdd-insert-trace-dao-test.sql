INSERT INTO e_rst_ses
    (id_ses, cd_ins, dh_str, va_thr, va_nam, va_usr, va_mth, va_pcl, va_hst, cd_prt,
     va_pth, va_qry, va_ath_sch, va_i_sze, va_i_cnt_enc, va_usr_agt, va_lnk, va_fwd_add, va_msk, va_cch_ctr)
VALUES
    ('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00', 'http-worker', 'original', 'alice', 'POST', 'https',
     'api.example.test', 443, '/orders', 'page=1', 'Bearer', 128, 'gzip', 'Postman/7.43.0',
     TRUE, '192.0.2.1, 192.0.2.2', 0, 'max-age=60'),
    ('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002',
     TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00', 'other-worker', 'other', 'charlie', 'GET', 'http',
     'other.example.test', 80, '/other', NULL, NULL, 0, NULL, NULL, FALSE, NULL, 0, NULL);

INSERT INTO e_main_ses
    (id_ses, cd_ins, dh_str, va_thr, va_typ, va_lct, va_nam, va_usr)
VALUES
    ('10000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001',
     TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00', 'main-worker', 'SCHEDULE', 'Original.run', 'original', 'alice'),
    ('10000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002',
     TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00', 'other-worker', 'STARTUP', 'Other.run', 'other', 'charlie');

INSERT INTO e_rst_rqt
    (id_rst_rqt, cd_prn_ses, cd_ins, dh_str, va_thr, va_usr, va_mth, va_pcl, va_hst,
     cd_prt, va_pth, va_qry, va_ath_sch, va_o_sze, va_o_cnt_enc)
VALUES
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00',
     'rest-worker', 'alice', 'GET', 'https', 'remote.example.test', 8443, '/products', 'id=42', 'Basic', 64, 'gzip'),
    ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000002', TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00',
     'other-worker', 'charlie', 'GET', 'http', 'other.example.test', 80, '/other', NULL, NULL, 0, NULL);

INSERT INTO e_lcl_rqt
    (id_lcl_rqt, cd_prn_ses, cd_ins, dh_str, va_thr, va_usr, va_typ, va_nam, va_lct)
VALUES
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00',
     'local-worker', 'alice', 'FILE', 'readFile', 'Storage.read'),
    ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000002', TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00',
     'other-worker', 'charlie', 'CACHE', 'other', 'Other.read');

INSERT INTO e_smtp_rqt
    (id_smtp_rqt, cd_prn_ses, cd_ins, dh_str, va_thr, va_usr, va_hst, cd_prt, va_pcl)
VALUES
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00',
     'mail-worker', 'alice', 'smtp.example.test', 587, 'smtp'),
    ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000002', TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00',
     'other-worker', 'charlie', 'other.example.test', 25, 'smtp');

INSERT INTO e_ftp_rqt
    (id_ftp_rqt, cd_prn_ses, cd_ins, dh_str, va_thr, va_usr, va_hst, cd_prt, va_pcl, va_srv_vrs, va_clt_vrs)
VALUES
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00',
     'ftp-worker', 'alice', 'ftp.example.test', 21, 'ftp', 'server-2.0', 'client-1.0'),
    ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000002', TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00',
     'other-worker', 'charlie', 'other.example.test', 22, 'sftp', NULL, NULL);

INSERT INTO e_ldap_rqt
    (id_ldap_rqt, cd_prn_ses, cd_ins, dh_str, va_thr, va_usr, va_hst, cd_prt, va_pcl)
VALUES
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00',
     'ldap-worker', 'alice', 'ldap.example.test', 389, 'ldap'),
    ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000002', TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00',
     'other-worker', 'charlie', 'other.example.test', 636, 'ldaps');

INSERT INTO e_dtb_rqt
    (id_dtb_rqt, cd_prn_ses, cd_ins, dh_str, va_thr, va_usr, va_hst, cd_prt,
     va_she, va_nam, va_sha, va_drv, va_prd_nam, va_prd_vrs)
VALUES
    ('10000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
     '20000000-0000-0000-0000-000000000001', TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00',
     'jdbc-worker', 'alice', 'db.example.test', 5432, 'postgresql', 'inspect', 'public', '42.7', 'PostgreSQL', '16'),
    ('10000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000002',
     '20000000-0000-0000-0000-000000000002', TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00',
     'other-worker', 'charlie', 'other.example.test', 5432, 'other', 'other', 'other', NULL, NULL, NULL);

INSERT INTO e_env_ins (id_ins, dh_str, va_app)
VALUES
    ('10000000-0000-0000-0000-000000000001', TIMESTAMP WITH TIME ZONE '2026-01-15 10:00:00+00', 'original'),
    ('10000000-0000-0000-0000-000000000002', TIMESTAMP WITH TIME ZONE '2026-01-15 09:00:00+00', 'other');
