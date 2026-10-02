package com.earthkiii.createregexfilter.regex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.re2j.Pattern;

/**
 * A pattern evaluated by RE2J, whose automaton-based engine runs in time linear in the input length
 * whatever the pattern, so a player cannot freeze the server with a pathological pattern like
 * {@code (a+)+$} (which makes backtracking engines such as {@link java.util.regex} take exponential time).
 * <p>
 * Filters see a small, finite set of subjects (item names, mod IDs) over and over, so results are
 * memoized per input string: after warm-up a test costs one hash lookup instead of a regex run.
 */
final class Re2Regex extends CompiledRegex {

	/** Past this many distinct inputs the memo is dropped and rebuilt, which bounds its memory. */
	private static final int MAX_MEMOIZED_INPUTS = 4096;

	private final Pattern pattern;
	private final Map<String, Boolean> memo = new ConcurrentHashMap<>();

	/** @param pattern compiled with {@link Pattern#CASE_INSENSITIVE} when the filter ignores case */
	Re2Regex(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override
	public boolean find(String input) {
		Boolean known = memo.get(input);
		if (known != null)
			return known;

		boolean result = pattern.matcher(input).find();
		if (memo.size() >= MAX_MEMOIZED_INPUTS)
			memo.clear();
		memo.put(input, result);
		return result;
	}
}
