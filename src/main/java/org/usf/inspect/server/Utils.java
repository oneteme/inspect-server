package org.usf.inspect.server;

import static java.util.Arrays.fill;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {
	
    public static <T> T requireSingle(Collection<T> c){
    	if(isEmpty(c)) {
    		return null;
    	}
    	if(c.size() > 1) {
    		throw new IllegalArgumentException("too many results"); //custom exception
    	}
    	return c.iterator().next();
    }

    public static boolean isEmpty(Collection<?> c) {
    	return isNull(c) || c.isEmpty();
    }

    public static <T> boolean isEmpty(T[] arr){
    	return isNull(arr) || arr.length == 0;
    }

	public static String nArg(int n) {
		if (n < 1) {
			throw new IllegalArgumentException("n < 1");
		}
		return n == 1 ? "?" : "?" + ", ?".repeat(n - 1);
	}
	
	public static int[] newArray(int size, int defaultValue) {
		var arr = new int[size];
		fill(arr, defaultValue);
		return arr;
	}

	public static Instant fromNullableTimestamp(Timestamp timestamp) {
		return ofNullable(timestamp).map(Timestamp::toInstant).orElse(null);
	}
}
