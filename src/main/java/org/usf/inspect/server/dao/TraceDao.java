package org.usf.inspect.server.dao;

import static java.sql.Types.INTEGER;
import static java.sql.Types.OTHER;
import static java.sql.Types.VARCHAR;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.jdbc.datasource.DataSourceUtils.getConnection;
import static org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection;
import static org.usf.inspect.core.RequestMask.LOCAL;
import static org.usf.inspect.server.JsonUtils.safeWriteValue;
import static org.usf.inspect.server.Utils.contentTypeExtract;
import static org.usf.inspect.server.Utils.fromNullableInstant;
import static org.usf.inspect.server.Utils.joinValuesOrNull;
import static org.usf.inspect.server.Utils.userAgentExtract;
import static org.usf.inspect.server.Utils.valueOfNullable;
import static org.usf.inspect.server.Utils.valueOfNullableArray;
import static org.usf.jquery.core.Utils.isEmpty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.*;
import org.usf.inspect.server.event.UnsavedEventTraceEvent;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.TracePacket;
import org.usf.inspect.server.model.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Using Types.OTHER with JSON serialization to ensure portability:
 * - In PostgreSQL: Types.OTHER is interpreted as native JSONB type
 * - In H2: Types.OTHER is treated as VARCHAR without JSON parsing attempts
 * Prior serialization with writeValueAsString prevents conversion errors
 * between the two DBMSs, making the code compatible with both environments.
 */
@Slf4j
@Repository
public class TraceDao {

    private static final int BATCH_SIZE = 1_000;

    private final JdbcTemplate template;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher publisher;
    private final boolean supportsSavePoints;

	public TraceDao(JdbcTemplate template, ObjectMapper mapper, ApplicationEventPublisher publisher) {
		this.template = template;
		this.mapper = mapper;
		this.publisher = publisher;
		this.supportsSavePoints = supportsSavePoints(template.getDataSource());
	}

