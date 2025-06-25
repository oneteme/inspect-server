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

	public static String userAgentExtract(String userAgent) {
		if (userAgent == null) return null;
		try {
			if (userAgent.contains("Mozilla")) {

				var os = java.util.regex.Pattern.compile("Mozilla/([\\d.]+) (\\([^)]+\\))").matcher(userAgent);
				var osGroup = os.find() ? " " + os.group(2): "";

				// L'ordre est important (Edge contient Chrome, Chrome contient Safari)
				if (userAgent.contains("Edg") || userAgent.contains("Edge")) {
					var m = java.util.regex.Pattern.compile("(Edg|Edge)/([\\d.]+)").matcher(userAgent);
					return m.find() ? "Edge/" + m.group(2) + osGroup : "Edge" + osGroup;
				}

				if (userAgent.contains("Chrome")) {
					var m = java.util.regex.Pattern.compile("Chrome/([\\d.]+)").matcher(userAgent);
					return m.find() ? "Chrome/" + m.group(1) + osGroup : "Chrome" + osGroup;
				}

				if (userAgent.contains("Firefox")) {
					var m = java.util.regex.Pattern.compile("Firefox/([\\d.]+)").matcher(userAgent);
					return m.find() ? "Firefox/" + m.group(1) + osGroup : "Firefox" + osGroup;
				}

				if (userAgent.contains("Safari")) {
					var m = java.util.regex.Pattern.compile("Safari/([\\d.]+)").matcher(userAgent);
					return m.find() ? "Safari/" + m.group(1) + osGroup : "Safari" + osGroup;
				}
			}
			if (userAgent.contains("Postman")) {
				var m = java.util.regex.Pattern.compile("PostmanRuntime/([\\d.]+)").matcher(userAgent);
				return m.find() ? "Postman/" + m.group(1) : "Postman";
			}
			return userAgent;
		} catch (Exception e) {
			return userAgent;
		}
	}

	public static String contentTypeExtract(String contentType) {
		if (contentType == null) return null;
		try {
			var cnt = contentType.split("/");
			var val = cnt.length == 2 ? cnt[1] : contentType;
			if(val.contains("json") || val.contains("problem+json")) return "json";
			if(val.contains("xml")) return "xml";
			if(val.contains("octet-stream")) return "stream";
			if(val.contains("vnd.openxmlformats-officedocument.spreadsheetml.sheet")) return "xlsx";
			if(val.contains("vnd.ms-excel")) return "xls";
			if(val.contains("zip")) return "zip";
			if(val.contains("gzip")) return "gzip";
			if(val.contains("x-www-form-urlencoded")) return "form";
			if(val.contains("vnd.openxmlformats-officedocument.wordprocessingml.document")) return "docx";
			if(val.contains("msword")) return "doc";
			if(val.contains("pdf")) return "pdf";
			if(val.contains("html")) return "html";
			if(val.contains("plain")) return "txt";
			if(val.contains("javascript")) return "js";
			if(val.contains("css")) return "css";
			if(val.contains("csv")) return "csv";
			if(val.contains("jpeg")) return "jpeg";
			if(val.contains("png")) return "png";
			return val;
		} catch (Exception e) {
			return contentType;
		}
	}
}
