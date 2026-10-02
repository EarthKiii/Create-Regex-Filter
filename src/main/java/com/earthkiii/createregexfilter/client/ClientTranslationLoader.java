package com.earthkiii.createregexfilter.client;

import java.util.List;

import com.earthkiii.createregexfilter.lang.TranslationTable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;

/**
 * Loads languages from the client's resources: every vanilla language (from the launcher's asset
 * index), every mod's language files and the player's resource packs.
 */
final class ClientTranslationLoader {

	private ClientTranslationLoader() {
	}

	static TranslationTable load(String code) {
		Minecraft minecraft = Minecraft.getInstance();
		if (code.equals(minecraft.getLanguageManager().getSelected()))
			return TranslationTable.of(Language.getInstance());

		// English first, so keys missing from the language fall back to English, like in game.
		List<String> files = code.equals(Language.DEFAULT) ? List.of(Language.DEFAULT) : List.of(Language.DEFAULT, code);
		return TranslationTable.of(ClientLanguage.loadFrom(minecraft.getResourceManager(), files, false));
	}
}
