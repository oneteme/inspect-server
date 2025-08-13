package org.usf.inspect.server.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.*;
import org.usf.inspect.server.model.InstanceEnvironmentUpdate;
import org.usf.inspect.server.model.InstanceTrace;
import org.usf.inspect.server.model.wrapper.MailWrapper;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.server.Utils.*;
import static org.usf.inspect.server.dao.RequestCompletableType.*;
import static org.usf.inspect.server.model.RequestMask.LOCAL;


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
values(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?::json,?::json,?::json)""", ps -> {
            ps.setString(1, instance.getId());
            ps.setString(2, instance.getType() != null ? instance.getType().name() : null);
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
                ps.setString(14, instance.getConfiguration() != null ? mapper.writeValueAsString(instance.getConfiguration()) : null);
                ps.setString(15, instance.getResource() != null ? mapper.writeValueAsString(instance.getResource()) : null);
                ps.setString(16, instance.getAdditionalProperties() != null ? mapper.writeValueAsString(instance.getAdditionalProperties()) : null);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void updateInstanceEnvironments(List<InstanceEnvironmentUpdate> instances){
        executeBatch("update e_env_ins set dh_end = ? where id_ins = ?::uuid", instances.iterator(), (ps, instance) -> {
            ps.setTimestamp(1, fromNullableInstant(instance.getEnd()));
            ps.setString(2, instance.getId());
        });
    }

    public void saveInstanceTraces(List<InstanceTrace> instanceTraces) {
        executeBatch("""
insert into e_ins_trc (va_pnd, va_atp, va_ses_sze, dh_str, va_fln, cd_ins)
values (?, ?, ?, ?, ?, ?::uuid)""", instanceTraces.iterator(), (ps, instanceTrace) -> {
            ps.setObject(1, instanceTrace.getPending(), Types.INTEGER);
            ps.setObject(2, instanceTrace.getAttempts(), Types.INTEGER);
            ps.setInt(3, instanceTrace.getTraceCount());
            ps.setTimestamp(4, fromNullableInstant(instanceTrace.getInstant()));
            ps.setString(5, instanceTrace.getFileName());
            ps.setString(6, instanceTrace.getInstanceId());
        });
    }

    public void saveLogEntries(List<LogEntry> logEntries) {
        executeBatch("""
insert into e_log_ent(va_lvl,va_msg,va_stk,dh_str,cd_prn_ses,cd_ins)
values (?,?,?::json,?,?::uuid,?::uuid)""", logEntries.iterator(), (ps, o)-> {
            ps.setString(1, String.valueOf(o.getLevel()));
            ps.setString(2, o.getMessage());
            try {
                ps.setString(3, o.getStackRows() != null ? mapper.writeValueAsString(o.getStackRows()) : null);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            ps.setTimestamp(4, fromNullableInstant(o.getInstant()));
            ps.setString(5, o.getSessionId());
            ps.setString(6, o.getInstanceId());
        });
    }

    public void saveMachineResourceUsages(List<MachineResourceUsage> usages) {
        executeBatch("""
insert into e_rsc_usg(dh_str,va_usd_hep,va_cmt_hep,va_usd_met,va_cmt_met,va_usd_dsk,cd_ins)
values (?,?,?,?,?,?,?::uuid)""",usages.iterator(), (ps, o)-> {
            ps.setTimestamp(1, fromNullableInstant(o.getInstant()));
            ps.setInt(2, o.getUsedHeap());
            ps.setInt(3, o.getCommitedHeap());
            ps.setInt(4, o.getUsedMeta());
            ps.setInt(5, o.getCommitedMeta());
            ps.setInt(6, o.getUsedDiskSpace());
            ps.setString(7, o.getInstanceId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestSessions(List<RestSession> sessions) {
        completableProcess(REST_SESSION, sessions, toUpdate ->
                executeBatch("""
