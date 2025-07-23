package org.usf.inspect.server.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.usf.inspect.core.EventTrace;
import org.usf.inspect.server.model.*;

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
    public void saveInstanceTrace(InstanceTrace instanceTrace) {
        template.update("""
INSERT INTO E_INS_TRC (VA_PND, VA_ATP, VA_SES_SZE, DH_STR, CD_INS)
VALUES (?, ?, ?, ?, ?::uuid);""", ps -> {
            ps.setInt(1, instanceTrace.getPending());
            ps.setInt(2, instanceTrace.getAttempts());
            ps.setInt(3, instanceTrace.getSessionLength());
            ps.setTimestamp(4, fromNullableInstant(instanceTrace.getInstant()));
            ps.setString(5, instanceTrace.getInstanceId());
        });
    }

    public int saveLogEntry(List<LogEntry> logEntries) {
        var arr = executeBatch("""
INSERT INTO E_LOG_ENT(VA_LVL,VA_MSG,DH_STR,CD_SES,CD_INS)
VALUES (?,?,?,?::uuid,?::uuid)""", logEntries.iterator(), (ps, o)-> {
            ps.setString(1, String.valueOf(o.getLevel()));
            ps.setString(2, o.getMessage());
            ps.setTimestamp(3, fromNullableInstant(o.getInstant()));
            ps.setString(4, o.getSessionId());
            ps.setString(5, o.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    public int saveMachineResourceUsage(List<MachineResourceUsage> usages) {
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

    public void updateInstanceEnvironment(Instant end, String instanceId){
        template.update("UPDATE E_ENV_INS SET DH_END = ? WHERE ID_INS = ?", ps -> {
            ps.setTimestamp(1,fromNullableInstant(end));
            ps.setString(2,instanceId);
        });
    }

    public long saveTraceables(List<EventTrace> eventTraces) {
        var ms = filterAndSave(eventTraces, MainSession.class, this::saveMainSessions);
        var rs = filterAndSave(eventTraces, RestSession.class, this::saveRestSessions);
        var rr = filterAndSave(eventTraces, RestRequest.class, this::saveRestRequests);
        var lr = filterAndSave(eventTraces, LocalRequest.class, this::saveLocalRequests);
        var mr = filterAndSave(eventTraces, MailRequest.class, this::saveMailRequests);
        var fr = filterAndSave(eventTraces, FtpRequest.class, this::saveFtpRequests);
        var nr = filterAndSave(eventTraces, NamingRequest.class, this::saveLdapRequests);
        var dr = filterAndSave(eventTraces, DatabaseRequest.class, this::saveDatabaseRequests);
        var hrs = filterAndSave(eventTraces, HttpRequestStage.class, this::saveHttpRequestStages);
        var mrs = filterAndSave(eventTraces, MailRequestStage.class, this::saveMailRequestStages);
        var frs = filterAndSave(eventTraces, FtpRequestStage.class, this::saveFtpRequestStages);
        var nrs = filterAndSave(eventTraces, NamingRequestStage.class, this::saveLdapRequestStages);
        var drs = filterAndSave(eventTraces, DatabaseRequestStage.class, this::saveDatabaseRequestStages);
        var mmr = filterAndSave(eventTraces, MachineResourceUsage.class, this::saveMachineResourceUsage);
        var log = filterAndSave(eventTraces, LogEntry.class, this::saveLogEntry);
        return ms + rs + rr + lr + mr + fr + nr + dr + hrs + mrs + frs + nrs + drs + mmr + log;
    }

    private int saveRestSessions(List<RestSession> sessions) {
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
            ps.setInt(24, mask(ses));
            ps.setObject(25, ses.getInstanceId());
            //ses.updateCdSession();
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveMainSessions(List<MainSession> sessions) {
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
            ps.setInt(11, mask(ses));
            ps.setObject(12, ses.getInstanceId());
           // ses.updateCdSession();
        });
        saveUserActions(sessions);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveRestRequests(List<RestRequest> requests) {
        var arr = executeBatch("""
INSERT INTO E_RST_RQT(ID_RST_RQT,CD_RMT_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,VA_BDY_CNT,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getIdRequest());
            ps.setObject(2, req.getId());
            ps.setString(3, req.getMethod());
            ps.setString(4, req.getProtocol());
            ps.setString(5, req.getHost());
            ps.setInt(6, req.getPort());
            ps.setString(7, req.getPath());
            ps.setString(8, req.getQuery());
            ps.setString(9, contentTypeExtract(req.getContentType()));
            ps.setString(10, req.getAuthScheme());
            ps.setInt(11, req.getStatus());
            ps.setLong(12, req.getInDataSize());
            ps.setLong(13, req.getOutDataSize());
            ps.setString(14, req.getInContentEncoding());
            ps.setString(15, req.getOutContentEncoding());
            ps.setTimestamp(16, fromNullableInstant(req.getStart()));
            ps.setTimestamp(17, fromNullableInstant(req.getEnd()));
            ps.setString(18, req.getThreadName());
            ps.setString(19, req.getBodyContent());
            ps.setObject(20, req.getCdSession());
            ps.setObject(21, req.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveLocalRequests(List<LocalRequest> requests){

        var arr = executeBatch("""
INSERT INTO E_LCL_RQT(ID_LCL_RQT,VA_NAM,VA_LCT,DH_STR,DH_END,VA_USR,VA_THR,VA_FAIL,CD_PRN_SES,VA_TYP,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?::uuid,?,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getIdRequest());
            ps.setString(2,req.getName());
            ps.setString(3,req.getLocation());
            ps.setTimestamp(4,fromNullableInstant(req.getStart()));
            ps.setTimestamp(5,fromNullableInstant(req.getEnd()));
            ps.setString(6,req.getUser());
            ps.setString(7,req.getThreadName());
            ps.setBoolean(8, isNull(req.getException()));
            ps.setObject(9, req.getCdSession());
            ps.setString(10, req.getType());
            ps.setObject(11, req.getInstanceId());
        });
        saveExceptions(requests.stream()
                .map(LocalRequest::getException)
                .filter(Objects::nonNull)
                .toList(), LOCAL);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveMailRequests(List<MailRequest> requests) {
        var arr = executeBatch("""
INSERT INTO E_SMTP_RQT(ID_SMTP_RQT,VA_HST,CD_PRT,VA_USR,DH_STR,DH_END,VA_THR,VA_FAIL,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getIdRequest());
            ps.setString(2, req.getHost());
            ps.setInt(3, req.getPort());
            ps.setString(4, req.getUser());
            ps.setTimestamp(5, fromNullableInstant(req.getStart()));
            ps.setTimestamp(6, fromNullableInstant(req.getEnd()));
            ps.setString(7, req.getThreadName());
            ps.setBoolean(8, req.isFailed());
            ps.setObject(9, req.getCdSession());
            ps.setObject(10, req.getInstanceId());
            // req.updateIdRequest();
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private void saveMailRequestMails(List<MailRequest> requests) {
        executeBatch("INSERT INTO E_SMTP_MAIL(VA_SBJ,VA_CNT_TYP,VA_FRM,VA_RCP,VA_RPL,VA_SZE,CD_SMTP_RQT) VALUES(?,?,?,?,?,?,?::uuid)", treeIterator(requests, MailRequest::getMails), (ps, mail)-> {
            ps.setString(1, mail.getSubject());
            ps.setString(2, mail.getContentType());
            ps.setString(3, mail.getFrom() != null ? String.join(", ", mail.getFrom()) : null);
            ps.setString(4, mail.getRecipients() != null ? String.join(", ", mail.getRecipients()) : null);
            ps.setString(5, mail.getReplyTo() != null ? String.join(", ", mail.getReplyTo()) : null);
            ps.setInt(6,mail.getSize());
            ps.setObject(7, mail.getIdRequest());
        });
    }

    private int saveFtpRequests(List<FtpRequest> requests){
        var arr = executeBatch("""
INSERT INTO E_FTP_RQT(ID_FTP_RQT,VA_HST,CD_PRT,VA_PCL,VA_SRV_VRS,VA_CLT_VRS,VA_USR,DH_STR,DH_END,VA_THR,VA_FAIL,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getIdRequest());
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
            ps.setObject(12, req.getCdSession());
            ps.setObject(13, req.getInstanceId());
            //req.updateIdRequest();
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveLdapRequests(List<NamingRequest> requests) {
        var arr = executeBatch("""
INSERT INTO E_LDAP_RQT(ID_LDAP_RQT,VA_HST,CD_PRT,VA_PCL,VA_USR,DH_STR,DH_END,VA_THR,VA_FAIL,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getIdRequest());
            ps.setString(2, req.getHost());
            ps.setInt(3, req.getPort());
            ps.setString(4, req.getProtocol());
            ps.setString(5, req.getUser());
            ps.setTimestamp(6, fromNullableInstant(req.getStart()));
            ps.setTimestamp(7, fromNullableInstant(req.getEnd()));
            ps.setString(8, req.getThreadName());
            ps.setBoolean(9, req.isFailed());
            ps.setObject(10, req.getCdSession());
            ps.setObject(11, req.getInstanceId());
            //req.updateIdRequest();
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveDatabaseRequests(List<DatabaseRequest> requests) {
        var arr = executeBatch("""
INSERT INTO E_DTB_RQT(ID_DTB_RQT,VA_HST,CD_PRT,VA_NAM,VA_SCH,DH_STR,DH_END,VA_USR,VA_THR,VA_DRV,VA_PRD_NAM,VA_PRD_VRS,VA_CMD,VA_FAIL,CD_PRN_SES,CD_INS)
VALUES(?::uuid,?,?,?,?,?,?,?,?,?,?,?,?,?,?::uuid,?::uuid)""", requests.iterator(), (ps, req)-> {
            ps.setObject(1, req.getIdRequest());
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
            ps.setString(13, req.mainCommand());
            ps.setBoolean(14, req.isFailed());
            ps.setObject(15, req.getCdSession());
            ps.setObject(16, req.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveHttpRequestStages(List<HttpRequestStage> stages) {
        var arr = executeBatch("INSERT INTO E_RST_STG(VA_NAM,DH_STR,DH_END,CD_ORD,CD_RST_RQT) VALUES(?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setObject(5, stage.getIdRequest());
        });
        saveExceptions(stages.stream()
                .map(HttpRequestStage::getException)
                .filter(Objects::nonNull)
                .toList(), REST);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveMailRequestStages(List<MailRequestStage> stages) {
        var arr = executeBatch("INSERT INTO E_SMTP_STG(VA_NAM,DH_STR,DH_END,CD_ORD,CD_SMTP_RQT) VALUES(?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setObject(5, stage.getIdRequest());
        });
        saveExceptions(stages.stream()
                .map(MailRequestStage::getException)
                .filter(Objects::nonNull)
                .toList(), SMTP);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveFtpRequestStages(List<FtpRequestStage> stages) {
        var arr = executeBatch("INSERT INTO E_FTP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_FTP_RQT) VALUES(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
            ps.setInt(5, stage.getOrder());
            ps.setObject(6, stage.getIdRequest());
        });
        saveExceptions(stages.stream()
                .map(FtpRequestStage::getException)
                .filter(Objects::nonNull)
                .toList(), FTP);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveLdapRequestStages(List<NamingRequestStage> stages) {
        var arr = executeBatch("INSERT INTO E_LDAP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_LDAP_RQT) VALUES(?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
            ps.setInt(5, stage.getOrder());
            ps.setObject(6, stage.getIdRequest());
        });
        saveExceptions(stages.stream()
                .map(NamingRequestStage::getException)
                .filter(Objects::nonNull)
                .toList(), LDAP);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveDatabaseRequestStages(List<DatabaseRequestStage> stages) {
        var arr = executeBatch("INSERT INTO E_DTB_STG(VA_NAM,DH_STR,DH_END,VA_CNT,VA_CMD,CD_ORD,CD_DTB_RQT) VALUES(?,?,?,?,?,?,?::uuid)", stages.iterator(), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, valueOfNullableArray(stage.getCount()));
            ps.setString(5,valueOfNullableList(stage.getCommands()));
            ps.setInt(6, stage.getOrder());
            ps.setObject(7, stage.getIdRequest());
        });
        saveExceptions(stages.stream()
                .map(DatabaseRequestStage::getException)
                .filter(Objects::nonNull)
                .toList(), JDBC);
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private void saveUserActions(List<MainSession> sessions) {
        if(sessions.stream().anyMatch(s-> !isEmpty(s.getUserActions()))) {
            var inc = new AtomicLong(selectMaxId("E_USR_ACN", "ID_ACN"));
            executeBatch("""
INSERT INTO E_USR_ACN(ID_ACN,VA_TYP,DH_STR,VA_NAM,VA_NDE_NAM,CD_PRN_SES)
VALUES(?,?,?,?,?,?::uuid)""", treeIterator(sessions, MainSession::getUserActions), (ps, action) -> {
                ps.setLong(1, inc.incrementAndGet());
                ps.setString(2, action.getType());
                ps.setTimestamp(3, fromNullableInstant(action.getStart()));
                ps.setString(4, action.getName());
                ps.setString(5, action.getNodeName());
                ps.setString(6, action.getCdSession());
            });
        }
    }

    private void saveExceptions(List<ExceptionInfo> exceptions, RequestMask mask) {
        if(!isEmpty(exceptions)) {
            executeBatch("INSERT INTO E_EXC_INF(VA_TYP,VA_ERR_TYP,VA_ERR_MSG,CD_ORD,CD_RQT) VALUES(?,?,?,?,?::uuid)", exceptions.iterator(), (ps, exp) -> {
                ps.setString(1, mask.name());
                ps.setString(2, exp.getType());
                ps.setString(3, exp.getMessage());
                ps.setObject(4, exp.getOrder(), Types.INTEGER);
                ps.setString(5, exp.getIdRequest());
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

