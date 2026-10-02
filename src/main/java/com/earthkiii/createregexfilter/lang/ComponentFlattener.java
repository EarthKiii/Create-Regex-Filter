package com.earthkiii.createregexfilter.lang;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Renders a text component to a plain string in a given language, like {@link Component#getString()}
 * does for the current language.
 * <p>
 * {@link TranslatableContents} always resolves against the global language, so translatable parts
 * are formatted here instead, following {@code TranslatableContents#decomposeTemplate} exactly
 * (including its fallback to the raw template on a malformed format string).
 */
public final class ComponentFlattener {

	// Same pattern as TranslatableContents. It is fixed and cannot backtrack badly, so java.util.regex is fine.
	private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");
	private static final int MAX_DEPTH = 32;

	private ComponentFlattener() {
	}

	public static String flatten(Component component, TranslationTable table) {
		StringBuilder out = new StringBuilder();
		append(component, table, out, 0);
		return out.toString();
	}

	private static void append(Component component, TranslationTable table, StringBuilder out, int depth) {
		if (depth > MAX_DEPTH)
			return;

		ComponentContents contents = component.getContents();
		if (contents instanceof TranslatableContents translatable) {
			appendTranslatable(translatable, table, out, depth);
		} else {
			contents.visit(text -> {
				out.append(text);
				return Optional.empty();
			});
		}

		for (Component sibling : component.getSiblings())
			append(sibling, table, out, depth + 1);
	}

	private static void appendTranslatable(TranslatableContents translatable, TranslationTable table, StringBuilder out,
		int depth) {
		Component asComponent = table.component(translatable.getKey());
		if (asComponent != null) {
			append(asComponent, table, out, depth + 1);
			return;
		}

		String template = table.translate(translatable.getKey(), translatable.getFallback());
		int mark = out.length();
		if (!appendTemplate(template, translatable.getArgs(), table, out, depth)) {
			out.setLength(mark);
			out.append(template);
		}
	}

	/** @return false where vanilla would reject the template (and show it unformatted instead) */
	private static boolean appendTemplate(String template, Object[] args, TranslationTable table, StringBuilder out,
		int depth) {
		Matcher matcher = FORMAT_PATTERN.matcher(template);
		int nextArg = 0;
		int pos = 0;

		while (matcher.find(pos)) {
			int start = matcher.start();
			int end = matcher.end();
			if (start > pos) {
				if (template.indexOf('%', pos, start) >= 0)
					return false;
				out.append(template, pos, start);
			}

			String conversion = matcher.group(2);
			if ("%".equals(conversion) && end - start == 2) {
				out.append('%');
			} else {
				if (!"s".equals(conversion))
					return false;
				int index;
				String explicitIndex = matcher.group(1);
				try {
					index = explicitIndex != null ? Integer.parseInt(explicitIndex) - 1 : nextArg++;
				} catch (NumberFormatException e) {
					return false;
				}
				if (index < 0 || index >= args.length)
					return false;

				Object arg = args[index];
				if (arg instanceof Component argComponent)
					append(argComponent, table, out, depth + 1);
				else
					out.append(arg);
			}
			pos = end;
		}

		if (pos < template.length()) {
			if (template.indexOf('%', pos) >= 0)
				return false;
			out.append(template, pos, template.length());
		}
		return true;
	}
}
