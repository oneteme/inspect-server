-- H2 test data for purging abandoned instances.
INSERT INTO e_env_ins (id_ins, va_typ, dh_str, va_app, va_env, cd_nsp) VALUES
    ('10000000-0000-0000-0000-000000000001', 'SERVER', DATEADD('DAY', -15, CURRENT_DATE), 'app-a', 'dev', 'JARVIS_DEV'),
    ('10000000-0000-0000-0000-000000000002', 'SERVER', DATEADD('DAY', -15, CURRENT_DATE), 'app-a', 'dev', 'JARVIS_DEV'),
    ('10000000-0000-0000-0000-000000000003', 'SERVER', DATEADD('DAY', -12, CURRENT_DATE), 'app-a', 'dev', 'JARVIS_DEV');

INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES
    (0, 0, 0, 1, DATEADD('DAY', -12, CURRENT_DATE), '10000000-0000-0000-0000-000000000001'),
    (0, 0, 0, 1, DATEADD('DAY', -12, CURRENT_DATE), '10000000-0000-0000-0000-000000000002'),
    (0, 0, 0, 1, DATEADD('DAY', -1, CURRENT_DATE), '10000000-0000-0000-0000-000000000002');
