package org.usf.trace.api.server.dao.v3;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.usf.trace.api.server.model.InstanceMainSession;
import org.usf.trace.api.server.model.InstanceRestSession;
import org.usf.trace.api.server.model.InstanceSession;
import org.usf.trace.api.server.model.wrapper.ApiRequestWrapper;
import org.usf.trace.api.server.model.wrapper.DatabaseRequestWrapper;
import org.usf.trace.api.server.model.wrapper.InstanceEnvironmentWrapper;
import org.usf.trace.api.server.model.wrapper.RunnableStageWrapper;
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
public class V3RequestDao {

    private final JdbcTemplate template;

    public void saveInstanceEnvironment(List<InstanceEnvironmentWrapper> instances) {
        template.batchUpdate("INSERT INTO E_INS_ENV(ID_INS_ENV, VA_TYP, DH_DBT, VA_APP_NME, VA_VRS, VA_ADRS, VA_ENV, VA_OS, VA_RE, VA_USR, VA_CLCT) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", instances, instances.size(), (ps, o) -> {
            ps.setString(1, o.getInstanceId());
            ps.setString(2, o.getType().name()); //TODO nullable !?
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
        filterAndSave(sessions, InstanceRestSession.class, this::saveApiSessions);
        filterAndSave(sessions, InstanceMainSession.class, this::saveMainSessions);
        filterSubAndSave(sessions, Session::getRequests, (s, r) -> new ApiRequestWrapper(s.getId(), r), this::saveApiRequests);
        filterSubAndSave(sessions, Session::getQueries, (s, q) -> new DatabaseRequestWrapper(s.getId(), q), this::saveDatabaseRequests);
        filterSubAndSave(sessions, Session::getStages, (s, st) -> new RunnableStageWrapper(s.getId(), st), this::saveRunnableStages);
    }

    private void saveMainSessions(List<InstanceMainSession> reqList) {
        template.batchUpdate("INSERT INTO E_MAIN_SES(ID_SES,VA_NAME,VA_USR,DH_DBT,DH_FIN,LNCH,LOC,VA_THRED,VA_ERR_CLS,VA_ERR_MSG,CD_INS_ENV)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
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
            ps.setString(11, o.getInstanceId());
        });
    }

    private void saveApiSessions(List<InstanceRestSession> reqList) {
        template.batchUpdate("INSERT INTO E_API_SES(ID_SES,VA_MTH,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_AUTH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_DBT,DH_FIN,VA_THRED,VA_ERR_CLS,VA_ERR_MSG,VA_API_NME,VA_USR,VA_USR_AGT,CD_INS_ENV)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", reqList, reqList.size(), (ps, o) -> {
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
            ps.setString(23, o.getInstanceId());
        });
    }

    public void saveApiRequests(List<ApiRequestWrapper> reqList) {
        template.batchUpdate("INSERT INTO E_API_REQ(CD_API,VA_MTH,VA_PRTCL,VA_HST,CD_PRT,VA_PTH,VA_QRY,VA_CNT_TYP,VA_AUTH,CD_STT,VA_I_SZE,VA_O_SZE,VA_I_CNT_ENC,VA_O_CNT_ENC,DH_DBT,DH_FIN,VA_THRED,VA_ERR_CLS,VA_ERR_MSG,CD_SES)"
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

    public void saveRunnableStages(List<RunnableStageWrapper> stagesList){
        template.batchUpdate("INSERT INTO E_STG(VA_NAME,LOC,DH_DBT,DH_FIN,VA_USR,VA_THRED,VA_ERR_CLS,VA_ERR_MSG,CD_SES)"
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
    public void saveDatabaseRequests(List<DatabaseRequestWrapper> qryList) {
        var maxId = template.queryForObject("SELECT COALESCE(MAX(ID_OUT_QRY), 0) FROM E_DB_REQ", Long.class);
        var inc = new AtomicLong(maxId);

        template.batchUpdate("INSERT INTO E_DB_REQ(ID_OUT_QRY,VA_HST,CD_PRT,VA_DB,DH_DBT,DH_FIN,VA_USR,VA_THRED,VA_DRV,VA_DB_NME,VA_DB_VRS,VA_CMD,VA_CMPLT,CD_SES)"
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
            ps.setString(10, o.getDatabaseName());
            ps.setString(11, o.getDatabaseVersion());
            ps.setString(12, valueOfNullableList(o.getCommands()));
            ps.setString(13, completed ? "T" : "F");
            ps.setString(14, o.getParentId());
            o.setId(inc.get());
        });
        saveDatabaseActions(qryList);
    }

    private void saveDatabaseActions(List<DatabaseRequestWrapper> queries) {
        template.batchUpdate("INSERT INTO E_DB_ACT(VA_TYP,DH_DBT,DH_FIN,VA_ERR_CLS,VA_ERR_MSG,CD_COUNT,CD_OUT_QRY) VALUES(?,?,?,?,?,?,?)",
                queries.stream()
                        .flatMap(e -> e.getActions().stream().map(da -> {
                                    var exp = nullableException(da.getException());
                                    return new Object[]{da.getType().toString(), fromNullableInstant(da.getStart()), fromNullableInstant(da.getEnd()), exp.getClassname(), exp.getMessage(),valueOfNullableArray(da.getCount()), e.getId()
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
                " select  recusive.chld, E_API_REQ.CD_API " +
                " from E_API_REQ, recusive " +
                " where recusive.chld= E_API_REQ.CD_SES " +
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
