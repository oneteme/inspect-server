package org.usf.inspect.server.dao;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.*;
import org.usf.inspect.server.event.UnsavedEventTraceEvent;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.Pair;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static java.sql.Types.*;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.usf.inspect.core.RequestMask.*;
import static org.usf.inspect.server.JsonUtils.*;
import static org.usf.inspect.server.Utils.*;


/**
 * Using Types.OTHER with JSON serialization to ensure portability:
 * - In PostgreSQL: Types.OTHER is interpreted as native JSONB type
 * - In H2: Types.OTHER is treated as VARCHAR without JSON parsing attempts
 * Prior serialization with writeValueAsString prevents conversion errors
 * between the two DBMSs, making the code compatible with both environments.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TraceDao {
	
    private final JdbcTemplate template;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher publisher;

    private static final int BATCH_SIZE = 1_000;

    public void saveInstanceEnvironment(InstanceEnvironment instance) {
        template.update("""
insert into e_env_ins(id_ins,va_typ,dh_str,va_app,va_vrs,va_adr,va_env,va_os,va_re,va_usr,va_clr,va_brch,va_hsh,va_cnf,va_rsr,va_add_prp)
values(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", ps -> {
            ps.setString(1, instance.getId());
            ps.setString(2, ofNullable(instance.getType()).map(InstanceType::name).orElse(null));
            ps.setTimestamp(3, fromNullableInstant(instance.getInstant()));
            ps.setString(4, instance.getName());
            ps.setString(5, instance.getVersion());
            ps.setString(6, instance.getAddress());
            ps.setString(7, instance.getEnv());
            ps.setString(8, instance.getOs());
            ps.setString(9, instance.getRe());
            ps.setString(10, instance.getUser());
            ps.setString(11, instance.getCollector());
            ps.setString(12, instance.getBranch());
            ps.setString(13, instance.getHash());
            ps.setObject(14, safeWriteValue(instance.getConfiguration(), mapper), OTHER);
            ps.setObject(15, safeWriteValue(instance.getResource(), mapper), OTHER);
            ps.setObject(16, safeWriteValue(instance.getAdditionalProperties(), mapper), OTHER);
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateInstanceEnvironments(List<InstanceEnvironmentUpdate> instances){
        executeBatch("update e_env_ins set dh_end = ? where id_ins = ?::uuid", instances, (ps, ins) -> {
            ps.setTimestamp(1, fromNullableInstant(ins.getEnd()));
            ps.setString(2, ins.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveInstanceTraces(List<InstanceTrace> instanceTraces) {
        executeBatch("insert into e_ins_trc (va_pnd, va_atp, va_trc_cnt, dh_str, va_fln, cd_ins) values (?, ?, ?, ?, ?, ?::uuid)", 
        		instanceTraces, (ps, trc) -> {
		            ps.setObject(1, trc.getPending(), INTEGER);
		            ps.setObject(2, trc.getAttempts(), INTEGER);
		            ps.setInt(3, trc.getTraceCount());
		            ps.setTimestamp(4, fromNullableInstant(trc.getInstant()));
		            ps.setString(5, trc.getFileName());
		            ps.setString(6, trc.getInstanceId());
		        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLogEntries(List<LogEntry> logEntries) {
        executeBatch("insert into e_log_ent(va_lvl,va_msg,va_stk,dh_str,cd_prn_ses,cd_ins) values (?,?,?,?,?::uuid,?::uuid)", 
        		logEntries, (ps, o)-> {
		            ps.setString(1, String.valueOf(o.getLevel()));
		            ps.setString(2, o.getMessage());
		            ps.setObject(3, safeWriteValue(o.getStackRows(), mapper), OTHER);
		            ps.setTimestamp(4, fromNullableInstant(o.getInstant()));
		            ps.setString(5, o.getSessionId());
		            ps.setString(6, o.getInstanceId());
		        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMachineResourceUsages(List<MachineResourceUsage> usages) {
        executeBatch("insert into e_rsc_usg(dh_str,va_usd_hep,va_cmt_hep,va_usd_dsk,cd_ins) values (?,?,?,?,?::uuid)", usages, (ps, o)-> {
            ps.setTimestamp(1, fromNullableInstant(o.getInstant()));
            ps.setInt(2, o.getUsedHeap());
            ps.setInt(3, o.getCommitedHeap());
            ps.setInt(4, o.getUsedDiskSpace());
            ps.setString(5, o.getInstanceId());
        });
    }

    // New version
    @Transactional(rollbackFor = Throwable.class)
    public void savePartialRestSessions(List<HttpSessionSignal> sessions) {
        executeBatch("""
insert into e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_lnk,dh_str,va_nam,va_usr,va_usr_agt,va_msk)
values(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", sessions, (ps, ses) -> {
            restSessionSetter(ps, ses);
            ps.setString(15, ses.getName());
            ps.setString(16, ses.getUser());
            ps.setString(17, userAgentExtract(ses.getUserAgent()));
            ps.setInt(18, 0);
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteRestSessions(List<Pair<HttpSessionSignal, HttpSessionUpdate>> sessions) {
    	executeBatchPair("""
insert into e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_i_sze,va_i_cnt_enc,va_thr,va_lnk,dh_str,dh_end,va_err_typ,va_err_msg,va_stk,va_nam,va_usr,va_usr_agt,va_cch_ctr,va_cnt_typ,cd_stt,va_o_sze,va_o_cnt_enc,va_msk)
values(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", sessions, (ps, ses) -> {
            var session = ses.getV1();
            var callback = ses.getV2();
            var exp = callback.getException();
            restSessionSetter(ps, session);
            ps.setTimestamp(15, fromNullableInstant(callback.getEnd()));
            ps.setString(16, nonNull(exp) ? exp.getType() : null);
            ps.setString(17, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(18, nonNull(exp) ? safeWriteValue(exp.getStackTraceRows(), mapper) : null, OTHER);
            ps.setString(19, nonNull(callback.getName()) ? callback.getName() : session.getName());
            ps.setString(20, nonNull(callback.getUser()) ? callback.getUser() : session.getUser());
            ps.setString(21, userAgentExtract(session.getUserAgent()));
            ps.setString(22, callback.getCacheControl());
            ps.setString(23, contentTypeExtract(callback.getContentType()));
            ps.setInt(24, callback.getStatus());
            ps.setLong(25, callback.getDataSize());
            ps.setString(26, callback.getContentEncoding());
            ps.setInt(27, callback.getRequestMask().get());
        });
    }

    static void restSessionSetter(PreparedStatement ps, HttpSessionSignal ses) throws SQLException {
        ps.setString(1, ses.getId());
        ps.setString(2, ses.getInstanceId());
        ps.setString(3, ses.getMethod());
        ps.setString(4, ses.getProtocol());
        ps.setString(5, ses.getHost());
        ps.setInt(6, ses.getPort());
        ps.setString(7, ses.getPath());
        ps.setString(8, ses.getQuery());
        ps.setString(9, ses.getAuthScheme());
        ps.setLong(10, ses.getDataSize());
        ps.setString(11, ses.getContentEncoding());
        ps.setString(12, ses.getThreadName());
        ps.setBoolean(13, ses.isLinked());
        ps.setTimestamp(14, fromNullableInstant(ses.getStart()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateRestSessions(List<HttpSessionUpdate> sessions) {
        executeBatch("""
update e_rst_ses set va_err_typ = coalesce(?, va_err_typ), va_err_msg = coalesce(?, va_err_msg), va_stk = coalesce(?, va_stk), va_nam = coalesce(?, va_nam), va_usr = coalesce(?, va_usr), va_cch_ctr = coalesce(?, va_usr_agt), va_cnt_typ = ?, cd_stt = ?, va_o_sze = ?, va_o_cnt_enc = ?, dh_end = ?, va_msk = ?
where id_ses = ?::uuid""", sessions, (ps, ses) -> {
            var exp = ses.getException();
            ps.setString(1, nonNull(exp) ? exp.getType() : null);
            ps.setString(2, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(3, nonNull(exp) ? safeWriteValue(exp.getStackTraceRows(), mapper) : null, OTHER);
            ps.setString(4, ses.getName());
            ps.setString(5, ses.getUser());
            ps.setString(6, ses.getCacheControl());
            ps.setString(7, contentTypeExtract(ses.getContentType()));
            ps.setInt(8, ses.getStatus());
            ps.setLong(9, ses.getDataSize());
            ps.setString(10, ses.getContentEncoding());
            ps.setTimestamp(11, fromNullableInstant(ses.getEnd()));
            ps.setInt(12, ses.getRequestMask().get());
            ps.setString(13, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMaskRestSessions(List<SessionMaskUpdate> sessions) {
        executeBatch("update e_rst_ses set va_msk = ? where id_ses = ?::uuid", sessions, (ps, ses) -> {
            ps.setInt(1, ses.getMask());
            ps.setString(2, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialMainSessions(List<MainSessionSignal> sessions) {
        executeBatch("""
insert into e_main_ses(id_ses,cd_ins,va_typ,va_thr,va_lct,va_nam,va_usr,dh_str,va_msk)
values(?::uuid,?::uuid,?,?,?,?,?,?,?)""", sessions, (ps, ses) -> {
            mainSessionSetter(ps, ses);
            ps.setString(5, ses.getLocation());
            ps.setString(6, ses.getName());
            ps.setString(7, ses.getUser());
            ps.setTimestamp(8, fromNullableInstant(ses.getStart()));
            ps.setInt(9, 0);
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteMainSessions(List<Pair<MainSessionSignal, MainSessionUpdate>> sessions) {
    	executeBatchPair("""
insert into e_main_ses(id_ses,cd_ins,va_typ,va_thr,va_lct,va_nam,va_usr,dh_str,dh_end,va_err_typ,va_err_msg,va_stk,va_msk)
values(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?)""", sessions, (ps, ses) -> {
            var session = ses.getV1();
            var callback = ses.getV2();
            var exp = callback.getException();
            mainSessionSetter(ps, session);
            ps.setString(5, nonNull(callback.getLocation()) ? callback.getLocation() : session.getLocation());
            ps.setString(6, nonNull(callback.getName()) ? callback.getName() : session.getName());
            ps.setString(7, nonNull(callback.getUser()) ? callback.getUser() : session.getUser());
            ps.setTimestamp(8, fromNullableInstant(nonNull(callback.getStart()) ? callback.getStart() : session.getStart()));
            ps.setTimestamp(9, fromNullableInstant(callback.getEnd()));
            ps.setString(10, nonNull(exp) ? exp.getType() : null);
            ps.setString(11, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(12, nonNull(exp) ? safeWriteValue(exp.getStackTraceRows(), mapper) : null, OTHER);
            ps.setInt(13, callback.getRequestMask().get());
        });
    }

    static void mainSessionSetter(PreparedStatement ps, MainSessionSignal ses) throws SQLException {
        ps.setString(1, ses.getId());
        ps.setString(2, ses.getInstanceId());
        ps.setString(3, valueOfNullable(ses.getType()));
        ps.setString(4, ses.getThreadName());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMainSessions(List<MainSessionUpdate> sessions) {
        executeBatch("""
update e_main_ses set va_lct = coalesce(?, va_lct), va_nam = coalesce(?, va_nam), va_usr = coalesce(?, va_usr), dh_str = coalesce(?, dh_str), dh_end = ?, va_err_typ = ?, va_err_msg = ?, va_stk = ?, va_msk = ?
where id_ses = ?::uuid""", sessions, (ps, ses) -> {
            var exp = ses.getException();
            ps.setString(1, ses.getLocation());
            ps.setString(2, ses.getName());
            ps.setString(3, ses.getUser());
            ps.setTimestamp(4, fromNullableInstant(ses.getStart()));
            ps.setTimestamp(5, fromNullableInstant(ses.getEnd()));
            ps.setString(6, nonNull(exp) ? exp.getType() : null);
            ps.setString(7, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(8, nonNull(exp) ? safeWriteValue(exp.getStackTraceRows(), mapper) : null, OTHER);
            ps.setInt(9, ses.getRequestMask().get());
            ps.setString(10, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMaskMainSessions(List<SessionMaskUpdate> sessions) {
        executeBatch("update e_main_ses set va_msk = ? where id_ses = ?::uuid", sessions, (ps, ses) -> {
            ps.setInt(1, ses.getMask());
            ps.setString(2, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialRestRequests(List<HttpRequestSignal> requests) {
        executeBatch("""
insert into e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_usr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, TraceDao::restRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteRestRequests(List<Pair<HttpRequestSignal, HttpRequestUpdate>> requests) {
    	executeBatchPair("""
insert into e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_usr,dh_str,dh_end,va_cnt_typ,cd_stt,va_i_sze,va_i_cnt_enc,va_bdy_cnt,va_lnk)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, ses) -> {
            var request = ses.getV1();
            var callback = ses.getV2();
            restRequestSetter(ps, request);
            ps.setTimestamp(16, fromNullableInstant(callback.getEnd()));
            ps.setString(17, contentTypeExtract(callback.getContentType()));
            ps.setInt(18, callback.getStatus());
            ps.setLong(19, callback.getDataSize());
            ps.setString(20, callback.getContentEncoding());
            ps.setString(21, callback.getBodyContent());
            ps.setBoolean(22, callback.isLinked());
        });
    }

    static void restRequestSetter(PreparedStatement ps, HttpRequestSignal req) throws SQLException {
        ps.setString(1, req.getId());
        ps.setString(2, req.getSessionId());
        ps.setString(3, req.getInstanceId());
        ps.setString(4, req.getMethod());
        ps.setString(5, req.getProtocol());
        ps.setString(6, req.getHost());
        ps.setInt(7, req.getPort());
        ps.setString(8, req.getPath());
        ps.setString(9, req.getQuery());
        ps.setString(10, req.getAuthScheme());
        ps.setLong(11, req.getDataSize());
        ps.setString(12, req.getContentEncoding());
        ps.setString(13, req.getThreadName());
        ps.setString(14, req.getUser());
        ps.setTimestamp(15, fromNullableInstant(req.getStart()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateRestRequests(List<HttpRequestUpdate> requests) {
        executeBatch("""
update e_rst_rqt set va_cnt_typ = ?, cd_stt = ?, va_i_sze = ?, va_i_cnt_enc = ?, dh_end = ?, va_bdy_cnt = ?, va_lnk = ?
where id_rst_rqt = ?::uuid""", requests, (ps, req) -> {
            ps.setString(1, contentTypeExtract(req.getContentType()));
            ps.setInt(2, req.getStatus());
            ps.setLong(3, req.getDataSize());
            ps.setString(4, req.getContentEncoding());
            ps.setTimestamp(5, fromNullableInstant(req.getEnd()));
            ps.setString(6, req.getBodyContent());
            ps.setBoolean(7, req.isLinked());
            ps.setString(8, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialLocalRequests(List<LocalRequestSignal> requests) {
        executeBatch("""
insert into e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?)""", requests, (ps, req) -> {
            localRequestSetter(ps, req);
            ps.setTimestamp(9, fromNullableInstant(req.getStart()));
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteLocalRequests(List<Pair<LocalRequestSignal, LocalRequestUpdate>> requests) {
    	executeBatchPair("""
insert into e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str,dh_end,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            localRequestSetter(ps, req);
            ps.setTimestamp(9, fromNullableInstant(nonNull(callback.getStart()) ? callback.getStart() : req.getStart()));
            ps.setTimestamp(10, fromNullableInstant(callback.getEnd()));
            ps.setBoolean(11, nonNull(callback.getException()));
        });
        var exceptions = requests.stream()
                .map(Pair::getV2)
                .filter(r -> nonNull(r.getException()))
                .toList();
        if(!exceptions.isEmpty()) {
            saveLocalRequestExceptions(exceptions);
        }
    }

    static void localRequestSetter(PreparedStatement ps, LocalRequestSignal req) throws SQLException {
        ps.setString(1, req.getId());
        ps.setString(2, req.getSessionId());
        ps.setString(3, req.getInstanceId()); //instance id
        ps.setString(4, req.getType());
        ps.setString(5, req.getName());
        ps.setString(6, req.getLocation());
        ps.setString(7, req.getUser());
        ps.setString(8, req.getThreadName());
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateLocalRequests(List<LocalRequestUpdate> requests) {
        executeBatch("""
update e_lcl_rqt set dh_str = coalesce(?, dh_str), dh_end = ?, va_fail = ?
where id_lcl_rqt = ?::uuid""", requests, (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getStart()));
            ps.setTimestamp(2, fromNullableInstant(req.getEnd()));
            ps.setBoolean(3, nonNull(req.getException()));
            ps.setString(4, req.getId());
        });
        var exceptions = requests.stream()
                .filter(r -> nonNull(r.getException()))
                .toList();
        if(!exceptions.isEmpty()) {
            saveLocalRequestExceptions(exceptions);
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialMailRequests(List<MailRequestSignal> requests) {
        executeBatch("""
insert into e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?)""", requests, TraceDao::mailRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteMailRequests(List<Pair<MailRequestSignal, MailRequestUpdate>> requests) {
    	executeBatchPair("""
insert into e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            mailRequestSetter(ps, req);
            ps.setTimestamp(10, fromNullableInstant(callback.getEnd()));
            ps.setString(11, callback.getCommand());
            ps.setBoolean(12, callback.isFailed());
        });
    }

    static void mailRequestSetter(PreparedStatement ps, MailRequestSignal req) throws SQLException {
        ps.setString(1, req.getId());
        ps.setString(2, req.getSessionId());
        ps.setString(3, req.getInstanceId()); //instance id
        ps.setString(4, req.getHost());
        ps.setInt(5, req.getPort());
        ps.setString(6, req.getProtocol());
        ps.setString(7, req.getUser());
        ps.setString(8, req.getThreadName());
        ps.setTimestamp(9, fromNullableInstant(req.getStart()));

    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMailRequests(List<MailRequestUpdate> requests) {
        executeBatch("""
update e_smtp_rqt set dh_end = ?, va_cmd = ?, va_fail = ?
where id_smtp_rqt = ?::uuid""", requests, (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getEnd()));
            ps.setString(2, req.getCommand());
            ps.setBoolean(3, req.isFailed());
            ps.setString(4, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialFtpRequests(List<FtpRequestSignal> requests) {
        executeBatch("""
insert into e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?)""", requests, TraceDao::ftpRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteFtpRequests(List<Pair<FtpRequestSignal, FtpRequestUpdate>> requests) {
    	executeBatchPair("""
insert into e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str,dh_end,va_cmd,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            ftpRequestSetter(ps, req);
            ps.setTimestamp(12, fromNullableInstant(callback.getEnd()));
            ps.setString(13, callback.getCommand());
            ps.setBoolean(14, callback.isFailed());
        });
    }

    static void ftpRequestSetter(PreparedStatement ps, FtpRequestSignal req) throws SQLException {
        ps.setString(1, req.getId());
        ps.setString(2, req.getSessionId());
        ps.setString(3, req.getInstanceId());
        ps.setString(4, req.getHost());
        ps.setInt(5, req.getPort());
        ps.setString(6, req.getProtocol());
        ps.setString(7, req.getServerVersion());
        ps.setString(8, req.getClientVersion());
        ps.setString(9, req.getUser());
        ps.setString(10, req.getThreadName());
        ps.setTimestamp(11, fromNullableInstant(req.getStart()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateFtpRequests(List<FtpRequestUpdate> requests) {
        executeBatch("""
update e_ftp_rqt set dh_end = ?, va_cmd = ?, va_fail = ?
where id_ftp_rqt = ?::uuid""", requests, (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getEnd()));
            ps.setString(2, req.getCommand());
            ps.setBoolean(3, req.isFailed());
            ps.setString(4, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialLdapRequests(List<DirectoryRequestSignal> requests) {
        executeBatch("""
insert into e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?)""", requests, TraceDao::ldapRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteLdapRequests(List<Pair<DirectoryRequestSignal, DirectoryRequestUpdate>> requests) {
    	executeBatchPair("""
insert into e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            ldapRequestSetter(ps, req);
            ps.setTimestamp(10, fromNullableInstant(callback.getEnd()));
            ps.setString(11, callback.getCommand());
            ps.setBoolean(12, callback.isFailed());
        });
    }

    static void ldapRequestSetter(PreparedStatement ps, DirectoryRequestSignal req) throws SQLException {
        ps.setString(1, req.getId());
        ps.setString(2, req.getSessionId());
        ps.setString(3, req.getInstanceId());
        ps.setString(4, req.getHost());
        ps.setInt(5, req.getPort());
        ps.setString(6, req.getProtocol());
        ps.setString(7, req.getUser());
        ps.setString(8, req.getThreadName());
        ps.setTimestamp(9, fromNullableInstant(req.getStart()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateLdapRequests(List<DirectoryRequestUpdate> requests) {
        executeBatch("""
update e_ldap_rqt set dh_end = ?, va_cmd = ?, va_fail = ?
where id_ldap_rqt = ?::uuid""", requests, (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getEnd()));
            ps.setString(2, req.getCommand());
            ps.setBoolean(3, req.isFailed());
            ps.setString(4, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialDatabaseRequests(List<DatabaseRequestSignal> requests) {
        executeBatch("""
insert into e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?)""", requests, TraceDao::databaseRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteDatabaseRequests(List<Pair<DatabaseRequestSignal, DatabaseRequestUpdate>> requests) {
        executeBatchPair("""
insert into e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str,dh_end,va_cmd,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            databaseRequestSetter(ps, req);
            ps.setTimestamp(15, fromNullableInstant(callback.getEnd()));
            ps.setString(16, callback.getCommand());
            ps.setBoolean(17, callback.isFailed());
        });
    }

    static void databaseRequestSetter(PreparedStatement ps, DatabaseRequestSignal req) throws SQLException {
        ps.setString(1, req.getId());
        ps.setString(2, req.getSessionId());
        ps.setString(3, req.getInstanceId());
        ps.setString(4, req.getHost());
        ps.setInt(5, req.getPort());
        ps.setString(6, req.getScheme());
        ps.setString(7, req.getName());
        ps.setString(8, req.getSchema());
        ps.setString(9, req.getUser());
        ps.setString(10, req.getThreadName());
        ps.setString(11, req.getDriverVersion());
        ps.setString(12, req.getProductName());
        ps.setString(13, req.getProductVersion());
        ps.setTimestamp(14, fromNullableInstant(req.getStart()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateDatabaseRequests(List<DatabaseRequestUpdate> requests) {
        executeBatch("""
update e_dtb_rqt set dh_end = ?, va_cmd = ?, va_fail = ?
where id_dtb_rqt = ?::uuid""", requests, (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getEnd()));
            ps.setString(2, req.getCommand());
            ps.setBoolean(3, req.isFailed());
            ps.setString(4, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpRequestStages(List<HttpRequestStage> stages) {
        executeBatch("insert into e_rst_rqt_stg(va_nam,dh_str,dh_end,cd_ord,cd_rst_rqt) values(?,?,?,?,?::uuid)", stages, (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setInt(4, stg.getOrder());
            ps.setString(5, stg.getRequestId());
        });
        saveStageExceptions(stages, REST);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpSessionStages(List<HttpSessionStage> stages) {
        executeBatch("insert into e_rst_ses_stg(va_nam,dh_str,dh_end,cd_ord,cd_prn_ses) values(?,?,?,?,?::uuid)", stages, (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setInt(4, stg.getOrder());
            ps.setString(5, stg.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequestStages(List<MailRequestStage> stages) {
        executeBatch("insert into e_smtp_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_smtp_rqt) values(?,?,?,?,?,?::uuid)", stages, (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setString(4, stg.getCommand());
            ps.setInt(5, stg.getOrder());
            ps.setString(6, stg.getRequestId());
        });
        saveMailRequestMails(stages);
        saveStageExceptions(stages, SMTP);
    }

    private void saveMailRequestMails(List<MailRequestStage> mails) {
        executeBatch("insert into e_smtp_mail(va_sbj,va_cnt_typ,va_frm,va_rcp,va_rpl,va_sze,cd_smtp_rqt) values(?,?,?,?,?,?,?::uuid)", 
        		mails.stream().filter(m -> nonNull(m.getMail())).toList(), (ps, stg)-> {
		            ps.setString(1, stg.getMail().getSubject());
		            ps.setString(2, stg.getMail().getContentType());
		            if(nonNull(stg.getMail())) { //
		            	var mail = stg.getMail();
		                ps.setString(3, joinValuesOrNull(mail.getFrom()));
		                ps.setString(4, joinValuesOrNull(mail.getRecipients()));
		                ps.setString(5, joinValuesOrNull(mail.getReplyTo()));
		            }
		            else {
		            	ps.setNull(3, VARCHAR);
		            	ps.setNull(4, VARCHAR);
		            	ps.setNull(5, VARCHAR);
		            }
		            ps.setInt(6,stg.getMail().getSize());
		            ps.setString(7, stg.getRequestId());
		        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequestStages(List<FtpRequestStage> stages) {
        executeBatch("insert into e_ftp_stg(va_nam,dh_str,dh_end,va_cmd,va_arg,cd_ord,cd_ftp_rqt) values(?,?,?,?,?,?,?::uuid)", stages, (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setString(4, stg.getCommand());
            ps.setString(5, joinValuesOrNull(stg.getArgs()));
            ps.setInt(6, stg.getOrder());
            ps.setString(7, stg.getRequestId());
        });
        saveStageExceptions(stages, FTP);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequestStages(List<DirectoryRequestStage> stages) {
        executeBatch("insert into e_ldap_stg(va_nam,dh_str,dh_end,va_cmd,va_arg,cd_ord,cd_ldap_rqt) values(?,?,?,?,?,?,?::uuid)", stages, (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setString(4, stg.getCommand());
            ps.setString(5, joinValuesOrNull(stg.getArgs()));
            ps.setInt(6, stg.getOrder());
            ps.setString(7, stg.getRequestId());
        });
        saveStageExceptions(stages, LDAP);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequestStages(List<DatabaseRequestStage> stages) {
        executeBatch("insert into e_dtb_stg(va_nam,dh_str,dh_end,va_cnt,va_cmd,va_arg,cd_ord,cd_dtb_rqt) values(?,?,?,?,?,?,?,?::uuid)", stages, (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setString(4, valueOfNullableArray(stg.getCount()));
            ps.setString(5, stg.getCommand());
            ps.setString(6, joinValuesOrNull(stg.getArgs()));
            ps.setInt(7, stg.getOrder());
            ps.setString(8, stg.getRequestId());
        });
        saveStageExceptions(stages, JDBC);
    }

    private void saveStageExceptions(List<? extends AbstractStage> stages, RequestMask mask) {
        var exceptions = stages.stream()
                .filter(e -> nonNull(e.getException())).toList();
        executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?::uuid)", exceptions, (ps, exp) -> {
            ps.setString(1, mask.name());
            ps.setString(2, exp.getException().getType());
            ps.setString(3, exp.getException().getMessage());
            ps.setObject(4, safeWriteValue(exp.getException().getStackTraceRows(), mapper), OTHER);
            ps.setInt(5, exp.getOrder());
            ps.setString(6, exp.getRequestId());
        });
    }

    private void saveLocalRequestExceptions(List<LocalRequestUpdate> stages) {
        executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?::uuid)", stages, (ps, exp) -> {
            ps.setString(1, LOCAL.name());
            ps.setString(2, exp.getException().getType());
            ps.setString(3, exp.getException().getMessage());
            ps.setObject(4, safeWriteValue(exp.getException().getStackTraceRows(), mapper), OTHER);
            ps.setInt(5, 0);
            ps.setString(6, exp.getId());
        });
    }
    
    private <T extends EventTrace> void executeBatch(String sql, Collection<T> it, ParameterizedPreparedStatementSetter<T> pss) {
    	executeBatch(sql, it, pss, t-> publisher.publishEvent(new UnsavedEventTraceEvent(this, t, false)));
    }
    
    private <T extends EventTrace, V extends EventTrace> void executeBatchPair(String sql, Collection<Pair<T,V>> it, ParameterizedPreparedStatementSetter<Pair<T,V>> pss) {
    	executeBatch(sql, it, pss, t-> {
    		publisher.publishEvent(new UnsavedEventTraceEvent(this, t.getV1(), false));
    		publisher.publishEvent(new UnsavedEventTraceEvent(this, t.getV2(), true));
    	});
    }

    private <T> void executeBatch(String sql, Collection<T> it, ParameterizedPreparedStatementSetter<T> pss, Consumer<T> fallback) {
        try {
            template.batchUpdate(sql, it, BATCH_SIZE, pss);
        } catch (DuplicateKeyException ex) {
            log.warn("Batch insert failed due to duplicate key, retrying with single inserts", ex);
            try {
            	executeSingle(sql, it, pss, fallback);
			} catch (Exception e) {
				log.error("Failed to fallback traces", e);
			}
        }
    }

    private <T> void executeSingle(String sql, Collection<T> it, ParameterizedPreparedStatementSetter<T> pss, Consumer<T> fallback) {
        for(T t : it) {
            try {
                template.update(sql, (PreparedStatement ps) -> pss.setValues(ps, t));
            } catch (DuplicateKeyException e) {
            	 try {
            		 fallback.accept(t);
     			} catch (Exception ex) {
     				log.error("Failed to fallback traces", ex);
     			}
            }
        }
    }
}
