package org.usf.inspect.server.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.usf.inspect.core.ExceptionInfo;
import org.usf.inspect.core.Session;
import org.usf.inspect.server.RequestMask;
import org.usf.inspect.server.model.*;
import org.usf.inspect.server.model.wrapper.*;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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
import static org.usf.inspect.server.RequestMask.*;

@Repository
@RequiredArgsConstructor
public class RequestDao {

    private final JdbcTemplate template;

    public void saveInstanceEnvironment(ServerInstanceEnvironment instance) {
        template.update("INSERT INTO E_ENV_INS(ID_INS,VA_TYP,DH_STR,VA_APP,VA_VRS,VA_ADR,VA_ENV,VA_OS,VA_RE,VA_USR,VA_CLR) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)", ps -> {
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

    public void saveSessions(List<ServerSession> sessions) {
        filterAndSave(sessions, ServerRestSession.class, this::saveRestSessions);
        filterAndSave(sessions, ServerMainSession.class, this::saveMainSessions);
        filterSubAndSave(sessions, Session::getRestRequests, (s, rest) -> new RestRequestWrapper(s.getId(), rest), this::saveRestRequests);
        filterSubAndSave(sessions, Session::getFtpRequests, (s, ftp) -> new FtpRequestWrapper(s.getId(), ftp), this::saveFtpRequests);
        filterSubAndSave(sessions, Session::getMailRequests, (s, smtp) -> new MailRequestWrapper(s.getId(), smtp), this::saveMailRequests);
        filterSubAndSave(sessions, Session::getDatabaseRequests, (s, jdbc) -> new DatabaseRequestWrapper(s.getId(), jdbc), this::saveDatabaseRequests);
        filterSubAndSave(sessions, Session::getLocalRequests, (s, local) -> new LocalRequestWrapper(s.getId(), local), this::saveLocalRequests);
        filterSubAndSave(sessions, Session::getLdapRequests, (s, ldap) -> new NamingRequestWrapper(s.getId(), ldap), this::saveLdapRequests);
    }

    private void saveMainSessions(List<ServerMainSession> reqList) {
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
            ps.setInt(11, mask(o));
            ps.setString(12, o.getInstanceId());
        });
    }

    private void saveRestSessions(List<ServerRestSession> reqList) {
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
            ps.setInt(24, mask(o));
            ps.setString(25, o.getInstanceId());
        });
    }

    public void saveRestRequests(List<RestRequestWrapper> reqList) {
        var exceptions = new ArrayList<ServerException>();
        var inc = new AtomicLong(selectMaxId("E_RST_RQT", "ID_RST_RQT"));
        template.batchUpdate("INSERT INTO E_RST_RQT(ID_RST_RQT,CD_RMT_SES,VA_MTH,VA_PCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_ATH_SCH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_STR,DH_END,VA_THR,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getId());
            ps.setString(3, o.getMethod());
            ps.setString(4, o.getProtocol());
            ps.setString(5, o.getHost());
            ps.setInt(6, o.getPort());
            ps.setString(7, o.getPath());
            ps.setString(8, o.getQuery());
            ps.setString(9, o.getContentType());
            ps.setString(10, o.getAuthScheme());
            ps.setInt(11, o.getStatus());
            ps.setLong(12, o.getInDataSize());
            ps.setLong(13, o.getOutDataSize());
            ps.setString(14, o.getInContentEncoding());
            ps.setString(15, o.getOutContentEncoding());
            ps.setTimestamp(16, fromNullableInstant(o.getStart()));
            ps.setTimestamp(17, fromNullableInstant(o.getEnd()));
            ps.setString(18, o.getThreadName());
            ps.setString(19, o.getCdSession());
            if(o.getException() != null) {
                exceptions.add(new ServerException(inc.get(), null, new ExceptionInfo(o.getException().getType(), o.getException().getMessage())));
            }
        });
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, REST);
        }
    }

    public void saveLocalRequests(List<LocalRequestWrapper> stagesList){
        var exceptions = new ArrayList<ServerException>();
        var inc = new AtomicLong(selectMaxId("E_LCL_RQT", "ID_LCL_RQT"));
        template.batchUpdate("INSERT INTO E_LCL_RQT(ID_LCL_RQT,VA_NAM,VA_LCT,DH_STR,DH_END,VA_USR,VA_THR,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?)", stagesList,stagesList.size(),(ps,o)-> {
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2,o.getName());
            ps.setString(3,o.getLocation());
            ps.setTimestamp(4,fromNullableInstant(o.getStart()));
            ps.setTimestamp(5,fromNullableInstant(o.getEnd()));
            ps.setString(6,o.getUser());
            ps.setString(7,o.getThreadName());
            ps.setString(8,o.getCdSession());
            if(o.getException() != null) {
                exceptions.add(new ServerException(inc.get(), null, new ExceptionInfo(o.getException().getType(), o.getException().getMessage())));
            }
        });
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, LOCAL);
        }
    }

    public void saveMailRequests(List<MailRequestWrapper> mailList) {
        var inc = new AtomicLong(selectMaxId("E_SMTP_RQT", "ID_SMTP_RQT"));

        template.batchUpdate("INSERT INTO E_SMTP_RQT(ID_SMTP_RQT,VA_HST,CD_PRT,VA_USR,DH_STR,DH_END,VA_THR,VA_CPT,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?)", mailList, mailList.size(), (ps, o) -> {
            var completed = o.getActions().stream().allMatch(a-> isNull(a.getException()));
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setInt(3, o.getPort());
            ps.setString(4, o.getUser());
            ps.setTimestamp(5, fromNullableInstant(o.getStart()));
            ps.setTimestamp(6, fromNullableInstant(o.getEnd()));
            ps.setString(7, o.getThreadName());
            ps.setString(8, completed ? "T" : "F");
            ps.setString(9, o.getCdSession());
            o.setId(inc.get());
        });
        saveMailRequestStages(mailList);
        saveMailRequestMails(mailList);
    }

    private void saveMailRequestStages(List<MailRequestWrapper> mailList) {
        var exceptions = new ArrayList<ServerException>();
        template.batchUpdate("INSERT INTO E_SMTP_STG(VA_NAM,DH_STR,DH_END,CD_ORD,CD_SMTP_RQT) VALUES(?,?,?,?,?)",
                mailList.stream()
                        .flatMap(e -> {
                            var inc = new AtomicLong(0);
                            return e.getActions().stream().map(da -> {
                                var id = inc.incrementAndGet();
                                if(da.getException() != null) {
                                    exceptions.add(new ServerException(e.getId(), id, new ExceptionInfo(da.getException().getType(), da.getException().getMessage())));
                                }
                                return new Object[]{da.getName(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()), id, e.getId()};
                            });
                        }).toList(),
                new int[]{VARCHAR, TIMESTAMP, TIMESTAMP, INTEGER, BIGINT});
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, SMTP);
        }
    }

    private void saveMailRequestMails(List<MailRequestWrapper> mailList) {
        template.batchUpdate("INSERT INTO E_SMTP_MAIL(VA_SBJ,VA_CNT_TYP,VA_FRM,VA_RCP,VA_RPL,VA_SZE,CD_SMTP_RQT) VALUES(?,?,?,?,?,?,?)",
                mailList.stream()
                        .flatMap(e -> e.getMails().stream().filter(Objects::nonNull).map(da -> new Object[]{da.getSubject(), da.getContentType(), da.getFrom() != null ? String.join(", ", da.getFrom()) : null, da.getRecipients() != null ? String.join(", ", da.getRecipients()) : null, da.getReplyTo() != null ? String.join(", ", da.getReplyTo()) : null, da.getSize(), e.getId()}))
                        .toList(),
                new int[]{VARCHAR, VARCHAR, VARCHAR, VARCHAR, VARCHAR, BIGINT, BIGINT});
    }

    public void saveFtpRequests(List<FtpRequestWrapper> ftpList) {
        var inc = new AtomicLong(selectMaxId("E_FTP_RQT", "ID_FTP_RQT"));

        template.batchUpdate("INSERT INTO E_FTP_RQT(ID_FTP_RQT,VA_HST,CD_PRT,VA_PCL,VA_SRV_VRS,VA_CLT_VRS,VA_USR,DH_STR,DH_END,VA_THR,VA_CPT,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", ftpList, ftpList.size(), (ps, o) -> {
            var completed = o.getActions().stream().allMatch(a-> isNull(a.getException()));
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
            ps.setString(11, completed ? "T" : "F");
            ps.setString(12, o.getCdSession());
            o.setId(inc.get());
        });
        saveFtpRequestStages(ftpList);
    }

    private void saveFtpRequestStages(List<FtpRequestWrapper> ftpList) {
        var exceptions = new ArrayList<ServerException>();
        template.batchUpdate("INSERT INTO E_FTP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_FTP_RQT) VALUES(?,?,?,?,?,?)",
                ftpList.stream()
                        .flatMap(e ->  {
                            var inc = new AtomicLong(0);
                            return e.getActions().stream().map(da -> {
                                var id = inc.incrementAndGet();
                                if(da.getException() != null) {
                                    exceptions.add(new ServerException(e.getId(), id, new ExceptionInfo(da.getException().getType(), da.getException().getMessage())));
                                }
                                return new Object[]{da.getName(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()), da.getArgs() != null ? String.join(", ", da.getArgs()) : null, id, e.getId()};
                            });
                        }).toList(),
                new int[]{VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, INTEGER, BIGINT});
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, FTP);
        }
    }

    public void saveLdapRequests(List<NamingRequestWrapper> ldapList) {
        var inc = new AtomicLong(selectMaxId("E_LDAP_RQT", "ID_LDAP_RQT"));

        template.batchUpdate("INSERT INTO E_LDAP_RQT(ID_LDAP_RQT,VA_HST,CD_PRT,VA_PCL,VA_USR,DH_STR,DH_END,VA_THR,VA_CPT,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?)", ldapList, ldapList.size(), (ps, o) -> {
            var completed = o.getActions().stream().allMatch(a-> isNull(a.getException()));
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setInt(3, o.getPort());
            ps.setString(4, o.getProtocol());
            ps.setString(5, o.getUser());
            ps.setTimestamp(6, fromNullableInstant(o.getStart()));
            ps.setTimestamp(7, fromNullableInstant(o.getEnd()));
            ps.setString(8, o.getThreadName());
            ps.setString(9, completed ? "T" : "F");
            ps.setString(10, o.getCdSession());
            o.setId(inc.get());
        });
        saveLdapRequestStages(ldapList);
    }

    private void saveLdapRequestStages(List<NamingRequestWrapper> ldapList) {
        var exceptions = new ArrayList<ServerException>();
        template.batchUpdate("INSERT INTO E_LDAP_STG(VA_NAM,DH_STR,DH_END,VA_ARG,CD_ORD,CD_LDAP_RQT) VALUES(?,?,?,?,?,?)",
                ldapList.stream()
                        .flatMap(e -> {
                            var inc = new AtomicLong(0);
                            return e.getActions().stream().map(da -> {
                                var id = inc.incrementAndGet();
                                if(da.getException() != null) {
                                    exceptions.add(new ServerException(e.getId(), id, new ExceptionInfo(da.getException().getType(), da.getException().getMessage())));
                                }
                                return new Object[]{da.getName(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()), da.getArgs() != null ? String.join(", ", da.getArgs()) : null, id, e.getId()};
                            });
                        }).toList(),
                new int[]{VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, INTEGER, BIGINT});
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, LDAP);
        }
    }

    public void saveDatabaseRequests(List<DatabaseRequestWrapper> qryList) {
        var inc = new AtomicLong(selectMaxId("E_DTB_RQT", "ID_DTB_RQT"));

        template.batchUpdate("INSERT INTO E_DTB_RQT(ID_DTB_RQT,VA_HST,CD_PRT,VA_NAM,DH_STR,DH_END,VA_USR,VA_THR,VA_DRV,VA_PRD_NAM,VA_PRD_VRS,VA_CMD,VA_CPT,CD_PRN_SES)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)", qryList, qryList.size(), (ps, o) -> {
            var completed = o.getActions().stream().allMatch(a-> isNull(a.getException()));
            ps.setLong(1, inc.incrementAndGet());
            ps.setString(2, o.getHost());
            ps.setInt(3, o.getPort());
            ps.setString(4, o.getName());
            ps.setTimestamp(5, fromNullableInstant(o.getStart()));
            ps.setTimestamp(6, fromNullableInstant(o.getEnd()));
            ps.setString(7, o.getUser());
            ps.setString(8, o.getThreadName());
            ps.setString(9, o.getDriverVersion());
            ps.setString(10, o.getProductName());
            ps.setString(11, o.getProductVersion());
            ps.setString(12, valueOfNullableList(o.getCommands()));
            ps.setString(13, completed ? "T" : "F");
            ps.setString(14, o.getCdSession());
            o.setId(inc.get());
        });
        saveDatabaseActions(qryList);
    }

    private void saveDatabaseActions(List<DatabaseRequestWrapper> queries) {
        var exceptions = new ArrayList<ServerException>();
        template.batchUpdate("INSERT INTO E_DTB_STG(VA_NAM,DH_STR,DH_END,VA_CNT,CD_ORD,CD_DTB_RQT) VALUES(?,?,?,?,?,?)",
                queries.stream()
                        .flatMap(e -> {
                            var inc = new AtomicLong(0);
                            return e.getActions().stream().map(da -> {
                                var id = inc.incrementAndGet();
                                if(da.getException() != null) {
                                    exceptions.add(new ServerException(e.getId(), id, new ExceptionInfo(da.getException().getType(), da.getException().getMessage())));
                                }
                                return new Object[]{da.getName(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()),valueOfNullableArray(da.getCount()), id, e.getId()};
                            });
                        }).toList(),
                new int[]{VARCHAR, TIMESTAMP, TIMESTAMP, VARCHAR, BIGINT, BIGINT});
        if(!exceptions.isEmpty()) {
            saveExceptions(exceptions, JDBC);
        }
    }

    private void saveExceptions(List<ServerException> exceptionList, RequestMask mask) {
        template.batchUpdate("INSERT INTO E_EXC_INF(VA_TYP,VA_ERR_TYP,VA_ERR_MSG,CD_ORD,CD_RQT) VALUES(?,?,?,?,?)",
                exceptionList.stream().map(e -> new Object[]{mask.name(), e.exceptionInfo().getType(), e.exceptionInfo().getMessage(), e.order(), e.cdRequest()}).toList(),
                new int[]{VARCHAR, VARCHAR, VARCHAR, INTEGER, BIGINT});
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

    private static <T extends Enum<T>> String valueOfNullableList(List<T> enumList) {
        return ofNullable(enumList)
                .map(list ->
                        list.stream().filter(Objects::nonNull).map(Enum::name).collect(Collectors.joining(","))
                )
                .orElse(null);
    }
    private static String  valueOfNullableArray(long[]array){
        return ofNullable(array).map(arr -> LongStream.of(arr).mapToObj(Long::toString).collect(Collectors.joining(","))).orElse(null);
    }

    private static ExceptionInfo nullableException(ExceptionInfo exp) {
        return ofNullable(exp).orElseGet(() -> new ExceptionInfo(null, null));
    }
}
