package org.usf.inspect.server.model;

import lombok.Getter;


@Getter
public class Query {
    private String sql;
    private Object[] params;
    private String name;

    public Query(String sql, Object[] params){
        this.sql = sql;
        this.params = params != null ? params : new Object[0];
    }

    public Query(String sql, String name){
        this.sql = sql;
        this.name = name;
    }

    public Query(String sql){
        this.sql = sql;
    }

}
