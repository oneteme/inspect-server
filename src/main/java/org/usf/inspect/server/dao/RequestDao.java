package org.usf.inspect.server.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.usf.inspect.server.RequestMask;
import org.usf.inspect.server.model.object.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
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
import static org.usf.inspect.server.TreeIterator.treeIterator;
import static org.usf.inspect.server.Utils.isEmpty;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RequestDao {

    private final JdbcTemplate template;
    
    private static final int BATCH_SIZE = 1_000;

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
        updateSessions(sessions);
        saveRestRequests(sessions);
        saveLocalRequests(sessions);
        saveMailRequests(sessions);
        saveDatabaseRequests(sessions);
        saveFtpRequests(sessions);
        saveLdapRequests(sessions);
        return rs + ms;
    }

    private static void updateSessions(List<Session> sessions) {
        for(var s : sessions) {
            if(s.getRestRequests() != null) {
                for(var r : s.getRestRequests()) {
                    r.setCdSession(s.getId());
                }
            }
            if(s.getLocalRequests() != null) {
                for(var r : s.getLocalRequests()) {
                    r.setCdSession(s.getId());
                }
            }
            if(s.getMailRequests() != null) {
                for(var r : s.getMailRequests()) {
                    r.setCdSession(s.getId());
                }
            }
            if(s.getFtpRequests() != null) {
                for(var r : s.getFtpRequests()) {
                    r.setCdSession(s.getId());
                }
            }
            if(s.getLdapRequests() != null) {
                for(var r : s.getLdapRequests()) {
                    r.setCdSession(s.getId());
                }
            }
            if(s.getDatabaseRequests() != null) {
                for(var r : s.getDatabaseRequests()) {
                    r.setCdSession(s.getId());
                }
            }
        }
    }

    private int saveMainSessions(List<MainSession> sessions) {
        var arr = executeBatch("""
INSERT INTO E_MAIN_SES(ID_SES,VA_NAM,VA_USR,DH_STR,DH_END,VA_TYP,VA_LCT,VA_THR,VA_ERR_TYP,VA_ERR_MSG,VA_MSK,CD_INS)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""", sessions.iterator(), (ps, ses) -> {
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
            ps.setInt(11, mask(ses));
            ps.setString(12, ses.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
    }

    private int saveRestSessions(List<RestSession> sessions) {
        var arr = executeBatch("""
INSERT INTO E_RST_SES(ID_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,VA_ERR_TYP,VA_ERR_MSG,VA_NAM,VA_USR,VA_USR_AGT,VA_CCH_CTR,VA_MSK,CD_INS)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", sessions.iterator(), (ps, ses) -> {
            var exp = ses.getException();
            ps.setString(1, ses.getId());
            ps.setString(2, ses.getMethod());
            ps.setString(3, ses.getProtocol());
            ps.setString(4, ses.getHost());
            ps.setInt(5, ses.getPort());
            ps.setString(6, ses.getPath());
            ps.setString(7, ses.getQuery());
            ps.setString(8, ses.getContentType());
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
            ps.setString(22, ses.getUserAgent());
            ps.setString(23, ses.getCacheControl());
            ps.setInt(24, mask(ses));
            ps.setString(25, ses.getInstanceId());
        });
        return Stream.of(arr).mapToInt(o-> IntStream.of(o).sum()).sum();
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

    public void saveRestRequests(List<Session> sessions) {
    	if(sessions.stream().anyMatch(s-> !isEmpty(s.getRestRequests()))){ //avoid exec select max
	        var exp = new ArrayList<ExceptionInfo>();
	        var inc = new AtomicLong(selectMaxId("E_RST_RQT", "ID_RST_RQT"));
	        executeBatch("""
INSERT INTO E_RST_RQT(ID_RST_RQT,CD_RMT_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,CD_PRN_SES)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", treeIterator(sessions, Session::getRestRequests), (ps, req)-> {
                ps.setLong(1, inc.incrementAndGet());
                ps.setString(2, req.getId());
                ps.setString(3, req.getMethod());
                ps.setString(4, req.getProtocol());
                ps.setString(5, req.getHost());
                ps.setInt(6, req.getPort());
                ps.setString(7, req.getPath());
                ps.setString(8, req.getQuery());
                ps.setString(9, req.getContentType());
                ps.setString(10, req.getAuthScheme());
                ps.setInt(11, req.getStatus());
                ps.setLong(12, req.getInDataSize());
                ps.setLong(13, req.getOutDataSize());
                ps.setString(14, req.getInContentEncoding());
                ps.setString(15, req.getOutContentEncoding());
                ps.setTimestamp(16, fromNullableInstant(req.getStart()));
                ps.setTimestamp(17, fromNullableInstant(req.getEnd()));
                ps.setString(18, req.getThreadName());
                ps.setString(19, req.getCdSession());
                if (req.getException() != null) {
                    req.getException().setIdRequest(inc.get());
                    exp.add(req.getException());
                }
	        });
	        if(!exp.isEmpty()) {
	            saveExceptions(exp, REST);
	        }
    	}
    }

    public void saveLocalRequests(List<Session> sessions){
        if(sessions.stream().anyMatch(s-> !isEmpty(s.getLocalRequests()))){
            var exp = new ArrayList<ExceptionInfo>();
            var inc = new AtomicLong(selectMaxId("E_LCL_RQT", "ID_LCL_RQT"));
            executeBatch("""
INSERT INTO E_LCL_RQT(ID_LCL_RQT,VA_NAM,VA_LCT,DH_STR,DH_END,VA_USR,VA_THR,VA_STT,CD_PRN_SES)
VALUES(?,?,?,?,?,?,?,?,?)""", treeIterator(sessions, Session::getLocalRequests), (ps, req)-> {
                var completed = isNull(req.getException());
                ps.setLong(1, inc.incrementAndGet());
                ps.setString(2,req.getName());
                ps.setString(3,req.getLocation());
                ps.setTimestamp(4,fromNullableInstant(req.getStart()));
                ps.setTimestamp(5,fromNullableInstant(req.getEnd()));
                ps.setString(6,req.getUser());
                ps.setString(7,req.getThreadName());
                ps.setBoolean(8, completed);
                ps.setString(9, req.getCdSession());
                if(req.getException() != null) {
                    req.getException().setIdRequest(inc.get());
                    exp.add(req.getException());
                }
            });
            if(!exp.isEmpty()) {
                saveExceptions(exp, LOCAL);
            }
        }
    }

    public void saveMailRequests(List<Session> sessions) {
        if(sessions.stream().anyMatch(s-> !isEmpty(s.getMailRequests()))){
            var inc = new AtomicLong(selectMaxId("E_SMTP_RQT", "ID_SMTP_RQT"));
            executeBatch("""
INSERT INTO E_SMTP_RQT(ID_SMTP_RQT,VA_HST,CD_PRT,VA_USR,DH_STR,DH_END,VA_THR,VA_STT,CD_PRN_SES)
VALUES(?,?,?,?,?,?,?,?,?)""", treeIterator(sessions, Session::getMailRequests), (ps, req)-> {
                req.setId(inc.incrementAndGet());
                var completed = req.getActions() == null || req.getActions().stream().allMatch(a-> isNull(a.getException()));
                ps.setLong(1, req.getId());
                ps.setString(2, req.getHost());
                ps.setInt(3, req.getPort());
                ps.setString(4, req.getUser());
                ps.setTimestamp(5, fromNullableInstant(req.getStart()));
                ps.setTimestamp(6, fromNullableInstant(req.getEnd()));
                ps.setString(7, req.getThreadName());
                ps.setBoolean(8, completed);
                ps.setString(9, req.getCdSession());
                updateStages(req);
            });
            saveMailRequestStages(sessions);
            saveMailRequestMails(sessions);
        }
    }
    private static void updateStages(MailRequest request) {
        if(!isEmpty(request.getMails())) {
            var inc = new AtomicInteger(0);
            for(MailRequestStage stage: request.getActions()) {
                stage.setId(request.getId());
                stage.setOrder(inc.incrementAndGet());
            }
            for(Mail mail: request.getMails()) {
                mail.setId(request.getId());
            }
        }
    }

    private void saveMailRequestStages(List<Session> sessions) {
        var exp = new ArrayList<ExceptionInfo>();
        executeBatch("INSERT INTO E_SMTP_STG(VA_NAM,DH_STR,DH_END,CD_ORD,CD_SMTP_RQT) VALUES(?,?,?,?,?)", treeIterator(sessions, Session::getMailRequests, MailRequest::getActions), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setInt(4, stage.getOrder());
            ps.setLong(5, stage.getId());
            if(stage.getException() != null) {
                stage.getException().setIdRequest(stage.getId());
                stage.getException().setOrder(stage.getOrder());
                exp.add(stage.getException());
            }
        });
        if(!exp.isEmpty()) {
            saveExceptions(exp, SMTP);
        }
    }

    private void saveMailRequestMails(List<Session> sessions) {
        executeBatch("INSERT INTO E_SMTP_MAIL(VA_SBJ,VA_CNT_TYP,VA_FRM,VA_RCP,VA_RPL,VA_SZE,CD_SMTP_RQT) VALUES(?,?,?,?,?,?,?)", treeIterator(sessions, Session::getMailRequests, MailRequest::getMails), (ps, mail)-> {
            ps.setString(1, mail.getSubject());
            ps.setString(2, mail.getContentType());
            ps.setString(3, mail.getFrom() != null ? String.join(", ", mail.getFrom()) : null);
            ps.setString(4, mail.getRecipients() != null ? String.join(", ", mail.getRecipients()) : null);
            ps.setString(5, mail.getReplyTo() != null ? String.join(", ", mail.getReplyTo()) : null);
            ps.setInt(6,mail.getSize());
            ps.setLong(7, mail.getId());
        });
    }

    private void saveExceptions(List<ExceptionInfo> exceptionList, RequestMask mask) {
        executeBatch("INSERT INTO E_EXC_INF(VA_TYP,VA_ERR_TYP,VA_ERR_MSG,CD_ORD,CD_RQT) VALUES(?,?,?,?,?)", exceptionList.iterator(),  (ps, exp)-> {
            ps.setString(1, mask.name());
            ps.setString(2, exp.getType());
            ps.setString(3, exp.getMessage());
            ps.setInt(4, exp.getOrder());
            ps.setLong(5, exp.getIdRequest());
        });
    }

    public void saveFtpRequests(List<Session> sessions){
        if(sessions.stream().anyMatch(s-> !isEmpty(s.getFtpRequests()))){
            var inc = new AtomicLong(selectMaxId("E_FTP_RQT", "ID_FTP_RQT"));
            executeBatch("""
INSERT INTO E_FTP_RQT(ID_FTP_RQT,VA_HST,CD_PRT,VA_PCL,VA_SRV_VRS,VA_CLT_VRS,VA_USR,DH_STR,DH_END,VA_THR,VA_STT,CD_PRN_SES)
VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""", treeIterator(sessions, Session::getFtpRequests), (ps, req)-> {
                var completed = req.getActions() == null || req.getActions().stream().allMatch(a -> isNull(a.getException()));
                req.setId(inc.incrementAndGet());
                ps.setLong(1, req.getId());
                ps.setString(2, req.getHost());
                ps.setInt(3, req.getPort());
                ps.setString(4, req.getProtocol());
                ps.setString(5, req.getServerVersion());
                ps.setString(6, req.getClientVersion());
                ps.setString(7, req.getUser());
                ps.setTimestamp(8, fromNullableInstant(req.getStart()));
                ps.setTimestamp(9, fromNullableInstant(req.getEnd()));
                ps.setString(10, req.getThreadName());
                ps.setBoolean(11, completed);
                ps.setString(12, req.getCdSession());
                updateStages(req);
            });
            saveFtpRequestStages(sessions);
        }
    }

    private static void updateStages(FtpRequest request) {
        if(!isEmpty(request.getActions())) {
            var inc = new AtomicInteger(0);
            for(FtpRequestStage stage: request.getActions()) {
                stage.setId(request.getId());
                stage.setOrder(inc.incrementAndGet());
            }
        }
    }

    private void saveFtpRequestStages(List<Session> sessions) {
        var exceptions = new ArrayList<ExceptionInfo>();
        executeBatch("INSERT INTO E_FTP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_FTP_RQT) VALUES(?,?,?,?,?,?)", treeIterator(sessions, Session::getFtpRequests, FtpRequest::getActions), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
            ps.setInt(5, stage.getOrder());
            ps.setLong(6, stage.getId());
            if(stage.getException() != null) {
                stage.getException().setIdRequest(stage.getId());
                stage.getException().setOrder(stage.getOrder());
                exceptions.add(stage.getException());
            }
        });
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, FTP);
        }
    }

    public void saveLdapRequests(List<Session> sessions) {
        if(sessions.stream().anyMatch(s-> !isEmpty(s.getLdapRequests()))){
            var inc = new AtomicLong(selectMaxId("E_LDAP_RQT", "ID_LDAP_RQT"));
            executeBatch("""
    INSERT INTO E_LDAP_RQT(ID_LDAP_RQT,VA_HST,CD_PRT,VA_PCL,VA_USR,DH_STR,DH_END,VA_THR,VA_STT,CD_PRN_SES)
    VALUES(?,?,?,?,?,?,?,?,?,?)""", treeIterator(sessions, Session::getLdapRequests), (ps, req)-> {
                var completed = req.getActions().stream().allMatch(a-> isNull(a.getException()));
                req.setId(inc.incrementAndGet());
                ps.setLong(1, req.getId());
                ps.setString(2, req.getHost());
                ps.setInt(3, req.getPort());
                ps.setString(4, req.getProtocol());
                ps.setString(5, req.getUser());
                ps.setTimestamp(6, fromNullableInstant(req.getStart()));
                ps.setTimestamp(7, fromNullableInstant(req.getEnd()));
                ps.setString(8, req.getThreadName());
                ps.setBoolean(9, completed);
                ps.setString(10, req.getCdSession());
                updateStages(req);
            });
            saveLdapRequestStages(sessions);
        }
    }

    private static void updateStages(NamingRequest request) {
        if(!isEmpty(request.getActions())) {
            var inc = new AtomicInteger(0);
            for(NamingRequestStage stage: request.getActions()) {
                stage.setId(request.getId());
                stage.setOrder(inc.incrementAndGet());
            }
        }
    }

    private void saveLdapRequestStages(List<Session> sessions) {
        var exceptions = new ArrayList<ExceptionInfo>();
        executeBatch("INSERT INTO E_LDAP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_LDAP_RQT) VALUES(?,?,?,?,?,?)", treeIterator(sessions, Session::getLdapRequests, NamingRequest::getActions), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, stage.getArgs() != null ? String.join(", ", stage.getArgs()) : null);
            ps.setInt(5, stage.getOrder());
            ps.setLong(6, stage.getId());
            if(stage.getException() != null) {
                stage.getException().setIdRequest(stage.getId());
                stage.getException().setOrder(stage.getOrder());
                exceptions.add(stage.getException());
            }
        });
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, LDAP);
        }
    }

    public void saveDatabaseRequests(List<Session> sessions) {
        if(sessions.stream().anyMatch(s-> !isEmpty(s.getDatabaseRequests()))){
            var inc = new AtomicLong(selectMaxId("E_DTB_RQT", "ID_DTB_RQT"));
            executeBatch("""
    INSERT INTO E_DTB_RQT(ID_DTB_RQT,VA_HST,CD_PRT,VA_NAM,VA_SCH,DH_STR,DH_END,VA_USR,VA_THR,VA_DRV,VA_PRD_NAM,VA_PRD_VRS,VA_CMD,VA_STT,CD_PRN_SES)
    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", treeIterator(sessions, Session::getDatabaseRequests), (ps, req)-> {
                var completed = req.getActions().stream().allMatch(a-> isNull(a.getException()));
                req.setId(inc.incrementAndGet());
                ps.setLong(1, req.getId());
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
                ps.setString(13, valueOfNullableList(req.getCommands()));
                ps.setBoolean(14, completed);
                ps.setString(15, req.getCdSession());
                updateStages(req);
            });
            saveDatabaseActions(sessions);
        }
    }

    private static void updateStages(DatabaseRequest request) {
        if(!isEmpty(request.getActions())) {
            var inc = new AtomicInteger(0);
            for(DatabaseRequestStage stage: request.getActions()) {
                stage.setId(request.getId());
                stage.setOrder(inc.incrementAndGet());
            }
        }
    }

    private void saveDatabaseActions(List<Session> sessions) {
        var exceptions = new ArrayList<ExceptionInfo>();
        executeBatch("INSERT INTO E_DTB_STG(VA_NAM,DH_STR,DH_END,VA_CNT,CD_ORD,CD_DTB_RQT) VALUES(?,?,?,?,?,?)", treeIterator(sessions, Session::getDatabaseRequests, DatabaseRequest::getActions), (ps, stage)-> {
            ps.setString(1, stage.getName());
            ps.setTimestamp(2, fromNullableInstant(stage.getStart()));
            ps.setTimestamp(3, fromNullableInstant(stage.getEnd()));
            ps.setString(4, valueOfNullableArray(stage.getCount()));
            ps.setInt(5, stage.getOrder());
            ps.setLong(6, stage.getId());
            if(stage.getException() != null) {
                stage.getException().setIdRequest(stage.getId());
                stage.getException().setOrder(stage.getOrder());
                exceptions.add(stage.getException());
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
