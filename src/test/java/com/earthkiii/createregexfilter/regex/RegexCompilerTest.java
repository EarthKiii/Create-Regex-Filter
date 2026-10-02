package com.earthkiii.createregexfilter.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.re2j.Pattern;

class RegexCompilerTest {

	private static CompiledRegex valid(String pattern) {
		return valid(pattern, false);
	}

	private static CompiledRegex valid(String pattern, boolean ignoreCase) {
		return assertInstanceOf(RegexCompiler.Valid.class, RegexCompiler.compile(pattern, ignoreCase), pattern).regex();
	}

	private static String invalid(String pattern) {
		return assertInstanceOf(RegexCompiler.Invalid.class, RegexCompiler.compile(pattern, false), pattern).error();
	}

	@Test
	void literalPatternsUseTheFastPathWithRe2Semantics() {
		List<String> patterns = List.of("", "ingot", "^Iron", "_ore$", "^minecraft$", "^", "$", "^$", "Iron Ingot", "é");
		List<String> inputs = List.of("", "Iron Ingot", "Gold Ingot", "iron_ore", "minecraft", "minecraft2", "Épée", "ingot");
		for (String pattern : patterns) {
			CompiledRegex regex = valid(pattern);
			assertInstanceOf(LiteralRegex.class, regex, pattern);
			Pattern reference = Pattern.compile(pattern);
			for (String input : inputs)
				assertEquals(reference.matcher(input).find(), regex.find(input), "/" + pattern + "/ on \"" + input + "\"");
		}
	}

	@Test
	void ignoringCaseMatchesRe2CaseFolding() {
		// Includes the characters where Java's and RE2's case folding differ (Turkish i, Kelvin sign, long s).
		List<String> patterns = List.of("ingot", "^IRON", "_ORE$", "^Minecraft$", "k", "s", "épée", "^É", "i", "Iron I.got");
		List<String> inputs = List.of("", "Iron Ingot", "iron_ore", "MINECRAFT", "\u212A", "\u017F", "ÉPÉE", "épée",
			"\u0131", "\u0130", "I", "Iron ingot");
		for (String pattern : patterns) {
			CompiledRegex regex = valid(pattern, true);
			Pattern reference = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			for (String input : inputs)
				assertEquals(reference.matcher(input).find(), regex.find(input), "/" + pattern + "/i on \"" + input + "\"");
		}
		assertInstanceOf(LiteralRegex.class, valid("^Minecraft$", true));
		// Non-ASCII literals are left to RE2 when ignoring case.
		assertInstanceOf(Re2Regex.class, valid("épée", true));
	}

	@Test
	void regexPatternsUseRe2() {
		CompiledRegex regex = valid("^(iron|gold) ingot$", true);
		assertInstanceOf(Re2Regex.class, regex);
		assertTrue(regex.find("Iron Ingot"));
		assertTrue(regex.find("GOLD INGOT"));
		assertFalse(regex.find("Copper Ingot"));
		assertFalse(regex.find("Iron Ingots"));
		assertFalse(valid("^(iron|gold) ingot$", false).find("Iron Ingot"));
	}

	@Test
	void inlineCaseFlagIsRejected() {
		for (String pattern : List.of("(?i)iron", "(?i:iron)", "a(?-i)b", "(?si)x", "(?U)(?i)x"))
			assertTrue(invalid(pattern).contains("Ignore Case"), pattern);
		// Other flags, and "(?i)" quoted, escaped or in a class, are fine.
		valid("(?s)a.b");
		valid("\\Q(?i)\\E");
		valid("\\(?i\\)");
		valid("[(?i)]");
		valid("(?P<name>i)");
	}

	@Test
	void findSemanticsMatchAnywhere() {
		CompiledRegex regex = valid("[0-9]+");
		assertTrue(regex.find("Music Disc 13"));
		assertFalse(regex.find("Music Disc"));
	}

	@Test
	void memoizedResultsStayCorrectPastTheMemoLimit() {
		CompiledRegex regex = valid("7$");
		for (int round = 0; round < 2; round++)
			for (int i = 0; i < 10_000; i++)
				assertEquals(i % 10 == 7, regex.find("item " + i));
	}

	@Test
	void compiledPatternsAreShared() {
		assertSame(RegexCompiler.compile("a|b", false), RegexCompiler.compile("a|b", false));
		assertFalse(RegexCompiler.compile("a|b", false) == RegexCompiler.compile("a|b", true));
	}

	@Test
	void syntaxErrorsAreReported() {
		assertTrue(invalid("(abc").contains("missing closing )"));
		assertTrue(invalid("a**").contains("invalid nested repetition operator"));
		// Back-references and lookarounds are not supported by RE2.
		invalid("(a)\\1");
		invalid("a(?=b)");
	}

	@Test
	void tooLongPatternsAreRejected() {
		assertEquals(32, RegexCompiler.MAX_PATTERN_LENGTH);
		invalid("a".repeat(RegexCompiler.MAX_PATTERN_LENGTH + 1));
		valid("a".repeat(RegexCompiler.MAX_PATTERN_LENGTH));
		valid("^(iron|gold|copper) ingot$");
	}

	@Test
	void catastrophicBacktrackingPatternsRunInLinearTime() {
		// java.util.regex needs about 2^40 steps for this; RE2 needs 41.
		CompiledRegex regex = valid("(a+)+$");
		String input = "a".repeat(40) + "!";
		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertFalse(regex.find(input)));
		CompiledRegex alternation = valid("^(a|aa)+$");
		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertFalse(alternation.find("a".repeat(60) + "b")));
	}

	@Test
	void nestedRepetitionBlowUpIsRejectedBeforeCompiling() {
		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
			assertTrue(invalid("((a{1000}){1000}){1000}").contains("nested repetition"));
			assertTrue(invalid("(a{2}){600}").contains("nested repetition"));
			assertTrue(invalid("(?:x{100}){100}").contains("nested repetition"));
			assertTrue(invalid("(abcdefghijklmnopqrstuvw){999}").contains("too complex"));
		});
		valid("a{1000}");
		valid("(a{10}){100}");
		valid("[0-9]{1,3}(\\.[0-9]{1,3}){3}");
	}

	@Test
	void complexityScanUnderstandsClassesAndEscapes() {
		// Braces inside classes, escaped or quoted, or not forming a count, do not multiply anything.
		assertEquals(5, ComplexityGuard.estimate("[{}]{5}").repeatProduct());
		assertEquals(1, ComplexityGuard.estimate("\\{1000\\}").repeatProduct());
		assertEquals(1, ComplexityGuard.estimate("\\Q(a{1000}){1000}\\E").repeatProduct());
		assertEquals(1, ComplexityGuard.estimate("a{,5}").repeatProduct());
		assertEquals(1, ComplexityGuard.estimate("[[:alpha:]{]").repeatProduct());
		assertEquals(1000, ComplexityGuard.estimate("(\\p{L}{10}){100}").repeatProduct());
		valid("\\Q(a{1000}){1000}\\E");
		valid("a{,5}");
	}
}
