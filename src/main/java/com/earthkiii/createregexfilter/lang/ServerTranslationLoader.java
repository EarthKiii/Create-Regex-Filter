package com.earthkiii.createregexfilter.lang;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.earthkiii.createregexfilter.CreateRegexFilter;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Loads a language on a dedicated server, which only has English loaded (and no client resources).
 * <p>
 * Reads {@code assets/<namespace>/lang/<code>.json} from every pack the server has: all mod jars,
 * plus any enabled data pack that contains an {@code assets} folder. This is the same approach
 * NeoForge's {@code LanguageHook} uses to load English on the server.
 * <p>
 * Minecraft's own translations are the exception: the server jar only contains {@code en_us}, and the
 * other languages are downloaded by the launcher on clients only. To filter vanilla item names in
 * another language on a dedicated server, add a data pack containing
 * {@code assets/minecraft/lang/<code>.json} (copied from a client). Missing keys fall back to English.
 */
final class ServerTranslationLoader {

	private ServerTranslationLoader() {
	}

	static TranslationTable load(String code) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null || code.equals(Language.DEFAULT))
			return new FallbackTable(Map.of(), Map.of());

		Map<String, String> strings = new HashMap<>();
		Map<String, Component> components = new HashMap<>();

		ResourceManager dataResources = server.getServerResources().resourceManager();
		// Same view over the server's packs, but for assets/ instead of data/. It must not be closed:
		// that would close the packs, which the server still uses.
		ResourceManager assets = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, dataResources.listPacks().toList());
		String path = String.format(Locale.ROOT, "lang/%s.json", code);

		int files = 0;
		for (String namespace : assets.getNamespaces()) {
			ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, path);
			for (Resource resource : assets.getResourceStack(location)) {
				try (InputStream stream = resource.open()) {
					Language.loadFromJson(stream, strings::put, components::put);
					files++;
				} catch (Exception e) {
					CreateRegexFilter.LOGGER.warn("Skipped language file {} from pack {}", location, resource.sourcePackId(), e);
				}
			}
		}
		CreateRegexFilter.LOGGER.debug("Loaded {} translations for {} from {} files", strings.size(), code, files);
		return new FallbackTable(strings, components);
	}

	/** The loaded language, falling back to the server's English for keys it does not define. */
	private record FallbackTable(Map<String, String> strings, Map<String, Component> components)
		implements TranslationTable {

		@Override
		public String translate(String key, @Nullable String fallback) {
			String value = strings.get(key);
			if (value != null)
				return value;
			Language english = Language.getInstance();
			return fallback != null ? english.getOrDefault(key, fallback) : english.getOrDefault(key);
		}

		@Override
		public Component component(String key) {
			Component value = components.get(key);
			if (value != null || strings.containsKey(key))
				return value;
			return Language.getInstance().getComponent(key);
		}
	}
}
