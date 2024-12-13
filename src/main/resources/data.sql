    INSERT INTO e_dtb_rqt (id_dtb_rqt,va_hst,cd_prt,va_nam,dh_str,dh_end,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,va_cmd,cd_prn_ses,va_stt,va_cpt,va_sch) VALUES
	 (1,'HOST',-1,'TEST','2024-07-19 11:07:43.386','2024-07-19 11:07:44.085','TEST','ForkJoinPool-1-worker-6','17.1','Teradata','Teradata Database','SELECT','f136f9a0-3487-4169-bd50-1b692a7a2718',true,NULL,NULL);
INSERT INTO e_dtb_stg (va_nam,dh_str,dh_end,va_cnt,cd_ord,cd_dtb_rqt) VALUES
	 ('CONNECTION','2024-07-18 17:58:15.601','2024-07-18 17:58:15.684',NULL,1,1),
	 ('STATEMENT','2024-07-18 17:58:15.698','2024-07-18 17:58:15.701',NULL,2,1),
	 ('EXECUTE','2024-07-18 17:58:15.715','2024-07-18 17:58:15.723',NULL,3,1),
	 ('FETCH','2024-07-18 17:58:15.728','2024-07-18 17:58:15.768','629',4,1),
	 ('MORE','2024-07-18 17:58:15.768','2024-07-18 17:58:15.768',NULL,5,1),
	 ('DISCONNECTION','2024-07-18 17:58:15.769','2024-07-18 17:58:15.770',NULL,6,1);
