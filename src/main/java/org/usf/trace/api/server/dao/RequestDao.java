package org.usf.trace.api.server.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.trace.api.server.RequestMask;
import org.usf.trace.api.server.model.InstanceMainSession;
import org.usf.trace.api.server.model.InstanceRestSession;
import org.usf.trace.api.server.model.InstanceSession;
import org.usf.trace.api.server.model.wrapper.*;
import org.usf.traceapi.core.*;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.sql.Types.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

@Repository
@RequiredArgsConstructor
public class RequestDao {

    private final JdbcTemplate template;

    public void saveInstanceEnvironment(List<InstanceEnvironmentWrapper> instances) {
        template.batchUpdate("INSERT INTO E_ENV_INS(ID_INS,VA_TYP,DH_STR,VA_APP,VA_VRS,VA_ADR,VA_ENV,VA_OS,VA_RE,VA_USR,VA_CLR) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)", instances, instances.size(), (ps, o) -> {
            ps.setString(1, o.getId());
            ps.setString(2, o.getType() != null ? o.getType().name() : null);
            ps.setTimestamp(3, fromNullableInstant(o.getInstant()));
            ps.setString(4, o.getName());
            ps.setString(5, o.getVersion());
            ps.setString(6, o.getAddress());
            ps.setString(7, o.getEnv());
            ps.setString(8, o.getOs());
            ps.setString(9, o.getRe());
            ps.setString(10, o.getUser());
            ps.setString(11, o.getCollector());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveSessions(List<InstanceSession> sessions) {
        filterAndSave(sessions, InstanceRestSession.class, this::saveRestSessions);
        filterAndSave(sessions, InstanceMainSession.class, this::saveMainSessions);
        filterSubAndSave(sessions, Session::getRestRequests, (s, r) -> new RestRequestWrapper(s.getId(), r), this::saveRestRequests);
        filterSubAndSave(sessions, Session::getFtpRequests, (s, r) -> new FtpRequestWrapper(s.getId(), r), this::saveFtpRequests);
        filterSubAndSave(sessions, Session::getMailRequests, (s, r) -> new MailRequestWrapper(s.getId(), r), this::saveMailRequests);
        filterSubAndSave(sessions, Session::getDatabaseRequests, (s, q) -> new DatabaseRequestWrapper(s.getId(), q), this::saveDatabaseRequests);
        filterSubAndSave(sessions, Session::getLocalRequests, (s, st) -> new LocalRequestWrapper(s.getId(), st), this::saveLocalRequests);
    }

    private void saveMainSessions(List<InstanceMainSession> reqList) {
        template.batchUpdate("INSERT INTO E_MAIN_SES(ID_SES,VA_NAM,VA_USR,DH_STR,DH_END,VA_TYP,VA_LCT,VA_THR,VA_ERR_TYP,VA_ERR_MSG,VA_MSK,CD_INS)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            var exp = nullableException(o.getException());
            ps.setString(1, o.getId());
            ps.setString(2, o.getName());
            ps.setString(3, o.getUser());
            ps.setTimestamp(4, fromNullableInstant(o.getStart()));
            ps.setTimestamp(5, fromNullableInstant(o.getEnd()));
            ps.setString(6, valueOfNullable(o.getType()));
            ps.setString(7, o.getLocation());
            ps.setString(8, o.getThreadName());
            ps.setString(9, exp.getType());
            ps.setString(10, exp.getMessage());
            ps.setInt(11, RequestMask.mask(o));
            ps.setString(12, o.getInstanceId());
        });
    }

    private void saveRestSessions(List<InstanceRestSession> reqList) {
        template.batchUpdate("INSERT INTO E_RST_SES(ID_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,VA_ERR_TYP,VA_ERR_MSG,VA_NAM,VA_USR,VA_USR_AGT,VA_CCH_CTR,VA_MSK,CD_INS)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            var exp = nullableException(o.getException());
            ps.setString(1, o.getId());
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
            ps.setString(18, exp.getType());
            ps.setString(19, exp.getMessage());
            ps.setString(20, o.getName());
            ps.setString(21, o.getUser());
            ps.setString(22, o.getUserAgent());
            ps.setString(23, o.getCacheControl());
            ps.setInt(24, RequestMask.mask(o));
            ps.setString(25, o.getInstanceId());
        });
    }

    public void saveRestRequests(List<RestRequestWrapper> reqList) {
        template.batchUpdate("INSERT INTO E_RST_RQT(CD_RMT_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,VA_ERR_TYP,VA_ERR_MSG,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            var exp = nullableException(o.getException());
            ps.setString(1, o.getId());
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
            ps.setString(18, exp.getType());
            ps.setString(19, exp.getMessage());
            ps.setString(20, o.getParentId());
        });
    }

