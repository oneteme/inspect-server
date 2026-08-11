package org.usf.inspect.server.model;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Defines supported time-based partition granularities and their SQL generation helpers.
 */
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

    /**
     * Formats a partition table name for the given date and base table.
     *
     * @param ldt the date-time used to build the partition suffix.
     * @param table the base table name.
     * @return the formatted partition table name.
     */
    public final String format(LocalDateTime ldt, String table) {
        return String.format("%s_partitioned_%s", table, formatInternally(ldt));
    }

    /**
     * Returns the next partition boundary after the given date-time.
     *
     * @param ldt the current partition boundary.
     * @return the next partition boundary.
     */
    public abstract LocalDateTime next(LocalDateTime ldt);

    /**
     * Returns the first date-time included for the given starting month.
     *
     * @param start the starting month.
     * @return the start date-time for partition creation.
     */
    public abstract LocalDateTime toStartDate(YearMonth start);

    /**
     * Returns the exclusive end date-time for the given ending month.
     *
     * @param end the ending month.
     * @return the exclusive end date-time for partition creation.
     */
    public abstract LocalDateTime toEndDate(YearMonth end);
    abstract String formatInternally(LocalDateTime ldt);

    /**
     * Builds the SQL statement required to create a single partition.
     *
     * @param table the parent table name.
     * @param from the inclusive partition start.
     * @param to the exclusive partition end.
     * @param name the partition table name.
     * @return the SQL statement that creates the partition.
     */
    public String createPartition(String table, LocalDateTime from , LocalDateTime to, String name){
        return String.format("CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s');", name, table, from, to);
    }
    
    /**
     * Builds partition creation SQL for all configured partitioned tables within the given period.
     *
     * @param start the starting month of the requested period.
     * @param end the ending month of the requested period.
     * @param map the partition strategy to use for each supported table.
     * @return the SQL statements required to create the requested partitions.
     */
    public static List<String> buildPartitionScript(YearMonth start, YearMonth end, Map<PartitionedTable, Partition> map){
        var scripts = new ArrayList<String>();
        for (String table : PartitionedTable.tables) {
            var part = PartitionedTable.enumOf(table).map(map::get).orElse(MONTH);
            var from = part.toStartDate(start);
            var to = part.toEndDate(end);
            LocalDateTime next = null;
            if(table != null && from != null && to != null) {
                do {
                    next = part.next(from);
                    var name = part.format(from, table);
                    scripts.add(part.createPartition(table, from, next, name));
                    from = next;
                } while (to.isAfter(next));
            }
        }
        return scripts;
    }
}