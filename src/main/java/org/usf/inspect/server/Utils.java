package org.usf.inspect.server;

import static java.lang.Thread.ofVirtual;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.LongStream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Provides shared helper methods used across the server module.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Utils {

    private static final Predicate<String> isUUID = compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$").asPredicate();

    /**
     * Returns the single element from the collection when exactly one value is present.
     *
     * @param c the collection to inspect
     * @param <T> the element type contained in the collection
     * @return the single element in the collection, or {@code null} when the collection is empty or {@code null}
     */
    public static <T> T requireSingle(Collection<T> c){
    	if(isEmpty(c)) {
    		return null;
    	}
    	if(c.size() > 1) {
    		throw new IllegalArgumentException("too many results"); //custom exception
    	}
    	return c.iterator().next();
    }

    /**
     * Joins the provided values with a comma separator when the array is not {@code null}.
     *
     * @param args the values to join
     * @return the joined string, or {@code null} when the array is {@code null}
     */
    public static String joinValuesOrNull(String... args) {
        return nonNull(args) ? String.join(", ", args) : null;
    }

    /**
     * Checks whether the collection is {@code null} or contains no elements.
     *
     * @param c the collection to evaluate
     * @return {@code true} if the collection is {@code null} or empty, otherwise {@code false}
     */
    public static boolean isEmpty(Collection<?> c) {
    	return isNull(c) || c.isEmpty();
    }

	/**
	 * Converts a nullable SQL timestamp to an instant.
	 *
	 * @param timestamp the timestamp to convert
	 * @return the corresponding instant, or {@code null} when the timestamp is {@code null}
	 */
	public static Instant fromNullableTimestamp(Timestamp timestamp) {
		return ofNullable(timestamp).map(Timestamp::toInstant).orElse(null);
	}

	/**
	 * Converts a nullable instant to a SQL timestamp.
	 *
	 * @param instant the instant to convert
	 * @return the corresponding timestamp, or {@code null} when the instant is {@code null}
	 */
	public static Timestamp fromNullableInstant(Instant instant) {
		return nonNull(instant) ? Timestamp.from(instant) : null;
	}

	/**
	 * Returns the string representation of the object when it is not {@code null}.
	 *
	 * @param o the object to convert
	 * @return the object's string representation, or {@code null} when the object is {@code null}
	 */
	public static String valueOfNullable(Object o) {// do not use Objects::toString
		return nonNull(o) ? o.toString() : null;
	}

	/**
	 * Converts a nullable array of primitive long values to a comma-separated string.
	 *
	 * @param array the array to convert
	 * @return the comma-separated values, or {@code null} when the array is {@code null}
	 */
	public static String  valueOfNullableArray(long[]array){
		return nonNull(array)
				? LongStream.of(array).mapToObj(Long::toString).collect(joining(","))
				: null;
	}

	/**
	 * Creates a fixed-size executor backed by virtual threads using the given name prefix.
	 *
	 * @param name the prefix to use for created thread names
	 * @param size the maximum number of concurrent threads
	 * @return a virtual-thread-based executor service
	 */
	public static ExecutorService virtualThreadExecutor(String name, int size) {
		return newFixedThreadPool(size, ofVirtual().name(name + "-", 0).factory());
	}

	//TODO declare regex pattern as static final and reuse it
	/**
	 * Extracts a simplified user agent description from the raw header value.
	 *
	 * @param userAgent the raw user agent string to analyze
	 * @return a simplified user agent description, or the original value when it cannot be simplified
	 */
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

	/**
	 * Extracts a normalized short content type label from the raw content type value.
	 *
	 * @param contentType the raw content type string to analyze
	 * @return the normalized content type label, or the original value when it cannot be normalized
	 */
	public static String contentTypeExtract(String contentType) {
		if (nonNull(contentType)) {
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
			} catch (Exception e) { /* ignore it */ }
		}
		return contentType;
	}

    /**
     * Validates that the provided string is a UUID and returns it unchanged when valid.
     *
     * @param uuid the value to validate
     * @param name the logical name of the value used in the error message
     * @return the validated UUID string
     */
    public static String assertUUID(String uuid, String name) {
        if (isUUID(uuid)) {
            return uuid;
        }
        throw new IllegalArgumentException(name + " is not a valid UUID: " + uuid);
    }

    /**
     * Checks whether the provided string matches the expected UUID format.
     *
     * @param uuid the value to validate
     * @return {@code true} if the value is a non-null UUID string, otherwise {@code false}
     */
    public static boolean isUUID(String uuid) {
        return nonNull(uuid) && isUUID.test(uuid);
    }
}