update e_rst_ses set va_mth = ?, va_pcl = ?, va_hst = ?, cd_prt = ?, va_pth = ?, va_qry = ?, va_cnt_typ = ?, va_ath_sch = ?, cd_stt = ?, va_i_sze = ?, va_o_sze = ?, va_i_cnt_enc = ?, va_o_cnt_enc = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_err_typ = ?, va_err_msg = ?, va_nam = ?, va_usr = ?, va_usr_agt = ?, va_cch_ctr = ?, va_msk = ?
where id_ses = ?::uuid""", toUpdate, (ps, ses) -> {
            var exp = ses.getException();
            ps.setString(1, ses.getMethod());
            ps.setString(2, ses.getProtocol());
            ps.setString(3, ses.getHost());
            ps.setInt(4, ses.getPort());
            ps.setString(5, ses.getPath());
            ps.setString(6, ses.getQuery());
            ps.setString(7, contentTypeExtract(ses.getContentType()));
            ps.setString(8, ses.getAuthScheme());
            ps.setInt(9, ses.getStatus());
            ps.setLong(10, ses.getInDataSize());
            ps.setLong(11, ses.getOutDataSize());
            ps.setString(12, ses.getInContentEncoding());
            ps.setString(13, ses.getOutContentEncoding());
            ps.setTimestamp(14, fromNullableInstant(ses.getStart()));
            ps.setTimestamp(15, fromNullableInstant(ses.getEnd()));
            ps.setString(16, ses.getThreadName());
            ps.setString(17, nonNull(exp) ? exp.getType() : null);
            ps.setString(18, nonNull(exp) ? exp.getMessage() : null);
            ps.setString(19, ses.getName());
            ps.setString(20, ses.getUser());
            ps.setString(21, userAgentExtract(ses.getUserAgent()));
            ps.setString(22, ses.getCacheControl());
            ps.setInt(23, ses.getRequestsMask());
            ps.setString(24, ses.getId());
        }), toInsert ->
                executeBatch("""
insert into e_rst_ses(id_ses,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_cnt_typ,va_ath_sch,cd_stt,va_i_sze,va_o_sze,va_i_cnt_enc,va_o_cnt_enc,dh_str,dh_end,va_thr,va_err_typ,va_err_msg,va_nam,va_usr,va_usr_agt,va_cch_ctr,va_msk,cd_ins)
values(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid)""", toInsert, (ps, ses) -> {
            var exp = ses.getException();
            ps.setString(1, ses.getId());
            ps.setString(2, ses.getMethod());
            ps.setString(3, ses.getProtocol());
            ps.setString(4, ses.getHost());
            ps.setInt(5, ses.getPort());
            ps.setString(6, ses.getPath());
            ps.setString(7, ses.getQuery());
            ps.setString(8, contentTypeExtract(ses.getContentType()));
            ps.setString(9, ses.getAuthScheme());
            ps.setInt(10, ses.getStatus());
            ps.setLong(11, ses.getInDataSize());
            ps.setLong(12, ses.getOutDataSize());
            ps.setString(13, ses.getInContentEncoding());
            ps.setString(14, ses.getOutContentEncoding());
            ps.setTimestamp(15, fromNullableInstant(ses.getStart()));
            ps.setTimestamp(16, fromNullableInstant(ses.getEnd()));
            ps.setString(17, ses.getThreadName());
            ps.setString(18, nonNull(exp) ? exp.getType() : null);
            ps.setString(19, nonNull(exp) ? exp.getMessage() : null);
            ps.setString(20, ses.getName());
            ps.setString(21, ses.getUser());
            ps.setString(22, userAgentExtract(ses.getUserAgent()));
            ps.setString(23, ses.getCacheControl());
            ps.setInt(24, ses.getRequestsMask());
            ps.setString(25, ses.getInstanceId());
        }), RestSession::getId, s -> nonNull(s.getEnd()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMainSessions(List<MainSession> sessions) {
        completableProcess(MAIN_SESSION, sessions, toUpdate ->
                executeBatch("""
update e_main_ses set va_nam = ?, va_usr = ?, dh_str = ?, dh_end = ?, va_typ = ?, va_lct = ?, va_thr = ?, va_err_typ = ?, va_err_msg = ?, va_msk = ?
where id_ses = ?::uuid""", toUpdate, (ps, ses) -> {
            var exp = ses.getException();
            ps.setString(1, ses.getName());
            ps.setString(2, ses.getUser());
            ps.setTimestamp(3, fromNullableInstant(ses.getStart()));
            ps.setTimestamp(4, fromNullableInstant(ses.getEnd()));
            ps.setString(5, valueOfNullable(ses.getType()));
            ps.setString(6, ses.getLocation());
            ps.setString(7, ses.getThreadName());
            ps.setString(8, nonNull(exp) ? exp.getType() : null);
            ps.setString(9, nonNull(exp) ? exp.getMessage() : null);
            ps.setInt(10, ses.getRequestsMask());
            ps.setString(11, ses.getId());
        }), toInsert ->
                executeBatch("""
