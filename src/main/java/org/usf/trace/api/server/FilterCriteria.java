package org.usf.trace.api.server;

import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;


import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FilterCriteria {

    private final String[] idSession; //
    private final String[] method;
    private final String[] protocol;
    private final String[] host;
    private final String[] port;
    private final String path;
    private final String query;
    private final String[] media;
    private final String[] auth;
    private final String[] status;
    private final Instant start;
    private final Instant  end;
    private final String[] apiname;
    private final String[] user;
    private final String[] appname;
    private final String[] env;
    private final String[] launchMode;
    private final String location;
    private final String[] name;



    public String toSql(Filters idSession,
                        Filters methodColName,
                        Filters protocolColName,
                        Filters hostColName,
                        Filters portColName,
                        Filters pathColName,
                        Filters queryColName,
                        Filters mediaColName,
                        Filters authColName,
                        Filters statusColName,
                        Filters startColName,
                        Filters endColName,
                        Filters apiNameColName,
                        Filters userColName,
                        Filters appNameColName,
                        Filters envColname,
                        Filters launchModeColName,
                        Filters locationColName,
                        Filters nameColName,
                        Collection<Object> args,
                        Collection<Integer> argTypes) {

        var sql = " WHERE 1 = 1";
        sql += toSql(idSession,args,argTypes,getIdSession());
        sql += toSql(methodColName,args,argTypes,getMethod());
        sql += toSql(protocolColName,args,argTypes,getProtocol());
        sql += toSql(hostColName,args,argTypes,getHost());
        sql += toSql(portColName,args,argTypes,getPort());
        sql += toSqlLike(pathColName,args,argTypes,getPath());
        sql += toSqlLike(queryColName,args,argTypes,getQuery());
        sql += toSql(mediaColName,args,argTypes,getMedia());
        sql += toSql(authColName,args,argTypes,getAuth());
        sql += toSql(statusColName,args,argTypes,getStatus());
        sql += startToSql(startColName,args,argTypes,getStart()) ;
        sql += endToSql( endColName,args,argTypes,getEnd());
        sql += toSql(apiNameColName,args,argTypes,getApiname());
        sql += toSql(userColName,args,argTypes,getUser());
        sql += toSql(appNameColName,args,argTypes,getAppname());
        sql += toSql(envColname,args,argTypes,getEnv());
        sql += toSql(launchModeColName,args,argTypes,getLaunchMode());
        sql += toSqlLike(locationColName,args,argTypes,getLocation());
        sql += toSql(nameColName,args,argTypes,getName());
        return  sql;
    }


    String toSql(Filters colname, Collection<Object> args, Collection<Integer> argTypes, String... values){
        if(values == null || values.length == 0){
            return "";
        }

        String sql = "";
        if(Stream.of(values).anyMatch(Objects::isNull)){
            sql = colname + " IS NULL";
        }
        if(!sql.isEmpty()){
            if(values.length == 1){
                return " AND "+ sql;
            }
            return " AND (" + sql + " " + colname + " IN(" + Utils.nArg(values.length) + "))";//error
        }
        args.addAll(Arrays.asList(values));
        argTypes.addAll(Collections.nCopies(values.length, colname.getType()));
        return " AND "+ colname + " IN(" + Utils.nArg(values.length) + ")";
    }

    String startToSql(Filters start , Collection<Object> args, Collection<Integer> argTypes, Instant value){

        if(value  != null ){
            args.add(Timestamp.from(value));
            argTypes.add(TIMESTAMP);
            return  " AND "+ start +" >= ?";
        }
        return "";
    }

    String endToSql(Filters end , Collection<Object> args, Collection<Integer> argTypes, Instant value){

        if(value != null ){
            args.add(Timestamp.from(value));
            argTypes.add(TIMESTAMP);
            return " AND "+ end +" < ?";
        }
        return "";
    }

    String toSqlLike(Filters colname, Collection<Object> args, Collection<Integer> argTypes, String value) {
        if(value  != null ){
            args.add('%'+value+'%');
            argTypes.add(VARCHAR);
            return  " AND "+ colname +" LIKE ?";
        }
        return "";
    }


    @Override
    public String toString() {
        return "FilterCriteria{" +
                "idSession=" + Arrays.toString(idSession) +
                ", method=" + Arrays.toString(method) +
                ", protocol=" + Arrays.toString(protocol) +
                ", host=" + Arrays.toString(host) +
                ", port=" + Arrays.toString(port) +
                ", path='" + path + '\'' +
                ", query='" + query + '\'' +
                ", media=" + Arrays.toString(media) +
                ", auth=" + Arrays.toString(auth) +
                ", status=" + Arrays.toString(status) +
                ", start=" + start +
                ", end=" + end +
                ", apiname=" + Arrays.toString(apiname) +
                ", user=" + Arrays.toString(user) +
                ", appname=" + Arrays.toString(appname) +
                ", env=" + Arrays.toString(env) +
                ", launchMode=" + Arrays.toString(launchMode) +
                ", location=" + location +
                ", name=" + Arrays.toString(name) +
                '}';
    }


}