INSERT INTO e_env_ins
(id_ins, va_typ, dh_str, va_app, va_vrs, va_adr, va_env, va_os, va_re, va_usr, va_clr)
VALUES('b8610804-3b52-4913-beb0-a81792914512', 'CLIENT', '2024-11-27 10:35:21.550', 'ihm', '__GIT_BUILD_VERSION__', '0:0:0:0:0:0:0:1', 'dev', 'Windows 10.0', 'Edge', NULL, 'inspect-ng-collector-0.0.1');
INSERT INTO e_exc_inf
(va_typ, va_err_typ, va_err_msg, cd_ord, cd_rqt)
VALUES('JDBC', 'java.sql.SQLTransientConnectionException', 'HikariPool-1 - Connection is not available, request timed out after 180000ms.', 1, 1);
INSERT INTO e_ftp_rqt
(id_ftp_rqt, va_hst, cd_prt, va_pcl, va_srv_vrs, va_clt_vrs, va_usr, dh_str, dh_end, va_thr, cd_prn_ses, va_stt)
VALUES(1, 'host', 2222, 'ftps', 'SSH-2.0tp', 'SSH-2.0-JSCH-0.1.54', 'cosHRDHy', '2024-07-18 23:30:00.875', '2024-07-18 23:30:00.888', 'pool-6-thread-1', 'f136f9a0-3487-4169-bd50-1b692a7a2718', true);
INSERT INTO e_ftp_stg
(va_nam, dh_str, dh_end, va_arg, cd_ord, cd_ftp_rqt)
VALUES('CONNECTION', '2024-07-26 12:30:00.899', '2024-07-26 12:30:00.911', NULL, 1, 1);
INSERT INTO e_lcl_rqt
(id_lcl_rqt, va_nam, va_lct, dh_str, dh_end, va_usr, va_thr, cd_prn_ses, va_stt)
VALUES(1, 'cacheCalendar', 'CalendarDaoImpl', '2024-07-18 17:49:55.168', '2024-07-18 17:49:59.068', NULL, 'main', '120032b3-a1cb-411f-b61c-32efd82b0540', true);
INSERT INTO e_ldap_rqt
(id_ldap_rqt, va_hst, cd_prt, va_pcl, va_usr, dh_str, dh_end, va_thr, cd_prn_ses, va_stt)
VALUES(1, 'host', 636, NULL, 'u', '2024-07-19 09:44:05.948', '2024-07-19 09:44:06.089', 'http-nio-9000-exec-10', 'f136f9a0-3487-4169-bd50-1b692a7a2718', true);
INSERT INTO e_ldap_stg
(va_nam, dh_str, dh_end, va_arg, cd_ord, cd_ldap_rqt)
VALUES('CONNECTION', '2024-07-19 09:44:05.948', '2024-07-19 09:44:06.076', NULL, 1, 1);
INSERT INTO e_main_ses
(id_ses, va_typ, va_nam, va_usr, dh_str, dh_end, va_lct, va_thr, va_err_typ, va_err_msg, va_msk, cd_ins)
VALUES('120032b3-a1cb-411f-b61c-32efd82b0540', 'VIEW', 'f', 'f', '2024-07-30 15:53:00.148', '2024-07-30 15:53:02.807', 'https://host.fr/#/home', NULL, NULL, NULL, 4, 'b8610804-3b52-4913-beb0-a81792914512');
INSERT INTO e_rst_rqt
(id_rst_rqt, va_mth, va_pcl, va_hst, cd_prt, va_pth, va_qry, va_cnt_typ, va_ath_sch, cd_stt, va_i_sze, va_o_sze, va_i_cnt_enc, va_o_cnt_enc, dh_str, dh_end, va_thr, cd_prn_ses, cd_rmt_ses)
VALUES(10622, 'POST', 'https', 'host.fr', 0, '/user/action', '', 'json', NULL, 200, 0, 0, NULL, NULL, '2024-07-23 10:59:56.263', '2024-07-23 10:59:56.397', NULL, 'f136f9a0-3487-4169-bd50-1b692a7a2718', '3d3b241e-0f6d-434c-ba2a-4abc84244e83');
INSERT INTO e_rst_ses
(id_ses, va_mth, va_pcl, va_hst, cd_prt, va_pth, va_qry, va_cnt_typ, va_ath_sch, cd_stt, va_i_sze, va_o_sze, va_i_cnt_enc, va_o_cnt_enc, dh_str, dh_end, va_thr, va_err_typ, va_err_msg, va_nam, va_usr, va_usr_agt, va_cch_ctr, va_msk, cd_ins)
VALUES('f136f9a0-3487-4169-bd50-1b692a7a2718', 'GET', 'http', 'host.svc', 9000, '/RSYERYsdgsdSRY', NULL, 'application/json', 'Bearer', 200, 0, 25, NULL, NULL, '2024-07-19 11:07:43.386', '2024-07-18 17:52:41.612', 'http-nio-9000-exec-2', NULL, NULL, 'authoSERYe_roles', 'fe', 'Apache-HttpClient/5.1.4 (Java/17.0.4.1)', 'no-cache, no-store, max-age=0, must-revalidate', 0, 'b8610804-3b52-4913-beb0-a81792914512');
INSERT INTO e_smtp_mail
(va_sbj, va_cnt_typ, va_frm, va_rcp, va_rpl, va_sze, cd_smtp_rqt)
VALUES('Une erreur est survenue lors de la communication avzf', 'text/html;charset=utf-8', 'zffz', 'Dzdzf', 'zfzfn', -1, 1);
INSERT INTO e_smtp_rqt
(id_smtp_rqt, va_hst, cd_prt, va_usr, dh_str, dh_end, va_thr, cd_prn_ses, va_stt)
VALUES(1, 'mai:ugougvc', 9001, 'webadm', '2024-07-19 08:00:01.633', '2024-07-19 08:00:01.748', 'http-nio-9000-exec-5', 'f136f9a0-3487-4169-bd50-1b692a7a2718', true);
INSERT INTO e_smtp_stg
(va_nam, dh_str, dh_end, cd_ord, cd_smtp_rqt)
VALUES('CONNECTION', '2024-07-19 04:50:01.547', '2024-07-19 04:50:01.732', 1, 1);
--delete from e_dtb_rqt;
--delete from e_dtb_stg;
--delete from e_env_ins;
--delete from e_exc_inf;
--delete from e_ftp_rqt;
--delete from e_ftp_stg;
--delete from e_lcl_rqt;
--delete from e_ldap_rqt;
--delete from e_ldap_stg;
--delete from e_smtp_rqt;
--delete from e_smtp_stg;
--delete from e_smtp_mail;
--delete from e_main_ses;
--delete from e_rst_ses;
--delete from e_rst_rqt;
