package org.usf.inspect.server.dao;

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
import static org.usf.jquery.core.Utils.isEmpty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.DatabaseRequestSignal;
import org.usf.inspect.core.DatabaseRequestStage;
import org.usf.inspect.core.DatabaseRequestUpdate;
import org.usf.inspect.core.DirectoryRequestSignal;
import org.usf.inspect.core.DirectoryRequestStage;
import org.usf.inspect.core.DirectoryRequestUpdate;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.core.ExceptionTrace;
import org.usf.inspect.core.FtpRequestSignal;
import org.usf.inspect.core.FtpRequestStage;
import org.usf.inspect.core.FtpRequestUpdate;
import org.usf.inspect.core.HttpRequestSignal;
import org.usf.inspect.core.HttpRequestStage;
import org.usf.inspect.core.HttpRequestUpdate;
import org.usf.inspect.core.HttpSessionSignal;
import org.usf.inspect.core.HttpSessionStage;
import org.usf.inspect.core.HttpSessionUpdate;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.LocalRequestSignal;
import org.usf.inspect.core.LocalRequestUpdate;
import org.usf.inspect.core.LogEntry;
import org.usf.inspect.core.MachineResourceUsage;
import org.usf.inspect.core.MailRequestSignal;
import org.usf.inspect.core.MailRequestStage;
import org.usf.inspect.core.MailRequestUpdate;
import org.usf.inspect.core.MainSessionSignal;
import org.usf.inspect.core.MainSessionUpdate;
import org.usf.inspect.core.SessionEvent;
import org.usf.inspect.core.SessionMaskUpdate;
import org.usf.inspect.server.event.UnsavedEventTraceEvent;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.Pair;
import org.usf.inspect.server.model.TracePacket;

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
    private final ApplicationEventPublisher publisher;
    private final boolean supportsSavePoints;

	public TraceDao(JdbcTemplate template, ApplicationEventPublisher publisher) {
		this.template = template;
		this.publisher = publisher;
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
        executeBatch("update e_env_ins set dh_end=? where id_ins=?", updates, (ps, ins) -> {
            var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(ins.getEnd()));
            ps.setObject(++idx, ins.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveTracePackets(List<TracePacket> packets) {
        executeBatch("insert into e_ins_trc(va_pnd,va_atp,va_seq,va_trc_cnt,dh_str,cd_ins) values(?,?,?,?,?,?)", packets, (ps, pck) -> {
                    var idx=0;
                    ps.setObject(++idx, pck.getPending(), INTEGER);
                    ps.setObject(++idx, pck.getAttempts(), INTEGER);
                    ps.setObject(++idx, pck.getSequence(), INTEGER);
                    ps.setInt(++idx, pck.getTraceCount());
                    ps.setTimestamp(++idx, fromNullableInstant(pck.getInstant()));
                    //TODO  delete filename column
                    ps.setObject(++idx, pck.getInstanceId());
                });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLogEntries(List<LogEntry> logEntries) {
        executeBatch("insert into e_log_ent(va_lvl,va_msg,va_stk,dh_str,cd_prn_ses,cd_ins) values(?,?,?,?,?,?)", logEntries, (ps, lg)-> {
                    var idx=0;
                    ps.setString(++idx, toStringOrNull(lg.getLevel()));
                    ps.setString(++idx, lg.getMessage());
                    ps.setObject(++idx, toJson(lg.getStackRows()), OTHER);
                    ps.setTimestamp(++idx, fromNullableInstant(lg.getInstant()));
                    ps.setObject(++idx, lg.getSessionId());
                    ps.setObject(++idx, lg.getInstanceId());
                });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMachineResourceUsages(List<MachineResourceUsage> usages) {
        executeBatch("insert into e_rsc_usg(dh_str,va_usd_hep,va_cmt_hep,va_usd_dsk,nb_act_thr,nb_str_thr,va_cpu_usg,cd_ins) values(?,?,?,?,?,?,?,?)", usages, (ps, usg)-> {
            var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(usg.getInstant()));
            ps.setInt(++idx, usg.getUsedHeap());
            ps.setInt(++idx, usg.getCommitedHeap());
            ps.setInt(++idx, usg.getUsedDiskSpace());
            ps.setInt(++idx, usg.getActiveThreadCount());  //TODO create column nb_act_thr
            ps.setInt(++idx, usg.getStartedThreadCount()); //TODO create column nb_str_thr
            ps.setByte(++idx, usg.getCpuUsage()); 		   //TODO create column va_cpu_usg
            ps.setObject(++idx, usg.getInstanceId());
        });
    }

    // New version
    @Transactional(rollbackFor = Throwable.class)
    public void saveRestSessionSignals(List<HttpSessionSignal> signals) {
        executeBatch("""
insert into e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_lnk,dh_str,va_nam,va_usr,va_usr_agt,va_msk,va_fwd_add)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", signals, (ps, sgn) -> {
            var idx = restSessionSginalSetter(ps, sgn);
            ps.setString(++idx, sgn.getName());
            ps.setString(++idx, sgn.getUser());
            ps.setString(++idx, userAgentExtract(sgn.getUserAgent())); //TODO -> restSessionSginalSetter
            ps.setInt(++idx, 0); //Unnecessary 
            ps.setString(++idx, joinValuesOrNull(sgn.getForwardedAddresses())); //TODO -> restSessionSginalSetter
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestSessions(List<Pair<HttpSessionSignal, HttpSessionUpdate>> session) {
    	executeBatchPair("""
insert into e_rst_ses(id_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_i_sze,va_i_cnt_enc,va_thr,va_lnk,dh_str,dh_end,va_nam,va_usr,va_usr_agt,va_cch_ctr,va_cnt_typ,cd_stt,va_o_sze,va_o_cnt_enc,va_msk,va_fwd_add)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", session, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
            var idx = restSessionSginalSetter(ps, sgn);
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, nonNull(upd.getName()) ? upd.getName() : sgn.getName());
            ps.setString(++idx, nonNull(upd.getUser()) ? upd.getUser() : sgn.getUser());
            ps.setString(++idx, userAgentExtract(sgn.getUserAgent()));  //TODO -> restSessionSginalSetter
            ps.setString(++idx, upd.getCacheControl());
            ps.setString(++idx, contentTypeExtract(upd.getContentType()));
            ps.setShort(++idx, upd.getStatus());
            ps.setLong(++idx, upd.getDataSize());
            ps.setString(++idx, upd.getContentEncoding());
            ps.setInt(++idx, upd.getRequestMask().get());
            ps.setString(++idx, joinValuesOrNull(sgn.getForwardedAddresses())); //TODO -> restSessionSginalSetter
        });
    }

    static int restSessionSginalSetter(PreparedStatement ps, HttpSessionSignal sgn) throws SQLException {
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
        return idx;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateRestSessions(List<HttpSessionUpdate> updates) {
        executeBatch("""
update e_rst_ses set va_nam=coalesce(?, va_nam), va_usr=coalesce(?, va_usr), va_cch_ctr=coalesce(?, va_cch_ctr), va_cnt_typ=?, cd_stt=?, va_o_sze=?, va_o_cnt_enc=?, dh_end=?, va_msk =?
where id_ses=?""", updates, (ps, upd) -> {
            var idx = 0;
            ps.setString(++idx, upd.getName());
            ps.setString(++idx, upd.getUser());
            ps.setString(++idx, upd.getCacheControl());
            ps.setString(++idx, contentTypeExtract(upd.getContentType()));
            ps.setShort(++idx, upd.getStatus());
            ps.setLong(++idx, upd.getDataSize());
            ps.setString(++idx, upd.getContentEncoding());
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setInt(++idx, upd.getRequestMask().get());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMaskRestSessions(List<SessionMaskUpdate> updates) {
        executeBatch("update e_rst_ses set va_msk=? where id_ses=?", updates, (ps, upd) -> {
            ps.setInt(1, upd.getMask());
            ps.setObject(2, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMainSessionSignals(List<MainSessionSignal> signals) {
        executeBatch("insert into e_main_ses(id_ses,cd_ins,va_typ,va_thr,va_lct,va_nam,va_usr,dh_str) values(?,?,?,?,?,?,?,?)", signals, (ps, sgn) -> {
            var idx = mainSessionSignalSetter(ps, sgn);
            ps.setString(++idx, sgn.getLocation());
            ps.setString(++idx, sgn.getName());
            ps.setString(++idx, sgn.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(sgn.getStart()));
          //  ps.setInt(++idx, 0); //Unnecessary
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMainSessions(List<Pair<MainSessionSignal, MainSessionUpdate>> sessions) {
    	executeBatchPair("insert into e_main_ses(id_ses,cd_ins,va_typ,va_thr,va_lct,va_nam,va_usr,dh_str,dh_end,va_msk) values(?,?,?,?,?,?,?,?,?,?)", sessions, (ps, pr) -> {
            var sgn = pr.signal();
            var upd = pr.update();
            var idx = mainSessionSignalSetter(ps, sgn);
            ps.setString(++idx, nonNull(upd.getLocation()) ? upd.getLocation() : sgn.getLocation());
            ps.setString(++idx, nonNull(upd.getName()) ? upd.getName() : sgn.getName());
            ps.setString(++idx, nonNull(upd.getUser()) ? upd.getUser() : sgn.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(nonNull(upd.getStart()) ? upd.getStart() : sgn.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setInt(++idx, upd.getRequestMask().get());
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
        executeBatch("""
update e_main_ses set va_lct=coalesce(?, va_lct), va_nam=coalesce(?, va_nam), va_usr=coalesce(?, va_usr), dh_str=coalesce(?, dh_str), dh_end=?, va_msk=?
where id_ses=?""", updates, (ps, upd) -> {
            var idx = 0;
            ps.setString(++idx, upd.getLocation());
            ps.setString(++idx, upd.getName());
            ps.setString(++idx, upd.getUser());
            ps.setTimestamp(++idx, fromNullableInstant(upd.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setInt(++idx, upd.getRequestMask().get());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void updateMaskMainSessions(List<SessionMaskUpdate> updates) {
        executeBatch("update e_main_ses set va_msk=? where id_ses=?", updates, (ps, upd) -> {
            var idx = 0;
            ps.setInt(++idx, upd.getMask());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestRequestSignals(List<HttpRequestSignal> signals) {
        executeBatch("""
insert into e_rst_rqt(id_rst_rqt,cd_prn_ses,cd_ins,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_ath_sch,va_o_sze,va_o_cnt_enc,va_thr,va_usr,dh_str)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", signals, TraceDao::restRequestSignalSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestRequests(List<Pair<HttpRequestSignal, HttpRequestUpdate>> requests) {
    	executeBatchPair("""
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
        executeBatch("update e_rst_rqt set va_cnt_typ=?, cd_stt=?, va_i_sze=?, va_i_cnt_enc=?, dh_end=?, va_bdy_cnt=?, va_lnk=? where id_rst_rqt=?", updates, (ps, upd) -> {
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
        executeBatch("insert into e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str) values(?,?,?,?,?,?,?,?,?)", signals, (ps, req) -> {
            var idx = localRequestSignalSetter(ps, req);
            ps.setTimestamp(++idx, fromNullableInstant(req.getStart()));
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLocalRequests(List<Pair<LocalRequestSignal, LocalRequestUpdate>> requests) {
    	executeBatchPair("""
insert into e_lcl_rqt(id_lcl_rqt,cd_prn_ses,cd_ins,va_typ,va_nam,va_lct,va_usr,va_thr,dh_str,dh_end,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
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
        executeBatch("update e_lcl_rqt set dh_str=coalesce(?, dh_str), dh_end=?, status=? where id_lcl_rqt=?", requests, (ps, upd) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequestSignals(List<MailRequestSignal> signals) {
        executeBatch("""
insert into e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
values(?,?,?,?,?,?,?,?,?)""", signals, TraceDao::mailRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequests(List<Pair<MailRequestSignal, MailRequestUpdate>> requests) {
        executeBatchPair("""
insert into e_smtp_rqt(id_smtp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
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
        executeBatch("update e_smtp_rqt set dh_end=?, va_cmd=?, status=? where id_smtp_rqt=?", updates, (ps, upd) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequestSignals(List<FtpRequestSignal> signals) {
        executeBatch("""
insert into e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str)
values(?,?,?,?,?,?,?,?,?,?,?)""", signals, TraceDao::ftpRequestSignalSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequests(List<Pair<FtpRequestSignal, FtpRequestUpdate>> requests) {
        executeBatchPair("""
insert into e_ftp_rqt(id_ftp_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
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
        executeBatch("update e_ftp_rqt set dh_end=?, va_cmd=?, status=? where id_ftp_rqt=?", updates, (ps, upd) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequestSignals(List<DirectoryRequestSignal> signals) {
        executeBatch("""
insert into e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str)
values(?,?,?,?,?,?,?,?,?)""", signals, TraceDao::ldapRequestSignalSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequests(List<Pair<DirectoryRequestSignal, DirectoryRequestUpdate>> requests) {
        executeBatchPair("""
insert into e_ldap_rqt(id_ldap_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_pcl,va_usr,va_thr,dh_str,dh_end,va_cmd,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pr) -> {
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
        executeBatch("update e_ldap_rqt set dh_end=?, va_cmd=?, status=? where id_ldap_rqt=?", updates, (ps, upd) -> {
            var idx = 0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequestSignals(List<DatabaseRequestSignal> signals) {
        executeBatch("""
insert into e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", signals, TraceDao::databaseRequestSetter);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequests(List<Pair<DatabaseRequestSignal, DatabaseRequestUpdate>> requests) {
        executeBatchPair("""
insert into e_dtb_rqt(id_dtb_rqt,cd_prn_ses,cd_ins,va_hst,cd_prt,va_she,va_nam,va_sha,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,dh_str,dh_end,va_cmd,cd_stt)
values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", requests, (ps, pair) -> {
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
        ps.setString(++idx, sgn.getSchema());
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
        executeBatch("update e_dtb_rqt set dh_end=?, va_cmd=?, status=? where id_dtb_rqt=?", updates, (ps, upd) -> {
           var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(upd.getEnd()));
            ps.setString(++idx, upd.getCommand());
            ps.setShort(++idx, upd.getStatus());
            ps.setObject(++idx, upd.getId());
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
    public void saveFtpRequestStages(List<FtpRequestStage> stages) {
        executeBatch("insert into e_ftp_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_ftp_rqt,va_pld) values(?,?,?,?,?,?,?)", stages, (ps, stg)-> {
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
        executeBatch("insert into e_ldap_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_ldap_rqt,va_pld) values(?,?,?,?,?,?,?)", stages, (ps, stg)-> {
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
        executeBatch("insert into e_smtp_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_smtp_rqt) values(?,?,?,?,?,?)", stages, (ps, stg)-> {
        	var idx=0;
            ps.setString(++idx, stg.getName());
            ps.setTimestamp(++idx, fromNullableInstant(stg.getStart()));
            ps.setTimestamp(++idx, fromNullableInstant(stg.getEnd()));
            ps.setString(++idx, stg.getCommand());
            ps.setInt(++idx, stg.getOrder());
            ps.setObject(++idx, stg.getRequestId());
            //ps.setObject(++idx, toJson(stg.getPayload()), OTHER) no payload
        });
        saveMailRequestMails(stages);
    }

    void saveMailRequestMails(List<MailRequestStage> stages) {
    	var mails = stages.stream().filter(m -> nonNull(m.getMail())).toList();
        executeBatch("insert into e_smtp_mail(va_sbj,va_cnt_typ,va_frm,va_rcp,va_rpl,va_sze,cd_smtp_rqt) values(?,?,?,?,?,?,?)", mails, (ps, stg)-> {
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
        executeBatch("insert into e_dtb_stg(va_nam,dh_str,dh_end,va_cmd,cd_ord,cd_dtb_rqt,va_pld) values(?,?,?,?,?,?,?)", stages, (ps, stg)-> {
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
        executeBatch("insert into e_exc_inf(va_err_typ,va_err_msg,va_stk,va_cas,cd_ord,cd_rqt) values(?,?,?,?,?,?)", exceptions, (ps, exp) -> {
            var idx=0;
            ps.setString(++idx, exp.getType());
            ps.setString(++idx, exp.getMessage());
            ps.setObject(++idx, toJson(exp.getStackTraceRows()), OTHER);
            ps.setObject(++idx, toJson(exp.getCause()), OTHER);
            ps.setLong(++idx, exp.getOffset());
            ps.setObject(++idx, exp.getTraceId());
        });
    }
    
    @Transactional(rollbackFor = Throwable.class)
    public void saveSessionEvents(List<SessionEvent> exceptions) {
        executeBatch("insert into e_ses_evt(dh_str,va_typ,va_cnt,va_lct,cd_ins) values(?,?,?,?,?,?)", exceptions, (ps, exp) -> {
            var idx=0;
            ps.setTimestamp(++idx, fromNullableInstant(exp.getInstant()));
            ps.setString(++idx, exp.getType());
            ps.setString(++idx, exp.getValue());
            ps.setString(++idx, exp.getLocation());
            ps.setObject(++idx, exp.getSessionId(), OTHER);
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
    
    static String toStringOrNull(Enum<?> e) {
    	return nonNull(e) ? e.name() : null;
	}
}
