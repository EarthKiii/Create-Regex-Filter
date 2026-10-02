package com.earthkiii.createregexfilter.lang;

import org.jetbrains.annotations.Nullable;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * The translations of one language, looked up without touching the global {@link Language} instance
 * (which is the language of this game instance, not the one a filter asks for).
 */
public interface TranslationTable {

	/**
	 * @return the translation of {@code key}, else {@code fallback}, else the key itself
	 *     (the same rules as {@link Language#getOrDefault(String, String)})
	 */
	String translate(String key, @Nullable String fallback);

	/** NeoForge lets a language file define a translation as a whole text component. */
	@Nullable
	Component component(String key);

	static TranslationTable of(Language language) {
		return new TranslationTable() {
			@Override
			public String translate(String key, @Nullable String fallback) {
				return fallback != null ? language.getOrDefault(key, fallback) : language.getOrDefault(key);
			}

			@Override
			public Component component(String key) {
				return language.getComponent(key);
			}
		};
	}
}
