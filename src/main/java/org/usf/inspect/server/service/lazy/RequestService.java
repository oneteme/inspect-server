package org.usf.inspect.server.service.lazy;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.usf.inspect.core.TraceableStage;
import org.usf.inspect.server.dao.lazy.RequestDao;
import org.usf.inspect.server.model.lazy.InstanceEnvironment;
import org.usf.inspect.server.model.lazy.Metric;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Setter
public class RequestService {

    private final RequestDao dao;

    public void addInstance(InstanceEnvironment instance) {
        dao.saveInstanceEnvironment(instance);
    }

    public void updateInstance(Instant end,String instanceId){
        dao.updateInstanceEnvironment(end, instanceId);
    }

    @TraceableStage
    @Transactional(rollbackFor = Throwable.class)
    public long addMetrics(List<Metric> metrics) {
        return dao.saveMetrics(metrics);
    }
}
