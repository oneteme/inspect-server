-- H2 test data for purging closed instances.
INSERT INTO e_env_ins (id_ins, va_typ, dh_str, dh_end, va_app, va_env, cd_nsp) VALUES
    ('10000000-0000-0000-0000-000000000001', 'SERVER', DATEADD('DAY', -15, CURRENT_DATE), DATEADD('DAY', -10, CURRENT_DATE), 'app-a', 'dev', 'dev'),
    ('10000000-0000-0000-0000-000000000002', 'SERVER', DATEADD('DAY', -2, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_DATE), 'app-a', 'dev', 'dev');
