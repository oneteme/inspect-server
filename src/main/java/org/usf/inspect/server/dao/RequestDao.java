package org.usf.inspect.server.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import org.usf.inspect.server.RequestMask;
import org.usf.inspect.server.model.object.*;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.usf.inspect.server.RequestMask.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RequestDao {

    private final JdbcTemplate template;

    public void saveInstanceEnvironment(InstanceEnvironment instance) {
        template.update("""
                INSERT INTO E_ENV_INS(ID_INS,VA_TYP,DH_STR,VA_APP,VA_VRS,VA_ADR,VA_ENV,VA_OS,VA_RE,VA_USR,VA_CLR)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)""", ps -> {
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
        });
    }

    public long saveSessions(List<Session> sessions) {
        var rs = filterAndSave(sessions, RestSession.class, this::saveRestSessions);
        var ms = filterAndSave(sessions, MainSession.class, this::saveMainSessions);
        saveRestRequests(sessions);
        saveLocalRequests(sessions);
        saveMailRequests(sessions);
        saveDatabaseRequests(sessions);
        saveFtpRequests(sessions);
        saveLdapRequests(sessions);
        return rs + ms;
    }

    private int saveMainSessions(List<MainSession> reqList) {
    	 var arr = template.batchUpdate("""
                     INSERT INTO E_MAIN_SES(ID_SES,VA_NAM,VA_USR,DH_STR,DH_END,VA_TYP,VA_LCT,VA_THR,VA_ERR_TYP,VA_ERR_MSG,VA_MSK,CD_INS)
                     VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""", reqList, reqList.size(), (ps, o) -> {
            var exp = o.getException();
            ps.setString(1, o.getId());
            ps.setString(2, o.getName());
            ps.setString(3, o.getUser());
            ps.setTimestamp(4, fromNullableInstant(o.getStart()));
            ps.setTimestamp(5, fromNullableInstant(o.getEnd()));
            ps.setString(6, valueOfNullable(o.getType()));
            ps.setString(7, o.getLocation());
            ps.setString(8, o.getThreadName());
            ps.setString(9, nonNull(exp) ? exp.getType() : null);
            ps.setString(10, nonNull(exp) ? exp.getMessage() : null);
            ps.setInt(11, mask(o));
            ps.setString(12, o.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveRestSessions(List<RestSession> reqList) {
        var arr = template.batchUpdate("""
                    INSERT INTO E_RST_SES(ID_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,VA_ERR_TYP,VA_ERR_MSG,VA_NAM,VA_USR,VA_USR_AGT,VA_CCH_CTR,VA_MSK,CD_INS)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", reqList, reqList.size(), (ps, o) -> {
            var exp = o.getException();
            ps.setString(1, o.getId()); ps.getConnection()
            ps.setString(2, o.getMethod());
            ps.setString(3, o.getProtocol());
            ps.setString(4, o.getHost());
            ps.setInt(5, o.getPort());
            ps.setString(6, o.getPath());
            ps.setString(7, o.getQuery());
            ps.setString(8, o.getContentType());
            ps.setString(9, o.getAuthScheme());
            ps.setInt(10, o.getStatus());
            ps.setLong(11, o.getInDataSize());
            ps.setLong(12, o.getOutDataSize());
            ps.setString(13, o.getInContentEncoding());
            ps.setString(14, o.getOutContentEncoding());
            ps.setTimestamp(15, fromNullableInstant(o.getStart()));
            ps.setTimestamp(16, fromNullableInstant(o.getEnd()));
            ps.setString(17, o.getThreadName());
            ps.setString(18, nonNull(exp) ? exp.getType() : null);
            ps.setString(19, nonNull(exp) ? exp.getMessage() : null);
            ps.setString(20, o.getName());
            ps.setString(21, o.getUser());
            ps.setString(22, o.getUserAgent());
            ps.setString(23, o.getCacheControl());
            ps.setInt(24, mask(o));
            ps.setString(25, o.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    interface BatchParameterSetter {
        int setParameters(PreparedStatement ps) throws SQLException;
    }

    private void executeBatch(String sql, BatchParameterSetter batch) {
        var cnx = DataSourceUtils.getConnection(template.getDataSource());
        try(var ps = cnx.prepareStatement(sql)){
            var idx = batch.setParameters(ps);
            log.debug(sql + ", batch size {}", idx == 0 ? 0 : IntStream.of(ps.executeBatch()).sum());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveRestRequests(List<Session> sessions) {
        var exceptions = new ArrayList<ExceptionInfo>();
        var inc = new AtomicLong(selectMaxId("E_RST_RQT", "ID_RST_RQT"));
        String sql = """
INSERT INTO E_RST_RQT(ID_RST_RQT,CD_RMT_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,CD_PRN_SES)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""";
        executeBatch(sql, ps-> {
            var size = 0;
            for (var session : sessions) {
                if (session.getRestRequests() != null) {
                    for (var request : session.getRestRequests()) {
                        ps.setLong(1, inc.incrementAndGet());
                        ps.setString(2, request.getId());
                        ps.setString(3, request.getMethod());
                        ps.setString(4, request.getProtocol());
                        ps.setString(5, request.getHost());
                        ps.setInt(6, request.getPort());
                        ps.setString(7, request.getPath());
                        ps.setString(8, request.getQuery());
                        ps.setString(9, request.getContentType());
                        ps.setString(10, request.getAuthScheme());
                        ps.setInt(11, request.getStatus());
                        ps.setLong(12, request.getInDataSize());
                        ps.setLong(13, request.getOutDataSize());
                        ps.setString(14, request.getInContentEncoding());
                        ps.setString(15, request.getOutContentEncoding());
                        ps.setTimestamp(16, fromNullableInstant(request.getStart()));
                        ps.setTimestamp(17, fromNullableInstant(request.getEnd()));
                        ps.setString(18, request.getThreadName());
                        ps.setString(19, request.getCdSession());
                        if (request.getException() != null) {
                            request.getException().setIdRequest(inc.get());
                            exceptions.add(request.getException());
                        }
                        ps.addBatch();
                        size++;
                    }
                }
            }
            return size;
        });
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, REST);
        }
    }

    public void saveLocalRequests(List<Session> sessions){
        var exceptions = new ArrayList<ExceptionInfo>();
        var inc = new AtomicLong(selectMaxId("E_LCL_RQT", "ID_LCL_RQT"));
        template.batchUpdate("""
                INSERT INTO E_LCL_RQT(ID_LCL_RQT,VA_NAM,VA_LCT,DH_STR,DH_END,VA_USR,VA_THR,VA_STT,CD_PRN_SES)
                VALUES(?,?,?,?,?,?,?,?,?)""", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                for (Session session: sessions) {
                    if(session.getLocalRequests() != null) {
                        for (LocalRequest request: session.getLocalRequests()) {
                            var completed = isNull(request.getException());
                            ps.setLong(1, inc.incrementAndGet());
                            ps.setString(2,request.getName());
                            ps.setString(3,request.getLocation());
                            ps.setTimestamp(4,fromNullableInstant(request.getStart()));
                            ps.setTimestamp(5,fromNullableInstant(request.getEnd()));
                            ps.setString(6,request.getUser());
                            ps.setString(7,request.getThreadName());
                            ps.setBoolean(8, completed);
                            ps.setString(9,request.getCdSession());

                            if(request.getException() != null) {
                                request.getException().setIdRequest(inc.get());
                                exceptions.add(request.getException());
                            }

                            ps.addBatch();
                        }
                    }
                }
            }

            @Override
            public int getBatchSize() {
                var size  = 0;
                for (Session session : sessions) {
                    size += session.getLocalRequests() != null ? session.getLocalRequests().size() : 0;
                }
                return size;
            }
        });

        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, LOCAL);
        }
    }

    public void saveMailRequests(List<Session> sessions) {
        var inc = new AtomicLong(selectMaxId("E_SMTP_RQT", "ID_SMTP_RQT"));
        template.batchUpdate("""
                INSERT INTO E_SMTP_RQT(ID_SMTP_RQT,VA_HST,CD_PRT,VA_USR,DH_STR,DH_END,VA_THR,VA_STT,CD_PRN_SES)
                VALUES(?,?,?,?,?,?,?,?,?)""", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                for (Session session: sessions) {
                    if(session.getMailRequests() != null) {
                        for (MailRequest request : session.getMailRequests()) {
                            var completed = request.getActions() == null || request.getActions().stream().allMatch(a-> isNull(a.getException()));
                            ps.setLong(1, inc.incrementAndGet());
                            ps.setString(2, request.getHost());
                            ps.setInt(3, request.getPort());
                            ps.setString(4, request.getUser());
                            ps.setTimestamp(5, fromNullableInstant(request.getStart()));
                            ps.setTimestamp(6, fromNullableInstant(request.getEnd()));
                            ps.setString(7, request.getThreadName());
                            ps.setBoolean(8, completed);
                            ps.setString(9, request.getCdSession());
                            request.setId(inc.get());
                            ps.addBatch();
                        }
                    }
                }
            }

            @Override
            public int getBatchSize() {
                var size  = 0;
                for (Session session : sessions) {
                    size += session.getMailRequests() != null ? session.getMailRequests().size() : 0;
                }
                return size;
            }
        });
        saveMailRequestStages(sessions);
        saveMailRequestMails(sessions);
    }

    private void saveMailRequestStages(List<Session> sessions) {
        var exceptions = new ArrayList<ExceptionInfo>();
        template.batchUpdate("INSERT INTO E_SMTP_STG(VA_NAM,DH_STR,DH_END,CD_ORD,CD_SMTP_RQT) VALUES(?,?,?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        for (Session session: sessions) {
                            if(session.getMailRequests() != null) {
                                for (MailRequest request : session.getMailRequests()) {
                                    if(request.getActions() != null) {
                                        var inc = new AtomicInteger(0);
                                        for (MailRequestStage stage: request.getActions()) {
                                            ps.setString(1, stage.getName());
                                            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
                                            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
                                            ps.setInt(3, inc.incrementAndGet());
                                            ps.setLong(4, request.getId());
                                            if(stage.getException() != null) {
                                                stage.getException().setIdRequest(request.getId());
                                                stage.getException().setOrder(inc.get());
                                                exceptions.add(stage.getException());
                                            }
                                            ps.addBatch();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        var size  = 0;
                        for (Session session : sessions) {
                            if(session.getMailRequests() != null) {
                                for (MailRequest request : session.getMailRequests()) {
                                    size += request.getActions() != null ? request.getActions().size() : 0;
                                }
                            }

                        }
                        return size;
                    }
                });

        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, SMTP);
        }
    }

    private void saveMailRequestMails(List<Session> sessions) {
        template.batchUpdate("INSERT INTO E_SMTP_MAIL(VA_SBJ,VA_CNT_TYP,VA_FRM,VA_RCP,VA_RPL,VA_SZE,CD_SMTP_RQT) VALUES(?,?,?,?,?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        for (Session session: sessions) {
                            if(session.getMailRequests() != null) {
                                for (MailRequest request : session.getMailRequests()) {
                                    if(request.getMails() != null) {
                                        for (Mail mail: request.getMails()) {
                                            ps.setString(1, mail.getSubject());
                                            ps.setString(2, mail.getContentType());
                                            ps.setString(3, mail.getFrom() != null ? String.join(", ", mail.getFrom()) : null);
                                            ps.setString(4, mail.getRecipients() != null ? String.join(", ", mail.getRecipients()) : null);
                                            ps.setString(5, mail.getReplyTo() != null ? String.join(", ", mail.getReplyTo()) : null);
                                            ps.setInt(6,mail.getSize());
                                            ps.setLong(7, mail.getIdRequest());
                                            ps.addBatch();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        var size  = 0;
                        for (Session session : sessions) {
                            if(session.getMailRequests() != null) {
                                for (MailRequest request : session.getMailRequests()) {
                                    size += request.getMails() != null ? request.getMails().size() : 0;
                                }
                            }
                        }
                        return size;
                    }
                });

    }

    private void saveExceptions(List<ExceptionInfo> exceptionList, RequestMask mask) {
        template.batchUpdate("INSERT INTO E_EXC_INF(VA_TYP,VA_ERR_TYP,VA_ERR_MSG,CD_ORD,CD_RQT) VALUES(?,?,?,?,?)",
                exceptionList,
                exceptionList.size(),
                (ps, o) -> {
                    ps.setString(1, mask.name());
                    ps.setString(2, o.getType());
                    ps.setString(3, o.getMessage());
                    ps.setInt(4, o.getOrder());
                    ps.setLong(5, o.getIdRequest());
                });
    }

    public void saveFtpRequests(List<Session> sessions){
        var inc = new AtomicLong(selectMaxId("E_FTP_RQT", "ID_FTP_RQT"));
        template.batchUpdate("""
                INSERT INTO E_FTP_RQT(ID_FTP_RQT,VA_HST,CD_PRT,VA_PCL,VA_SRV_VRS,VA_CLT_VRS,VA_USR,DH_STR,DH_END,VA_THR,VA_STT,CD_PRN_SES)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                for (Session session: sessions) {
                    if(session.getFtpRequests() != null) {
                        for (FtpRequest request : session.getFtpRequests()) {
                            var completed = request.getActions() == null || request.getActions().stream().allMatch(a -> isNull(a.getException()));
                            ps.setLong(1, inc.incrementAndGet());
                            ps.setString(2, request.getHost());
                            ps.setInt(3, request.getPort());
                            ps.setString(4, request.getProtocol());
                            ps.setString(5, request.getServerVersion());
                            ps.setString(6, request.getClientVersion());
                            ps.setString(7, request.getUser());
                            ps.setTimestamp(8, fromNullableInstant(request.getStart()));
                            ps.setTimestamp(9, fromNullableInstant(request.getEnd()));
                            ps.setString(10, request.getThreadName());
                            ps.setBoolean(11, completed);
                            ps.setString(12, request.getCdSession());
                            request.setId(inc.get());
                            ps.addBatch();
                        }
                    }
                }
            }

            @Override
            public int getBatchSize() {
                var size  = 0;
                for (Session session : sessions) {
                    size += session.getFtpRequests() != null ? session.getFtpRequests().size() : 0;
                }
                return size;
            }
        });
        saveFtpRequestStages(sessions);
    }

    private void saveFtpRequestStages(List<Session> sessions) {
        var exceptions = new ArrayList<ExceptionInfo>();
        template.batchUpdate("INSERT INTO E_FTP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_FTP_RQT) VALUES(?,?,?,?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        for(Session session: sessions) {
                            if(session.getFtpRequests() != null) {
                                for (FtpRequest request : session.getFtpRequests()) {
                                    if(request.getActions() != null) {
                                        var inc = new AtomicInteger(0);
                                        for(FtpRequestStage stage : request.getActions()) {
                                            ps.setString(1, stage.getName());
                                            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
                                            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
                                            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
                                            ps.setInt(5, inc.incrementAndGet());
                                            ps.setLong(6, request.getId());
                                            if(stage.getException() != null) {
                                                stage.getException().setIdRequest(request.getId());
                                                stage.getException().setOrder(inc.get());
                                                exceptions.add(stage.getException());
                                            }
                                            ps.addBatch();
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        var size  = 0;
                        for (Session session : sessions) {
                            if(session.getFtpRequests() != null) {
                                for (FtpRequest request : session.getFtpRequests()) {
                                    size += request.getActions() != null ? request.getActions().size() : 0;
                                }
                            }
                        }
                        return size;
                    }
                });
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, FTP);
        }
    }

    public void saveLdapRequests(List<Session> sessions) {
        var inc = new AtomicLong(selectMaxId("E_LDAP_RQT", "ID_LDAP_RQT"));
        template.batchUpdate("""
                    INSERT INTO E_LDAP_RQT(ID_LDAP_RQT,VA_HST,CD_PRT,VA_PCL,VA_USR,DH_STR,DH_END,VA_THR,VA_STT,CD_PRN_SES)
                    VALUES(?,?,?,?,?,?,?,?,?,?)""",  new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                for(Session session: sessions) {
                    if(session.getLdapRequests() != null) {
                        for (NamingRequest request : session.getLdapRequests()) {
                            var completed = request.getActions().stream().allMatch(a-> isNull(a.getException()));
                            ps.setLong(1, inc.incrementAndGet());
                            ps.setString(2, request.getHost());
                            ps.setInt(3, request.getPort());
                            ps.setString(4, request.getProtocol());
                            ps.setString(5, request.getUser());
                            ps.setTimestamp(6, fromNullableInstant(request.getStart()));
                            ps.setTimestamp(7, fromNullableInstant(request.getEnd()));
                            ps.setString(8, request.getThreadName());
                            ps.setBoolean(9, completed);
                            ps.setString(10, request.getCdSession());
                            request.setId(inc.get());
                            ps.addBatch();
                        }
                    }
                }
            }

            @Override
            public int getBatchSize() {
                var size  = 0;
                for (Session session : sessions) {
                    size += session.getLdapRequests() != null ? session.getLdapRequests().size() : 0;
                }
                return size;
            }
        });
        saveLdapRequestStages(sessions);
    }

    private void saveLdapRequestStages(List<Session> sessions) {
        var exceptions = new ArrayList<ExceptionInfo>();
        template.batchUpdate("INSERT INTO E_LDAP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_LDAP_RQT) VALUES(?,?,?,?,?,?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                for(Session session: sessions) {
                    if(session.getLdapRequests() != null) {
                        for (NamingRequest request : session.getLdapRequests()) {
                            if(request.getActions() != null) {
                                var inc = new AtomicInteger(0);
                                for (NamingRequestStage stage : request.getActions()) {
                                    ps.setString(1, stage.getName());
                                    ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
                                    ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
                                    ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
                                    ps.setInt(5, inc.incrementAndGet());
                                    ps.setLong(6, request.getId());
                                    if(stage.getException() != null) {
                                        stage.getException().setIdRequest(request.getId());
                                        stage.getException().setOrder(inc.get());
                                        exceptions.add(stage.getException());
                                    }
                                    ps.addBatch();
                                }
                            }

                        }
                    }

                }
            }

            @Override
            public int getBatchSize() {
                var size  = 0;
                for (Session session : sessions) {
                    if(session.getLdapRequests() != null) {
                        for (NamingRequest request : session.getLdapRequests()) {
                            size += request.getActions() != null ? request.getActions().size() : 0;
                        }
                    }
                }
                return size;
            }
        });
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, LDAP);
        }
    }

    public void saveDatabaseRequests(List<Session> sessions) {
        var inc = new AtomicLong(selectMaxId("E_DTB_RQT", "ID_DTB_RQT"));
        template.batchUpdate("""
                INSERT INTO E_DTB_RQT(ID_DTB_RQT,VA_HST,CD_PRT,VA_NAM,VA_SCH,DH_STR,DH_END,VA_USR,VA_THR,VA_DRV,VA_PRD_NAM,VA_PRD_VRS,VA_CMD,VA_STT,CD_PRN_SES)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                for(Session session: sessions) {
                    if(session.getDatabaseRequests() != null) {
                        for (DatabaseRequest request : session.getDatabaseRequests()) {
                            var completed = request.getActions().stream().allMatch(a-> isNull(a.getException()));
                            ps.setLong(1, inc.incrementAndGet());
                            ps.setString(2, request.getHost());
                            ps.setInt(3, request.getPort());
                            ps.setString(4, request.getName());
                            ps.setString(5, request.getSchema());
                            ps.setTimestamp(6, fromNullableInstant(request.getStart()));
                            ps.setTimestamp(7, fromNullableInstant(request.getEnd()));
                            ps.setString(8, request.getUser());
                            ps.setString(9, request.getThreadName());
                            ps.setString(10, request.getDriverVersion());
                            ps.setString(11, request.getProductName());
                            ps.setString(12, request.getProductVersion());
                            ps.setString(13, valueOfNullableList(request.getCommands()));
                            ps.setBoolean(14, completed);
                            ps.setString(15, request.getCdSession());
                            request.setId(inc.get());
                            ps.addBatch();
                        }
                    }
                }
            }

            @Override
            public int getBatchSize() {
                var size  = 0;
                for (Session session : sessions) {
                    size += session.getDatabaseRequests() != null ? session.getDatabaseRequests().size() : 0;
                }
                return size;
            }
        });
        saveDatabaseActions(sessions);
    }

    private void saveDatabaseActions(List<Session> sessions) {
        var exceptions = new ArrayList<ExceptionInfo>();
        template.batchUpdate("INSERT INTO E_DTB_STG(VA_NAM,DH_STR,DH_END,VA_CNT,CD_ORD,CD_DTB_RQT) VALUES(?,?,?,?,?,?)", new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                for(Session session: sessions) {
                    if(session.getDatabaseRequests() != null) {
                        for (DatabaseRequest request : session.getDatabaseRequests()) {
                            if(request.getActions() != null) {
                                var inc = new AtomicInteger(0);
                                for (DatabaseRequestStage stage : request.getActions()) {
                                    ps.setString(1, stage.getName());
                                    ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
                                    ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
                                    ps.setString(4, valueOfNullableArray(stage.getCount()));
                                    ps.setInt(5, inc.incrementAndGet());
                                    ps.setLong(6, request.getId());
                                    if(stage.getException() != null) {
                                        stage.getException().setIdRequest(request.getId());
                                        stage.getException().setOrder(inc.get());
                                        exceptions.add(stage.getException());
                                    }
                                    ps.addBatch();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public int getBatchSize() {
                var size  = 0;
                for (Session session : sessions) {
                    if(session.getDatabaseRequests() != null) {
                        for (DatabaseRequest request : session.getDatabaseRequests()) {
                            size += request.getActions() != null ? request.getActions().size() : 0;
                        }
                    }
                }
                return size;
            }
        });
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, JDBC);
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

    private static <T, U extends T> long filterAndSave(Collection<T> c, Class<U> classe, ToIntFunction<List<U>> saveFn) {
        var list = c.stream()
                .filter(classe::isInstance)
                .map(classe::cast)
                .toList();
        return list.isEmpty() ? 0 : saveFn.applyAsInt(list);
    }

    private static Timestamp fromNullableInstant(Instant instant) {
        return nonNull(instant) ? Timestamp.from(instant) : null;
    }

    private static String valueOfNullable(Object o) {// do not use Objects::toString
        return nonNull(o) ? o.toString() : null;
    }

    private static <T extends Enum<T>> String valueOfNullableList(List<T> enumList) {
        return nonNull(enumList)
                ? enumList.stream().filter(Objects::nonNull).map(Enum::name).collect(joining(","))
                : null;
    }
    
    private static String  valueOfNullableArray(long[]array){
        return nonNull(array)
        		? LongStream.of(array).mapToObj(Long::toString).collect(joining(","))
				: null;
    }
}
