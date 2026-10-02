package com.earthkiii.createregexfilter.regex;

import org.jetbrains.annotations.Nullable;

/**
 * Fast path for patterns without any metacharacter, optionally anchored with {@code ^} and/or {@code $}
 * (e.g. {@code ingot}, {@code ^Iron}, {@code _ore$}, {@code ^minecraft$}). These are by far the most
 * common filter patterns and need no regex engine at all: they become a plain
 * {@code contains / startsWith / endsWith / equals} call.
 * <p>
 * The semantics are identical to RE2's: without the {@code m} flag, {@code ^} and {@code $} only
 * match at the start and end of the whole input. Ignoring case is only handled here for ASCII
 * literals, using RE2's case folding exactly (which, unlike Java's, does not equate the Turkish
 * {@code ı} and {@code İ} with {@code i}); other literals go to RE2.
 */
final class LiteralRegex extends CompiledRegex {

	private static final String METACHARACTERS = "\\^$.|?*+()[]{}";

	private final String literal;
	private final boolean anchoredStart;
	private final boolean anchoredEnd;
	private final boolean ignoreCase;

	private LiteralRegex(String literal, boolean anchoredStart, boolean anchoredEnd, boolean ignoreCase) {
		this.literal = literal;
		this.anchoredStart = anchoredStart;
		this.anchoredEnd = anchoredEnd;
		this.ignoreCase = ignoreCase;
	}

	@Nullable
	static LiteralRegex tryCreate(String pattern, boolean ignoreCase) {
		int start = 0;
		int end = pattern.length();
		boolean anchoredStart = end > 0 && pattern.charAt(0) == '^';
		if (anchoredStart)
			start++;
		boolean anchoredEnd = end > start && pattern.charAt(end - 1) == '$';
		if (anchoredEnd)
			end--;

		for (int i = start; i < end; i++) {
			char c = pattern.charAt(i);
			if (METACHARACTERS.indexOf(c) >= 0 || ignoreCase && c >= 0x80)
				return null;
		}

		return new LiteralRegex(pattern.substring(start, end), anchoredStart, anchoredEnd, ignoreCase);
	}

	@Override
	public boolean find(String input) {
		if (!ignoreCase) {
			if (anchoredStart && anchoredEnd)
				return input.equals(literal);
			if (anchoredStart)
				return input.startsWith(literal);
			if (anchoredEnd)
				return input.endsWith(literal);
			return input.contains(literal);
		}

		int last = input.length() - literal.length();
		if (anchoredStart && anchoredEnd)
			return last == 0 && matchesAt(input, 0);
		if (anchoredStart)
			return last >= 0 && matchesAt(input, 0);
		if (anchoredEnd)
			return last >= 0 && matchesAt(input, last);
		for (int i = 0; i <= last; i++)
			if (matchesAt(input, i))
				return true;
		return false;
	}

	private boolean matchesAt(String input, int offset) {
		for (int i = 0; i < literal.length(); i++)
			if (!sameIgnoringCase(input.charAt(offset + i), literal.charAt(i)))
				return false;
		return true;
	}

	/** RE2's simple case folding, for an ASCII character of the literal. */
	private static boolean sameIgnoringCase(char inputChar, char literalChar) {
		if (inputChar == literalChar)
			return true;
		char lower = toLowerAscii(literalChar);
		if (lower < 'a' || lower > 'z')
			return false;
		if (inputChar < 0x80)
			return toLowerAscii(inputChar) == lower;
		// The only non-ASCII characters RE2 folds onto ASCII letters: KELVIN SIGN and LATIN SMALL LETTER LONG S.
		return lower == 'k' && inputChar == '\u212A' || lower == 's' && inputChar == '\u017F';
	}

	private static char toLowerAscii(char c) {
		return c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c;
	}
}
