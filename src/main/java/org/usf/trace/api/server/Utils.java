package org.usf.trace.api.server;

import static java.util.Objects.isNull;

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
    		throw new RuntimeException("too many results"); //custom exception
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
			return ""; // throw
		}
		return n == 1 ? "?" : "?" + ", ?".repeat(n - 1);
	}
	
}