    public void saveLocalRequests(List<LocalRequestWrapper> stagesList){
        template.batchUpdate("INSERT INTO E_LCL_RQT(VA_NAM,VA_LCT,DH_STR,DH_END,VA_USR,VA_THR,VA_ERR_TYP,VA_ERR_MSG,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?)", stagesList,stagesList.size(),(ps,o)-> {
            var exp = nullableException(o.getException());
            ps.setString(1,o.getName());
            ps.setString(2,o.getLocation());
            ps.setTimestamp(3,fromNullableInstant(o.getStart()));
            ps.setTimestamp(4,fromNullableInstant(o.getEnd()));
            ps.setString(5,o.getUser());
            ps.setString(6,o.getThreadName());
            ps.setString(7,exp.getType());
            ps.setString(8,exp.getMessage());
            ps.setString(9,o.getParentId());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveMailRequests(List<MailRequestWrapper> mailList) {
        var maxId = template.queryForObject("SELECT COALESCE(MAX(ID_SMTP_RQT), 0) FROM E_SMTP_RQT", Long.class);
        var inc = new AtomicLong(maxId);

        template.batchUpdate("INSERT INTO E_SMTP_RQT(ID_SMTP_RQT,VA_HST,CD_PRT,VA_USR,DH_STR,DH_END,VA_THR,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?)", mailList, mailList.size(), (ps, o) -> {
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setInt(3, o.getPort());
            ps.setString(4, o.getUser());
            ps.setTimestamp(5, fromNullableInstant(o.getStart()));
            ps.setTimestamp(6, fromNullableInstant(o.getEnd()));
            ps.setString(7, o.getThreadName());
            ps.setString(8, o.getParentId());
            o.setId(inc.get());
        });
        saveMailRequestStages(mailList);
        saveMailRequestMails(mailList);
    }

    private void saveMailRequestStages(List<MailRequestWrapper> mailList) {
        template.batchUpdate("INSERT INTO E_SMTP_STG(VA_NAM,DH_STR,DH_END,VA_ERR_TYP,VA_ERR_MSG,CD_SMTP_RQT) VALUES(?,?,?,?,?,?)",
                mailList.stream()
                        .flatMap(e -> e.getActions().stream().map(da -> {
                                    var exp = nullableException(da.getException());
                                    return new Object[]{da.getName(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()), exp.getType(), exp.getMessage(), e.getId()
                                    };
                                }
                        ))
                        .toList(),
                new int[]{VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, VARCHAR, BIGINT});
    }

    private void saveMailRequestMails(List<MailRequestWrapper> mailList) {
        template.batchUpdate("INSERT INTO E_SMTP_MAIL(VA_SBJ,VA_CNT_TYP,VA_FRM,VA_RCP,VA_RPL,VA_SZE,CD_SMTP_RQT) VALUES(?,?,?,?,?,?,?)",
                mailList.stream()
                        .flatMap(e -> e.getMails().stream().map(da -> {
                                    return new Object[]{da.getSubject(), da.getContentType(), String.join(", ", da.getFrom()), String.join(", ", da.getRecipients()), String.join(", ", da.getReplyTo()), da.getSize(), e.getId()
                                    };
                                }
                        ))
                        .toList(),
                new int[]{VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, BIGINT, BIGINT});
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveFtpRequests(List<FtpRequestWrapper> ftpList) {
        var maxId = template.queryForObject("SELECT COALESCE(MAX(ID_FTP_RQT), 0) FROM E_FTP_RQT", Long.class);
        var inc = new AtomicLong(maxId);

        template.batchUpdate("INSERT INTO E_FTP_RQT(ID_FTP_RQT,VA_HST,CD_PRT,VA_PCL,VA_SRV_VRS,VA_CLT_VRS,VA_USR,DH_STR,DH_END,VA_THR,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?)", ftpList, ftpList.size(), (ps, o) -> {
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setInt(3, o.getPort());
            ps.setString(4, o.getProtocol());
            ps.setString(5, o.getServerVersion());
            ps.setString(6, o.getClientVersion());
            ps.setString(7, o.getUser());
            ps.setTimestamp(8, fromNullableInstant(o.getStart()));
            ps.setTimestamp(9, fromNullableInstant(o.getEnd()));
            ps.setString(10, o.getThreadName());
            ps.setString(11, o.getParentId());
            o.setId(inc.get());
        });
        saveFtpRequestStages(ftpList);
    }

    private void saveFtpRequestStages(List<FtpRequestWrapper> ftpList) {
        template.batchUpdate("INSERT INTO E_FTP_STG(VA_NAM,DH_STR,DH_END,VA_ERR_TYP,VA_ERR_MSG,VA_ARG,CD_FTP_RQT) VALUES(?,?,?,?,?,?,?)",
                ftpList.stream()
                        .flatMap(e -> e.getActions().stream().map(da -> {
                                    var exp = nullableException(da.getException());
                                    return new Object[]{da.getName(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()), exp.getType(), exp.getMessage(), String.join(", ", da.getArgs()), e.getId()
                                    };
                                }
                        ))
                        .toList(),
                new int[]{VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, VARCHAR, VARCHAR, BIGINT});
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveDatabaseRequests(List<DatabaseRequestWrapper> qryList) {
        var maxId = template.queryForObject("SELECT COALESCE(MAX(ID_DTB_RQT), 0) FROM E_DTB_RQT", Long.class);
        var inc = new AtomicLong(maxId);

        template.batchUpdate("INSERT INTO E_DTB_RQT(ID_DTB_RQT,VA_HST,CD_PRT,VA_NAM,DH_STR,DH_END,VA_USR,VA_THR,VA_DRV,VA_PRD_NAM,VA_PRD_VRS,VA_CMD,VA_CPT,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", qryList, qryList.size(), (ps, o) -> {
            var completed = o.getActions().stream().allMatch(a-> isNull(a.getException()));
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setInt(3, o.getPort());
            ps.setString(4, o.getDatabase());
            ps.setTimestamp(5, fromNullableInstant(o.getStart()));
            ps.setTimestamp(6, fromNullableInstant(o.getEnd()));
            ps.setString(7, o.getUser());
            ps.setString(8, o.getThreadName());
            ps.setString(9, o.getDriverVersion());
            ps.setString(10, o.getProductName());
            ps.setString(11, o.getProductVersion());
            ps.setString(12, valueOfNullableList(o.getCommands()));
            ps.setString(13, completed ? "T" : "F");
            ps.setString(14, o.getParentId());
            o.setId(inc.get());
        });
        saveDatabaseActions(qryList);
    }

    private void saveDatabaseActions(List<DatabaseRequestWrapper> queries) {
        template.batchUpdate("INSERT INTO E_DTB_STG(VA_NAM,DH_STR,DH_END,VA_ERR_TYP,VA_ERR_MSG,VA_CNT,CD_DTB_RQT) VALUES(?,?,?,?,?,?,?)",
                queries.stream()
                        .flatMap(e -> e.getActions().stream().map(da -> {
                                    var exp = nullableException(da.getException());
                                    return new Object[]{da.getName(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()), exp.getType(), exp.getMessage(),valueOfNullableArray(da.getCount()), e.getId()
                                    };
                                }
                        ))
                        .toList(),
                new int[]{VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, VARCHAR, VARCHAR, BIGINT});
    }

    public List<String> selectChildsById(String id) {
        var query = " with recursive recusive(prnt,chld) as (" +
                " select ''::varchar as prnt, ? as chld " +
                " union all " +
                " select  recusive.chld, E_RST_RQT.CD_RMT_SES " +
                " from E_RST_RQT, recusive " +
                " where recusive.chld = E_RST_RQT.CD_RMT_SES " +
                ") select distinct(chld) from recusive";
        return template.query(query, (ResultSet rs, int rowNum) -> (rs.getString("chld")), id).stream().filter(Objects::nonNull).toList();
    }

    private static <T, U extends T> void filterAndSave(Collection<T> c, Class<U> classe, Consumer<List<U>> saveFn) {
        var list = c.stream()
                .filter(classe::isInstance)
                .map(classe::cast)
                .toList();
        if (!list.isEmpty()) {
            saveFn.accept(list);
        }
    }

    private static <T, U, R> void filterSubAndSave(Collection<T> c, Function<T, Collection<U>> accessor, BiFunction<T, U, R> mapper, Consumer<List<R>> saveFn) {
        var list = c.stream()
                .filter(o -> nonNull(accessor.apply(o)))
                .flatMap(o -> accessor.apply(o).stream().map(s -> mapper.apply(o, s)))
                .toList();
        if (!list.isEmpty()) {
            saveFn.accept(list);
        }
    }

    private static Timestamp fromNullableInstant(Instant instant) {
        return ofNullable(instant).map(Timestamp::from).orElse(null);
    }

    private static String valueOfNullable(Object o) {
        return ofNullable(o).map(Object::toString).orElse(null);
    }

    private static <T extends Enum<T>> String valueOfNullableList(List<T> enumList) { return ofNullable(enumList).map(list -> list.stream().map(Enum::toString).collect(Collectors.joining(","))).orElse(null);}
    private static String  valueOfNullableArray(long[]array){ return ofNullable(array).map(arr -> LongStream.of(arr).mapToObj(Long::toString).collect(Collectors.joining(","))).orElse(null);}

    private static ExceptionInfo nullableException(ExceptionInfo exp) {
        return ofNullable(exp).orElseGet(() -> new ExceptionInfo(null, null));
    }
}
