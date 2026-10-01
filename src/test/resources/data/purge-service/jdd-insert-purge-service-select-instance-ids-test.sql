-- H2 test data for selecting expired instance identifiers.
INSERT INTO e_env_ins (id_ins, va_typ, dh_str, dh_end, va_app, va_env, cd_nsp) VALUES
    ('10000000-0000-0000-0000-000000000001', 'SERVER', DATEADD('DAY', -12, CURRENT_DATE), NULL, 'app-a', 'dev', 'JARVIS_DEV'),
    ('10000000-0000-0000-0000-000000000002', 'SERVER', DATEADD('DAY', -1, CURRENT_DATE), NULL, 'app-a', 'dev', 'JARVIS_DEV'),
    ('10000000-0000-0000-0000-000000000003', 'SERVER', DATEADD('DAY', -12, CURRENT_DATE), NULL, 'app-a', 'dev', 'OTHER_NAMESPACE'),
    ('10000000-0000-0000-0000-000000000004', 'SERVER', DATEADD('DAY', -12, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_DATE), 'app-a', 'dev', 'JARVIS_DEV');