insert into e_main_ses(id_ses,va_nam,va_usr,dh_str,dh_end,va_typ,va_lct,va_thr,va_err_typ,va_err_msg,va_msk,cd_ins)
values(?::uuid,?,?,?,?,?,?,?,?,?,?,?::uuid)""", toInsert, (ps, ses) -> {
            var exp = ses.getException();
            ps.setString(1, ses.getId());
            ps.setString(2, ses.getName());
            ps.setString(3, ses.getUser());
            ps.setTimestamp(4, fromNullableInstant(ses.getStart()));
            ps.setTimestamp(5, fromNullableInstant(ses.getEnd()));
            ps.setString(6, valueOfNullable(ses.getType()));
            ps.setString(7, ses.getLocation());
            ps.setString(8, ses.getThreadName());
            ps.setString(9, nonNull(exp) ? exp.getType() : null);
            ps.setString(10, nonNull(exp) ? exp.getMessage() : null);
            ps.setInt(11, ses.getRequestsMask());
            ps.setString(12, ses.getInstanceId());
        }), MainSession::getId, s -> nonNull(s.getEnd()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveRestRequests(List<RestRequest> requests) {
        completableProcess(REST_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_rst_rqt set va_mth = ?, va_pcl = ?, va_hst = ?, cd_prt = ?, va_pth = ?, va_qry = ?, va_cnt_typ = ?, va_ath_sch = ?, cd_stt = ?, va_i_sze = ?, va_o_sze = ?, va_i_cnt_enc = ?, va_o_cnt_enc = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_bdy_cnt = ?
where id_rst_rqt = ?::uuid""", toUpdate, (ps, req) -> {
            ps.setString(1, req.getMethod());
            ps.setString(2, req.getProtocol());
            ps.setString(3, req.getHost());
            ps.setInt(4, req.getPort());
            ps.setString(5, req.getPath());
            ps.setString(6, req.getQuery());
            ps.setString(7, contentTypeExtract(req.getContentType()));
            ps.setString(8, req.getAuthScheme());
            ps.setInt(9, req.getStatus());
            ps.setLong(10, req.getInDataSize());
            ps.setLong(11, req.getOutDataSize());
            ps.setString(12, req.getInContentEncoding());
            ps.setString(13, req.getOutContentEncoding());
            ps.setTimestamp(14, fromNullableInstant(req.getStart()));
            ps.setTimestamp(15, fromNullableInstant(req.getEnd()));
            ps.setString(16, req.getThreadName());
            ps.setString(17, req.getBodyContent());
            ps.setString(18, req.getId());
        }), toInsert ->
                executeBatch("""
insert into e_rst_rqt(id_rst_rqt,va_mth,va_pcl,va_hst,cd_prt,va_pth,va_qry,va_cnt_typ,va_ath_sch,cd_stt,va_i_sze,va_o_sze,va_i_cnt_enc,va_o_cnt_enc,dh_str,dh_end,va_thr,va_bdy_cnt,cd_prn_ses,cd_ins)
values(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
            ps.setString(1, req.getId());
            ps.setString(2, req.getMethod());
            ps.setString(3, req.getProtocol());
            ps.setString(4, req.getHost());
            ps.setInt(5, req.getPort());
            ps.setString(6, req.getPath());
            ps.setString(7, req.getQuery());
            ps.setString(8, contentTypeExtract(req.getContentType()));
            ps.setString(9, req.getAuthScheme());
            ps.setInt(10, req.getStatus());
            ps.setLong(11, req.getInDataSize());
            ps.setLong(12, req.getOutDataSize());
            ps.setString(13, req.getInContentEncoding());
            ps.setString(14, req.getOutContentEncoding());
            ps.setTimestamp(15, fromNullableInstant(req.getStart()));
            ps.setTimestamp(16, fromNullableInstant(req.getEnd()));
            ps.setString(17, req.getThreadName());
            ps.setString(18, req.getBodyContent());
            ps.setString(19, req.getSessionId());
            ps.setString(20, req.getInstanceId());
        }), RestRequest::getId, s -> nonNull(s.getEnd()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLocalRequests(List<LocalRequest> requests){
        completableProcess(LOCAL_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_lcl_rqt set va_nam = ?, va_lct = ?, dh_str = ?, dh_end = ?, va_usr = ?, va_thr = ?, va_fail = ?, va_typ = ?
where id_lcl_rqt = ?::uuid""", toUpdate, (ps, req) -> {
            ps.setString(1,req.getName());
            ps.setString(2,req.getLocation());
            ps.setTimestamp(3,fromNullableInstant(req.getStart()));
            ps.setTimestamp(4,fromNullableInstant(req.getEnd()));
            ps.setString(5,req.getUser());
            ps.setString(6,req.getThreadName());
            ps.setBoolean(7, !isNull(req.getException()));
            ps.setString(8, req.getType());
            ps.setString(9, req.getId());
        }), toInsert ->
                executeBatch("""
insert into e_lcl_rqt(id_lcl_rqt,va_nam,va_lct,dh_str,dh_end,va_usr,va_thr,va_fail,cd_prn_ses,va_typ,cd_ins)
values(?::uuid,?,?,?,?,?,?,?,?::uuid,?,?::uuid)""", toInsert, (ps, req) -> {
            ps.setString(1, req.getId());
            ps.setString(2,req.getName());
            ps.setString(3,req.getLocation());
            ps.setTimestamp(4,fromNullableInstant(req.getStart()));
            ps.setTimestamp(5,fromNullableInstant(req.getEnd()));
            ps.setString(6,req.getUser());
            ps.setString(7,req.getThreadName());
            ps.setBoolean(8, nonNull(req.getException()));
            ps.setString(9, req.getSessionId());
            ps.setString(10, req.getType());
            ps.setString(11, req.getInstanceId());
        }), LocalRequest::getId, s -> nonNull(s.getEnd()));
        saveLocalRequestExceptions(requests);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequests(List<MailRequest> requests) {
        completableProcess(SMTP_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_smtp_rqt set va_hst = ?, cd_prt = ?, va_pcl = ?, va_usr = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_fail = ?
where id_smtp_rqt = ?::uuid""", toUpdate, (ps, req) -> {
            ps.setString(1, req.getHost());
            ps.setInt(2, req.getPort());
            ps.setString(3, req.getProtocol());
            ps.setString(4, req.getUser());
            ps.setTimestamp(5, fromNullableInstant(req.getStart()));
            ps.setTimestamp(6, fromNullableInstant(req.getEnd()));
            ps.setString(7, req.getThreadName());
            ps.setBoolean(8, req.isFailed());
            ps.setString(9, req.getId());
        }), toInsert ->
                executeBatch("""
insert into e_smtp_rqt(id_smtp_rqt,va_hst,cd_prt,va_pcl,va_usr,dh_str,dh_end,va_thr,va_fail,cd_prn_ses,cd_ins)
values(?::uuid,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
            ps.setString(1, req.getId());
            ps.setString(2, req.getHost());
            ps.setInt(3, req.getPort());
            ps.setString(4, req.getProtocol());
            ps.setString(5, req.getUser());
            ps.setTimestamp(6, fromNullableInstant(req.getStart()));
            ps.setTimestamp(7, fromNullableInstant(req.getEnd()));
            ps.setString(8, req.getThreadName());
            ps.setBoolean(9, req.isFailed());
            ps.setString(10, req.getSessionId());
            ps.setString(11, req.getInstanceId());
        }), MailRequest::getId, s -> nonNull(s.getEnd()));
        saveMailRequestMails(requests.stream().filter(r -> nonNull(r.getMails()))
                .flatMap(r -> r.getMails().stream().map(m -> {
                    var mail = new MailWrapper();
                    mail.setRecipients(m.getRecipients());
                    mail.setFrom(m.getFrom());
                    mail.setReplyTo(m.getReplyTo());
                    mail.setSubject(m.getSubject());
                    mail.setSize(m.getSize());
                    mail.setContentType(m.getContentType());
                    mail.setRequestId(r.getId());
                    return mail;
                })).toList());
    }

    private void saveMailRequestMails(List<MailWrapper> mails) {
        executeBatch("insert into e_smtp_mail(va_sbj,va_cnt_typ,va_frm,va_rcp,va_rpl,va_sze,cd_smtp_rqt) values(?,?,?,?,?,?,?::uuid)", mails.iterator(), (ps, mail)-> {
            ps.setString(1, mail.getSubject());
            ps.setString(2, mail.getContentType());
            ps.setString(3, mail.getFrom() != null ? String.join(", ", mail.getFrom()) : null);
            ps.setString(4, mail.getRecipients() != null ? String.join(", ", mail.getRecipients()) : null);
            ps.setString(5, mail.getReplyTo() != null ? String.join(", ", mail.getReplyTo()) : null);
            ps.setInt(6,mail.getSize());
            ps.setString(7, mail.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequests(List<FtpRequest> requests){
        completableProcess(FTP_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_ftp_rqt set va_hst = ?, cd_prt = ?, va_pcl = ?, va_srv_vrs = ?, va_clt_vrs = ?, va_usr = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_fail = ?
where id_ftp_rqt = ?::uuid""", toUpdate, (ps, req) -> {
            ps.setString(1, req.getHost());
            ps.setInt(2, req.getPort());
            ps.setString(3, req.getProtocol());
            ps.setString(4, req.getServerVersion());
            ps.setString(5, req.getClientVersion());
            ps.setString(6, req.getUser());
            ps.setTimestamp(7, fromNullableInstant(req.getStart()));
            ps.setTimestamp(8, fromNullableInstant(req.getEnd()));
            ps.setString(9, req.getThreadName());
            ps.setBoolean(10, req.isFailed());
            ps.setString(11, req.getId());
        }), toInsert ->
                executeBatch("""
insert into e_ftp_rqt(id_ftp_rqt,va_hst,cd_prt,va_pcl,va_srv_vrs,va_clt_vrs,va_usr,dh_str,dh_end,va_thr,va_fail,cd_prn_ses,cd_ins)
values(?::uuid,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
                    ps.setString(1, req.getId());
                    ps.setString(2, req.getHost());
                    ps.setInt(3, req.getPort());
                    ps.setString(4, req.getProtocol());
                    ps.setString(5, req.getServerVersion());
                    ps.setString(6, req.getClientVersion());
                    ps.setString(7, req.getUser());
                    ps.setTimestamp(8, fromNullableInstant(req.getStart()));
                    ps.setTimestamp(9, fromNullableInstant(req.getEnd()));
                    ps.setString(10, req.getThreadName());
                    ps.setBoolean(11, req.isFailed());
                    ps.setString(12, req.getSessionId());
                    ps.setString(13, req.getInstanceId());
                }), FtpRequest::getId, s -> nonNull(s.getEnd()));
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequests(List<DirectoryRequest> requests) {
        completableProcess(LDAP_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_ldap_rqt set va_hst = ?, cd_prt = ?, va_pcl = ?, va_usr = ?, dh_str = ?, dh_end = ?, va_thr = ?, va_fail = ?
where id_ldap_rqt = ?::uuid""", toUpdate, (ps, req) -> {
                ps.setString(1, req.getHost());
                ps.setInt(2, req.getPort());
                ps.setString(3, req.getProtocol());
                ps.setString(4, req.getUser());
                ps.setTimestamp(5, fromNullableInstant(req.getStart()));
                ps.setTimestamp(6, fromNullableInstant(req.getEnd()));
                ps.setString(7, req.getThreadName());
                ps.setBoolean(8, req.isFailed());
                ps.setString(9, req.getId());
            }), toInsert ->
                executeBatch("""
insert into e_ldap_rqt(id_ldap_rqt,va_hst,cd_prt,va_pcl,va_usr,dh_str,dh_end,va_thr,va_fail,cd_prn_ses,cd_ins)
values(?::uuid,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
                ps.setString(1, req.getId());
                ps.setString(2, req.getHost());
                ps.setInt(3, req.getPort());
                ps.setString(4, req.getProtocol());
                ps.setString(5, req.getUser());
                ps.setTimestamp(6, fromNullableInstant(req.getStart()));
                ps.setTimestamp(7, fromNullableInstant(req.getEnd()));
                ps.setString(8, req.getThreadName());
                ps.setBoolean(9, req.isFailed());
                ps.setString(10, req.getSessionId());
                ps.setString(11, req.getInstanceId());
            }), DirectoryRequest::getId, s -> nonNull(s.getEnd())
        );
    }


    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequests(List<DatabaseRequest> requests) {
        completableProcess(JDBC_REQUEST, requests, toUpdate ->
                executeBatch("""
update e_dtb_rqt set va_hst = ?, cd_prt = ?, va_she = ?, va_nam = ?, va_sha = ?, dh_str = ?, dh_end = ?, va_usr = ?, va_thr = ?, va_drv = ?, va_prd_nam = ?, va_prd_vrs = ?, va_cmd = ?, va_fail = ?
where id_dtb_rqt = ?::uuid""", toUpdate, (ps, req) -> {
                ps.setString(1, req.getHost());
                ps.setInt(2, req.getPort());
                ps.setString(3, req.getScheme());
                ps.setString(4, req.getName());
                ps.setString(5, req.getSchema());
                ps.setTimestamp(6, fromNullableInstant(req.getStart()));
                ps.setTimestamp(7, fromNullableInstant(req.getEnd()));
                ps.setString(8, req.getUser());
                ps.setString(9, req.getThreadName());
                ps.setString(10, req.getDriverVersion());
                ps.setString(11, req.getProductName());
                ps.setString(12, req.getProductVersion());
                ps.setString(13, req.getCommand()); // TODO use commands on DatabaseRequest
                ps.setBoolean(14, req.isFailed());
                ps.setString(15, req.getId());
            }), toInsert ->
                executeBatch("""
insert into e_dtb_rqt(id_dtb_rqt,va_hst,cd_prt,va_she,va_nam,va_sha,dh_str,dh_end,va_usr,va_thr,va_drv,va_prd_nam,va_prd_vrs,va_cmd,va_fail,cd_prn_ses,cd_ins)
values(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", toInsert, (ps, req) -> {
                ps.setString(1, req.getId());
                ps.setString(2, req.getHost());
                ps.setInt(3, req.getPort());
                ps.setString(4, req.getScheme());
                ps.setString(5, req.getName());
                ps.setString(6, req.getSchema());
                ps.setTimestamp(7, fromNullableInstant(req.getStart()));
                ps.setTimestamp(8, fromNullableInstant(req.getEnd()));
                ps.setString(9, req.getUser());
                ps.setString(10, req.getThreadName());
                ps.setString(11, req.getDriverVersion());
                ps.setString(12, req.getProductName());
                ps.setString(13, req.getProductVersion());
                ps.setString(14, req.getCommand()); // TODO use commands on DatabaseRequest
                ps.setBoolean(15, req.isFailed());
                ps.setString(16, req.getSessionId());
                ps.setString(17, req.getInstanceId());
            }), DatabaseRequest::getId, s -> nonNull(s.getEnd())
        );
    }

    private <T> void completableProcess(
            RequestCompletableType type,
            List<T> items,
            ToIntFunction<Iterator<T>> updateBatchExecutor,
            ToIntFunction<Iterator<T>> insertBatchExecutor,
            Function<T, String> idExtractor,
            Predicate<T> endCondition
    ) {
        List<String> completableMetrics = template.queryForList(
                String.format("select id_cmp_mtc from e_cmp_mtc where cd_typ = %s", type.getValue()),
                String.class
        );
        var toUpdate = items.stream().filter(s -> completableMetrics.contains(idExtractor.apply(s))).toList();
        var toInsert = items.stream().filter(s -> !completableMetrics.contains(idExtractor.apply(s))).toList();

        if(!toUpdate.isEmpty()) {

            var updated = updateBatchExecutor.applyAsInt(toUpdate.iterator());
            if(updated != toUpdate.size()) {
                log.warn("Not all {} {} were updated, only {} were updated", type.name(), toUpdate.size(), updated);
            }
            var completedMetrics = toUpdate.stream()
                    .filter(endCondition)
                    .map(idExtractor).toList(); //Insertion des sessions uncompleted
            if(!completedMetrics.isEmpty()) {
                var inClause = "?::uuid" + ", ?::uuid".repeat(completedMetrics.size() - 1);
                template.update(String.format("delete from e_cmp_mtc where id_cmp_mtc in (%s) and cd_typ = %s", inClause, type.getValue()), ps -> {
                    var n = 1;
                    for (String cmpMetric : completedMetrics) {
                        ps.setString(n++, cmpMetric);
                    }
                });
            }
        }

        if(!toInsert.isEmpty()) {

            insertBatchExecutor.applyAsInt(toInsert.iterator());
            var uncompletedMetrics = toInsert.stream()
                    .filter(s -> !endCondition.test(s))
                    .map(idExtractor).toList(); //Insertion des sessions uncompleted
            if(!uncompletedMetrics.isEmpty()) {
                executeBatch(String.format("insert into e_cmp_mtc(id_cmp_mtc,cd_typ) values(?::uuid, %s)", type.getValue()), uncompletedMetrics.iterator(), (ps, id) -> {
                    ps.setString(1, id);
                });
            }
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpRequestStages(List<HttpRequestStage> stages) {
        executeBatch("insert into e_rst_rqt_stg(va_nam,dh_str,dh_end,cd_ord,cd_rst_rqt) values(?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setString(5, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.REST);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveHttpSessionStages(List<HttpSessionStage> stages) {
        executeBatch("insert into e_rst_ses_stg(va_nam,dh_str,dh_end,cd_ord,cd_prn_ses) values(?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setString(5, stage.getRequestId());
        });
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveMailRequestStages(List<MailRequestStage> stages) {
        executeBatch("insert into e_smtp_stg(va_nam,dh_str,dh_end,cd_ord,cd_smtp_rqt) values(?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setString(5, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.SMTP);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveFtpRequestStages(List<FtpRequestStage> stages) {
        executeBatch("insert into e_ftp_stg(va_nam,dh_str,dh_end,va_arg,cd_ord,cd_ftp_rqt) values(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
            ps.setInt(5, stage.getOrder());
            ps.setString(6, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.FTP);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveLdapRequestStages(List<DirectoryRequestStage> stages) {
        executeBatch("insert into e_ldap_stg(va_nam,dh_str,dh_end,va_arg,cd_ord,cd_ldap_rqt) values(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
            ps.setInt(5, stage.getOrder());
            ps.setString(6, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.LDAP);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void saveDatabaseRequestStages(List<DatabaseRequestStage> stages) {
        executeBatch("insert into e_dtb_stg(va_nam,dh_str,dh_end,va_cnt,va_cmd,cd_ord,cd_dtb_rqt) values(?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, valueOfNullableArray(stage.getCount()));
            ps.setString(5,valueOfNullableList(stage.getCommands()));
            ps.setInt(6, stage.getOrder());
            ps.setString(7, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.JDBC);
    }

    private void saveExceptions(List<? extends AbstractStage> stages, RequestMask mask) {
        var exceptions = stages.stream()
                .filter(e -> nonNull(e.getException()))
                .toList();
        if(!isEmpty(exceptions)) {
            executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?::json,?,?::uuid)",
                    exceptions.iterator(), (ps, exp) -> {
                        ps.setString(1, mask.name());
                        ps.setString(2, exp.getException().getType());
                        ps.setString(3, exp.getException().getMessage());
                        try {
                            ps.setObject(4, exp.getException().getStackTraceRows() != null ? mapper.writeValueAsString(exp.getException().getStackTraceRows()) : null);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        ps.setInt(5, exp.getOrder());
                        ps.setString(6, exp.getRequestId());
                    });
        }
    }

    private void saveLocalRequestExceptions(List<LocalRequest> stages) {
        var exceptions = stages.stream()
                .filter(e -> nonNull(e.getException()))
                .toList();
        if(!isEmpty(exceptions)) {
            executeBatch("insert into e_exc_inf(va_typ,va_err_typ,va_err_msg,va_stk,cd_ord,cd_rqt) values(?,?,?,?::json,?,?::uuid)",
                    exceptions.iterator(), (ps, exp) -> {
                        ps.setString(1, LOCAL.name());
                        ps.setString(2, exp.getException().getType());
                        ps.setString(3, exp.getException().getMessage());
                        try {
                            ps.setString(4, exp.getException().getStackTraceRows() != null ? mapper.writeValueAsString(exp.getException().getStackTraceRows()) : null);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        ps.setInt(5, 0);
                        ps.setString(6, exp.getId());
                    });
        }
    }

    private <T> int executeBatch(String sql, Iterator<T> it, ParameterizedPreparedStatementSetter<T> pss) {
        return template.execute(sql, (PreparedStatement ps) -> {
            long rows = 0;
            var n = 0;
            while (it.hasNext()) {
                pss.setValues(ps, it.next());
                ps.addBatch();
                if (++n % BATCH_SIZE == 0) {
                    rows += IntStream.of(ps.executeBatch()).sum();
                }
            }
            if (n % BATCH_SIZE != 0) {
                rows += IntStream.of(ps.executeBatch()).sum();
            }
            log.debug("{} batch added, {} rows inserted", n, rows);
            return n;
        });
    }
}
