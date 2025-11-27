package org.usf.inspect.server.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.RequestCompletableType;
import org.usf.inspect.server.service.DatabaseDispatcherService;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import static java.sql.Types.*;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;
import static org.usf.inspect.core.RequestMask.*;
import static org.usf.inspect.server.Utils.*;
import static org.usf.inspect.server.model.RequestCompletableType.*;


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
            try {
                ps.setObject(14, toJsonOrNull(instance.getConfiguration()), OTHER);
                ps.setObject(15, toJsonOrNull(instance.getResource()), OTHER);
                ps.setObject(16, toJsonOrNull(instance.getAdditionalProperties()), OTHER);
            } catch (JsonProcessingException e) {
                throw new SQLException("parsing parameter", e);
            }
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateInstanceEnvironments(List<InstanceEnvironmentUpdate> instances){
        executeBatch("update e_env_ins set dh_end = ? where id_ins = ?::uuid", instances.iterator(), (ps, ins) -> {
            ps.setTimestamp(1, fromNullableInstant(ins.getEnd()));
            ps.setString(2, ins.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveInstanceTraces(List<InstanceTrace> instanceTraces) {
        executeBatch("insert into e_ins_trc (va_pnd, va_atp, va_trc_cnt, dh_str, va_fln, cd_ins) values (?, ?, ?, ?, ?, ?::uuid)", 
        		instanceTraces.iterator(), (ps, trc) -> {
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
        		logEntries.iterator(), (ps, o)-> {
		            ps.setString(1, String.valueOf(o.getLevel()));
		            ps.setString(2, o.getMessage());
		            ps.setObject(3, toJsonOrNull(o.getStackRows()), OTHER);
		            ps.setTimestamp(4, fromNullableInstant(o.getInstant()));
		            ps.setString(5, o.getSessionId());
		            ps.setString(6, o.getInstanceId());
		        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMachineResourceUsages(List<MachineResourceUsage> usages) {
        executeBatch("insert into e_rsc_usg(dh_str,va_usd_hep,va_cmt_hep,va_usd_dsk,cd_ins) values (?,?,?,?,?::uuid)", usages.iterator(), (ps, o)-> {
            ps.setTimestamp(1, fromNullableInstant(o.getInstant()));
            ps.setInt(2, o.getUsedHeap());
            ps.setInt(3, o.getCommitedHeap());
            ps.setInt(4, o.getUsedDiskSpace());
            ps.setString(5, o.getInstanceId());
        });
    }

    // New version
    @Transactional(rollbackFor = Throwable.class)
    public Long savePartialRestSessions(List<HttpSession2> sessions) {
        return executeBatch("""
insert into e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_lnk,dh_str,va_err_typ,va_err_msg,va_stk,va_nam,va_usr,va_usr_agt,va_cch_ctr)
values(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", sessions.iterator(), (ps, ses) -> {
            var exp = ses.getException();
            restSessionSetter(ps, ses);
            ps.setString(15, nonNull(exp) ? exp.getType() : null);
            ps.setString(16, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(17, nonNull(exp) && nonNull(exp.getStackTraceRows()) ? mapper.writeValueAsString(exp.getStackTraceRows()) : null, OTHER);
            ps.setString(18, ses.getName());
            ps.setString(19, ses.getUser());
            ps.setString(20, userAgentExtract(ses.getUserAgent()));
            ps.setString(21, ses.getCacheControl());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long saveCompleteRestSessions(List<DatabaseDispatcherService.Pair<HttpSession2, HttpSessionCallback>> sessions) {
        return executeBatch("""
insert into e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_i_sze,va_i_cnt_enc,va_thr,va_lnk,dh_str,dh_end,va_err_typ,va_err_msg,va_stk,va_nam,va_usr,va_usr_agt,va_cch_ctr,va_cnt_typ,cd_stt,va_o_sze,va_o_cnt_enc,va_msk)
values(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", sessions.iterator(), (ps, ses) -> {
            var session = ses.getV1();
            var callback = ses.getV2();
            var exp = nonNull(callback.getException()) ? callback.getException() : session.getException();
            restSessionSetter(ps, session);
            ps.setTimestamp(15, fromNullableInstant(callback.getEnd()));
            ps.setString(16, nonNull(exp) ? exp.getType() : null);
            ps.setString(17, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(18, nonNull(exp) && nonNull(exp.getStackTraceRows()) ? mapper.writeValueAsString(exp.getStackTraceRows()) : null, OTHER);
            ps.setString(19, nonNull(callback.getName()) ? callback.getName() : session.getName());
            ps.setString(20, nonNull(callback.getUser()) ? callback.getUser() : session.getUser());
            ps.setString(21, userAgentExtract(nonNull(callback.getUserAgent()) ? callback.getUserAgent() : session.getUserAgent()));
            ps.setString(22, nonNull(callback.getCacheControl()) ? callback.getCacheControl() : session.getCacheControl());
            ps.setString(23, contentTypeExtract(callback.getContentType()));
            ps.setInt(24, callback.getStatus());
            ps.setLong(25, callback.getDataSize());
            ps.setString(26, callback.getContentEncoding());
            ps.setInt(27, callback.getRequestMask().get());
        });
    }

    static void restSessionSetter(PreparedStatement ps, HttpSession2 ses) throws SQLException {
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
    public Long updateRestSessions(List<HttpSessionCallback> sessions) {
        return executeBatch("""
update e_rst_ses set va_err_typ = ?, va_err_msg = ?, va_stk = ?, va_nam = ?, va_usr = ?, va_usr_agt = ?, va_cch_ctr = ?, va_cnt_typ = ?, cd_stt = ?, va_o_sze = ?, va_o_cnt_enc = ?, dh_end = ?, va_msk = ?
where id_ses = ?::uuid""", sessions.iterator(), (ps, ses) -> {
            var exp = ses.getException();
            ps.setString(1, nonNull(exp) ? exp.getType() : null);
            ps.setString(2, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(3, nonNull(exp) && nonNull(exp.getStackTraceRows()) ? mapper.writeValueAsString(exp.getStackTraceRows()) : null, OTHER);
            ps.setString(4, ses.getName());
            ps.setString(5, ses.getUser());
            ps.setString(6, userAgentExtract(ses.getUserAgent()));
            ps.setString(7, ses.getCacheControl());
            ps.setString(8, contentTypeExtract(ses.getContentType()));
            ps.setInt(9, ses.getStatus());
            ps.setLong(10, ses.getDataSize());
            ps.setString(11, ses.getContentEncoding());
            ps.setTimestamp(12, fromNullableInstant(ses.getEnd()));
            ps.setInt(13, ses.getRequestMask().get());
            ps.setString(14, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long savePartialMainSessions(List<MainSession2> sessions) {
        return executeBatch("""
insert into e_main_ses(id_ses,cd_ins,va_nam,va_usr,va_typ,va_lct,va_thr,dh_str)
values(?::uuid,?::uuid,?,?,?,?,?,?)""", sessions.iterator(), (ps, ses) -> {
            mainSessionSetter(ps, ses);
            ps.setTimestamp(8, fromNullableInstant(ses.getStart()));
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long saveCompleteMainSessions(List<DatabaseDispatcherService.Pair<MainSession2, MainSessionCallback>> sessions) {
        return executeBatch("""
insert into e_main_ses(id_ses,cd_ins,va_nam,va_usr,va_typ,va_lct,va_thr,dh_str,dh_end,va_err_typ,va_err_msg,va_stk,va_msk)
values(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?)""", sessions.iterator(), (ps, ses) -> {
            var session = ses.getV1();
            var callback = ses.getV2();
            var exp = callback.getException();
            mainSessionSetter(ps, session);
            ps.setTimestamp(8, fromNullableInstant(nonNull(callback.getStart()) ? callback.getStart() : session.getStart()));
            ps.setTimestamp(9, fromNullableInstant(callback.getEnd()));
            ps.setString(10, nonNull(exp) ? exp.getType() : null);
            ps.setString(11, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(12, nonNull(exp) && nonNull(exp.getStackTraceRows()) ? mapper.writeValueAsString(exp.getStackTraceRows()) : null, OTHER);
            ps.setInt(13, callback.getRequestMask().get());
        });
    }

    static void mainSessionSetter(PreparedStatement ps, MainSession2 ses) throws SQLException {
        ps.setString(1, ses.getId());
        ps.setString(2, ses.getInstanceId());
        ps.setString(3, ses.getName());
        ps.setString(4, ses.getUser());
        ps.setString(5, valueOfNullable(ses.getType()));
        ps.setString(6, ses.getLocation());
        ps.setString(7, ses.getThreadName());
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long updateMainSessions(List<MainSessionCallback> sessions) {
        return executeBatch("""
update e_main_ses set dh_str = ?, dh_end = ?, va_err_typ = ?, va_err_msg = ?, va_stk = ?, va_msk = ?
where id_ses = ?::uuid""", sessions.iterator(), (ps, ses) -> {
            var exp = ses.getException();
            ps.setTimestamp(1, fromNullableInstant(ses.getStart()));
            ps.setTimestamp(2, fromNullableInstant(ses.getEnd()));
            ps.setString(3, nonNull(exp) ? exp.getType() : null);
            ps.setString(4, nonNull(exp) ? exp.getMessage() : null);
            ps.setObject(5, nonNull(exp) && nonNull(exp.getStackTraceRows()) ? mapper.writeValueAsString(exp.getStackTraceRows()) : null, OTHER);
            ps.setInt(6, ses.getRequestMask().get());
            ps.setString(7, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long savePartialRestRequests(List<HttpRequest2> requests) {
        return executeBatch("""
insert into e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?)""", requests.iterator(), TraceDao::restRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long saveCompleteRestRequests(List<DatabaseDispatcherService.Pair<HttpRequest2, HttpRequestCallback>> requests) {
        return executeBatch("""
insert into e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,dh_str,dh_end,va_thr,va_cnt_typ,cd_stt,va_i_sze,va_i_cnt_enc,va_bdy_cnt,va_lnk)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests.iterator(), (ps, ses) -> {
            var request = ses.getV1();
            var callback = ses.getV2();
            restRequestSetter(ps, request);
            ps.setTimestamp(15, fromNullableInstant(callback.getEnd()));
            ps.setString(16, contentTypeExtract(callback.getContentType()));
            ps.setInt(17, callback.getStatus());
            ps.setLong(18, callback.getDataSize());
            ps.setString(19, callback.getContentEncoding());
            ps.setString(20, callback.getBodyContent());
            ps.setBoolean(21, callback.isLinked());
        });
    }

    static void restRequestSetter(PreparedStatement ps, HttpRequest2 req) throws SQLException {
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
        ps.setTimestamp(14, fromNullableInstant(req.getStart()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long updateRestRequests(List<HttpRequestCallback> requests) {
        return executeBatch("""
update e_rst_rqt set va_cnt_typ = ?, cd_stt = ?, va_i_sze = ?, va_i_cnt_enc = ?, dh_end = ?, va_bdy_cnt = ?, va_lnk = ?
where id_rst_rqt = ?::uuid""", requests.iterator(), (ps, req) -> {
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
    public Long savePartialLocalRequests(List<LocalRequest2> requests) {
        return executeBatch("""
insert into e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?)""", requests.iterator(), (ps, req) -> {
            localRequestSetter(ps, req);
            ps.setTimestamp(9, fromNullableInstant(req.getStart()));
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long saveCompleteLocalRequests(List<DatabaseDispatcherService.Pair<LocalRequest2, LocalRequestCallback>> requests) {
        var rows = executeBatch("""
insert into e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str,dh_end,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?)""", requests.iterator(), (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            localRequestSetter(ps, req);
            ps.setTimestamp(9, fromNullableInstant(nonNull(callback.getStart()) ? callback.getStart() : req.getStart()));
            ps.setTimestamp(10, fromNullableInstant(callback.getEnd()));
            ps.setBoolean(11, nonNull(callback.getException()));
        });
        var exceptions = requests.stream()
                .map(DatabaseDispatcherService.Pair::getV2)
                .filter(r -> nonNull(r.getException()))
                .toList();
        if(!exceptions.isEmpty()) {
            saveLocalRequestExceptions(exceptions);
        }
        return rows;
    }

    static void localRequestSetter(PreparedStatement ps, LocalRequest2 req) throws SQLException {
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
    public Long updateLocalRequests(List<LocalRequestCallback> requests) {
        var rows = executeBatch("""
update e_lcl_rqt set dh_str = ?, dh_end = ?, va_fail = ?
where id_lcl_rqt = ?::uuid""", requests.iterator(), (ps, req) -> {
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
        return rows;
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long savePartialMailRequests(List<MailRequest2> requests) {
        return executeBatch("""
insert into e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?)""", requests.iterator(), TraceDao::mailRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long saveCompleteMailRequests(List<DatabaseDispatcherService.Pair<MailRequest2, MailRequestCallback>> requests) {
        return executeBatch("""
insert into e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?)""", requests.iterator(), (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            mailRequestSetter(ps, req);
            ps.setTimestamp(10, fromNullableInstant(callback.getEnd()));
            ps.setString(11, callback.getCommand());
            ps.setBoolean(12, callback.isFailed());
        });
    }

    static void mailRequestSetter(PreparedStatement ps, MailRequest2 req) throws SQLException {
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
    public Long updateMailRequests(List<MailRequestCallback> requests) {
        return executeBatch("""
update e_smtp_rqt set dh_end = ?, va_cmd = ?, va_fail = ?
where id_smtp_rqt = ?::uuid""", requests.iterator(), (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getEnd()));
            ps.setString(2, req.getCommand());
            ps.setBoolean(3, req.isFailed());
            ps.setString(4, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long savePartialFtpRequests(List<FtpRequest2> requests) {
        return executeBatch("""
insert into e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?)""", requests.iterator(), TraceDao::ftpRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long saveCompleteFtpRequests(List<DatabaseDispatcherService.Pair<FtpRequest2, FtpRequestCallback>> requests) {
        return executeBatch("""
insert into e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str,dh_end,va_cmd,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?)""", requests.iterator(), (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            ftpRequestSetter(ps, req);
            ps.setTimestamp(12, fromNullableInstant(callback.getEnd()));
            ps.setString(13, callback.getCommand());
            ps.setBoolean(14, callback.isFailed());
        });
    }

    static void ftpRequestSetter(PreparedStatement ps, FtpRequest2 req) throws SQLException {
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
    public Long updateFtpRequests(List<FtpRequestCallback> requests) {
        return executeBatch("""
update e_ftp_rqt set dh_end = ?, va_cmd = ?, va_fail = ?
where id_ftp_rqt = ?::uuid""", requests.iterator(), (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getEnd()));
            ps.setString(2, req.getCommand());
            ps.setBoolean(3, req.isFailed());
            ps.setString(4, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long savePartialLdapRequests(List<DirectoryRequest2> requests) {
        return executeBatch("""
insert into e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?)""", requests.iterator(), TraceDao::ldapRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long saveCompleteLdapRequests(List<DatabaseDispatcherService.Pair<DirectoryRequest2, DirectoryRequestCallback>> requests) {
        return executeBatch("""
insert into e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?)""", requests.iterator(), (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            ldapRequestSetter(ps, req);
            ps.setTimestamp(10, fromNullableInstant(callback.getEnd()));
            ps.setString(11, callback.getCommand());
            ps.setBoolean(12, callback.isFailed());
        });
    }

    static void ldapRequestSetter(PreparedStatement ps, DirectoryRequest2 req) throws SQLException {
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
    public Long updateLdapRequests(List<DirectoryRequestCallback> requests) {
        return executeBatch("""
update e_ldap_rqt set dh_end = ?, va_cmd = ?, va_fail = ?
where id_ldap_rqt = ?::uuid""", requests.iterator(), (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getEnd()));
            ps.setString(2, req.getCommand());
            ps.setBoolean(3, req.isFailed());
            ps.setString(4, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long savePartialDatabaseRequests(List<DatabaseRequest2> requests) {
        return executeBatch("""
insert into e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,dh_str,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?)""", requests.iterator(), TraceDao::databaseRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public Long saveCompleteDatabaseRequests(List<DatabaseDispatcherService.Pair<DatabaseRequest2, DatabaseRequestCallback>> requests) {
        return executeBatch("""
insert into e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str,dh_end,va_cmd,va_fail)
values(?::uuid,?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests.iterator(), (ps, pair) -> {
            var req = pair.getV1();
            var callback = pair.getV2();
            databaseRequestSetter(ps, req);
            ps.setTimestamp(15, fromNullableInstant(callback.getEnd()));
            ps.setString(16, callback.getCommand());
            ps.setBoolean(17, callback.isFailed());
        });
    }

    static void databaseRequestSetter(PreparedStatement ps, DatabaseRequest2 req) throws SQLException {
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
    public Long updateDatabaseRequests(List<DatabaseRequestCallback> requests) {
        return executeBatch("""
update e_dtb_rqt set dh_end = ?, va_cmd = ?, va_fail = ?
where id_dtb_rqt = ?::uuid""", requests.iterator(), (ps, req) -> {
            ps.setTimestamp(1, fromNullableInstant(req.getEnd()));
            ps.setString(2, req.getCommand());
            ps.setBoolean(3, req.isFailed());
            ps.setString(4, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpRequestStages(List<HttpRequestStage> stages) {
        executeBatch("insert into e_rst_rqt_stg(va_nam,dh_str,dh_end,cd_ord,cd_rst_rqt) values(?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
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
        executeBatch("insert into e_rst_ses_stg(va_nam,dh_str,dh_end,cd_ord,cd_prn_ses) values(?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
            ps.setString(1, stg.getName());
            ps.setTimestamp(2, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stg.getEnd()));
            ps.setInt(4, stg.getOrder());
            ps.setString(5, stg.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequestStages(List<MailRequestStage> stages) {
        executeBatch("insert into e_smtp_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_smtp_rqt) values(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
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
        		mails.stream().filter(m -> nonNull(m.getMail())).iterator(), (ps, stg)-> {
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
        executeBatch("insert into e_ftp_stg(va_nam,dh_str,dh_end,va_cmd,va_arg,cd_ord,cd_ftp_rqt) values(?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
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
        executeBatch("insert into e_ldap_stg(va_nam,dh_str,dh_end,va_cmd,va_arg,cd_ord,cd_ldap_rqt) values(?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
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
        executeBatch("insert into e_dtb_stg(va_nam,dh_str,dh_end,va_cnt,va_cmd,va_arg,cd_ord,cd_dtb_rqt) values(?,?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stg)-> {
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
                .filter(e -> nonNull(e.getException()))
                .iterator();
        executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?::uuid)", exceptions, (ps, exp) -> {
            ps.setString(1, mask.name());
            ps.setString(2, exp.getException().getType());
            ps.setString(3, exp.getException().getMessage());
            ps.setObject(4, toJsonOrNull(exp.getException().getStackTraceRows()), OTHER);
            ps.setInt(5, exp.getOrder());
            ps.setString(6, exp.getRequestId());
        });
    }

    private void saveLocalRequestExceptions(List<LocalRequestCallback> stages) {
        executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, exp) -> {
            ps.setString(1, LOCAL.name());
            ps.setString(2, exp.getException().getType());
            ps.setString(3, exp.getException().getMessage());
            ps.setObject(4, toJsonOrNull(exp.getException().getStackTraceRows()), OTHER);
            ps.setInt(5, 0);
            ps.setString(6, exp.getId());
        });
    }

    private <T> Long executeBatch(String sql, Iterator<T> it, PreparedStatementSetter<T> pss) {
        return it.hasNext() ? template.execute(sql, (PreparedStatement ps) -> {
        	long idx = 0;
            long rows = 0;
            try {
                while (it.hasNext()) {
                    pss.setValues(ps, it.next());
                    ps.addBatch();
                    if (++idx % BATCH_SIZE == 0) {
                        rows += stream(ps.executeBatch()).sum();
                    }
                }
            }
            catch (JsonProcessingException e) {
            	throw new SQLException("parsing parameter, row="+idx, e);
            }
            if (idx % BATCH_SIZE > 0) {
                rows += stream(ps.executeBatch()).sum();
            }
            log.debug("{}/{} rows was updated", idx, rows);
            return rows;
        }) : 0L;
    }

    String toJsonOrNull(Object o) throws JsonProcessingException {
    	return nonNull(o) ? mapper.writeValueAsString(o) : null;
    }
    
    static String joinValuesOrNull(String... args) {
    	return nonNull(args) ? String.join(", ", args) : null;
    }
    
    @FunctionalInterface
    interface PreparedStatementSetter<T> {
    	
    	void setValues(PreparedStatement ps, T argument) throws SQLException, JsonProcessingException;
    }
}
