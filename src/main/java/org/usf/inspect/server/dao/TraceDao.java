package org.usf.inspect.server.dao;

import static java.sql.Types.BIGINT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.OTHER;
import static java.util.Objects.nonNull;
import static org.springframework.jdbc.datasource.DataSourceUtils.getConnection;
import static org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection;
import static org.usf.inspect.server.JsonUtils.toJson;
import static org.usf.inspect.server.Utils.contentTypeExtract;
import static org.usf.inspect.server.Utils.fromNullableInstant;
import static org.usf.inspect.server.Utils.joinValuesOrNull;
import static org.usf.inspect.server.Utils.userAgentExtract;
import static org.usf.inspect.server.Utils.valueOfNullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.DatabaseRequestSignal;
import org.usf.inspect.DatabaseRequestStage;
import org.usf.inspect.DatabaseRequestUpdate;
import org.usf.inspect.DirectoryRequestSignal;
import org.usf.inspect.DirectoryRequestStage;
import org.usf.inspect.DirectoryRequestUpdate;
import org.usf.inspect.ExceptionTrace;
import org.usf.inspect.FtpRequestSignal;
import org.usf.inspect.FtpRequestStage;
import org.usf.inspect.FtpRequestUpdate;
import org.usf.inspect.HttpRequestSignal;
import org.usf.inspect.HttpRequestStage;
import org.usf.inspect.HttpRequestUpdate;
import org.usf.inspect.HttpSessionSignal;
import org.usf.inspect.HttpSessionStage;
import org.usf.inspect.HttpSessionUpdate;
import org.usf.inspect.InstanceEnvironment;
import org.usf.inspect.LocalRequestSignal;
import org.usf.inspect.LocalRequestUpdate;
import org.usf.inspect.MailRequestSignal;
import org.usf.inspect.MailRequestStage;
import org.usf.inspect.MailRequestUpdate;
import org.usf.inspect.MainSessionSignal;
import org.usf.inspect.MainSessionUpdate;
import org.usf.inspect.ReportEvent;
import org.usf.inspect.SessionAsyncDurationUpdate;
import org.usf.inspect.SessionEvent;
import org.usf.inspect.SessionMaskUpdate;
import org.usf.inspect.SystemMetrics;
import org.usf.inspect.server.dto.BrowserConfigDto;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.Pair;
import org.usf.inspect.server.model.TraceBatch;

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
    private final boolean supportsSavePoints;

	public TraceDao(JdbcTemplate template) {
		this.template = template;
		this.supportsSavePoints = supportsSavePoints(template.getDataSource());
	}

    public void saveInstanceEnvironment(InstanceEnvironment instance) {
        template.update("""
insert into e_env_ins(id_ins,va_typ,dh_str,va_app,va_vrs,va_adr,va_env,va_os,va_re,va_usr,va_clr,va_brch,va_hsh,va_cnf,va_rsr,va_add_prp,cd_nsp)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", ps -> {
            var idx=0;
            ps.setObject(++idx, instance.getId());
            ps.setString(++idx, toStringOrNull(instance.getType()));
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
            ps.setObject(++idx, toJson(instance.getConfiguration()), OTHER);
            ps.setObject(++idx, toJson(instance.getResource()), OTHER);
            ps.setObject(++idx, toJson(instance.getAdditionalProperties()), OTHER);
            ps.setString(++idx, instance.getNamespace());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateInstanceEnvironments(List<InstanceEnvironmentUpdate> updates){
        batchUpdateWithRetry("UPDATE e_env_ins set dh_end=? where id_ins=? AND dh_end is NULL", updates, (ps, ins) -> {
            var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(ins.getEnd()));
            ps.setObject(++idx, ins.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveTracePackets(List<TraceBatch> packets) {
        batchUpdateWithRetry("INSERT INTO e_ins_trc(va_pnd,va_atp,va_seq,va_trc_cnt,dh_str,cd_ins) VALUES(?,?,?,?,?,?)", packets, (ps, pck) -> {
            var idx=0;
            ps.setObject(++idx, pck.getPending(), INTEGER);
            ps.setObject(++idx, pck.getAttempts(), INTEGER);
            ps.setObject(++idx, pck.getSequence(), INTEGER);
            ps.setInt(++idx, pck.getTraceCount());
            ps.setTimestamp(++idx, fromNullableInstant(pck.getInstant()));
            ps.setObject(++idx, pck.getInstanceId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLogEntries(List<ReportEvent> logEntries) {
        batchUpdateWithRetry("INSERT INTO e_rpt_evt(va_msg,va_err_typ,va_stk,dh_str,va_thr,va_act,cd_ins) VALUES(?,?,?,?,?,?,?)", logEntries, (ps, lg)-> {
        	var idx=0;
            ps.setString(++idx, lg.getMessage());
            ps.setString(++idx, lg.getException());
            ps.setObject(++idx, toJson(lg.getStackRows()), OTHER);
            ps.setTimestamp(++idx, fromNullableInstant(lg.getInstant()));
            ps.setString(++idx, lg.getThread());
            ps.setString(++idx, lg.getAction());
            ps.setObject(++idx, lg.getInstanceId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMachineResourceUsages(List<SystemMetrics> usages) {
        batchUpdateWithRetry("INSERT INTO e_rsc_usg(dh_str,va_usd_hep,va_cmt_hep,va_usd_dsk,nb_act_thr,nb_str_thr,va_cpu_usg,cd_ins) VALUES(?,?,?,?,?,?,?,?)", usages, (ps, usg)-> {
        	var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(usg.getInstant()));
            ps.setInt(++idx, usg.getUsedHeap());
            ps.setInt(++idx, usg.getCommitedHeap());
            ps.setInt(++idx, usg.getUsedDiskSpace());
            ps.setInt(++idx, usg.getActiveThreadCount());
            ps.setInt(++idx, usg.getStartedThreadCount());
            ps.setShort(++idx, usg.getCpuUsage());
            ps.setObject(++idx, usg.getInstanceId());
        });
    }

    // New version
    @Transactional(rollbackFor = Throwable.class)
    public void saveRestSessionSignals(List<HttpSessionSignal> signals) {
        batchUpdateWithRetry("""
INSERT INTO e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_i_sze,va_i_cnt_enc,va_thr,va_lnk,dh_str,va_usr_agt,va_fwd_add,va_nam,va_usr)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", signals, (ps, sgn) -> {
            var idx = restSessionSignalSetter(ps, sgn);
            ps.setString(++idx, sgn.getName());
            ps.setString(++idx, sgn.getUser());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestSessions(List<Pair<HttpSessionSignal, HttpSessionUpdate>> session) {
    	batchUpdateWithRetry("""
INSERT INTO e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_i_sze,va_i_cnt_enc,va_thr,va_lnk,dh_str,va_usr_agt,va_fwd_add,dh_end,va_nam,va_usr,va_cch_ctr,va_cnt_typ,cd_stt,va_o_sze,va_o_cnt_enc,va_msk,va_asy_drt)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", session, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
            var idx = restSessionSignalSetter(ps, sgn);
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, nonNull(upd.getName()) ? upd.getName() : sgn.getName());
            ps.setString(++idx, nonNull(upd.getUser()) ? upd.getUser() : sgn.getUser());
            ps.setString(++idx, upd.getCacheControl());
            ps.setString(++idx, contentTypeExtract(upd.getContentType()));
            ps.setShort(++idx, upd.getStatus());
            ps.setLong(++idx, upd.getDataSize());
            ps.setString(++idx, upd.getContentEncoding());
            ps.setInt(++idx, upd.getEventMask());
            ps.setObject(++idx, upd.getAsyncDuration(), BIGINT);
        });
    }

    static int restSessionSignalSetter(PreparedStatement ps, HttpSessionSignal sgn) throws SQLException {
        var idx=0;
        ps.setObject(++idx, sgn.getId());
        ps.setObject(++idx, sgn.getInstanceId());
        ps.setString(++idx, sgn.getMethod());
        ps.setString(++idx, sgn.getProtocol());
        ps.setString(++idx, sgn.getHost());
        ps.setInt(++idx, sgn.getPort());
        ps.setString(++idx, sgn.getPath());
        ps.setString(++idx, sgn.getQuery());
        ps.setString(++idx, sgn.getAuthScheme());
        ps.setLong(++idx, sgn.getDataSize());
        ps.setString(++idx, sgn.getContentEncoding());
        ps.setString(++idx, sgn.getThreadName());
        ps.setBoolean(++idx, sgn.isLinked());
        ps.setTimestamp(++idx, fromNullableInstant(sgn.getStart()));
        ps.setString(++idx, userAgentExtract(sgn.getUserAgent()));
        ps.setString(++idx, joinValuesOrNull(sgn.getForwardedAddresses()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateRestSessions(List<HttpSessionUpdate> updates) {
        batchUpdateWithRetry("""
UPDATE e_rst_ses SET va_nam=coalesce(?, va_nam), va_usr=coalesce(?, va_usr), va_cch_ctr=coalesce(?, va_cch_ctr), va_cnt_typ=?, cd_stt=?, va_o_sze=?, va_o_cnt_enc=?, dh_end=?, va_msk=?, va_asy_drt=?
WHERE id_ses=? AND dh_end IS NULL""", updates, (ps, upd) -> {
            var idx = 0;
            ps.setString(++idx, upd.getName());
            ps.setString(++idx, upd.getUser());
            ps.setString(++idx, upd.getCacheControl());
            ps.setString(++idx, contentTypeExtract(upd.getContentType()));
            ps.setShort(++idx, upd.getStatus());
            ps.setLong(++idx, upd.getDataSize());
            ps.setString(++idx, upd.getContentEncoding());
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setInt(++idx, upd.getEventMask());
            ps.setObject(++idx, upd.getAsyncDuration(), BIGINT);
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMaskRestSessions(List<SessionMaskUpdate> updates) {
        batchUpdateWithRetry("UPDATE e_rst_ses SET va_msk=? WHERE id_ses=?", updates, (ps, upd) -> { //TODO : va_msk&?<>? H2 compatibility
            ps.setInt(1, upd.getMask());
            ps.setObject(2, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMainSessionSignals(List<MainSessionSignal> signals) {
        batchUpdateWithRetry("INSERT INTO e_main_ses(id_ses,cd_ins,va_typ,va_thr,va_lct,va_nam,va_usr,dh_str) VALUES(?,?,?,?,?,?,?,?)", signals, (ps, sgn) -> {
            var idx = mainSessionSignalSetter(ps, sgn);
            ps.setString(++idx, sgn.getLocation());
            ps.setString(++idx, sgn.getName());
            ps.setString(++idx, sgn.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(sgn.getStart()));
        });
    }
    
    @Transactional(rollbackFor = Throwable.class)
    public void saveMainSessions(List<Pair<MainSessionSignal, MainSessionUpdate>> sessions) {
    	batchUpdateWithRetry("INSERT INTO e_main_ses(id_ses,cd_ins,va_typ,va_thr,va_lct,va_nam,va_usr,dh_str,dh_end,va_msk,cd_stt,va_asy_drt) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", sessions, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
            var idx = mainSessionSignalSetter(ps, sgn);
            ps.setString(++idx, nonNull(upd.getLocation()) ? upd.getLocation() : sgn.getLocation());
            ps.setString(++idx, nonNull(upd.getName()) ? upd.getName() : sgn.getName());
            ps.setString(++idx, nonNull(upd.getUser()) ? upd.getUser() : sgn.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(nonNull(upd.getStart()) ? upd.getStart() : sgn.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setInt(++idx, upd.getEventMask());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getAsyncDuration(), BIGINT);
        });
    }

    static int mainSessionSignalSetter(PreparedStatement ps, MainSessionSignal sgn) throws SQLException {
    	var idx=0;
        ps.setObject(++idx, sgn.getId());
        ps.setObject(++idx, sgn.getInstanceId());
        ps.setString(++idx, valueOfNullable(sgn.getType()));
        ps.setString(++idx, sgn.getThreadName());
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMainSessions(List<MainSessionUpdate> updates) {
        batchUpdateWithRetry("""
UPDATE e_main_ses SET va_lct=coalesce(?, va_lct), va_nam=coalesce(?, va_nam), va_usr=coalesce(?, va_usr), dh_str=coalesce(?, dh_str), dh_end=?, va_msk=?, cd_stt=?, va_asy_drt=?
WHERE id_ses=? AND dh_end IS NULL""", updates, (ps, upd) -> {
            var idx = 0;
            ps.setString(++idx, upd.getLocation());
            ps.setString(++idx, upd.getName());
            ps.setString(++idx, upd.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(upd.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setInt(++idx, upd.getEventMask());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getAsyncDuration(), BIGINT);
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMaskMainSessions(List<SessionMaskUpdate> updates) {
        batchUpdateWithRetry("UPDATE e_main_ses SET va_msk=? WHERE id_ses=? ", updates, (ps, upd) -> { //TODO : va_msk&?<>? H2 compatibility
            var idx = 0;
            ps.setInt(++idx, upd.getMask());
            ps.setObject(++idx, upd.getId());
        });
    }
    
    @Transactional(rollbackFor = Throwable.class)
    public void updateMainSessionAsyncDuration(List<SessionAsyncDurationUpdate> updates) {
        batchUpdateWithRetry("UPDATE e_main_ses SET va_asy_drt=? WHERE id_ses=? AND va_asy_drt=-1", updates, (ps, upd) -> {
            ps.setLong(1, upd.getDuration());
            ps.setObject(2, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateRestSessionAsyncDuration(List<SessionAsyncDurationUpdate> updates) {
        batchUpdateWithRetry("UPDATE e_rst_ses SET va_asy_drt=? WHERE id_ses=? AND va_asy_drt=-1", updates, (ps, upd) -> {
            ps.setLong(1, upd.getDuration());
            ps.setObject(2, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestRequestSignals(List<HttpRequestSignal> signals) {
        batchUpdateWithRetry("""
INSERT INTO e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_usr,dh_str)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", signals, TraceDao::restRequestSignalSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestRequests(List<Pair<HttpRequestSignal, HttpRequestUpdate>> requests) {
    	batchUpdateWithRetry("""
insert into e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_usr,dh_str,dh_end,va_cnt_typ,cd_stt,va_i_sze,va_i_cnt_enc,va_bdy_cnt,va_lnk)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
            var idx = restRequestSignalSetter(ps, sgn);
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, contentTypeExtract(upd.getContentType()));
            ps.setShort(++idx, upd.getStatus());
            ps.setLong(++idx, upd.getDataSize());
            ps.setString(++idx, upd.getContentEncoding());
            ps.setString(++idx, upd.getBodyContent());
            ps.setBoolean(++idx, upd.isLinked());
        });
    }

    static int restRequestSignalSetter(PreparedStatement ps, HttpRequestSignal sgn) throws SQLException {
        var idx = 0;
        ps.setObject(++idx, sgn.getId());
        ps.setObject(++idx, sgn.getSessionId());
        ps.setObject(++idx, sgn.getInstanceId());
        ps.setString(++idx, sgn.getMethod());
        ps.setString(++idx, sgn.getProtocol());
        ps.setString(++idx, sgn.getHost());
        ps.setInt(++idx, sgn.getPort());
        ps.setString(++idx, sgn.getPath());
        ps.setString(++idx, sgn.getQuery());
        ps.setString(++idx, sgn.getAuthScheme());
        ps.setLong(++idx, sgn.getDataSize());
        ps.setString(++idx, sgn.getContentEncoding());
        ps.setString(++idx, sgn.getThreadName());
        ps.setString(++idx, sgn.getUser());
        ps.setTimestamp(++idx, fromNullableInstant(sgn.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateRestRequests(List<HttpRequestUpdate> updates) {
        batchUpdateWithRetry("UPDATE e_rst_rqt SET va_cnt_typ=?, cd_stt=?, va_i_sze=?, va_i_cnt_enc=?, dh_end=?, va_bdy_cnt=?, va_lnk=? WHERE id_rst_rqt=? AND dh_end IS NULL", updates, (ps, upd) -> {
            var idx = 0;
            ps.setString(++idx, contentTypeExtract(upd.getContentType()));
            ps.setShort(++idx, upd.getStatus());
            ps.setLong(++idx, upd.getDataSize());
            ps.setString(++idx, upd.getContentEncoding());
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getBodyContent());
            ps.setBoolean(++idx, upd.isLinked());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLocalRequestSignals(List<LocalRequestSignal> signals) {
        batchUpdateWithRetry("INSERT INTO e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str) VALUES(?,?,?,?,?,?,?,?,?)", signals, (ps, req) -> {
            var idx = localRequestSignalSetter(ps, req);
            ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLocalRequests(List<Pair<LocalRequestSignal, LocalRequestUpdate>> requests) {
    	batchUpdateWithRetry("""
INSERT INTO e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str,dh_end,cd_stt)
VALUES(?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
          var idx=  localRequestSignalSetter(ps, sgn);
            ps.setTimestamp(++idx, fromNullableInstant(nonNull(upd.getStart()) ? upd.getStart() : sgn.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setShort(++idx, upd.getStatus());
        });
    }

    static int localRequestSignalSetter(PreparedStatement ps, LocalRequestSignal sgn) throws SQLException {
        var idx=0;
        ps.setObject(++idx, sgn.getId());
        ps.setObject(++idx, sgn.getSessionId());
        ps.setObject(++idx, sgn.getInstanceId()); //instance id
        ps.setString(++idx, sgn.getType());
        ps.setString(++idx, sgn.getName());
        ps.setString(++idx, sgn.getLocation());
        ps.setString(++idx, sgn.getUser());
        ps.setString(++idx, sgn.getThreadName());
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateLocalRequests(List<LocalRequestUpdate> requests) {
        batchUpdateWithRetry("UPDATE e_lcl_rqt SET dh_str=coalesce(?, dh_str), dh_end=?, cd_stt=? WHERE id_lcl_rqt=? AND dh_end IS NULL", requests, (ps, upd) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequestSignals(List<MailRequestSignal> signals) {
        batchUpdateWithRetry("""
INSERT INTO e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
VALUES(?,?,?,?,?,?,?,?,?)""", signals, TraceDao::mailRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequests(List<Pair<MailRequestSignal, MailRequestUpdate>> requests) {
    	batchUpdateWithRetry("""
INSERT INTO e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
            var idx=mailRequestSetter(ps, sgn);
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
        });
    }

    static int mailRequestSetter(PreparedStatement ps, MailRequestSignal sgn) throws SQLException {
        var idx = 0;
        ps.setObject(++idx, sgn.getId());
        ps.setObject(++idx, sgn.getSessionId());
        ps.setObject(++idx, sgn.getInstanceId()); //instance id
        ps.setString(++idx, sgn.getHost());
        ps.setInt(++idx, sgn.getPort());
        ps.setString(++idx, sgn.getProtocol());
        ps.setString(++idx, sgn.getUser());
        ps.setString(++idx, sgn.getThreadName());
        ps.setTimestamp(++idx, fromNullableInstant(sgn.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMailRequests(List<MailRequestUpdate> updates) {
        batchUpdateWithRetry("UPDATE e_smtp_rqt SET dh_end=?, va_cmd=?, cd_stt=? WHERE id_smtp_rqt=? AND dh_end IS NULL", updates, (ps, upd) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequestSignals(List<FtpRequestSignal> signals) {
        batchUpdateWithRetry("""
INSERT INTO e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str)
VALUES(?,?,?,?,?,?,?,?,?,?,?)""", signals, TraceDao::ftpRequestSignalSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequests(List<Pair<FtpRequestSignal, FtpRequestUpdate>> requests) {
    	batchUpdateWithRetry("""
INSERT INTO e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
            var idx = ftpRequestSignalSetter(ps, sgn);
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
        });
    }

    static int ftpRequestSignalSetter(PreparedStatement ps, FtpRequestSignal sgn) throws SQLException {
      var idx = 0;
        ps.setObject(++idx, sgn.getId());
        ps.setObject(++idx, sgn.getSessionId());
        ps.setObject(++idx, sgn.getInstanceId());
        ps.setString(++idx, sgn.getHost());
        ps.setInt(++idx, sgn.getPort());
        ps.setString(++idx, sgn.getProtocol());
        ps.setString(++idx, sgn.getServerVersion());
        ps.setString(++idx, sgn.getClientVersion());
        ps.setString(++idx, sgn.getUser());
        ps.setString(++idx, sgn.getThreadName());
        ps.setTimestamp(++idx, fromNullableInstant(sgn.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateFtpRequests(List<FtpRequestUpdate> updates) {
    	batchUpdateWithRetry("UPDATE e_ftp_rqt SET dh_end=?, va_cmd=?, cd_stt=? WHERE id_ftp_rqt=? AND dh_end IS NULL", updates, (ps, upd) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequestSignals(List<DirectoryRequestSignal> signals) {
    	batchUpdateWithRetry("""
INSERT INTO e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
VALUES(?,?,?,?,?,?,?,?,?)""", signals, TraceDao::ldapRequestSignalSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequests(List<Pair<DirectoryRequestSignal, DirectoryRequestUpdate>> requests) {
    	batchUpdateWithRetry("""
INSERT INTO e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
            var idx = ldapRequestSignalSetter(ps, sgn);
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
        });
    }

    static int ldapRequestSignalSetter(PreparedStatement ps, DirectoryRequestSignal sgn) throws SQLException {
    	var idx = 0;
        ps.setObject(++idx, sgn.getId());
        ps.setObject(++idx, sgn.getSessionId());
        ps.setObject(++idx, sgn.getInstanceId());
        ps.setString(++idx, sgn.getHost());
        ps.setInt(++idx, sgn.getPort());
        ps.setString(++idx, sgn.getProtocol());
        ps.setString(++idx, sgn.getUser());
        ps.setString(++idx, sgn.getThreadName());
        ps.setTimestamp(++idx, fromNullableInstant(sgn.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateLdapRequests(List<DirectoryRequestUpdate> updates) {
    	batchUpdateWithRetry("UPDATE e_ldap_rqt SET dh_end=?, va_cmd=?, cd_stt=? WHERE id_ldap_rqt=? AND dh_end IS NULL", updates, (ps, upd) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequestSignals(List<DatabaseRequestSignal> signals) {
    	batchUpdateWithRetry("""
INSERT INTO e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", signals, TraceDao::databaseRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequests(List<Pair<DatabaseRequestSignal, DatabaseRequestUpdate>> requests) {
    	batchUpdateWithRetry("""
INSERT INTO e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str,dh_end,va_cmd,cd_stt)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
            var sgn = pair.signal();
            var upd = pair.update();
            var idx = databaseRequestSetter(ps, sgn);
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
        });
    }

    static int databaseRequestSetter(PreparedStatement ps, DatabaseRequestSignal sgn) throws SQLException {
        var idx = 0;
        ps.setObject(++idx, sgn.getId());
        ps.setObject(++idx, sgn.getSessionId());
        ps.setObject(++idx, sgn.getInstanceId());
        ps.setString(++idx, sgn.getHost());
        ps.setInt(++idx, sgn.getPort());
        ps.setString(++idx, sgn.getSchema());
        ps.setString(++idx, sgn.getName());
        ps.setString(++idx, sgn.getSchema());//FIXME called twice
        ps.setString(++idx, sgn.getUser());
        ps.setString(++idx, sgn.getThreadName());
        ps.setString(++idx, sgn.getDriverVersion());
        ps.setString(++idx, sgn.getProductName());
        ps.setString(++idx, sgn.getProductVersion());
        ps.setTimestamp(++idx, fromNullableInstant(sgn.getStart()));
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateDatabaseRequests(List<DatabaseRequestUpdate> updates) {
        batchUpdateWithRetry("UPDATE e_dtb_rqt SET dh_end=?, va_cmd=?, cd_stt=? WHERE id_dtb_rqt=? AND dh_end IS NULL", updates, (ps, upd) -> {
           var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpRequestStages(List<HttpRequestStage> stages) {
        batchUpdateWithRetry("INSERT INTO e_rst_rqt_stg(va_nam,dh_str,dh_end,cd_ord,cd_rst_rqt) VALUES(?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpSessionStages(List<HttpSessionStage> stages) {
        batchUpdateWithRetry("INSERT INTO e_rst_ses_stg(va_nam,dh_str,dh_end,cd_ord,cd_prn_ses) VALUES(?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequestStages(List<FtpRequestStage> stages) {
        batchUpdateWithRetry("INSERT INTO e_ftp_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_ftp_rqt,va_pld) VALUES(?,?,?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, stg.getCommand());
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
            ps.setObject(++idx, toJson(stg.getPayload()), OTHER);
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequestStages(List<DirectoryRequestStage> stages) {
        batchUpdateWithRetry("INSERT INTO e_ldap_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_ldap_rqt,va_pld) VALUES(?,?,?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, stg.getCommand());
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
            ps.setObject(++idx, toJson(stg.getPayload()), OTHER);
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequestStages(List<MailRequestStage> stages) {
        batchUpdateWithRetry("INSERT INTO e_smtp_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_smtp_rqt) VALUES(?,?,?,?,?,?)", stages, (ps, stg)-> {
        	var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, stg.getCommand());
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
        });
        saveMailRequestMails(stages);
    }

    void saveMailRequestMails(List<MailRequestStage> stages) {
    	var mails = stages.stream().filter(m -> nonNull(m.getMail())).toList();
        batchUpdateWithRetry("INSERT INTO e_smtp_mail(va_sbj,va_cnt_typ,va_frm,va_rcp,va_rpl,va_sze,cd_smtp_rqt) VALUES(?,?,?,?,?,?,?)", mails, (ps, stg)-> {
        	var idx=0;
            var mail = stg.getMail();
            ps.setString(++idx, mail.getSubject());
            ps.setString(++idx, mail.getContentType());
            ps.setString(++idx, joinValuesOrNull(mail.getFrom()));
            ps.setString(++idx, joinValuesOrNull(mail.getRecipients()));
            ps.setString(++idx, joinValuesOrNull(mail.getReplyTo()));
            ps.setInt(++idx, mail.getSize());
            ps.setObject(++idx, stg.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequestStages(List<DatabaseRequestStage> stages) {
        batchUpdateWithRetry("INSERT INTO e_dtb_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_dtb_rqt,va_pld) VALUES(?,?,?,?,?,?,?)", stages, (ps, stg)-> {
            var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, stg.getCommand());
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
            ps.setObject(++idx, toJson(stg.getPayload()), OTHER);
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveExceptionTraces(List<ExceptionTrace> exceptions) {
        batchUpdateWithRetry("INSERT INTO e_exc_inf(va_err_typ,va_err_msg,va_stk,va_cas,cd_ord,cd_trc,va_trc_typ) VALUES(?,?,?,?,?,?,?)", exceptions, (ps, exp) -> {
            var idx=0;
            ps.setString(++idx, exp.getType());
            ps.setString(++idx, exp.getMessage());
            ps.setObject(++idx, toJson(exp.getStackTraceRows()), OTHER);
            ps.setObject(++idx, toJson(exp.getCause()), OTHER);
            ps.setLong(++idx, exp.getOffset());
            ps.setObject(++idx, exp.getTraceId());
            ps.setByte(++idx, exp.getTraceType());
        });
    }
    
    @Transactional(rollbackFor = Throwable.class)
    public void saveSessionEvents(List<SessionEvent> exceptions) {
        batchUpdateWithRetry("INSERT INTO e_ses_evt(dh_str,va_typ,va_cnt,va_scp,va_lct,cd_prn_ses,cd_ins) VALUES(?,?,?,?,?,?,?)", exceptions, (ps, exp) -> {
            var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(exp.getInstant()));
            ps.setString(++idx, exp.getType());
            ps.setString(++idx, exp.getValue());
            ps.setString(++idx, exp.getScope());
            ps.setString(++idx, exp.getLocation());
            ps.setObject(++idx, exp.getSessionId(), OTHER);
            ps.setObject(++idx, exp.getInstanceId(), OTHER);
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveBrowserConfigs(List<BrowserConfigDto> configs) {
        batchUpdateWithRetry("INSERT INTO o_brw_cfg(va_dvc_dsp_rsl,va_dvc_orn,va_dvc_cnt, va_sav_dta, va_wdw_vpt_bds, va_wdw_zom_lvl, va_usr_lng,va_usr_thm,cd_prn_ses) VALUES(?,?,?,?,?,?,?,?,?)", configs, (ps, cfg) -> {
            var idx = 0;
            ps.setString(++idx, cfg.getDeviceDisplayResolution());
            ps.setString(++idx, cfg.getDeviceOrientation());
            ps.setString(++idx, cfg.getDeviceConnectivity());
            ps.setObject(++idx, cfg.getSaveData());
            ps.setString(++idx, cfg.getWindowViewportBounds());
            ps.setString(++idx, cfg.getWindowZoomLevel());
            ps.setString(++idx, cfg.getUserLanguage());
            ps.setString(++idx, cfg.getUserTheme());
            ps.setObject(++idx, cfg.getSessionId());
        });
    }


    private <T> void batchUpdateWithRetry(String sql, List<T> records, ParameterizedPreparedStatementSetter<T> pss) {
    	if(nonNull(records) && !records.isEmpty()) {
	       var result = updateAll(sql, records, pss);
	        var idx=0;
	        for (var batch : result) {
	            for (var updatedRows : batch) {
	                if(updatedRows == 0) {
	                    log.warn("TraceDao.batchUpdateWithRetry: Record {} was not updated, sql={}", records.get(idx), sql);
	                }
	                idx++;
	            }
	        }
	        if(records.size() != idx) {
	        	log.warn("TraceDao.batchUpdateWithRetry: Number of records processed ({}) does not match number of records provided ({}), sql={}", idx, records.size(), sql);
	        }
    	}
    }

    private <T> int[][] updateAll(String sql, List<T> records, ParameterizedPreparedStatementSetter<T> pss) {
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
            var results = template.batchUpdate(sql, records, BATCH_SIZE, pss);
			if(nonNull(sp)) {
				try {
					cnx.releaseSavepoint(sp);
				}
				catch (SQLException e) {
					log.warn("Failed to release savepoint after batch update", e);
				}
			}
            return results;
		} catch (DuplicateKeyException e) { //SQLState 23505
			if(nonNull(sp)) {
				log.warn("Batch update failed with DuplicateKeyException, retrying as single updates for batch", e);
				try {
					cnx.rollback(sp);
                    log.warn("Rolling back to savepoint after batch update failure");
				} catch (SQLException e1) {
					throw new IllegalStateException("Failed to rollback after batch update failure", e1);
				}
				var rows = retryAsSingles(sql, records, pss, cnx);
		        log.warn("Batch update failed with DuplicateKeyException, retried as single updates for batch, total rows updated: {}", rows);
                return new int[][] { rows };
            }
			else {
				log.warn("Batch update failed with DuplicateKeyException, but no active savepoint available to retry as single updates", e);
				throw e;
			}
		} finally {
			releaseConnection(cnx, template.getDataSource());
		}
    }

    private <T> int[] retryAsSingles(String sql, List<T> records, ParameterizedPreparedStatementSetter<T> pss, Connection cnx) {
        if(supportsSavePoints) {
        	var rows = new int[records.size()];
            for(int i = 0; i < records.size(); i++) {
            	var r = records.get(i);
            	Savepoint sp = null;
                try {
                    sp = cnx.setSavepoint();
                    rows[i] = template.update(sql, ps-> pss.setValues(ps, r));
                    cnx.releaseSavepoint(sp);
                } catch (DuplicateKeyException e) { //cannot be setSavepoint, so we cannot retry as single updates
                    try {
                        cnx.rollback(sp);
                        rows[i] = 0;
                    } catch (SQLException ex) {
    					throw new IllegalStateException("Failed to rollback after single update failure", ex);
                    }
                }
                catch (RuntimeException e) {
    				throw e;
    			}
                catch (Exception e) {
                	throw new RuntimeException("Failed to execute single update for record " + r, e);
    			}
            }
            return rows;
        }
        throw new UnsupportedOperationException("Cannot retry as single updates because savepoints are not supported");
    }

    static boolean supportsSavePoints(DataSource ds) {
    	if(nonNull(ds)) {
        	try(var cnx = ds.getConnection()){
    			return cnx.getMetaData().supportsSavepoints();
    		} catch (Exception e) {
    			log.warn("TraceDao.supportsSavePoints: Failed to check if savepoints are supported", e);
    		}
    	}
    	return false;
    }
    
    static String toStringOrNull(Enum<?> e) {
    	return nonNull(e) ? e.name() : null;
	}
}
