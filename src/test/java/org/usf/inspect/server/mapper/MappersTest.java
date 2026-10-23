package org.usf.inspect.server.mapper;

import org.junit.jupiter.api.Test;
import org.usf.inspect.server.model.RestSession;

import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MappersTest {


    @Test
    void restSessionResultSetMapper_ShouldParseIntermediateNodes_WhenColumnIsCommaSeparated() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getString("intermediateNodes")).thenReturn("nœud1,nœud2");

        RestSession result = Mappers.restSessionResultSetMapper().map(rs);

        assertEquals(List.of("nœud1", "nœud2"), result.getIntermediateNodes());
    }

    @Test
    void restSessionResultSetMapper_ShouldLeaveIntermediateNodesNull_WhenColumnIsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true);
        when(rs.getString("intermediateNodes")).thenReturn(null);

        RestSession result = Mappers.restSessionResultSetMapper().map(rs);

        assertNull(result.getIntermediateNodes());
    }
}
