package org.usf.inspect.server.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.usf.inspect.core.*;
import org.usf.inspect.core.InstanceEnvironment;
import org.usf.inspect.core.RequestMask;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.wrapper.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.usf.inspect.server.TreeIterator.treeIterator;
import static org.usf.inspect.server.Utils.*;
import static org.usf.inspect.server.model.RequestMask.*;

@Repository
@Slf4j
public class RequestDao {
    private final JdbcTemplate template;
    private final ObjectMapper mapper;

    private static final int BATCH_SIZE = 1_000;

    public RequestDao(JdbcTemplate template, ObjectMapper mapper) {
        this.template = template;
        this.mapper = mapper;
    }

    public void saveInstanceEnvironment(InstanceEnvironment instance) {
        template.update("""
INSERT INTO E_ENV_INS(ID_INS,VA_TYP,DH_STR,VA_APP,VA_VRS,VA_ADR,VA_ENV,VA_OS,VA_RE,VA_USR,VA_CLR,VA_BRCH,VA_HSH,VA_CNF,VA_RSR,VA_ADD_PRP)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?::json,?::json,?::json)""", ps -> {
            ps.setObject(1, instance.getId());
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
                ps.setObject(14, instance.getConfiguration() != null ? mapper.writeValueAsString(instance.getConfiguration()) : null);
                ps.setObject(15, instance.getResource() != null ? mapper.writeValueAsString(instance.getResource()) : null);
                ps.setObject(16, instance.getAdditionalProperties() != null ? mapper.writeValueAsString(instance.getAdditionalProperties()) : null);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void updateInstanceEnvironment(Instant end, String instanceId){
        template.update("UPDATE E_ENV_INS SET DH_END = ? WHERE ID_INS = ?", ps -> {
            ps.setTimestamp(1,fromNullableInstant(end));
            ps.setString(2,instanceId);
        });
    }

    public void saveInstanceTrace(InstanceTrace instanceTrace) {
        template.update("""
INSERT INTO E_INS_TRC (VA_PND, VA_ATP, VA_SES_SZE, DH_STR, VA_FLN, CD_INS)
VALUES (?, ?, ?, ?, ?, ?::uuid);""", ps -> {
            ps.setInt(1, instanceTrace.getPending());
            ps.setInt(2, instanceTrace.getAttempts());
            ps.setInt(3, instanceTrace.getSessionLength());
            ps.setTimestamp(4, fromNullableInstant(instanceTrace.getInstant()));
            ps.setString(5, instanceTrace.getFileName());
            ps.setString(6, instanceTrace.getInstanceId());
        });
    }

    public int saveLogEntry(List<LogEntryWrapper> logEntries) {
        var arr = executeBatch("""
INSERT INTO E_LOG_ENT(VA_LVL,VA_MSG,VA_STK,DH_STR,CD_PRN_SES,CD_INS)
VALUES (?,?,?::json,?,?::uuid,?::uuid)""", logEntries.iterator(), (ps, o)-> {
            ps.setString(1, String.valueOf(o.getLevel()));
            ps.setString(2, o.getMessage());
            try {
                ps.setObject(3, o.getStackRows() != null ? mapper.writeValueAsString(o.getStackRows()) : null);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            ps.setTimestamp(4, fromNullableInstant(o.getInstant()));
            ps.setString(5, o.getSessionId());
            ps.setString(6, o.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    public int saveMachineResourceUsage(List<MachineResourceUsageWrapper> usages) {
        var arr = executeBatch("""
INSERT INTO E_RSC_USG(DH_STR,VA_LOW_HEP,VA_HIG_HEP,VA_LOW_MET,VA_HIG_MET,CD_INS)
    VALUES (?,?,?,?,?,?::uuid)""",usages.iterator(), (ps, o)-> {
            ps.setTimestamp(1, fromNullableInstant(o.getInstant()));
            ps.setInt(2, o.getLowHeap());
            ps.setInt(3, o.getHighHeap());
            ps.setInt(4, o.getLowMeta());
            ps.setInt(5, o.getHighMeta());
            ps.setString(6, o.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    public long saveEventTraces(List<EventTrace> eventTraces) {
        var ms = filterAndSave(eventTraces, MainSessionWrapper.class, this::saveMainSessions);
        var rs = filterAndSave(eventTraces, RestSessionWrapper.class, this::saveRestSessions);
        var rr = filterAndSave(eventTraces, RestRequestWrapper.class, this::saveRestRequests);
        var lr = filterAndSave(eventTraces, LocalRequestWrapper.class, this::saveLocalRequests);
        var mr = filterAndSave(eventTraces, MailRequestWrapper.class, this::saveMailRequests);
        var fr = filterAndSave(eventTraces, FtpRequestWrapper.class, this::saveFtpRequests);
        var nr = filterAndSave(eventTraces, NamingRequestWrapper.class, this::saveLdapRequests);
        var dr = filterAndSave(eventTraces, DatabaseRequestWrapper.class, this::mergeDatabaseRequests);
        var hrs = filterAndSave(eventTraces, HttpRequestStageWrapper.class, this::saveHttpRequestStages);
        var hss = filterAndSave(eventTraces, HttpSessionStageWrapper.class, this::saveHttpSessionStages);
        var mrs = filterAndSave(eventTraces, MailRequestStageWrapper.class, this::saveMailRequestStages);
        var frs = filterAndSave(eventTraces, FtpRequestStageWrapper.class, this::saveFtpRequestStages);
        var nrs = filterAndSave(eventTraces, NamingRequestStageWrapper.class, this::saveLdapRequestStages);
        var drs = filterAndSave(eventTraces, DatabaseRequestStageWrapper.class, this::saveDatabaseRequestStages);
        var mmr = filterAndSave(eventTraces, MachineResourceUsageWrapper.class, this::saveMachineResourceUsage);
        var log = filterAndSave(eventTraces, LogEntryWrapper.class, this::saveLogEntry);
        return ms + rs + rr + lr + mr + fr + nr + dr + hrs + hss + mrs + frs + nrs + drs + mmr + log;
    }

    private int saveRestSessions(List<RestSessionWrapper> sessions) {
        var arr = executeBatch("""
INSERT INTO E_RST_SES(ID_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,VA_ERR_TYP,VA_ERR_MSG,VA_NAM,VA_USR,VA_USR_AGT,VA_CCH_CTR,VA_MSK,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid)""", sessions.iterator(), (ps, ses) -> {
            var exp = ses.getException();
            ps.setObject(1, ses.getId());
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
            ps.setObject(25, ses.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveMainSessions(List<MainSessionWrapper> sessions) {
        var arr = executeBatch("""
INSERT INTO E_MAIN_SES(ID_SES,VA_NAM,VA_USR,DH_STR,DH_END,VA_TYP,VA_LCT,VA_THR,VA_ERR_TYP,VA_ERR_MSG,VA_MSK,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?,?,?::uuid)""", sessions.iterator(), (ps, ses) -> {
            var exp = ses.getException();
            ps.setObject(1, ses.getId());
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
            ps.setObject(12, ses.getInstanceId());
        });
        saveUserActions(sessions);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveRestRequests(List<RestRequestWrapper> requests) {
        var arr = executeBatch("""
INSERT INTO E_RST_RQT(ID_RST_RQT,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,VA_BDY_CNT,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getId());
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
            ps.setObject(19, req.getSessionId());
            ps.setObject(20, req.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveLocalRequests(List<LocalRequestWrapper> requests){

        var arr = executeBatch("""
INSERT INTO E_LCL_RQT(ID_LCL_RQT,VA_NAM,VA_LCT,DH_STR,DH_END,VA_USR,VA_THR,VA_FAIL,CD_PRN_SES,VA_TYP,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?::uuid,?,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getId());
            ps.setString(2,req.getName());
            ps.setString(3,req.getLocation());
            ps.setTimestamp(4,fromNullableInstant(req.getStart()));
            ps.setTimestamp(5,fromNullableInstant(req.getEnd()));
            ps.setString(6,req.getUser());
            ps.setString(7,req.getThreadName());
            ps.setBoolean(8, !isNull(req.getException()));
            ps.setObject(9, req.getSessionId());
            ps.setString(10, req.getType());
            ps.setObject(11, req.getInstanceId());
        });
        saveLocalRequestExceptions(requests);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveMailRequests(List<MailRequestWrapper> requests) {
        var arr = executeBatch("""
INSERT INTO E_SMTP_RQT(ID_SMTP_RQT,VA_HST,CD_PRT,VA_USR,DH_STR,DH_END,VA_THR,VA_FAIL,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getId());
            ps.setString(2, req.getHost());
            ps.setInt(3, req.getPort());
            ps.setString(4, req.getUser());
            ps.setTimestamp(5, fromNullableInstant(req.getStart()));
            ps.setTimestamp(6, fromNullableInstant(req.getEnd()));
            ps.setString(7, req.getThreadName());
            ps.setBoolean(8, req.isFailed());
            ps.setObject(9, req.getSessionId());
            ps.setObject(10, req.getInstanceId());
        });
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
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private void saveMailRequestMails(List<MailWrapper> mails) {
        executeBatch("INSERT INTO E_SMTP_MAIL(VA_SBJ,VA_CNT_TYP,VA_FRM,VA_RCP,VA_RPL,VA_SZE,CD_SMTP_RQT) VALUES(?,?,?,?,?,?,?::uuid)", mails.iterator(), (ps, mail)-> {
            ps.setString(1, mail.getSubject());
            ps.setString(2, mail.getContentType());
            ps.setString(3, mail.getFrom() != null ? String.join(", ", mail.getFrom()) : null);
            ps.setString(4, mail.getRecipients() != null ? String.join(", ", mail.getRecipients()) : null);
            ps.setString(5, mail.getReplyTo() != null ? String.join(", ", mail.getReplyTo()) : null);
            ps.setInt(6,mail.getSize());
            ps.setObject(7, mail.getRequestId());
        });
    }

    private int saveFtpRequests(List<FtpRequestWrapper> requests){
        var arr = executeBatch("""
INSERT INTO E_FTP_RQT(ID_FTP_RQT,VA_HST,CD_PRT,VA_PCL,VA_SRV_VRS,VA_CLT_VRS,VA_USR,DH_STR,DH_END,VA_THR,VA_FAIL,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getId());
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
            ps.setObject(12, req.getSessionId());
            ps.setObject(13, req.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveLdapRequests(List<NamingRequestWrapper> requests) {
        var arr = executeBatch("""
INSERT INTO E_LDAP_RQT(ID_LDAP_RQT,VA_HST,CD_PRT,VA_PCL,VA_USR,DH_STR,DH_END,VA_THR,VA_FAIL,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getId());
            ps.setString(2, req.getHost());
            ps.setInt(3, req.getPort());
            ps.setString(4, req.getProtocol());
            ps.setString(5, req.getUser());
            ps.setTimestamp(6, fromNullableInstant(req.getStart()));
            ps.setTimestamp(7, fromNullableInstant(req.getEnd()));
            ps.setString(8, req.getThreadName());
            ps.setBoolean(9, req.isFailed());
            ps.setObject(10, req.getSessionId());
            ps.setObject(11, req.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveDatabaseRequests(List<DatabaseRequestWrapper> requests) {
        var arr = executeBatch("""
INSERT INTO E_DTB_RQT(ID_DTB_RQT,VA_HST,CD_PRT,VA_NAM,VA_SCH,DH_STR,DH_END,VA_USR,VA_THR,VA_DRV,VA_PRD_NAM,VA_PRD_VRS,VA_CMD,VA_FAIL,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getId());
            ps.setString(2, req.getHost());
            ps.setInt(3, req.getPort());
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
            ps.setObject(15, req.getSessionId());
            ps.setObject(16, req.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int mergeDatabaseRequests(List<DatabaseRequestWrapper> requests) {
        var rows = executeBatch("""
MERGE INTO E_DTB_RQT USING 
    (VALUES (?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)) AS src 
    (ID_DTB_RQT,VA_HST,CD_PRT,VA_NAM,VA_SCH,DH_STR,DH_END,VA_USR,VA_THR,VA_DRV,VA_PRD_NAM,VA_PRD_VRS,VA_CMD,VA_FAIL,CD_PRN_SES,CD_INS)
    ON (E_DTB_RQT.ID_DTB_RQT = src.ID_DTB_RQT)
    WHEN MATCHED THEN        
        UPDATE SET VA_HST = src.VA_HST, CD_PRT = src.CD_PRT, VA_NAM = src.VA_NAM,
            VA_SCH = src.VA_SCH, DH_STR = src.DH_STR, DH_END = src.DH_END, VA_USR = src.VA_USR,
            VA_THR = src.VA_THR, VA_DRV = src.VA_DRV, VA_PRD_NAM = src.VA_PRD_NAM,
            VA_PRD_VRS = src.VA_PRD_VRS, VA_CMD = src.VA_CMD, VA_FAIL = src.VA_FAIL
        WHEN NOT MATCHED THEN
            INSERT(ID_DTB_RQT,VA_HST,CD_PRT,VA_NAM,VA_SCH,DH_STR,DH_END,VA_USR,VA_THR,VA_DRV,VA_PRD_NAM,VA_PRD_VRS,VA_CMD,VA_FAIL,CD_PRN_SES,CD_INS)
            VALUES (src.ID_DTB_RQT, src.VA_HST, src.CD_PRT, src.VA_NAM, src.VA_SCH, src.DH_STR, src.DH_END, src.VA_USR, src.VA_THR, src.VA_DRV, src.VA_PRD_NAM, src.VA_PRD_VRS, src.VA_CMD, src.VA_FAIL, src.CD_PRN_SES, src.CD_INS)
""", requests.iterator(), (ps, req) -> {
            ps.setObject(1, req.getId());
            ps.setString(2, req.getHost());
            ps.setInt(3, req.getPort());
            ps.setString(4, req.getName());
            ps.setString(5, req.getSchema());
            ps.setTimestamp(6, fromNullableInstant(req.getStart()));
            ps.setTimestamp(7, fromNullableInstant(req.getEnd()));
            ps.setString(8, req.getUser());
            ps.setString(9, req.getThreadName());
            ps.setString(10, req.getDriverVersion());
            ps.setString(11, req.getProductName());
            ps.setString(12, req.getProductVersion());
            ps.setString(13, req.getCommand());
            ps.setBoolean(14, req.isFailed());
            ps.setObject(15, req.getSessionId());
            ps.setObject(16, req.getInstanceId());
        });
        return Stream.of(rows).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveHttpRequestStages(List<HttpRequestStageWrapper> stages) {
        var arr = executeBatch("INSERT INTO E_RST_RQT_STG(VA_NAM,DH_STR,DH_END,CD_ORD,CD_RST_RQT) VALUES(?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setObject(5, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.REST);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveHttpSessionStages(List<HttpSessionStageWrapper> stages) {
        var arr = executeBatch("INSERT INTO E_RST_SES_STG(VA_NAM,DH_STR,DH_END,CD_ORD,CD_PRN_SES) VALUES(?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setObject(5, stage.getRequestId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveMailRequestStages(List<MailRequestStageWrapper> stages) {
        var arr = executeBatch("INSERT INTO E_SMTP_STG(VA_NAM,DH_STR,DH_END,CD_ORD,CD_SMTP_RQT) VALUES(?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setObject(5, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.SMTP);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveFtpRequestStages(List<FtpRequestStageWrapper> stages) {
        var arr = executeBatch("INSERT INTO E_FTP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_FTP_RQT) VALUES(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
            ps.setInt(5, stage.getOrder());
            ps.setObject(6, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.FTP);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveLdapRequestStages(List<NamingRequestStageWrapper> stages) {
        var arr = executeBatch("INSERT INTO E_LDAP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_LDAP_RQT) VALUES(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
            ps.setInt(5, stage.getOrder());
            ps.setObject(6, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.LDAP);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveDatabaseRequestStages(List<DatabaseRequestStageWrapper> stages) {
        var arr = executeBatch("INSERT INTO E_DTB_STG(VA_NAM,DH_STR,DH_END,VA_CNT,VA_CMD,CD_ORD,CD_DTB_RQT) VALUES(?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, valueOfNullableArray(stage.getCount()));
            ps.setString(5,valueOfNullableList(stage.getCommands()));
            ps.setInt(6, stage.getOrder());
            ps.setObject(7, stage.getRequestId());
        });
        saveExceptions(stages, RequestMask.JDBC);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private void saveUserActions(List<MainSessionWrapper> sessions) {
        if(sessions.stream().anyMatch(s-> !isEmpty(s.getUserActions()))) {
            var inc = new AtomicLong(selectMaxId("E_USR_ACN", "ID_ACN"));
            executeBatch("""
INSERT INTO E_USR_ACN(ID_ACN,VA_TYP,DH_STR,VA_NAM,VA_NDE_NAM,CD_PRN_SES)
VALUES(?,?,?,?,?,?::uuid)""", treeIterator(sessions, MainSessionWrapper::getUserActions), (ps, action) -> {
                ps.setLong(1, inc.incrementAndGet());
                ps.setString(2, action.getType());
                ps.setTimestamp(3, fromNullableInstant(action.getStart()));
                ps.setString(4, action.getName());
                ps.setString(5, action.getNodeName());
                ps.setString(6, action.getCdSession());
            });
        }
    }

    private void saveExceptions(List<? extends Wrapper<? extends AbstractStage>> stages, RequestMask mask) {
        if(!isEmpty(stages)) {
            executeBatch("INSERT INTO E_EXC_INF(VA_TYP,VA_ERR_TYP,VA_ERR_MSG,VA_STK,CD_ORD,CD_RQT) VALUES(?,?,?,?::json,?,?::uuid)",
                    stages.stream().map(Wrapper::unwrap).filter(e -> nonNull(e.getException())).iterator(), (ps, exp) -> {
                ps.setString(1, mask.name());
                ps.setString(2, exp.getException().getType());
                ps.setString(3, exp.getException().getMessage());
                try {
                    ps.setObject(4, exp.getException().getStackTraceRows() != null ? mapper.writeValueAsString(exp.getException().getStackTraceRows()) : null);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                ps.setObject(5, exp.getOrder(), Types.INTEGER);
                ps.setString(6, exp.getRequestId());
            });
        }
    }

    private void saveLocalRequestExceptions(List<LocalRequestWrapper> stages) {
        if(!isEmpty(stages)) {
            executeBatch("INSERT INTO E_EXC_INF(VA_TYP,VA_ERR_TYP,VA_ERR_MSG,VA_STK,CD_ORD,CD_RQT) VALUES(?,?,?,?::json,?,?::uuid)",
                    stages.stream().filter(e -> nonNull(e.getException())).iterator(), (ps, exp) -> {
                        ps.setString(1, LOCAL.name());
                        ps.setString(2, exp.getException().getType());
                        ps.setString(3, exp.getException().getMessage());
                        try {
                            ps.setObject(4, exp.getException().getStackTraceRows() != null ? mapper.writeValueAsString(exp.getException().getStackTraceRows()) : null);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        ps.setObject(5, 1, Types.INTEGER);
                        ps.setString(6, exp.getId());
                    });
        }
    }

    // TODO use RequestQueryBuilder
    private long selectMaxId(String table, String column) {
        return template.queryForObject(String.format("SELECT COALESCE(MAX(%s),0) FROM %s", column, table), Long.class);
    }

    public List<String> selectChildsById(String id) {
        var query = "with recursive recusive(prnt,chld) as (" +
                " select ''::varchar as prnt, ? as chld " +
                " union all " +
                " select  recusive.chld, E_RST_RQT.CD_RMT_SES " +
                " from E_RST_RQT, recusive " +
                " where recusive.chld = E_RST_RQT.CD_PRN_SES " +
                ") select distinct(chld) from recusive";
        return template.query(query, (ResultSet rs, int rowNum) -> (rs.getString("chld")), id).stream().filter(Objects::nonNull).toList();
    }



    private <T> Integer executeBatch(String sql, Iterator<T> it, ParameterizedPreparedStatementSetter<T> pss) {
        return template.execute(sql, (PreparedStatement ps)->{
            long rows = 0;
            var n = 0;
            while(it.hasNext()) {
                pss.setValues(ps, it.next());
                ps.addBatch();
                if(++n % BATCH_SIZE == 0) {
                    rows += IntStream.of(ps.executeBatch()).sum();
                }
            }
            if(n % BATCH_SIZE != 0) {
                rows += IntStream.of(ps.executeBatch()).sum();
            }
            log.debug("{} batch added, {} rows inserted", n, rows);
            return n;
        });
    }

    private static <T, U extends T> long filterAndSave(Collection<T> c, Class<U> classe, ToIntFunction<List<U>> saveFn) {
        var list = c.stream()
                .filter(classe::isInstance)
                .map(classe::cast)
                .toList();
        return list.isEmpty() ? 0 : saveFn.applyAsInt(list);
    }
}

