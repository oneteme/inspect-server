package org.usf.inspect.server.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
public class Query {
    private String sql;
    private Object[] params;

    public Query(String sql, Object[] params){
        this.sql = sql;
        this.params = params != null ? params : new Object[0];
    }

    public Query(String sql){
        this.sql = sql;
    }

}
