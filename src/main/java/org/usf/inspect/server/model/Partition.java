package org.usf.inspect.server.model;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum Partition {
    DAY {
        @Override
        public LocalDateTime next(LocalDateTime ldt) {
            return ldt.plusDays(1);
        }

        @Override
        String formatInternally(LocalDateTime ldt) {
            return String.format("%s_%02d_%02d", ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth());
        }

        @Override
        public LocalDateTime toStartDate(YearMonth start) {
            return start.atDay(1).atStartOfDay();
        }

        @Override
        public LocalDateTime toEndDate(YearMonth end) {
            return end.plusMonths(1).atDay(1).atStartOfDay();
        }
    },
    MONTH {
        @Override
        public LocalDateTime next(LocalDateTime ldt) {
            return ldt.plusMonths(1);
        }

        @Override
        public String formatInternally(LocalDateTime ldt) {
            return String.format("%s_%02d", ldt.getYear(), ldt.getMonthValue());
        }

        @Override
        public LocalDateTime toStartDate(YearMonth start) {
            return start.atDay(1).atStartOfDay();
        }

        @Override
        public LocalDateTime toEndDate(YearMonth end) {
            return end.plusMonths(1).atDay(1).atStartOfDay();
        }
    };

    public final String format(LocalDateTime ldt, String table) {
        return String.format("%s_partitioned_%s", table, formatInternally(ldt));
    }

    public abstract LocalDateTime next(LocalDateTime ldt);
    public abstract LocalDateTime toStartDate(YearMonth start);
    public abstract LocalDateTime toEndDate(YearMonth end);
    abstract String formatInternally(LocalDateTime ldt);

    public String createPartition(String table, LocalDateTime from , LocalDateTime to, String name){
        return String.format("CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s');", name, table, from, to);
    }

    public static List<String> buildPartitionScript(YearMonth start, YearMonth end, Map<PartitionedTable, Partition> map){
        var scripts = new ArrayList<String>();
        for (String table : PartitionedTable.tables) {
            var part = PartitionedTable.enumOf(table).map(map::get).orElse(Partition.MONTH);
            var from = part.toStartDate(start);
            var to = part.toEndDate(end);
            LocalDateTime next = null;
            do {
                next = part.next(from);
                var name = part.format(from, table);
                scripts.add(part.createPartition(table, from, next, name));
                from = next;
            } while(to.isAfter(next));
        }
        return scripts;
    }
}