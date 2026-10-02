package com.earthkiii.createregexfilter.regex;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Estimates how large a pattern's compiled program would be, before handing it to RE2J.
 * <p>
 * RE2J matches in linear time, but it compiles counted repetitions by copying the repeated
 * sub-expression: {@code x{1000}} becomes 1000 copies of {@code x}. Unlike Go's regexp and C++ RE2,
 * RE2J 1.8 only checks each count on its own (at most 1000), not their nesting, so
 * {@code ((a{1000}){1000}){1000}} would try to build a billion instructions and exhaust the heap.
 * This scan applies the same two limits RE2 does: the product of nested counts (1000) and the
 * overall program size.
 * <p>
 * It only has to understand the structure of the pattern (groups, escapes, classes, counts); syntax
 * errors are left to RE2J, which reports them properly.
 */
final class ComplexityGuard {

	/** Same limit as Go's regexp / RE2 for nested counted repetition, e.g. {@code (a{10}){100}} = 1000. */
	static final long MAX_REPEAT_PRODUCT = 1000;
	/** Upper bound on the estimated number of compiled atoms (a few MB of RE2J instructions at most). */
	static final long MAX_PROGRAM_SIZE = 20_000;

	private static final long CAP = 1L << 40;

	record Estimate(long size, long repeatProduct) {
	}

	private ComplexityGuard() {
	}

	/** Returns an error message if the pattern is too complex to compile safely, or {@code null}. */
	static String check(String pattern) {
		Estimate estimate = estimate(pattern);
		if (estimate.repeatProduct() > MAX_REPEAT_PRODUCT)
			return "nested repetition is too large (" + estimate.repeatProduct() + " > " + MAX_REPEAT_PRODUCT + ")";
		if (estimate.size() > MAX_PROGRAM_SIZE)
			return "pattern is too complex (expands to about " + estimate.size() + " parts, max " + MAX_PROGRAM_SIZE + ")";
		return null;
	}

	static Estimate estimate(String p) {
		Deque<Frame> enclosing = new ArrayDeque<>();
		Frame frame = new Frame();
		// The atom a following quantifier applies to; size 0 means there is none.
		long atomSize = 0;
		long atomProduct = 1;

		int n = p.length();
		for (int i = 0; i < n; i++) {
			char c = p.charAt(i);
			switch (c) {
				case '(' -> {
					enclosing.push(frame);
					frame = new Frame();
					atomSize = 0;
				}
				case ')' -> {
					Frame inner = frame;
					frame = enclosing.isEmpty() ? new Frame() : enclosing.pop();
					frame.add(inner.size, inner.product);
					atomSize = inner.size;
					atomProduct = inner.product;
				}
				case '|' -> atomSize = 0;
				case '*', '+', '?' -> {
					// Loops and optionals compile to a constant number of extra instructions.
				}
				case '{' -> {
					int close = p.indexOf('}', i);
					long count = close < 0 ? -1 : parseRepeatCount(p, i + 1, close);
					if (count >= 0 && atomSize > 0) {
						long copies = Math.max(count, 1);
						frame.size = saturate(frame.size + atomSize * (copies - 1));
						atomSize = saturate(atomSize * copies);
						atomProduct = saturate(atomProduct * copies);
						frame.product = Math.max(frame.product, atomProduct);
						i = close;
					} else {
						// Not a valid count, so RE2 reads '{' as a literal character.
						frame.add(1, 1);
						atomSize = 1;
						atomProduct = 1;
					}
				}
				default -> {
					if (c == '\\')
						i = PatternSyntax.skipEscape(p, i);
					else if (c == '[')
						i = PatternSyntax.skipCharClass(p, i);
					frame.add(1, 1);
					atomSize = 1;
					atomProduct = 1;
				}
			}
		}

		// Unbalanced '(' (a syntax error RE2J will report): fold the open groups in.
		while (!enclosing.isEmpty()) {
			Frame inner = frame;
			frame = enclosing.pop();
			frame.add(inner.size, inner.product);
		}
		return new Estimate(frame.size, frame.product);
	}

	/**
	 * Parses the content of {@code {n}}, {@code {n,}} or {@code {n,m}} and returns the largest count,
	 * or -1 if it is not a valid repetition (RE2 then treats the braces literally).
	 */
	private static long parseRepeatCount(String p, int from, int to) {
		int comma = -1;
		for (int i = from; i < to; i++) {
			char c = p.charAt(i);
			if (c == ',' && comma < 0)
				comma = i;
			else if (c < '0' || c > '9')
				return -1;
		}
		int minEnd = comma < 0 ? to : comma;
		if (minEnd == from)
			return -1;
		long min = parseBounded(p, from, minEnd);
		if (comma < 0 || comma + 1 == to)
			return min;
		return Math.max(min, parseBounded(p, comma + 1, to));
	}

	private static long parseBounded(String p, int from, int to) {
		long value = 0;
		for (int i = from; i < to; i++)
			value = saturate(value * 10 + (p.charAt(i) - '0'));
		return value;
	}

	private static long saturate(long value) {
		return value < 0 || value > CAP ? CAP : value;
	}

	private static final class Frame {
		long size;
		long product = 1;

		void add(long atomSize, long atomProduct) {
			size = saturate(size + atomSize);
			product = Math.max(product, atomProduct);
		}
	}
}
