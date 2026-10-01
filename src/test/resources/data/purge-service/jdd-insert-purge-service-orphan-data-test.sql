-- H2 test data for purging orphan traces.
INSERT INTO e_ins_trc (va_pnd, va_atp, va_seq, va_trc_cnt, dh_str, cd_ins) VALUES
    (0, 0, 0, 1, DATEADD('DAY', -2, CURRENT_DATE), '30000000-0000-0000-0000-000000000001');

INSERT INTO e_log_ent (dh_str, va_lvl, va_msg, cd_ins) VALUES
    (DATEADD('DAY', -2, CURRENT_DATE), 'INFO', 'orphan', '30000000-0000-0000-0000-000000000001');
