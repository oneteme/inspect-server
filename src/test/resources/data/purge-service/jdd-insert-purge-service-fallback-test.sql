-- H2 test data for the instance start-date fallback scenario.
INSERT INTO e_env_ins (id_ins, va_typ, dh_str, dh_end, va_app, va_env, va_cnf, cd_nsp) VALUES
    ('22222222-2222-2222-2222-222222222222', 'SERVER', DATEADD('DAY', -11, CURRENT_DATE), DATEADD('DAY', -11, CURRENT_DATE), 'app-a', 'dev', '{"tracing":{"remote":{"retentionMaxAge":{"audit":"PT240H","diagnostic":"PT168H"}}}}', 'JARVIS_DEV'),
    ('77777777-7777-7777-7777-777777777777', 'SERVER', DATEADD('DAY', -11, CURRENT_DATE), NULL, 'app-a', 'dev', '{"tracing":{"remote":{"retentionMaxAge":{"audit":"PT240H","diagnostic":"PT168H"}}}}', 'JARVIS_DEV'),
    ('99999999-9999-9999-9999-999999999999', 'SERVER', DATEADD('DAY', -1, CURRENT_DATE), NULL, 'app-a', 'dev', '{"tracing":{"remote":{"retentionMaxAge":{"audit":"PT240H","diagnostic":"PT168H"}}}}', 'JARVIS_DEV'),
    ('11111111-1111-1111-1111-111111111111', 'SERVER', DATEADD('DAY', -1, CURRENT_DATE), NULL, 'app-a', 'dev', '{"tracing":{"remote":{"retentionMaxAge":{"audit":"PT240H","diagnostic":"PT168H"}}}}', 'JARVIS_REC');

INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES
    (0, 0, 0, 1, DATEADD('DAY', -12, CURRENT_DATE), '22222222-2222-2222-2222-222222222222'),
    (0, 0, 0, 1, DATEADD('DAY', -15, CURRENT_DATE), '11111111-1111-1111-1111-111111111111'),
    (0, 0, 0, 1, DATEADD('DAY', -1, CURRENT_DATE), '11111111-1111-1111-1111-111111111111');

INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES
    (DATEADD('DAY', -12, CURRENT_DATE), 'INFO', 'closed', '22222222-2222-2222-2222-222222222222'),
    (DATEADD('DAY', -12, CURRENT_DATE), 'INFO', 'no-trace', '77777777-7777-7777-7777-777777777777'),
    (DATEADD('DAY', -1, CURRENT_DATE), 'INFO', 'recent-no-trace', '99999999-9999-9999-9999-999999999999'),
    (DATEADD('DAY', -1, CURRENT_DATE), 'INFO', 'active', '11111111-1111-1111-1111-111111111111');

INSERT INTO e_rst_rqt (id_rst_rqt, dh_str, dh_end, cd_ins) VALUES
    ('55555555-5555-5555-5555-555555555555', DATEADD('DAY', -11, CURRENT_DATE), DATEADD('DAY', -11, CURRENT_DATE), '22222222-2222-2222-2222-222222222222'),
    ('88888888-8888-8888-8888-888888888888', DATEADD('DAY', -11, CURRENT_DATE), DATEADD('DAY', -11, CURRENT_DATE), '77777777-7777-7777-7777-777777777777'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', DATEADD('DAY', -1, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_DATE), '99999999-9999-9999-9999-999999999999'),
    ('44444444-4444-4444-4444-444444444444', DATEADD('DAY', -1, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_DATE), '11111111-1111-1111-1111-111111111111');

INSERT INTO e_rst_rqt_stg (va_nam, dh_str, dh_end, cd_ord, cd_rst_rqt) VALUES
    ('closed-stage', DATEADD('DAY', -12, CURRENT_DATE), DATEADD('DAY', -12, CURRENT_DATE), 1, '55555555-5555-5555-5555-555555555555'),
    ('no-trace-stage', DATEADD('DAY', -12, CURRENT_DATE), DATEADD('DAY', -12, CURRENT_DATE), 1, '88888888-8888-8888-8888-888888888888'),
    ('recent-no-trace-stage', DATEADD('DAY', -1, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_DATE), 1, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    ('active-stage', DATEADD('DAY', -1, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_DATE), 1, '44444444-4444-4444-4444-444444444444');