    public void saveInstanceEnvironment(InstanceEnvironment instance) {
        template.update("""
insert into e_env_ins(id_ins,va_typ,dh_str,va_app,va_vrs,va_adr,va_env,va_os,va_re,va_usr,va_clr,va_brch,va_hsh,va_cnf,va_rsr,va_add_prp,cd_nsp)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", ps -> {
            var idx=0;
            ps.setObject(++idx, instance.getId());
            ps.setString(++idx, ofNullable(instance.getType()).map(InstanceType::name).orElse(null));
            ps.setTimestamp(++idx, fromNullableInstant(instance.getInstant()));
            ps.setString(++idx, instance.getName());
            ps.setString(++idx, instance.getVersion());
            ps.setString(++idx, instance.getAddress());
            ps.setString(++idx, instance.getEnv());
            ps.setString(++idx, instance.getOs());
            ps.setString(++idx, instance.getRe());
            ps.setString(++idx, instance.getUser());
            ps.setString(++idx, instance.getCollector());
            ps.setString(++idx, instance.getBranch());
            ps.setString(++idx, instance.getHash());
            ps.setObject(++idx, safeWriteValue(instance.getConfiguration(), mapper), OTHER);
            ps.setObject(++idx, safeWriteValue(instance.getResource(), mapper), OTHER);
            ps.setObject(++idx, safeWriteValue(instance.getAdditionalProperties(), mapper), OTHER);
            ps.setString(++idx, instance.getNamespace());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateInstanceEnvironments(List<InstanceEnvironmentUpdate> instances){
        executeBatch("update e_env_ins set dh_end = ? where id_ins = ?::uuid", instances, (ps, ins) -> {
            ps.setTimestamp(1, fromNullableInstant(ins.getEnd()));
            ps.setObject(2, ins.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveInstanceTraces(List<TracePacket> instanceTraces) {
        executeBatch("insert into e_ins_trc (va_pnd, va_atp, va_trc_cnt, dh_str, cd_ins) values (?, ?, ?, ?, ?, ?)",
                instanceTraces, (ps, trc) -> {
                    var idx=0;
                    ps.setObject(++idx, trc.getPending(), INTEGER);
                    ps.setObject(++idx, trc.getAttempts(), INTEGER);
                    ps.setInt(++idx, trc.getTraceCount());
                    ps.setTimestamp(++idx, fromNullableInstant(trc.getInstant()));
//                    ps.setString(++idx, trc.getFileName());
                    //TODO save sequence & delete filename column
                    ps.setObject(++idx, trc.getInstanceId());
                });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLogEntries(List<LogEntry> logEntries) {
        executeBatch("insert into e_log_ent(va_lvl,va_msg,va_stk,dh_str,cd_prn_ses,cd_ins) values (?,?,?,?,?,?)",
                logEntries, (ps, o)-> {
                    var idx=0;
                    ps.setString(++idx, String.valueOf(o.getLevel()));
                    ps.setString(++idx, o.getMessage());
                    ps.setObject(++idx, safeWriteValue(o.getStackRows(), mapper), OTHER);
                    ps.setTimestamp(++idx, fromNullableInstant(o.getInstant()));
                    ps.setObject(++idx, o.getSessionId());
                    ps.setObject(++idx, o.getInstanceId());
                });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMachineResourceUsages(List<MachineResourceUsage> usages) {
        executeBatch("insert into e_rsc_usg(dh_str,va_usd_hep,va_cmt_hep,va_usd_dsk,cd_ins) values (?,?,?,?,?,?)", usages, (ps, o)-> {
            var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(o.getInstant()));
            ps.setInt(++idx, o.getUsedHeap());
            ps.setInt(++idx, o.getCommitedHeap());
            ps.setInt(++idx, o.getUsedDiskSpace());
            ps.setObject(++idx, o.getInstanceId());
            //TODO add column + save  activeThreadCount, startedThreadCount, cpuUsage
        });
    }

    // New version
    @Transactional(rollbackFor = Throwable.class)
    public void savePartialRestSessions(List<HttpSessionSignal> sessions) {
        executeBatch("""
insert into e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_lnk,dh_str,va_nam,va_usr,va_usr_agt,va_msk,va_fwd_add)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", sessions, (ps, ses) -> {
            var idx = restSessionSetter(ps, ses);
            ps.setString(++idx, ses.getName());
            ps.setString(++idx, ses.getUser());
            ps.setString(++idx, userAgentExtract(ses.getUserAgent()));
            ps.setInt(++idx, 0);
            ps.setString(++idx, Arrays.toString(ses.getForwardedAddresses()));
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteRestSessions(List<Pair<HttpSessionSignal, HttpSessionUpdate>> sessions) {
    	executeBatchPair("""
insert into e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_i_sze,va_i_cnt_enc,va_thr,va_lnk,dh_str,dh_end,va_nam,va_usr,va_usr_agt,va_cch_ctr,va_cnt_typ,cd_stt,va_o_sze,va_o_cnt_enc,va_msk,va_fwd_add)
values(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", sessions, (ps, ses) -> {
            var session = ses.signal();
            var callback = ses.update();
            var idx = restSessionSetter(ps, session);
            ps.setTimestamp(++idx, fromNullableInstant(callback.getEnd()));
            ps.setString(++idx, nonNull(callback.getName()) ? callback.getName() : session.getName());
            ps.setString(++idx, nonNull(callback.getUser()) ? callback.getUser() : session.getUser());
            ps.setString(++idx, userAgentExtract(session.getUserAgent()));
            ps.setString(++idx, callback.getCacheControl());
            ps.setString(++idx, contentTypeExtract(callback.getContentType()));
            ps.setShort(++idx, callback.getStatus());
            ps.setLong(++idx, callback.getDataSize());
            ps.setString(++idx, callback.getContentEncoding());
            ps.setInt(++idx, callback.getRequestMask().get());
            ps.setString(++idx, Arrays.toString(session.getForwardedAddresses()));
        });
    }

    static int restSessionSetter(PreparedStatement ps, HttpSessionSignal ses) throws SQLException {
        var idx=0;
        ps.setObject(++idx, ses.getId());
        ps.setObject(++idx, ses.getInstanceId());
        ps.setString(++idx, ses.getMethod());
        ps.setString(++idx, ses.getProtocol());
        ps.setString(++idx, ses.getHost());
        ps.setInt(++idx, ses.getPort());
        ps.setString(++idx, ses.getPath());
        ps.setString(++idx, ses.getQuery());
        ps.setString(++idx, ses.getAuthScheme());
        ps.setLong(++idx, ses.getDataSize());
        ps.setString(++idx, ses.getContentEncoding());
        ps.setString(++idx, ses.getThreadName());
        ps.setBoolean(++idx, ses.isLinked());
        ps.setTimestamp(++idx, fromNullableInstant(ses.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateRestSessions(List<HttpSessionUpdate> sessions) {
        executeBatch("""
update e_rst_ses set  va_nam = coalesce(?, va_nam), va_usr = coalesce(?, va_usr), va_cch_ctr = coalesce(?, va_cch_ctr), va_cnt_typ = ?, cd_stt = ?, va_o_sze = ?, va_o_cnt_enc = ?, dh_end = ?, va_msk = ?
where id_ses = ?""", sessions, (ps, ses) -> {
            var idx = 0;
            ps.setString(++idx, ses.getName());
            ps.setString(++idx, ses.getUser());
            ps.setString(++idx, ses.getCacheControl());
            ps.setString(++idx, contentTypeExtract(ses.getContentType()));
            ps.setShort(++idx, ses.getStatus());
            ps.setLong(++idx, ses.getDataSize());
            ps.setString(++idx, ses.getContentEncoding());
            ps.setTimestamp(++idx, fromNullableInstant(ses.getEnd()));
            ps.setInt(++idx, ses.getRequestMask().get());
            ps.setObject(++idx, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMaskRestSessions(List<SessionMaskUpdate> sessions) {
        executeBatch("update e_rst_ses set va_msk = ? where id_ses = ?", sessions, (ps, ses) -> {
            ps.setInt(1, ses.getMask());
            ps.setObject(2, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialMainSessions(List<MainSessionSignal> sessions) {
        executeBatch("""
insert into e_main_ses(id_ses,cd_ins,va_typ,va_thr,va_lct,va_nam,va_usr,dh_str,va_msk)
values(?,?,?,?,?,?,?,?,?)""", sessions, (ps, ses) -> {
            var idx = mainSessionSetter(ps, ses);
            ps.setString(++idx, ses.getLocation());
            ps.setString(++idx, ses.getName());
            ps.setString(++idx, ses.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(ses.getStart()));
          //  ps.setInt(++idx, 0); //Unnecessary
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteMainSessions(List<Pair<MainSessionSignal, MainSessionUpdate>> sessions) {
    	executeBatchPair("""
insert into e_main_ses(id_ses,cd_ins,va_typ,va_thr,va_lct,va_nam,va_usr,dh_str,dh_end,va_msk)
values(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?)""", sessions, (ps, ses) -> {
            var session = ses.signal();
            var callback = ses.update();
            var idx = mainSessionSetter(ps, session);
            ps.setString(++idx, nonNull(callback.getLocation()) ? callback.getLocation() : session.getLocation());
            ps.setString(++idx, nonNull(callback.getName()) ? callback.getName() : session.getName());
            ps.setString(++idx, nonNull(callback.getUser()) ? callback.getUser() : session.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(nonNull(callback.getStart()) ? callback.getStart() : session.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(callback.getEnd()));
            ps.setInt(++idx, callback.getRequestMask().get());
        });
    }

    static int mainSessionSetter(PreparedStatement ps, MainSessionSignal ses) throws SQLException {
    	var idx=0;
        ps.setObject(++idx, ses.getId());
        ps.setObject(++idx, ses.getInstanceId());
        ps.setString(++idx, valueOfNullable(ses.getType()));
        ps.setString(++idx, ses.getThreadName());
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMainSessions(List<MainSessionUpdate> sessions) {
        executeBatch("""
update e_main_ses set va_lct = coalesce(?, va_lct), va_nam = coalesce(?, va_nam), va_usr = coalesce(?, va_usr), dh_str = coalesce(?, dh_str), dh_end = ?, va_msk = ?
where id_ses = ?""", sessions, (ps, ses) -> {
            var idx = 0;
            ps.setString(++idx, ses.getLocation());
            ps.setString(++idx, ses.getName());
            ps.setString(++idx, ses.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(ses.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(ses.getEnd()));
            ps.setInt(++idx, ses.getRequestMask().get());
            ps.setObject(++idx, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMaskMainSessions(List<SessionMaskUpdate> sessions) {
        executeBatch("update e_main_ses set va_msk = ? where id_ses = ?", sessions, (ps, ses) -> {
            var idx = 0;
            ps.setInt(++idx, ses.getMask());
            ps.setObject(++idx, ses.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialRestRequests(List<HttpRequestSignal> requests) {
        executeBatch("""
insert into e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_usr,dh_str)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, TraceDao::restRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteRestRequests(List<Pair<HttpRequestSignal, HttpRequestUpdate>> requests) {
    	executeBatchPair("""
insert into e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_usr,dh_str,dh_end,va_cnt_typ,cd_stt,va_i_sze,va_i_cnt_enc,va_bdy_cnt,va_lnk)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, ses) -> {
            var request = ses.signal();
            var callback = ses.update();
            var idx = restRequestSetter(ps, request);
            ps.setTimestamp(++idx, fromNullableInstant(callback.getEnd()));
            ps.setString(++idx, contentTypeExtract(callback.getContentType()));
            ps.setShort(++idx, callback.getStatus());
            ps.setLong(++idx, callback.getDataSize());
            ps.setString(++idx, callback.getContentEncoding());
            ps.setString(++idx, callback.getBodyContent());
            ps.setBoolean(++idx, callback.isLinked());
        });
    }

    static int restRequestSetter(PreparedStatement ps, HttpRequestSignal req) throws SQLException {
        var idx = 0;
        ps.setObject(++idx, req.getId());
        ps.setObject(++idx, req.getSessionId());
        ps.setObject(++idx, req.getInstanceId());
        ps.setString(++idx, req.getMethod());
        ps.setString(++idx, req.getProtocol());
        ps.setString(++idx, req.getHost());
        ps.setInt(++idx, req.getPort());
        ps.setString(++idx, req.getPath());
        ps.setString(++idx, req.getQuery());
        ps.setString(++idx, req.getAuthScheme());
        ps.setLong(++idx, req.getDataSize());
        ps.setString(++idx, req.getContentEncoding());
        ps.setString(++idx, req.getThreadName());
        ps.setString(++idx, req.getUser());
        ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateRestRequests(List<HttpRequestUpdate> requests) {
        executeBatch("""
update e_rst_rqt set va_cnt_typ = ?, cd_stt = ?, va_i_sze = ?, va_i_cnt_enc = ?, dh_end = ?, va_bdy_cnt = ?, va_lnk = ?
where id_rst_rqt = ?::uuid""", requests, (ps, req) -> {

            var idx = 0;
            ps.setString(++idx, contentTypeExtract(req.getContentType()));
            ps.setShort(++idx, req.getStatus());
            ps.setLong(++idx, req.getDataSize());
            ps.setString(++idx, req.getContentEncoding());
            ps.setTimestamp(++idx, fromNullableInstant(req.getEnd()));
            ps.setString(++idx, req.getBodyContent());
            ps.setBoolean(++idx, req.isLinked());
            ps.setObject(++idx, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialLocalRequests(List<LocalRequestSignal> requests) {
        executeBatch("""
insert into e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str)
values(?,?,?,?,?,?,?,?,?)""", requests, (ps, req) -> {
            var idx = localRequestSetter(ps, req);
            ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteLocalRequests(List<Pair<LocalRequestSignal, LocalRequestUpdate>> requests) {
    	executeBatchPair("""
insert into e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str,dh_end,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.signal();
            var callback = pair.update();
          var idx=  localRequestSetter(ps, req);
            ps.setTimestamp(++idx, fromNullableInstant(nonNull(callback.getStart()) ? callback.getStart() : req.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(callback.getEnd()));
            ps.setShort(++idx, callback.getStatus());
        });

    }

    static int localRequestSetter(PreparedStatement ps, LocalRequestSignal req) throws SQLException {
        var idx=0;
        ps.setObject(++idx, req.getId());
        ps.setObject(++idx, req.getSessionId());
        ps.setObject(++idx, req.getInstanceId()); //instance id
        ps.setString(++idx, req.getType());
        ps.setString(++idx, req.getName());
        ps.setString(++idx, req.getLocation());
        ps.setString(++idx, req.getUser());
        ps.setString(++idx, req.getThreadName());
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateLocalRequests(List<LocalRequestUpdate> requests) {
        executeBatch("""
update e_lcl_rqt set dh_str = coalesce(?, dh_str), dh_end = ?, status = ?
where id_lcl_rqt = ?""", requests, (ps, req) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(req.getEnd()));
            ps.setBoolean(++idx, nonNull(req.getException())); //TODO req.status !!??
            ps.setObject(++idx, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialMailRequests(List<MailRequestSignal> requests) {
        executeBatch("""
insert into e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
values(?,?,?,?,?,?,?,?,?)""", requests, TraceDao::mailRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteMailRequests(List<Pair<MailRequestSignal, MailRequestUpdate>> requests) {
        executeBatchPair("""
insert into e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.signal();
            var callback = pair.update();
            var idx=mailRequestSetter(ps, req);
            ps.setTimestamp(++idx, fromNullableInstant(callback.getEnd()));
            ps.setString(++idx, callback.getCommand());
            ps.setShort(++idx, callback.getStatus());
        });
    }

    static int mailRequestSetter(PreparedStatement ps, MailRequestSignal req) throws SQLException {
        var idx = 0;
        ps.setObject(++idx, req.getId());
        ps.setObject(++idx, req.getSessionId());
        ps.setObject(++idx, req.getInstanceId()); //instance id
        ps.setString(++idx, req.getHost());
        ps.setInt(++idx, req.getPort());
        ps.setString(++idx, req.getProtocol());
        ps.setString(++idx, req.getUser());
        ps.setString(++idx, req.getThreadName());
        ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMailRequests(List<MailRequestUpdate> requests) {
        executeBatch("""
update e_smtp_rqt set dh_end = ?, va_cmd = ?, status = ?
where id_smtp_rqt = ?""", requests, (ps, req) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(req.getEnd()));
            ps.setString(++idx, req.getCommand());
            ps.setShort(++idx, req.getStatus());
            ps.setObject(++idx, req.getId());
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
insert into e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.signal();
            var callback = pair.update();
            var idx = ftpRequestSetter(ps, req);
            ps.setTimestamp(++idx, fromNullableInstant(callback.getEnd()));
            ps.setString(++idx, callback.getCommand());
            ps.setShort(++idx, callback.getStatus());
        });
    }

    static int ftpRequestSetter(PreparedStatement ps, FtpRequestSignal req) throws SQLException {
      var idx = 0;
        ps.setObject(++idx, req.getId());
        ps.setObject(++idx, req.getSessionId());
        ps.setObject(++idx, req.getInstanceId());
        ps.setString(++idx, req.getHost());
        ps.setInt(++idx, req.getPort());
        ps.setString(++idx, req.getProtocol());
        ps.setString(++idx, req.getServerVersion());
        ps.setString(++idx, req.getClientVersion());
        ps.setString(++idx, req.getUser());
        ps.setString(++idx, req.getThreadName());
        ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateFtpRequests(List<FtpRequestUpdate> requests) {
        executeBatch("""
update e_ftp_rqt set dh_end = ?, va_cmd = ?, status = ?
where id_ftp_rqt = ?""", requests, (ps, req) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(req.getEnd()));
            ps.setString(++idx, req.getCommand());
            ps.setShort(++idx, req.getStatus());
            ps.setObject(++idx, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialLdapRequests(List<DirectoryRequestSignal> requests) {
        executeBatch("""
insert into e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
values(?,?,?,?,?,?,?,?,?)""", requests, TraceDao::ldapRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteLdapRequests(List<Pair<DirectoryRequestSignal, DirectoryRequestUpdate>> requests) {
        executeBatchPair("""
insert into e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.signal();
            var callback = pair.update();
            var idx = ldapRequestSetter(ps, req);
            ps.setTimestamp(++idx, fromNullableInstant(callback.getEnd()));
            ps.setString(++idx, callback.getCommand());
            ps.setShort(++idx, callback.getStatus());
        });
    }

    static int ldapRequestSetter(PreparedStatement ps, DirectoryRequestSignal req) throws SQLException {
            var idx = 0;
       ps.setObject(++idx, req.getId());
        ps.setObject(++idx, req.getSessionId());
        ps.setObject(++idx, req.getInstanceId());
        ps.setString(++idx, req.getHost());
        ps.setInt(++idx, req.getPort());
        ps.setString(++idx, req.getProtocol());
        ps.setString(++idx, req.getUser());
        ps.setString(++idx, req.getThreadName());
        ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateLdapRequests(List<DirectoryRequestUpdate> requests) {
        executeBatch("""
update e_ldap_rqt set dh_end = ?, va_cmd = ?, status = ?
where id_ldap_rqt = ?""", requests, (ps, req) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(req.getEnd()));
            ps.setString(++idx, req.getCommand());
            ps.setShort(++idx, req.getStatus());
            ps.setObject(++idx, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void savePartialDatabaseRequests(List<DatabaseRequestSignal> requests) {
        executeBatch("""
insert into e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, TraceDao::databaseRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveCompleteDatabaseRequests(List<Pair<DatabaseRequestSignal, DatabaseRequestUpdate>> requests) {
        executeBatchPair("""
insert into e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str,dh_end,va_cmd,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var req = pair.signal();
            var callback = pair.update();
            var idx = databaseRequestSetter(ps, req);
            ps.setTimestamp(++idx, fromNullableInstant(callback.getEnd()));
            ps.setString(++idx, callback.getCommand());
            ps.setShort(++idx, callback.getStatus());
        });
    }

    static int databaseRequestSetter(PreparedStatement ps, DatabaseRequestSignal req) throws SQLException {
        var idx = 0;
        ps.setObject(++idx, req.getId());
        ps.setObject(++idx, req.getSessionId());
        ps.setObject(++idx, req.getInstanceId());
        ps.setString(++idx, req.getHost());
        ps.setInt(++idx, req.getPort());
        ps.setString(++idx, req.getSchema());
        ps.setString(++idx, req.getName());
        ps.setString(++idx, req.getSchema());
        ps.setString(++idx, req.getUser());
        ps.setString(++idx, req.getThreadName());
        ps.setString(++idx, req.getDriverVersion());
        ps.setString(++idx, req.getProductName());
        ps.setString(++idx, req.getProductVersion());
        ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateDatabaseRequests(List<DatabaseRequestUpdate> requests) {
        executeBatch("""
update e_dtb_rqt set dh_end = ?, va_cmd = ?, status = ?
where id_dtb_rqt = ?""", requests, (ps, req) -> {
           var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(req.getEnd()));
            ps.setString(++idx, req.getCommand());
            ps.setShort(++idx, req.getStatus());
            ps.setObject(++idx, req.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpRequestStages(List<HttpRequestStage> stages) {
        executeBatch("insert into e_rst_rqt_stg(va_nam,dh_str,dh_end,cd_ord,cd_rst_rqt) values(?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });
      //  saveStageExceptions(stages, REST);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpSessionStages(List<HttpSessionStage> stages) {
        executeBatch("insert into e_rst_ses_stg(va_nam,dh_str,dh_end,cd_ord,cd_prn_ses) values(?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequestStages(List<MailRequestStage> stages) {
        executeBatch("insert into e_smtp_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_smtp_rqt) values(?,?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, stg.getCommand());
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });
        saveMailRequestMails(stages);
       // saveStageExceptions(stages, SMTP);
    }

    private void saveMailRequestMails(List<MailRequestStage> mails) {
        executeBatch("insert into e_smtp_mail(va_sbj,va_cnt_typ,va_frm,va_rcp,va_rpl,va_sze,cd_smtp_rqt) values(?,?,?,?,?,?,?)",
                mails.stream().filter(m -> nonNull(m.getMail())).toList(), (ps, stg)-> {
                    var idx=0;
                    ps.setString(++idx, stg.getMail().getSubject());
                    ps.setString(++idx, stg.getMail().getContentType());
                    if(nonNull(stg.getMail())) { //
                        var mail = stg.getMail();
                        ps.setString(++idx, joinValuesOrNull(mail.getFrom()));
                        ps.setString(++idx, joinValuesOrNull(mail.getRecipients()));
                        ps.setString(++idx, joinValuesOrNull(mail.getReplyTo()));
                    }
                    else {
                        ps.setNull(++idx, VARCHAR);
                        ps.setNull(++idx, VARCHAR);
                        ps.setNull(++idx, VARCHAR);
                    }
                    ps.setInt(++idx,stg.getMail().getSize());
                    ps.setObject(++idx, stg.getRequestId());
                });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequestStages(List<FtpRequestStage> stages) {
        executeBatch("insert into e_ftp_stg(va_nam,dh_str,dh_end,va_cmd,va_arg,cd_ord,cd_ftp_rqt) values(?,?,?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, stg.getCommand());
            ps.setString(++idx, joinValuesOrNull(stg.getArgs()));
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });

    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequestStages(List<DirectoryRequestStage> stages) {
        executeBatch("insert into e_ldap_stg(va_nam,dh_str,dh_end,va_cmd,va_arg,cd_ord,cd_ldap_rqt) values(?,?,?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, stg.getCommand());
            ps.setString(++idx, joinValuesOrNull(stg.getArgs()));
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });

    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequestStages(List<DatabaseRequestStage> stages) {
        executeBatch("insert into e_dtb_stg(va_nam,dh_str,dh_end,va_cnt,va_cmd,va_arg,cd_ord,cd_dtb_rqt) values(?,?,?,?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, valueOfNullableArray(stg.getCount()));
            ps.setString(++idx, stg.getCommand());
            ps.setString(++idx, joinValuesOrNull(stg.getArgs()));
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });
    }


    @Deprecated(forRemoval = true)
    private void saveStageExceptions(List<? extends AbstractStage> stages, RequestMask mask) {
        var exceptions = stages.stream()
                .filter(e -> nonNull(e.getException())).toList();
        executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?)", exceptions, (ps, exp) -> {
            var idx=0;
            ps.setString(++idx, mask.name());
            ps.setString(++idx, exp.getException().getType());
            ps.setString(++idx, exp.getException().getMessage());
            ps.setObject(++idx, safeWriteValue(exp.getException().getStackTraceRows(), mapper), OTHER);
            ps.setInt(++idx, exp.getOrder());
            ps.setObject(++idx, exp.getRequestId());//.toString() ?
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveExceptionTraces(List<ExceptionTrace> exceptions) {
        executeBatch("insert into e_exc_inf(va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?)", exceptions, (ps, exp) -> {
            var idx=0;
            ps.setString(++idx, exp.getType());
            ps.setString(++idx, exp.getMessage());
            ps.setObject(++idx, safeWriteValue(exp.getStackTraceRows(), mapper), OTHER);
            ps.setLong(++idx, exp.getOffset());
            ps.setObject(++idx, exp.getTraceId()); //getTraceId can
            //TODO add cause exception as json !
        });
    }

    @Deprecated(forRemoval = true)
    private void saveLocalRequestExceptions(List<LocalRequestUpdate> stages) {
        executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?,?,?)", stages, (ps, exp) -> {
            var idx=0;
            ps.setString(++idx, LOCAL.name());
            ps.setString(++idx, exp.getException().getType());
            ps.setString(++idx, exp.getException().getMessage());
            ps.setObject(++idx, safeWriteValue(exp.getException().getStackTraceRows(), mapper), OTHER);
            ps.setInt(++idx, 0);
            ps.setObject(++idx, exp.getId());
        });
    }

    private <T extends EventTrace> void executeBatch(String sql, List<T> records, ParameterizedPreparedStatementSetter<T> pss) {
        updateAll(sql, records, pss, t-> publisher.publishEvent(new UnsavedEventTraceEvent(this, t, false)));
    }

    private <T extends EventTrace, V extends EventTrace> void executeBatchPair(String sql, List<Pair<T,V>> it, ParameterizedPreparedStatementSetter<Pair<T,V>> pss) {
		updateAll(sql, it, pss, t-> {
			publisher.publishEvent(new UnsavedEventTraceEvent(this, t.signal(), false));
			publisher.publishEvent(new UnsavedEventTraceEvent(this, t.update(), true));
		});
    }

    private <T> void updateAll(String sql, List<T> records, ParameterizedPreparedStatementSetter<T> pss, Consumer<T> fallback) {
    	if(isEmpty(records)) {
    		return;
    	}
		var cnx = getConnection(template.getDataSource()); //current transaction connection
		Savepoint sp = null;
		if(supportsSavePoints) {
			try {
				sp = cnx.setSavepoint();
			}
			catch (SQLException e) {
				log.warn("Failed to create savepoint before batch update", e);
			}
		}
		try {
			template.batchUpdate(sql, records, BATCH_SIZE, pss);
			if(nonNull(sp)) {
				try {
					cnx.releaseSavepoint(sp);
				}
				catch (SQLException e) {
					log.warn("Failed to release savepoint after batch update", e);
				}
			}
		} catch (DuplicateKeyException e) { //SQLState 23505
			if(nonNull(sp)) {
				log.warn("Batch update failed with DuplicateKeyException, retrying as single updates for batch", e);
				try {
					cnx.rollback(sp);
				} catch (SQLException e1) {
					throw new IllegalStateException("Failed to rollback after batch update failure", e1);
				}
				var rows = retryAsSingles(sql, records, pss, fallback, cnx);
		        log.warn("Batch update failed with DuplicateKeyException, retried as single updates for batch, total rows updated: {}", rows);
			}
			else {
				log.warn("Batch update failed with DuplicateKeyException, but no active savepoint available to retry as single updates", e);
				throw e;
			}
		} finally {
			releaseConnection(cnx, template.getDataSource());
		}
    }

    private <T> int retryAsSingles(String sql, List<T> records, ParameterizedPreparedStatementSetter<T> pss, Consumer<T> fallback, Connection cnx) {
        if(!supportsSavePoints) {
			throw new IllegalStateException("Cannot retry as single updates because savepoints are not supported");
		}
    	var rows = 0;
        for (var r : records) {
        	Savepoint sp = null;
            try {
                sp = cnx.setSavepoint();
                rows+= template.update(sql, ps -> pss.setValues(ps, r));
                cnx.releaseSavepoint(sp);
            } catch (Exception e) {
                if (nonNull(sp)) {
                    try {
                        cnx.rollback(sp);
                    } catch (SQLException ex) {
    					throw new IllegalStateException("Failed to rollback after single update failure", ex);
                    }
                }
                if (e instanceof DuplicateKeyException) {
                    try {
                        fallback.accept(r); //continue
                    } catch (Exception ex) {
                        log.error("Failed to execute fallback for record " + r, ex);
                    }
                }
                else if (e instanceof RuntimeException re) {
                	throw re;
                }
                else {
                	throw new RuntimeException("Failed to execute single update for record " + r, e);
                }
            }
        }
        return rows;
    }

    static boolean supportsSavePoints(DataSource ds) {
    	if(nonNull(ds)) {
        	try(var cnx = ds.getConnection()){
    			return cnx.getMetaData().supportsSavepoints();
    		} catch (Exception e) {
    			// do nothing
    		}
    	}
    	return false;
    }

}
