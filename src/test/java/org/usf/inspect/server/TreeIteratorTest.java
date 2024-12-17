package org.usf.inspect.server;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.usf.inspect.server.TreeIterator.treeIterator;

import org.junit.jupiter.api.Test;

class TreeIteratorTest {

	@Test
	void test1() {
		var list = asList(
				asList('A', 'B', 'C'),
				asList('1', '2', '3'),
				asList('a', 'b', 'c'));
		var s = "";
		var it = treeIterator(list, identity());
		while(it.hasNext()) {
			s+=it.next();
		}
		assertEquals("ABC123abc", s);
	}

	@Test
	void test2() {
		var list = asList(
				null,
				asList(
						asList('A', 'B', 'C'),
						asList(),
						asList('a', 'b', 'c'),
						asList()),
				null,
				asList(
						asList(),
						asList(),
						asList('1', '2', '3')),
				null);
		var s = "";
		var it = treeIterator(list, identity(), c-> c);
		while(it.hasNext()) {
			s+=it.next();
		}
		assertEquals("ABCabc123", s);
	}
}
