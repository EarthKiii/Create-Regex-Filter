package com.earthkiii.createregexfilter.regex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;

/**
 * Turns filter patterns into {@link CompiledRegex} matchers.
 * <p>
 * Compilation results (including errors) are cached by pattern and case setting, so the many filters of a
 * factory that share a pattern share one compiled matcher and one result memo, and re-reading a
 * filter item never recompiles.
 */
public final class RegexCompiler {

	/** Maximum pattern length in chars, also enforced by the item data and the network packet. */
	public static final int MAX_PATTERN_LENGTH = 32;

	private static final int MAX_CACHED_PATTERNS = 512;
	private static final Map<Key, Result> CACHE = new ConcurrentHashMap<>();

	private record Key(String pattern, boolean ignoreCase) {
	}

	private RegexCompiler() {
	}

	public sealed interface Result {
	}

	public record Valid(CompiledRegex regex) implements Result {
	}

	/** @param error a short, English description of the problem (RE2J's own messages are English too). */
	public record Invalid(String error) implements Result {
	}

	public static Result compile(String pattern, boolean ignoreCase) {
		Key key = new Key(pattern, ignoreCase);
		Result cached = CACHE.get(key);
		if (cached != null)
			return cached;

		Result result = compileUncached(pattern, ignoreCase);
		if (CACHE.size() >= MAX_CACHED_PATTERNS)
			CACHE.clear();
		CACHE.put(key, result);
		return result;
	}

	static Result compileUncached(String pattern, boolean ignoreCase) {
		if (pattern.length() > MAX_PATTERN_LENGTH)
			return new Invalid("pattern is longer than " + MAX_PATTERN_LENGTH + " characters");

		LiteralRegex literal = LiteralRegex.tryCreate(pattern, ignoreCase);
		if (literal != null)
			return new Valid(literal);

		// One way to ignore case, so a filter's tooltip and button always tell the truth.
		if (PatternSyntax.usesCaseFlag(pattern))
			return new Invalid("(?i) is not supported: use the Ignore Case button (Aa)");

		String complexityError = ComplexityGuard.check(pattern);
		if (complexityError != null)
			return new Invalid(complexityError);

		try {
			return new Valid(new Re2Regex(Pattern.compile(pattern, ignoreCase ? Pattern.CASE_INSENSITIVE : 0)));
		} catch (PatternSyntaxException e) {
			return new Invalid(describe(e));
		}
	}

	private static String describe(PatternSyntaxException e) {
		String pattern = e.getPattern();
		if (pattern == null || pattern.isEmpty())
			return e.getDescription();
		return e.getDescription() + ": " + pattern;
	}
}
