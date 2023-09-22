package org.usf.trace.api.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

import static java.sql.Types.TIMESTAMP;
import static java.sql.Types.VARCHAR;

@Getter
@Setter
@AllArgsConstructor
public class FilterCriteria {

    private final String[] idIncoming; //

    private final String[] idMain;
    private final String[] name;
    private final String[] env;
    private final String[] port;

    private final String[] launchMode;
    private final Instant start;
    private final Instant  end;


    public String toSql(Filters idIncomingColname,
                        Filters idMainColname,
                        Filters nameColname,
                        Filters envColname,
                        Filters portColname,
                        Filters launchModeColName,
                        Filters startColName,
                        Filters endColName,
                        Collection<Object> args,
                        Collection<Integer> argTypes) {

        var sql = " WHERE 1 = 1";
        sql += toSql(idIncomingColname,args,argTypes,getIdIncoming());
        sql += toSql(idMainColname,args,argTypes,getIdMain());
        sql += toSql(nameColname,args,argTypes,getName());
        sql += toSql(envColname,args,argTypes,getEnv());
        sql += toSql(portColname,args,argTypes,getPort());
        sql += toSql(launchModeColName,args,argTypes,getLaunchMode());
        sql += startToSql(startColName,args,argTypes,getStart()) ;
        sql += endToSql( endColName,args,argTypes,getEnd());
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
            return " AND (" + sql + " " + colname + "IN(" + Utils.nArg(values.length) + "))";//error
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


    @Override
    public String toString() {
        return "FilterCriteria{" +
                "id=" + Arrays.toString(idIncoming) +
                ", host=" + Arrays.toString(name) +
                ", env=" + Arrays.toString(env) +
                ", port=" + Arrays.toString(port) +
                ", start='" + start + '\'' +
                ", end='" + end + '\'' +
                '}';
    }


}
