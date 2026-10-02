package com.earthkiii.createregexfilter.regex;

/**
 * A compiled filter pattern. Instances are immutable, thread-safe and shared between every filter
 * that uses the same pattern string (see {@link RegexCompiler}).
 * <p>
 * Matching uses "find" semantics, like grep: the pattern matches if it matches anywhere in the input.
 * Use {@code ^} and {@code $} to anchor it.
 */
public abstract sealed class CompiledRegex permits LiteralRegex, Re2Regex {

	public abstract boolean find(String input);
}
