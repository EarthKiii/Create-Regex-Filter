package com.earthkiii.createregexfilter.regex;

/**
 * Just enough knowledge of RE2 syntax to walk a pattern's structure: escapes and character classes
 * are skipped as a whole, so their contents are never mistaken for groups, counts or flags.
 */
final class PatternSyntax {

	private static final String FLAG_CHARACTERS = "imsU-";

	private PatternSyntax() {
	}

	/**
	 * Whether the pattern sets or clears the case-insensitive flag inline, as in {@code (?i)abc},
	 * {@code (?i:abc)} or {@code (?-i)}. Case is controlled by the filter's Ignore Case button instead.
	 */
	static boolean usesCaseFlag(String p) {
		int n = p.length();
		for (int i = 0; i < n; i++) {
			char c = p.charAt(i);
			if (c == '\\') {
				i = skipEscape(p, i);
			} else if (c == '[') {
				i = skipCharClass(p, i);
			} else if (c == '(' && i + 1 < n && p.charAt(i + 1) == '?') {
				for (int j = i + 2; j < n && FLAG_CHARACTERS.indexOf(p.charAt(j)) >= 0; j++)
					if (p.charAt(j) == 'i')
						return true;
			}
		}
		return false;
	}

	/** Returns the index of the last character of the escape sequence starting at {@code i}. */
	static int skipEscape(String p, int i) {
		int n = p.length();
		if (i + 1 >= n)
			return i;
		char e = p.charAt(i + 1);
		if (e == 'Q') {
			// \Q...\E quotes everything in between.
			int end = p.indexOf("\\E", i + 2);
			return end < 0 ? n - 1 : end + 1;
		}
		if ((e == 'p' || e == 'P' || e == 'x') && i + 2 < n && p.charAt(i + 2) == '{') {
			int close = p.indexOf('}', i + 3);
			return close < 0 ? n - 1 : close;
		}
		return i + 1;
	}

	/** Returns the index of the ']' closing the character class starting at {@code i}. */
	static int skipCharClass(String p, int i) {
		int n = p.length();
		int j = i + 1;
		if (j < n && p.charAt(j) == '^')
			j++;
		if (j < n && p.charAt(j) == ']')
			j++;
		while (j < n) {
			char c = p.charAt(j);
			if (c == '\\') {
				j = skipEscape(p, j) + 1;
				continue;
			}
			if (c == '[' && j + 1 < n && p.charAt(j + 1) == ':') {
				int end = p.indexOf(":]", j + 2);
				if (end >= 0) {
					j = end + 2;
					continue;
				}
			}
			if (c == ']')
				return j;
			j++;
		}
		return n - 1;
	}
}
