package com.earthkiii.createregexfilter.filter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.earthkiii.createregexfilter.regex.RegexCompiler;
import io.netty.buffer.ByteBuf;
import net.minecraft.locale.Language;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * Settings of a Regex Filter, stored on the item as a data component.
 *
 * @param pattern    RE2 syntax, matched anywhere in the subject ("find" semantics)
 * @param target     what the pattern is matched against
 * @param language   language code (e.g. {@code fr_fr}) item names are rendered in; empty when matching mod IDs
 * @param deny       deny mode: let through only what the pattern does not match
 * @param ignoreCase upper and lower case letters match each other
 */
public record RegexFilterConfig(String pattern, Target target, String language, boolean deny, boolean ignoreCase) {

	/** Longest language code accepted (vanilla's longest is 6 characters). */
	public static final int MAX_LANGUAGE_LENGTH = 16;

	public static final RegexFilterConfig DEFAULT = new RegexFilterConfig("", Target.ITEM_NAME, Language.DEFAULT, false, true);

	public static final Codec<RegexFilterConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.fieldOf("pattern").forGetter(RegexFilterConfig::pattern),
		Target.CODEC.optionalFieldOf("target", Target.ITEM_NAME).forGetter(RegexFilterConfig::target),
		Codec.STRING.optionalFieldOf("language", "").forGetter(RegexFilterConfig::language),
		Codec.BOOL.optionalFieldOf("deny", false).forGetter(RegexFilterConfig::deny),
		Codec.BOOL.optionalFieldOf("ignore_case", true).forGetter(RegexFilterConfig::ignoreCase)
	).apply(instance, RegexFilterConfig::new));

	public static final StreamCodec<ByteBuf, RegexFilterConfig> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.stringUtf8(RegexCompiler.MAX_PATTERN_LENGTH), RegexFilterConfig::pattern,
		Target.STREAM_CODEC, RegexFilterConfig::target,
		ByteBufCodecs.stringUtf8(MAX_LANGUAGE_LENGTH), RegexFilterConfig::language,
		ByteBufCodecs.BOOL, RegexFilterConfig::deny,
		ByteBufCodecs.BOOL, RegexFilterConfig::ignoreCase,
		RegexFilterConfig::new
	);

	/** Values from saves, commands or packets are clamped here, so the rest of the mod can trust them. */
	public RegexFilterConfig {
		if (pattern == null)
			pattern = "";
		if (pattern.length() > RegexCompiler.MAX_PATTERN_LENGTH)
			pattern = pattern.substring(0, RegexCompiler.MAX_PATTERN_LENGTH);
		if (target == null)
			target = Target.ITEM_NAME;
		// The language only means something for item names, so mod ID filters do not keep one.
		if (target == Target.MOD_ID)
			language = "";
		else if (!isValidLanguageCode(language))
			language = Language.DEFAULT;
	}

	public RegexFilterConfig withPattern(String pattern) {
		return new RegexFilterConfig(pattern, target, language, deny, ignoreCase);
	}

	public RegexFilterConfig withTarget(Target target) {
		return new RegexFilterConfig(pattern, target, language, deny, ignoreCase);
	}

	public RegexFilterConfig withLanguage(String language) {
		return new RegexFilterConfig(pattern, target, language, deny, ignoreCase);
	}

	public RegexFilterConfig withDeny(boolean deny) {
		return new RegexFilterConfig(pattern, target, language, deny, ignoreCase);
	}

	public RegexFilterConfig withIgnoreCase(boolean ignoreCase) {
		return new RegexFilterConfig(pattern, target, language, deny, ignoreCase);
	}

	public RegexCompiler.Result compile() {
		return RegexCompiler.compile(pattern, ignoreCase);
	}

	/** Language codes are resource path fragments like {@code en_us}, {@code lol_us} or {@code tok}. */
	public static boolean isValidLanguageCode(String code) {
		if (code == null || code.isEmpty() || code.length() > MAX_LANGUAGE_LENGTH)
			return false;
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-'))
				return false;
		}
		return true;
	}

	public enum Target implements StringRepresentable {
		/** The stack's display name (custom name included), rendered in the filter's language. */
		ITEM_NAME("item_name"),
		/** The namespace of the item's (or fluid's) registry ID, e.g. {@code create}. */
		MOD_ID("mod_id");

		public static final Codec<Target> CODEC = StringRepresentable.fromEnum(Target::values);
		public static final StreamCodec<ByteBuf, Target> STREAM_CODEC =
			ByteBufCodecs.idMapper(ByIdMap.continuous(Target::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO),
				Target::ordinal);

		private final String serializedName;

		Target(String serializedName) {
			this.serializedName = serializedName;
		}

		@Override
		public String getSerializedName() {
			return serializedName;
		}
	}
}
